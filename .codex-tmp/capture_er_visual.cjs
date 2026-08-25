const { chromium } = require('playwright');
const { pathToFileURL } = require('url');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  });
  const source = pathToFileURL('C:\\Users\\33114\\.codex\\visualizations\\2026\\08\\24\\01a0340c-0d95-7c21-8aa7-267024a7dd52\\er-user-organization-chen.html').href;
  for (const width of [736, 360]) {
    const page = await browser.newPage({ viewport: { width, height: 900 }, deviceScaleFactor: 1 });
    await page.emulateMedia({ colorScheme: 'light', reducedMotion: 'reduce' });
    await page.goto(source, { waitUntil: 'load' });
    await page.addStyleTag({ content: `:root {
      --background: #ffffff;
      --foreground: #1f2937;
      --muted-foreground: #64748b;
      --muted: #e5e7eb;
      --border: #94a3b8;
      --viz-series-1: #2563eb;
      --viz-series-2: #ea580c;
    } body { margin: 8px; }` });
    await page.screenshot({
      path: `E:\\graduation-project\\.codex-tmp\\er-user-organization-${width}.png`,
      fullPage: true,
    });
    if (width === 736) {
      const diagrams = page.locator('svg.diagram');
      const count = await diagrams.count();
      for (let index = 0; index < count; index += 1) {
        await diagrams.nth(index).screenshot({
          path: `E:\\graduation-project\\.codex-tmp\\er-user-organization-panel-${index + 1}.png`,
        });
      }
    }
    await page.close();
  }
  await browser.close();
})();
