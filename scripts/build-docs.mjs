import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { resolve, posix } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const coveragePath = resolve(root, 'docs/coverage.json');
const siteSource = resolve(root, 'docs/site');
const siteOutput = resolve(root, 'build/site');
const siteConfig = JSON.parse(await readFile(resolve(siteSource, 'site-config.json'), 'utf8'));
if (siteConfig.schemaVersion !== 1 || (siteConfig.publicSourceRepository !== null && !/^https:\/\/github\.com\/[A-Za-z0-9-]+\/[A-Za-z0-9._-]+$/.test(siteConfig.publicSourceRepository))) {
  throw new Error('Invalid docs/site/site-config.json');
}
const repository = siteConfig.publicSourceRepository ? `${siteConfig.publicSourceRepository}/blob/main/` : null;
const guideSources = new Map([
  ['docs/installation.md', 'installation'],
  ['docs/showcase.md', 'showcase-guide'],
  ['docs/theming.md', 'theming'],
  ['docs/compatibility.md', 'compatibility'],
  ['docs/core-components.md', 'core-components'],
  ['docs/content-feedback.md', 'content-feedback'],
  ['docs/semantic-content.md', 'semantic-content'],
  ['docs/loading-feedback.md', 'loading-feedback'],
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
  ['docs/tag-input.md', 'tag-input'],
  ['docs/radio-segmented.md', 'radio-segmented'],
  ['docs/datetime-picker-input.md', 'datetime-picker-input'],
  ['docs/time-picker-input.md', 'time-picker-input'],
  ['docs/datetime-range.md', 'datetime-range'],
  ['docs/table-viewport.md', 'table-viewport'],
  ['docs/table-selection-resize.md', 'table-selection-resize'],
  ['docs/table-copying.md', 'table-copying'],
  ['docs/milestones/m21-table-copying.md', 'milestone-m21'],
  ['docs/web-mechanisms.md', 'web-mechanisms'],
  ['docs/links.md', 'links'],
  ['docs/time-zone-select.md', 'time-zone-select'],
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
  ['docs/milestones/m17-loading-feedback.md', 'milestone-m17'],
  ['docs/milestones/m25-top-bar.md', 'milestone-m25'],
  ['docs/milestones/m15-tag-input.md', 'milestone-m15'],
  ['docs/milestones/m19-radio-segmented.md', 'milestone-m19'],
  ['docs/milestones/m13-datetime-picker.md', 'milestone-m13'],
  ['docs/milestones/m30-time-picker-input.md', 'milestone-m30'],
  ['docs/milestones/m33-date-range.md', 'milestone-m33'],
  ['docs/milestones/m14-table-viewport.md', 'milestone-m14'],
  ['docs/milestones/m18-table-selection-resize.md', 'milestone-m18'],
  ['docs/milestones/m35-blueprint-icon-pack.md', 'milestone-m35'],
  ['docs/milestones/m36-semantic-content.md', 'milestone-m36'],
  ['docs/milestones/m54-web-mechanisms.md', 'milestone-m54'],
  ['docs/milestones/m20-links.md', 'milestone-m20'],
  ['docs/milestones/m55-blueprint-next-icons.md', 'milestone-m55'],
  ['docs/milestones/m57-icon-large-text.md', 'milestone-m57'],
  ['docs/milestones/m34-timezone-select.md', 'milestone-m34'],
  ['docs/milestones/m59-visual-catalog.md', 'milestone-m59'],
  ['CONTRIBUTING.md', 'contributing'],
  ['docs/attribution.md', 'attribution'],
]);
const coverage = JSON.parse(await readFile(coveragePath, 'utf8'));
const capturesPath = resolve(siteSource, 'showcase/captures.json');
const captureManifest = JSON.parse(await readFile(capturesPath, 'utf8'));
const pngSignature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

if (!Array.isArray(coverage.entries) || coverage.entries.length === 0 || coverage.summary?.totalRows !== coverage.entries.length) {
  throw new Error('Generated docs/coverage.json must contain entries and matching generated counts');
}

