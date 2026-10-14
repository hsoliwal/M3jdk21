#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed authority/bootstrap helpers for high-risk JEP residue.

This module records implementation-commit identity only. It does not classify Java-21
compatibility, infer semantic equivalence, apply generated crates, or grant promotion authority.
"""

from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
import json
from pathlib import Path
import re
import subprocess
from typing import Sequence

LEDGER = Path("m3/backports/HIGH_RISK_IMPLEMENTATION_AUTHORITY.tsv")
RESIDUE = Path("m3/backports/JEP_RESIDUE_QUEUE.tsv")
DEDICATED = {404, 475}
HIGH_RISK_ACTION = "CLOSE_DEPENDENCY_AND_HIGH_RISK_PROOF"
STATUS = "IMPLEMENTATION_COMMIT_RESOLVED"
DONOR_TAGS = {24: "jdk-24+36", 25: "jdk-25+36", 26: "jdk-26+35", 27: "jdk-27+35"}
HEX40 = re.compile(r"[0-9a-f]{40}")
ISO_DATE = re.compile(r"20[0-9]{2}-[0-9]{2}-[0-9]{2}")


@dataclass(frozen=True)
class Authority:
    jep: int
    release: int
    title: str
    implementation_jbs: int
    implementation_commit: str
    implementation_date: str
    implementation_path_count: int
    donor_tag: str
    dependency_jep_ids: tuple[int, ...]
    authority_status: str
    lineage_note: str


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError(f"empty TSV: {path}")
    return rows


def _dependencies(value: str) -> tuple[int, ...]:
    if not value:
        return ()
    ids = tuple(int(item) for item in value.split(","))
    if ids != tuple(sorted(set(ids))) or any(value <= 0 for value in ids):
        raise ValueError(f"invalid dependency_jep_ids: {value}")
    return ids


def load_authority(root: Path) -> list[Authority]:
    rows = read_tsv(root / LEDGER)
    result: list[Authority] = []
    seen: set[int] = set()
    for raw in rows:
        try:
            row = Authority(
                jep=int(raw["jep"]),
                release=int(raw["release"]),
                title=raw["title"].strip(),
                implementation_jbs=int(raw["implementation_jbs"]),
                implementation_commit=raw["implementation_commit"].strip(),
                implementation_date=raw["implementation_date"].strip(),
                implementation_path_count=int(raw["implementation_path_count"]),
                donor_tag=raw["donor_tag"].strip(),
                dependency_jep_ids=_dependencies(raw["dependency_jep_ids"].strip()),
                authority_status=raw["authority_status"].strip(),
                lineage_note=raw["lineage_note"].strip(),
            )
        except (KeyError, ValueError) as failure:
            raise ValueError(f"invalid implementation-authority row: {raw}") from failure

        if row.jep in seen or row.jep <= 0:
            raise ValueError(f"duplicate/invalid JEP: {row.jep}")
        seen.add(row.jep)
        if row.release not in DONOR_TAGS or row.donor_tag != DONOR_TAGS[row.release]:
            raise ValueError(f"invalid donor tag for JEP {row.jep}: {row.donor_tag}")
        if not row.title or row.implementation_jbs <= 0:
            raise ValueError(f"missing title/JBS for JEP {row.jep}")
        if not HEX40.fullmatch(row.implementation_commit):
            raise ValueError(f"invalid implementation commit for JEP {row.jep}")
        if not ISO_DATE.fullmatch(row.implementation_date):
            raise ValueError(f"invalid implementation date for JEP {row.jep}")
        if row.implementation_path_count <= 0:
            raise ValueError(f"invalid implementation path count for JEP {row.jep}")
        if row.authority_status != STATUS:
            raise ValueError(f"invalid authority status for JEP {row.jep}")

        result.append(row)

    if [row.jep for row in result] != sorted(seen):
        raise ValueError("implementation authority rows must be sorted by JEP")
    return result


def high_risk_residue(root: Path) -> dict[int, dict[str, str]]:
    rows = read_tsv(root / RESIDUE)
    result: dict[int, dict[str, str]] = {}
    for row in rows:
        if row["evidence_state"] != "NO_RECIPE_EVIDENCE":
            continue
        if row["next_action"] != HIGH_RISK_ACTION:
            continue
        jep = int(row["jep"])
        if jep in DEDICATED:
            continue
        if jep in result:
            raise ValueError(f"duplicate residue JEP: {jep}")
        result[jep] = row
    return result


def verify_repository_coverage(root: Path) -> list[Authority]:
    authority = load_authority(root)
    residue = high_risk_residue(root)
    by_jep = {row.jep: row for row in authority}
    if set(by_jep) != set(residue):
        raise ValueError(
            "high-risk implementation-authority coverage mismatch "
            f"missing={sorted(set(residue) - set(by_jep))} "
            f"extra={sorted(set(by_jep) - set(residue))}"
        )
    for jep, raw in residue.items():
        row = by_jep[jep]
        if row.release != int(raw["release"]) or row.title != raw["title"]:
            raise ValueError(f"residue identity mismatch for JEP {jep}")
    return authority


def _git(repo: Path, *args: str, check: bool = True) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args],
        check=check,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return result.stdout


def canonical_path(value: str) -> str:
    if (
        not value
        or value.startswith(("/", "\\"))
        or "\\" in value
        or ":" in value
        or len(value) > 4096
    ):
        raise ValueError(f"noncanonical upstream path: {value!r}")
    parts = value.split("/")
    if any(part in ("", ".", "..", ".git") for part in parts):
        raise ValueError(f"noncanonical upstream path: {value!r}")
    return value


def verify_upstream_commit(repo: Path, row: Authority) -> list[str]:
    repo = repo.resolve()
    _git(repo, "cat-file", "-e", f"{row.implementation_commit}^{{commit}}")
    ancestor = subprocess.run(
        ["git", "-C", str(repo), "merge-base", "--is-ancestor",
         row.implementation_commit, row.donor_tag],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if ancestor.returncode != 0:
        raise ValueError(
            f"implementation commit is not an ancestor of {row.donor_tag}: JEP {row.jep}"
        )
    raw = _git(
        repo,
        "diff-tree",
        "--no-commit-id",
        "--name-only",
        "-r",
        row.implementation_commit,
    )
    paths = sorted({canonical_path(line.strip()) for line in raw.splitlines() if line.strip()})
    if len(paths) != row.implementation_path_count:
        raise ValueError(
            f"implementation path count drift for JEP {row.jep}: "
            f"{len(paths)} != {row.implementation_path_count}"
        )
    return paths


def matrix(root: Path) -> dict[str, list[dict[str, object]]]:
    return {
        "include": [
            {
                "jep": row.jep,
                "release": row.release,
                "title": row.title,
                "implementation_jbs": row.implementation_jbs,
                "commit": row.implementation_commit,
                "path_count": row.implementation_path_count,
                "donor_tag": row.donor_tag,
                "dependency_jep_ids": ",".join(str(value) for value in row.dependency_jep_ids),
            }
            for row in verify_repository_coverage(root)
        ]
    }


def find(root: Path, jep: int) -> Authority:
    for row in verify_repository_coverage(root):
        if row.jep == jep:
            return row
    raise ValueError(f"JEP not in high-risk implementation authority: {jep}")


def _args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--matrix-json", action="store_true")
    parser.add_argument("--repo", type=Path)
    parser.add_argument("--jep", type=int)
    parser.add_argument("--paths-out", type=Path)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _args(argv)
    root = args.root.resolve()
    if args.matrix_json:
        print(json.dumps(matrix(root), sort_keys=True, separators=(",", ":")))
        return 0
    if args.repo is None or args.jep is None or args.paths_out is None:
        raise SystemExit("--repo, --jep and --paths-out are required outside --matrix-json")
    row = find(root, args.jep)
    paths = verify_upstream_commit(args.repo, row)
    args.paths_out.parent.mkdir(parents=True, exist_ok=True)
    args.paths_out.write_text("".join(path + "\n" for path in paths), encoding="utf-8")
    print(
        f"PASS: JEP {row.jep} implementation={row.implementation_commit} "
        f"paths={len(paths)} donor={row.donor_tag}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
