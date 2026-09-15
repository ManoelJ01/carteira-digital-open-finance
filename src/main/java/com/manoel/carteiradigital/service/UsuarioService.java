package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.request.RegistroUsuarioRequest;
import com.manoel.carteiradigital.dto.response.UsuarioResponse;
import com.manoel.carteiradigital.exception.RegraDeNegocioException;
import com.manoel.carteiradigital.mapper.UsuarioMapper;
import com.manoel.carteiradigital.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Transactional
    public UsuarioResponse registrar(RegistroUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este e-mail");
        }
        if (usuarioRepository.existsByCpf(request.cpf())) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este CPF");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .cpf(request.cpf())
                .senha(passwordEncoder.encode(request.senha()))
                .ativo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.toResponse(usuario);
    }
}