async function validateCaptures() {
  if (captureManifest.schemaVersion !== 1 || !Array.isArray(captureManifest.captures) || captureManifest.captures.length === 0) {
    throw new Error('The showcase needs at least one catalog capture in docs/site/showcase/captures.json');
  }
  const inventoryIds = new Set(coverage.entries.map(entry => entry.id));
  const captureIds = new Set();
  for (const capture of captureManifest.captures) {
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(capture.id) || captureIds.has(capture.id)) {
      throw new Error(`Invalid or repeated showcase capture id: ${capture.id}`);
    }
    captureIds.add(capture.id);
    if (!Array.isArray(capture.inventoryIds) || capture.inventoryIds.length === 0 ||
        capture.inventoryIds.some(id => !inventoryIds.has(id)) || new Set(capture.inventoryIds).size !== capture.inventoryIds.length) {
      throw new Error(`${capture.id}: inventoryIds must name unique pinned inventory rows`);
    }
    for (const field of ['alt', 'caption', 'device', 'sourceBranch', 'sourceFile', 'usage']) {
      if (typeof capture[field] !== 'string' || !capture[field].trim()) throw new Error(`${capture.id}: missing ${field}`);
    }
    if (!/^showcase\/[a-z0-9-]+\.png$/.test(capture.image)) throw new Error(`${capture.id}: invalid image path`);
    if (!/^[a-f0-9]{40}$/.test(capture.sourceCommit)) throw new Error(`${capture.id}: sourceCommit must be a full Git SHA`);
    if (!/^catalog\/src\/main\/.+\.kt$/.test(capture.sourceFile)) throw new Error(`${capture.id}: sourceFile must point to a Kotlin catalog source file`);
    if (!/^(main|joelromanpr\/[a-z0-9-]+)$/.test(capture.sourceBranch)) throw new Error(`${capture.id}: invalid sourceBranch`);
    if (!['draft', 'merged'].includes(capture.sourceStage) || (capture.sourceStage === 'merged') !== (capture.sourceBranch === 'main')) {
      throw new Error(`${capture.id}: sourceStage and sourceBranch disagree`);
    }
    if (!['phone', 'landscape'].includes(capture.format) || !['light', 'dark'].includes(capture.theme) ||
        !['standard', 'high'].includes(capture.contrast) || !['comfortable', 'compact'].includes(capture.density)) {
      throw new Error(`${capture.id}: invalid capture appearance metadata`);
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(capture.capturedAt)) throw new Error(`${capture.id}: invalid capture date`);
    const bytes = await readFile(resolve(siteSource, capture.image));
    if (!bytes.subarray(0, 8).equals(pngSignature) || bytes.toString('ascii', 12, 16) !== 'IHDR') {
      throw new Error(`${capture.id}: image is not a PNG`);
    }
    const width = bytes.readUInt32BE(16);
    const height = bytes.readUInt32BE(20);
    if (capture.pixelWidth !== width || capture.pixelHeight !== height) {
      throw new Error(`${capture.id}: manifest dimensions ${capture.pixelWidth}×${capture.pixelHeight} differ from PNG ${width}×${height}`);
    }
  }
}

await validateCaptures();

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
}

