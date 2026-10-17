#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only SHA-256 admission for the existing source-sealed OpenRewrite recipe.

No rewriting is performed here. The canonical M3GpuToolJoinRecipe owns source
candidate creation, source drift refusal and fixed-point proof through JUnit.
"""
import hashlib
import sys
from pathlib import Path

ROOT = Path("synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-javac-constant-regex")
EXPECTED_ROWS = 8


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def verify(root: Path) -> None:
    manifest = (root / ROOT / "manifest.tsv").read_text(encoding="utf-8")
    rows = manifest.splitlines()
    if len(rows) != EXPECTED_ROWS:
        raise ValueError("exactly one unchanged compiler owner plus seven additions are required")
    seen: set[str] = set()
    kept = added = 0
    for line in rows:
        fields = line.split("\t")
        if len(fields) != 4:
            raise ValueError("invalid canonical manifest row")
        target, before, after, resource = fields
        path = Path(target)
        if target in seen or path.is_absolute() or ".." in path.parts:
            raise ValueError("missing or duplicate canonical FILE target")
        seen.add(target)
        actual = sha256((root / path).read_bytes())
        if resource == "KEEP":
            kept += 1
            if before != after or actual != before:
                raise ValueError("compiler AST authority changed")
        else:
            added += 1
            if before != "ABSENT" or actual != after:
                raise ValueError("source drift, or overlapping existing candidate")
            template = ROOT / resource
            if Path(resource).name != resource:
                raise ValueError("unsafe candidate resource path")
            if sha256((root / template).read_bytes()) != after:
                raise ValueError("stale source-sealed template")
    if kept != 1 or added != 7:
        raise ValueError("invalid precompute recipe owner or addition count")
    print("M3_JAVAC_REGEX_SOURCE_SEALS=PASS owners=1 additions=7")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: verify_source_seals.py REPOSITORY_ROOT")
    verify(Path(sys.argv[1]).resolve())
