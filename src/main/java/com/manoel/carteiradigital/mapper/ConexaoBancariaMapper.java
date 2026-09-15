package com.manoel.carteiradigital.mapper;

import com.manoel.carteiradigital.domain.model.ConexaoBancaria;
import com.manoel.carteiradigital.dto.response.ConexaoBancariaResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConexaoBancariaMapper {
    ConexaoBancariaResponse toResponse(ConexaoBancaria conexaoBancaria);
}
