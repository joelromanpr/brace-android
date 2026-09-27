#!/usr/bin/env python3
"""Check the Maven Local artifacts before a Central release is staged."""

import argparse
import json
import subprocess
import sys
import tempfile
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
SIGNING_FINGERPRINT = "F152FD630BE99A9BB2483995BA23075E89D123B5"
PUBLIC_KEY = Path(__file__).resolve().parents[1] / ".github/release-signing-key.asc"


def check_archive(path: Path, suffix: str) -> set[str]:
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError(f"corrupt {suffix}: {path.name}")
        return set(archive.namelist())


def check_signature(path: Path, keyring: Path) -> None:
    signature = path.with_name(path.name + ".asc")
    if not signature.is_file() or signature.stat().st_size == 0:
        raise ValueError(f"missing signature for {path.name}")
    result = subprocess.run(
        ["gpg", "--batch", "--no-tty", "--homedir", str(keyring), "--status-fd=1",
         "--verify", str(signature), str(path)],
        capture_output=True, text=True, check=False,
    )
    if result.returncode != 0 or f"[GNUPG:] VALIDSIG {SIGNING_FINGERPRINT} " not in result.stdout:
        raise ValueError(f"invalid Brace release signature for {path.name}")


def check_module(root: Path, module: str, version: str, keyring: Path | None = None) -> None:
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
        if keyring is not None:
            check_signature(path, keyring)

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
    parser.add_argument("--require-signatures", action="store_true",
                        help="verify all five files in each module against the pinned release key")
    args = parser.parse_args()
    try:
        # A short path keeps GPG's Unix socket below macOS's path-length limit.
        with tempfile.TemporaryDirectory(prefix="brace-signatures-", dir="/tmp") as temporary:
            keyring = None
            if args.require_signatures:
                keyring = Path(temporary)
                import_result = subprocess.run(
                    ["gpg", "--batch", "--no-tty", "--homedir", str(keyring),
                     "--import", str(PUBLIC_KEY)],
                    capture_output=True, text=True, check=False,
                )
                if import_result.returncode != 0:
                    raise ValueError("could not import pinned release public key")
                key_list = subprocess.run(
                    ["gpg", "--batch", "--no-tty", "--homedir", str(keyring),
                     "--with-colons", "--fingerprint", "--list-keys"],
                    capture_output=True, text=True, check=False,
                )
                fingerprints = [line.split(":")[9] for line in key_list.stdout.splitlines()
                                if line.startswith("fpr:")]
                if key_list.returncode != 0 or fingerprints != [SIGNING_FINGERPRINT]:
                    raise ValueError("pinned release public key fingerprint mismatch")
            for module in MODULES:
                check_module(args.repository, module, args.version, keyring)
    except (OSError, ValueError, ET.ParseError, zipfile.BadZipFile) as error:
        print(f"Maven publication check failed: {error}", file=sys.stderr)
        return 1
    suffix = " and verified release signatures" if args.require_signatures else ""
    print(f"Maven publication check: {len(MODULES)} aligned AARs with POM, metadata, sources, and docs{suffix}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
