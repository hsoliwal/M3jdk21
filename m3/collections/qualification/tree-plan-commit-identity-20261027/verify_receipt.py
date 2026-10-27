#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only M3JDK21 receiver proof: hash pinned Java postimages and catalogue.

No source modifications, no OpenRewrite implementation and no background work.
"""
from __future__ import annotations

import csv
import hashlib
import io
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[4]
RECEIPT = Path(__file__).resolve().parent / "RECEIVER_RECEIPT.json"
SCHEMA = "M3JDK21_NATIVE_TREE_PLAN_COMMIT_RECEIVER_V1"
SHA1 = re.compile(r"^[0-9a-f]{40}$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")
EXPECTED = {
    "m3/collections/src/main/java/com/m3/collections/M3TreeWindowPlan.java",
    "m3/collections/src/test/java/com/m3/collections/M3TreeCommitPlanIdentityTest.java",
}
CATALOGUE = "m3/collections/algorithm-catalog/tree-plan-commit-identity-20261027.tsv"


def refuse(message: str) -> None:
    raise ValueError(message)


def exact_json(pairs: list[tuple[str, object]]) -> dict:
    result: dict = {}
    for key, value in pairs:
        if key in result:
            refuse("duplicate receipt JSON key: " + key)
        result[key] = value
    return result


def source_path(root: Path, name: str) -> Path:
    if (not isinstance(name, str) or not name or "\x00" in name
            or "\\" in name or name.startswith("/")
            or re.match(r"^[A-Za-z]:", name)):
        refuse("unsafe source path")
    pieces = name.split("/")
    if any(piece in ("", ".", "..") for piece in pieces):
        refuse("noncanonical source path: " + name)
    base = root.resolve()
    current = base
    for piece in pieces:
        current = current / piece
        if current.is_symlink():
            refuse("symlink source path: " + name)
    location = current.resolve()
    if not location.is_relative_to(base):
        refuse("source path leaves checkout: " + name)
    return location


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\x00" + data).hexdigest()


def verify_digest(data: bytes, expected_blob: str, expected_sha256: str) -> None:
    if not SHA1.fullmatch(expected_blob) or not SHA256.fullmatch(expected_sha256):
        refuse("malformed source identity in receipt")
    if git_blob(data) != expected_blob:
        refuse("Git postimage blob drift")
    if hashlib.sha256(data).hexdigest() != expected_sha256:
        refuse("SHA-256 postimage drift")


def validate(root: Path = ROOT, receipt: Path = RECEIPT) -> dict[str, object]:
    record = json.loads(receipt.read_text(encoding="utf-8"),
                        object_pairs_hook=exact_json)
    if record.get("schema") != SCHEMA:
        refuse("unknown M3JDK21 receiver schema")
    if record.get("decision") != "DRAFT_REVIEWABLE_ATOM_NOT_JDK_RUNTIME_PROMOTED":
        refuse("source-only receipt falsely claims runtime qualification")
    if record.get("product", {}).get("java_se_public_api_changed") is not False:
        refuse("unexpected canonical JDK API change")
    if record.get("product", {}).get("jni_abi_changed") is not False:
        refuse("unexpected JNI ABI mutation")
    files = record.get("files")
    if not isinstance(files, list) or len(files) != 2:
        refuse("unexpected number of received Java postimages")
    seen: set[str] = set()
    for item in files:
        if not isinstance(item, dict):
            refuse("invalid Java receipt row")
        path = item.get("path")
        if path in seen:
            refuse("duplicate received Java path")
        seen.add(path)
        raw = source_path(root, path).read_bytes()
        verify_digest(raw, item.get("postimage_git_blob_sha1"),
                      item.get("postimage_sha256"))
        if not path.endswith(".java"):
            refuse("non-Java artifact inside implementation scope")
        if path.endswith("M3TreeWindowPlan.java"):
            if not SHA1.fullmatch(item.get("preimage_git_blob_sha1", "")):
                refuse("missing exact original owner Git blob")
            if not SHA256.fullmatch(item.get("preimage_sha256", "")):
                refuse("missing original owner SHA-256")
            if b"commit differs from planned window" not in raw:
                refuse("missing native plan identity admission")
            if b"retained native handle changed" not in raw:
                refuse("missing native handle identity admission")
        elif item.get("preimage_git_blob_sha1") is not None:
            refuse("new JUnit oracle was not ABSENT at baseline")
    if seen != EXPECTED:
        refuse("unexpected Java implementation or test scope")
    if record["catalogue"]["path"] != CATALOGUE:
        refuse("wrong algorithm decision catalogue")
    catalogue = source_path(root, CATALOGUE).read_bytes()
    if git_blob(catalogue) != record["catalogue"]["git_blob_sha1"]:
        refuse("algorithm donor/cost selection catalogue drift")
    text = catalogue.decode("utf-8")
    rows = list(csv.DictReader(io.StringIO(text), delimiter="\t"))
    if len(rows) != record["catalogue"]["problem_categories"]:
        refuse("missing algorithm decision rows")
    categories = [row.get("problem_category") for row in rows]
    if len(categories) != len(set(categories)):
        refuse("ambiguous algorithm problem category")
    if not {"ARRAY_COORDINATE_IDENTITY", "NATIVE_HANDLE_REUSE",
            "PHYSICAL_PARENT_OFFSET", "JNI_MASK_WINDOW"}.issubset(categories):
        refuse("lost core array/native problem categories")
    for row in rows:
        if not row.get("selection") or not row.get("custody_and_license"):
            refuse("unreviewed donor algorithm selection")
    return {
        "schema": SCHEMA,
        "files": len(files),
        "categories": len(rows),
        "digest": hashlib.sha256(receipt.read_bytes()).hexdigest(),
        "runtime_qualified": False,
    }


def main() -> int:
    try:
        result = validate()
        print(json.dumps(result, sort_keys=True))
        return 0
    except (ValueError, OSError, UnicodeError, json.JSONDecodeError, TypeError) as error:
        print("M3JDK21_TREE_PLAN_RECEIPT_REFUSED: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
