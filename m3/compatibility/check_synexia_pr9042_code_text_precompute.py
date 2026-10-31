#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed verifier for the Synexia PR #9042 -> M3JDK21 precompute handoff."""

from __future__ import annotations

import csv
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RECEIPT = ROOT / "m3/compatibility/synexia-pr9042-code-text-precompute.tsv"

EXPECTED_PR = "9042"
EXPECTED_HEAD = "aa9b4ff7bd69ff58beba0c3cdd2c66b878c781ed"
EXPECTED_MERGE = "d50ba4e3fe0d4c99259d006c725b1eb2d1eff234"
EXPECTED_ROWS = 8

RUNTIME_PREFIX = "m3/ports/precompute/src/main/java/"
NATIVE_JNI = ROOT / "m3/ports/precompute/src/main/native/src/m3_precompute_signals_jni.c"
REQUIRED_JNI_SYMBOL = (
    "Java_com_m3_precompute_M3CodeTextSignalBatchNative_nativeAnalyzeRange"
)
FORBIDDEN_RUNTIME_TOKENS = (
    "package com.synexia",
    "import com.synexia",
    "JniMIndexCodeTextSignalBatch",
)


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def main() -> None:
    with RECEIPT.open(newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))

    assert len(rows) == EXPECTED_ROWS, f"expected {EXPECTED_ROWS} rows, got {len(rows)}"

    seen_targets: set[str] = set()
    for row in rows:
        assert row["source_pr"] == EXPECTED_PR
        assert row["source_head"] == EXPECTED_HEAD
        assert row["source_merge"] == EXPECTED_MERGE
        assert row["source_blob"] and len(row["source_blob"]) == 40

        target = row["target_path"]
        assert target not in seen_targets, f"duplicate target mapping: {target}"
        seen_targets.add(target)
        assert target.startswith("m3/"), f"target escapes M3-owned tree: {target}"

        path = ROOT / target
        assert path.is_file(), f"missing target: {target}"
        actual_blob = git_blob_sha1(path.read_bytes())
        assert actual_blob == row["target_blob"], (
            f"target blob drift: {target}: expected {row['target_blob']}, got {actual_blob}"
        )

        if target.startswith(RUNTIME_PREFIX) and target.endswith(".java"):
            text = path.read_text(encoding="utf-8")
            assert "package com.m3.precompute;" in text, f"wrong package: {target}"
            assert Path(target).name.startswith("M3"), f"non-M3 runtime name: {target}"
            for token in FORBIDDEN_RUNTIME_TOKENS:
                assert token not in text, f"Synexia ABI leaked into target runtime: {target}: {token}"

        authority = row["authority"]
        assert authority, f"missing authority rule: {target}"
        assert "PROMOTION" not in authority, f"handoff cannot grant promotion: {target}"

    native = NATIVE_JNI.read_text(encoding="utf-8")
    assert REQUIRED_JNI_SYMBOL in native, "missing target-owned JNI symbol"
    assert "Java_com_synexia_" not in native, "Synexia JNI ABI leaked into M3 target"
    assert "JniMIndex" not in native, "duplicate donor JNI owner leaked into M3 target"

    # The isolated receiver must stay outside java.base until separately promoted.
    assert all(not row["target_path"].startswith("src/java.base/") for row in rows)

    print(
        "PASS synexia-pr9042-code-text-precompute "
        f"rows={len(rows)} target_blobs=verified naming=M3-owned runtime_synexia_dependency=false"
    )


if __name__ == "__main__":
    main()
