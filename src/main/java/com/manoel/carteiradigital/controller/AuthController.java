package com.manoel.carteiradigital.controller;

import com.manoel.carteiradigital.dto.request.LoginRequest;
import com.manoel.carteiradigital.dto.request.RegistroUsuarioRequest;
import com.manoel.carteiradigital.dto.response.TokenResponse;
import com.manoel.carteiradigital.dto.response.UsuarioResponse;
import com.manoel.carteiradigital.service.AuthService;
import com.manoel.carteiradigital.service.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Cadastro e login de usuários da aplicação")
@SecurityRequirements
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroUsuarioRequest request) {
        UsuarioResponse response = usuarioService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
