package com.manoel.carteiradigital.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegistrarItemRequest(
        @NotBlank(message = "itemId é obrigatório")
        String itemId
) {}
