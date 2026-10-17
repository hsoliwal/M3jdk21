#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed exact Java21 source custody for primitive binary sorted set algebra."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("sorted-set-algebra-postimages-20261017.tsv")

def main() -> None:
    lines = [x.split("\t") for x in MANIFEST.read_text(encoding="utf-8").splitlines()
             if x and not x.startswith("#")]
    if len(lines) != 3 or len({x[0] for x in lines}) != 3:
        raise SystemExit("expected exactly three unique sorted-set-algebra source paths")
    for row in lines:
        if len(row) != 3:
            raise SystemExit("invalid sorted-set-algebra manifest row")
        path, digest, state = row
        relative = Path(path)
        if (relative.is_absolute() or ".." in relative.parts
                or relative.parts[:2] != ("m3", "collections")
                or relative.suffix != ".java"
                or state != "MODULE_SOURCE_NOT_INSTALLED_JDK"):
            raise SystemExit("inadmissible sorted algebra target: " + path)
        target = ROOT / relative
        if not target.is_file() or target.is_symlink():
            raise SystemExit("missing or linked sorted algebra target: " + path)
        actual = hashlib.sha256(target.read_bytes()).hexdigest()
        if actual != digest:
            raise SystemExit("sorted algebra source drift: " + path)
        print("SEALED", path, actual)
    print("PASS exact three primitive sorted set algebra postimages")

if __name__ == "__main__":
    main()
