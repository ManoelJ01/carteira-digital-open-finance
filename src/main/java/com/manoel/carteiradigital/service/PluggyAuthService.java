package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.client.PluggyClient;
import com.manoel.carteiradigital.client.dto.PluggyAuthRequest;
import com.manoel.carteiradigital.exception.PluggyIntegracaoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Responsável por obter e cachear a apiKey da Pluggy (gerada a partir do CLIENT_ID/CLIENT_SECRET),
 * evitando autenticar a cada requisição. A apiKey da Pluggy costuma ter validade de ~2h;
 * aqui ela é renovada automaticamente um pouco antes de expirar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PluggyAuthService {

    private static final long MARGEM_SEGURANCA_SEGUNDOS = 60;
    private static final long VALIDADE_ESTIMADA_SEGUNDOS = 60 * 55; // ~55 min, com margem de segurança

    private final PluggyClient pluggyClient;

    @Value("${app.pluggy.client-id}")
    private String clientId;

    @Value("${app.pluggy.client-secret}")
    private String clientSecret;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile String apiKeyCache;
    private volatile Instant expiraEm = Instant.EPOCH;

    public String obterApiKey() {
        if (apiKeyValida()) {
            return apiKeyCache;
        }

        lock.lock();
        try {
            if (apiKeyValida()) {
                return apiKeyCache;
            }
            return autenticarNaPluggy();
        } finally {
            lock.unlock();
        }
    }

    private boolean apiKeyValida() {
        return apiKeyCache != null && Instant.now().isBefore(expiraEm);
    }

    private String autenticarNaPluggy() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new PluggyIntegracaoException(
                    "Credenciais da Pluggy não configuradas. Defina PLUGGY_CLIENT_ID e PLUGGY_CLIENT_SECRET.");
        }

        try {
            var resposta = pluggyClient.autenticar(new PluggyAuthRequest(clientId, clientSecret));
            this.apiKeyCache = resposta.apiKey();
            this.expiraEm = Instant.now().plusSeconds(VALIDADE_ESTIMADA_SEGUNDOS - MARGEM_SEGURANCA_SEGUNDOS);
            log.info("Nova apiKey da Pluggy obtida com sucesso");
            return apiKeyCache;
        } catch (Exception e) {
            log.error("Falha ao autenticar na Pluggy", e);
            throw new PluggyIntegracaoException("Não foi possível autenticar na API da Pluggy", e);
        }
    }
}
