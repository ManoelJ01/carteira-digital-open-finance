package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.domain.enums.CategoriaTransacao;
import com.manoel.carteiradigital.domain.enums.TipoConta;
import com.manoel.carteiradigital.domain.enums.TipoTransacao;
import com.manoel.carteiradigital.domain.model.ContaBancaria;
import com.manoel.carteiradigital.domain.model.TransacaoConsolidada;
import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.response.ContaResumoResponse;
import com.manoel.carteiradigital.dto.response.DashboardResumoResponse;
import com.manoel.carteiradigital.dto.response.TransacaoResponse;
import com.manoel.carteiradigital.mapper.ContaBancariaMapper;
import com.manoel.carteiradigital.mapper.TransacaoConsolidadaMapper;
import com.manoel.carteiradigital.repository.ContaBancariaRepository;
import com.manoel.carteiradigital.repository.TransacaoConsolidadaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ContaBancariaRepository contaBancariaRepository;
    @Mock
    private TransacaoConsolidadaRepository transacaoConsolidadaRepository;
    @Mock
    private ContaBancariaMapper contaBancariaMapper;
    @Mock
    private TransacaoConsolidadaMapper transacaoConsolidadaMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void deveConsolidarSaldoDeMultiplasContasDeBancosDiferentes() {
        Usuario usuario = Usuario.builder().id(1L).nome("Usuário de teste").build();

        ContaBancaria contaBancoA = ContaBancaria.builder().id(10L).tipo(TipoConta.BANK)
                .saldo(new BigDecimal("1500.50")).build();
        ContaBancaria contaBancoB = ContaBancaria.builder().id(20L).tipo(TipoConta.BANK)
                .saldo(new BigDecimal("320.00")).build();

        TransacaoConsolidada gastoAlimentacao = TransacaoConsolidada.builder()
                .id(100L).contaBancaria(contaBancoA).valor(new BigDecimal("50.00"))
                .tipo(TipoTransacao.DEBITO).categoria(CategoriaTransacao.ALIMENTACAO)
                .dataTransacao(LocalDate.now()).build();

        TransacaoConsolidada gastoTransporte = TransacaoConsolidada.builder()
                .id(101L).contaBancaria(contaBancoB).valor(new BigDecimal("30.00"))
                .tipo(TipoTransacao.DEBITO).categoria(CategoriaTransacao.TRANSPORTE)
                .dataTransacao(LocalDate.now()).build();

        when(contaBancariaRepository.findAllByUsuarioId(1L)).thenReturn(List.of(contaBancoA, contaBancoB));
        when(transacaoConsolidadaRepository.findExtratoUnificado(1L)).thenReturn(List.of(gastoAlimentacao, gastoTransporte));

        when(contaBancariaMapper.toResumoResponse(contaBancoA))
                .thenReturn(new ContaResumoResponse(10L, "Banco A", "Conta Corrente", "BANK", contaBancoA.getSaldo(), "BRL"));
        when(contaBancariaMapper.toResumoResponse(contaBancoB))
                .thenReturn(new ContaResumoResponse(20L, "Banco B", "Conta Corrente", "BANK", contaBancoB.getSaldo(), "BRL"));

        when(transacaoConsolidadaMapper.toResponse(gastoAlimentacao))
                .thenReturn(new TransacaoResponse(100L, "Restaurante", new BigDecimal("50.00"), TipoTransacao.DEBITO,
                        CategoriaTransacao.ALIMENTACAO, LocalDate.now(), "Conta Corrente", 10L));
        when(transacaoConsolidadaMapper.toResponse(gastoTransporte))
                .thenReturn(new TransacaoResponse(101L, "Uber", new BigDecimal("30.00"), TipoTransacao.DEBITO,
                        CategoriaTransacao.TRANSPORTE, LocalDate.now(), "Conta Corrente", 20L));

        DashboardResumoResponse resumo = dashboardService.resumo(usuario);

        assertThat(resumo.saldoConsolidado()).isEqualByComparingTo("1820.50");
        assertThat(resumo.contas()).hasSize(2);
        assertThat(resumo.extratoUnificado()).hasSize(2);
        assertThat(resumo.extratoUnificado()).extracting(TransacaoResponse::contaId).containsExactly(10L, 20L);
        assertThat(resumo.usuario().nome()).isEqualTo("Usuário de teste");
        assertThat(resumo.gastosPorCategoria())
                .extracting("categoria")
                .containsExactlyInAnyOrder(CategoriaTransacao.ALIMENTACAO, CategoriaTransacao.TRANSPORTE);
    }
}
