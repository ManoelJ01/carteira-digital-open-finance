package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.request.RegistroUsuarioRequest;
import com.manoel.carteiradigital.dto.response.UsuarioResponse;
import com.manoel.carteiradigital.exception.RegraDeNegocioException;
import com.manoel.carteiradigital.mapper.UsuarioMapper;
import com.manoel.carteiradigital.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UsuarioService usuarioService;

    private RegistroUsuarioRequest request;

    @BeforeEach
    void setUp() {
        request = new RegistroUsuarioRequest("Manoel Juvino", "manoel@example.com", "12345678901", "senhaSegura123");
    }

    @Test
    void deveRegistrarUsuarioComSenhaCriptografada() {
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(passwordEncoder.encode(request.senha())).thenReturn("hash-bcrypt");

        Usuario usuarioSalvo = Usuario.builder()
                .id(1L)
                .nome(request.nome())
                .email(request.email())
                .cpf(request.cpf())
                .senha("hash-bcrypt")
                .criadoEm(LocalDateTime.now())
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);
        when(usuarioMapper.toResponse(usuarioSalvo))
                .thenReturn(new UsuarioResponse(1L, request.nome(), request.email(), request.cpf(), usuarioSalvo.getCriadoEm()));

        UsuarioResponse response = usuarioService.registrar(request);

        assertThat(response.email()).isEqualTo(request.email());
        verify(passwordEncoder).encode(request.senha());
        verify(usuarioRepository).save(argThat(u -> u.getSenha().equals("hash-bcrypt")));
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() {
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("e-mail");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoCpfJaCadastrado() {
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByCpf(request.cpf())).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CPF");

        verify(usuarioRepository, never()).save(any());
    }
}
