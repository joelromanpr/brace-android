import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { resolve, posix } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const coveragePath = resolve(root, 'docs/coverage.json');
const siteSource = resolve(root, 'docs/site');
const siteOutput = resolve(root, 'build/site');
const repository = 'https://github.com/joelromanpr/brace-android/blob/main/';
const guideSources = new Map([
  ['docs/installation.md', 'installation'],
  ['docs/theming.md', 'theming'],
  ['docs/compatibility.md', 'compatibility'],
  ['docs/core-components.md', 'core-components'],
  ['docs/content-feedback.md', 'content-feedback'],
  ['docs/navigation-feedback.md', 'navigation-feedback'],
  ['docs/overlays.md', 'overlays'],
  ['docs/drawers-popovers.md', 'drawers-popovers'],
  ['docs/tooltip-toast.md', 'tooltip-toast'],
  ['docs/context-shortcuts.md', 'context-shortcuts'],
  ['docs/form-text.md', 'form-text'],
  ['docs/form-layout.md', 'form-layout'],
  ['docs/numeric-input.md', 'numeric-input'],
  ['docs/icons.md', 'icons'],
  ['docs/select-query.md', 'select-query'],
  ['docs/top-bar.md', 'top-bar'],
  ['docs/radio-segmented.md', 'radio-segmented'],
  ['docs/milestones/m1-foundation-core.md', 'milestone-m1'],
  ['docs/milestones/m2-content-feedback.md', 'milestone-m2'],
  ['docs/milestones/m3-navigation-feedback.md', 'milestone-m3'],
  ['docs/milestones/m4-overlays.md', 'milestone-m4'],
  ['docs/milestones/m5-drawers-popovers.md', 'milestone-m5'],
  ['docs/milestones/m6-tooltip-toast.md', 'milestone-m6'],
  ['docs/milestones/m7-context-shortcuts.md', 'milestone-m7'],
  ['docs/milestones/m8-form-text.md', 'milestone-m8'],
  ['docs/milestones/m9-form-layout.md', 'milestone-m9'],
  ['docs/milestones/m10-numeric-input.md', 'milestone-m10'],
  ['docs/milestones/m11-icons.md', 'milestone-m11'],
  ['docs/milestones/m12-select-query.md', 'milestone-m12'],
  ['docs/milestones/m25-top-bar.md', 'milestone-m25'],
  ['docs/milestones/m19-radio-segmented.md', 'milestone-m19'],
  ['CONTRIBUTING.md', 'contributing'],
  ['docs/attribution.md', 'attribution'],
]);
const coverage = JSON.parse(await readFile(coveragePath, 'utf8'));

if (!Array.isArray(coverage.entries) || coverage.entries.length === 0 || coverage.summary?.totalRows !== coverage.entries.length) {
  throw new Error('Generated docs/coverage.json must contain entries and matching generated counts');
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
}

