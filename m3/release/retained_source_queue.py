#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only candidate queue for original source paths preserved by PR landings.

This is a repository-tree census, not a semantic loss detector or a merge recipe.
Only the pinned HEAD tree is examined; working-tree files are never consulted.
"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile

ARCHIVE_ROOT = "m3/qualification/master-landing-20261028/retained-variants/"
SOURCE_SUFFIXES = (".java", ".c", ".h", ".cc", ".cpp", ".hpp", ".S", ".s")
OID = re.compile(r"[0-9a-f]{40}\Z")
PR_PREFIX = re.compile(r"pr([1-9][0-9]*)/(.+)\Z")
HEADER = ("priority\tpr\toriginal_path\tarchive_path\tarchive_mode\tarchive_oid"
          "\tactive_mode\tactive_oid\tpath_state\tnext_gate\n")
MAX_TREE_BYTES = 128 * 1024 * 1024
MAX_SOURCE_ROWS = 100_000


def git(root: Path, *args: str) -> bytes:
    return subprocess.check_output(["git", "-C", str(root), *args], stderr=subprocess.PIPE)


def tree(root: Path, *paths: str) -> dict[str, tuple[str, str]]:
    command = ["ls-tree", "-r", "-z", "HEAD"]
    if paths:
        command += ["--", *paths]
    data = git(root, *command)
    if len(data) > MAX_TREE_BYTES:
        raise ValueError("TREE_CENSUS_EXCEEDS_BUDGET")
    entries: dict[str, tuple[str, str]] = {}
    for item in data.split(b"\0"):
        if not item:
            continue
        try:
            metadata, raw_path = item.split(b"\t", 1)
            mode, kind, oid = metadata.decode("ascii").split(" ")
            path = raw_path.decode("utf-8", "strict")
        except (UnicodeError, ValueError) as error:
            raise ValueError("TREE_ENTRY_CANNOT_BE_VERIFIED") from error
        if kind != "blob" or not OID.fullmatch(oid) or not re.fullmatch(r"[0-7]{6}", mode):
            raise ValueError("TREE_ENTRY_UNEXPECTED_OBJECT")
        if path in entries:
            raise ValueError("TREE_ENTRY_DUPLICATE_PATH")
        entries[path] = (mode, oid)
    return entries


def valid_path(path: str) -> bool:
    return (bool(path) and "\\" not in path and "\r" not in path and "\n" not in path
            and "\t" not in path and "\0" not in path
            and all(p and p not in (".", "..", ".git") for p in path.split("/")))


def scan(root: Path) -> tuple[str, str, dict[str, object]]:
    root = root.resolve()
    top = Path(git(root, "rev-parse", "--show-toplevel").decode("utf-8").strip()).resolve()
    if root != top:
        raise ValueError("ROOT_MUST_BE_GIT_TOPLEVEL")
    head = git(root, "rev-parse", "HEAD").decode("ascii").strip()
    if not OID.fullmatch(head):
        raise ValueError("HEAD_NOT_SHA1_COMMIT")
    archived = tree(root, ARCHIVE_ROOT)
    if not archived:
        raise ValueError("RETAINED_VARIANTS_TREE_MISSING")
    # One all-path index, not a subprocess for every donor source file.
    active = tree(root)
    lines: list[str] = []
    seen: set[tuple[int, str]] = set()
    counts: Counter[str] = Counter()
    skipped = 0
    for archived_path, (archive_mode, archive_oid) in sorted(archived.items()):
        if not archived_path.startswith(ARCHIVE_ROOT):
            raise ValueError("ARCHIVE_PATH_OUTSIDE_SCOPE")
        match = PR_PREFIX.fullmatch(archived_path[len(ARCHIVE_ROOT):])
        if match is None:
            raise ValueError("ARCHIVE_PATH_MISSING_PR_ORIGINAL_PATH")
        pr, original_path = int(match[1]), match[2]
        if not valid_path(original_path):
            raise ValueError("ARCHIVE_UNSAFE_ORIGINAL_PATH")
        if not original_path.endswith(SOURCE_SUFFIXES):
            skipped += 1
            continue
        if len(lines) >= MAX_SOURCE_ROWS:
            raise ValueError("ARCHIVED_SOURCE_ROW_BUDGET")
        if (pr, original_path) in seen:
            raise ValueError("DUPLICATE_ARCHIVED_SOURCE_IDENTITY")
        seen.add((pr, original_path))
        present = active.get(original_path)
        priority = "P0" if original_path.startswith("src/java.base/") else "P1"
        if archive_mode not in ("100644", "100755"):
            state, next_gate = "HOLD_ARCHIVE_MODE", "review-source-type-and-mode"
        elif present is None:
            state, next_gate = "REVIEW_ABSENT_ACTIVE_PATH", "resolve-rename-or-intentional-exclusion"
        elif present[0] != archive_mode:
            state, next_gate = "REVIEW_MODE_DIFFERENCE", "review-executable-bit-and-symlink"
        elif present[1] == archive_oid:
            state, next_gate = "EXACT_ACTIVE_PATH", "none-source-byte-equality-only"
        else:
            state, next_gate = "REVIEW_DIVERGENT_ACTIVE_PATH", "compare-evolved-abi-behavior-and-history"
        counts[state] += 1
        current_mode, current_oid = present if present is not None else ("-", "-")
        lines.append(f"{priority}\t{pr}\t{original_path}\t{archived_path}\t"
                     f"{archive_mode}\t{archive_oid}\t{current_mode}\t{current_oid}\t"
                     f"{state}\t{next_gate}\n")
    rows = HEADER + "".join(lines)
    summary: dict[str, object] = {
        "schema": "M3_RETAINED_SOURCE_QUEUE_V1",
        "tree_head": head,
        "scope": ARCHIVE_ROOT,
        "source_rows": len(lines),
        "non_source_rows_skipped": skipped,
        "states": dict(sorted(counts.items())),
        "unresolved_rows": sum(n for status, n in counts.items() if status != "EXACT_ACTIVE_PATH"),
        "tsv_sha256": hashlib.sha256(rows.encode("utf-8")).hexdigest(),
        "merge_authorized": False,
        "rename_authorized": False,
        "runtime_qualified": False,
    }
    return rows, json.dumps(summary, sort_keys=True, indent=2) + "\n", summary


def atomic_write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", newline="\n",
                                 dir=path.parent, prefix=".m3-queue-", delete=False) as tmp:
        tmp.write(content)
        tmp_path = Path(tmp.name)
    tmp_path.replace(path)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--tsv-out", type=Path, required=True)
    parser.add_argument("--summary-out", type=Path, required=True)
    parser.add_argument("--strict", action="store_true", help="block if any source path needs review")
    args = parser.parse_args(argv)
    try:
        if args.tsv_out.resolve() == args.summary_out.resolve():
            raise ValueError("QUEUE_OUTPUTS_MUST_BE_DISTINCT")
        tsv, summary_json, summary = scan(args.root)
        atomic_write(args.tsv_out, tsv)
        atomic_write(args.summary_out, summary_json)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print("M3_RETAINED_QUEUE_REFUSED: " + str(error), file=sys.stderr)
        return 2
    print(f"M3_RETAINED_QUEUE head={summary['tree_head']} rows={summary['source_rows']} "
          f"review={summary['unresolved_rows']} sha256={summary['tsv_sha256']}")
    return 1 if args.strict and summary["unresolved_rows"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
