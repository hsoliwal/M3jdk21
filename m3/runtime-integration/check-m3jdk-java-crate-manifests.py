#!/usr/bin/env python3
"""Fail-fast structural verifier for hash-pinned recipe crates.

The Java recipe remains the admission authority. This source-bound gate catches
manifest shape, ordering, sentinel, resource existence, and postimage-hash
errors before Maven/OpenRewrite parsing starts.
"""

from __future__ import annotations

import hashlib
from pathlib import Path
import re
import sys


HEX = re.compile(r"^[0-9a-f]{64}$")
TEMPLATE = re.compile(r"^[A-Za-z0-9_.-]+$")
PART = re.compile(r"^[A-Za-z0-9_$.-]+$")
HEADER = "# path\tbefore\tafter\ttemplate"
TARGET_PREFIXES = (
    "src/",
    "test/",
    "m3/ports/",
    ".m3/openrewrite-recipes/src/main/java/",
    ".m3/openrewrite-recipes/src/test/java/",
    "m3/tooling/migration-recipes/src/main/java/",
    "m3/tooling/migration-recipes/src/test/java/",
    "m3/tooling/a3/src/main/java/",
    "m3/tooling/a3/src/test/java/",
)


def fail(message: str) -> None:
    raise SystemExit(f"M3JDK_JAVA_MANIFEST_FAIL|{message}")


def valid_target_path(value: str) -> bool:
    if (
        "\\" in value
        or len(value) > 4096
        or not value.startswith(TARGET_PREFIXES)
        or any(part in {".", ".."} or not PART.fullmatch(part)
               for part in value.split("/"))
    ):
        return False
    return all(ord(char) >= 32 for char in value)


def main() -> None:
    root = Path(__file__).resolve().parents[2]
    resource_root = (
        root
        / "m3/tooling/migration-recipes/src/main/resources"
        / "com/m3/rewrite/backport/jdk21-hash-pinned"
    )
    if not resource_root.is_dir():
        fail(f"missing_resource_root={resource_root}")

    manifests = sorted(resource_root.glob("*/manifest.tsv"))
    if not manifests:
        fail("no_java_crate_manifests")

    crates = 0
    rows = 0
    for manifest in manifests:
        crate = manifest.parent.name
        lines = manifest.read_text(encoding="utf-8").splitlines()
        if not lines:
            fail(f"empty_manifest|crate={crate}")
        previous = ""
        seen: set[str] = set()
        crate_rows = 0
        for line_number, line in enumerate(lines, start=1):
            if not line.strip() or line.startswith("#"):
                continue
            cells = line.split("\t")
            if len(cells) != 4:
                fail(f"columns|crate={crate}|line={line_number}")
            path, before, after, template = cells
            if (
                not valid_java_path(path)
                or path in seen
                or (previous and previous >= path)
                or (before != "ABSENT" and not HEX.fullmatch(before))
                or not HEX.fullmatch(after)
                or not TEMPLATE.fullmatch(template)
            ):
                fail(
                    f"shape|crate={crate}|line={line_number}|"
                    f"row={line.replace(chr(9), '<TAB>')}"
                )
            if crate.startswith("synexia-") and path.startswith(("src/", "test/")):
                fail(f"synexia_receiver_path|crate={crate}|line={line_number}")
            template_path = manifest.parent / template
            if not template_path.is_file():
                fail(f"missing_template|crate={crate}|line={line_number}")
            digest = hashlib.sha256(template_path.read_bytes()).hexdigest()
            if digest != after:
                fail(f"template_hash|crate={crate}|line={line_number}")
            seen.add(path)
            previous = path
            crate_rows += 1
        if not crate_rows or crate_rows > 256:
            fail(f"target_budget|crate={crate}|rows={crate_rows}")
        crates += 1
        rows += crate_rows

    print(f"M3JDK_JAVA_MANIFEST_PASS|crates={crates}|rows={rows}")


if __name__ == "__main__":
    main()
