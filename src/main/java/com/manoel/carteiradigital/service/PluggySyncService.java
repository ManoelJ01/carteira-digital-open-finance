package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.client.PluggyClient;
import com.manoel.carteiradigital.client.dto.PluggyAccountDto;
import com.manoel.carteiradigital.client.dto.PluggyItemResponse;
import com.manoel.carteiradigital.client.dto.PluggyTransactionDto;
import com.manoel.carteiradigital.client.dto.PluggyTransactionsResponse;
import com.manoel.carteiradigital.domain.enums.StatusConexao;
import com.manoel.carteiradigital.domain.enums.TipoConta;
import com.manoel.carteiradigital.domain.enums.TipoTransacao;
import com.manoel.carteiradigital.domain.model.ConexaoBancaria;
import com.manoel.carteiradigital.domain.model.ContaBancaria;
import com.manoel.carteiradigital.domain.model.TransacaoConsolidada;
import com.manoel.carteiradigital.dto.response.SincronizacaoResponse;
import com.manoel.carteiradigital.exception.PluggyIntegracaoException;
import com.manoel.carteiradigital.repository.ConexaoBancariaRepository;
import com.manoel.carteiradigital.repository.ContaBancariaRepository;
import com.manoel.carteiradigital.repository.TransacaoConsolidadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Responsável pela sincronização de contas e transações de uma conexão bancária (item da Pluggy),
 * consumindo as rotas /accounts e /v2/transactions. Usado tanto pelo endpoint manual
 * POST /open-finance/sync/{itemId} quanto pela rotina agendada (@Scheduled).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PluggySyncService {

    private static final int TAMANHO_PAGINA_TRANSACOES = 50;

    private final PluggyClient pluggyClient;
    private final PluggyAuthService pluggyAuthService;
    private final CategorizacaoService categorizacaoService;
    private final ConexaoBancariaRepository conexaoBancariaRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final TransacaoConsolidadaRepository transacaoConsolidadaRepository;

    @Transactional
    public SincronizacaoResponse sincronizar(ConexaoBancaria conexao) {
        String apiKey = pluggyAuthService.obterApiKey();
        String itemId = conexao.getPluggyItemId();

        atualizarStatusItem(conexao, apiKey, itemId);

        List<PluggyAccountDto> contasPluggy = buscarContas(apiKey, itemId);

        int contasSincronizadas = 0;
        int transacoesNovas = 0;

        for (PluggyAccountDto contaDto : contasPluggy) {
            ContaBancaria conta = sincronizarConta(conexao, contaDto);
            contasSincronizadas++;
            transacoesNovas += sincronizarTransacoes(apiKey, conta);
        }

        conexao.setUltimaSincronizacao(LocalDateTime.now());
        conexaoBancariaRepository.save(conexao);

        return new SincronizacaoResponse(conexao.getId(), conexao.getStatus().name(), contasSincronizadas, transacoesNovas);
    }

    private void atualizarStatusItem(ConexaoBancaria conexao, String apiKey, String itemId) {
        try {
            PluggyItemResponse item = pluggyClient.buscarItem(apiKey, itemId);
            conexao.setExecutionStatus(item.executionStatus());
            conexao.setStatus(mapearStatus(item.status()));
        } catch (Exception e) {
            log.error("Falha ao atualizar status do item {} na Pluggy", itemId, e);
            throw new PluggyIntegracaoException("Não foi possível consultar o status do item na Pluggy", e);
        }
    }

    private List<PluggyAccountDto> buscarContas(String apiKey, String itemId) {
        try {
            return pluggyClient.listarContas(apiKey, itemId).results();
        } catch (Exception e) {
            log.error("Falha ao buscar contas do item {} na Pluggy", itemId, e);
            throw new PluggyIntegracaoException("Não foi possível sincronizar as contas na Pluggy", e);
        }
    }

    private ContaBancaria sincronizarConta(ConexaoBancaria conexao, PluggyAccountDto contaDto) {
        ContaBancaria conta = contaBancariaRepository.findByPluggyAccountId(contaDto.id())
                .orElseGet(() -> ContaBancaria.builder()
                        .conexaoBancaria(conexao)
                        .pluggyAccountId(contaDto.id())
                        .build());

        conta.setTipo("CREDIT".equalsIgnoreCase(contaDto.type()) ? TipoConta.CREDIT : TipoConta.BANK);
        conta.setSubtipo(contaDto.subtype());
        conta.setNome(contaDto.name());
        conta.setNumero(contaDto.number());
        conta.setSaldo(contaDto.balance() != null ? contaDto.balance() : BigDecimal.ZERO);
        conta.setMoeda(contaDto.currencyCode() != null ? contaDto.currencyCode() : "BRL");

        return contaBancariaRepository.save(conta);
    }

    private int sincronizarTransacoes(String apiKey, ContaBancaria conta) {
        int novasTransacoes = 0;

        PluggyTransactionsResponse resposta = buscarTransacoes(apiKey, conta.getPluggyAccountId());

        if (resposta != null && resposta.results() != null) {
            for (PluggyTransactionDto transacaoDto : resposta.results()) {
                if (transacaoConsolidadaRepository.existsByPluggyTransactionId(transacaoDto.id())) {
                    continue; // idempotência: transação já sincronizada anteriormente
                }
                transacaoConsolidadaRepository.save(converterTransacao(conta, transacaoDto));
                novasTransacoes++;
            }
        }

        return novasTransacoes;
    }

    private PluggyTransactionsResponse buscarTransacoes(String apiKey, String accountId) {
        try {
            return pluggyClient.listarTransacoes(apiKey, accountId);
        } catch (Exception e) {
            log.error("Falha ao buscar transações da conta {} na Pluggy", accountId, e);
            throw new PluggyIntegracaoException("Não foi possível sincronizar as transações na Pluggy", e);
        }
    }

    private TransacaoConsolidada converterTransacao(ContaBancaria conta, PluggyTransactionDto dto) {
        BigDecimal valor = dto.amount() != null ? dto.amount() : BigDecimal.ZERO;

        return TransacaoConsolidada.builder()
                .contaBancaria(conta)
                .pluggyTransactionId(dto.id())
                .descricao(dto.description())
                .descricaoOriginal(dto.descriptionRaw())
                .valor(valor.abs())
                .tipo(valor.signum() < 0 ? TipoTransacao.DEBITO : TipoTransacao.CREDITO)
                .categoria(categorizacaoService.categorizar(dto.category()))
                .dataTransacao(parseData(dto.date()))
                .build();
    }

    private LocalDate parseData(String data) {
        if (data == null) {
            return LocalDate.now();
        }
        try {
            // A Pluggy retorna datas no formato ISO-8601 (ex.: 2026-09-01T00:00:00.000Z)
            return LocalDate.parse(data.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            log.warn("Não foi possível interpretar a data '{}' retornada pela Pluggy, usando data atual", data);
            return LocalDate.now();
        }
    }

    private StatusConexao mapearStatus(String statusPluggy) {
        if (statusPluggy == null) {
            return StatusConexao.PENDENTE;
        }
        return switch (statusPluggy.toUpperCase()) {
            case "UPDATED" -> StatusConexao.ATUALIZADO;
            case "UPDATING" -> StatusConexao.ATUALIZANDO;
            case "LOGIN_ERROR" -> StatusConexao.LOGIN_ERROR;
            case "OUTDATED" -> StatusConexao.OUTDATED;
            case "ERROR" -> StatusConexao.ERRO;
            default -> StatusConexao.PENDENTE;
        };
    }
}