function hrefFrom(sourcePath, href) {
  if (/^https:\/\//.test(href) || href.startsWith('#')) return href;
  const normalized = posix.normalize(posix.join(posix.dirname(sourcePath), href));
  if (normalized === 'docs/coverage.md') return './index.html#coverage';
  if (normalized === 'docs/site/index.html') return './index.html#showcase';
  const guide = guideSources.get(normalized);
  if (guide) return `./${guide}.html`;
  return repository ? repository + normalized.replace(/^\.\.\//, '') : null;
}

function inline(source, sourcePath) {
  let result = escapeHtml(source);
  result = result.replace(/`([^`]+)`/g, '<code>$1</code>');
  result = result.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  result = result.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, href) => {
    const mapped = hrefFrom(sourcePath, href.replaceAll('&amp;', '&'));
    return mapped ? `<a href="${escapeHtml(mapped)}">${label}</a>` : `<span class="source-pending" title="Source link available when the repository is public">${label}</span>`;
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

const primaryGuideLinks = [
  ['Visual showcase', 'showcase-guide'],
  ['Installation', 'installation'],
  ['Theming', 'theming'],
  ['Compatibility', 'compatibility'],
];
const componentGuideLinks = [
  ['Core controls', 'core-components'],
  ['Semantic content', 'semantic-content'],
  ['Forms and text', 'form-text'],
  ['Select and query', 'select-query'],
  ['Date and time', 'datetime-picker-input'],
  ['Date ranges', 'datetime-range'],
  ['Time-zone selection', 'time-zone-select'],
  ['Data tables', 'table-viewport'],
  ['Selection and resizing', 'table-selection-resize'],
  ['Copying cells', 'table-copying'],
  ['Icons', 'icons'],
];
const extraGuideLinks = [
  ['Content and feedback', 'content-feedback'],
  ['Loading feedback', 'loading-feedback'],
  ['Navigation and messages', 'navigation-feedback'],
  ['Menus and overlays', 'overlays'],
  ['Drawers and popovers', 'drawers-popovers'],
  ['Tooltips and toasts', 'tooltip-toast'],
  ['Context menus and shortcuts', 'context-shortcuts'],
  ['Labels and control groups', 'form-layout'],
  ['Numeric input', 'numeric-input'],
  ['Top bar', 'top-bar'],
  ['Tag input', 'tag-input'],
  ['Radio and segmented choices', 'radio-segmented'],
  ['Time picker and field', 'time-picker-input'],
  ['Web mechanisms in Compose', 'web-mechanisms'],
  ['Links', 'links'],
];

function guidePage(title, body, sourcePath) {
  const current = guideSources.get(sourcePath);
  const navLink = ([label, id]) => `<a href="./${id}.html"${current === id ? ' aria-current="page"' : ''}>${label}</a>`;
  const milestoneLinks = [...guideSources.values()]
    .filter(id => /^milestone-m\d+$/.test(id))
    .sort((a, b) => Number(a.slice(11)) - Number(b.slice(11)))
    .map(id => [`M${id.slice(11)} report`, id]);
  const navSections = `
      <div class="guide-nav-group"><span class="guide-nav-title">Start here</span>${primaryGuideLinks.map(navLink).join('')}</div>
      <details class="guide-nav-details"${[...componentGuideLinks, ...extraGuideLinks].some(([, id]) => id === current) ? ' open' : ''}><summary>Component guides</summary><div>${[...componentGuideLinks, ...extraGuideLinks].filter(([, id]) => [...guideSources.values()].includes(id)).map(navLink).join('')}</div></details>
      <div class="guide-nav-group"><span class="guide-nav-title">Reference</span><a href="./index.html#coverage">Coverage inventory</a><a href="./contributing.html"${current === 'contributing' ? ' aria-current="page"' : ''}>Contributing</a><a href="./attribution.html"${current === 'attribution' ? ' aria-current="page"' : ''}>Attribution</a></div>
      <details class="guide-nav-details"${current?.startsWith('milestone-') ? ' open' : ''}><summary>Milestone audit</summary><div>${milestoneLinks.map(navLink).join('')}</div></details>`;
  const navigation = `<aside class="guide-nav" aria-label="Documentation"><details class="guide-nav-mobile"><summary>Browse documentation</summary>${navSections}</details><div class="guide-nav-desktop">${navSections}</div></aside>`;
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#142338"><title>${escapeHtml(title)} · Brace Android</title><link rel="stylesheet" href="./styles.css"><link rel="stylesheet" href="./guide.css"></head>
<body><a class="skip" href="#main">Skip to content</a><header class="topbar"><a class="brand" href="./index.html" aria-label="Brace Android home"><span class="mark" aria-hidden="true">B</span><span>Brace <b>Android</b></span></a><nav aria-label="Main navigation"><a href="./index.html#examples">App examples</a><a href="./index.html#coverage">Coverage</a><a href="./installation.html">Get started</a><a href="https://github.com/joelromanpr/brace-android">GitHub ↗</a></nav></header><main id="main" class="guide-layout">${navigation}<article class="guide-article"><p class="eyebrow">Brace Android documentation</p>${body}<p class="source-link">${repository ? `Source: <a href="${repository + sourcePath}">${escapeHtml(sourcePath)} ↗</a>` : `Source path: <code>${escapeHtml(sourcePath)}</code> · public repository link pending`}</p></article></main><footer><span>Brace Android · Apache-2.0</span><span>Independent Android design system</span></footer></body></html>`;
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
console.log(`Built documentation site with ${coverage.entries.length} inventory rows, ${captureManifest.captures.length} real catalog captures, and ${guideSources.size} guides at ${siteOutput}`);
