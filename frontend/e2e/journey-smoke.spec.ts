import { expect, test } from '@playwright/test';

import { screenshotPath } from './screenshot';

const username = process.env.WORKFORCEOS_DEMO_USERNAME ?? 'admin';
const password = process.env.WORKFORCEOS_DEMO_PASSWORD ?? 'admin';

test('core user journey: login, employees, risk, overview (with screenshots)', async ({ page }) => {
  await page.goto('/login');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('.submit-btn').click();
  await expect(page).toHaveURL('/');
  await page.screenshot({ path: screenshotPath('journey-login.png'), fullPage: true });

  await page.locator('.home__link', { hasText: 'Employees' }).click();
  await expect(page).toHaveURL('**/employees');
  await expect(page.locator('.table tbody tr').first()).toBeVisible();
  await page.screenshot({ path: screenshotPath('journey-employees.png'), fullPage: true });

  await page.locator('.employees__link, .home__link', { hasText: 'Risk intelligence' }).click();
  await expect(page).toHaveURL('**/risk');
  await expect(page.locator('.risk__title')).toBeVisible();
  await page.screenshot({ path: screenshotPath('journey-risk.png'), fullPage: true });

  await page.locator('.risk__link', { hasText: 'Overview' }).click();
  await expect(page).toHaveURL('/');
  await expect(page.locator('.home__title')).toBeVisible();
  await page.screenshot({ path: screenshotPath('journey-overview.png'), fullPage: true });
});