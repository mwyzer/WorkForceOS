import { expect, test } from '@playwright/test';

import { screenshotPath } from './screenshot';

test('risk dashboard renders and saves a screenshot', async ({ page }) => {
  await page.goto('/risk');

  await expect(page.locator('.risk__title')).toContainText('Workforce risk intelligence');
  await page.waitForTimeout(1_000);
  await page.screenshot({ path: screenshotPath('risk.png'), fullPage: true });
});