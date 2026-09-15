package com.manoel.carteiradigital.dto.response;

import com.manoel.carteiradigital.domain.enums.CategoriaTransacao;

import java.math.BigDecimal;

public record GastoPorCategoriaResponse(
        CategoriaTransacao categoria,
        BigDecimal total
) {}
