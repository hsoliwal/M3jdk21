#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Fail-closed verifier for the exact Synexia recipe source custody and emitted Java files."""
import argparse
import hashlib
from pathlib import Path

CRATE = "m3jdk21-five-library-index-views-20261017"
ROOT = Path(__file__).resolve().parents[5]
HERE = Path(__file__).resolve().parent
RES = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-java" / CRATE
MIRROR = HERE / "canonical/src/main/resources/com/synexia/rewrite/hash-pinned-java" / CRATE


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", choices=("source", "installed"), default="installed")
    stage = parser.parse_args().stage
    rows = []
    manifest = RES / "manifest.tsv"
    assert manifest.read_bytes() == (MIRROR / "manifest.tsv").read_bytes(), "manifest drift"
    for line in manifest.read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        path, before, after, template = line.split("	")
        assert before == "ABSENT", (path, before)
        assert digest(RES / template) == after, ("active template drift", path)
        assert (RES / template).read_bytes() == (MIRROR / template).read_bytes(), "canonical drift"
        target = ROOT / path
        if stage == "source":
            assert not target.exists(), ("occupied before recipe", path)
        else:
            assert target.is_file() and not target.is_symlink(), ("missing source", path)
            assert digest(target) == after, ("unexpected generated Java", path)
        rows.append((path, after))
    assert len(rows) == 2 and len({p for p, _ in rows}) == 2
    assert (HERE / "SOURCE_RECEIPT.tsv").is_file()
    print("M3 five-library exact source", stage, "PASS", len(rows))


if __name__ == "__main__":
    main()
