package com.manoel.carteiradigital.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manoel.carteiradigital.dto.request.LoginRequest;
import com.manoel.carteiradigital.dto.request.RegistroUsuarioRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração ponta a ponta do fluxo de registro + login, subindo um MySQL real
 * via Testcontainers e aplicando as migrations do Flyway (mesmo comportamento de produção).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.3")
            .withDatabaseName("carteira_digital_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveRegistrarUsuarioEEfetuarLoginRetornandoTokenJwt() throws Exception {
        RegistroUsuarioRequest registro = new RegistroUsuarioRequest(
                "Manoel Juvino", "manoel.it@example.com", "98765432100", "senhaSegura123");

        mockMvc.perform(post("/auth/registro")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registro)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("manoel.it@example.com"));

        LoginRequest login = new LoginRequest("manoel.it@example.com", "senhaSegura123");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void deveRetornarBadRequestQuandoDadosDeRegistroForemInvalidos() throws Exception {
        RegistroUsuarioRequest registroInvalido = new RegistroUsuarioRequest(
                "", "email-invalido", "123", "123");

        mockMvc.perform(post("/auth/registro")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registroInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }
}
