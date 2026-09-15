package com.manoel.carteiradigital.dto.response;

import com.manoel.carteiradigital.domain.enums.StatusConexao;

import java.time.LocalDateTime;

public record ConexaoBancariaResponse(
        Long id,
        String pluggyItemId,
        String nomeInstituicao,
        StatusConexao status,
        LocalDateTime ultimaSincronizacao
) {}
