package com.manoel.carteiradigital.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResumoResponse(
        BigDecimal saldoConsolidado,
        List<ContaResumoResponse> contas,
        List<TransacaoResponse> extratoUnificado,
        List<GastoPorCategoriaResponse> gastosPorCategoria
) {}
