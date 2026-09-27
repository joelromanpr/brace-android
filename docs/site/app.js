const search = document.querySelector('#search');
const familySelect = document.querySelector('#package');
const statusSelect = document.querySelector('#status');
const kindSelect = document.querySelector('#kind');
const results = document.querySelector('#components');
const resultCount = document.querySelector('#result-count');
const stats = document.querySelector('#stats');
let publicSourceRepository = null;
let repository = null;
let guideMap = {};
const showcaseGrid = document.querySelector('#showcase-grid');
const showcaseTheme = document.querySelector('#showcase-theme');
const showcaseFamily = document.querySelector('#showcase-family');
const showcaseResult = document.querySelector('#showcase-result');
const showcaseMore = document.querySelector('#showcase-more');
const showcaseMoreWrap = document.querySelector('#showcase-more-wrap');
const featuredCaptureCount = 8;
let showcaseExpanded = false;
const statusChips = document.querySelector('#status-chips');
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
    const guide = guideMap[path];
    const href = /^https:\/\//.test(value) ? value : guide ? `./${guide}${fragment ? `#${fragment}` : ''}` : repository ? repository + value.replace(/^\/+/, '') : null;
    if (href) {
      const text = ({ Code: 'View code ↗', 'Catalog sample': 'View sample ↗', Tests: 'View tests ↗', 'Status record': 'Full coverage record ↗' })[label] || (guide ? 'Read guide ↗' : 'View public source ↗');
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
  const title = el('span', 'component-name', (item.braceApi || item.blueprintName || item.id).split(' / ')[0]);
  title.append(el('span', 'component-family', `${item.package || 'Unassigned'} · ${item.family || 'General'}`));
  const badge = el('span', `pill ${String(item.status || 'planned').replace(/\s+/g, '-')}`, item.status || 'planned');
  summary.append(title, badge);
  const body = el('dl', 'component-body');
  addFact(body, 'Brace API', item.braceApi);
  addFact(body, 'How it works', item.behavior);
  const visual = captures.find(capture => capture.inventoryIds.includes(item.id));
  if (visual) {
    const visualFact = el('div', 'fact');
    const visualDescription = el('dd');
    const visualLink = el('a', '', 'See Android screenshot ↗');
    visualLink.href = `#capture-${visual.id}`;
    visualLink.dataset.captureId = visual.id;
    visualDescription.append(visualLink);
    visualFact.append(el('dt', '', 'Android screenshot'), visualDescription);
    body.append(visualFact);
  }
  addFact(body, 'Module', item.artifact);
  addFact(body, 'Code', item.implementation, true);
  addFact(body, 'Catalog sample', item.sample, true);
  addFact(body, 'Guide', item.documentation, true);
  addFact(body, 'Tests', item.tests, true);
  addFact(body, 'First release', item.firstRelease);
  addFact(body, 'Status record', 'docs/coverage.md', true);
  details.append(summary, body);
  return details;
}

function render() {
  const query = search.value.trim().toLocaleLowerCase();
  const filtered = entries.filter(item =>
    (!familySelect.value || item.family === familySelect.value) &&
    (!statusSelect.value || item.status === statusSelect.value) &&
    (!kindSelect.value || item.kind === kindSelect.value) &&
    (!query || [item.id, item.blueprintName, item.family, item.braceApi, item.artifact]
      .some(value => String(value || '').toLocaleLowerCase().includes(query)))
  );
  results.replaceChildren(...filtered.map(card));
  if (filtered.length === 0) results.append(el('p', 'empty', 'No components match these filters.'));
  resultCount.textContent = `${filtered.length} of ${entries.length} items`;
  for (const chip of statusChips.querySelectorAll('[data-status]')) {
    chip.setAttribute('aria-pressed', String(chip.dataset.status === statusSelect.value));
  }
}

function renderStatusChips() {
  const statuses = ['', 'in progress', 'planned', 'experimental', 'stable'];
  statusChips.replaceChildren(...statuses.map(status => {
    const count = status ? entries.filter(item => item.status === status).length : entries.length;
    const button = el('button', 'status-chip');
    button.type = 'button';
    button.dataset.status = status;
    button.setAttribute('aria-pressed', String(status === statusSelect.value));
    button.textContent = `${status ? status.replace(/^./, c => c.toUpperCase()) : 'All'} (${count})`;
    return button;
  }));
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
  media.setAttribute('aria-label', `Open full-size Android capture of ${capture.title || item.blueprintName} in a new tab`);
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
  body.append(badges);
  body.append(el('p', 'shot-family', `${item.package} · ${item.family}`));
  body.append(el('h3', '', capture.title || item.blueprintName));
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
  const inventoryButton = el('button', 'shot-inventory', 'View component details →');
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
  const hasFilter = Boolean(showcaseTheme.value || showcaseFamily.value);
  const visible = hasFilter || showcaseExpanded ? filtered : filtered.slice(0, featuredCaptureCount);
  showcaseGrid.replaceChildren(...visible.map(captureCard));
  if (filtered.length === 0) showcaseGrid.append(el('p', 'empty', 'No screenshots match these filters.'));
  showcaseResult.textContent = `Showing ${visible.length} of ${captures.length} Android screenshots`;
  showcaseMoreWrap.hidden = hasFilter || captures.length <= featuredCaptureCount;
  showcaseMore.setAttribute('aria-expanded', String(showcaseExpanded));
  showcaseMore.textContent = showcaseExpanded
    ? `Show ${Math.min(featuredCaptureCount, captures.length)} featured captures`
    : `Show all ${captures.length} captures`;
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
    renderShowcase();
  } catch (error) {
    showcaseResult.textContent = 'Captures could not be loaded';
    showcaseGrid.append(el('p', 'empty', `Build the site with node scripts/build-docs.mjs. ${error.message}`));
  }
}

