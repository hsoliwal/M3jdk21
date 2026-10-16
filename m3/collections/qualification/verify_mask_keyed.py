#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only SHA256 admission guard for four source-sealed M3 keyed mask targets."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("mask-keyed-postimages-20261016.tsv")

def main() -> None:
    rows = [line.split("\t") for line in MANIFEST.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#")]
    if len(rows) != 4 or len({r[0] for r in rows}) != 4:
        raise SystemExit("keyed mask manifest requires exactly four unique targets")
    for row in rows:
        if len(row) != 3:
            raise SystemExit("invalid receipt column count")
        path, expected, disposition = row
        rel = Path(path)
        if (rel.is_absolute() or ".." in rel.parts or rel.parts[:2] != ("m3", "collections")
                or disposition != "SOURCE_RECEIVED_NOT_INSTALLED_JDK"):
            raise SystemExit("invalid target source authority: " + path)
        file = ROOT / rel
        if not file.is_file():
            raise SystemExit("missing keyed mask receiver: " + path)
        actual = hashlib.sha256(file.read_bytes()).hexdigest()
        if actual != expected:
            raise SystemExit("keyed mask target drift: " + path)
        print("SEALED", path, actual)
    print("PASS four source sealed primitive keyed mask owners")

if __name__ == "__main__":
    main()
