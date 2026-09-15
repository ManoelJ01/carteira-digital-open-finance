package com.manoel.carteiradigital.mapper;

import com.manoel.carteiradigital.domain.model.ContaBancaria;
import com.manoel.carteiradigital.dto.response.ContaResumoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContaBancariaMapper {

    @Mapping(target = "contaId", source = "id")
    @Mapping(target = "instituicao", source = "conexaoBancaria.nomeInstituicao")
    @Mapping(target = "tipo", expression = "java(contaBancaria.getTipo().name())")
    ContaResumoResponse toResumoResponse(ContaBancaria contaBancaria);
}
