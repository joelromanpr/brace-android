#!/usr/bin/env python3
"""Check the Maven Local artifacts before a Central release is staged."""

import argparse
import json
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


GROUP = "io.github.joelromanpr.brace"
MODULES = (
    "brace-foundation",
    "brace-core",
    "brace-icons",
    "brace-blueprint-icons",
    "brace-blueprint-icons-next",
    "brace-select",
    "brace-datetime",
    "brace-table",
)
POM_NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def check_archive(path: Path, suffix: str) -> set[str]:
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError(f"corrupt {suffix}: {path.name}")
        return set(archive.namelist())


def check_module(root: Path, module: str, version: str) -> None:
    directory = root / GROUP.replace(".", "/") / module / version
    stem = f"{module}-{version}"
    files = {kind: directory / name for kind, name in {
        "aar": f"{stem}.aar",
        "pom": f"{stem}.pom",
        "module": f"{stem}.module",
        "sources": f"{stem}-sources.jar",
        "javadoc": f"{stem}-javadoc.jar",
    }.items()}
    for kind, path in files.items():
        if not path.is_file() or path.stat().st_size == 0:
            raise ValueError(f"{module}: missing or empty {kind} artifact")

    pom = ET.parse(files["pom"]).getroot()
    field = lambda path: pom.findtext(
        "/".join(f"m:{part}" for part in path.split("/")), namespaces=POM_NS
    )
    for name, expected in (
        ("groupId", GROUP), ("artifactId", module),
        ("version", version), ("packaging", "aar"),
    ):
        if field(name) != expected:
            raise ValueError(f"{module}: POM {name} does not match {expected}")
    for name in ("name", "description", "url", "licenses/license/name",
                 "licenses/license/url", "developers/developer/id", "scm/url"):
        if not field(name):
            raise ValueError(f"{module}: POM {name} is empty")
    for dependency in pom.findall("m:dependencies/m:dependency", POM_NS):
        if dependency.findtext("m:groupId", namespaces=POM_NS) == GROUP:
            if dependency.findtext("m:version", namespaces=POM_NS) != version:
                raise ValueError(f"{module}: internal dependency version is not aligned")

    metadata = json.loads(files["module"].read_text())
    component = metadata.get("component", {})
    if any(component.get(key) != expected for key, expected in (
        ("group", GROUP), ("module", module), ("version", version),
    )):
        raise ValueError(f"{module}: Gradle module coordinates do not match")
    sources = check_archive(files["sources"], "sources")
    if not any(name.endswith((".kt", ".java")) for name in sources):
        raise ValueError(f"{module}: sources JAR contains no source files")
    if "index.html" not in check_archive(files["javadoc"], "documentation"):
        raise ValueError(f"{module}: documentation JAR has no index")
    aar = check_archive(files["aar"], "AAR")
    if not {"AndroidManifest.xml", "classes.jar"}.issubset(aar):
        raise ValueError(f"{module}: AAR is missing its manifest or classes")
    if module.startswith("brace-blueprint-icons"):
        asset_prefix = module.removeprefix("brace-")
        required = {f"assets/{asset_prefix}-LICENSE.txt", f"assets/{asset_prefix}-ATTRIBUTION.txt"}
        if not required.issubset(aar):
            raise ValueError(f"{module}: bundled glyph license or attribution is missing")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version", help="The version published to Maven Local")
    parser.add_argument("--repository", type=Path, default=Path.home() / ".m2/repository")
    args = parser.parse_args()
    try:
        for module in MODULES:
            check_module(args.repository, module, args.version)
    except (OSError, ValueError, ET.ParseError, zipfile.BadZipFile) as error:
        print(f"Maven publication check failed: {error}", file=sys.stderr)
        return 1
    print(f"Maven publication check: {len(MODULES)} aligned AARs with POM, metadata, sources, and docs")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
