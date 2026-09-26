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
  ['docs/milestones/m1-foundation-core.md', 'milestone-m1'],
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
<body><a class="skip" href="#main">Skip to content</a><header class="topbar"><a class="brand" href="./index.html" aria-label="Brace Android home"><span class="mark" aria-hidden="true">B</span><span>Brace <b>Android</b></span></a><nav aria-label="Main navigation"><a href="./index.html#coverage">Coverage</a><a href="./installation.html">Get started</a><a href="https://github.com/joelromanpr/brace-android">GitHub ↗</a></nav></header><main id="main" class="guide-layout"><aside class="guide-nav" aria-label="Documentation"><span>Documentation</span><a href="./installation.html">Installation</a><a href="./theming.html">Theming</a><a href="./compatibility.html">Compatibility</a><a href="./core-components.html">Core components</a><a href="./milestone-m1.html">M1 report</a><a href="./contributing.html">Contributing</a><a href="./attribution.html">Attribution</a><a href="./index.html#coverage">Coverage inventory</a></aside><article class="guide-article"><p class="eyebrow">Brace Android documentation</p>${body}<p class="source-link">Source: <a href="${repository + sourcePath}">${escapeHtml(sourcePath)} ↗</a></p></article></main><footer><span>Brace Android · Apache-2.0</span><span>Independent Android design system</span></footer></body></html>`;
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
