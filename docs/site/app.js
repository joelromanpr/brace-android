const search = document.querySelector('#search');
const packageSelect = document.querySelector('#package');
const statusSelect = document.querySelector('#status');
const kindSelect = document.querySelector('#kind');
const results = document.querySelector('#components');
const resultCount = document.querySelector('#result-count');
const stats = document.querySelector('#stats');
const baseline = document.querySelector('#baseline');
let publicSourceRepository = null;
let repository = null;
const showcaseGrid = document.querySelector('#showcase-grid');
const showcaseTheme = document.querySelector('#showcase-theme');
const showcaseFamily = document.querySelector('#showcase-family');
const showcaseResult = document.querySelector('#showcase-result');
const heroSnapshotCount = document.querySelector('#hero-snapshot-count');
const heroImage = document.querySelector('#hero-image');
let entries = [];
let captures = [];
let entryById = new Map();

function el(tag, className, content) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (content !== undefined && content !== null) node.textContent = String(content);
  return node;
}

function addOptions(select, values) {
  for (const value of [...new Set(values.filter(Boolean))].sort((a, b) => a.localeCompare(b))) {
    const option = el('option', '', value);
    option.value = value;
    select.append(option);
  }
}

function stat(value, label) {
  const box = el('div', 'stat');
  box.append(el('strong', '', value), el('span', '', label));
  return box;
}

function addFact(grid, label, value, isLink = false) {
  if (!value) return;
  const wrap = el('div', 'fact');
  const term = el('dt', '', label);
  const description = el('dd');
  if (isLink) {
    const [path, fragment] = String(value).split('#', 2);
    const guide = { 'docs/core-components.md': 'core-components.html', 'docs/content-feedback.md': 'content-feedback.html', 'docs/semantic-content.md': 'semantic-content.html', 'docs/loading-feedback.md': 'loading-feedback.html', 'docs/navigation-feedback.md': 'navigation-feedback.html', 'docs/overlays.md': 'overlays.html', 'docs/drawers-popovers.md': 'drawers-popovers.html', 'docs/tooltip-toast.md': 'tooltip-toast.html', 'docs/context-shortcuts.md': 'context-shortcuts.html', 'docs/form-text.md': 'form-text.html', 'docs/form-layout.md': 'form-layout.html', 'docs/numeric-input.md': 'numeric-input.html', 'docs/icons.md': 'icons.html', 'docs/select-query.md': 'select-query.html', 'docs/top-bar.md': 'top-bar.html', 'docs/radio-segmented.md': 'radio-segmented.html', 'docs/datetime-picker-input.md': 'datetime-picker-input.html', 'docs/time-picker-input.md': 'time-picker-input.html', 'docs/datetime-range.md': 'datetime-range.html', 'docs/table-viewport.md': 'table-viewport.html', 'docs/table-selection-resize.md': 'table-selection-resize.html', 'docs/links.md': 'links.html', 'docs/web-mechanisms.md': 'web-mechanisms.html', 'docs/theming.md': 'theming.html', 'docs/installation.md': 'installation.html', 'docs/compatibility.md': 'compatibility.html', 'docs/attribution.md': 'attribution.html' }[path];
    const href = /^https:\/\//.test(value) ? value : guide ? `./${guide}${fragment ? `#${fragment}` : ''}` : repository ? repository + value.replace(/^\/+/, '') : null;
    if (href) {
      const text = label === 'Blueprint documentation' ? 'Open Blueprint docs ↗' : label === 'Pinned Blueprint source' ? 'Open pinned source ↗' : guide ? 'Read guide ↗' : 'View public source ↗';
      const link = el('a', '', text);
      link.href = href;
      link.rel = 'noopener noreferrer';
      description.append(link);
    } else description.textContent = `${value} · public source link pending`;
  } else description.textContent = value;
  wrap.append(term, description);
  grid.append(wrap);
}

