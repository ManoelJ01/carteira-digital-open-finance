import { test, expect, type Page } from '@playwright/test';

// Respostas isoladas dos testes de interface; o aplicativo continua usando a API.
const transactions = Array.from({ length: 36 }, (_, index) => ({
  id: index + 1, contaId: index % 2 ? 20 : 10, descricao: `Movimentação ${index + 1}`,
  valor: 25, tipo: index % 5 ? 'DEBITO' : 'CREDITO', categoria: 'COMPRAS', dataTransacao: '2026-01-10',
}));
const response = {
  saldoConsolidado: 456.78,
  usuario: { id: 1, nome: 'Pessoa de teste' },
  contas: [
    { contaId: 10, instituicao: 'Pluggy Bank', nome: 'Conta Corrente', tipo: 'BANK', saldo: 250, moeda: 'BRL' },
    { contaId: 20, instituicao: 'Itaú', nome: 'Mastercard Black', tipo: 'CREDIT', saldo: 206.78, moeda: 'BRL' },
  ],
  extratoUnificado: transactions,
  gastosPorCategoria: [{ categoria: 'COMPRAS', total: transactions.filter(t => t.tipo === 'DEBITO').length * 25 }],
};

async function assertLegible(page: Page) {
  const smallText = await page.locator('body').evaluate(body => [...body.querySelectorAll<HTMLElement>('*')].filter(element => {
    const box = element.getBoundingClientRect();
    return box.width > 4 && box.height > 4 && !element.closest('svg, .sr-only') && [...element.childNodes].some(node => node.nodeType === Node.TEXT_NODE && node.textContent?.trim()) && Number.parseFloat(getComputedStyle(element).fontSize) < 14;
  }).map(element => `${element.tagName}: ${element.textContent?.trim().slice(0, 40)}`));
  expect(smallText).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
}

async function openDashboard(page: Page) {
  await page.route('**/api/auth/login', route => route.fulfill({ json: { accessToken: 'token-apenas-de-teste', tokenType: 'Bearer', expiresInMs: 3600000 } }));
  await page.route('**/api/dashboard/resumo', route => route.fulfill({ json: response }));
  await page.goto('/');
  await expect(page.locator('.auth-card .brand')).toBeVisible();
  await expect(page.locator('.primary-button')).toHaveCSS('background-color', 'rgb(91, 33, 182)');
  await assertLegible(page);
  await page.getByLabel('E-mail', { exact: true }).fill('teste@example.com');
  await page.getByLabel('Senha', { exact: true }).fill('senha-teste');
  await page.getByRole('button', { name: 'Entrar na minha carteira' }).click();
  await expect(page.locator('[data-view="overview"]')).toBeVisible();
}

for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }, { width: 320, height: 700 }]) {
  test.describe(`${viewport.width}px`, () => {
    test.use({ viewport });
    test('login e views isoladas mantêm tema, legibilidade, filtros e navegação', async ({ page }) => {
      await openDashboard(page);
      const nav = page.getByRole('navigation', { name: 'Navegação principal' });
      await expect(page.locator('[data-view]')).toHaveCount(1);
      await expect(page.locator('.balance-card')).toHaveCSS('background-color', 'rgb(59, 23, 104)');
      await expect(page.locator('.transaction-row')).toHaveCount(5);
      await assertLegible(page);
      await page.screenshot({ path: `test-results/overview-${viewport.width}.png`, fullPage: true });

      await nav.getByRole('link', { name: /Minhas contas/ }).click();
      await expect(page.locator('[data-view="accounts"]')).toBeVisible();
      await expect(page.locator('[data-view]')).toHaveCount(1);
      await expect(page.locator('.balance-card')).toHaveCount(0);
      await expect(page.getByRole('heading', { level: 1 })).toBeFocused();
      await expect(nav.locator('a[aria-current="page"] svg')).toHaveClass(/lucide-landmark/);
      await expect(page.locator('.bank-icon').first()).toHaveCSS('background-color', 'rgb(91, 33, 182)');
      await assertLegible(page);

      await page.getByRole('button', { name: /Ver extrato de Pluggy Bank/ }).click();
      await expect(page.locator('[data-view="transactions"]')).toBeVisible();
      await expect(page.getByLabel('Filtrar por banco')).toHaveValue('10');
      await expect(page.locator('[data-view]')).toHaveCount(1);
      await expect(page.locator('.transaction-row')).toHaveCount(18);
      await assertLegible(page);

      await page.getByRole('button', { name: 'Ocultar valores', exact: true }).click();
      await expect(page.getByRole('button', { name: 'Exportar extrato filtrado em CSV' })).toBeDisabled();
      await page.goBack();
      await expect(page.locator('[data-view="accounts"]')).toBeVisible();
      await expect(page.locator('.account-bottom strong').first()).toHaveText('••••••');

      await nav.getByRole('link', { name: 'Transações' }).click();
      await page.getByLabel('Filtrar por banco').selectOption('all');
      await page.locator('main').evaluate(element => { element.scrollTop = 500; });
      expect(await page.locator('main').evaluate(element => element.scrollTop)).toBeGreaterThan(0);
      expect(await page.evaluate(() => window.scrollY)).toBe(0);
      await nav.getByRole('link', { name: 'Visão geral' }).click();
      await expect(page.locator('[data-view="overview"]')).toBeVisible();
      expect(await page.locator('main').evaluate(element => element.scrollTop)).toBe(0);
      expect(await page.evaluate(() => document.documentElement.scrollHeight <= innerHeight)).toBe(true);
    });
  });
}
