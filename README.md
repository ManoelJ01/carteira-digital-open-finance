# 💳 Carteira Digital — Agregador Multibanco (Open Finance)

API RESTful em **Java 21 + Spring Boot 3** que funciona como um **Agregador Multibanco e Carteira Unificada de Open Finance**, integrando com a **API da Pluggy** (ambiente Sandbox) para conectar múltiplas contas bancárias de um mesmo usuário, consolidando saldos, extratos e categorização financeira em um único dashboard.

---

## 🧱 Stack

Java 21 · Spring Boot 3 · Spring Data JPA/Hibernate · MySQL 8+ · Flyway · Spring Security + JWT · Spring Cloud OpenFeign · Lombok · Jakarta Bean Validation · MapStruct · JUnit 5 · Mockito · Testcontainers · Docker & Docker Compose · Springdoc OpenAPI/Swagger UI

## 📂 Arquitetura

Arquitetura em camadas (*layered*), organizada por responsabilidade dentro de `com.manoel.carteiradigital`:

```
domain/model         → Entidades JPA (Usuario, ConexaoBancaria, ContaBancaria, TransacaoConsolidada)
domain/enums         → Enums de domínio (StatusConexao, TipoConta, TipoTransacao, CategoriaTransacao)
repository           → Spring Data JPA repositories
dto/request|response → Contratos de entrada e saída da API
mapper               → MapStruct (entidade ⇄ DTO)
service              → Regras de negócio (Usuario, Auth, OpenFinance, PluggySync, Dashboard, Categorização)
client + client/dto  → Integração Feign com a API da Pluggy
controller           → Endpoints REST
security             → JWT (filtro, serviço de token, UserDetailsService)
config               → Security, Feign, OpenAPI, Scheduling
exception            → Tratamento global de exceções (RFC 7807 / Problem Details)
scheduler            → Rotina @Scheduled de sincronização em segundo plano
```