function card(item) {
  const details = el('details', 'component');
  details.id = `component-${item.id}`;
  const summary = el('summary');
  const title = el('span', 'component-name', item.blueprintName || item.id);
  title.append(el('span', 'component-family', `${item.package || 'Unassigned'} · ${item.family || 'General'}`));
  const badge = el('span', `pill ${String(item.status || 'planned').replace(/\s+/g, '-')}`, item.status || 'planned');
  summary.append(title, badge, el('span', 'milestone', item.milestone || 'Unscheduled'));
  const body = el('dl', 'component-body');
  addFact(body, 'Brace API', item.braceApi);
  addFact(body, 'Artifact', item.artifact);
  addFact(body, 'Android mapping', item.classification);
  addFact(body, 'Behavior and accessibility', item.behavior);
  const visual = captures.find(capture => capture.inventoryIds.includes(item.id));
  if (visual) {
    const visualFact = el('div', 'fact');
    const visualDescription = el('dd');
    const visualLink = el('a', '', 'See Android catalog capture ↗');
    visualLink.href = `#capture-${visual.id}`;
    visualLink.dataset.captureId = visual.id;
    visualDescription.append(visualLink);
    visualFact.append(el('dt', '', 'Visual preview'), visualDescription);
    body.append(visualFact);
  }
  addFact(body, 'Adaptation or exclusion', item.reason);
  addFact(body, 'Priority', item.priority);
  addFact(body, 'Blueprint documentation', item.blueprintUrl, true);
  addFact(body, 'Pinned Blueprint source', item.pinnedSourceUrl, true);
  addFact(body, 'Implementation', item.implementation, true);
  addFact(body, 'Sample', item.sample, true);
  addFact(body, 'Documentation', item.documentation, true);
  addFact(body, 'Tests', item.tests, true);
  addFact(body, 'First release', item.firstRelease);
  details.append(summary, body);
  return details;
}

function render() {
  const query = search.value.trim().toLocaleLowerCase();
  const filtered = entries.filter(item =>
    (!packageSelect.value || item.package === packageSelect.value) &&
    (!statusSelect.value || item.status === statusSelect.value) &&
    (!kindSelect.value || item.classification === kindSelect.value) &&
    (!query || [item.id, item.blueprintName, item.family, item.braceApi, item.artifact]
      .some(value => String(value || '').toLocaleLowerCase().includes(query)))
  );
  results.replaceChildren(...filtered.map(card));
  if (filtered.length === 0) results.append(el('p', 'empty', 'No inventory rows match these filters.'));
  resultCount.textContent = `${filtered.length} of ${entries.length} inventory rows`;
}

function captureAppearance(capture) {
  return capture.contrast === 'high' ? 'High contrast' : capture.theme === 'dark' ? 'Dark' : 'Light';
}

