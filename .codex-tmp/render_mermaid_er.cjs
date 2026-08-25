const fs = require('fs');
const { chromium } = require('playwright');

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}

(async () => {
  const sourcePath = 'C:\\Users\\33114\\.codex\\visualizations\\2026\\08\\24\\01a0340c-0d95-7c21-8aa7-267024a7dd52\\er-user-organization-mermaid.mmd';
  const outputPath = 'C:\\Users\\33114\\.codex\\visualizations\\2026\\08\\24\\01a0340c-0d95-7c21-8aa7-267024a7dd52\\er-user-organization-mermaid.png';
  const source = fs.readFileSync(sourcePath, 'utf8');

  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  });
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 }, deviceScaleFactor: 1.5 });
  await page.emulateMedia({ colorScheme: 'light', reducedMotion: 'reduce' });
  await page.setContent(`
    <div id="capture"><pre class="mermaid">${escapeHtml(source)}</pre></div>
    <style>
      html, body { margin: 0; background: #ffffff; }
      body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; }
      #capture { display: inline-block; box-sizing: border-box; min-width: 1500px; padding: 36px; background: #ffffff; }
      .mermaid { margin: 0; }
      .mermaid svg { display: block; max-width: none !important; height: auto; }
    </style>
  `, { waitUntil: 'load' });
  await page.addScriptTag({ url: 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js' });
  await page.evaluate(async () => {
    mermaid.initialize({ startOnLoad: false, securityLevel: 'loose' });
    await mermaid.run({ nodes: [document.querySelector('.mermaid')] });
  });
  await page.waitForSelector('.mermaid svg');
  await page.locator('#capture').screenshot({ path: outputPath });
  const size = await page.locator('.mermaid svg').evaluate((svg) => ({
    width: svg.getBoundingClientRect().width,
    height: svg.getBoundingClientRect().height,
  }));
  process.stdout.write(JSON.stringify(size));
  await browser.close();
})();
