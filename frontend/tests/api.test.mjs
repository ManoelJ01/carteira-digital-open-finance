import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createApi, ApiError } from '../src/api.ts';
import { adaptDashboard, availableMonths, filterTransactions, exportRows } from '../src/data.ts';

// Fixtures exclusivas dos testes; nunca importadas pelo aplicativo.
const response = () => ({
  saldoConsolidado: 150.25,
  usuario: { id: 1, nome: 'Pessoa de teste' },
  contas: [
    { contaId: 10, instituicao: 'Banco A', nome: 'Conta corrente', tipo: 'BANK', saldo: 100, moeda: 'BRL' },
    { contaId: 20, instituicao: 'Banco B', nome: 'Conta corrente', tipo: 'BANK', saldo: 50.25, moeda: 'BRL' },
  ],
  extratoUnificado: [
    { id: 1, contaId: 10, contaOrigem: 'Conta corrente', descricao: 'Compra', valor: 10.25, tipo: 'DEBITO', categoria: 'ALIMENTACAO', dataTransacao: '2025-12-20' },
    { id: 2, contaId: 20, contaOrigem: 'Conta corrente', descricao: 'Recebimento', valor: 100, tipo: 'CREDITO', categoria: 'RENDA', dataTransacao: '2026-01-10' },
  ],
  gastosPorCategoria: [{ categoria: 'ALIMENTACAO', total: 10.25 }],
});
const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
const session = () => ({ accessToken: 'token-exclusivo-do-teste', expiresAt: Date.now() + 60000 });

test('login envia email/senha; dashboard envia Bearer e usa o saldo da API', async () => {
  const calls = [];
  const api = createApi('/api/', async (url, options) => {
    calls.push({ url, options });
    return url.endsWith('/login') ? json({ accessToken: 'token-teste', tokenType: 'Bearer', expiresInMs: 60000 }) : json(response());
  });
  const login = await api.login('teste@example.com', 'senha-teste');
  const data = await api.dashboard(login);
  assert.equal(calls[0].url, '/api/auth/login');
  assert.deepEqual(JSON.parse(calls[0].options.body), { email: 'teste@example.com', senha: 'senha-teste' });
  assert.equal(calls[0].options.headers.Authorization, undefined);
  assert.equal(calls[1].url, '/api/dashboard/resumo');
  assert.equal(calls[1].options.headers.Authorization, 'Bearer token-teste');
  assert.equal(calls[1].options.cache, 'no-store');
  assert.equal(data.total, 150.25);
  assert.equal(data.user.name, 'Pessoa de teste');
});

test('nomes de conta repetidos não misturam bancos; débitos absolutos viram saídas', () => {
  const data = adaptDashboard(response());
  assert.deepEqual(data.transactions.map(t => t.id), ['2', '1']);
  assert.equal(data.transactions[1].amount, -10.25);
  assert.equal(data.transactions[1].type, 'expense');
  assert.deepEqual(availableMonths(data.transactions), ['2026-01', '2025-12']);
  assert.deepEqual(filterTransactions(data.transactions, '2025-12', '10', 'expense', 'alimentacao').map(t => t.id), ['1']);
  assert.equal(filterTransactions(data.transactions, 'all', '20', 'expense', '').length, 0);
});

test('resposta vazia válida continua vazia; resposta incompleta não vira saldo zero', () => {
  const data = adaptDashboard({ ...response(), saldoConsolidado: 0, contas: [], extratoUnificado: [], gastosPorCategoria: [] });
  assert.equal(data.total, 0);
  assert.deepEqual(data.accounts, []);
  assert.deepEqual(data.transactions, []);
  assert.throws(() => adaptDashboard({}), /incompleta/);
  const invalid = response(); delete invalid.extratoUnificado[0].contaId;
  assert.throws(() => adaptDashboard(invalid), /incompleta/);
  const mixed = response(); mixed.contas[1].moeda = 'USD';
  assert.throws(() => adaptDashboard(mixed), /moedas diferentes/);
});

test('401 e 403 exigem novo login, sem resposta financeira substituta', async () => {
  for (const status of [401, 403]) {
    const api = createApi('/api', async () => new Response('', { status }));
    await assert.rejects(api.dashboard(session()), error => error instanceof ApiError && error.status === status && /sessão expirou/.test(error.message));
  }
});

test('sessão vencida não envia requisição', async () => {
  let called = false;
  const api = createApi('/api', async () => { called = true; return json(response()); });
  await assert.rejects(api.dashboard({ ...session(), expiresAt: 0 }), error => error.status === 401);
  assert.equal(called, false);
});

test('erros de rede, ProblemDetail e HTML não são tratados como sucesso', async () => {
  await assert.rejects(createApi('/api', async () => { throw new TypeError('fetch failed'); }).dashboard(session()), /conectar ao servidor/);
  await assert.rejects(createApi('/api', async () => json({ detail: 'Falha de leitura' }, 500)).dashboard(session()), /Falha de leitura/);
  await assert.rejects(createApi('/api', async () => new Response('<html></html>')).dashboard(session()), /resposta inválida/);
  await assert.rejects(createApi('/api', async () => json({ accessToken: '', tokenType: 'Bearer', expiresInMs: 0 })).login('a@b.com', 'x'), /sessão válida/);
});

test('cancelamento de requisição é preservado', async () => {
  const controller = new AbortController(); controller.abort();
  const api = createApi('/api', async (_url, options) => { options.signal.throwIfAborted(); return json(response()); });
  await assert.rejects(api.dashboard(session(), controller.signal), { name: 'AbortError' });
});

test('CSV usa banco pelo ID, protege fórmulas e mantém débito numérico', () => {
  const raw = response(); raw.extratoUnificado[0].descricao = '=HYPERLINK("url")';
  const data = adaptDashboard(raw);
  const csv = exportRows(filterTransactions(data.transactions, 'all', '10', 'all', ''), data.accounts, data.currency);
  assert.ok(csv.includes('Banco A'));
  assert.ok(!csv.includes('Banco B'));
  assert.ok(csv.includes("'=HYPERLINK"));
  assert.ok(csv.includes('"-10,25"'));
  assert.ok(csv.includes('"BRL"'));
});
