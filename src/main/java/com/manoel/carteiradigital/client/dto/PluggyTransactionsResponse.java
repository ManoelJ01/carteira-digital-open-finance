package com.manoel.carteiradigital.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyTransactionsResponse(
        List<PluggyTransactionDto> results,
        Long total
) {}