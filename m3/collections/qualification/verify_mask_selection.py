#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed exact installed-source SHA-256 verifier (not a recipe emitter)."""
import hashlib
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from m3_long_mask_receipt import approved_successor, effective_hash

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("mask-selection-postimages-20261016.tsv")

def main() -> None:
    rows = [line.split("\t") for line in MANIFEST.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#")]
    if len(rows) != 6 or len({row[0] for row in rows}) != 6:
        raise SystemExit("missing/duplicate M3 mask receiving paths")
    successor = approved_successor()
    for row in rows:
        if len(row) != 3:
            raise SystemExit("invalid mask receiving manifest row")
        path, expected, disposition = row
        rel = Path(path)
        if (rel.is_absolute() or ".." in rel.parts
                or rel.parts[:2] != ("m3", "collections")
                or disposition != "RECEIVER_CANDIDATE_NOT_INSTALLED_JDK"):
            raise SystemExit("out-of-scope/invalid target path")
        file = ROOT / rel
        if not file.is_file():
            raise SystemExit("missing installed target: " + path)
        actual = hashlib.sha256(file.read_bytes()).hexdigest()
        if actual != effective_hash(path, expected, successor):
            raise SystemExit("source identity drift: " + path)
        print("SEALED", path, actual)
    print("PASS all six sealed M3 array/mask receiver sources")

if __name__ == "__main__":
    main()
