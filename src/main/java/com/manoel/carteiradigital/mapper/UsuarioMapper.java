package com.manoel.carteiradigital.mapper;

import com.manoel.carteiradigital.domain.model.Usuario;
import com.manoel.carteiradigital.dto.response.UsuarioResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    UsuarioResponse toResponse(Usuario usuario);
}
