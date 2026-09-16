package com.manoel.carteiradigital.dto.response;

import com.manoel.carteiradigital.domain.enums.CategoriaTransacao;
import com.manoel.carteiradigital.domain.enums.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        CategoriaTransacao categoria,
        LocalDate dataTransacao,
        String contaOrigem,
        Long contaId
) {}
