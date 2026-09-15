package com.manoel.carteiradigital.mapper;

import com.manoel.carteiradigital.domain.model.TransacaoConsolidada;
import com.manoel.carteiradigital.dto.response.TransacaoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransacaoConsolidadaMapper {

    @Mapping(target = "contaOrigem", source = "contaBancaria.nome")
    TransacaoResponse toResponse(TransacaoConsolidada transacao);
}
