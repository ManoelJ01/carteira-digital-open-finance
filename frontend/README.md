# Front-end da carteira digital

Requer Node.js e npm. Na pasta `frontend`, execute `npm ci` e `npm run dev`.
Configure `API_PROXY_TARGET` em `.env.local` com o endereço do backend (padrão: `http://localhost:8080`). O navegador usa `VITE_API_BASE_URL=/api`.

O login usa a API Spring Boot. **Adicionar Banco** abre o Pluggy Connect com bancos Sandbox habilitados, registra a conexão autorizada e sincroniza os dados. Configure as credenciais da Pluggy somente no backend.

Validação: `npm test`, `npm run build` e `npm run test:ui`. No Windows, os testes de interface usam Microsoft Edge. Nos demais sistemas, instale o navegador com `npx playwright install chromium`.

Para publicar, sirva `dist` após `npm run build` e configure o servidor web para encaminhar `/api/` ao backend, removendo o prefixo `/api`. Por exemplo, `/api/auth/login` deve chegar ao Spring como `/auth/login`. O proxy do Vite funciona apenas no desenvolvimento e no preview. Use HTTPS na publicação; variáveis `VITE_*` são incorporadas ao código público e não devem conter segredos.
