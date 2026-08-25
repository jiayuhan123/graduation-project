const { chromium } = require('playwright');
const path = require('path');

const source = 'C:\\Users\\33114\\.codex\\visualizations\\2026\\08\\24\\01a0340c-0d95-7c21-8aa7-267024a7dd52\\practice-answering-revised-chen.html';
const output = 'C:\\Users\\33114\\.codex\\visualizations\\2026\\08\\24\\01a0340c-0d95-7c21-8aa7-267024a7dd52\\practice-answering-revised-chen.png';
const sourceUrl = `file:///${source.replaceAll('\\', '/')}`;
const qaDir = 'E:\\graduation-project\\.codex-tmp';

const themes = {
  light: {
    background: '#ffffff', foreground: '#1f2937', mutedForeground: '#64748b',
    muted: '#e5e7eb', border: '#94a3b8', series1: '#2563eb', series2: '#ea580c',
  },
  dark: {
    background: '#171717', foreground: '#f1f5f9', mutedForeground: '#a8b0bc',
    muted: '#2b2b2b', border: '#3f3f46', series1: '#60a5fa', series2: '#fb923c',
  },
};

function themeCss(theme) {
  return `:root {
    --background: ${theme.background};
    --foreground: ${theme.foreground};
    --muted-foreground: ${theme.mutedForeground};
    --muted: ${theme.muted};
    --border: ${theme.border};
    --viz-series-1: ${theme.series1};
    --viz-series-2: ${theme.series2};
  }
  html, body { background: ${theme.background}; }
  body { margin: 8px; }`;
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  });

  for (const [themeName, theme] of Object.entries(themes)) {
    for (const width of [736, 360]) {
      const page = await browser.newPage({ viewport: { width, height: 800 }, deviceScaleFactor: 1 });
      await page.emulateMedia({ colorScheme: themeName, reducedMotion: 'reduce' });
      await page.goto(sourceUrl, { waitUntil: 'load' });
      await page.addStyleTag({ content: themeCss(theme) });
      await page.screenshot({
        path: path.join(qaDir, `practice-answering-revised-${themeName}-${width}.png`),
        fullPage: true,
      });
      await page.close();
    }
  }

  const page = await browser.newPage({ viewport: { width: 736, height: 800 }, deviceScaleFactor: 2 });
  await page.emulateMedia({ colorScheme: 'light', reducedMotion: 'reduce' });
  await page.goto(sourceUrl, { waitUntil: 'load' });
  await page.addStyleTag({ content: themeCss(themes.light) });
  await page.screenshot({ path: output, fullPage: true });
  await page.close();
  await browser.close();
  process.stdout.write(output);
})();
