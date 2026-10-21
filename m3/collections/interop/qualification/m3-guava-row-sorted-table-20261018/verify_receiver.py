# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Verify exact source-sealed Synexia templates against installed product sources."""
from pathlib import Path
import hashlib
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
CRATE = "m3-guava-row-sorted-table-20261018"
CANONICAL_SHA = "137abc1c56eed1d1eb3e705e69ada5bcd442efed"
RESOURCE = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-java" / CRATE


def verify():
    rows = [line.split("\t") for line in (RESOURCE / "manifest.tsv").read_text().splitlines()
            if line and not line.startswith("#")]
    if len(rows) != 3 or any(len(row) != 4 for row in rows):
        raise RuntimeError("expected exactly three source-sealed Java targets")
    paths = [row[0] for row in rows]
    if paths != sorted(paths) or len(set(paths)) != 3:
        raise RuntimeError("manifest must be sorted and unique")
    for path, before, expected, template_name in rows:
        if before != "ABSENT":
            raise RuntimeError("preimage was not ABSENT: " + path)
        if not path.startswith("m3/collections/interop/src/"):
            raise RuntimeError("unexpected output path: " + path)
        template = RESOURCE / template_name
        target = ROOT / path
        if not template.is_file() or template.is_symlink():
            raise RuntimeError("missing canonical template: " + str(template))
        content = template.read_bytes()
        if hashlib.sha256(content).hexdigest() != expected:
            raise RuntimeError("canonical SHA-256 drift: " + path)
        if not target.is_file() or target.is_symlink():
            raise RuntimeError("missing generated product source: " + path)
        if target.read_bytes() != content:
            raise RuntimeError("product source differs from recipe postimage: " + path)
    print("M3_GUAVA_ROWSORTEDTABLE_SHA256_PASS", len(rows), CANONICAL_SHA)


if __name__ == "__main__":
    try:
        verify()
    except Exception as e:
        print("M3_GUAVA_ROWSORTEDTABLE_VERIFY_FAILED:", e, file=sys.stderr)
        raise
