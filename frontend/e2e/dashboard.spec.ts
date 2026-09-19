import { expect, test } from '@playwright/test';

import { screenshotPath } from './screenshot';

test('home dashboard renders and saves a screenshot', async ({ page }) => {
  await page.goto('/');

  await expect(page.locator('.home__title')).toContainText('Workforce overview');
  await expect(page.locator('.stat-card')).toBeVisible();
  await page.screenshot({ path: screenshotPath('dashboard.png'), fullPage: true });
});