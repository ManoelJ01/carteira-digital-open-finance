package com.manoel.carteiradigital.dto.response;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String cpf,
        LocalDateTime criadoEm
) {}
