package com.manoel.carteiradigital.client.dto;

public record PluggyItemResponse(
        String id,
        PluggyConnectorDto connector,
        String status,
        String executionStatus
) {}
