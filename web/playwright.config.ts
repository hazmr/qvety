import { defineConfig, devices } from '@playwright/test';

/**
 * Phone walkthrough against a running `make up` (backend :8080 with the dev seed, web :4200).
 * Three viewports, both languages; the spec loops the languages. Firefox is what the dev machine has
 * installed (`npx playwright install firefox`); nothing here is engine-specific.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop-1280', use: { ...devices['Desktop Firefox'], viewport: { width: 1280, height: 800 } } },
    { name: 'phone-390', use: { ...devices['Desktop Firefox'], viewport: { width: 390, height: 780 }, hasTouch: true } },
    { name: 'phone-360', use: { ...devices['Desktop Firefox'], viewport: { width: 360, height: 780 }, hasTouch: true } },
  ],
});
