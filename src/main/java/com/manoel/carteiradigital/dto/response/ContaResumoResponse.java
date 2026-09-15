package com.manoel.carteiradigital.dto.response;

import java.math.BigDecimal;

public record ContaResumoResponse(
        Long contaId,
        String instituicao,
        String nome,
        String tipo,
        BigDecimal saldo,
        String moeda
) {}
