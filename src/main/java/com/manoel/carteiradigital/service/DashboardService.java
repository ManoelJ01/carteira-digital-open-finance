package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.domain.model.ContaBancaria;
import com.manoel.carteiradigital.domain.model.TransacaoConsolidada;
import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.response.ContaResumoResponse;
import com.manoel.carteiradigital.dto.response.DashboardResumoResponse;
import com.manoel.carteiradigital.dto.response.GastoPorCategoriaResponse;
import com.manoel.carteiradigital.dto.response.TransacaoResponse;
import com.manoel.carteiradigital.dto.response.PerfilUsuarioResponse;
import com.manoel.carteiradigital.domain.enums.CategoriaTransacao;
import com.manoel.carteiradigital.domain.enums.TipoTransacao;
import com.manoel.carteiradigital.mapper.ContaBancariaMapper;
import com.manoel.carteiradigital.mapper.TransacaoConsolidadaMapper;
import com.manoel.carteiradigital.repository.ContaBancariaRepository;
import com.manoel.carteiradigital.repository.TransacaoConsolidadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GET /dashboard/resumo — consolida saldo, extrato unificado e gastos por categoria
 * de todas as contas bancárias conectadas do usuário autenticado.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContaBancariaRepository contaBancariaRepository;
    private final TransacaoConsolidadaRepository transacaoConsolidadaRepository;
    private final ContaBancariaMapper contaBancariaMapper;
    private final TransacaoConsolidadaMapper transacaoConsolidadaMapper;

    @Transactional(readOnly = true)
    public DashboardResumoResponse resumo(Usuario usuario) {
        List<ContaBancaria> contas = contaBancariaRepository.findAllByUsuarioId(usuario.getId());
        List<TransacaoConsolidada> transacoes = transacaoConsolidadaRepository.findExtratoUnificado(usuario.getId());

        BigDecimal saldoConsolidado = contas.stream()
                .map(ContaBancaria::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ContaResumoResponse> contasResponse = contas.stream()
                .map(contaBancariaMapper::toResumoResponse)
                .toList();

        List<TransacaoResponse> extratoResponse = transacoes.stream()
                .map(transacaoConsolidadaMapper::toResponse)
                .toList();

        List<GastoPorCategoriaResponse> gastosPorCategoria = gastosPorCategoria(transacoes);

        return new DashboardResumoResponse(saldoConsolidado, contasResponse, extratoResponse, gastosPorCategoria,
                new PerfilUsuarioResponse(usuario.getId(), usuario.getNome()));
    }

    private List<GastoPorCategoriaResponse> gastosPorCategoria(List<TransacaoConsolidada> transacoes) {
        Map<CategoriaTransacao, BigDecimal> totais = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DEBITO)
                .collect(Collectors.groupingBy(
                        TransacaoConsolidada::getCategoria,
                        Collectors.reducing(BigDecimal.ZERO, TransacaoConsolidada::getValor, BigDecimal::add)));

        return totais.entrySet().stream()
                .map(entry -> new GastoPorCategoriaResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(GastoPorCategoriaResponse::total).reversed())
                .toList();
    }
}
