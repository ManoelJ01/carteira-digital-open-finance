package com.manoel.carteiradigital.domain.model;

import com.manoel.carteiradigital.domain.enums.StatusConexao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conexoes_bancarias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ConexaoBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "pluggy_item_id", nullable = false, unique = true, length = 100)
    private String pluggyItemId;

    @Column(name = "pluggy_connector_id", length = 100)
    private String pluggyConnectorId;

    @Column(name = "nome_instituicao", length = 150)
    private String nomeInstituicao;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private StatusConexao status = StatusConexao.PENDENTE;

    @Column(name = "execution_status", length = 50)
    private String executionStatus;

    @Column(name = "ultima_sincronizacao")
    private LocalDateTime ultimaSincronizacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Builder.Default
    @OneToMany(mappedBy = "conexaoBancaria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContaBancaria> contas = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
