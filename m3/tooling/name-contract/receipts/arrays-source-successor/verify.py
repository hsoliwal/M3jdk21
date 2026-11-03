#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify a reversible JDK-8309665 successor without weakening the original name gate."""
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
SOURCE = "src/java.base/share/classes/java/util/Arrays.java"
MAPPING = "m3/docs/name-mapping.json"
PLAN = "m3/tooling/name-contract/name-plan.json"
COMPANION = "m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java"
OLD_BLOB = "2347e48700de1d26f630b348554ea9db3b4db25b"
NEW_BLOB = "c5412776d202bc6fb36a3a9e65ba35437a98cbef"
OLD_SHA = "e81e711d52fee23756fd0bf8e48c27ff0fe971419f4bb1c25ec1aa5ebc203a88"
NEW_SHA = "0ba39f0b1a0be0c1e8040f778253ea41b5342f34b39786be64988f8b31d12c73"
MAP_BLOB = "6bbdbdec9f1930b8282ac3d2d7693b99e036f465"
PATCH_BLOB = "90b87926492054fc4ac5107e6f52cb6b93cc9e38"
COMPANION_BLOB = "ffece349f5ef3f1c48191fb54a8b037f8e635d63"
OLD_COMMIT = "669a949d192345acefcfc5e9bb0b3724174d270c"
NEW_COMMIT = "3a8e844c6866b8d8c28c08fae2a35bdd2ca1985f"


def git_blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def pinned(data, blob, digest=None):
    if git_blob(data) != blob or (digest is not None and hashlib.sha256(data).hexdigest() != digest):
        raise ValueError("source/patch/authority identity drift")


def read(file):
    if not file.is_file() or file.is_symlink():
        raise ValueError("missing or symlinked source: " + str(file))
    return file.read_bytes()


def reverse_patch(current, patch):
    pinned(current, NEW_BLOB, NEW_SHA)
    pinned(patch, PATCH_BLOB)
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
        restored = file.read_bytes()
        pinned(restored, OLD_BLOB, OLD_SHA)
        return restored


def verify(root=ROOT):
    mapping = read(root / MAPPING)
    pinned(mapping, MAP_BLOB)
    plan = json.loads(read(root / PLAN))
    if plan.get("authority") != MAPPING or plan.get("authority_blob") != MAP_BLOB:
        raise ValueError("name plan authority differs")
    records = json.loads(mapping)["migration"]["records"]
    matched = [x for x in records if x.get("id") == "m3.arrays.api-companion"]
    if len(matched) != 1:
        raise ValueError("companion relation duplicated/missing")
    record = matched[0]
    owners = [x for x in record["sources"] if x.get("path") == SOURCE]
    older = [x for x in record["lineage"]["previous_sources"] if x.get("path") == SOURCE]
    if len(owners) != 1 or len(older) != 1:
        raise ValueError("historical/current source ambiguous")
    for row, commit, blob, digest in ((owners[0], NEW_COMMIT, NEW_BLOB, NEW_SHA),
                                     (older[0], OLD_COMMIT, OLD_BLOB, OLD_SHA)):
        if (row.get("commit"), row.get("git_blob_sha1"), row.get("sha256"),
                row.get("module"), row.get("symbol")) != (
                commit, blob, digest, "java.base", "java.util.Arrays"):
            raise ValueError("original or successor source pin mismatches")
    target = [x for x in record["targets"] if x.get("path") == COMPANION]
    if (len(target) != 1 or target[0].get("git_blob_sha1") != COMPANION_BLOB
            or record["sync"].get("source_revision") != NEW_COMMIT):
        raise ValueError("companion/sync changed")
    pinned(read(root / COMPANION), COMPANION_BLOB)
    receipt = json.loads(read(HERE / "RECEIPT.json"))
    if (receipt["schema"] != "M3_ARRAYS_JDK8309665_SOURCE_SUCCESSOR_V1"
            or receipt["old"]["blob"] != OLD_BLOB
            or receipt["current"]["blob"] != NEW_BLOB
            or receipt["map"]["new_blob"] != MAP_BLOB
            or receipt["jdk_patch_blob"] != PATCH_BLOB):
        raise ValueError("source receipt altered")
    current = read(root / SOURCE)
    historical = reverse_patch(current, read(HERE / "jdk-8309665.patch.txt"))
    if current == historical:
        raise ValueError("no actual JDK successor transition")
    return "M3_ARRAYS_JDK8309665_SUCCESSOR_PASS old=1 current=1 original_patch=1 companion=1"


if __name__ == "__main__":
    try:
        print(verify())
    except (ValueError, OSError, KeyError, subprocess.SubprocessError) as error:
        print("M3_ARRAYS_JDK8309665_REFUSED: " + str(error), file=sys.stderr)
        sys.exit(1)
