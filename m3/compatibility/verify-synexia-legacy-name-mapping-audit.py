#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed audit of the 47 legacy Synexia name-mapping rows."""

from __future__ import annotations

import csv
import hashlib
import json
from pathlib import Path

MAPPING_PATH = Path(__file__).parents[1] / "name-mapping.json"
AUDIT_PATH = Path(__file__).with_name("synexia-legacy-name-mapping-audit-20261009.tsv")
EXPECTED_MAPPING_BLOB = "c29a64eaea0ca614d551ad4bcd3fa2695f0fdc9e"
EXPECTED_ROWS = 47
EXPECTED_PROOF = "M3JDK_SYNEXIA_LEGACY_MAPPING_AUDIT_PASS rows=47 unpinned_rows=47"


def git_blob_sha1(payload: bytes) -> str:
    header = f"blob {len(payload)}\\0".encode("ascii")
    return hashlib.sha1(header + payload).hexdigest()


def disposition(status: str) -> tuple[str, str]:
    value = status.lower()
    if "donor-only" in value or "do not port" in value:
        return "NO_TARGET_BY_POLICY", "DONOR_ONLY_OR_EXCLUDED"
    if value == "pending":
        return "PENDING", "PENDING_TARGET_DESIGN"
    if (
        "inventory mapping" in value
        or "requires per-type" in value
        or "provisional" in value
        or "review-required" in value
    ):
        return "REVIEW_REQUIRED", "REQUIRES_TARGET_CONSUMER_REVIEW"
    return "UNPINNED", "REQUIRES_EXACT_TARGET_RECEIPT"


def fail(message: str) -> None:
    raise SystemExit(f"M3JDK_SYNEXIA_LEGACY_MAPPING_AUDIT_FAIL {message}")


def main() -> int:
    if not MAPPING_PATH.is_file() or not AUDIT_PATH.is_file():
        fail("required mapping or audit file is missing")
    mapping_bytes = MAPPING_PATH.read_bytes()
    if git_blob_sha1(mapping_bytes) != EXPECTED_MAPPING_BLOB:
        fail("name-mapping.json blob drift")
    mapping = json.loads(mapping_bytes.decode("utf-8"))
    source_rows = mapping.get("mappings")
    if not isinstance(source_rows, list) or len(source_rows) != EXPECTED_ROWS:
        fail("source mapping row-count drift")

    comments: list[str] = []
    data_lines: list[str] = []
    for line in AUDIT_PATH.read_text(encoding="utf-8").splitlines():
        if line.startswith("#"):
            comments.append(line)
        elif line.strip():
            data_lines.append(line)
    if EXPECTED_PROOF not in comments:
        fail("proof marker missing")
    expected_header = [
        "mapping_index",
        "source",
        "target",
        "status",
        "source_commit",
        "target_receipt_state",
        "disposition",
    ]
    rows = list(csv.reader(data_lines, delimiter="\\t"))
    if not rows or rows[0] != expected_header:
        fail("audit header drift")
    audit_rows = rows[1:]
    if len(audit_rows) != EXPECTED_ROWS:
        fail(f"expected {EXPECTED_ROWS} audit rows, got {len(audit_rows)}")

    unpinned = 0
    for index, (source, audit_row) in enumerate(zip(source_rows, audit_rows)):
        if len(audit_row) != len(expected_header):
            fail(f"column-count drift at row {index}")
        mapping_index, source_name, target, status, source_commit, state, reason = audit_row
        if mapping_index != str(index):
            fail(f"mapping index drift at row {index}")
        if (source_name, target, status, source_commit) != (
            source.get("source", ""),
            source.get("target", ""),
            source.get("status", ""),
            source.get("source_commit") or "",
        ):
            fail(f"source mapping drift at row {index}")
        expected_state, expected_reason = disposition(status)
        if (state, reason) != (expected_state, expected_reason):
            fail(f"disposition drift at row {index}")
        if state == "UNPINNED":
            unpinned += 1
    if unpinned != EXPECTED_ROWS:
        fail(f"expected all {EXPECTED_ROWS} rows to remain unpinned, got {unpinned}")
    print(EXPECTED_PROOF)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
