package com.manoel.carteiradigital.domain.model;

import com.manoel.carteiradigital.domain.enums.TipoConta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contas_bancarias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conexao_bancaria_id", nullable = false)
    private ConexaoBancaria conexaoBancaria;

    @Column(name = "pluggy_account_id", nullable = false, unique = true, length = 100)
    private String pluggyAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoConta tipo;

    @Column(length = 30)
    private String subtipo;

    @Column(length = 150)
    private String nome;

    @Column(length = 50)
    private String numero;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String moeda = "BRL";

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Builder.Default
    @OneToMany(mappedBy = "contaBancaria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransacaoConsolidada> transacoes = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void onSave() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