## 🔌 Endpoints principais

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/auth/registro` | Cadastro de usuário (senha com BCrypt) |
| `POST` | `/auth/login` | Login → retorna token JWT |
| `POST` | `/open-finance/connect-token` | Gera token temporário para o Pluggy Connect Widget |
| `POST` | `/open-finance/items` | Associa o `itemId` (banco autorizado) ao usuário logado |
| `GET`  | `/open-finance/items` | Lista as conexões bancárias do usuário |
| `POST` | `/open-finance/sync/{itemId}` | Sincroniza contas e transações de um item específico |
| `GET`  | `/dashboard/resumo` | Saldo consolidado, extrato unificado e gastos por categoria |

Todos os endpoints (exceto `/auth/**`) exigem o header `Authorization: Bearer <token>`.

Além disso, uma rotina `@Scheduled` (configurável em `SYNC_CRON`, padrão a cada 6h) sincroniza **todas** as conexões bancárias automaticamente em segundo plano.

---

## ✅ Pré-requisitos

- **Java 21** (JDK)
- **Maven 3.9+** (ou use a IDE)
- **Docker** e **Docker Compose** (recomendado — sobe MySQL + app com um comando)
- Uma conta gratuita no [Pluggy Dashboard](https://dashboard.pluggy.ai) para obter `CLIENT_ID` e `CLIENT_SECRET` do ambiente **Sandbox**

---

## 🚀 Como rodar — Opção 1: Docker Compose (recomendado)

Essa opção sobe o **MySQL** e a **aplicação** juntos, sem precisar instalar nada além de Docker.

1. **Clone/abra o projeto** e copie o arquivo de variáveis de ambiente:
   ```bash
   cp .env.example .env
   ```

2. **Edite o `.env`** e preencha, no mínimo:
   - `PLUGGY_CLIENT_ID` e `PLUGGY_CLIENT_SECRET` (gerados no [Pluggy Dashboard](https://dashboard.pluggy.ai), ambiente Sandbox)
   - `JWT_SECRET` (troque pelo valor sugerido por uma string aleatória própria, com pelo menos 256 bits)

3. **Suba os containers:**
   ```bash
   docker compose up --build
   ```
   Isso vai:
   - Subir o MySQL 8 em `localhost:3306`
   - Buildar a aplicação (multi-stage Docker build com Maven)
   - Rodar as *migrations* do Flyway automaticamente na inicialização
   - Expor a API em `http://localhost:8080`

4. **Pronto!** A API estará disponível e o Swagger UI em:
   ```
   http://localhost:8080/swagger-ui.html
   ```

Para parar: `docker compose down`. Para apagar também os dados do MySQL: `docker compose down -v`.

---

## 🛠️ Como rodar — Opção 2: Ambiente local (MySQL via Docker + app via Maven)

Útil durante o desenvolvimento, para ter hot-reload/debug direto na IDE.

1. **Suba apenas o MySQL** via Docker:
   ```bash
   docker compose up mysql
   ```

2. **Exporte as variáveis de ambiente** (ou configure na sua IDE — Run Configuration → Environment variables):
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/carteira_digital?useSSL=false&serverTimezone=UTC
   export SPRING_DATASOURCE_USERNAME=carteira_user
   export SPRING_DATASOURCE_PASSWORD=carteira_pass
   export JWT_SECRET=troque-esta-chave-por-uma-string-aleatoria-com-pelo-menos-256-bits
   export PLUGGY_CLIENT_ID=seu-client-id-aqui
   export PLUGGY_CLIENT_SECRET=seu-client-secret-aqui
   ```

3. **Rode a aplicação com Maven:**
   ```bash
   mvn spring-boot:run
   ```
   O Flyway aplica as *migrations* (`src/main/resources/db/migration/V1__create_tables.sql`) automaticamente ao subir.

4. Acesse `http://localhost:8080/swagger-ui.html`.

> 💡 Se preferir, gere o wrapper do Maven com `mvn -N wrapper:wrapper` para poder usar `./mvnw` em vez de depender do Maven instalado globalmente.

---

## 🔑 Obtendo credenciais da Pluggy (Sandbox)

1. Crie uma conta gratuita em https://dashboard.pluggy.ai
2. No painel, gere um par `CLIENT_ID` / `CLIENT_SECRET` do ambiente **Sandbox** (não envolve bancos reais — a Pluggy disponibiliza conectores fictícios para testes, como o "Pluggy Bank").
3. Use esses valores no `.env` (ou nas variáveis de ambiente locais).
4. Para testar o fluxo completo de conexão bancária (Pluggy Connect Widget), você precisará de um front-end simples que:
   - Chame `POST /open-finance/connect-token` para obter o `accessToken` de inicialização do widget;
   - Inicialize o [Pluggy Connect Widget](https://docs.pluggy.ai/docs/connect-widget) com esse token;
   - Após o usuário autorizar um conector de teste, capture o `itemId` retornado pelo widget;
   - Envie esse `itemId` para `POST /open-finance/items` (autenticado com o JWT do usuário).

---

## 🧪 Rodando os testes

O projeto usa **JUnit 5 + Mockito** para testes unitários e **Testcontainers** (MySQL real em container) para testes de integração ponta a ponta.

```bash
mvn test
```

> ⚠️ Os testes de integração (`*IT.java`, ex.: `AuthControllerIT`) sobem um container MySQL via Testcontainers — é necessário ter o **Docker rodando** localmente para executá-los.

Cobertura incluída:
- `CategorizacaoServiceTest` — regras de categorização das transações vindas da Pluggy
- `UsuarioServiceTest` — cadastro de usuário, criptografia de senha, validações de e-mail/CPF duplicados
- `DashboardServiceTest` — consolidação de saldo multibanco e agrupamento de gastos por categoria
- `AuthControllerIT` — fluxo ponta a ponta de registro + login com banco MySQL real (Testcontainers)

---

## 📖 Documentação da API (Swagger)

Com a aplicação rodando, acesse:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

Todos os endpoints protegidos usam autenticação **Bearer JWT** — clique em "Authorize" no Swagger e cole o token obtido em `/auth/login`.

---

## 🗃️ Modelagem do banco (Flyway)

A migration `V1__create_tables.sql` cria as 4 tabelas principais:

- **usuarios** — dados de cadastro e credenciais (senha com hash BCrypt)
- **conexoes_bancarias** — cada `item` da Pluggy vinculado a um usuário (um banco conectado)
- **contas_bancarias** — contas dentro de cada conexão (corrente, poupança, cartão de crédito etc.)
- **transacoes_consolidadas** — extrato consolidado de todas as contas, já categorizado

Novas migrations devem seguir o padrão `V2__descricao.sql`, `V3__descricao.sql` etc. — nunca edite uma migration já aplicada.

---

## ⚠️ Tratamento de erros

Todas as exceções da API seguem o padrão **RFC 7807 (Problem Details)**, com corpo padronizado:

```json
{
  "type": "https://carteira-digital.dev/erros/recurso-nao-encontrado",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Conexão bancária não encontrada para este usuário: 42",
  "instance": "/open-finance/sync/item-abc123",
  "timestamp": "2026-09-14T12:00:00Z"
}
```

---

## 🔐 Segurança

- Senhas armazenadas com **BCrypt**
- Autenticação **stateless** via **JWT** (`Spring Security` + `jjwt`)
- Credenciais da Pluggy (`CLIENT_ID`/`CLIENT_SECRET`) e o segredo do JWT nunca ficam hardcoded — vêm sempre de variáveis de ambiente (`.env` / `application.yml`)
- **Nunca** commite o arquivo `.env` (já está no `.gitignore`)

---

## 👤 Autor

Manoel Juvino dos Santos Neto — Estudante de Análise e Desenvolvimento de Sistemas (UNIT, Recife/PE)