function hrefFrom(sourcePath, href) {
  if (/^https:\/\//.test(href) || href.startsWith('#')) return href;
  const normalized = posix.normalize(posix.join(posix.dirname(sourcePath), href));
  if (normalized === 'docs/coverage.md') return './index.html#coverage';
  const guide = guideSources.get(normalized);
  if (guide) return `./${guide}.html`;
  return repository + normalized.replace(/^\.\.\//, '');
}

function inline(source, sourcePath) {
  let result = escapeHtml(source);
  result = result.replace(/`([^`]+)`/g, '<code>$1</code>');
  result = result.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  result = result.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, href) => {
    const mapped = hrefFrom(sourcePath, href.replaceAll('&amp;', '&'));
    return `<a href="${escapeHtml(mapped)}">${label}</a>`;
  });
  return result;
}

function renderMarkdown(source, sourcePath) {
  const lines = source.replaceAll('\r\n', '\n').split('\n');
  const output = [];
  let paragraph = [];
  let list = null;
  let table = false;
  let code = null;
  let codeLines = [];
  const closeParagraph = () => {
    if (paragraph.length) output.push(`<p>${inline(paragraph.join(' '), sourcePath)}</p>`);
    paragraph = [];
  };
  const closeList = () => {
    if (list) output.push(`</${list}>`);
    list = null;
  };
  const closeTable = () => {
    if (table) output.push('</tbody></table>');
    table = false;
  };
  for (let index = 0; index < lines.length; index++) {
    const line = lines[index];
    const fence = line.match(/^```([^`]*)$/);
    if (fence) {
      closeParagraph(); closeList(); closeTable();
      if (code !== null) {
        output.push(`<pre><code class="language-${escapeHtml(code)}">${escapeHtml(codeLines.join('\n'))}</code></pre>`);
        code = null; codeLines = [];
      } else code = fence[1].trim();
      continue;
    }
    if (code !== null) { codeLines.push(line); continue; }
    const row = /^\|.*\|$/.test(line.trim());
    const cells = () => line.trim().slice(1, -1).split('|').map(cell => inline(cell.trim(), sourcePath));
    if (row && !table && /^\|[\s:|-]+\|$/.test(lines[index + 1]?.trim() || '')) {
      closeParagraph(); closeList();
      output.push(`<table><thead><tr>${cells().map(cell => `<th scope="col">${cell}</th>`).join('')}</tr></thead><tbody>`);
      table = true; index++; continue;
    }
    if (table && row) { output.push(`<tr>${cells().map(cell => `<td>${cell}</td>`).join('')}</tr>`); continue; }
    closeTable();
    if (!line.trim()) { closeParagraph(); closeList(); continue; }
    const heading = line.match(/^(#{1,4})\s+(.+)$/);
    if (heading) {
      closeParagraph(); closeList();
      const level = heading[1].length;
      const title = heading[2];
      const id = title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
      output.push(`<h${level} id="${id}">${inline(title, sourcePath)}</h${level}>`);
      continue;
    }
    const bullet = line.match(/^\s*-\s+(.+)$/);
    const number = line.match(/^\s*\d+\.\s+(.+)$/);
    if (bullet || number) {
      closeParagraph();
      const kind = bullet ? 'ul' : 'ol';
      if (list !== kind) { closeList(); output.push(`<${kind}>`); list = kind; }
      output.push(`<li>${inline((bullet || number)[1], sourcePath)}</li>`);
      continue;
    }
    closeList();
    paragraph.push(line.trim());
  }
  closeParagraph(); closeList(); closeTable();
  if (code !== null) throw new Error(`Unclosed code fence in ${sourcePath}`);
  return output.join('\n');
}

function guidePage(title, body, sourcePath) {
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#142338"><title>${escapeHtml(title)} · Brace Android</title><link rel="stylesheet" href="./styles.css"><link rel="stylesheet" href="./guide.css"></head>
<body><a class="skip" href="#main">Skip to content</a><header class="topbar"><a class="brand" href="./index.html" aria-label="Brace Android home"><span class="mark" aria-hidden="true">B</span><span>Brace <b>Android</b></span></a><nav aria-label="Main navigation"><a href="./index.html#coverage">Coverage</a><a href="./installation.html">Get started</a><a href="https://github.com/joelromanpr/brace-android">GitHub ↗</a></nav></header><main id="main" class="guide-layout"><aside class="guide-nav" aria-label="Documentation"><span>Documentation</span><a href="./installation.html">Installation</a><a href="./theming.html">Theming</a><a href="./compatibility.html">Compatibility</a><a href="./core-components.html">Core components</a><a href="./content-feedback.html">Content and feedback</a><a href="./navigation-feedback.html">Navigation and messages</a><a href="./overlays.html">Menus and overlays</a><a href="./drawers-popovers.html">Drawers and popovers</a><a href="./tooltip-toast.html">Tooltips and toasts</a><a href="./context-shortcuts.html">Context menus and shortcuts</a><a href="./form-text.html">Form fields and editable text</a><a href="./form-layout.html">Labels and control groups</a><a href="./numeric-input.html">Numeric input</a><a href="./icons.html">Icons</a><a href="./select-query.html">Select and query</a><a href="./top-bar.html">Top bar</a><a href="./radio-segmented.html">Radio and segmented choices</a><a href="./milestone-m1.html">M1 report</a><a href="./milestone-m2.html">M2 report</a><a href="./milestone-m3.html">M3 report</a><a href="./milestone-m4.html">M4 report</a><a href="./milestone-m5.html">M5 report</a><a href="./milestone-m6.html">M6 report</a><a href="./milestone-m7.html">M7 report</a><a href="./milestone-m8.html">M8 report</a><a href="./milestone-m9.html">M9 report</a><a href="./milestone-m10.html">M10 report</a><a href="./milestone-m11.html">M11 report</a><a href="./milestone-m12.html">M12 report</a><a href="./milestone-m25.html">M25 report</a><a href="./milestone-m19.html">M19 report</a><a href="./contributing.html">Contributing</a><a href="./attribution.html">Attribution</a><a href="./index.html#coverage">Coverage inventory</a></aside><article class="guide-article"><p class="eyebrow">Brace Android documentation</p>${body}<p class="source-link">Source: <a href="${repository + sourcePath}">${escapeHtml(sourcePath)} ↗</a></p></article></main><footer><span>Brace Android · Apache-2.0</span><span>Independent Android design system</span></footer></body></html>`;
}

await rm(siteOutput, { recursive: true, force: true });
await mkdir(siteOutput, { recursive: true });
await cp(siteSource, siteOutput, { recursive: true });
await cp(coveragePath, resolve(siteOutput, 'coverage.json'));
for (const [path, slug] of guideSources) {
  const markdown = await readFile(resolve(root, path), 'utf8');
  const title = markdown.match(/^# (.+)$/m)?.[1] || slug;
  await writeFile(resolve(siteOutput, `${slug}.html`), guidePage(title, renderMarkdown(markdown, path), path));
}
console.log(`Built documentation site with ${coverage.entries.length} inventory rows and ${guideSources.size} guides at ${siteOutput}`);
