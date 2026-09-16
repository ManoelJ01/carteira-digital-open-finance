package com.manoel.carteiradigital.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manoel.carteiradigital.domain.enums.CategoriaTransacao;
import com.manoel.carteiradigital.domain.enums.TipoConta;
import com.manoel.carteiradigital.domain.enums.TipoTransacao;
import com.manoel.carteiradigital.domain.model.ConexaoBancaria;
import com.manoel.carteiradigital.domain.model.ContaBancaria;
import com.manoel.carteiradigital.domain.model.TransacaoConsolidada;
import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.repository.ConexaoBancariaRepository;
import com.manoel.carteiradigital.repository.ContaBancariaRepository;
import com.manoel.carteiradigital.repository.TransacaoConsolidadaRepository;
import com.manoel.carteiradigital.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard-api;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false", "app.scheduler.sync-cron=-",
        "app.jwt.secret=dashboard-api-test-key-at-least-32-characters"
})
@AutoConfigureMockMvc
@Transactional
class DashboardApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ConexaoBancariaRepository conexoes;
    @Autowired ContaBancariaRepository contas;
    @Autowired TransacaoConsolidadaRepository transacoes;
    @Autowired PasswordEncoder encoder;

    @Test
    void loginEResumoDevemUsarPerfilRealIdsDeContasEIsolarUsuarios() throws Exception {
        Usuario usuario = usuario("pessoa@example.com", "12345678901");
        Usuario outro = usuario("outro@example.com", "12345678902");
        ContaBancaria primeira = conta(usuario, "Banco A", "100.50");
        ContaBancaria segunda = conta(usuario, "Banco B", "250.00");
        ContaBancaria alheia = conta(outro, "Banco C", "9999.00");
        transacao(primeira, TipoTransacao.DEBITO, "25.00", LocalDate.of(2026, 1, 1));
        transacao(segunda, TipoTransacao.CREDITO, "100.00", LocalDate.of(2026, 1, 2));
        transacao(alheia, TipoTransacao.CREDITO, "9999.00", LocalDate.of(2026, 1, 3));

        mvc.perform(get("/dashboard/resumo").header("Authorization", "Bearer " + login(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoConsolidado").value(350.50))
                .andExpect(jsonPath("$.usuario.id").value(usuario.getId()))
                .andExpect(jsonPath("$.usuario.nome").value("Pessoa de teste"))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                .andExpect(jsonPath("$.usuario.cpf").doesNotExist())
                .andExpect(jsonPath("$.contas.length()").value(2))
                .andExpect(jsonPath("$.extratoUnificado.length()").value(2))
                .andExpect(jsonPath("$.extratoUnificado[0].contaId").value(segunda.getId()))
                .andExpect(jsonPath("$.extratoUnificado[1].contaId").value(primeira.getId()))
                .andExpect(jsonPath("$.extratoUnificado[1].valor").value(25.00))
                .andExpect(jsonPath("$.extratoUnificado[1].tipo").value("DEBITO"))
                .andExpect(jsonPath("$.gastosPorCategoria[0].total").value(25.00));
    }

    @Test
    void usuarioSemContasRecebeResumoVazio() throws Exception {
        Usuario usuario = usuario("vazio@example.com", "12345678903");
        mvc.perform(get("/dashboard/resumo").header("Authorization", "Bearer " + login(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoConsolidado").value(0))
                .andExpect(jsonPath("$.contas").isEmpty())
                .andExpect(jsonPath("$.extratoUnificado").isEmpty())
                .andExpect(jsonPath("$.usuario.nome").value(usuario.getNome()));
    }

    @Test
    void tokenInvalidoOuExpiradoRetorna401() throws Exception {
        mvc.perform(get("/dashboard/resumo").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
        String expirado = Jwts.builder().subject("pessoa@example.com")
                .expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor("dashboard-api-test-key-at-least-32-characters".getBytes(StandardCharsets.UTF_8)))
                .compact();
        mvc.perform(get("/dashboard/resumo").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    private Usuario usuario(String email, String cpf) {
        return usuarios.saveAndFlush(Usuario.builder().nome("Pessoa de teste").email(email).cpf(cpf)
                .senha(encoder.encode("senha-de-teste")).build());
    }

    private ContaBancaria conta(Usuario usuario, String banco, String saldo) {
        var conexao = conexoes.saveAndFlush(ConexaoBancaria.builder().usuario(usuario)
                .pluggyItemId(banco).nomeInstituicao(banco).build());
        return contas.saveAndFlush(ContaBancaria.builder().conexaoBancaria(conexao)
                .pluggyAccountId(banco).nome("Conta corrente").tipo(TipoConta.BANK)
                .saldo(new BigDecimal(saldo)).build());
    }

    private void transacao(ContaBancaria conta, TipoTransacao tipo, String valor, LocalDate data) {
        transacoes.saveAndFlush(TransacaoConsolidada.builder().contaBancaria(conta)
                .pluggyTransactionId("transacao-" + conta.getId()).descricao("Movimentação de teste")
                .tipo(tipo).categoria(tipo == TipoTransacao.DEBITO ? CategoriaTransacao.COMPRAS : CategoriaTransacao.RENDA)
                .valor(new BigDecimal(valor)).dataTransacao(data).build());
    }

    private String login(Usuario usuario) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("email", usuario.getEmail(), "senha", "senha-de-teste"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }
}
