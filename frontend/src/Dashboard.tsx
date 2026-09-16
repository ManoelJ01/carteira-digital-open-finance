import { useEffect, useRef, useState } from 'react';
import { ArrowDownLeft, ArrowUpRight, ArrowRight, Wallet, LayoutGrid, Landmark, ListOrdered, Search, Eye, EyeOff, ChevronDown, ShieldCheck, Download, Coffee, ShoppingBag, Car, Music2, House, Layers3, RefreshCw, LogOut, Plus, CheckCircle2 } from 'lucide-react';
import { greeting, initials, money, availableMonths, periodLabel, filterTransactions, exportRows, type DashboardData, type Account, type Transaction } from './data';

const sections = [
  { id: 'overview', label: 'Visão geral', Icon: LayoutGrid },
  { id: 'accounts', label: 'Minhas contas', Icon: Landmark },
  { id: 'transactions', label: 'Transações', Icon: ListOrdered },
] as const;
type Section = typeof sections[number]['id'];
const currentSection = (): Section => sections.find(section => section.id === window.location.hash.slice(1))?.id || 'overview';
const categoryIcons = { Transporte: Car, Alimentação: Coffee, Serviços: Music2, Moradia: House, Compras: ShoppingBag };
const categoryColor = (index: number) => `var(--color-chart-${index % 5 + 1})`;

function BankIcon({ account, small = false }: { account: Account; small?: boolean }) {
  return <span className={`bank-icon ${small ? 'small' : ''}`} style={{ background: account.color }} aria-label={account.bank} title={account.bank}>{account.mark}</span>;
}

