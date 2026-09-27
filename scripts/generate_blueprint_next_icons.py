#!/usr/bin/env python3
"""Generate and audit the public @blueprintjs/icons/next artwork at the Blueprint pin.

``--check`` verifies committed artifacts without a checkout. Add ``--upstream PATH`` to
compare each source SVG, manifest, legacy migration map, and license byte for byte.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "brace-blueprint-icons-next/src/main/assets"
MANIFEST = ASSETS / "brace-blueprint-icons-next.json"
LICENSE = ASSETS / "blueprint-icons-next-LICENSE.txt"
NAMES = ROOT / "brace-blueprint-icons-next/src/main/java/io/github/joelromanpr/brace/blueprinticonsnext/BraceBlueprintNextIconNames.kt"
PIN = "a60d4c92257612808fbfac81cfeee4fcba91a8b4"
EXPECTED_OUTLINED = 695
EXPECTED_FILLED = 386
EXPECTED_LEGACY_MAP = 706
NAME = re.compile(r"[a-z][a-z0-9]*(?:-[a-z0-9]+)*\Z")
SVG_NS = "{http://www.w3.org/2000/svg}"


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def compact(value: object) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n"


def vector(path: Path) -> dict:
    raw = path.read_bytes()
    root = ET.fromstring(raw)
    if root.tag != SVG_NS + "svg" or set(root.attrib) != {"viewBox"}:
        raise ValueError(f"Unexpected SVG root: {path}")
    view_box = [float(value) for value in root.attrib["viewBox"].split()]
    if len(view_box) != 4 or view_box[:2] != [0.0, 0.0] or min(view_box[2:]) <= 0:
        raise ValueError(f"Unexpected SVG viewBox: {path}")
    children = list(root)
    paths = [child for child in children if child.tag == SVG_NS + "path"]
    if any(set(child.attrib) != {"d"} for child in paths) or len(paths) > 1:
        raise ValueError(f"Unexpected SVG path: {path}")
    # The source has one <rect height="16"/> before magnifying-glass-lines's filled
    # path. SVG's default rect width is zero, so it draws no pixels and is omitted.
    inert_rects = [child for child in children if child.tag == SVG_NS + "rect"]
    if inert_rects and (path.name != "magnifying-glass-lines.svg" or
                      path.parent.name != "filled" or len(inert_rects) != 1 or
                      inert_rects[0].attrib != {"height": "16"}):
        raise ValueError(f"Unexpected rect in SVG: {path}")
    if len(children) != len(paths) + len(inert_rects):
        raise ValueError(f"Unexpected SVG child: {path}")
    if not paths and (path.name != "blank.svg" or path.parent.name != "outlined"):
        raise ValueError(f"Unexpected empty SVG: {path}")
    return {
        "viewBox": [int(v) if v.is_integer() else v for v in view_box],
        "path": paths[0].attrib["d"] if paths else "",
        "svgSha256": sha(raw),
    }


def from_upstream(path: Path) -> dict:
    commit = subprocess.check_output(["git", "-C", str(path), "rev-parse", "HEAD"], text=True).strip()
    if commit != PIN:
        raise ValueError(f"Blueprint checkout {commit} differs from {PIN}")
    dirty = subprocess.check_output(["git", "-C", str(path), "status", "--porcelain", "--untracked-files=no"], text=True).strip()
    if dirty:
        raise ValueError("Pinned Blueprint checkout has modified tracked files")
    package = path / "packages/icons"
    metadata = package / "icons-next.json"
    legacy_metadata = package / "icons.json"
    name_map = package / "icons-name-map.json"
    upstream_license = package / "LICENSE"
    rows = json.loads(metadata.read_text())
    legacy = json.loads(legacy_metadata.read_text())
    aliases = json.loads(name_map.read_text())
    if len(rows) != EXPECTED_OUTLINED or sum(row["hasFilled"] for row in rows) != EXPECTED_FILLED:
        raise ValueError("Unexpected next glyph counts")
    if len(aliases) != EXPECTED_LEGACY_MAP or set(aliases) != {row["iconName"] for row in legacy}:
        raise ValueError("Incomplete legacy-to-next map")
    names = {row["name"] for row in rows}
    if len(names) != len(rows) or set(aliases.values()) != names:
        raise ValueError("Next names or aliases mismatch")
    directory = path / "resources/icons/next"
    result = []
    for row in rows:
        name = row["name"]
        if not NAME.fullmatch(name) or not isinstance(row["hasFilled"], bool) or not isinstance(row["tags"], list):
            raise ValueError(f"Unexpected next metadata: {name}")
        outlined = vector(directory / "outlined" / f"{name}.svg")
        filled = vector(directory / "filled" / f"{name}.svg") if row["hasFilled"] else None
        result.append({"name": name, "tags": row["tags"], "outlined": outlined, "filled": filled})
    if {p.stem for p in (directory / "outlined").glob("*.svg")} != names:
        raise ValueError("Outlined SVG set differs from metadata")
    if {p.stem for p in (directory / "filled").glob("*.svg")} != {row["name"] for row in rows if row["hasFilled"]}:
        raise ValueError("Filled SVG set differs from metadata")
    return {
        "schemaVersion": 1,
        "source": {
            "project": "Palantir Blueprint",
            "release": "@blueprintjs/icons@6.13.0 (Blueprint core 6.18.0 pin)",
            "commit": PIN,
            "subpath": "@blueprintjs/icons/next",
            "metadataPath": "packages/icons/icons-next.json",
            "metadataSha256": sha(metadata.read_bytes()),
            "nameMapPath": "packages/icons/icons-name-map.json",
            "nameMapSha256": sha(name_map.read_bytes()),
            "licensePath": "packages/icons/LICENSE",
            "licenseSha256": sha(upstream_license.read_bytes()),
            "svgPathTemplate": "resources/icons/next/{variant}/{name}.svg",
            "license": "Apache-2.0",
            "transformation": "SVG paths/viewBoxes transcribed; one zero-width source rect has no pixels and is omitted.",
        },
        "legacyNameMap": aliases,
        "icons": result,
    }


def validate(manifest: dict) -> None:
    if manifest.get("schemaVersion") != 1 or manifest.get("source", {}).get("commit") != PIN:
        raise ValueError("Unexpected manifest schema or source pin")
    source = manifest["source"]
    if source.get("subpath") != "@blueprintjs/icons/next" or source.get("license") != "Apache-2.0":
        raise ValueError("Unexpected next subpath or license")
    for key in ("metadataSha256", "nameMapSha256", "licenseSha256"):
        if not re.fullmatch(r"[0-9a-f]{64}", source.get(key, "")):
            raise ValueError(f"Missing {key}")
    rows = manifest.get("icons", [])
    names = [row["name"] for row in rows]
    if len(rows) != EXPECTED_OUTLINED or len(set(names)) != len(names) or any(not NAME.fullmatch(n) for n in names):
        raise ValueError("Unexpected next glyph names")
    ids = ["".join(piece.capitalize() for piece in name.split("-")) for name in names]
    if len(set(ids)) != len(ids):
        raise ValueError("Generated Kotlin name collision")
    if sum(row["filled"] is not None for row in rows) != EXPECTED_FILLED:
        raise ValueError("Unexpected filled glyph count")
    aliases = manifest.get("legacyNameMap", {})
    if len(aliases) != EXPECTED_LEGACY_MAP or any(not NAME.fullmatch(n) for n in aliases) or set(aliases.values()) != set(names):
        raise ValueError("Incomplete legacy migration map")
    for row in rows:
        if not isinstance(row.get("tags"), list) or any(not isinstance(tag, str) for tag in row["tags"]):
            raise ValueError(f"Invalid tags: {row['name']}")
        for variant in ("outlined", "filled"):
            glyph = row[variant]
            if glyph is None:
                continue
            box = glyph.get("viewBox")
            if not isinstance(box, list) or len(box) != 4 or box[:2] != [0, 0] or min(box[2:]) <= 0:
                raise ValueError(f"Invalid viewBox: {row['name']} {variant}")
            if not isinstance(glyph.get("path"), str) or (not glyph["path"] and row["name"] != "blank"):
                raise ValueError(f"Missing path: {row['name']} {variant}")
            if not re.fullmatch(r"[0-9a-f]{64}", glyph.get("svgSha256", "")):
                raise ValueError(f"Missing SVG SHA: {row['name']} {variant}")
    if next(row for row in rows if row["name"] == "cube-pen")["outlined"]["viewBox"] != [0, 0, 17, 16]:
        raise ValueError("Pinned cube-pen viewBox was lost")
    if next(row for row in rows if row["name"] == "blank")["outlined"]["path"] != "":
        raise ValueError("Pinned blank glyph changed")


def generated_kotlin(manifest: dict) -> str:
    names = [row["name"] for row in manifest["icons"]]
    ids = ["".join(piece.capitalize() for piece in name.split("-")) for name in names]
    lines = [
        "// Generated by scripts/generate_blueprint_next_icons.py from pinned Blueprint /next assets. DO NOT EDIT.",
        "package io.github.joelromanpr.brace.blueprinticonsnext",
        "",
        "import io.github.joelromanpr.brace.icons.BraceIconName",
        "",
        "/** Typed names for all 695 canonical glyphs in pinned @blueprintjs/icons/next. */",
        "public object BraceBlueprintNextIconNames {",
    ]
    lines += [f'    public val {identifier}: BraceIconName = BraceIconName("{name}")' for identifier, name in zip(ids, names)]
    lines += ["", "    /** Canonical names in pinned manifest order. */", "    public val all: List<BraceIconName> = listOf("]
    lines += [f"        {identifier}," for identifier in ids]
    lines += ["    )", "}", ""]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        manifest = json.loads(MANIFEST.read_text())
        validate(manifest)
        if MANIFEST.read_text() != compact(manifest):
            raise ValueError("Committed next manifest is not canonical JSON")
        if sha(LICENSE.read_bytes()) != manifest["source"]["licenseSha256"]:
            raise ValueError("Packaged next license differs from pinned source")
        if NAMES.read_text() != generated_kotlin(manifest):
            raise ValueError("Generated next Kotlin names are stale")
        if args.upstream and compact(manifest) != compact(from_upstream(args.upstream)):
            raise ValueError("Committed next manifest differs from pinned metadata, SVGs, mapping, or license")
        print(f"Verified {EXPECTED_OUTLINED} next outlined, {EXPECTED_FILLED} filled glyphs and {EXPECTED_LEGACY_MAP} mappings")
        return 0
    if not args.upstream:
        parser.error("--upstream is required to generate assets")
    manifest = from_upstream(args.upstream)
    validate(manifest)
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    NAMES.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(compact(manifest))
    NAMES.write_text(generated_kotlin(manifest))
    LICENSE.write_bytes((args.upstream / "packages/icons/LICENSE").read_bytes())
    print(f"Generated {EXPECTED_OUTLINED} next outlined, {EXPECTED_FILLED} filled glyphs and {EXPECTED_LEGACY_MAP} mappings")
    return 0


if __name__ == "__main__":
    sys.exit(main())
