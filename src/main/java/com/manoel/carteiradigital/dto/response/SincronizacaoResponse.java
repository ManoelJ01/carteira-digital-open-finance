package com.manoel.carteiradigital.dto.response;

public record SincronizacaoResponse(
        Long conexaoId,
        String status,
        int contasSincronizadas,
        int transacoesNovas
) {}
