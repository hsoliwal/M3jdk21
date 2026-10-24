#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only M3JDK21 source-receiver evidence verification, not a transform.

The canonical source mutation recipe belongs only to Synexia. This checker
verifies its actual public-product outputs against immutable Git blob and
SHA-256 identities before collection compilation. No source is changed.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import re
import sys

REPO_ROOT = Path(__file__).resolve().parents[4]
RECEIPT = Path("m3/collections/qualification/arraylist-dual-word-20261023/RECEIVER_RECEIPT.json")
SHA1 = re.compile(r"^[a-f0-9]{40}$")
SHA256 = re.compile(r"^[a-f0-9]{64}$")
CATEGORIES = re.compile(r"^[A-Z][A-Z0-9_]{2,100}$")
EXPECTED_ROLES = {
    "m3/collections/src/main/java/com/m3/collections/M3ArrayList.java": "implementation_postimage",
    "m3/collections/src/test/java/com/m3/collections/M3ArrayListShortWordMaskContract.java": "differential_oracle_postimage",
    "m3/arrays/pom.xml": "test_only_maven_dependency_postimage",
    "m3/collections/src/test/java/com/m3/collections/M3LongTreeSetMaskCompactionContract.java": "one_character_java21_test_syntax_postimage",
    "m3/collections/src/main/java/com/m3/collections/M3TreeWindowPlan.java": "atomic_tree_sibling_key_validation_postimage",
    "m3/collections/src/test/java/com/m3/collections/M3TreeCoverageTest.java": "negative_and_overflow_atomic_refusal_regression",
}
NATIVE_PATHS = {
    "m3/collections/src/main/native/collections/m3_index_mask_compactor.c",
    "m3/collections/src/main/native/collections/m3_index_mask_window.c",
}
FIELDS = (
    "schema", "problem_category", "source_owner", "source_preimage_sha256",
    "candidate", "algorithm_reference", "complexity",
    "contract_and_failure_semantics", "measured_or_observed",
    "selection", "custody_and_license", "next_gate",
)


def refuse(message: str) -> None:
    raise ValueError(message)


def unique_keys(pairs: list[tuple[str, object]]) -> dict:
    out = {}
    for key, value in pairs:
        if key in out:
            refuse("duplicate JSON key: " + key)
        out[key] = value
    return out


def safe_file(root: Path, relative: str) -> Path:
    if not isinstance(relative, str) or not relative or "\\" in relative or "\x00" in relative:
        refuse("invalid source path")
    segments = relative.split("/")
    path = Path(relative)
    if path.is_absolute() or any(s in ("", ".", "..") for s in segments):
        refuse("noncanonical source path: " + relative)
    base = root.resolve()
    target = base
    for segment in segments:
        target = target / segment
        if target.is_symlink():
            refuse("source symlink: " + relative)
    if not target.resolve().is_relative_to(base) or not target.is_file():
        refuse("missing/out-of-root source: " + relative)
    return target


