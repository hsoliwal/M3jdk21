#!/usr/bin/env python3
"""Read-only exact candidate hash verification; not an OpenRewrite execution receipt."""
from __future__ import annotations
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "m3/compatibility/m3-object-adapter-progress-20261017.tsv"

def verify() -> int:
    lines = MANIFEST.read_text(encoding="utf-8").splitlines()
    seen: set[str] = set()
    count = 0
    for row in lines:
        if not row or row.startswith("#"):
            continue
        path, before, after, template = row.split("\t")
        assert path.startswith(("src/java.base/share/classes/", "test/jdk/")), path
        assert path not in seen, path
        assert len(after) == 64 and (before == "ABSENT" or len(before) == 64)
        seen.add(path)
        file = ROOT / path
        if not file.is_file():
            raise RuntimeError(f"missing receiver: {path}")
        actual = hashlib.sha256(file.read_bytes()).hexdigest()
        if actual != after:
            raise RuntimeError(f"source drift: {path}: {actual} != {after}")
        count += 1
    if count != 11:
        raise RuntimeError(f"receiver coverage {count} != 11")
    print(f"PASS exact 11-file receiver identity: {count}")
    return count

if __name__ == "__main__":
    verify()
