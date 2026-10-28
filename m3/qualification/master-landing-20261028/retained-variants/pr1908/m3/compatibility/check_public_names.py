#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail closed on newly introduced noncanonical public JDK names and artifacts.

Existing product source is grandfathered, not relabeled or automatically renamed.
Canonical migration ownership remains m3/docs/name-mapping.json; Synexia keeps
recipe names, candidate inventories, and chronological evidence.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys

SHA = re.compile(r"[0-9a-f]{40}\Z")
DATE = re.compile(r"(?:^|[-_.])20\d{6}(?:$|[-_.])")
VERSION = re.compile(r"(?:[-_.]v\d+|V\d+)(?=\.|$)", re.IGNORECASE)
TYPE = re.compile(r"\b(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)\b")
PACKAGE = re.compile(r"(?m)^\s*package\s+([a-zA-Z_$][\w$]*(?:\.[a-zA-Z_$][\w$]*)*)\s*;")
PUBLIC = re.compile(r"\bpublic\s+(?:(?:abstract|final|sealed|non-sealed)\s+)*"
                    r"(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)\b")
JDK_SOURCE = re.compile(r"^src/[^/]+/(?:share|unix|linux|windows|macosx|bsd)/classes/(java|javax)/")
RECIPE_ROOTS = (
    "synexia-openrewrite-recipes/",
    "synexia-m3-recipe/",
    "m3/tooling/migration-recipes/",
)
RECIPE_DESCRIPTOR = re.compile(r"(?:^|/)META-INF/rewrite/[^/]+\.ya?ml$")
RECIPE_JAVA = re.compile(r"(?:^|/)(?:rewrite|recipes?)/[^/]*Recipe(?:Test)?\.java$")


# Narrow, reviewable exceptions for original immutable DATA evidence only.
# This list never authorizes a new runtime, workflow, public Java type or recipe.
# Updates require a fresh Synexia source-sealed correction and negative tests.
QUALIFICATION_RECEIPT_ROOT = "m3/qualification/collection-convergence-20261024/"
QUALIFICATION_RECEIPTS = frozenset({
    "ADJACENT_IMAGE_RECEIPT.json",
    "BATCH_ADMISSION_RECEIPT.json",
    "COW_SINGLE_WORD_RECEIPT.json",
    "DENSE_ORACLE_RECEIPT.json",
    "INDEXED_SORT_QUALIFICATION_RECEIPT.json",
    "INDEXED_VIEW_LAZY_DIRECTORY_RECEIPT.json",
    "MASK_WORD_SURVIVORS_RECEIPT.json",
    "MASTER_RECONCILIATION_RECEIPT.json",
    "NATIVE_COUNT_BATCHING_RECEIPT.json",
    "NATIVE_IMAGE_RECEIPT.json",
    "PREDICATE_RUN_RECEIPT.json",
    "RECEIPT.json",
    "REGEX_COLLECTION_SUPERSET_RECEIPT.json",
    "REGEX_COMPACT_METADATA_RECEIPT.json",
    "STRING_SOURCE_CONCAT_RECEIPT.json",
    "UNICODE_FRONTIER_RECEIPT.json",
    "VERIFICATION_RECEIPT.json",
})
REVIEWED_FULL_BORROW_OBSERVATION = (
    "m3/synexia-import/current-full-borrow/string-source-observation-v3.tsv"
)


def is_reviewed_historical_data_path(path: str) -> bool:
    if path == REVIEWED_FULL_BORROW_OBSERVATION:
        return True
    if not path.startswith(QUALIFICATION_RECEIPT_ROOT):
        return False
    return path[len(QUALIFICATION_RECEIPT_ROOT):] in QUALIFICATION_RECEIPTS





@dataclass(frozen=True)
class Change:
    status: str
    path: str
    previous: str | None = None


def git(root: Path, *args: str) -> bytes:
    result = subprocess.run(["git", *args], cwd=root, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, check=False)
    if result.returncode:
        raise ValueError("git request refused: " + result.stderr.decode("utf-8", "replace").strip())
    return result.stdout


