// Contrato de GET /dashboard/resumo. Nenhuma informação financeira é criada localmente.
export interface DashboardResponse {
  saldoConsolidado: number;
  contas: { contaId: number; instituicao?: string; nome?: string; tipo: string; saldo: number; moeda: string }[];
  extratoUnificado: { id: number; descricao?: string; valor: number; tipo: 'CREDITO' | 'DEBITO'; categoria: string; dataTransacao: string; contaOrigem?: string; contaId: number }[];
  gastosPorCategoria: { categoria: string; total: number }[];
  usuario: { id: number; nome: string };
}
export interface Account { id: string; bank: string; mark: string; color: string; balance: number; name: string; type: string; currency: string }
export interface Transaction { id: string; accountId: string; description: string; category: string; date: string; amount: number; type: 'income' | 'expense' }
export interface DashboardData { total: number; currency: string; user: { id: number; name: string }; accounts: Account[]; transactions: Transaction[]; categories: [string, number][] }

const categoryLabels: Record<string, string> = {
  ALIMENTACAO: 'Alimentação', TRANSPORTE: 'Transporte', LAZER: 'Lazer', SAUDE: 'Saúde', EDUCACAO: 'Educação',
  MORADIA: 'Moradia', COMPRAS: 'Compras', SERVICOS: 'Serviços', RENDA: 'Renda', TRANSFERENCIA: 'Transferência', OUTROS: 'Outros',
};
const labelCategory = (value: string) => categoryLabels[value] || value;
const requireValid = (valid: unknown): void => { if (!valid) throw new Error('A resposta da API está incompleta ou incompatível. Atualize o backend e tente novamente.'); };
const validId = (value: unknown) => typeof value === 'number' && Number.isSafeInteger(value) && value > 0;
const validMoney = (value: unknown) => typeof value === 'number' && Number.isFinite(value) && Number.isSafeInteger(Math.round(value * 100));
const validDate = (value: unknown): value is string => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(value)) && new Date(value).toISOString().slice(0, 10) === value;

export function adaptDashboard(response: DashboardResponse): DashboardData {
  requireValid(response && validMoney(response.saldoConsolidado) && Array.isArray(response.contas) && Array.isArray(response.extratoUnificado) && Array.isArray(response.gastosPorCategoria));
  requireValid(response.usuario && validId(response.usuario.id) && typeof response.usuario.nome === 'string' && response.usuario.nome.trim());
  const accounts = response.contas.map(account => {
    requireValid(account && validId(account.contaId) && validMoney(account.saldo) && typeof account.moeda === 'string' && /^[A-Z]{3}$/.test(account.moeda) && typeof account.tipo === 'string');
    const bank = account.instituicao?.trim() || 'Instituição não informada';
    const normalized = bank.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const color = normalized.includes('nubank') ? 'var(--color-bank-nubank)' : normalized.includes('itau') ? 'var(--color-bank-itau)' : normalized === 'inter' || normalized.includes('banco inter') ? 'var(--color-bank-inter)' : 'var(--color-primary)';
    return { id: String(account.contaId), bank, mark: bank.slice(0, 2).toUpperCase(), color, balance: account.saldo, name: account.nome || 'Conta sem nome', type: account.tipo === 'CREDIT' ? 'Cartão de crédito' : account.tipo === 'BANK' ? 'Conta bancária' : account.tipo, currency: account.moeda };
  });
  const accountIds = new Set(accounts.map(account => account.id));
  requireValid(accountIds.size === accounts.length);
  const currencies = new Set(accounts.map(account => account.currency));
  if (currencies.size > 1) throw new Error('As contas possuem moedas diferentes. A API precisa fornecer a conversão cambial antes de exibir um saldo consolidado.');
  const transactions = response.extratoUnificado.map(transaction => {
    requireValid(transaction && validId(transaction.id) && validId(transaction.contaId) && accountIds.has(String(transaction.contaId)) && validMoney(transaction.valor) && transaction.valor >= 0 && ['CREDITO', 'DEBITO'].includes(transaction.tipo) && typeof transaction.categoria === 'string' && validDate(transaction.dataTransacao));
    return { id: String(transaction.id), accountId: String(transaction.contaId), description: transaction.descricao || 'Sem descrição', category: labelCategory(transaction.categoria), date: transaction.dataTransacao, amount: transaction.tipo === 'DEBITO' ? -transaction.valor : transaction.valor, type: transaction.tipo === 'DEBITO' ? 'expense' as const : 'income' as const };
  }).sort((a, b) => b.date.localeCompare(a.date) || b.id.localeCompare(a.id, undefined, { numeric: true }));
  requireValid(new Set(transactions.map(transaction => transaction.id)).size === transactions.length);
  const categories: [string, number][] = response.gastosPorCategoria.map(category => {
    requireValid(category && typeof category.categoria === 'string' && validMoney(category.total) && category.total >= 0);
    return [labelCategory(category.categoria), category.total];
  });
  return { total: response.saldoConsolidado, currency: accounts[0]?.currency || 'BRL', user: { id: response.usuario.id, name: response.usuario.nome }, accounts, transactions, categories: categories.sort((a, b) => b[1] - a[1]) };
}

export const money = (value: number, currency = 'BRL') => new Intl.NumberFormat('pt-BR', { style: 'currency', currency }).format(value);
export const greeting = (name: string) => `Olá, ${name.trim().split(/\s+/)[0]}`;
export const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
export const periodLabel = (month: string) => new Date(`${month}-01T12:00:00`).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
export const availableMonths = (transactions: Transaction[]) => [...new Set(transactions.map(transaction => transaction.date.slice(0, 7)))].sort().reverse();

export function filterTransactions(transactions: Transaction[], month: string, bank: string, type: string, query: string) {
  const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR');
  return transactions.filter(transaction => (month === 'all' || transaction.date.startsWith(month)) && (bank === 'all' || transaction.accountId === bank) && (type === 'all' || transaction.type === type) && normalize(`${transaction.description} ${transaction.category}`).includes(normalize(query)));
}

export function exportRows(transactions: Transaction[], accounts: Account[], currency: string) {
  // Textos vindos da API podem começar com fórmulas de planilhas.
  const textCell = (value: string) => /^[\s]*[=+\-@]/.test(value) ? `'${value}` : value;
  const rows = [['Data', 'Descrição', 'Banco', 'Categoria', 'Valor', 'Moeda'], ...transactions.map(transaction => [transaction.date, textCell(transaction.description), textCell(accounts.find(account => account.id === transaction.accountId)?.bank || ''), textCell(transaction.category), transaction.amount.toFixed(2).replace('.', ','), currency])];
  return '\uFEFF' + rows.map(row => row.map(value => `"${value.replace(/"/g, '""')}"`).join(';')).join('\r\n');
}