function captureCard(capture) {
  const item = entryById.get(capture.inventoryIds[0]);
  const article = el('article', `shot-card ${capture.format === 'landscape' ? 'shot-landscape' : ''}`);
  article.id = `capture-${capture.id}`;
  const media = el('a', `shot-media shot-${capture.theme}`);
  media.href = `./${capture.image}`;
  media.target = '_blank';
  media.rel = 'noopener noreferrer';
  media.setAttribute('aria-label', `Open full-size Android capture of ${item.blueprintName} in a new tab`);
  const screenshot = el('img');
  screenshot.src = `./${capture.image}`;
  screenshot.alt = capture.alt;
  screenshot.width = capture.pixelWidth;
  screenshot.height = capture.pixelHeight;
  screenshot.loading = 'lazy';
  screenshot.decoding = 'async';
  media.append(screenshot);

  const body = el('div', 'shot-body');
  const badges = el('div', 'shot-badges');
  badges.append(el('span', `pill ${item.status.replace(/\s+/g, '-')}`, item.status));
  badges.append(el('span', `shot-stage ${capture.sourceStage}`, capture.sourceStage === 'draft' ? 'Draft branch capture' : 'Merged source'));
  body.append(badges);
  body.append(el('p', 'shot-family', `${item.package} · ${item.family}`));
  body.append(el('h3', '', item.blueprintName));
  body.append(el('p', 'shot-api', item.braceApi));
  body.append(el('p', 'shot-caption', capture.caption));
  if (capture.inventoryIds.length > 1) {
    const also = capture.inventoryIds.slice(1).map(id => entryById.get(id).blueprintName).join(' · ');
    body.append(el('p', 'shot-also', `Also shown: ${also}`));
  }
  const meta = el('dl', 'shot-meta');
  for (const [label, value] of [
    ['Appearance', `${captureAppearance(capture)} · ${capture.density}`],
    ['Device', `${capture.device} · ${capture.pixelWidth} × ${capture.pixelHeight}`],
    ['Captured', capture.capturedAt],
    ['Source', `${capture.sourceBranch} @ ${capture.sourceCommit.slice(0, 8)}`],
  ]) {
    const pair = el('div');
    pair.append(el('dt', '', label), el('dd', '', value));
    meta.append(pair);
  }
  body.append(meta);
  const actions = el('div', 'shot-actions');
  const inventoryButton = el('button', 'shot-inventory', 'View inventory row →');
  inventoryButton.type = 'button';
  inventoryButton.dataset.inventoryId = item.id;
  const source = publicSourceRepository ? el('a', '', 'View source ↗') : el('span', 'shot-source-pending', 'Source link pending public repository');
  if (publicSourceRepository) {
    source.href = `${publicSourceRepository}/blob/${capture.sourceCommit}/${capture.sourceFile}`;
    source.rel = 'noopener noreferrer';
  }
  actions.append(inventoryButton, source);
  body.append(actions);
  const code = el('details', 'shot-code');
  code.append(el('summary', '', 'Compose usage example'));
  const codeWrap = el('div', 'shot-code-content');
  const pre = el('pre');
  const snippet = el('code', '', capture.usage);
  snippet.className = 'language-kotlin';
  pre.append(snippet);
  const copy = el('button', 'copy-usage', 'Copy Kotlin');
  copy.type = 'button';
  copy.dataset.copyUsage = capture.id;
  codeWrap.append(pre, copy);
  code.append(codeWrap);
  body.append(code);
  article.append(media, body);
  return article;
}

function renderShowcase() {
  const filtered = captures.filter(capture =>
    (!showcaseTheme.value || captureAppearance(capture) === showcaseTheme.value) &&
    (!showcaseFamily.value || entryById.get(capture.inventoryIds[0]).family === showcaseFamily.value)
  );
  showcaseGrid.replaceChildren(...filtered.map(captureCard));
  if (filtered.length === 0) showcaseGrid.append(el('p', 'empty', 'No captures match these filters.'));
  showcaseResult.textContent = `${filtered.length} of ${captures.length} Android captures`;
}

async function loadShowcase() {
  try {
    const response = await fetch('./showcase/captures.json');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    if (!Array.isArray(data.captures)) throw new Error('Capture manifest is missing');
    captures = data.captures;
    const appearances = [...new Set(captures.map(captureAppearance))];
    for (const appearance of ['Light', 'Dark', 'High contrast']) {
      if (appearances.includes(appearance)) {
        const option = el('option', '', appearance);
        option.value = appearance;
        showcaseTheme.append(option);
      }
    }
    addOptions(showcaseFamily, captures.map(capture => entryById.get(capture.inventoryIds[0]).family));
    heroSnapshotCount.textContent = `${captures.length} real catalog captures`;
    const featured = captures.find(capture => capture.inventoryIds.includes('core-button') && capture.theme === 'light' && capture.contrast === 'standard') || captures[0];
    if (featured) {
      const featuredLink = el('a');
      featuredLink.href = `#capture-${featured.id}`;
      featuredLink.setAttribute('aria-label', `See the ${entryById.get(featured.inventoryIds[0]).blueprintName} Android capture`);
      const featuredImage = el('img');
      featuredImage.src = `./${featured.image}`;
      featuredImage.alt = featured.alt;
      featuredImage.width = featured.pixelWidth;
      featuredImage.height = featured.pixelHeight;
      featuredImage.decoding = 'async';
      featuredLink.append(featuredImage);
      heroImage.replaceChildren(featuredLink);
    }
    renderShowcase();
  } catch (error) {
    heroSnapshotCount.textContent = 'Gallery unavailable';
    showcaseResult.textContent = 'Captures could not be loaded';
    showcaseGrid.append(el('p', 'empty', `Build the site with node scripts/build-docs.mjs. ${error.message}`));
  }
}

