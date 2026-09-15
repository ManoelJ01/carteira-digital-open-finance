package com.manoel.carteiradigital.domain.model;

import com.manoel.carteiradigital.domain.enums.CategoriaTransacao;
import com.manoel.carteiradigital.domain.enums.TipoTransacao;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacoes_consolidadas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class TransacaoConsolidada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_bancaria_id", nullable = false)
    private ContaBancaria contaBancaria;

    @Column(name = "pluggy_transaction_id", nullable = false, unique = true, length = 100)
    private String pluggyTransactionId;

    @Column(length = 255)
    private String descricao;

    @Column(name = "descricao_original", length = 255)
    private String descricaoOriginal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 50)
    private CategoriaTransacao categoria = CategoriaTransacao.OUTROS;

    @Column(name = "data_transacao", nullable = false)
    private LocalDate dataTransacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}
