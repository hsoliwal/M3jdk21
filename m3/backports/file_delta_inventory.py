#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Compare every released donor tree verbatim against the pinned JDK 21 GA tree.

Git blob identity is the first oracle: equal blob OIDs mean equal file bytes. The output
contains the full union of paths so SAME files are explicit evidence, not silently skipped.
Changed Java files are marked for OpenRewrite semantic analysis/recipe generation. Native/JNI
files are classified separately so A3 can emit source-sealed FILE atoms without pretending
PlainText is a C/C++ semantic parser.
"""

from __future__ import annotations

import argparse
import csv
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

BASELINE = (21, "jdk-21+35")
DONORS: tuple[tuple[int, str], ...] = (
    (22, "jdk-22+36"),
    (23, "jdk-23+37"),
    (24, "jdk-24+36"),
    (25, "jdk-25+36"),
    (26, "jdk-26+35"),
    (27, "jdk-27+35"),
)


@dataclass(frozen=True)
class TreeEntry:
    mode: str
    object_type: str
    oid: str


@dataclass(frozen=True)
class FileDelta:
    release: int
    baseline_ref: str
    donor_ref: str
    path: str
    status: str
    baseline_mode: str
    donor_mode: str
    baseline_oid: str
    donor_oid: str
    java_source: bool
    native_source: bool
    recipe_lane: str


def _git(repo: Path, *args: str, text: bool = True):
    proc = subprocess.run(
        ("git", "-C", str(repo), *args),
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=text,
        encoding="utf-8" if text else None,
    )
    if proc.returncode:
        stderr = proc.stderr if text else proc.stderr.decode("utf-8", "replace")
        raise RuntimeError(
            f"git {' '.join(args)} failed with {proc.returncode}: {stderr.strip()}"
        )
    return proc.stdout


def _verify(repo: Path, refs: Iterable[str]) -> None:
    if not repo.is_dir():
        raise ValueError(f"not a directory: {repo}")
    if _git(repo, "rev-parse", "--is-inside-work-tree").strip() != "true":
        raise ValueError(f"not a Git work tree: {repo}")
    for ref in refs:
        _git(repo, "rev-parse", "--verify", f"{ref}^{{commit}}")


def _tree(repo: Path, ref: str) -> dict[str, TreeEntry]:
    raw = _git(repo, "ls-tree", "-r", "-z", ref, text=False)
    result: dict[str, TreeEntry] = {}
    for record in raw.split(b"\0"):
        if not record:
            continue
        meta, raw_path = record.split(b"\t", 1)
        mode, object_type, oid = meta.decode("ascii").split(" ", 2)
        path = raw_path.decode("utf-8", "surrogateescape")
        result[path] = TreeEntry(mode, object_type, oid)
    return result


def _status(base: TreeEntry | None, donor: TreeEntry | None) -> str:
    if base is None:
        return "ADDED"
    if donor is None:
        return "REMOVED"
    if base == donor:
        return "SAME"
    return "MODIFIED"


def _native_source(path: str) -> bool:
    name = path.lower()
    return name.endswith(
        (
            ".c",
            ".cc",
            ".cpp",
            ".cxx",
            ".h",
            ".hh",
            ".hpp",
            ".s",
            ".asm",
        )
    )


def _lane(path: str, status: str) -> str:
    if status == "SAME":
        return "NO_RECIPE"
    if path.endswith(".java"):
        if status == "ADDED":
            return "OPENREWRITE_ADD_JAVA"
        if status == "REMOVED":
            return "REVIEW_REMOVAL"
        return "OPENREWRITE_JAVA_PAIR"
    if _native_source(path):
        if status == "ADDED":
            return "SOURCE_SEALED_ADD_NATIVE"
        if status == "REMOVED":
            return "REVIEW_REMOVAL"
        return "SOURCE_SEALED_NATIVE_PAIR"
    if status == "ADDED":
        return "VERBATIM_ADD_RESOURCE"
    if status == "REMOVED":
        return "REVIEW_REMOVAL"
    return "VERBATIM_RESOURCE_PAIR"


def compare_refs(
    repo: Path,
    release: int,
    baseline_ref: str,
    donor_ref: str,
) -> list[FileDelta]:
    """Compare two exact refs without pretending the donor is an entire GA release."""
    if release not in range(22, 28):
        raise ValueError(f"unsupported donor release: {release}")
    if not baseline_ref or not donor_ref:
        raise ValueError("baseline_ref and donor_ref are required")
    _verify(repo, (baseline_ref, donor_ref))
    baseline = _tree(repo, baseline_ref)
    donor = _tree(repo, donor_ref)
    rows: list[FileDelta] = []
    for path in sorted(set(baseline) | set(donor)):
        before = baseline.get(path)
        after = donor.get(path)
        status = _status(before, after)
        rows.append(
            FileDelta(
                release=release,
                baseline_ref=baseline_ref,
                donor_ref=donor_ref,
                path=path,
                status=status,
                baseline_mode="" if before is None else before.mode,
                donor_mode="" if after is None else after.mode,
                baseline_oid="" if before is None else before.oid,
                donor_oid="" if after is None else after.oid,
                java_source=path.endswith(".java"),
                native_source=_native_source(path),
                recipe_lane=_lane(path, status),
            )
        )
    return rows


def compare(
    repo: Path,
    donor_releases: Iterable[int] = range(22, 28),
) -> list[FileDelta]:
    selected = frozenset(donor_releases)
    known = {release for release, _ref in DONORS}
    unknown = selected - known
    if unknown:
        raise ValueError(f"unsupported donor release(s): {sorted(unknown)}")

    baseline_release, baseline_ref = BASELINE
    assert baseline_release == 21
    rows: list[FileDelta] = []
    for release, donor_ref in DONORS:
        if release in selected:
            rows.extend(compare_refs(repo, release, baseline_ref, donor_ref))
    return rows


def write_tsv(rows: Sequence[FileDelta], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow(
        (
            "release",
            "baseline_ref",
            "donor_ref",
            "path",
            "status",
            "baseline_mode",
            "donor_mode",
            "baseline_oid",
            "donor_oid",
            "java_source",
            "recipe_lane",
            "native_source",
        )
    )
    for row in rows:
        writer.writerow(
            (
                row.release,
                row.baseline_ref,
                row.donor_ref,
                row.path,
                row.status,
                row.baseline_mode,
                row.donor_mode,
                row.baseline_oid,
                row.donor_oid,
                str(row.java_source).lower(),
                row.recipe_lane,
                str(row.native_source).lower(),
            )
        )


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument(
        "--release",
        type=int,
        action="append",
        choices=range(22, 28),
        help="donor release; repeatable; default is all released donors 22..27",
    )
    parser.add_argument("--out", type=Path)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    repo = args.repo.resolve()
    releases = tuple(args.release or range(22, 28))
    selected_refs = tuple(ref for release, ref in DONORS if release in releases)
    _verify(repo, (BASELINE[1], *selected_refs))
    rows = compare(repo, releases)
    if args.out is None:
        write_tsv(rows, sys.stdout)
    else:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        with args.out.open("w", encoding="utf-8", newline="") as handle:
            write_tsv(rows, handle)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
