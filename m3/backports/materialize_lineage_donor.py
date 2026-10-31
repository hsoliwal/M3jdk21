#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Build a synthetic donor tree from a baseline plus only ordered required commit deltas.

This prevents unrelated upstream mainline changes between required feature/fix commits from leaking
into a backport candidate. Each required commit contributes only its parent->commit delta, bounded
to the exact packet path set. Git three-way application is the mechanical conflict oracle.
"""

from __future__ import annotations

import argparse
import csv
import os
import re
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence


@dataclass(frozen=True)
class Lineage:
    order: int
    jbs: str
    commit: str
    parent: str
    date: str
    role: str


@dataclass(frozen=True)
class Receipt:
    order: int
    jbs: str
    source_commit: str
    source_parent: str
    role: str
    patch_paths: int
    synthetic_commit: str
    synthetic_tree: str


_HEX40 = re.compile(r"[0-9a-f]{40}")
_JBS = re.compile(r"[0-9]{4,10}")
_ROLE = re.compile(r"[A-Z][A-Z0-9_]{0,79}")


def git(repo: Path, *args: str, input_bytes: bytes | None = None, text: bool = True):
    process = subprocess.run(
        ("git", "-C", str(repo), *args),
        input=input_bytes,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
        text=False,
    )
    if process.returncode:
        raise RuntimeError(
            f"git {' '.join(args)} failed with {process.returncode}: "
            + process.stderr.decode("utf-8", "replace").strip()
        )
    return (
        process.stdout.decode("utf-8", "surrogateescape")
        if text
        else process.stdout
    )


def canonical_ref(value: str) -> str:
    checked = value.strip()
    if (
        not checked
        or len(checked) > 200
        or checked.startswith("-")
        or re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._/+\-]{0,199}", checked) is None
    ):
        raise ValueError(f"invalid Git ref: {value!r}")
    return checked


def canonical_path(value: str) -> str:
    checked = value.strip()
    if (
        not checked
        or len(checked) > 4096
        or checked.startswith(("/", "\\"))
        or "\\" in checked
        or ":" in checked
        or any(ord(char) < 32 or ord(char) == 127 for char in checked)
    ):
        raise ValueError(f"invalid relative path: {value!r}")
    parts = checked.split("/")
    if any(part in {"", ".", "..", ".git"} for part in parts):
        raise ValueError(f"invalid relative path: {value!r}")
    return checked


def selected_paths(path_file: Path) -> list[str]:
    values = [
        canonical_path(line)
        for line in path_file.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if not values:
        raise ValueError("path file selected zero paths")
    if values != sorted(values):
        raise ValueError("path file must be sorted")
    if len(values) != len(set(values)):
        raise ValueError("path file contains duplicate paths")
    return values


def lineage(path: Path) -> list[Lineage]:
    with path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    required_columns = {
        "order",
        "jbs",
        "commit",
        "parent",
        "date",
        "role",
        "required",
        "promotion_authority",
    }
    if not rows or set(rows[0]) != required_columns:
        raise ValueError("invalid lineage header")

    result: list[Lineage] = []
    for row in rows:
        if row["required"] != "true":
            continue
        if row["promotion_authority"] != "false":
            raise ValueError("lineage row cannot grant promotion authority")
        try:
            order = int(row["order"])
        except ValueError as failure:
            raise ValueError("invalid lineage order") from failure
        if order != len(result):
            raise ValueError("required lineage order must be contiguous from zero")
        if _JBS.fullmatch(row["jbs"]) is None:
            raise ValueError("invalid lineage JBS id")
        if _HEX40.fullmatch(row["commit"]) is None or _HEX40.fullmatch(row["parent"]) is None:
            raise ValueError("invalid lineage commit identity")
        if _ROLE.fullmatch(row["role"]) is None:
            raise ValueError("invalid lineage role")
        if not re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}", row["date"]):
            raise ValueError("invalid lineage date")
        result.append(
            Lineage(
                order,
                row["jbs"],
                row["commit"],
                row["parent"],
                row["date"],
                row["role"],
            )
        )
    if not result:
        raise ValueError("lineage selected zero required commits")
    return result


def verify_source(repo: Path, baseline_ref: str, rows: Sequence[Lineage]) -> None:
    if not repo.is_dir():
        raise ValueError(f"source repository missing: {repo}")
    if git(repo, "rev-parse", "--is-inside-work-tree").strip() != "true":
        raise ValueError(f"not a Git work tree: {repo}")
    git(repo, "rev-parse", "--verify", f"{baseline_ref}^{{commit}}")
    for row in rows:
        git(repo, "rev-parse", "--verify", f"{row.commit}^{{commit}}")
        git(repo, "rev-parse", "--verify", f"{row.parent}^{{commit}}")
        actual_parent = git(repo, "rev-parse", f"{row.commit}^").strip()
        if actual_parent != row.parent:
            raise ValueError(
                f"lineage parent mismatch order={row.order} "
                f"declared={row.parent} actual={actual_parent}"
            )


def changed_paths(repo: Path, parent: str, commit: str) -> set[str]:
    output = git(repo, "diff", "--name-only", "--no-renames", parent, commit, "--")
    return {
        canonical_path(line)
        for line in output.splitlines()
        if line.strip()
    }


def patch(repo: Path, row: Lineage, paths: Sequence[str]) -> tuple[bytes, int]:
    touched = changed_paths(repo, row.parent, row.commit)
    selected = sorted(touched.intersection(paths))
    if not selected:
        return b"", 0
    data = git(
        repo,
        "diff",
        "--binary",
        "--full-index",
        "--no-renames",
        row.parent,
        row.commit,
        "--",
        *selected,
        text=False,
    )
    if not data:
        raise ValueError(f"empty patch for selected lineage paths at order {row.order}")
    return data, len(selected)


def clone_baseline(source: Path, out: Path, baseline_ref: str) -> None:
    if out.exists():
        if not out.is_dir() or any(out.iterdir()):
            raise ValueError(f"synthetic donor destination must be absent or empty: {out}")
        out.rmdir()
    out.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ("git", "clone", "--quiet", "--no-checkout", "--shared", str(source), str(out)),
        check=True,
    )
    git(out, "checkout", "--quiet", "--detach", baseline_ref)
    git(out, "config", "user.name", "M3 Lineage Donor")
    git(out, "config", "user.email", "m3-lineage-donor@invalid")
    if git(out, "status", "--porcelain=v1", "-uall").strip():
        raise ValueError("synthetic donor baseline checkout is dirty")


def commit_synthetic(out: Path, row: Lineage) -> tuple[str, str]:
    env = os.environ.copy()
    timestamp = f"{row.date}T12:00:00+0000"
    env["GIT_AUTHOR_DATE"] = timestamp
    env["GIT_COMMITTER_DATE"] = timestamp
    process = subprocess.run(
        (
            "git",
            "-C",
            str(out),
            "commit",
            "--quiet",
            "--no-gpg-sign",
            "-m",
            f"M3 synthetic lineage {row.order}: JDK-{row.jbs} {row.role}",
        ),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        env=env,
        check=False,
    )
    if process.returncode:
        raise RuntimeError(
            f"cannot commit synthetic lineage order {row.order}: "
            + process.stderr.decode("utf-8", "replace").strip()
        )
    return (
        git(out, "rev-parse", "HEAD").strip(),
        git(out, "rev-parse", "HEAD^{tree}").strip(),
    )


def materialize(
    source: Path,
    baseline_ref: str,
    lineage_file: Path,
    paths_file: Path,
    out: Path,
) -> list[Receipt]:
    checked_ref = canonical_ref(baseline_ref)
    rows = lineage(lineage_file)
    paths = selected_paths(paths_file)
    verify_source(source, checked_ref, rows)
    clone_baseline(source, out, checked_ref)

    receipts: list[Receipt] = []
    for row in rows:
        patch_bytes, path_count = patch(source, row, paths)
        if not patch_bytes:
            receipts.append(
                Receipt(
                    row.order,
                    row.jbs,
                    row.commit,
                    row.parent,
                    row.role,
                    0,
                    git(out, "rev-parse", "HEAD").strip(),
                    git(out, "rev-parse", "HEAD^{tree}").strip(),
                )
            )
            continue
        process = subprocess.run(
            ("git", "-C", str(out), "apply", "--3way", "--index", "--whitespace=nowarn", "-"),
            input=patch_bytes,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if process.returncode:
            git(out, "reset", "--hard", "HEAD")
            raise RuntimeError(
                f"lineage delta conflict order={row.order} commit={row.commit}: "
                + process.stderr.decode("utf-8", "replace").strip()
            )
        if not git(out, "diff", "--cached", "--name-only").strip():
            raise ValueError(f"selected lineage delta produced no staged change: {row.order}")
        synthetic_commit, synthetic_tree = commit_synthetic(out, row)
        receipts.append(
            Receipt(
                row.order,
                row.jbs,
                row.commit,
                row.parent,
                row.role,
                path_count,
                synthetic_commit,
                synthetic_tree,
            )
        )

    if git(out, "status", "--porcelain=v1", "-uall").strip():
        raise ValueError("synthetic donor final tree is dirty")
    return receipts


def write_receipt(rows: Sequence[Receipt], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow(
        (
            "order",
            "jbs",
            "source_commit",
            "source_parent",
            "role",
            "patch_paths",
            "synthetic_commit",
            "synthetic_tree",
        )
    )
    for row in rows:
        writer.writerow(
            (
                row.order,
                row.jbs,
                row.source_commit,
                row.source_parent,
                row.role,
                row.patch_paths,
                row.synthetic_commit,
                row.synthetic_tree,
            )
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--baseline-ref", default="jdk-21+35")
    parser.add_argument("--lineage", type=Path, required=True)
    parser.add_argument("--paths-file", type=Path, required=True)
    parser.add_argument("--out-repo", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    rows = materialize(
        args.repo.resolve(),
        args.baseline_ref,
        args.lineage,
        args.paths_file,
        args.out_repo.resolve(),
    )
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    with args.receipt.open("w", encoding="utf-8", newline="") as handle:
        write_receipt(rows, handle)
    print(
        "LINEAGE_DONOR "
        f"commits={len(rows)} patch_paths={sum(row.patch_paths for row in rows)} "
        f"head={git(args.out_repo.resolve(), 'rev-parse', 'HEAD').strip()} "
        f"tree={git(args.out_repo.resolve(), 'rev-parse', 'HEAD^{tree}').strip()}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
