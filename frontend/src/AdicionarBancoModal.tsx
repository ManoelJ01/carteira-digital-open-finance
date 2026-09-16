import { useEffect, useRef, useState } from 'react';
import { PluggyConnect } from 'react-pluggy-connect';
import { AlertCircle, Landmark, LoaderCircle, X } from 'lucide-react';
import { ApiError, type ApiClient, type Session } from './api';

type Phase = 'closed' | 'token' | 'widget' | 'register' | 'sync' | 'refresh' | 'error';
interface Props {
  open: boolean;
  api: ApiClient;
  session: Session;
  onClose: () => void;
  onConnected: (signal: AbortSignal) => Promise<void>;
  onUnauthorized: (message: string) => void;
}
const messages = {
  token: ['Preparando conexão', 'Estamos abrindo a seleção de bancos.'],
  register: ['Registrando seu banco', 'A autorização foi recebida. Estamos adicionando a conta à sua carteira.'],
  sync: ['Sincronizando sua conta', 'Estamos buscando seu saldo e suas transações. Isso pode levar alguns instantes.'],
  refresh: ['Atualizando sua carteira', 'A sincronização terminou. Estamos carregando os dados atualizados.'],
} as const;

export default function AdicionarBancoModal(props: Props) {
  const { open } = props;
  const latest = useRef(props); latest.current = props;
  const [phase, setPhase] = useState<Phase>('closed');
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const phaseRef = useRef<Phase>('closed');
  const request = useRef<AbortController | null>(null);
  const checkpoint = useRef({ itemId: '', registered: false, synced: false });
  const accepted = useRef(false);
  const opener = useRef<HTMLElement | null>(null);
  const dialog = useRef<HTMLDialogElement>(null);

  function transition(next: Phase) { phaseRef.current = next; setPhase(next); }
  function close() {
    transition('closed'); request.current?.abort(); setToken('');
    latest.current.onClose();
    opener.current?.focus({ preventScroll: true });
  }
  function fail(failure: unknown, signal?: AbortSignal) {
    if (signal?.aborted || phaseRef.current === 'closed') return;
    if (failure instanceof ApiError && [401, 403].includes(failure.status)) {
      close(); latest.current.onUnauthorized(failure.message); return;
    }
    transition('error'); setToken('');
    setError(failure instanceof Error ? failure.message : 'Não foi possível conectar o banco. Tente novamente.');
  }
  async function startToken() {
    accepted.current = false; setError(''); setToken(''); transition('token');
    request.current?.abort();
    const controller = new AbortController(); request.current = controller;
    try {
      const connectToken = await latest.current.api.connectToken(latest.current.session, controller.signal);
      controller.signal.throwIfAborted(); setToken(connectToken); transition('widget');
    } catch (failure) { fail(failure, controller.signal); }
  }
  async function finishConnection() {
    request.current?.abort();
    const controller = new AbortController(); request.current = controller;
    const { api, session } = latest.current;
    setError('');
    try {
      if (!checkpoint.current.registered) {
        transition('register');
        await api.registerBank(session, checkpoint.current.itemId, controller.signal);
        controller.signal.throwIfAborted(); checkpoint.current.registered = true;
      }
      if (!checkpoint.current.synced) {
        transition('sync');
        await api.syncBank(session, checkpoint.current.itemId, controller.signal);
        controller.signal.throwIfAborted(); checkpoint.current.synced = true;
      }
      transition('refresh');
      await latest.current.onConnected(controller.signal);
      controller.signal.throwIfAborted();
      checkpoint.current = { itemId: '', registered: false, synced: false };
      close();
    } catch (failure) { fail(failure, controller.signal); }
  }
  function success({ item }: { item: { id: string } }) {
    if (accepted.current || phaseRef.current !== 'widget') return;
    if (!item?.id?.trim()) { fail(new Error('O banco não retornou uma conexão válida. Tente novamente.')); return; }
    accepted.current = true;
    checkpoint.current = { itemId: item.id, registered: false, synced: false };
    void finishConnection();
  }
  function widgetError({ message }: { message: string }) {
    if (phaseRef.current !== 'widget' || accepted.current) return;
    fail(new Error(message || 'O banco não concluiu a conexão. Tente novamente.'));
  }

  useEffect(() => {
    if (!open) return;
    opener.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    if (checkpoint.current.itemId) void finishConnection();
    else void startToken();
    return () => { phaseRef.current = 'closed'; request.current?.abort(); };
  }, [open]);

  useEffect(() => {
    if (!open || phase === 'widget' || phase === 'closed') return;
    const element = dialog.current;
    if (element && !element.open) element.showModal();
    return () => { element?.close(); };
  }, [open, phase]);

  if (!open || phase === 'closed') return null;
  if (phase === 'widget') return <PluggyConnect
    connectToken={token}
    includeSandbox={true}
    language="pt"
    theme="light"
    onSuccess={success}
    onError={widgetError}
    onLoadError={failure => widgetError({ message: failure.message || 'Não foi possível abrir a seleção de bancos. Verifique sua conexão.' })}
    onClose={() => { if (phaseRef.current === 'widget' && !accepted.current) close(); }}
  />;

  const busy = phase !== 'error';
  const [title, description] = busy ? messages[phase] : ['Não foi possível concluir', error];
  return <dialog ref={dialog} className="bank-dialog" aria-labelledby="bank-dialog-title" aria-describedby="bank-dialog-description" onCancel={event => { event.preventDefault(); close(); }}>
    <div className="bank-dialog-heading"><span className="bank-dialog-icon"><Landmark size={25} /></span><button className="text-button icon-button" onClick={close} aria-label="Fechar conexão de banco"><X size={22} /></button></div>
    <span className="eyebrow">ADICIONAR BANCO · SANDBOX</span>
    <div role={busy ? 'status' : 'alert'} aria-live="polite">
      <h2 id="bank-dialog-title">{title}</h2>
      <p id="bank-dialog-description">{description}</p>
      {busy ? <LoaderCircle className="spinner bank-loading" size={32} /> : <AlertCircle className="bank-error-icon" size={28} />}
    </div>
    {!busy && <button className="primary-button" onClick={() => { if (checkpoint.current.itemId) void finishConnection(); else void startToken(); }}>{checkpoint.current.synced ? 'Atualizar carteira novamente' : checkpoint.current.registered ? 'Tentar sincronizar novamente' : 'Tentar novamente'}</button>}
    {busy && checkpoint.current.itemId && <p className="bank-dialog-hint">Se fechar esta tela, você pode retomar em “Adicionar Banco”.</p>}
    <button className="text-button" onClick={close}>{busy ? 'Fechar' : 'Voltar para a carteira'}</button>
  </dialog>;
}
