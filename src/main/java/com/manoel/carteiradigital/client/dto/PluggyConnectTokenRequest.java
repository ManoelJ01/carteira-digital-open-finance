package com.manoel.carteiradigital.client.dto;

// itemId é opcional: quando presente, gera um token para atualizar um item já existente (update mode)
public record PluggyConnectTokenRequest(String itemId) {}
