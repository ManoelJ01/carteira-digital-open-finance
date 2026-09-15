package com.manoel.carteiradigital.client.dto;

import java.math.BigDecimal;

public record PluggyTransactionDto(
        String id,
        String description,
        String descriptionRaw,
        BigDecimal amount,
        String date,
        String category
) {}
