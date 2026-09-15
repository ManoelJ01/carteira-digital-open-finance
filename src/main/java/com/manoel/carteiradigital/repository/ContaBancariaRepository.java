package com.manoel.carteiradigital.repository;

import com.manoel.carteiradigital.domain.model.ContaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {
    Optional<ContaBancaria> findByPluggyAccountId(String pluggyAccountId);
    List<ContaBancaria> findByConexaoBancariaId(Long conexaoBancariaId);

    @org.springframework.data.jpa.repository.Query("""
            select c from ContaBancaria c
            join c.conexaoBancaria cb
            where cb.usuario.id = :usuarioId
            """)
    List<ContaBancaria> findAllByUsuarioId(Long usuarioId);
}
