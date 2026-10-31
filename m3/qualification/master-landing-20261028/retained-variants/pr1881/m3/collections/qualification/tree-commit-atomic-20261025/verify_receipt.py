#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only product receiving proof; Synexia alone owns the transformation."""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[4]
RECEIPT = "m3/collections/qualification/tree-commit-atomic-20261025/RECEIVER_RECEIPT.json"
PARENT = "m3/collections/qualification/arraylist-dual-word-20261023/RECEIVER_RECEIPT.json"
SOURCE = "m3/collections/src/main/java/com/m3/collections/M3TreeWindowPlan.java"
TEST = "m3/collections/src/test/java/com/m3/collections/M3TreeCommitAtomicityTest.java"
CATALOGUE = "m3/collections/algorithm-catalog/tree-commit-atomic-20261025.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")
HEX64 = re.compile(r"^[0-9a-f]{64}$")


def unique(pairs: list[tuple[str, object]]) -> dict:
    value = {}
    for key, item in pairs:
        if key in value:
            raise ValueError("duplicate receiver JSON key: " + key)
        value[key] = item
    return value


def read(root: Path, relative: str) -> bytes:
    if relative not in {RECEIPT, PARENT, SOURCE, TEST, CATALOGUE}:
        raise ValueError("unapproved product receiver path: " + str(relative))
    path = root.resolve()
    for part in relative.split("/"):
        path = path / part
        if path.is_symlink():
            raise ValueError("source symlink: " + relative)
    if not path.is_file() or not path.resolve().is_relative_to(root.resolve()):
        raise ValueError("missing or escaped receiver path: " + relative)
    return path.read_bytes()


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def checked_json(raw: bytes) -> dict:
    value = json.loads(raw.decode("utf-8"), object_pairs_hook=unique)
    if not isinstance(value, dict):
        raise ValueError("receipt must be a JSON object")
    return value


def verify(root: Path = ROOT) -> dict[str, int]:
    receipt = checked_json(read(root, RECEIPT))
    if receipt.get("schema") != "M3JDK21_TREE_NATIVE_COMMIT_ATOMIC_RECEIVER_V1":
        raise ValueError("unrecognized target receipt")
    product = receipt.get("product", {})
    if (product.get("repository") != "hsoliwal/M3jdk21"
            or product.get("jdk_java_se_api_changed") is not False
            or product.get("jni_abi_changed") is not False):
        raise ValueError("unqualified public JDK or JNI contract mutation")
    if receipt.get("admission") != "DRAFT_REVIEW_NOT_RUNTIME_PROMOTED":
        raise ValueError("unauthorized tree backend promotion")
    files = receipt.get("files", [])
    if (not isinstance(files, list) or len(files) != 2
            or {entry.get("path") for entry in files} != {SOURCE, TEST}):
        raise ValueError("unexpected source/test closure")
    for entry in files:
        path = entry["path"]
        content = read(root, path)
        expected_blob, expected_digest = entry.get("after_git_blob_sha1"), entry.get("after_sha256")
        if (not isinstance(expected_blob, str) or not HEX40.fullmatch(expected_blob)
                or not isinstance(expected_digest, str) or not HEX64.fullmatch(expected_digest)):
            raise ValueError("invalid receiving hash pin")
        if git_blob(content) != expected_blob or hashlib.sha256(content).hexdigest() != expected_digest:
            raise ValueError("source-sealed postimage drift: " + path)
    parent = read(root, PARENT)
    history = receipt.get("history", {})
    if history.get("previous_receiver") != PARENT or git_blob(parent) != history.get("previous_receiver_blob_sha1"):
        raise ValueError("historical receiving receipt drift")
    parent_doc = checked_json(parent)
    owner = next((entry for entry in parent_doc.get("files", []) if entry.get("path") == SOURCE), None)
    source_pin = next(entry for entry in files if entry["path"] == SOURCE)
    if (owner is None or owner.get("postimage_git_blob_sha1") != source_pin["after_git_blob_sha1"]
            or owner.get("previous_20261025", {}).get("git_blob_sha1")
                    != history.get("previous_source_postimage_git_blob_sha1")):
        raise ValueError("source-history DAG or parent receiver postimage drift")
    cat = receipt.get("catalogue", {})
    if cat.get("path") != CATALOGUE:
        raise ValueError("unapproved algorithm catalogue")
    text = read(root, CATALOGUE)
    if git_blob(text) != cat.get("git_blob_sha1"):
        raise ValueError("algorithm catalogue source drift")
    rows = text.decode("utf-8").splitlines()
    if len(rows) != cat.get("rows", -1) + 1 or len(rows) < 2:
        raise ValueError("algorithm catalogue count drift")
    names = [line.split("\t")[1] for line in rows[1:]]
    if len(names) != len(set(names)) or "FAILURE_ATOMIC_NATIVE_COMMIT" not in names:
        raise ValueError("missing unique indexed problem category")
    return {"postimages": len(files), "categories": len(names), "parent_source_history": 1}


if __name__ == "__main__":
    try:
        result = verify()
    except (ValueError, OSError, UnicodeError, TypeError, KeyError, json.JSONDecodeError) as failure:
        print("M3_TREE_COMMIT_RECEIVER_REFUSED: " + str(failure), file=sys.stderr)
        raise SystemExit(2)
    print("M3_TREE_COMMIT_RECEIVER_PASS "
          + " ".join(f"{key}={value}" for key, value in sorted(result.items())))
