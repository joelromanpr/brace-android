#!/usr/bin/env python3
"""Generate and audit the optional Blueprint 6.18.0 glyph pack.

Use --upstream PATH to rebuild from the pinned Blueprint checkout. --check works from
committed files alone; with --upstream it also compares every SVG byte and path.
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
ASSET = ROOT / "brace-blueprint-icons/src/main/assets/brace-blueprint-icons.json"
KOTLIN = ROOT / "brace-blueprint-icons/src/main/java/io/github/joelromanpr/brace/blueprinticons/BraceBlueprintIconNames.kt"
LICENSE = ROOT / "brace-blueprint-icons/src/main/assets/blueprint-icons-LICENSE.txt"
PIN = "a60d4c92257612808fbfac81cfeee4fcba91a8b4"
EXPECTED_COUNT = 706
NAME_PATTERN = re.compile(r"[a-z][a-z0-9]*(?:-[a-z0-9]+)*\Z")
SHA_PATTERN = re.compile(r"[0-9a-f]{64}\Z")


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def compact(value: object) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n"


def from_upstream(path: Path) -> dict:
    commit = subprocess.check_output(["git", "-C", str(path), "rev-parse", "HEAD"], text=True).strip()
    if commit != PIN:
        raise ValueError(f"Blueprint checkout is {commit}, expected {PIN}")
    tracked_changes = subprocess.check_output(
        ["git", "-C", str(path), "status", "--porcelain", "--untracked-files=no"], text=True,
    ).strip()
    if tracked_changes:
        raise ValueError("Pinned Blueprint checkout has modified tracked files")
    metadata = path / "packages/icons/icons.json"
    license_file = path / "packages/icons/LICENSE"
    rows = json.loads(metadata.read_text())
    if len(rows) != EXPECTED_COUNT:
        raise ValueError(f"Expected {EXPECTED_COUNT} pinned icons, got {len(rows)}")
    icons = []
    for row in rows:
        name = row["iconName"]
        if not NAME_PATTERN.fullmatch(name):
            raise ValueError(f"Invalid pinned icon name: {name}")
        sizes = {}
        for size in (16, 20):
            svg = path / f"resources/icons/{size}px/{name}.svg"
            raw = svg.read_bytes()
            root = ET.fromstring(raw)
            if root.tag != "{http://www.w3.org/2000/svg}svg":
                raise ValueError(f"Not an SVG: {svg}")
            view_box = [float(value) for value in root.attrib["viewBox"].split()]
            children = list(root)
            if len(children) > 1 or any(child.tag != "{http://www.w3.org/2000/svg}path" or set(child.attrib) != {"d"} for child in children):
                raise ValueError(f"Unexpected SVG structure: {svg}")
            if name != "blank" and len(children) != 1:
                raise ValueError(f"Missing nonblank path: {svg}")
            if name == "blank" and children:
                raise ValueError(f"Blank glyph changed: {svg}")
            if view_box[:2] != [0.0, 0.0] or any(v <= 0 for v in view_box[2:]):
                raise ValueError(f"Unexpected viewBox: {svg}")
            sizes[str(size)] = {
                "viewBox": [int(v) if v.is_integer() else v for v in view_box],
                "path": children[0].attrib["d"] if children else "",
                "svgSha256": sha(raw),
            }
        icons.append({
            "name": name,
            "displayName": row["displayName"],
            "group": row["group"],
            "tags": row["tags"],
            "codepoint": row["codepoint"],
            "sizes": sizes,
        })
    for size in (16, 20):
        filenames = {svg.stem for svg in (path / f"resources/icons/{size}px").glob("*.svg")}
        if filenames != {row["name"] for row in icons}:
            raise ValueError(f"SVG name set mismatch at {size}px")
    return {
        "schemaVersion": 1,
        "source": {
            "project": "Palantir Blueprint",
            "release": "@blueprintjs/icons@6.13.0 (Blueprint core 6.18.0 pin)",
            "commit": PIN,
            "metadataPath": "packages/icons/icons.json",
            "metadataSha256": sha(metadata.read_bytes()),
            "licensePath": "packages/icons/LICENSE",
            "licenseSha256": sha(license_file.read_bytes()),
            "svgPathTemplate": "resources/icons/{size}px/{name}.svg",
            "license": "Apache-2.0",
            "transformation": "SVG viewBox and path d transcribed into a compact runtime manifest; empty blank remains empty.",
        },
        "icons": icons,
    }


def validate(manifest: dict) -> None:
    if manifest.get("schemaVersion") != 1 or manifest.get("source", {}).get("commit") != PIN:
        raise ValueError("Unexpected manifest schema or pinned commit")
    if manifest["source"].get("license") != "Apache-2.0":
        raise ValueError("Unexpected license")
    if not SHA_PATTERN.fullmatch(manifest["source"].get("metadataSha256", "")) or not SHA_PATTERN.fullmatch(manifest["source"].get("licenseSha256", "")):
        raise ValueError("Missing upstream hashes")
    rows = manifest.get("icons", [])
    if len(rows) != EXPECTED_COUNT:
        raise ValueError(f"Expected {EXPECTED_COUNT} icons, got {len(rows)}")
    names = [row["name"] for row in rows]
    if len(set(names)) != len(names) or any(not NAME_PATTERN.fullmatch(name) for name in names):
        raise ValueError("Duplicate or invalid icon name")
    identifiers = ["".join(part.capitalize() for part in name.split("-")) for name in names]
    if len(set(identifiers)) != len(identifiers):
        raise ValueError("Generated Kotlin identifier collision")
    for row in rows:
        for key in ("displayName", "group", "tags"):
            if not isinstance(row.get(key), str):
                raise ValueError(f"Missing {key}: {row['name']}")
        if set(row.get("sizes", {})) != {"16", "20"}:
            raise ValueError(f"Missing size: {row['name']}")
        for size in ("16", "20"):
            source = row["sizes"][size]
            view_box = source.get("viewBox")
            if not isinstance(view_box, list) or len(view_box) != 4 or view_box[:2] != [0, 0] or view_box[2] <= 0 or view_box[3] <= 0:
                raise ValueError(f"Invalid viewBox: {row['name']} {size}")
            if not SHA_PATTERN.fullmatch(source.get("svgSha256", "")):
                raise ValueError(f"Missing SVG hash: {row['name']} {size}")
            if not isinstance(source.get("path"), str) or (not source["path"] and row["name"] != "blank"):
                raise ValueError(f"Missing path: {row['name']} {size}")
    if rows[names.index("third-party")]["sizes"]["20"]["viewBox"] != [0, 0, 20, 18]:
        raise ValueError("Pinned third-party viewBox was lost")


def generated_kotlin(manifest: dict) -> str:
    lines = [
        "// Generated by scripts/generate_blueprint_icons.py from the pinned Blueprint icon manifest. DO NOT EDIT.",
        "package io.github.joelromanpr.brace.blueprinticons",
        "",
        "import io.github.joelromanpr.brace.icons.BraceIconName",
        "",
        "/** Typed names for all 706 pinned Blueprint glyphs. Artwork lives in the opt-in AAR. */",
        "public object BraceBlueprintIconNames {",
    ]
    identifiers = []
    for row in manifest["icons"]:
        name = row["name"]
        identifier = "".join(part.capitalize() for part in name.split("-"))
        identifiers.append(identifier)
        lines.append(f'    public val {identifier}: BraceIconName = BraceIconName("{name}")')
    lines += ["", "    /** All pinned names in Blueprint metadata order. */", "    public val all: List<BraceIconName> = listOf("]
    lines += [f"        {identifier}," for identifier in identifiers]
    lines += ["    )", "}", ""]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream", type=Path, help="Pinned Blueprint 6.18.0 checkout")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        manifest = json.loads(ASSET.read_text())
        validate(manifest)
        if ASSET.read_text() != compact(manifest):
            raise ValueError("Committed icon manifest is not canonical compact JSON")
        if sha(LICENSE.read_bytes()) != manifest["source"]["licenseSha256"]:
            raise ValueError("Packaged Blueprint license differs from pinned source hash")
        if args.upstream and compact(manifest) != compact(from_upstream(args.upstream)):
            raise ValueError("Committed icon manifest differs from pinned SVGs or metadata")
        if KOTLIN.read_text() != generated_kotlin(manifest):
            raise ValueError("Generated Kotlin icon names are stale")
        print(f"Verified {EXPECTED_COUNT} pinned Blueprint glyphs at 16px and 20px")
        return 0
    if not args.upstream:
        parser.error("--upstream is required to regenerate source assets")
    manifest = from_upstream(args.upstream)
    validate(manifest)
    ASSET.write_text(compact(manifest))
    KOTLIN.write_text(generated_kotlin(manifest))
    print(f"Generated {EXPECTED_COUNT} pinned Blueprint glyphs at 16px and 20px")
    return 0


if __name__ == "__main__":
    sys.exit(main())
