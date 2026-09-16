# Nexo — Carteira Digital com Open Finance

Aplicação full stack para reunir contas de diferentes bancos em uma carteira digital. O Nexo apresenta saldo consolidado, contas conectadas, extrato unificado e gastos por categoria em uma interface responsiva. Os dados bancários são importados pela integração com a Pluggy e armazenados pelo backend.

## Funcionalidades

- Cadastro de usuários pela API e login com autenticação JWT.
- Dashboard com saldo consolidado, entradas, saídas e últimas movimentações.
- Seção **Minhas contas** com saldos por banco e botão **Adicionar Banco**.
- Autorização pelo Pluggy Connect, registro da conexão e sincronização de contas e transações.
- Extrato com busca e filtros por período, banco e tipo de movimentação.
- Exportação do extrato filtrado em CSV e opção de ocultar valores.
- Categorização de transações e sincronização periódica no backend.
- Interface adaptada para desktop e celular, com tema roxo.

O widget inclui bancos Sandbox: nesse ambiente, os dados fornecidos pela Pluggy podem ser de teste. A conexão depende de credenciais válidas e da autorização no widget.

## Stack utilizada

| Camada | Tecnologias |
| --- | --- |
| Frontend | React 19, TypeScript, Vite 6, Tailwind CSS 4 e CSS personalizado |
| Interface e conexão bancária | Lucide React e React Pluggy Connect |
| Backend | Java 21, Spring Boot 3.3, Spring Web e Bean Validation |
| Autenticação | Spring Security e JWT com JJWT |
| Persistência | Spring Data JPA, Hibernate, MySQL e migrations Flyway |
| Integração externa | Pluggy e Spring Cloud OpenFeign |
| Mapeamento e produtividade | MapStruct, Lombok e Maven |
| Documentação da API | OpenAPI e Swagger UI com Springdoc |
| Infraestrutura local | Docker e Docker Compose |
| Testes | JUnit, Spring Boot Test, Spring Security Test, H2, Testcontainers, Node Test Runner e Playwright |

## Arquitetura

O frontend consome a API REST autenticada com JWT. O backend aplica as regras de negócio, consulta a Pluggy e persiste os dados no MySQL. O dashboard lê os dados armazenados; atualizar sua visualização não dispara uma nova sincronização bancária.

```text
frontend/                   Interface React e testes de navegador
src/main/java/com/manoel/carteiradigital/
  controller/               Endpoints REST
  service/                  Regras de negócio e integração bancária
  client/                   Cliente HTTP da Pluggy
  domain/                   Entidades e enums
  repository/               Acesso ao banco de dados
  security/                 Autenticação e validação de JWT
  scheduler/                Sincronização agendada
src/main/resources/
  db/migration/             Migrations do banco
src/test/                   Testes do backend
docker-compose.yml          Serviços MySQL e backend
```

## Como executar localmente

Tenha Docker com Compose, Node.js e npm instalados. Para executar o backend fora do Docker, também são necessários JDK 21 e Maven.

### 1. Configure o ambiente

```bash
git clone https://github.com/ManoelJ01/carteira-digital-open-finance.git
cd carteira-digital-open-finance
```

Copie `.env.example` para `.env`. Configure as senhas do banco e `JWT_SECRET` com uma chave aleatória de pelo menos 256 bits. Para conectar bancos, preencha `PLUGGY_CLIENT_ID` e `PLUGGY_CLIENT_SECRET`. Sem credenciais Pluggy, deixe esses campos vazios: cadastro e login continuam disponíveis, mas não será possível conectar bancos.

O arquivo `.env` é usado pelo Docker Compose e não é versionado. As credenciais privadas da Pluggy ficam exclusivamente no backend.

### 2. Inicie o banco e a API

```bash
docker compose up --build -d
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- MySQL para acesso pelo computador: `localhost:3308`

### 3. Inicie o frontend

```bash
cd frontend
npm ci
npm run dev
```

Abra o endereço informado pelo Vite, normalmente `http://localhost:5173`. O proxy encaminha `/api` para `http://localhost:8080`. Para alterar o destino, copie `frontend/.env.example` para `frontend/.env.local` e ajuste `API_PROXY_TARGET`.

### 4. Cadastre-se e conecte um banco

No Swagger, use `POST /auth/registro` com os campos `nome`, `email`, `cpf` (11 dígitos) e `senha` (mínimo de 8 caracteres). Depois, faça login no frontend e abra **Minhas contas → Adicionar Banco**. Conclua a autorização no Pluggy Connect; a aplicação registra o item, sincroniza os dados e atualiza o dashboard.

Em caso de erro, o diálogo oferece uma nova tentativa. A retomada de uma conexão interrompida é mantida enquanto o dashboard continuar montado; recarregar a página descarta esse estado local.

## Testes e build

Frontend, dentro de `frontend/`:

```bash
npm test
npm run test:ui
npm run build
```

Os testes de interface usam Microsoft Edge no Windows. Em outros sistemas, instale o Chromium com `npx playwright install chromium`. Os testes usam respostas isoladas; não autorizam conexões bancárias reais.

Backend, na raiz, com JDK 21 e Maven:

```bash
mvn test
mvn clean package
```

Os testes padrão incluem serviços e API com H2 em memória. O teste `AuthControllerIT` usa MySQL via Testcontainers, precisa de Docker e pode ser executado explicitamente:

```bash
mvn -Dtest=AuthControllerIT test
```

## Publicação do frontend

O build gera `frontend/dist`. Configure o servidor web para encaminhar `/api/` ao backend, removendo o prefixo `/api`. O proxy do Vite não faz parte dos arquivos publicados. Use HTTPS e configure segredos por variáveis de ambiente. Veja também [o README do frontend](frontend/README.md).

## Autor

Manoel Juvino dos Santos Neto — [ManoelJ01](https://github.com/ManoelJ01).
