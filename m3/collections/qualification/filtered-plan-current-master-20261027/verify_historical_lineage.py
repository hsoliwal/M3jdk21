#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify old source receipts as immutable Git objects, not current files.

Historical exact-source verifiers correctly reject changed current owners.
This verifier proves custody of those sealed historical objects, while the
separate successor gate must still test the current product source.
"""
import csv
import hashlib
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[4]
MANIFEST = ROOT / "m3/collections/qualification/filtered-cursor-mask-20261021/SOURCE_MANIFEST.tsv"


SEALED_MANIFEST_BLOB = "4ca33e23eabc23764f7e43f30cba9b753cbd0b1f"


def verify_manifest_identity() -> None:
    if MANIFEST.is_symlink() or not MANIFEST.is_file():
        raise ValueError("missing or symbolic historical manifest")
    actual = subprocess.check_output(
        ["git", "-C", str(ROOT), "hash-object", str(MANIFEST)], text=True
    ).strip()
    if actual != SEALED_MANIFEST_BLOB:
        raise ValueError(f"historical manifest changed: {actual}")


def git_blob(sha: str) -> bytes:
    if len(sha) != 40 or any(c not in "0123456789abcdef" for c in sha):
        raise ValueError("invalid Git object id")
    kind = subprocess.check_output(
        ["git", "-C", str(ROOT), "cat-file", "-t", sha], text=True
    ).strip()
    if kind != "blob":
        raise ValueError(f"object is not a blob: {sha}")
    return subprocess.check_output(["git", "-C", str(ROOT), "cat-file", "blob", sha])


def main() -> int:
    verify_manifest_identity()
    records = []
    with MANIFEST.open(newline="", encoding="utf-8") as source:
        for row in csv.reader(source, delimiter="\t"):
            if not row or row[0].startswith("#"):
                continue
            if len(row) != 5:
                raise ValueError("invalid manifest row")
            records.append(row)
    if len(records) != 8 or [r[0] for r in records] != sorted(r[0] for r in records):
        raise ValueError("historical manifest cardinality/order changed")
    changed = []
    for path, before, after, blob, resource in records:
        p = pathlib.PurePosixPath(path)
        if p.is_absolute() or ".." in p.parts or not resource.endswith(".txt"):
            raise ValueError(f"unsafe historical record: {path}")
        original = git_blob(blob)
        if hashlib.sha256(original).hexdigest() != after:
            raise ValueError(f"historical seal invalid: {path}")
        current = ROOT / path
        current_sha = hashlib.sha256(current.read_bytes()).hexdigest() if current.is_file() else None
        if current_sha != after:
            changed.append(path)
    print(f"PASS: {len(records)} historical Git objects match their SHA-256 receipts")
    print(f"CURRENT_DIVERGENCES: {len(changed)} (expected for separately refined owners)")
    for path in changed:
        print(f"HISTORICAL_ONLY: {path}")
    print("NOTE: current behavior must pass successor recipe, Java/JUnit/JNI gates separately")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"REFUSAL: {error}", file=sys.stderr)
        sys.exit(1)
