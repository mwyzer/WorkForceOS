import { expect, test } from '@playwright/test';

import { screenshotPath } from './screenshot';

test('employees page lists staff and saves a screenshot', async ({ page }) => {
  await page.goto('/employees');

  await expect(page.locator('.employees__title')).toContainText('Employees');
  await expect(page.locator('.table tbody tr').first()).toBeVisible();
  await page.screenshot({ path: screenshotPath('employees.png'), fullPage: true });
});