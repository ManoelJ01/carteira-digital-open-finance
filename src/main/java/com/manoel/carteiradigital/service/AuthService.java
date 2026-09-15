package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.request.LoginRequest;
import com.manoel.carteiradigital.dto.response.TokenResponse;
import com.manoel.carteiradigital.repository.UsuarioRepository;
import com.manoel.carteiradigital.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String token = jwtService.gerarToken(usuario);
        return TokenResponse.bearer(token, jwtService.getExpirationMs());
    }
}
