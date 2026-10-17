#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Pinned M3JDK21 priority mask owner, standalone oracle and JUnit source check."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RECEIPT = Path(__file__).with_name("mask-priority-postimages-20261016.tsv")

def main() -> None:
    rows = [line.split("\t") for line in RECEIPT.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#")]
    if len(rows) != 3 or len({r[0] for r in rows}) != 3:
        raise SystemExit("expected three unique priority receiving sources")
    for entry in rows:
        if len(entry) != 3:
            raise SystemExit("invalid priority receipt")
        name, expected, state = entry
        path = Path(name)
        if (path.is_absolute() or ".." in path.parts
                or path.parts[:2] != ("m3", "collections")
                or state != "RECEIVER_CANDIDATE_NOT_INSTALLED"):
            raise SystemExit("invalid scope: " + name)
        source = ROOT / path
        if not source.is_file():
            raise SystemExit("missing priority receiving file: " + name)
        digest = hashlib.sha256(source.read_bytes()).hexdigest()
        if digest != expected:
            raise SystemExit("priority source drift: " + name)
        print("SEALED", name, digest)
    print("PASS priority in-place mask receiving custody")

if __name__ == "__main__":
    main()
