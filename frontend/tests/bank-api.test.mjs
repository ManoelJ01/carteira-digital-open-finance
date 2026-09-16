import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createApi, ApiError } from '../src/api.ts';

const session = { accessToken: 'test-token', expiresAt: Date.now() + 600000 };
const json = (body, status = 200) => new Response(JSON.stringify(body), { status });

test('conexão envia autenticação, registra o item e sincroniza', async () => {
  const calls = [];
  const responses = [{ accessToken: 'connect-token' }, { pluggyItemId: 'item/1' }, { conexaoId: 1, status: 'ATIVA', contasSincronizadas: 2, transacoesNovas: 3 }];
  const api = createApi('/api', async (url, options) => {
    calls.push({ url, options });
    return json(responses.shift());
  });
  assert.equal(await api.connectToken(session), 'connect-token');
  await api.registerBank(session, 'item/1');
  assert.equal((await api.syncBank(session, 'item/1')).contasSincronizadas, 2);
  assert.deepEqual(calls.map(call => call.url), ['/api/open-finance/connect-token', '/api/open-finance/items', '/api/open-finance/sync/item%2F1']);
  assert.deepEqual(JSON.parse(calls[1].options.body), { itemId: 'item/1' });
  for (const { options } of calls) {
    assert.equal(options.method, 'POST');
    assert.equal(options.headers.Authorization, 'Bearer test-token');
  }
});

test('registro duplicado só é aceito quando o item consta na lista do usuário', async () => {
  for (const owned of [true, false]) {
    const api = createApi('/api', async (_, options) => options.method === 'POST'
      ? json({ detail: 'Item já registrado' }, 422)
      : json([{ pluggyItemId: owned ? 'item-1' : 'outro-item' }]));
    if (owned) await api.registerBank(session, 'item-1');
    else await assert.rejects(api.registerBank(session, 'item-1'), error => error instanceof ApiError && error.status === 422);
  }
});

test('token vazio e sincronização inválida ou com erro são rejeitados', async () => {
  const api = createApi('/api', async () => json({ accessToken: ' ' }));
  await assert.rejects(api.connectToken(session), ApiError);
  for (const result of [{}, { conexaoId: 1, status: 'ERRO', contasSincronizadas: 0, transacoesNovas: 0 }]) {
    await assert.rejects(createApi('/api', async () => json(result)).syncBank(session, 'item-1'), ApiError);
  }
});
