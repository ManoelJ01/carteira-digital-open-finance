-- ============================================================
-- V1__create_tables.sql
-- Modelagem inicial do banco "Carteira Digital"
-- ============================================================

CREATE TABLE usuarios (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(150)        NOT NULL,
    email           VARCHAR(150)        NOT NULL,
    cpf             VARCHAR(11)         NOT NULL,
    senha           VARCHAR(255)        NOT NULL,
    ativo           BOOLEAN             NOT NULL DEFAULT TRUE,
    criado_em       DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_cpf UNIQUE (cpf)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE conexoes_bancarias (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id          BIGINT              NOT NULL,
    pluggy_item_id      VARCHAR(100)        NOT NULL,
    pluggy_connector_id VARCHAR(100),
    nome_instituicao    VARCHAR(150),
    status              VARCHAR(30)         NOT NULL DEFAULT 'PENDENTE',
    execution_status     VARCHAR(50),
    ultima_sincronizacao DATETIME,
    criado_em           DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_conexoes_pluggy_item_id UNIQUE (pluggy_item_id),
    CONSTRAINT fk_conexoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE contas_bancarias (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    conexao_bancaria_id BIGINT              NOT NULL,
    pluggy_account_id   VARCHAR(100)        NOT NULL,
    tipo                VARCHAR(30)         NOT NULL,
    subtipo             VARCHAR(30),
    nome                VARCHAR(150),
    numero              VARCHAR(50),
    saldo               DECIMAL(15,2)       NOT NULL DEFAULT 0.00,
    moeda               VARCHAR(10)         NOT NULL DEFAULT 'BRL',
    atualizado_em       DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_contas_pluggy_account_id UNIQUE (pluggy_account_id),
    CONSTRAINT fk_contas_conexao FOREIGN KEY (conexao_bancaria_id) REFERENCES conexoes_bancarias (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE transacoes_consolidadas (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    conta_bancaria_id   BIGINT              NOT NULL,
    pluggy_transaction_id VARCHAR(100)      NOT NULL,
    descricao           VARCHAR(255),
    descricao_original  VARCHAR(255),
    valor               DECIMAL(15,2)       NOT NULL,
    tipo                VARCHAR(20)         NOT NULL, -- CREDITO / DEBITO
    categoria           VARCHAR(50)         NOT NULL DEFAULT 'OUTROS',
    data_transacao       DATE                NOT NULL,
    criado_em           DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_transacoes_pluggy_transaction_id UNIQUE (pluggy_transaction_id),
    CONSTRAINT fk_transacoes_conta FOREIGN KEY (conta_bancaria_id) REFERENCES contas_bancarias (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_conexoes_usuario_id ON conexoes_bancarias (usuario_id);
CREATE INDEX idx_contas_conexao_id ON contas_bancarias (conexao_bancaria_id);
CREATE INDEX idx_transacoes_conta_id ON transacoes_consolidadas (conta_bancaria_id);
CREATE INDEX idx_transacoes_data ON transacoes_consolidadas (data_transacao);
CREATE INDEX idx_transacoes_categoria ON transacoes_consolidadas (categoria);
