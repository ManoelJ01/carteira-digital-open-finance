package com.manoel.carteiradigital.repository;

import com.manoel.carteiradigital.domain.model.ConexaoBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConexaoBancariaRepository extends JpaRepository<ConexaoBancaria, Long> {
    Optional<ConexaoBancaria> findByPluggyItemId(String pluggyItemId);
    List<ConexaoBancaria> findByUsuarioId(Long usuarioId);
    Optional<ConexaoBancaria> findByIdAndUsuarioId(Long id, Long usuarioId);
    boolean existsByPluggyItemId(String pluggyItemId);
}
