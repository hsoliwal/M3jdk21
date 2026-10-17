#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Check the *installed receiver* against exact Synexia source-sealed outputs.

A successful checksum does NOT mean Maven/OpenRewrite, JNI or full JDK validation passed.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
TARGETS = {
    "m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java":
        "e71a3ee67e69fa4f24d20e4a44e929333334ca82cf05793c241d8119cd2937f1",
    "m3/collections/wrappers/src/test/java/com/m3/collections/M3StoredRepresentativeProof.java":
        "2c19ec2913ab6f9e5874f0209565f90ea858b79521f55a0916ed9892548da136",
}
PREIMAGE = "115f16592a41ce78a63b4892904cb4fe934f5ee45974ffb3305ec39a7c0b9f6e"
PREIMAGE_REVISION = "c5e5e90e2dcc2298a372ab39640120dd60a2edb7"
RECIPE_REVISION = "4603897b59df5d59870f62f6d1fb4ffe58588e79"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def verify(canonical: Path | None) -> None:
    if len(TARGETS) != 2:
        raise SystemExit("unexpected target count")
    for relative, expected in sorted(TARGETS.items()):
        path = ROOT / relative
        if not path.is_file():
            raise SystemExit("RECEIVER_MISSING: " + relative)
        payload = path.read_bytes()
        if sha256(payload) != expected:
            raise SystemExit("RECEIVER_HASH_MISMATCH: " + relative)
        if canonical is not None:
            templ = canonical / (
                "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/"
                "hash-pinned-java/m3-stored-batch-union-20261017"
            )
            template = templ / (
                "M3CollectionViews.java.txt" if relative.endswith("M3CollectionViews.java")
                else "M3StoredRepresentativeProof.java.txt"
            )
            if not template.is_file() or payload != template.read_bytes():
                raise SystemExit("CANONICAL_TEMPLATE_MISMATCH: " + relative)
        print("RECEIVER_SOURCE_SHA256_OK", relative, expected)
    if canonical is not None:
        source = canonical / (
            "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/"
            "hash-pinned-java/m3-stored-batch-union-20261017"
        )
        baseline = (source / "pre/M3CollectionViews.java.txt").read_bytes()
        if sha256(baseline) != PREIMAGE:
            raise SystemExit("RECIPE_PREIMAGE_DRIFT")
    print("RECEIVER_CUSTODY_ONLY_PASS canonical_recipe_commit=" + RECIPE_REVISION)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--canonical", type=Path, default=None,
                        help="Optional exact Synexia checkout for template/manifest checks")
    verify(parser.parse_args().canonical)
