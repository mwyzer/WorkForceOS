import { FullConfig, chromium } from '@playwright/test';

const username = process.env.WORKFORCEOS_DEMO_USERNAME ?? 'admin';
const password = process.env.WORKFORCEOS_DEMO_PASSWORD ?? 'admin';

export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0].use.baseURL ?? 'http://localhost:4200';
  const browser = await chromium.launch();

  const page = await browser.newPage({ baseURL });
  await page.goto('/login');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('.submit-btn').click();
  await page.locator('.home__dashboard, .home__status--error').waitFor({ state: 'visible' });

  await page.context().storageState({ path: './e2e/.auth/user.json' });
  await browser.close();
}