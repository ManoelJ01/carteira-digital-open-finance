package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.domain.enums.CategoriaTransacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CategorizacaoServiceTest {

    private final CategorizacaoService categorizacaoService = new CategorizacaoService();

    @ParameterizedTest
    @CsvSource({
            "Food and drinks, ALIMENTACAO",
            "Restaurants, ALIMENTACAO",
            "Transport, TRANSPORTE",
            "Uber, TRANSPORTE",
            "Leisure, LAZER",
            "Streaming, LAZER",
            "Health, SAUDE",
            "Pharmacy, SAUDE",
            "Education, EDUCACAO",
            "Housing, MORADIA",
            "Shopping, COMPRAS",
            "Service, SERVICOS",
            "Salary, RENDA",
            "Transfer, TRANSFERENCIA",
            "Pix, TRANSFERENCIA"
    })
    void deveCategorizarCorretamenteCategoriasConhecidasDaPluggy(String categoriaPluggy, CategoriaTransacao esperado) {
        assertThat(categorizacaoService.categorizar(categoriaPluggy)).isEqualTo(esperado);
    }

    @Test
    void deveRetornarOutrosQuandoCategoriaForNulaOuVazia() {
        assertThat(categorizacaoService.categorizar(null)).isEqualTo(CategoriaTransacao.OUTROS);
        assertThat(categorizacaoService.categorizar("")).isEqualTo(CategoriaTransacao.OUTROS);
    }

    @Test
    void deveRetornarOutrosQuandoCategoriaNaoForReconhecida() {
        assertThat(categorizacaoService.categorizar("Categoria Desconhecida XYZ")).isEqualTo(CategoriaTransacao.OUTROS);
    }
}
