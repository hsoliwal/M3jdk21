#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only M3JDK21 collection receiver: exact source Git-blob and SHA-256 admission."""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import re
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
RECEIPT = HERE / "RECEIVER_RECEIPT.json"
SHA1 = re.compile(r"^[0-9a-f]{40}$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")


def no_duplicate_keys(pairs: list[tuple[str, object]]) -> dict:
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate receipt JSON key: " + key)
        result[key] = value
    return result


def blob_sha1(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def exact_file(root: Path, relative: str) -> Path:
    if (not isinstance(relative, str) or not relative.startswith("m3/")
            or "\\" in relative or "\x00" in relative
            or any(part in ("", ".", "..") for part in relative.split("/"))):
        raise ValueError("invalid canonical target path: " + str(relative))
    base = root.resolve()
    current = base
    for part in relative.split("/"):
        current = current / part
        if current.is_symlink():
            raise ValueError("symlink target path refused: " + relative)
    resolved = current.resolve()
    if not resolved.is_relative_to(base) or not resolved.is_file():
        raise ValueError("missing or escaped source file: " + relative)
    return resolved


def verify_blob(root: Path, row: dict, after_key: str) -> None:
    path = exact_file(root, row["path"])
    data = path.read_bytes()
    expected_blob = row[after_key]
    if not isinstance(expected_blob, str) or not SHA1.fullmatch(expected_blob):
        raise ValueError("invalid pinned Git blob: " + row["path"])
    if blob_sha1(data) != expected_blob:
        raise ValueError("source Git blob drift: " + row["path"])
    if "sha256_after" in row:
        if not SHA256.fullmatch(row["sha256_before"]) or not SHA256.fullmatch(row["sha256_after"]):
            raise ValueError("invalid source pre/post SHA-256: " + row["path"])
        if hashlib.sha256(data).hexdigest() != row["sha256_after"]:
            raise ValueError("source SHA-256 drift: " + row["path"])
        if not SHA1.fullmatch(row["source_blob_before"]):
            raise ValueError("missing old Git blob identity: " + row["path"])


def verify(root: Path, receipt: dict) -> tuple[int, int]:
    if receipt.get("schema") != "M3JDK21_SOURCE_SEALED_RECEIVER_V1":
        raise ValueError("unsupported collection receiving contract")
    if not receipt.get("synexia", {}).get("recipe_not_copied_to_product"):
        raise ValueError("executable Synexia recipe must not ship to M3JDK21")
    originals = receipt.get("source_files", [])
    if not isinstance(originals, list) or len(originals) != 3:
        raise ValueError("expected exactly three admitted source postimages")
    names = [row["path"] for row in originals]
    if names != sorted(set(names)):
        raise ValueError("duplicated or unsorted source mapping identities")
    for row in originals:
        verify_blob(root, row, "source_blob_after")
    native = receipt.get("native_sources_unchanged", [])
    if len(native) != 2:
        raise ValueError("existing JNI owner list must have two source seals")
    for row in native:
        verify_blob(root, row, "git_blob_sha1")
    catalogue = receipt["catalogue"]
    file = exact_file(root, catalogue["path"])
    data = file.read_bytes()
    if blob_sha1(data) != catalogue["git_blob_sha1"]:
        raise ValueError("donor catalogue Git blob drift")
    raw = data.decode("utf-8")
    reader = csv.DictReader(io.StringIO(raw, newline=""), delimiter="\t")
    if "problem_category" not in (reader.fieldnames or []) or "selection" not in (reader.fieldnames or []):
        raise ValueError("unexpected donor catalogue header")
    rows = list(reader)
    categories = [row["problem_category"] for row in rows]
    if len(rows) != catalogue["categories"] or len(set(categories)) != len(categories):
        raise ValueError("donor catalogue categories drift")
    if not any(row["selection"].startswith("SELECT_") for row in rows):
        raise ValueError("no admitted algorithm selection")
    if not any(row["selection"].startswith("REJECT_") for row in rows):
        raise ValueError("no contract-based candidate refusals")
    return len(originals), len(rows)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--receipt", type=Path, default=RECEIPT)
    args = parser.parse_args(argv)
    try:
        receipt = json.loads(args.receipt.read_text(encoding="utf-8"),
                             object_pairs_hook=no_duplicate_keys)
        sources, categories = verify(args.root, receipt)
    except (ValueError, KeyError, OSError, UnicodeError, json.JSONDecodeError) as error:
        print("M3JDK21_SOURCE_RECEIVER_REFUSED: " + str(error), file=sys.stderr)
        return 2
    print("M3JDK21_SOURCE_RECEIVER_PASS postimages=" + str(sources)
          + " catalogue_categories=" + str(categories)
          + " existing_jni_unchanged=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
