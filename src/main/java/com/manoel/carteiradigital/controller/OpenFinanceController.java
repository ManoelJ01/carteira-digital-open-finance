package com.manoel.carteiradigital.controller;

import com.manoel.carteiradigital.domain.model.ConexaoBancaria;
import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.request.RegistrarItemRequest;
import com.manoel.carteiradigital.dto.response.ConexaoBancariaResponse;
import com.manoel.carteiradigital.dto.response.ConnectTokenResponse;
import com.manoel.carteiradigital.dto.response.SincronizacaoResponse;
import com.manoel.carteiradigital.service.OpenFinanceService;
import com.manoel.carteiradigital.service.PluggySyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/open-finance")
@RequiredArgsConstructor
@Tag(name = "Open Finance", description = "Conexão e sincronização de contas bancárias via Pluggy")
public class OpenFinanceController {

    private final OpenFinanceService openFinanceService;
    private final PluggySyncService pluggySyncService;

    @PostMapping("/connect-token")
    public ResponseEntity<ConnectTokenResponse> gerarConnectToken() {
        return ResponseEntity.ok(openFinanceService.gerarConnectToken());
    }

    @PostMapping("/items")
    public ResponseEntity<ConexaoBancariaResponse> registrarItem(@AuthenticationPrincipal Usuario usuario,
                                                                   @Valid @RequestBody RegistrarItemRequest request) {
        ConexaoBancariaResponse response = openFinanceService.registrarItem(usuario, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/items")
    public ResponseEntity<List<ConexaoBancariaResponse>> listarConexoes(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(openFinanceService.listarConexoes(usuario));
    }

    @PostMapping("/sync/{itemId}")
    public ResponseEntity<SincronizacaoResponse> sincronizar(@AuthenticationPrincipal Usuario usuario,
                                                               @PathVariable String itemId) {
        ConexaoBancaria conexao = openFinanceService.buscarConexaoPorItemId(itemId, usuario);
        return ResponseEntity.ok(pluggySyncService.sincronizar(conexao));
    }
}
