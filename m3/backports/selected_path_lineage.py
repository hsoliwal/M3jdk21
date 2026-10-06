#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Deterministic selected-path Git lineage audit for M3JDK21 backport packets.

This tool is evidence-only. It does not apply commits, infer semantic equivalence, mutate source,
or grant promotion authority.
"""

from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
import hashlib
from pathlib import Path
import subprocess
import sys


@dataclass(frozen=True)
class Row:
    ordinal: int
    commit: str
    authored: str
    subject: str
    allowed: bool
    paths: tuple[str, ...]


def canonical_relative(value: str) -> str:
    checked = (value or "").strip().replace("\\", "/")
    if (
        not checked
        or checked.startswith("/")
        or ":" in checked
        or "\0" in checked
        or "\\" in checked
    ):
        raise ValueError(f"noncanonical relative path: {value!r}")
    parts = checked.split("/")
    if any(part in ("", ".", "..", ".git") for part in parts):
        raise ValueError(f"noncanonical relative path: {value!r}")
    return checked


def load_paths(path: Path) -> list[str]:
    values = [
        canonical_relative(line)
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if not values:
        raise ValueError("selected path list is empty")
    if len(values) != len(set(values)):
        raise ValueError("duplicate selected path")
    return sorted(values)


def _git(repo: Path, *args: str) -> str:
    return subprocess.check_output(
        ["git", "-C", str(repo), *args],
        text=True,
        stderr=subprocess.STDOUT,
    )


def require_commit(repo: Path, value: str, field: str) -> str:
    if not value or any(ch.isspace() for ch in value):
        raise ValueError(field)
    resolved = _git(repo, "rev-parse", "--verify", f"{value}^{{commit}}").strip()
    if not resolved or len(resolved) != 40:
        raise ValueError(field)
    return resolved


def audit(
    repo: Path,
    start_exclusive: str,
    end_inclusive: str,
    paths: list[str],
    allowed: set[str],
) -> list[Row]:
    root = repo.resolve()
    start = require_commit(root, start_exclusive, "start")
    end = require_commit(root, end_inclusive, "end")

    ancestor = subprocess.run(
        ["git", "-C", str(root), "merge-base", "--is-ancestor", start, end],
        check=False,
    )
    if ancestor.returncode != 0:
        raise ValueError("start is not an ancestor of end")

    allowed_resolved = {require_commit(root, value, "allowed commit") for value in allowed}

    command = [
        "log",
        "--reverse",
        "--date=iso-strict",
        "--format=@@%H%x09%aI%x09%s",
        "--name-only",
        f"{start}..{end}",
        "--",
        *paths,
    ]
    output = _git(root, *command)

    parsed: list[tuple[str, str, str, list[str]]] = []
    current: tuple[str, str, str, list[str]] | None = None
    for raw in output.splitlines():
        if raw.startswith("@@"):
            if current is not None:
                parsed.append(current)
            fields = raw[2:].split("\t", 2)
            if len(fields) != 3:
                raise ValueError("invalid git log header")
            current = (fields[0], fields[1], fields[2].replace("\t", " "), [])
        elif raw.strip():
            if current is None:
                raise ValueError("git log path before commit header")
            path = canonical_relative(raw.strip())
            current[3].append(path)
    if current is not None:
        parsed.append(current)

    rows: list[Row] = []
    observed_commits: set[str] = set()
    selected = set(paths)
    for ordinal, (commit, authored, subject, touched) in enumerate(parsed):
        stable_paths = tuple(sorted(set(touched) & selected))
        if not stable_paths:
            continue
        observed_commits.add(commit)
        rows.append(
            Row(
                ordinal=len(rows),
                commit=commit,
                authored=authored,
                subject=subject,
                allowed=commit in allowed_resolved,
                paths=stable_paths,
            )
        )

    missing_allowed = allowed_resolved - observed_commits
    if missing_allowed:
        raise ValueError(
            "allowed commit(s) do not touch selected paths in range: "
            + ",".join(sorted(missing_allowed))
        )
    return rows


def semantic_root(rows: list[Row]) -> str:
    digest = hashlib.sha256()
    for value in ("M3_SELECTED_PATH_LINEAGE_V1", str(len(rows))):
        _frame(digest, value)
    for row in rows:
        for value in (
            str(row.ordinal),
            row.commit,
            row.authored,
            row.subject,
            str(row.allowed).lower(),
            *row.paths,
        ):
            _frame(digest, value)
    return digest.hexdigest()


def _frame(digest: "hashlib._Hash", value: str) -> None:
    data = value.encode("utf-8")
    digest.update(len(data).to_bytes(4, "big"))
    digest.update(data)


def write(rows: list[Row], out: Path, root_out: Path | None) -> str:
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            ("ordinal", "commit", "authored", "subject", "allowed", "path_count", "paths")
        )
        for row in rows:
            writer.writerow(
                (
                    row.ordinal,
                    row.commit,
                    row.authored,
                    row.subject,
                    str(row.allowed).lower(),
                    len(row.paths),
                    ",".join(row.paths),
                )
            )
    root = semantic_root(rows)
    if root_out is not None:
        root_out.parent.mkdir(parents=True, exist_ok=True)
        root_out.write_text(root + "\n", encoding="utf-8")
    return root


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, default=Path("."))
    parser.add_argument("--start-exclusive", required=True)
    parser.add_argument("--end-inclusive", required=True)
    parser.add_argument("--paths-file", type=Path, required=True)
    parser.add_argument("--allowed-commit", action="append", default=[])
    parser.add_argument("--require-no-unexpected", action="store_true")
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--root-out", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        paths = load_paths(args.paths_file)
        rows = audit(
            args.repo,
            args.start_exclusive,
            args.end_inclusive,
            paths,
            set(args.allowed_commit),
        )
        root = write(rows, args.out, args.root_out)
        unexpected = [row for row in rows if not row.allowed]
        if args.require_no_unexpected and unexpected:
            print(
                "selected-path lineage refused: unexpected commits="
                + ",".join(row.commit for row in unexpected),
                file=sys.stderr,
            )
            return 2
    except (OSError, subprocess.CalledProcessError, ValueError) as failure:
        print(f"selected-path lineage refused: {failure}", file=sys.stderr)
        return 2

    print(
        f"M3_SELECTED_PATH_LINEAGE_PASS commits={len(rows)} "
        f"unexpected={len([row for row in rows if not row.allowed])} root={root}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
