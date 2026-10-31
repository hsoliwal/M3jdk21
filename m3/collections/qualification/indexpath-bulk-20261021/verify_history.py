#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Inventory historical rank recipe templates and current successor separately.

Historical M3IndexPathRank recipe inputs were ABSENT. A later source modification
must not be declared byte-equivalent to that initial template. This gate proves
custody of each original recipe template, independently validates the successor
receiver hashes, and explicitly reports unproven intermediate transitions.
"""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[4]
OLD_DIR = (ROOT / "m3/tooling/migration-recipes/src/main/resources"
           "/com/synexia/rewrite/hash-pinned-java/m3-index-path-rank-20261017")
NEW_DIR = ROOT / "m3/collections/qualification/indexpath-bulk-20261021"
OLD_NAMES = {
    "m3/collections/src/main/java/com/m3/collections/M3IndexPathRank.java",
    "m3/collections/src/test/java/com/m3/collections/M3IndexPathRankNativeSelfTest.java",
    "m3/collections/src/test/java/com/m3/collections/M3IndexPathRankSelfTest.java",
    "m3/collections/src/test/java/com/m3/collections/M3IndexPathRankTest.java",
}
SUCCESSOR_NAMES = {
    "m3/collections/src/main/java/com/m3/collections/M3IndexPathRank.java",
    "m3/collections/src/test/java/com/m3/collections/M3IndexPathRankBulkSelfTest.java",
    "m3/collections/src/test/java/com/m3/collections/M3IndexPathRankBulkTest.java",
    "m3/collections/src/test/java/com/m3/collections/M3IndexPathRankNativeSelfTest.java",
}
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_historical() -> dict[str, str]:
    manifest = OLD_DIR / "manifest.tsv"
    if not manifest.is_file():
        raise ValueError("historical manifest missing")
    entries: dict[str, str] = {}
    for line in manifest.read_text(encoding="utf-8").splitlines():
        path, before, after, template = line.split("\t")
        if path not in OLD_NAMES or path in entries or before != "ABSENT":
            raise ValueError(f"invalid historical recipe entry: {path}")
        if not HEX64.fullmatch(after) or not template.endswith(".java.txt") or "/" in template:
            raise ValueError(f"invalid original digest/template: {path}")
        if digest(OLD_DIR / template) != after:
            raise ValueError(f"historical template bytes changed: {path}")
        entries[path] = after
    if set(entries) != OLD_NAMES:
        raise ValueError("historical recipe incomplete")
    return entries


def read_successor() -> dict[str, tuple[str, str]]:
    entries: dict[str, tuple[str, str]] = {}
    previous = ""
    manifest = NEW_DIR / "SOURCE_MANIFEST.tsv"
    if not manifest.is_file():
        raise ValueError("successor manifest missing")
    for line in manifest.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        path, before, after, blob = line.split("\t")
        if path not in SUCCESSOR_NAMES or path <= previous or path in entries:
            raise ValueError(f"invalid successor entry/order: {path}")
        if before != "ABSENT" and not HEX64.fullmatch(before):
            raise ValueError(f"invalid successor preimage: {path}")
        if not HEX64.fullmatch(after) or not re.fullmatch(r"[0-9a-f]{40}", blob):
            raise ValueError(f"invalid successor postimage: {path}")
        if digest(ROOT / path) != after:
            raise ValueError(f"successor source SHA-256 drift: {path}")
        actual_blob = subprocess.check_output(
            ["git", "-C", str(ROOT), "hash-object", path], text=True).strip()
        if actual_blob != blob:
            raise ValueError(f"successor source Git blob drift: {path}")
        entries[path] = (before, after)
        previous = path
    if set(entries) != SUCCESSOR_NAMES:
        raise ValueError("successor source set incomplete")
    return entries


def main() -> None:
    historic = read_historical()
    successor = read_successor()
    unknown: list[str] = []
    for path, original in historic.items():
        if path in successor:
            successor_pre, successor_post = successor[path]
            if original != successor_pre and original != successor_post:
                unknown.append(path)
        elif digest(ROOT / path) != original:
            raise ValueError(f"untouched historical source drift: {path}")
    print(f"PASS historical template identities={len(historic)} "
          f"successor source identities={len(successor)}")
    if unknown:
        print("NOT_PROVEN historical-to-successor intermediate transitions:")
        for path in sorted(unknown):
            print(f"  {path}")
        print("REVIEW_REQUIRED: source ancestry is not established by either manifest; "
              "do not report behavioral equivalence or automatic promotion.")


if __name__ == "__main__":
    main()
