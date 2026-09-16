import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { AlertCircle, ArrowRight, Layers3, LoaderCircle, LogOut, RefreshCw, ShieldCheck } from 'lucide-react';
import Dashboard from './Dashboard';
import AdicionarBancoModal from './AdicionarBancoModal';
import { ApiError, createApi, type Session } from './api';
import type { DashboardData } from './data';

const api = createApi(import.meta.env.VITE_API_BASE_URL || '/api');

export default function App() {
  // O token permanece somente em memória. Recarregar a página exige novo login.
  const [session, setSession] = useState<Session | null>(null);
  const [notice, setNotice] = useState('');
  const logout = useCallback((message = '') => { setSession(null); setNotice(message); }, []);
  useEffect(() => {
    if (!session) return;
    const timer = window.setTimeout(() => logout('Sua sessão expirou. Entre novamente.'), Math.min(Math.max(0, session.expiresAt - Date.now()), 2_147_483_647));
    return () => window.clearTimeout(timer);
  }, [session, logout]);
  return session ? <ConnectedDashboard session={session} onLogout={logout} /> : <Login notice={notice} onLogin={value => { setNotice(''); setSession(value); }} />;
}

function Login({ notice, onLogin }: { notice: string; onLogin: (session: Session) => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const request = useRef<AbortController | null>(null);
  useEffect(() => () => request.current?.abort(), []);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (request.current) return;
    const controller = new AbortController(); request.current = controller;
    setLoading(true); setError('');
    try { const session = await api.login(email.trim(), password, controller.signal); if (!controller.signal.aborted) onLogin(session); }
    catch (failure) { if (!controller.signal.aborted) setError(failure instanceof Error ? failure.message : 'Não foi possível entrar. Tente novamente.'); }
    finally { request.current = null; if (!controller.signal.aborted) setLoading(false); }
  }
  return <main className="auth-page"><section className="auth-card"><a href="/" className="brand"><span className="brand-symbol"><Layers3 size={25} /></span>nexo<span className="brand-dot">.</span></a><div className="auth-heading"><span className="eyebrow">SUA VIDA FINANCEIRA CONECTADA</span><h1>Bem-vindo de volta.</h1><p>Entre para acompanhar suas contas em um só lugar.</p></div>{(error || notice) && <div className="status-message error" role="alert"><AlertCircle size={18} /><span>{error || notice}</span></div>}<form onSubmit={submit} aria-busy={loading}><label htmlFor="email">E-mail</label><input id="email" type="email" autoComplete="username" required value={email} onChange={event => setEmail(event.target.value)} disabled={loading} /><label htmlFor="password">Senha</label><input id="password" type="password" autoComplete="current-password" required value={password} onChange={event => setPassword(event.target.value)} disabled={loading} /><button className="primary-button" type="submit" disabled={loading}>{loading ? <><LoaderCircle className="spinner" size={18} />Entrando…</> : <>Entrar na minha carteira <ArrowRight size={18} /></>}</button></form><p className="auth-footer"><ShieldCheck size={16} />Use o e-mail e a senha da sua conta cadastrada.</p></section></main>;
}

function ConnectedDashboard({ session, onLogout }: { session: Session; onLogout: (message?: string) => void }) {
  const [addingBank, setAddingBank] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [hidden, setHidden] = useState(false);
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [reload, setReload] = useState(0);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError(''); setData(null);
    api.dashboard(session, controller.signal).then(result => {
      if (controller.signal.aborted) return;
      setData(result); setUpdatedAt(new Date());
    }).catch(failure => {
      if (controller.signal.aborted) return;
      if (failure instanceof ApiError && (failure.status === 401 || failure.status === 403)) { onLogout(failure.message); return; }
      setError(failure instanceof Error ? failure.message : 'Não foi possível carregar sua carteira.');
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [session, reload, onLogout]);
  if (loading || !data) return <main className="connection-state"><span className="brand-symbol"><Layers3 size={26} /></span>{loading ? <div role="status"><LoaderCircle className="spinner" size={30} /><h1>Carregando sua carteira</h1><p>Consultando suas contas e movimentações.</p></div> : <div role="alert"><AlertCircle size={30} /><h1>Não foi possível carregar</h1><p>{error}</p><button className="primary-button" onClick={() => setReload(value => value + 1)}><RefreshCw size={17} />Tentar novamente</button></div>}<button className="text-button" onClick={() => onLogout()}><LogOut size={16} />Sair da conta</button></main>;
  async function bankConnected(signal: AbortSignal) {
    const result = await api.dashboard(session, signal);
    signal.throwIfAborted();
    setData(result); setUpdatedAt(new Date());
    setSuccessMessage('Banco conectado e sincronizado. Sua carteira está atualizada.');
    window.location.hash = 'accounts';
  }
  return <>
    <Dashboard data={data} updatedAt={updatedAt!} onRefresh={() => { setSuccessMessage(''); setReload(value => value + 1); }} onLogout={() => onLogout()} hidden={hidden} setHidden={setHidden} onAddBank={() => { setSuccessMessage(''); setAddingBank(true); }} successMessage={successMessage} />
    <AdicionarBancoModal open={addingBank} api={api} session={session} onClose={() => setAddingBank(false)} onConnected={bankConnected} onUnauthorized={onLogout} />
  </>;
}