function revealCapture(id) {
  const index = captures.findIndex(capture => capture.id === id);
  if (index < 0) return null;
  showcaseExpanded ||= index >= featuredCaptureCount;
  showcaseTheme.value = '';
  showcaseFamily.value = '';
  renderShowcase();
  return document.getElementById(`capture-${id}`);
}

function revealHash() {
  const fragment = decodeURIComponent(location.hash.slice(1));
  if (fragment.startsWith('component-')) {
    const id = fragment.slice('component-'.length);
    if (!entryById.has(id)) return;
    search.value = id;
    familySelect.value = '';
    statusSelect.value = '';
    kindSelect.value = '';
    render();
    const match = document.getElementById(fragment);
    if (match) { match.open = true; match.scrollIntoView(); }
  } else if (fragment.startsWith('capture-')) {
    revealCapture(fragment.slice('capture-'.length))?.scrollIntoView();
  }
}

async function load() {
  try {
    const configResponse = await fetch('./site-config.json');
    if (!configResponse.ok) throw new Error(`Site config HTTP ${configResponse.status}`);
    const config = await configResponse.json();
    publicSourceRepository = config.publicSourceRepository;
    repository = publicSourceRepository ? `${publicSourceRepository}/blob/main/` : null;
    const guideResponse = await fetch('./guide-map.json');
    if (!guideResponse.ok) throw new Error(`Guide map HTTP ${guideResponse.status}`);
    guideMap = await guideResponse.json();
    if (!guideMap || Array.isArray(guideMap) || typeof guideMap !== 'object') throw new Error('Guide map is invalid');
    const response = await fetch('./coverage.json');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    if (!Array.isArray(data.entries) || data.entries.length === 0) throw new Error('Inventory entries are missing');
    entries = data.entries;
    entryById = new Map(entries.map(item => [item.id, item]));
    await loadShowcase();
    const pictured = new Set(captures.flatMap(capture => capture.inventoryIds));
    const rank = item => item.status === 'stable' ? 0 : pictured.has(item.id) ? 1 : item.kind === 'component' ? 2 : 3;
    entries.sort((a, b) => rank(a) - rank(b) ||
      String(a.family).localeCompare(String(b.family)) ||
      String(a.braceApi || a.blueprintName).localeCompare(String(b.braceApi || b.blueprintName)));
    const counts = data.summary || {};
    stats.replaceChildren(
      stat(`${counts.stableApplicableRows ?? 0}/${counts.applicableRows ?? entries.length}`, 'Stable Android items'),
      stat(`${counts.stableComponents ?? 0}/${counts.applicableComponents ?? 0}`, 'Stable components'),
      stat(`${counts.documentedWebSpecificMappings ?? 0}/${counts.webSpecificMappings ?? 0}`, 'Web behaviors explained'),
      stat(counts.labsRows ?? entries.filter(item => item.track === 'labs').length, 'Early experiments')
    );
    addOptions(familySelect, entries.map(item => item.family));
    addOptions(statusSelect, entries.map(item => item.status));
    renderStatusChips();
    render();
    revealHash();
  } catch (error) {
    resultCount.textContent = 'The generated inventory is unavailable.';
    results.append(el('p', 'empty', `Build the site with node scripts/build-docs.mjs. ${error.message}`));
  }
}

window.addEventListener('hashchange', revealHash);
for (const control of [search, familySelect, statusSelect, kindSelect]) control.addEventListener('input', render);
for (const control of [showcaseTheme, showcaseFamily]) control.addEventListener('input', renderShowcase);
statusChips.addEventListener('click', event => {
  const chip = event.target.closest('[data-status]');
  if (!chip) return;
  statusSelect.value = chip.dataset.status;
  render();
});
results.addEventListener('click', event => {
  const link = event.target.closest('[data-capture-id]');
  if (link) revealCapture(link.dataset.captureId);
});
showcaseMore.addEventListener('click', () => {
  showcaseExpanded = !showcaseExpanded;
  renderShowcase();
});
showcaseGrid.addEventListener('click', async event => {
  const inventoryButton = event.target.closest('[data-inventory-id]');
  if (inventoryButton) {
    search.value = inventoryButton.dataset.inventoryId;
    familySelect.value = '';
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
