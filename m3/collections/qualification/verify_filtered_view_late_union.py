#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Reject missing/drifted runtime and test outputs from exact Synexia receipt."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RECEIPT = Path(__file__).with_name("filtered-view-late-union-postimages-20261017.tsv")

def main() -> None:
    entries = [line.split("\t") for line in RECEIPT.read_text(encoding="utf-8").splitlines()
               if line and not line.startswith("#")]
    if len(entries) != 3 or len({row[0] for row in entries}) != 3:
        raise SystemExit("expected three unique Java21 source/test targets")
    for entry in entries:
        if len(entry) != 3:
            raise SystemExit("invalid source manifest row")
        path, expected, status = entry
        candidate = Path(path)
        if (candidate.is_absolute() or ".." in candidate.parts
                or candidate.parts[:2] != ("m3", "collections")
                or candidate.suffix != ".java"
                or status != "MODULE_SOURCE_NOT_INSTALLED_JDK"):
            raise SystemExit("invalid source provenance or path: " + path)
        target = ROOT / candidate
        if not target.is_file() or target.is_symlink():
            raise SystemExit("target missing or symlinked: " + path)
        digest = hashlib.sha256(target.read_bytes()).hexdigest()
        if digest != expected:
            raise SystemExit("source output drift: " + path)
        print("SEALED", path, digest)
    print("PASS three exact M3JDK21 filtered union postimages")

if __name__ == "__main__":
    main()
