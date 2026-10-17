#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Refuse drift in four exact M3JDK21 primitive filtered/sorted receiver targets."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("filtered-sorted-postimages-20261016.tsv")

def main():
    rows = [row.split("\t") for row in MANIFEST.read_text(encoding="utf-8").splitlines()
            if row and not row.startswith("#")]
    if len(rows) != 4 or len({row[0] for row in rows}) != 4:
        raise SystemExit("filtered/sorted receiver needs four unique sealed paths")
    for row in rows:
        if len(row) != 3:
            raise SystemExit("invalid manifest field count")
        path, digest, state = row
        rel = Path(path)
        if rel.is_absolute() or ".." in rel.parts or rel.parts[:2] != ("m3", "collections"):
            raise SystemExit("invalid receiver source path: " + path)
        if state != "MODULE_SOURCE_NOT_INSTALLED_JDK":
            raise SystemExit("unexpected source promotion state")
        absolute = ROOT / rel
        if not absolute.is_file():
            raise SystemExit("missing M3 receiving source: " + path)
        actual = hashlib.sha256(absolute.read_bytes()).hexdigest()
        if digest != actual:
            raise SystemExit("exact filtered/sorted source drift: " + path)
        print("SEALED", path, actual)
    print("PASS: four exact filtered/sorted Java receiver targets")

if __name__ == "__main__":
    main()
