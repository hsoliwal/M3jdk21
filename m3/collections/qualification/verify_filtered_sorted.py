#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Preserve four exact historical source seals, plus one approved M3 successor.

Original 2026-10-16 SHA256 rows remain unchanged and authoritative. The only
allowed difference is one reviewable source-version transition in
M3LongFilteredView. Its exact 2026-10-17 companion three-file receipt and
all three matching target files must be present; no wildcard or env override.
"""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("filtered-sorted-postimages-20261016.tsv")
SUCCESSOR_MANIFEST = Path(__file__).with_name(
    "filtered-view-late-union-postimages-20261017.tsv")
ORIGINAL_OWNER = "m3/collections/src/main/java/com/m3/collections/M3LongFilteredView.java"
SUCCESSOR_HASHES = {
    "m3/collections/src/main/java/com/m3/collections/M3LongFilteredView.java": "97fb0463c399ed39fc2b0e6eb3616ba434b8c1fa9eee7f5781ef606ca0be5f1a",
    "m3/collections/src/test/java/com/m3/collections/M3LongFilteredViewUnionSelfTest.java": "eb5ad9f57463e62596afad72c06935f1f2dac91ad224c7204dec2165ea1d1334",
    "m3/collections/src/test/java/com/m3/collections/M3LongFilteredViewUnionTest.java": "d4dd8a8ce7dcb17156c4646187a4382772ce6cf40879649d49add88a7d96fe61",
}
STATE = "MODULE_SOURCE_NOT_INSTALLED_JDK"


def read_rows(path: Path, expected_count: int):
    if not path.is_file() or path.is_symlink():
        raise SystemExit("missing or linked sealed manifest: " + str(path))
    rows = [line.split("\t") for line in path.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#")]
    if len(rows) != expected_count or len({row[0] for row in rows if row}) != expected_count:
        raise SystemExit("invalid count/duplicates in sealed manifest: " + str(path))
    if any(len(row) != 3 for row in rows):
        raise SystemExit("invalid sealed manifest field count: " + str(path))
    return rows


def actual_hash(path: str) -> str:
    rel = Path(path)
    if rel.is_absolute() or ".." in rel.parts or rel.parts[:2] != ("m3", "collections"):
        raise SystemExit("invalid receiver source path: " + path)
    absolute = ROOT / rel
    if not absolute.is_file() or absolute.is_symlink():
        raise SystemExit("missing or linked M3 receiving source: " + path)
    return hashlib.sha256(absolute.read_bytes()).hexdigest()


def successor_authorized() -> bool:
    # Fail closed rather than accept a separately configurable source hash.
    if not SUCCESSOR_MANIFEST.is_file() or SUCCESSOR_MANIFEST.is_symlink():
        return False
    rows = read_rows(SUCCESSOR_MANIFEST, len(SUCCESSOR_HASHES))
    received = {path: (digest, state) for path, digest, state in rows}
    if set(received) != set(SUCCESSOR_HASHES):
        return False
    for path, pinned in SUCCESSOR_HASHES.items():
        if received[path] != (pinned, STATE) or actual_hash(path) != pinned:
            return False
    return True


def main() -> None:
    rows = read_rows(MANIFEST, 4)
    for path, historical, state in rows:
        if state != STATE:
            raise SystemExit("unexpected source promotion state: " + path)
        observed = actual_hash(path)
        if observed == historical:
            print("SEALED", path, observed)
            continue
        if (path == ORIGINAL_OWNER
                and historical == "825e6b45c1af7ed4f0433ca06bd46767b9374002bcbc0f74da636b9ffe2f75f3"
                and observed == SUCCESSOR_HASHES[ORIGINAL_OWNER]
                and successor_authorized()):
            print("SEALED AUTHORIZED SUCCESSOR", path, observed)
            continue
        raise SystemExit("exact filtered/sorted source drift: " + path)
    print("PASS: historical filtered/sorted seals + exact registered successor")


if __name__ == "__main__":
    main()
