#!/usr/bin/env python3
"""Validate the pinned Blueprint inventory and generate public coverage views.

Run ``python3 scripts/generate_coverage.py`` after changing inventory rows.
CI runs the same command with ``--check`` to reject stale generated files.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import quote


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "inventory/blueprint-components.json"
NAVIGATION = ROOT / "inventory/blueprint-nav.json"
OUTPUTS = (
    ROOT / "docs/coverage.json",
    ROOT / "catalog/src/main/assets/coverage.json",
)
MARKDOWN = ROOT / "docs/coverage.md"
README = ROOT / "README.md"
README_START = "<!-- coverage:begin -->"
README_END = "<!-- coverage:end -->"
STATUSES = {"planned", "in progress", "experimental", "stable"}
CLASSIFICATIONS = {"direct", "adaptation", "web-specific"}
PACKAGES = {"colors", "core", "icons", "datetime", "select", "table", "labs"}
SEMVER = re.compile(
    r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)"
    r"(?:-(?:0|[1-9][0-9]*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*)"
    r"(?:\.(?:0|[1-9][0-9]*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*))*)?"
    r"(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?"
)
REQUIRED = {
    "id", "package", "family", "kind", "track", "blueprintName",
    "blueprintUrl", "pinnedSourceUrl", "sourcePage", "braceApi", "artifact", "behavior",
    "classification", "reason", "milestone", "priority", "status",
    "implementation", "sample", "documentation", "tests", "firstRelease",
}


def fail(message: str) -> None:
    raise ValueError(message)


def validate_path(row: dict, field: str) -> None:
    path = row[field]
    if path is None:
        return
    if not isinstance(path, str) or not path or path.startswith(("/", "http:", "https:")):
        fail(f"{row['id']}: {field} must be a repository-relative file path")
    resolved = (ROOT / path).resolve()
    if not resolved.is_relative_to(ROOT) or not resolved.is_file():
        fail(f"{row['id']}: {field} does not exist: {path}")


def validate(data: dict) -> None:
    if data.get("schemaVersion") != 1:
        fail("unsupported inventory schemaVersion")
    baseline = data.get("baseline", {})
    if not re.fullmatch(r"[0-9a-f]{40}", baseline.get("commit", "")):
        fail("baseline must pin a full 40-character commit")
    if not baseline.get("releaseTag") or not baseline.get("packageVersions"):
        fail("baseline release tag and package versions are required")
    nav_bytes = NAVIGATION.read_bytes()
    if hashlib.sha256(nav_bytes).hexdigest() != baseline.get("navigationSha256"):
        fail("pinned Blueprint navigation snapshot hash changed")
    nav = json.loads(nav_bytes)
    pages_from_nav = []
    for package in nav:
        if package["package"] == "blueprint":
            continue
        pages_from_nav.extend(f"{package['package']}/{page}" for page in package.get("pages", []))
        for section in package.get("sections", []):
            prefix = section.get("routeAlias", section["section"])
            pages_from_nav.extend(f"{package['package']}/{prefix}/{page}" for page in section["pages"])
    pages = baseline.get("documentedPages", [])
    if pages != pages_from_nav or len(pages) != len(set(pages)) or len(pages) != 85:
        fail("inventory baseline pages differ from the pinned Blueprint navigation")
    source_files = baseline.get("sourceFiles", {})
    if set(source_files) != set(pages):
        fail("every pinned page needs a source MDX path")
    if any(not path.startswith("packages/") or not path.endswith(".mdx") for path in source_files.values()):
        fail("invalid pinned source MDX path")
    rows = data.get("entries", [])
    if not rows:
        fail("inventory has no entries")
    ids = set()
    covered_pages = set()
    for row in rows:
        missing = REQUIRED - row.keys()
        if missing:
            fail(f"{row.get('id', '<unknown>')}: missing {sorted(missing)}")
        row_id = row["id"]
        if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", row_id):
            fail(f"{row_id}: id must be a lowercase slug")
        if row_id in ids:
            fail(f"duplicate id: {row_id}")
        ids.add(row_id)
        if row["package"] not in PACKAGES:
            fail(f"{row_id}: unknown Blueprint package")
        if row["kind"] not in {"component", "capability"}:
            fail(f"{row_id}: invalid kind")
        if row["track"] not in {"main", "labs"}:
            fail(f"{row_id}: invalid track")
        if (row["package"] == "labs") != (row["track"] == "labs"):
            fail(f"{row_id}: labs entries must be on the labs track")
        if row["status"] not in STATUSES or row["classification"] not in CLASSIFICATIONS:
            fail(f"{row_id}: invalid status or classification")
        if not row["blueprintUrl"].startswith("https://blueprintjs.com/docs/#"):
            fail(f"{row_id}: invalid Blueprint docs URL")
        if row["blueprintUrl"].split("#", 1)[1] != row["sourcePage"]:
            fail(f"{row_id}: source page and docs URL differ")
        if row["sourcePage"] not in pages:
            fail(f"{row_id}: source page missing from pinned navigation")
        pinned_url = "https://github.com/palantir/blueprint/blob/" + baseline["commit"] + "/" + source_files[row["sourcePage"]]
        if row["pinnedSourceUrl"] != pinned_url:
            fail(f"{row_id}: pinned source URL differs from baseline source file")
        if row.get("pinnedAssetUrl") and not row["pinnedAssetUrl"].startswith(
            "https://github.com/palantir/blueprint/blob/" + baseline["commit"] + "/packages/"
        ):
            fail(f"{row_id}: pinned asset URL must use the baseline commit")
        covered_pages.add(row["sourcePage"])
        for field in ("blueprintName", "braceApi", "artifact", "behavior", "milestone", "priority"):
            if not isinstance(row[field], str) or not row[field].strip():
                fail(f"{row_id}: {field} is empty")
        if row["classification"] != "direct" and not row["reason"].strip():
            fail(f"{row_id}: adaptation or web-specific API needs a reason")
        for field in ("implementation", "sample", "documentation", "tests"):
            validate_path(row, field)
        release = row["firstRelease"]
        if release is not None and (not isinstance(release, str) or not SEMVER.fullmatch(release)):
            fail(f"{row_id}: firstRelease must be SemVer")
        if row["status"] == "stable":
            evidence = ("documentation",) if row["classification"] == "web-specific" else (
                "implementation", "sample", "documentation", "tests", "firstRelease"
            )
            for field in evidence:
                if not row[field]:
                    fail(f"{row_id}: stable status requires {field}")
    uncovered = set(pages) - covered_pages
    if uncovered:
        fail(f"unpinned or unrepresented docs pages: {sorted(uncovered)}")


def make_summary(rows: list[dict], documented_pages: int) -> dict:
    main = [r for r in rows if r["track"] == "main"]
    applicable = [r for r in main if r["classification"] != "web-specific"]
    components = [r for r in applicable if r["kind"] == "component"]
    capabilities = [r for r in applicable if r["kind"] == "capability"]
    mapped = [r for r in main if r["classification"] == "web-specific"]
    labs = [r for r in rows if r["track"] == "labs"]
    package_counts = {}
    for package in sorted({r["package"] for r in rows}):
        group = [r for r in rows if r["package"] == package]
        package_counts[package] = {
            "total": len(group),
            "stable": sum(r["status"] == "stable" for r in group),
            "status": dict(sorted(Counter(r["status"] for r in group).items())),
        }
    return {
        "totalRows": len(rows),
        "documentedPages": documented_pages,
        "applicableRows": len(applicable),
        "stableApplicableRows": sum(r["status"] == "stable" for r in applicable),
        "applicableComponents": len(components),
        "stableComponents": sum(r["status"] == "stable" for r in components),
        "applicableCapabilities": len(capabilities),
        "stableCapabilities": sum(r["status"] == "stable" for r in capabilities),
        "webSpecificMappings": len(mapped),
        "documentedWebSpecificMappings": sum(bool(r["documentation"]) for r in mapped),
        "stableWebSpecificMappings": sum(r["status"] == "stable" for r in mapped),
        "labsRows": len(labs),
        "stableLabsRows": sum(r["status"] == "stable" for r in labs),
        "byPackage": package_counts,
        "fullApplicableCoverage": bool(applicable) and all(r["status"] == "stable" for r in applicable) and all(r["status"] == "stable" for r in mapped),
    }


def escape(value: object) -> str:
    return str(value).replace("|", "\\|").replace("\n", " ")


def link(path: str | None, label: str) -> str:
    return f"[{label}](../{path})" if path else "—"


def markdown(data: dict) -> str:
    baseline, summary, rows = data["baseline"], data["summary"], data["entries"]
    lines = [
        "<!-- Generated by scripts/generate_coverage.py. Edit inventory/blueprint-components.json. -->",
        "# Blueprint coverage",
        "",
        f"Baseline: [Blueprint `{baseline['releaseTag']}`](https://github.com/palantir/blueprint/releases/tag/{quote(baseline['releaseTag'], safe='@')}), commit [`{baseline['commit']}`](https://github.com/palantir/blueprint/commit/{baseline['commit']}).",
        "",
        "The inventory follows the pinned source documentation navigation, with nested public components split into rows. Colors, typography, other design-system capabilities, and table behaviors have explicit rows. Blueprint labs are tracked separately. A row is **stable** only when its implementation, interactive sample, documentation, tests, and first release are recorded; web-only APIs require a documented Compose mapping. First shipped records when code first appeared in an artifact; an alpha appearance does not mean the API is stable. Counts are generated from the inventory. Planned and in-progress APIs do not count as stable.",
        "",
        f"**Stable applicable rows: {summary['stableApplicableRows']}/{summary['applicableRows']}** ({summary['stableComponents']}/{summary['applicableComponents']} components; {summary['stableCapabilities']}/{summary['applicableCapabilities']} capabilities).",
        "",
        f"Web-specific mappings documented: {summary['documentedWebSpecificMappings']}/{summary['webSpecificMappings']} (stable: {summary['stableWebSpecificMappings']}). Labs rows: {summary['labsRows']} (stable: {summary['stableLabsRows']}). Full applicable coverage: **{'yes' if summary['fullApplicableCoverage'] else 'no'}**.",
        "",
        "## Package status",
        "",
        "| Blueprint package | Rows | Stable | Planned | In progress | Experimental |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for package, counts in summary["byPackage"].items():
        status = counts["status"]
        lines.append(f"| {package} | {counts['total']} | {counts['stable']} | {status.get('planned', 0)} | {status.get('in progress', 0)} | {status.get('experimental', 0)} |")
    lines += ["", "## Inventory", ""]
    grouped: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for row in rows:
        grouped[(row["track"], row["family"])].append(row)
    for (track, family), group in sorted(grouped.items(), key=lambda item: (item[0][0] == "labs", item[0][1])):
        lines += [f"### {family.title()}" + (" (Blueprint labs)" if track == "labs" else ""), "",
                  "| Blueprint | Brace API / artifact | Android behavior and accessibility | Mapping | Milestone / priority | Status | Evidence |",
                  "| --- | --- | --- | --- | --- | --- | --- |"]
        for row in sorted(group, key=lambda r: r["blueprintName"].lower()):
            evidence = ", ".join(filter(lambda x: x != "—", [
                link(row["implementation"], "code"), link(row["sample"], "sample"),
                link(row["documentation"], "docs"), link(row["tests"], "tests"),
            ])) or "—"
            mapping = row["classification"]
            if row["reason"]:
                mapping += ": " + row["reason"]
            if row["firstRelease"]:
                evidence += f"; first shipped {row['firstRelease']}"
            lines.append("| " + " | ".join(map(escape, [
                f"[{row['blueprintName']}]({row['blueprintUrl']}) ([pinned source]({row['pinnedSourceUrl']}))" +
                (f" ([pinned asset]({row['pinnedAssetUrl']}))" if row.get("pinnedAssetUrl") else ""),
                f"`{row['braceApi']}` / `{row['artifact']}`",
                row["behavior"], mapping,
                f"{row['milestone']} / {row['priority']}", row["status"], evidence,
            ])) + " |")
        lines.append("")
    lines += [
        "## Updating coverage",
        "",
        "Claim a row in `inventory/blueprint-components.json`, implement it, and update evidence fields in the same pull request. Run `python3 scripts/generate_coverage.py`; CI checks generated files with `--check`. Record the first shipped version when code reaches an artifact. Stable status additionally requires a working API, interactive sample, documentation, and meaningful tests; an alpha first release alone is not stable. See [baseline methodology](../BLUEPRINT_BASELINE.md).",
        "",
    ]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail when generated outputs differ")
    args = parser.parse_args()
    data = json.loads(SOURCE.read_text())
    validate(data)
    data = {**data, "summary": make_summary(data["entries"], len(data["baseline"]["documentedPages"]))}
    generated = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    outputs = {path: generated for path in OUTPUTS}
    outputs[MARKDOWN] = markdown(data)
    readme = README.read_text()
    if readme.count(README_START) != 1 or readme.count(README_END) != 1:
        fail("README must contain one generated coverage block")
    start = readme.index(README_START) + len(README_START)
    end = readme.index(README_END)
    if start > end:
        fail("README coverage markers are out of order")
    summary = data["summary"]
    coverage_lines = (
        f"\n**Component status:** {summary['stableComponents']} of {summary['applicableComponents']} components "
        f"and {summary['stableCapabilities']} of {summary['applicableCapabilities']} design tools have passed stable review. "
        f"[Check individual readiness](docs/coverage.md).\n"
    )
    outputs[README] = readme[:start] + coverage_lines + readme[end:]
    stale = []
    for path, content in outputs.items():
        if args.check:
            if not path.is_file() or path.read_text() != content:
                stale.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
    if stale:
        print("stale generated coverage: " + ", ".join(stale), file=sys.stderr)
        return 1
    summary = data["summary"]
    print(f"coverage: {summary['stableApplicableRows']}/{summary['applicableRows']} applicable rows stable; {summary['stableComponents']}/{summary['applicableComponents']} components stable")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ValueError, OSError, json.JSONDecodeError) as error:
        print(f"coverage error: {error}", file=sys.stderr)
        raise SystemExit(1)
