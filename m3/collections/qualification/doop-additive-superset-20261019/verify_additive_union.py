#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Proof-only source/ancestry gate for the M3JDK21 true Git merge union.

No transformation engine, source replacement, or PR state mutation.
Never weaken the supplied source pins when an original owner changes.
"""
from __future__ import annotations

import argparse
import csv
import re
import subprocess
from pathlib import Path


PRODUCT = Path("m3/collections/qualification/doop-additive-superset-20261019")
PINS = PRODUCT / "SOURCE_PINS.tsv"
ANCESTRY = PRODUCT / "MERGE_PARENTS.tsv"
EXPECTED_HEADS = {
    "1426": "22e7a14ccd2998d994cb9a74abcdc7abd2086201",
    "1428": "c4bd4b882dc93135d7a376dafce8fd70d9ce4689",
    "1434": "239989a18f2366a1f65d91a4d8e3974b755a7f0e",
    "1431": "4f33ce9281c8df8566c0a6731d9a06570bbec6db",
    "1429": "5c9b930228f7b3a9b8548eb9ba44cadd4d0312c7",
}
ALLOWED_CATEGORIES = {
    "DOOP_FILTERED_MAP", "DOOP_MUTABLE_MAP_SET", "AVL_SLOT_MAP",
    "AVL_SLOT_SET", "FILTERED_DEQUE_THROWING", "LINKED_SPARSE_CLEAR",
    "LINKED_MAP_SPARSE_CLEAR", "JAVA_BASE_ORDINAL_MAP",
    "JAVA_BASE_IDENTITY_MAP", "JNI_BOUNDED_MASK", "JDK_SET_DIFFERENTIAL",
    "DEQUE_THROW_DIFFERENTIAL", "LINKED_CLEAR_DIFFERENTIAL",
    "LICENSED_DONOR_CATALOGUE",
}
HEX = re.compile(r"[0-9a-f]{40}\Z")


def run(*args: str, cwd: Path) -> str:
    result = subprocess.run(
        list(args), cwd=cwd, check=True, text=True, capture_output=True
    )
    return result.stdout.strip()


def read_tsv(path: Path, columns: list[str]) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if reader.fieldnames != columns:
            raise ValueError("schema drift: " + str(path))
        rows = list(reader)
    if any(None in row or any(value is None for value in row.values()) for row in rows):
        raise ValueError("truncated or surplus columns: " + str(path))
    return rows


def validate(
    root: Path, entries: list[dict[str, str]], ancestors: list[dict[str, str]]
) -> None:
    if len(entries) != 14 or len(ancestors) != 5:
        raise ValueError("incomplete M3JDK21 collection donor universe")
    seen: set[str] = set()
    for item in entries:
        name = item["path"]
        if name in seen or not name or name.startswith("/") or "\\" in name:
            raise ValueError("duplicate/invalid product source path " + name)
        seen.add(name)
        parts = Path(name).parts
        if ".." in parts or "." in parts:
            raise ValueError("source escapes M3JDK21 workspace " + name)
        if item["category"] not in ALLOWED_CATEGORIES:
            raise ValueError("unknown algorithm category " + item["category"])
        if item["source_license"] not in ("Apache-2.0", "GPLv2+Classpath"):
            raise ValueError("original owner rights changed")
        if item["product_pr"] not in EXPECTED_HEADS:
            raise ValueError("unexpected collection source donor")
        digest = item["git_blob_sha1"]
        if not HEX.fullmatch(digest):
            raise ValueError("invalid exact source Git blob")
        source = root / name
        if not source.is_file() or source.is_symlink():
            raise ValueError("missing/nonregular source " + name)
        actual = run("git", "hash-object", "--", name, cwd=root)
        if actual != digest:
            raise ValueError(
                f"M3JDK21 source drift: {name}: expected {digest}, actual {actual}"
            )
    receipts: set[str] = set()
    for item in ancestors:
        pr = item["pr_number"]
        if pr not in EXPECTED_HEADS or pr in receipts:
            raise ValueError("missing, duplicated or unknown donor PR ancestor " + pr)
        receipts.add(pr)
        sha = item["commit_sha"]
        if sha != EXPECTED_HEADS[pr]:
            raise ValueError("source-preserving donor head pin drift " + pr)
        if not item["canonical_synexia_source"] or not item["contract"]:
            raise ValueError("attribution/contract metadata erased")
        process = subprocess.run(
            ["git", "merge-base", "--is-ancestor", sha, "HEAD"],
            cwd=root, check=False, text=True, capture_output=True
        )
        if process.returncode != 0:
            raise ValueError("donor commit not preserved as Git ancestor: " + pr)
    if receipts != set(EXPECTED_HEADS):
        raise ValueError("donor ancestry incomplete")


def refused(root: Path, rows: list[dict[str, str]],
            ancestors: list[dict[str, str]], target: str) -> None:
    try:
        validate(root, rows, ancestors)
    except ValueError as exc:
        if target not in str(exc):
            raise AssertionError(
                f"wrong refusal, expected {target}: {exc}"
            ) from exc
        return
    raise AssertionError("unqualified source admitted: " + target)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    entries = read_tsv(
        root / PINS,
        ["path", "git_blob_sha1", "category", "product_pr", "source_license"],
    )
    ancestors = read_tsv(
        root / ANCESTRY,
        ["pr_number", "merge_role", "commit_sha", "canonical_synexia_source",
         "contract"],
    )
    validate(root, entries, ancestors)
    if args.self_test:
        changed = [dict(row) for row in entries]
        changed[0]["git_blob_sha1"] = "0" * 40
        refused(root, changed, ancestors, "source drift")
        changed = [dict(row) for row in entries[:-1]]
        refused(root, changed, ancestors, "incomplete")
        changed = [dict(row) for row in entries]
        changed[-1]["path"] = changed[0]["path"]
        refused(root, changed, ancestors, "duplicate")
        changed = [dict(row) for row in entries]
        changed[0]["category"] = "COPIED_EDITORIAL"
        refused(root, changed, ancestors, "unknown algorithm category")
        changed = [dict(row) for row in ancestors]
        changed[0]["commit_sha"] = "0" * 40
        refused(root, entries, changed, "head pin drift")
        changed = [dict(row) for row in ancestors[:-1]]
        refused(root, entries, changed, "incomplete")
    print(
        f"M3_COLLECTIONS_SUPERSET_SOURCE_PASS files={len(entries)} "
        f"parentPRs={len(ancestors)} refusals={6 if args.self_test else 0}"
    )


if __name__ == "__main__":
    main()
