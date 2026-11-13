#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify the original JDK-8309665 transition and the inspected live successor.

The original receipt remains immutable. Scratch reconstruction verifies the
two exact historical transitions; no repository source is rewritten.
"""
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[5]
SOURCE = "src/java.base/share/classes/java/util/Arrays.java"
MAPPING = "doc/m3/name-mapping.json"
PLAN = "make/m3/tooling/name-contract/name-plan.json"
COMPANION = "src/jdk.m3.collections/share/classes/jdk/m3/arrays/M3Arrays.java"
OLD_BLOB = "2347e48700de1d26f630b348554ea9db3b4db25b"
NEW_BLOB = "c5412776d202bc6fb36a3a9e65ba35437a98cbef"
OLD_SHA = "e81e711d52fee23756fd0bf8e48c27ff0fe971419f4bb1c25ec1aa5ebc203a88"
NEW_SHA = "0ba39f0b1a0be0c1e8040f778253ea41b5342f34b39786be64988f8b31d12c73"
MAP_BLOB = "6bbdbdec9f1930b8282ac3d2d7693b99e036f465"
PATCH_BLOB = "90b87926492054fc4ac5107e6f52cb6b93cc9e38"
COMPANION_BLOB = "4145c7d2ec231f27cf05eea351521550800a8e0d"
OLD_COMMIT = "669a949d192345acefcfc5e9bb0b3724174d270c"
NEW_COMMIT = "3a8e844c6866b8d8c28c08fae2a35bdd2ca1985f"
CURRENT_COMMIT = "b9cc756f726de31af10652b5583d9bba58ba27b2"
CURRENT_BLOB = "23fcda31d0678a09caee8d458a7caeff12abec24"
CURRENT_SHA = "d05ab40d783e91cd608ffd76892f141bef38a82c0e105b67c4a6ee2f01faeb4f"
# V1 (companion at the pre-move m3/arrays path) is kept byte-identical as lineage of V2.
V1_RECEIPT = HERE.parent / "release-preparation/arrays-live-successor.json"
V1_RECEIPT_BLOB = "79550775b25d51bda6bac449adc447053291c3b8"
V1_RECEIPT_SHA = "19a70f45c2d80c29700b8bfa8b618f095fba3afc6a280ffc6da134e07f2998f8"
CURRENT_RECEIPT = HERE.parent / "release-preparation/arrays-live-successor-v2.json"
CURRENT_RECEIPT_BLOB = "4a7d726757fa89f57f7bb10a0dcae56579b8671c"
CURRENT_RECEIPT_SHA = "3baf055b421beeef576a47de60c4f28506d7a3f0e3bb486f95d753920ddf866e"
CURRENT_PATCH = CURRENT_RECEIPT.with_name("arrays-live-successor.patch.txt")
CURRENT_PATCH_BLOB = "fcc0e7620897279855999df1e5143f6e1792c21a"
CURRENT_PATCH_SHA = "38eecd500e05afedb6c64d9a177961ad9ded7b3b2cd2bb2636bdb34aa9a9a28c"


def git_blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def pinned(data, blob, digest=None):
    if git_blob(data) != blob or (digest is not None and hashlib.sha256(data).hexdigest() != digest):
        raise ValueError("source/patch/authority identity drift")


def read(file):
    if not file.is_file() or file.is_symlink():
        raise ValueError("missing or symlinked source: " + str(file))
    return file.read_bytes()


def _reverse_exact_patch(current, patch):
    with tempfile.TemporaryDirectory(prefix="m3-jdk-8309665-") as name:
        root = Path(name)
        file = root / SOURCE
        file.parent.mkdir(parents=True)
        file.write_bytes(current)
        diff = root / "jdk-8309665.patch.txt"
        diff.write_bytes(patch)
        for checking in (True, False):
            args = ["git", "apply", "--reverse"]
            if checking:
                args.append("--check")
            args.append(str(diff))
            run = subprocess.run(args, cwd=root, text=True, capture_output=True,
                                 check=False, timeout=30)
            if run.returncode:
                raise ValueError("historic OpenJDK patch cannot reverse: " + run.stderr)
        return file.read_bytes()


def reverse_patch(current, patch):
    pinned(current, NEW_BLOB, NEW_SHA)
    pinned(patch, PATCH_BLOB)
    restored = _reverse_exact_patch(current, patch)
    pinned(restored, OLD_BLOB, OLD_SHA)
    return restored


def reconstruct_source_chain(current, successor_patch, original_patch):
    pinned(current, CURRENT_BLOB, CURRENT_SHA)
    pinned(successor_patch, CURRENT_PATCH_BLOB, CURRENT_PATCH_SHA)
    historical = _reverse_exact_patch(current, successor_patch)
    pinned(historical, NEW_BLOB, NEW_SHA)
    original = reverse_patch(historical, original_patch)
    return historical, original


def verify(root=ROOT):
    mapping = read(root / MAPPING)
    plan = json.loads(read(root / PLAN))
    if plan.get("authority") != MAPPING or plan.get("authority_blob") != git_blob(mapping):
        raise ValueError("name plan authority differs")
    current_receipt_bytes = read(CURRENT_RECEIPT)
    pinned(current_receipt_bytes, CURRENT_RECEIPT_BLOB, CURRENT_RECEIPT_SHA)
    current_receipt = json.loads(current_receipt_bytes)
    v1_bytes = read(V1_RECEIPT)
    pinned(v1_bytes, V1_RECEIPT_BLOB, V1_RECEIPT_SHA)
    if current_receipt.get("supersedes", {}) != {
            **current_receipt.get("supersedes", {}), "path": "make/m3/tooling/name-contract/receipts/"
            "release-preparation/arrays-live-successor.json", "blob": V1_RECEIPT_BLOB, "sha256": V1_RECEIPT_SHA}:
        raise ValueError("successor receipt lineage altered")
    if (current_receipt["schema"] != "M3_ARRAYS_RELEASE_SOURCE_SUCCESSOR_V2"
            or current_receipt["source_commit"] != CURRENT_COMMIT
            or current_receipt["current_blob"] != CURRENT_BLOB
            or current_receipt["current_sha256"] != CURRENT_SHA
            or current_receipt["patch_blob"] != CURRENT_PATCH_BLOB):
        raise ValueError("current source receipt altered")
    records = json.loads(mapping)["migration"]["records"]
    matched = [x for x in records if x.get("id") == "m3.arrays.api-companion"]
    if len(matched) != 1:
        raise ValueError("companion relation duplicated/missing")
    record = matched[0]
    owners = [x for x in record["sources"] if x.get("path") == SOURCE]
    older = [x for x in record["lineage"]["previous_sources"] if x.get("path") == SOURCE]
    if len(owners) != 1:
        raise ValueError("current source ambiguous")
    previous = []
    for expected_blob in (NEW_BLOB, OLD_BLOB):
        matches = [row for row in older if row.get("git_blob_sha1") == expected_blob]
        if len(matches) != 1:
            raise ValueError("historical source ambiguous or missing")
        previous.append(matches[0])
    for row, commit, blob, digest in ((owners[0], CURRENT_COMMIT, CURRENT_BLOB, CURRENT_SHA),
                                     (previous[0], NEW_COMMIT, NEW_BLOB, NEW_SHA),
                                     (previous[1], OLD_COMMIT, OLD_BLOB, OLD_SHA)):
        if (row.get("commit"), row.get("git_blob_sha1"), row.get("sha256"),
                row.get("module"), row.get("symbol")) != (
                commit, blob, digest, "java.base", "java.util.Arrays"):
            raise ValueError("original or successor source pin mismatches")
    target = [x for x in record["targets"] if x.get("path") == COMPANION]
    companion = current_receipt["companion"]
    if (len(target) != 1 or target[0].get("git_blob_sha1") != companion["blob"]
            or target[0].get("sha256") != companion["sha256"]
            or target[0].get("module") != companion["module"]
            or target[0].get("symbol") != companion["symbol"]
            or record["sync"].get("source_revision") != CURRENT_COMMIT):
        raise ValueError("companion/sync changed")
    pinned(read(root / COMPANION), companion["blob"], companion["sha256"])
    receipt = json.loads(read(HERE / "RECEIPT.json"))
    if (receipt["schema"] != "M3_ARRAYS_JDK8309665_SOURCE_SUCCESSOR_V1"
            or receipt["old"]["blob"] != OLD_BLOB
            or receipt["current"]["blob"] != NEW_BLOB
            or receipt["map"]["new_blob"] != MAP_BLOB
            or receipt["jdk_patch_blob"] != PATCH_BLOB):
        raise ValueError("source receipt altered")
    current = read(root / SOURCE)
    historical, original = reconstruct_source_chain(
        current, read(CURRENT_PATCH), read(HERE / "jdk-8309665.patch.txt"))
    if current == historical or historical == original:
        raise ValueError("no actual JDK successor transition")
    return "M3_ARRAYS_JDK8309665_SUCCESSOR_PASS original=1 historical=1 current=1 patches=2 companion=1"


if __name__ == "__main__":
    try:
        print(verify())
    except (ValueError, OSError, KeyError, subprocess.SubprocessError) as error:
        print("M3_ARRAYS_JDK8309665_REFUSED: " + str(error), file=sys.stderr)
        sys.exit(1)
