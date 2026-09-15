package com.manoel.carteiradigital.client.dto;

import java.math.BigDecimal;

public record PluggyAccountDto(
        String id,
        String type,
        String subtype,
        String name,
        String number,
        BigDecimal balance,
        String currencyCode
) {}
