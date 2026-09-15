package com.manoel.carteiradigital.service;

import com.manoel.carteiradigital.client.PluggyClient;
import com.manoel.carteiradigital.client.dto.PluggyConnectTokenRequest;
import com.manoel.carteiradigital.client.dto.PluggyItemResponse;
import com.manoel.carteiradigital.domain.enums.StatusConexao;
import com.manoel.carteiradigital.domain.model.ConexaoBancaria;
import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.request.RegistrarItemRequest;
import com.manoel.carteiradigital.dto.response.ConexaoBancariaResponse;
import com.manoel.carteiradigital.dto.response.ConnectTokenResponse;
import com.manoel.carteiradigital.exception.PluggyIntegracaoException;
import com.manoel.carteiradigital.exception.RecursoNaoEncontradoException;
import com.manoel.carteiradigital.exception.RegraDeNegocioException;
import com.manoel.carteiradigital.mapper.ConexaoBancariaMapper;
import com.manoel.carteiradigital.repository.ConexaoBancariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestra o fluxo de conexão de contas via Open Finance (Pluggy):
 * geração do connect_token para o widget e registro do item autorizado pelo usuário.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenFinanceService {

    private final PluggyClient pluggyClient;
    private final PluggyAuthService pluggyAuthService;
    private final ConexaoBancariaRepository conexaoBancariaRepository;
    private final ConexaoBancariaMapper conexaoBancariaMapper;

    /**
     * POST /open-finance/connect-token
     * Solicita à Pluggy um token de conexão temporário para inicializar o Pluggy Connect Widget no front-end.
     */
    public ConnectTokenResponse gerarConnectToken() {
        try {
            String apiKey = pluggyAuthService.obterApiKey();
            var resposta = pluggyClient.criarConnectToken(apiKey, new PluggyConnectTokenRequest(null));
            return new ConnectTokenResponse(resposta.accessToken());
        } catch (Exception e) {
            log.error("Falha ao gerar connect_token na Pluggy", e);
            throw new PluggyIntegracaoException("Não foi possível gerar o connect_token da Pluggy", e);
        }
    }

    /**
     * POST /open-finance/items
     * Recebe o itemId gerado pela Pluggy após o usuário autorizar o banco, valida na Pluggy
     * e associa essa conexão ao usuário autenticado no MySQL.
     */
    @Transactional
    public ConexaoBancariaResponse registrarItem(Usuario usuario, RegistrarItemRequest request) {
        if (conexaoBancariaRepository.existsByPluggyItemId(request.itemId())) {
            throw new RegraDeNegocioException("Este item já está vinculado a um usuário");
        }

        PluggyItemResponse item = buscarItemNaPluggy(request.itemId());

        ConexaoBancaria conexao = ConexaoBancaria.builder()
                .usuario(usuario)
                .pluggyItemId(item.id())
                .pluggyConnectorId(item.connector() != null ? item.connector().id() : null)
                .nomeInstituicao(item.connector() != null ? item.connector().name() : null)
                .status(mapearStatus(item.status()))
                .executionStatus(item.executionStatus())
                .build();

        conexao = conexaoBancariaRepository.save(conexao);
        return conexaoBancariaMapper.toResponse(conexao);
    }

    @Transactional(readOnly = true)
    public List<ConexaoBancariaResponse> listarConexoes(Usuario usuario) {
        return conexaoBancariaRepository.findByUsuarioId(usuario.getId()).stream()
                .map(conexaoBancariaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConexaoBancaria buscarConexaoDoUsuario(Long conexaoId, Usuario usuario) {
        return conexaoBancariaRepository.findByIdAndUsuarioId(conexaoId, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Conexão bancária não encontrada para este usuário: " + conexaoId));
    }

    @Transactional(readOnly = true)
    public ConexaoBancaria buscarConexaoPorItemId(String itemId, Usuario usuario) {
        ConexaoBancaria conexao = conexaoBancariaRepository.findByPluggyItemId(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Conexão bancária não encontrada para o itemId: " + itemId));

        if (!conexao.getUsuario().getId().equals(usuario.getId())) {
            throw new RecursoNaoEncontradoException("Conexão bancária não encontrada para o itemId: " + itemId);
        }

        return conexao;
    }

    private PluggyItemResponse buscarItemNaPluggy(String itemId) {
        try {
            String apiKey = pluggyAuthService.obterApiKey();
            return pluggyClient.buscarItem(apiKey, itemId);
        } catch (Exception e) {
            log.error("Falha ao buscar item {} na Pluggy", itemId, e);
            throw new PluggyIntegracaoException("Não foi possível validar o item na Pluggy", e);
        }
    }

    private StatusConexao mapearStatus(String statusPluggy) {
        if (statusPluggy == null) {
            return StatusConexao.PENDENTE;
        }
        return switch (statusPluggy.toUpperCase()) {
            case "UPDATED" -> StatusConexao.ATUALIZADO;
            case "UPDATING" -> StatusConexao.ATUALIZANDO;
            case "LOGIN_ERROR" -> StatusConexao.LOGIN_ERROR;
            case "OUTDATED" -> StatusConexao.OUTDATED;
            case "ERROR" -> StatusConexao.ERRO;
            default -> StatusConexao.PENDENTE;
        };
    }
}
