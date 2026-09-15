package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.domain.enums.CategoriaTransacao;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Traduz a categoria bruta retornada pela Pluggy (em inglês, ex.: "Food and drinks")
 * para o enum interno de categorias usado no dashboard. Quando a categoria da Pluggy
 * não é reconhecida, cai em OUTROS.
 */
@Service
public class CategorizacaoService {

    public CategoriaTransacao categorizar(String categoriaPluggy) {
        if (categoriaPluggy == null || categoriaPluggy.isBlank()) {
            return CategoriaTransacao.OUTROS;
        }

        String categoria = categoriaPluggy.toLowerCase(Locale.ROOT);

        if (contains(categoria, "food", "restaurant", "grocery", "supermarket")) {
            return CategoriaTransacao.ALIMENTACAO;
        }
        if (contains(categoria, "transport", "uber", "taxi", "fuel", "gas station")) {
            return CategoriaTransacao.TRANSPORTE;
        }
        if (contains(categoria, "leisure", "entertainment", "streaming", "cinema", "game")) {
            return CategoriaTransacao.LAZER;
        }
        if (contains(categoria, "health", "pharmacy", "medical")) {
            return CategoriaTransacao.SAUDE;
        }
        if (contains(categoria, "education", "school", "course")) {
            return CategoriaTransacao.EDUCACAO;
        }
        if (contains(categoria, "housing", "rent", "utilities", "electricity", "water bill")) {
            return CategoriaTransacao.MORADIA;
        }
        if (contains(categoria, "shopping", "clothes", "electronics", "retail")) {
            return CategoriaTransacao.COMPRAS;
        }
        if (contains(categoria, "service", "subscription")) {
            return CategoriaTransacao.SERVICOS;
        }
        if (contains(categoria, "salary", "income", "revenue")) {
            return CategoriaTransacao.RENDA;
        }
        if (contains(categoria, "transfer", "pix", "ted", "doc")) {
            return CategoriaTransacao.TRANSFERENCIA;
        }

        return CategoriaTransacao.OUTROS;
    }

    private boolean contains(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) {
                return true;
            }
        }
        return false;
    }
}