def git_blob(raw: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()


def check_blob(root: Path, path: str, expected: str) -> bytes:
    if not isinstance(expected, str) or not SHA1.fullmatch(expected):
        refuse("invalid Git blob SHA-1 pin: " + path)
    source = safe_file(root, path).read_bytes()
    if git_blob(source) != expected:
        refuse("Git source postimage drift: " + path)
    return source


def verify_catalogue(raw: bytes, count: int) -> None:
    try:
        content = raw.decode("utf-8")
    except UnicodeError as error:
        raise ValueError("unparseable catalogue") from error
    rows = list(csv.DictReader(io.StringIO(content, newline=""), delimiter="\t"))
    if not rows or len(rows) != count:
        refuse("catalogue row count drift")
    fields = content.splitlines()[0].split("\t")
    if tuple(fields) != FIELDS:
        refuse("catalogue field contract drift")
    names = set()
    for row in rows:
        if None in row or any(value is None for value in row.values()):
            refuse("invalid catalogue row width")
        name = row["problem_category"]
        if row["schema"] != "M3_ALGORITHM_CATALOG_V1" or not CATEGORIES.fullmatch(name) or name in names:
            refuse("ambiguous donor problem category")
        names.add(name)
        if not row["source_owner"].startswith(("m3/", "src/")):
            refuse("invalid target source owner")
        pin = row["source_preimage_sha256"]
        if pin and not SHA256.fullmatch(pin):
            refuse("invalid SHA256 source pin: " + name)
        for field in FIELDS[4:]:
            if not row[field].strip():
                refuse("missing donor evidence/contract/license: " + name)
        if not row["selection"].startswith(("SELECT_", "KEEP_", "REUSE_", "REJECT_", "REFERENCE_")):
            refuse("unqualified donor decision: " + name)
        if any(not link.strip().startswith("https://") for link in row["algorithm_reference"].split(";")):
            refuse("unattributed donor URL: " + name)


def verify(root: Path = REPO_ROOT) -> dict[str, int]:
    receipt = json.loads(safe_file(root, RECEIPT.as_posix()).read_text(encoding="utf-8"),
                         object_pairs_hook=unique_keys)
    if receipt.get("schema") != "M3JDK21_COLLECTION_ATOM_RECEIVER_V1":
        refuse("unknown receiver authority")
    product = receipt.get("product", {})
    if (product.get("repository") != "hsoliwal/M3jdk21"
            or product.get("public_jdk_api_mutated") is not False):
        refuse("incorrect M3JDK21 canonical JDK public-API gate")
    files = receipt.get("files")
    if not isinstance(files, list) or len(files) != len(EXPECTED_ROLES):
        refuse("receiver postimage count drift")
    seen = set()
    for entry in files:
        if not isinstance(entry, dict):
            refuse("malformed source owner row")
        path = entry.get("path")
        if path in seen or EXPECTED_ROLES.get(path) != entry.get("role"):
            refuse("duplicate/unexpected product receiving owner: " + str(path))
        seen.add(path)
        raw = check_blob(root, path, entry.get("postimage_git_blob_sha1"))
        checksum = entry.get("postimage_sha256")
        if not isinstance(checksum, str) or not SHA256.fullmatch(checksum):
            refuse("missing SHA256 receiving postimage: " + path)
        if hashlib.sha256(raw).hexdigest() != checksum:
            refuse("SHA256 receiving postimage drift: " + path)
        for key, pattern in (("preimage_git_blob_sha1", SHA1),
                             ("preimage_sha256", SHA256)):
            if not isinstance(entry.get(key), str) or not pattern.fullmatch(entry[key]):
                refuse("invalid original source pin: " + path)
        if checksum == entry["preimage_sha256"]:
            refuse("unmodified source claimed as candidate: " + path)
    if seen != set(EXPECTED_ROLES):
        refuse("incomplete target source closure")
    catalogue = receipt.get("catalogue", {})
    data = check_blob(root, catalogue.get("path"), catalogue.get("git_blob_sha1"))
    verify_catalogue(data, catalogue.get("category_count"))
    native = receipt.get("unchanged_native_sources")
    if not isinstance(native, list) or {row.get("path") for row in native} != NATIVE_PATHS:
        refuse("JNI native evidence closure drift")
    for row in native:
        check_blob(root, row.get("path"), row.get("git_blob_sha1"))
    if receipt.get("admission", {}).get("state") != "DRAFT_REVIEW_CANDIDATE_NOT_CANONICAL_JDK_PROMOTION":
        refuse("unapproved canonical JDK promotion")
    return {"source_postimages": len(seen), "catalogue_categories": len(
        data.decode("utf-8").splitlines()) - 1, "unchanged_native_sources": len(native)}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=REPO_ROOT)
    args = parser.parse_args(argv)
    try:
        result = verify(args.root)
    except (ValueError, OSError, UnicodeError, TypeError, KeyError, json.JSONDecodeError) as error:
        print("M3JDK_COLLECTION_RECEIVER_REFUSED: " + str(error), file=sys.stderr)
        return 2
    print("M3JDK_COLLECTION_RECEIVER_PASS "
          + " ".join(key + "=" + str(value) for key, value in sorted(result.items())))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
