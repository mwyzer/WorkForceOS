import { expect, test } from '@playwright/test';

import { screenshotPath } from './screenshot';

const username = process.env.WORKFORCEOS_DEMO_USERNAME ?? 'admin';
const password = process.env.WORKFORCEOS_DEMO_PASSWORD ?? 'admin';

test.describe('login', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('rejects invalid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.locator('#username').fill(username);
    await page.locator('#password').fill('wrong-password');
    await page.locator('.submit-btn').click();

    await expect(page.locator('[role="alert"]')).toContainText('Incorrect username or password.');
  });

  test('signs in with valid credentials', async ({ page }, testInfo) => {
    await page.goto('/login');
    await page.screenshot({ path: screenshotPath('login.png'), fullPage: true });

    await page.locator('#username').fill(username);
    await page.locator('#password').fill(password);
    await page.locator('.submit-btn').click();

    await expect(page).toHaveURL('/');
    await expect(page.locator('.home__dashboard')).toBeVisible();
    await testInfo.attach('login-success', { path: screenshotPath('login-after-submit.png') });
  });
});