package com.manoel.carteiradigital.controller;

import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.response.DashboardResumoResponse;
import com.manoel.carteiradigital.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Painel unificado com saldo consolidado, extrato e gastos por categoria")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumo")
    public ResponseEntity<DashboardResumoResponse> resumo(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(dashboardService.resumo(usuario));
    }
}