def changes(root: Path, base: str, head: str) -> list[Change]:
    if not SHA.fullmatch(base) or not SHA.fullmatch(head):
        raise ValueError("base and head require exact 40-character commit IDs")
    parts = git(root, "diff", "--no-ext-diff", "--name-status", "-z",
                "--find-renames", "--diff-filter=ACMRT", base + "..." + head).split(b"\0")
    if parts[-1] != b"":
        raise ValueError("incomplete NUL-delimited git diff")
    rows = []
    i = 0
    while i < len(parts) - 1:
        status = parts[i].decode("ascii")
        i += 1
        if not re.fullmatch(r"[ACMRT](?:\d{1,3})?", status):
            raise ValueError("unexpected diff operation")
        previous = None
        if status.startswith(("R", "C")):
            previous = parts[i].decode("utf-8")
            i += 1
        name = parts[i].decode("utf-8")
        i += 1
        path = PurePosixPath(name)
        if path.is_absolute() or ".." in path.parts or "\\" in name or not name:
            raise ValueError("unsafe changed path")
        rows.append(Change(status, name, previous))
    return rows


def problems(change: Change, text: str | None = None) -> list[str]:
    path = change.path
    added = change.status.startswith(("A", "C", "R"))
    # Legacy public-repository recipe sources are custody, not a second
    # ongoing convergence workspace. Refuse their modification as well.
    if path.startswith(RECIPE_ROOTS):
        return ["Maven/OpenRewrite recipe sources belong exclusively in Synexia"]
    if not added:
        return []  # existing product APIs are separately guarded
    found = []
    if RECIPE_DESCRIPTOR.search(path) or RECIPE_JAVA.search(path):
        found.append("new transformation recipe source/descriptor belongs in Synexia")
    parts = PurePosixPath(path).parts
    name = parts[-1]
    if (any(DATE.search(seg) or VERSION.search(seg) for seg in parts)
            and not is_reviewed_historical_data_path(path)):
        found.append("unstable date/version identifier")
    if path.startswith("m3/docs/name-mapping.d/"):
        found.append("new parallel mapping fragment; update canonical name-mapping.json instead")
    matched_jdk = JDK_SOURCE.match(path)
    if matched_jdk is not None and name.endswith(".java"):
        if name.startswith("M3"):
            found.append("new public java.* M3 owner; implement existing JDK API in place")
        if text is not None:
            declared = PACKAGE.search(text)
            expected = ".".join(path[matched_jdk.start(1):].split("/")[:-1])
            if declared is None or declared.group(1) != expected:
                found.append("declared package differs from Java module path")
            direct_type = name[:-5]
            if direct_type not in ("module-info", "package-info") and direct_type not in TYPE.findall(text):
                found.append("Java filename does not match a declared type")
            if any(t.startswith("M3") for t in PUBLIC.findall(text)):
                found.append("new M3-prefixed public JDK type")
    if path.startswith("test/jdk/") and name.endswith(".java") and text is not None:
        declared = name[:-5]
        if declared not in TYPE.findall(text):
            found.append("jtreg file and type name mismatch")
    return found


def check(root: Path, base: str, head: str) -> tuple[list[Change], list[tuple[str, str]]]:
    delta = changes(root, base, head)
    violations = []
    for change in delta:
        source = None
        if change.status.startswith(("A", "C", "R")) and change.path.endswith(".java"):
            source = git(root, "show", head + ":" + change.path).decode("utf-8")
        for reason in problems(change, source):
            violations.append((change.path, reason))
    return delta, sorted(violations)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    parser.add_argument("--repo", default=".", type=Path)
    args = parser.parse_args(argv)
    try:
        delta, failures = check(args.repo, args.base, args.head)
    except (ValueError, UnicodeError, OSError) as exc:
        print("PUBLIC_NAMES_REFUSED: " + str(exc), file=sys.stderr)
        return 2
    for path, reason in failures:
        print(path + "\t" + reason)
    print(f"PUBLIC_NAMES files={len(delta)} violations={len(failures)}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
