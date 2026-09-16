import { adaptDashboard, type DashboardResponse } from './data.ts';

export interface Session { accessToken: string; expiresAt: number }
export interface BankConnection { id: number; pluggyItemId: string; nomeInstituicao?: string; status: string }
export interface SyncResult { conexaoId: number; status: string; contasSincronizadas: number; transacoesNovas: number }
export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) { super(message); this.name = 'ApiError'; this.status = status; }
}

export function createApi(baseUrl = '/api', fetcher: typeof fetch = fetch) {
  const base = baseUrl.replace(/\/$/, '');
  async function request(path: string, options: RequestInit, token?: string, timeoutMs = 15000): Promise<unknown> {
    const timeoutSignal = AbortSignal.timeout(timeoutMs);
    const signal = options.signal ? AbortSignal.any([options.signal, timeoutSignal]) : timeoutSignal;
    let response: Response;
    try {
      response = await fetcher(`${base}${path}`, { ...options, signal, cache: 'no-store', headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) } });
    } catch (error) {
      if (options.signal?.aborted) throw error;
      throw new ApiError(timeoutSignal.aborted ? 'O servidor demorou para responder. Tente novamente.' : 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.', 0);
    }
    let body: unknown;
    try { body = await response.json(); } catch {
      if (options.signal?.aborted) throw new DOMException('Requisição cancelada', 'AbortError');
      if (timeoutSignal.aborted) throw new ApiError('O servidor demorou para responder. Tente novamente.', 0);
      body = null;
    }
    if (!response.ok) {
      let message = 'Não foi possível carregar os dados. Tente novamente.';
      if (response.status === 401 || response.status === 403) message = token ? 'Sua sessão expirou. Entre novamente.' : 'E-mail ou senha inválidos.';
      else if (response.status === 502 || response.status === 503 || response.status === 504) message = 'O servidor está indisponível. Tente novamente em instantes.';
      else if (body && typeof body === 'object' && 'detail' in body && typeof body.detail === 'string') message = body.detail;
      throw new ApiError(message, response.status);
    }
    if (!body) throw new ApiError('O servidor retornou uma resposta inválida. Verifique a configuração da API.', response.status);
    return body;
  }
  function requireSession(session: Session) {
    if (session.expiresAt <= Date.now()) throw new ApiError('Sua sessão expirou. Entre novamente.', 401);
  }
  function requireItemId(itemId: string) {
    if (!itemId?.trim()) throw new ApiError('A conexão não retornou uma identificação válida. Tente conectar novamente.', 0);
  }
  return {
    async login(email: string, senha: string, signal?: AbortSignal): Promise<Session> {
      const body = await request('/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }), signal });
      if (typeof body !== 'object' || !body || !('accessToken' in body) || typeof body.accessToken !== 'string' || !body.accessToken || !('tokenType' in body) || body.tokenType !== 'Bearer' || !('expiresInMs' in body) || typeof body.expiresInMs !== 'number' || !Number.isFinite(body.expiresInMs) || body.expiresInMs <= 0) throw new ApiError('O servidor não retornou uma sessão válida.', 200);
      return { accessToken: body.accessToken, expiresAt: Date.now() + body.expiresInMs };
    },
    async dashboard(session: Session, signal?: AbortSignal) {
      requireSession(session);
      return adaptDashboard(await request('/dashboard/resumo', { method: 'GET', signal }, session.accessToken) as DashboardResponse);
    },
    async connectToken(session: Session, signal?: AbortSignal): Promise<string> {
      requireSession(session);
      const body = await request('/open-finance/connect-token', { method: 'POST', signal }, session.accessToken);
      if (!body || typeof body !== 'object' || !('accessToken' in body) || typeof body.accessToken !== 'string' || !body.accessToken.trim()) throw new ApiError('Não foi possível preparar a conexão com o banco. Tente novamente.', 200);
      return body.accessToken;
    },
    async registerBank(session: Session, itemId: string, signal?: AbortSignal): Promise<void> {
      requireSession(session); requireItemId(itemId);
      try {
        const body = await request('/open-finance/items', { method: 'POST', body: JSON.stringify({ itemId }), signal }, session.accessToken) as BankConnection;
        if (body.pluggyItemId !== itemId) throw new ApiError('O servidor não confirmou o registro da conexão. Tente novamente.', 200);
      } catch (error) {
        // Uma resposta anterior pode ter se perdido depois de o servidor salvar o item.
        // Só reconciliamos com a lista do próprio usuário, nunca com itens de outra conta.
        if (!(error instanceof ApiError) || error.status !== 422 || signal?.aborted) throw error;
        const existing = await request('/open-finance/items', { method: 'GET', signal }, session.accessToken);
        if (!Array.isArray(existing) || !existing.some((connection: BankConnection) => connection?.pluggyItemId === itemId)) throw error;
      }
    },
    async syncBank(session: Session, itemId: string, signal?: AbortSignal): Promise<SyncResult> {
      requireSession(session); requireItemId(itemId);
      const result = await request(`/open-finance/sync/${encodeURIComponent(itemId)}`, { method: 'POST', signal }, session.accessToken, 120000) as SyncResult;
      if (!Number.isSafeInteger(result.conexaoId) || result.conexaoId <= 0 || !Number.isSafeInteger(result.contasSincronizadas) || result.contasSincronizadas < 0 || !Number.isSafeInteger(result.transacoesNovas) || result.transacoesNovas < 0 || typeof result.status !== 'string') throw new ApiError('Não foi possível confirmar a sincronização. Tente novamente.', 200);
      if (['ERRO', 'LOGIN_ERROR', 'OUTDATED'].includes(result.status)) throw new ApiError('O banco não concluiu a atualização dos dados. Tente sincronizar novamente.', 200);
      return result;
    },
  };
}

export type ApiClient = ReturnType<typeof createApi>;
