#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed receiving check; this does not generate or rewrite target code."""
from __future__ import annotations
from pathlib import Path
import hashlib
import sys

CRATE = Path(__file__).resolve().parent
ROOT = CRATE.parents[3]
RESOURCES = CRATE / "src/main/resources/com/synexia/rewrite/hash-pinned-java/object-state-batch-28"
CORE = ROOT / "m3/collections/src/main/java/com/m3/collections"

def sha(path: Path) -> str:
    if path.is_symlink() or not path.is_file():
        raise ValueError(f"missing/aliased file: {path}")
    return hashlib.sha256(path.read_bytes()).hexdigest()

def check() -> None:
    rows = []
    for line in (RESOURCES / "manifest.tsv").read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        path, before, after, template = line.split("\t")
        if before != "ABSENT" or len(after) != 64 or Path(path).is_absolute() or ".." in Path(path).parts:
            raise ValueError("invalid recipe manifest")
        if not path.startswith("m3/collections/src/") or not path.endswith(".java"):
            raise ValueError("target outside collection module")
        rows.append((path, after, template))
    if len(rows) != 2 or [x[0] for x in rows] != sorted(set(x[0] for x in rows)):
        raise ValueError("source ownership/count drift")
    for name, expected, template in rows:
        original = ROOT / name
        image = RESOURCES / template
        mirror = ROOT / ("m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/object-state-batch-28/" + template)
        if sha(original) != expected or sha(image) != expected or sha(mirror) != expected:
            raise ValueError("SDK recipe/receiver/source drift: " + name)
        if original.read_bytes() != image.read_bytes():
            raise ValueError("generated Java differs from qualified template")
    owners = []
    for row in (CRATE / "M3_OWNER_SHA256.tsv").read_text().splitlines():
        if row.startswith("#") or not row:
            continue
        name, h, blob = row.split("\t")
        owner = CORE / name
        data = owner.read_bytes()
        git_blob = hashlib.sha1((f"blob {len(data)}\0").encode() + data).hexdigest()
        if sha(owner) != h or git_blob != blob:
            raise ValueError("base M3 owner drift: " + name)
        owners.append(name)
    if len(owners) != 5:
        raise ValueError("owner set drift")
    print("PASS M3_OBJECT_STATE_BATCH_RECEIVER generated=2 owners=5")

if __name__ == "__main__":
    check()
