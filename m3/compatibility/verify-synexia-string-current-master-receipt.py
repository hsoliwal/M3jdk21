#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checker for the Synexia String current-master receipt."""

from __future__ import annotations

import argparse
import csv
import re
from pathlib import Path

SOURCE_PR = "10064"
SOURCE_BRANCH = "codex/m3jdk-string-current-head-receipt-20261009"
SOURCE_HEAD = "77b24d03bbc08d57ed8765f09a0be84ac0285564"
SOURCE_RECIPE = "synexia-indexstring/recipes/m3jdk-string-canonical-dag-20261006/TARGET-APPLICATION.tsv"
TARGET_REPO = "hsoliwal/M3jdk21"
TARGET_REF = "refs/heads/master"
TARGET_COMMIT = "5f68880c6f4a669e2fe82d6a914c071aef4f80f5"

EXPECTED_BLOBS = {
    "src/java.base/share/classes/java/lang/M3String.java": "d99a5080f153c24d3efaf37c30c26f9528563a38",
    "src/java.base/share/classes/java/lang/M3StringPool.java": "d97d0dd8f2c26599086ad817ed5d521b29593d2d",
    "src/java.base/share/classes/java/lang/M3StringTuple.java": "0d4cfb75317e68d54244588a474eba98bf7df27b",
    "test/jdk/java/lang/String/M3StringCanonicalDagTest.java": "147478672c571d7942ec43c3df2f43a1d7d46f76",
    "m3/docs/m3-runtime-invariants.tsv": "aa0093f2f39fcaeda73d87fe0d786c254deef3df",
    "m3/docs/name-mapping.json": "c29a64eaea0ca614d551ad4bcd3fa2695f0fdc9e",
    "m3/runtime-integration/check-m3string-invariants.py": "6978f2c1e13e66a826ea692a2d8a34e11cfebc1e",
    "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-string-canonical-dag-master-repair.yml": "ff55e4500db960bde9e0d7b9b8801382e7ae39f0",
    "test/jdk/java/lang/String/M3StringStreamsTest.java": "a765bf2bf39be68b61af737d7be22a053fcdc7b8",
    "src/java.base/share/classes/java/util/regex/Matcher.java": "b31fde7511bbdd3a139b330d85b66cae9a739224",
    "test/jdk/java/util/regex/M3RegexReuseTest.java": "811585d82dd5cbdefa139380d716be46333d19c7",
    "test/jdk/java/lang/String/M3StringRegexReplacementTest.java": "da611842c33e1d66f493327d669e6c6d76f21a92",
    ".github/workflows/mindex-string-backing.yml": "1f3c7d140ed7eb9ceca6530ff003e06d16ef5d45",
}
SHA1 = re.compile(r"^[0-9a-f]{40}$")


def fail(message: str) -> None:
    raise SystemExit(f"M3JDK_SYNEXIA_STRING_RECEIPT_FAIL {message}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--path",
        type=Path,
        default=Path(__file__).with_name("synexia-string-current-master-receipt-20261009.tsv"),
    )
    args = parser.parse_args()
    if not args.path.is_file():
        fail(f"missing receipt {args.path}")

    with args.path.open(newline="", encoding="utf-8") as stream:
        rows = list(csv.reader((line for line in stream if not line.startswith("#")), delimiter="\t"))
    expected_header = [
        "source_pr", "source_branch", "source_head", "source_recipe",
        "target_repo", "target_ref", "target_commit", "target_path",
        "target_blob_sha1", "receipt_state",
    ]
    if not rows or rows[0] != expected_header:
        fail("header drift")
    data = rows[1:]
    if len(data) != len(EXPECTED_BLOBS):
        fail(f"expected {len(EXPECTED_BLOBS)} rows, got {len(data)}")

    seen = set()
    for row in data:
        if len(row) != len(expected_header):
            fail(f"column-count drift: {len(row)}")
        source_pr, source_branch, source_head, source_recipe, target_repo, target_ref, target_commit, target_path, blob, state = row
        if (source_pr, source_branch, source_head, source_recipe) != (
            SOURCE_PR, SOURCE_BRANCH, SOURCE_HEAD, SOURCE_RECIPE
        ):
            fail(f"source receipt drift for {target_path}")
        if (target_repo, target_ref, target_commit, state) != (
            TARGET_REPO, TARGET_REF, TARGET_COMMIT, "CURRENT_MASTER_OBSERVED"
        ):
            fail(f"target receipt drift for {target_path}")
        if target_path not in EXPECTED_BLOBS or target_path in seen:
            fail(f"target path coverage drift for {target_path}")
        if blob != EXPECTED_BLOBS[target_path] or not SHA1.fullmatch(blob):
            fail(f"target blob drift for {target_path}")
        seen.add(target_path)
    if seen != set(EXPECTED_BLOBS):
        fail("target path set drift")
    print("M3JDK_SYNEXIA_STRING_CURRENT_MASTER_RECEIPT_PASS rows=13")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
