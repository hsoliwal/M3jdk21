#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact source receipt: verifies recipe output, untouched C/Java JNI and catalog."""
import hashlib
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[3]  # repo root: m3/collections/qualification/<crate>/verify.py
TEMPLATES = (ROOT / "m3/tooling/migration-recipes/src/main/resources"
    / "com/synexia/rewrite/hash-pinned-java/m3jdk21-long-range-array-flags-20261017")
MANIFEST = TEMPLATES / "manifest.tsv"

def checked_data(p):
    if p.is_symlink() or not p.is_file():
        raise ValueError("missing or symlinked input: " + str(p))
    return p.read_bytes()

def sha256(data):
    return hashlib.sha256(data).hexdigest()

def git_blob(data):
    return hashlib.sha1(("blob " + str(len(data)) + "\0").encode("ascii") + data).hexdigest()

def verify():
    records = []
    for line in checked_data(MANIFEST).decode("utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        row = line.split("\t")
        if len(row) != 4:
            raise ValueError("bad manifest row")
        path, before, after, template = row
        if before != "ABSENT" or len(after) != 64 or not path.startswith(
                ("m3/collections/src/main/java/com/m3/collections/",
                 "m3/collections/src/test/java/com/m3/collections/")):
            raise ValueError("wrong target ownership")
        if pathlib.PurePosixPath(path).name + ".txt" != template or ".." in pathlib.PurePosixPath(path).parts:
            raise ValueError("target or resource name drift")
        original = checked_data(ROOT / path)
        expected = checked_data(TEMPLATES / template)
        if sha256(expected) != after or sha256(original) != after or original != expected:
            raise ValueError("target not an exact canonical recipe postimage: " + path)
        records.append((path, after))
    if len(records) != 3 or [r[0] for r in records] != sorted(set(r[0] for r in records)):
        raise ValueError("missing/duplicate/unordered recipe targets")

    owners = {
        "m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java":
            "ef0fac5c1e71f8493baab365c8af64bb22cb6171",
        "m3/collections/src/main/java/com/m3/collections/M3LongLongConsumer.java":
            "b1c01d1afacc76bd338179398ab42835e16992fa",
        "m3/collections/src/main/native/collections/m3_index_path_masks.c":
            "086b49293257fd17cc7fbd1ebbb98460aa677e35",
    }
    for path, blob in owners.items():
        if git_blob(checked_data(ROOT / path)) != blob:
            raise ValueError("original Java/JNI owner drift: " + path)

    # The catalogue remains the actual Synexia-authored donor/category review.
    cat = checked_data(HERE / "CATEGORY_REVIEW.tsv").decode("utf-8").splitlines()
    if len(cat) < 5 or not cat[0].startswith("category\t"):
        raise ValueError("unqualified algorithm catalogue")
    receipt = checked_data(HERE / "RECEIPT.tsv")
    if b"DRAFT_WAITING_FOR_STRING" not in receipt:
        raise ValueError("admission state accidentally promoted")
    print("PASS M3_LONG_RANGE_RECEIVER targets=3 canonical_owner_blobs=3 catalogue_rows=" + str(len(cat)-1))

if __name__ == "__main__":
    try:
        verify()
    except Exception as err:
        print("REFUSE M3_LONG_RANGE_RECEIVER: " + str(err), file=sys.stderr)
        sys.exit(1)