function revealHash() {
  const fragment = decodeURIComponent(location.hash.slice(1));
  if (fragment.startsWith('component-')) {
    const id = fragment.slice('component-'.length);
    if (!entryById.has(id)) return;
    search.value = id;
    packageSelect.value = '';
    statusSelect.value = '';
    kindSelect.value = '';
    render();
    const match = document.getElementById(fragment);
    if (match) { match.open = true; match.scrollIntoView(); }
  } else if (fragment.startsWith('capture-')) {
    if (!captures.some(capture => `capture-${capture.id}` === fragment)) return;
    showcaseTheme.value = '';
    showcaseFamily.value = '';
    renderShowcase();
    document.getElementById(fragment)?.scrollIntoView();
  }
}

async function load() {
  try {
    const configResponse = await fetch('./site-config.json');
    if (!configResponse.ok) throw new Error(`Site config HTTP ${configResponse.status}`);
    const config = await configResponse.json();
    publicSourceRepository = config.publicSourceRepository;
    repository = publicSourceRepository ? `${publicSourceRepository}/blob/main/` : null;
    const response = await fetch('./coverage.json');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    if (!Array.isArray(data.entries) || data.entries.length === 0) throw new Error('Inventory entries are missing');
    entries = data.entries;
    entryById = new Map(entries.map(item => [item.id, item]));
    await loadShowcase();
    const pin = data.baseline || {};
    baseline.textContent = `Blueprint baseline: ${pin.releaseTag || pin.version || 'pinned stable'} · ${pin.commit || pin.sha || 'commit recorded in inventory'}`;
    const counts = data.summary || {};
    stats.replaceChildren(
      stat(`${counts.stableApplicableRows ?? 0}/${counts.applicableRows ?? entries.length}`, 'Applicable rows stable'),
      stat(`${counts.stableComponents ?? 0}/${counts.applicableComponents ?? 0}`, 'Components stable'),
      stat(`${counts.documentedWebSpecificMappings ?? 0}/${counts.webSpecificMappings ?? 0}`, 'Web mappings documented'),
      stat(counts.labsRows ?? entries.filter(item => item.track === 'labs').length, 'Labs rows tracked')
    );
    addOptions(packageSelect, entries.map(item => item.package));
    addOptions(statusSelect, entries.map(item => item.status));
    addOptions(kindSelect, entries.map(item => item.classification));
    render();
    revealHash();
  } catch (error) {
    baseline.textContent = 'Coverage data could not be loaded.';
    resultCount.textContent = 'The generated inventory is unavailable.';
    results.append(el('p', 'empty', `Build the site with node scripts/build-docs.mjs. ${error.message}`));
  }
}

window.addEventListener('hashchange', revealHash);
for (const control of [search, packageSelect, statusSelect, kindSelect]) control.addEventListener('input', render);
for (const control of [showcaseTheme, showcaseFamily]) control.addEventListener('input', renderShowcase);
heroImage.addEventListener('click', () => {
  showcaseTheme.value = '';
  showcaseFamily.value = '';
  renderShowcase();
});
results.addEventListener('click', event => {
  if (!event.target.closest('[data-capture-id]')) return;
  showcaseTheme.value = '';
  showcaseFamily.value = '';
  renderShowcase();
});
showcaseGrid.addEventListener('click', async event => {
  const inventoryButton = event.target.closest('[data-inventory-id]');
  if (inventoryButton) {
    search.value = inventoryButton.dataset.inventoryId;
    packageSelect.value = '';
    statusSelect.value = '';
    kindSelect.value = '';
    render();
    const result = document.querySelector(`#component-${inventoryButton.dataset.inventoryId}`);
    if (result) {
      result.open = true;
      result.querySelector('summary')?.focus({ preventScroll: true });
    }
    history.replaceState(null, '', `#component-${inventoryButton.dataset.inventoryId}`);
    document.querySelector('#coverage').scrollIntoView();
    return;
  }
  const copyButton = event.target.closest('[data-copy-usage]');
  if (!copyButton) return;
  const capture = captures.find(item => item.id === copyButton.dataset.copyUsage);
  if (!capture) return;
  try {
    await navigator.clipboard.writeText(capture.usage);
    copyButton.textContent = 'Copied';
  } catch {
    copyButton.textContent = 'Select code to copy';
  }
});
load();
