package com.manoel.carteiradigital.repository;

import com.manoel.carteiradigital.domain.model.TransacaoConsolidada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransacaoConsolidadaRepository extends JpaRepository<TransacaoConsolidada, Long> {

    Optional<TransacaoConsolidada> findByPluggyTransactionId(String pluggyTransactionId);

    boolean existsByPluggyTransactionId(String pluggyTransactionId);

    @Query("""
            select t from TransacaoConsolidada t
            join t.contaBancaria c
            join c.conexaoBancaria cb
            where cb.usuario.id = :usuarioId
            order by t.dataTransacao desc
            """)
    List<TransacaoConsolidada> findExtratoUnificado(Long usuarioId);

    @Query("""
            select t from TransacaoConsolidada t
            join t.contaBancaria c
            join c.conexaoBancaria cb
            where cb.usuario.id = :usuarioId
              and t.dataTransacao between :inicio and :fim
            order by t.dataTransacao desc
            """)
    List<TransacaoConsolidada> findExtratoUnificadoPorPeriodo(Long usuarioId, LocalDate inicio, LocalDate fim);
}
