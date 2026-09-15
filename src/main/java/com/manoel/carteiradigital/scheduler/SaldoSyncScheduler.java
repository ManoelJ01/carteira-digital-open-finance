package com.manoel.carteiradigital.scheduler;

import com.manoel.carteiradigital.domain.model.ConexaoBancaria;
import com.manoel.carteiradigital.repository.ConexaoBancariaRepository;
import com.manoel.carteiradigital.service.PluggySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rotina assíncrona que atualiza em segundo plano os saldos e extratos de todas as
 * contas bancárias vinculadas na base, sem que o usuário precise chamar o endpoint manual.
 * A frequência é configurável via app.scheduler.sync-cron (application.yml).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SaldoSyncScheduler {

    private final ConexaoBancariaRepository conexaoBancariaRepository;
    private final PluggySyncService pluggySyncService;

    @Async
    @Scheduled(cron = "${app.scheduler.sync-cron}")
    public void sincronizarTodasAsConexoes() {
        List<ConexaoBancaria> conexoes = conexaoBancariaRepository.findAll();
        log.info("Iniciando sincronização agendada de {} conexões bancárias", conexoes.size());

        for (ConexaoBancaria conexao : conexoes) {
            try {
                pluggySyncService.sincronizar(conexao);
            } catch (Exception e) {
                // uma falha em uma conexão não deve interromper a sincronização das demais
                log.error("Falha ao sincronizar a conexão {} (item {})", conexao.getId(), conexao.getPluggyItemId(), e);
            }
        }

        log.info("Sincronização agendada concluída");
    }
}