function TransactionList({ transactions, accounts, hidden, currency }: { transactions: Transaction[]; accounts: Account[]; hidden: boolean; currency: string }) {
  return <div className="transaction-list">
    {transactions.map((transaction, index) => {
      const account = accounts.find(value => value.id === transaction.accountId);
      const Icon = transaction.type === 'income' ? ArrowDownLeft : categoryIcons[transaction.category as keyof typeof categoryIcons] || ShoppingBag;
      const showDate = index === 0 || transactions[index - 1].date !== transaction.date;
      return <div key={transaction.id}>
        {showDate && <div className="date-divider"><time dateTime={transaction.date}>{new Date(`${transaction.date}T12:00:00`).toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' })}</time></div>}
        <div className="transaction-row">
          <span className={`transaction-icon ${transaction.type === 'income' ? 'income-bg' : ''}`}><Icon size={21} /></span>
          <div className="transaction-description"><strong>{transaction.description}</strong><span>{transaction.category}</span></div>
          {account && <span className="transaction-bank"><BankIcon account={account} small /><span>{account.bank}</span></span>}
          <strong className={`transaction-amount ${transaction.type}`}><span className="sr-only">{transaction.type === 'income' ? 'Entrada: ' : 'Saída: '}</span>{hidden ? '••••••' : `${transaction.type === 'income' ? '+' : '−'} ${money(Math.abs(transaction.amount), currency)}`}</strong>
        </div>
      </div>;
    })}
    {!transactions.length && <div className="empty-state"><Search size={28} /><strong>Nenhuma transação encontrada</strong><p>Experimente outro período ou ajuste os filtros.</p></div>}
  </div>;
}

interface DashboardProps {
  data: DashboardData;
  updatedAt: Date;
  onRefresh: () => void;
  onLogout: () => void;
  hidden: boolean;
  setHidden: (value: boolean) => void;
  onAddBank: () => void;
  successMessage: string;
}

export default function Dashboard({ data, updatedAt, onRefresh, onLogout, hidden, setHidden, onAddBank, successMessage }: DashboardProps) {
  const [section, setSection] = useState<Section>(currentSection);
  const [bank, setBank] = useState('all');
  const [query, setQuery] = useState('');
  const [type, setType] = useState('all');
  const [month, setMonth] = useState('all');
  const [exported, setExported] = useState(false);
  const content = useRef<HTMLElement>(null);
  const heading = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    const onHashChange = () => setSection(currentSection());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);
  useEffect(() => {
    if (content.current) content.current.scrollTop = 0;
    heading.current?.focus({ preventScroll: true });
    document.title = `${sections.find(value => value.id === section)!.label} — Nexo`;
  }, [section]);

  const accounts = data.accounts;
  const monthly = filterTransactions(data.transactions, month, 'all', 'all', '');
  const income = monthly.filter(t => t.type === 'income').reduce((sum, t) => sum + Math.round(t.amount * 100), 0) / 100;
  const expenses = monthly.filter(t => t.type === 'expense').reduce((sum, t) => sum + Math.round(Math.abs(t.amount) * 100), 0) / 100;
  const filtered = filterTransactions(data.transactions, month, bank, type, query);
  const display = (value: number) => hidden ? '••••••' : money(value, data.currency);
  const categories = month === 'all' ? data.categories : Object.entries(monthly.filter(t => t.type === 'expense').reduce<Record<string, number>>((result, t) => {
    result[t.category] = (result[t.category] || 0) + Math.round(Math.abs(t.amount) * 100); return result;
  }, {})).map(([category, cents]): [string, number] => [category, cents / 100]).sort((a, b) => b[1] - a[1]);
  const categoryTotal = categories.reduce((sum, [, value]) => sum + Math.round(value * 100), 0) / 100;
  const sectionLabel = sections.find(value => value.id === section)!.label;

  function navigate(next: Section) { window.location.hash = next; }
  function viewAccount(id: string) {
    setBank(id); setQuery(''); setType('all'); setMonth('all'); setExported(false);
    navigate('transactions');
  }
  function exportCsv() {
    if (hidden || !filtered.length) return;
    const url = URL.createObjectURL(new Blob([exportRows(filtered, accounts, data.currency)], { type: 'text/csv;charset=utf-8;' }));
    const anchor = document.createElement('a');
    anchor.href = url; anchor.download = `nexo-extrato-${month}.csv`; anchor.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000); setExported(true);
  }

  return <div className="app-shell">
    <a className="skip-link" href="#main-content" onClick={event => { event.preventDefault(); content.current?.focus({ preventScroll: true }); }}>Ir para o conteúdo</a>
    <aside className="sidebar">
      <a className="brand" href="#overview" aria-label="Nexo, início"><span className="brand-symbol"><Layers3 size={25} /></span>nexo<span className="brand-dot">.</span></a>
      <div className="workspace"><span className="workspace-icon"><Wallet size={22} /></span><div><strong>Minha carteira</strong><small>Seu espaço financeiro</small></div></div>
      <span className="nav-caption">PRINCIPAL</span>
      <nav aria-label="Navegação principal">{sections.map(({ id, label, Icon }) => <a key={id} href={`#${id}`} className={`nav-item ${section === id ? 'active' : ''}`} aria-current={section === id ? 'page' : undefined}><Icon size={22} /><span>{label}</span>{id === 'accounts' && <span className="nav-count">{accounts.length}</span>}</a>)}</nav>
      <div className="sidebar-bottom">
        <div className="security-note"><ShieldCheck size={26} /><strong>Conexões que cuidam de você</strong><p>Mais clareza para sua vida financeira, em um só lugar.</p><span>Open Finance <ArrowUpRight size={16} /></span></div>
        <div className="profile"><span className="avatar">{initials(data.user.name)}</span><div><strong>{data.user.name}</strong><small>Conta pessoal</small></div></div>
      </div>
    </aside>

    <div className="main-area">
      <header className="topbar">
        <span className="breadcrumb"><span className="breadcrumb-parent">Minha carteira <span>/</span></span><strong>{sectionLabel}</strong></span>
        <div className="topbar-right">
          <span className="api-status">Consultado às {updatedAt.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</span>
          <button className="text-button icon-button" onClick={() => setHidden(!hidden)} aria-label={hidden ? 'Mostrar valores' : 'Ocultar valores'} aria-pressed={hidden}>{hidden ? <EyeOff size={20} /> : <Eye size={20} />}</button>
          <button className="text-button icon-button" onClick={onRefresh} aria-label="Atualizar dados da carteira"><RefreshCw size={20} /></button>
          <button className="text-button icon-button" onClick={onLogout} aria-label="Sair da conta"><LogOut size={20} /></button>
          <span className="avatar mini">{initials(data.user.name)}</span>
        </div>
      </header>

      <main id="main-content" className="section-content" ref={content} tabIndex={-1} aria-labelledby="view-title">
        <div className="content-container">
          <div className="page-heading">
            <div><div className="eyebrow">TUDO CONECTADO. TUDO MAIS SIMPLES.</div><h1 id="view-title" ref={heading} tabIndex={-1}>{section === 'overview' ? greeting(data.user.name) : sectionLabel}<span className="greeting-dot">.</span></h1><p>{section === 'overview' ? 'Suas finanças, juntas em um só lugar.' : section === 'accounts' ? 'Seus bancos e saldos, com todos os detalhes.' : 'Encontre e acompanhe cada movimentação.'}</p></div>
            <div className="page-actions">
              {section !== 'accounts' && <label className="period-select"><span className="sr-only">Período do extrato e resumo</span><select value={month} onChange={event => { setMonth(event.target.value); setExported(false); }}><option value="all">Todo o histórico</option>{availableMonths(data.transactions).map(value => <option key={value} value={value}>{periodLabel(value)}</option>)}</select><ChevronDown size={18} /></label>}
              {section === 'accounts' && <button onClick={onAddBank} className="primary-button add-bank-button" aria-haspopup="dialog"><Plus size={20} />Adicionar Banco</button>}
            </div>
          </div>
          {successMessage && <div className="bank-success" role="status"><CheckCircle2 size={21} /><span>{successMessage}</span></div>}

          {section === 'overview' && <section data-view="overview" aria-label="Visão geral financeira">
            <div className="summary-grid">
              <div className="balance-card"><div className="balance-top"><span><Wallet size={20} />Saldo total unificado</span><button className="eye-button" onClick={() => setHidden(!hidden)} aria-label={hidden ? 'Mostrar saldo unificado' : 'Ocultar saldo unificado'} aria-pressed={hidden}>{hidden ? <EyeOff size={20} /> : <Eye size={20} />}</button></div><strong className="balance-value">{display(data.total)}</strong><div className="balance-footer"><span>Distribuído em {accounts.length} contas</span><Layers3 size={26} /></div></div>
              <div className="flow-card"><span className="flow-icon income-bg"><ArrowDownLeft size={23} /></span><span className="muted">Entradas no período</span><strong>{display(income)}</strong><span className="flow-caption">{monthly.filter(t => t.type === 'income').length} recebimentos no período</span></div>
              <div className="flow-card"><span className="flow-icon expense-bg"><ArrowUpRight size={23} /></span><span className="muted">Saídas no período</span><strong>{display(expenses)}</strong><span className="flow-caption">{monthly.filter(t => t.type === 'expense').length} pagamentos no período</span></div>
            </div>
            <div className="overview-grid">
              <section className="panel recent-panel"><div className="section-heading"><div><h2>Últimas movimentações</h2><p className="section-subtitle">As cinco mais recentes do período.</p></div></div><TransactionList transactions={monthly.slice(0, 5)} accounts={accounts} hidden={hidden} currency={data.currency} /><button className="section-link" onClick={() => { setBank('all'); setQuery(''); setType('all'); setExported(false); navigate('transactions'); }}>Ver todas as transações <ArrowRight size={18} /></button></section>
              <aside className="insights">
                <section className="panel spending-panel"><span className="eyebrow">SEU DINHEIRO EM PERSPECTIVA</span><h2>Gastos por categoria</h2><p className="section-subtitle">Entenda para onde seu dinheiro vai.</p><div className="spending-total"><span>Total de saídas</span><strong>{display(categoryTotal)}</strong></div><div className="segmented-bar" aria-hidden="true">{categories.map(([category, value], index) => <span key={category} style={{ flex: value, background: categoryColor(index) }} />)}</div><div className="category-list">{categories.map(([category, value], index) => <div key={category}><span className="category-label"><i style={{ background: categoryColor(index) }} />{category}</span><span>{hidden ? '•••' : `${categoryTotal > 0 ? Math.round(value / categoryTotal * 100) : 0}%`}</span></div>)}</div>{!categories.length && <p className="muted">Sem gastos neste período.</p>}</section>
                <section className="connection-card"><Landmark size={30} /><h2>{accounts.length} contas na sua carteira</h2><p>Consulte os detalhes de cada banco.</p><button className="section-link" onClick={() => navigate('accounts')}>Ver minhas contas <ArrowRight size={18} /></button></section>
              </aside>
            </div>
          </section>}

          {section === 'accounts' && <section data-view="accounts" aria-label="Contas bancárias">
            <div className="section-heading"><h2>Bancos conectados <span className="count-badge">{accounts.length}</span></h2><span className="muted">Selecione uma conta para ver seu extrato.</span></div>
            <div className="accounts-grid">{accounts.map(account => <button key={account.id} className="account-card" onClick={() => viewAccount(account.id)} aria-label={`Ver extrato de ${account.bank}, ${account.name}, conta ${account.id}`}><div className="account-top"><BankIcon account={account} /><div><strong>{account.bank}</strong><small>{account.type}</small></div></div><div className="account-bottom"><div><span>Saldo disponível</span><strong>{display(account.balance)}</strong></div><ArrowUpRight size={22} /></div><span className="account-holder">{account.name}</span><span className="account-action">Ver extrato <ArrowRight size={18} /></span></button>)}</div>
            {!accounts.length && <div className="panel empty-state"><Landmark size={32} /><strong>Nenhuma conta cadastrada</strong><p>As contas aparecerão aqui após a conexão e sincronização no Open Finance.</p></div>}
          </section>}

          {section === 'transactions' && <section data-view="transactions" className="panel transactions-panel" aria-label="Extrato unificado">
            <div className="section-heading"><div><h2>Extrato unificado</h2><p className="section-subtitle">Cada movimento, uma visão completa.</p></div><button className="export-button" onClick={exportCsv} disabled={!filtered.length || hidden} aria-label="Exportar extrato filtrado em CSV"><Download size={19} /><span>Exportar</span></button></div>
            <div className="filters"><label className="search-field"><Search size={21} /><input aria-label="Buscar transações" placeholder="Buscar transação..." value={query} onChange={event => { setQuery(event.target.value); setExported(false); }} /></label><label className="bank-filter"><span className="sr-only">Filtrar por banco</span><select value={bank} onChange={event => { setBank(event.target.value); setExported(false); }}><option value="all">Todos os bancos</option>{accounts.map(account => <option key={account.id} value={account.id}>{account.bank} · {account.name} (#{account.id})</option>)}</select><ChevronDown size={18} /></label></div>
            <div className="transaction-tabs" aria-label="Tipo de transação">{[{ id: 'all', label: 'Todas' }, { id: 'income', label: 'Entradas' }, { id: 'expense', label: 'Saídas' }].map(tab => <button key={tab.id} className={type === tab.id ? 'selected' : ''} aria-pressed={type === tab.id} onClick={() => { setType(tab.id); setExported(false); }}>{tab.label}</button>)}</div>
            <TransactionList transactions={filtered} accounts={accounts} hidden={hidden} currency={data.currency} />
            <div className="list-footer"><span>{filtered.length} transações no período</span><span aria-live="polite">{exported ? 'Arquivo CSV exportado' : hidden ? 'Valores protegidos' : `Valores em ${data.currency}`}</span></div>
          </section>}

          <footer className="page-footer"><span><ShieldCheck size={18} />Sua vida financeira merece clareza.</span><span>Nexo © {new Date().getFullYear()} · Open Finance</span></footer>
        </div>
      </main>
    </div>
  </div>;
}
