import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests/browser',
  fullyParallel: false,
  timeout: 60000,
  workers: 1,
  use: {
    baseURL: 'http://127.0.0.1:5179',
    ...(process.platform === 'win32' ? { channel: 'msedge' } : {}),
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 5179 --strictPort',
    url: 'http://127.0.0.1:5179',
    reuseExistingServer: false,
  },
});
