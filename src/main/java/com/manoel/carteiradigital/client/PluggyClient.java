package com.manoel.carteiradigital.client;

import com.manoel.carteiradigital.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Cliente Feign responsável por toda a comunicação HTTP com a API da Pluggy
 * (https://docs.pluggy.ai). A URL base e as credenciais vêm de application.yml
 * (propriedades app.pluggy.*), nunca hardcoded.
 */
@FeignClient(name = "pluggyClient", url = "${app.pluggy.base-url}", configuration = com.manoel.carteiradigital.config.FeignConfig.class)
public interface PluggyClient {

    /**
     * Autentica com clientId/clientSecret e retorna a apiKey usada nas demais chamadas.
     */
    @PostMapping("/auth")
    PluggyAuthResponse autenticar(@RequestBody PluggyAuthRequest request);

    /**
     * Gera um connect_token temporário para inicializar o Pluggy Connect Widget no front-end.
     * Quando itemId é informado, o token é gerado em "update mode" para aquele item.
     */
    @PostMapping("/connect_token")
    PluggyConnectTokenResponse criarConnectToken(@RequestHeader("X-API-KEY") String apiKey,
                                                  @RequestBody PluggyConnectTokenRequest request);

    @GetMapping("/items/{itemId}")
    PluggyItemResponse buscarItem(@RequestHeader("X-API-KEY") String apiKey,
                                   @PathVariable("itemId") String itemId);

    @GetMapping("/accounts")
    PluggyAccountsResponse listarContas(@RequestHeader("X-API-KEY") String apiKey,
                                         @RequestParam("itemId") String itemId);

    @GetMapping("/v2/transactions")
    PluggyTransactionsResponse listarTransacoes(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestParam("accountId") String accountId);
}

