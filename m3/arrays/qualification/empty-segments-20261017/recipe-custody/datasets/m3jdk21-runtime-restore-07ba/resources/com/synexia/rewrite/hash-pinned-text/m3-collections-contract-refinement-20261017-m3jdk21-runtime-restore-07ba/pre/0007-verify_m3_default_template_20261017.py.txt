#!/usr/bin/env python3
"""Fail-closed exact M3 source receiver digest check. Does not run OpenRewrite or jtreg."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "m3/compatibility/m3-default-template-source-seal.tsv"

def run():
    seen = set()
    for raw in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not raw or raw.startswith("#"):
            continue
        path, before, after, template = raw.split("\t")
        if path in seen or not path.startswith(("src/java.base/share/classes/", "test/jdk/")):
            raise ValueError("unexpected/duplicate source: " + path)
        if len(after) != 64 or (before != "ABSENT" and len(before) != 64):
            raise ValueError("unsealed source: " + path)
        expected = hashlib.sha256((ROOT / path).read_bytes()).hexdigest()
        if expected != after:
            raise AssertionError(path + ": " + expected + " != " + after)
        seen.add(path)
    if len(seen) != 5:
        raise AssertionError("expected five source-sealed Java files")
    print("PASS 5 exact M3 Java/template receiver digests")

if __name__ == "__main__":
    run()
