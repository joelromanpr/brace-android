const search = document.querySelector('#search');
const packageSelect = document.querySelector('#package');
const statusSelect = document.querySelector('#status');
const kindSelect = document.querySelector('#kind');
const results = document.querySelector('#components');
const resultCount = document.querySelector('#result-count');
const stats = document.querySelector('#stats');
const baseline = document.querySelector('#baseline');
const repository = 'https://github.com/joelromanpr/brace-android/blob/main/';
let entries = [];

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
    const link = el('a', '', label === 'Blueprint documentation' ? 'Open Blueprint docs ↗' : label === 'Pinned Blueprint source' ? 'Open pinned source ↗' : 'View in repository ↗');
    const [path, fragment] = String(value).split('#', 2);
    const guide = { 'docs/core-components.md': 'core-components.html', 'docs/button-group.md': 'button-group.html', 'docs/content-feedback.md': 'content-feedback.html', 'docs/navigation-feedback.md': 'navigation-feedback.html', 'docs/overlays.md': 'overlays.html', 'docs/drawers-popovers.md': 'drawers-popovers.html', 'docs/tooltip-toast.md': 'tooltip-toast.html', 'docs/context-shortcuts.md': 'context-shortcuts.html', 'docs/form-text.md': 'form-text.html', 'docs/form-layout.md': 'form-layout.html', 'docs/numeric-input.md': 'numeric-input.html', 'docs/icons.md': 'icons.html', 'docs/select-query.md': 'select-query.html', 'docs/theming.md': 'theming.html', 'docs/installation.md': 'installation.html', 'docs/compatibility.md': 'compatibility.html', 'docs/attribution.md': 'attribution.html' }[path];
    link.href = /^https:\/\//.test(value) ? value : guide ? `./${guide}${fragment ? `#${fragment}` : ''}` : repository + value.replace(/^\/+/, '');
    link.rel = 'noopener noreferrer';
    description.append(link);
  } else description.textContent = value;
  wrap.append(term, description);
  grid.append(wrap);
}

function card(item) {
  const details = el('details', 'component');
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

async function load() {
  try {
    const response = await fetch('./coverage.json');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    if (!Array.isArray(data.entries) || data.entries.length === 0) throw new Error('Inventory entries are missing');
    entries = data.entries;
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
  } catch (error) {
    baseline.textContent = 'Coverage data could not be loaded.';
    resultCount.textContent = 'The generated inventory is unavailable.';
    results.append(el('p', 'empty', `Build the site with node scripts/build-docs.mjs. ${error.message}`));
  }
}

for (const control of [search, packageSelect, statusSelect, kindSelect]) control.addEventListener('input', render);
load();
