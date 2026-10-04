#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Compare the pinned JDK 21 GA tree against immutable released/snapshot donor trees.

Git blob identity is the first oracle: equal blob OIDs mean equal file bytes. Released JDK
22..26 donors are exact GA tags. JDK 27 is an explicitly pinned in-development snapshot commit
from RELEASE_DONOR_REFS.tsv; it is never represented as a GA tag by this tool.
"""

from __future__ import annotations

import argparse
import csv
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

import release_donor_refs

BASELINE = (21, "jdk-21+35")


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
    recipe_lane: str


def default_donors() -> dict[int, str]:
    return release_donor_refs.read(Path(__file__).with_name("RELEASE_DONOR_REFS.tsv"))


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


def _lane(path: str, status: str) -> str:
    if status == "SAME":
        return "NO_RECIPE"
    if path.endswith(".java"):
        if status == "ADDED":
            return "OPENREWRITE_ADD_JAVA"
        if status == "REMOVED":
            return "REVIEW_REMOVAL"
        return "OPENREWRITE_JAVA_PAIR"
    if status == "ADDED":
        return "VERBATIM_ADD_RESOURCE"
    if status == "REMOVED":
        return "REVIEW_REMOVAL"
    return "VERBATIM_RESOURCE_PAIR"


def compare(
    repo: Path,
    donor_releases: Iterable[int] = range(22, 28),
    donor_refs: Mapping[int, str] | None = None,
) -> list[FileDelta]:
    refs = dict(default_donors() if donor_refs is None else donor_refs)
    selected = frozenset(donor_releases)
    unknown = selected - set(refs)
    if unknown:
        raise ValueError(f"unsupported donor release(s): {sorted(unknown)}")

    baseline_release, baseline_ref = BASELINE
    assert baseline_release == 21
    baseline = _tree(repo, baseline_ref)
    rows: list[FileDelta] = []

    for release in sorted(selected):
        donor_ref = refs[release]
        donor = _tree(repo, donor_ref)
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
                    recipe_lane=_lane(path, status),
                )
            )
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
        help="donor release; repeatable; default is authority-defined releases 22..27",
    )
    parser.add_argument(
        "--donor-refs",
        type=Path,
        default=Path(__file__).with_name("RELEASE_DONOR_REFS.tsv"),
        help="immutable release/snapshot donor-ref authority",
    )
    parser.add_argument("--out", type=Path)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    repo = args.repo.resolve()
    donors = release_donor_refs.read(args.donor_refs.resolve())
    releases = tuple(args.release or sorted(donors))
    selected_refs = [donors[release] for release in releases]
    _verify(repo, (BASELINE[1], *selected_refs))
    rows = compare(repo, releases, donors)
    if args.out is None:
        write_tsv(rows, sys.stdout)
    else:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        with args.out.open("w", encoding="utf-8", newline="") as handle:
            write_tsv(rows, handle)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
