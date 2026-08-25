const { chromium } = require('playwright');
const { pathToFileURL } = require('url');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  });
  const source = pathToFileURL('C:\\Users\\33114\\.codex\\visualizations\\2026\\08\\24\\01a0340c-0d95-7c21-8aa7-267024a7dd52\\er-user-organization-logical.html').href;
  const themes = {
    light: {
      background: '#ffffff', foreground: '#172033', card: '#ffffff', cardForeground: '#172033',
      mutedForeground: '#5b6576', border: '#94a3b8', primary: '#2563eb',
    },
    dark: {
      background: '#151719', foreground: '#f1f5f9', card: '#202327', cardForeground: '#f1f5f9',
      mutedForeground: '#a8b0bc', border: '#5d6673', primary: '#60a5fa',
    },
  };

  for (const [themeName, theme] of Object.entries(themes)) {
    for (const width of [736, 360]) {
      const page = await browser.newPage({ viewport: { width, height: 900 }, deviceScaleFactor: 1 });
      await page.emulateMedia({ colorScheme: themeName, reducedMotion: 'reduce' });
      await page.goto(source, { waitUntil: 'load' });
      await page.addStyleTag({ content: `:root {
        --background: ${theme.background};
        --foreground: ${theme.foreground};
        --card: ${theme.card};
        --card-foreground: ${theme.cardForeground};
        --muted-foreground: ${theme.mutedForeground};
        --border: ${theme.border};
        --primary: ${theme.primary};
        --font-size-base: 16px;
      }
      html, body { background: ${theme.background}; }
      body { margin: 12px; font-family: "Microsoft YaHei", "PingFang SC", sans-serif; }
      ` });
      await page.waitForTimeout(120);
      await page.screenshot({
        path: `E:\\graduation-project\\.codex-tmp\\logical-er-${themeName}-${width}.png`,
        fullPage: true,
      });
      await page.close();
    }
  }

  const exportPage = await browser.newPage({ viewport: { width: 736, height: 900 }, deviceScaleFactor: 2 });
  await exportPage.emulateMedia({ colorScheme: 'light', reducedMotion: 'reduce' });
  await exportPage.goto(source, { waitUntil: 'load' });
  await exportPage.addStyleTag({ content: `:root {
    --background: #ffffff;
    --foreground: #172033;
    --card: #ffffff;
    --card-foreground: #172033;
    --muted-foreground: #5b6576;
    --border: #94a3b8;
    --primary: #2563eb;
    --font-size-base: 16px;
  }
  html, body { background: #ffffff; }
  body { margin: 12px; font-family: "Microsoft YaHei", "PingFang SC", sans-serif; }
  ` });
  await exportPage.waitForTimeout(120);
  await exportPage.screenshot({
    path: 'C:\\Users\\33114\\.codex\\visualizations\\2026\\08\\24\\01a0340c-0d95-7c21-8aa7-267024a7dd52\\er-user-organization-logical.png',
    fullPage: true,
  });
  await exportPage.close();
  await browser.close();
})();
