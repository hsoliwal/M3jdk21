#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Inventory JEP-numbered upstream commit seeds and exact touched paths.

This tool is deliberately evidence-only. A commit message mentioning a JEP is a discovery seed,
not dependency closure and never a compatibility/admission decision. The resulting per-JEP path
lists feed the existing exact JDK21↔donor file-delta / hash-pinned recipe generator.

The complete released-change denominator remains inventory.py / compatibility_queue.py.
"""

from __future__ import annotations

import argparse
import csv
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

BASELINE_REF = "jdk-21+35"
GA_TAGS: dict[int, str] = {
    22: "jdk-22+36",
    23: "jdk-23+37",
    24: "jdk-24+36",
    25: "jdk-25+36",
    26: "jdk-26+35",
    27: "jdk-27+35",
}

NON_PRODUCT_DISPOSITIONS = {
    "reject-language",
    "superseded",
}

HOLD_DISPOSITIONS = {
    "hold-jit",
    "hold-compat",
}


@dataclass(frozen=True)
class JepRow:
    release: int
    jep: int
    title: str
    domain: str
    disposition: str
    reason: str
    superseded_by: str


@dataclass(frozen=True)
class CommitSeed:
    release: int
    jep: int
    disposition: str
    donor_ref: str
    commit: str
    subject: str


@dataclass(frozen=True)
class TouchedPath:
    release: int
    jep: int
    commit: str
    status: str
    path: str


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


def _verify_repo(repo: Path, releases: Iterable[int]) -> None:
    if not repo.is_dir():
        raise ValueError(f"not a directory: {repo}")
    if _git(repo, "rev-parse", "--is-inside-work-tree").strip() != "true":
        raise ValueError(f"not a Git work tree: {repo}")

    refs = {BASELINE_REF}
    for release in releases:
        try:
            refs.add(GA_TAGS[release])
        except KeyError as failure:
            raise ValueError(f"unsupported JEP release: {release}") from failure
    for ref in sorted(refs):
        _git(repo, "rev-parse", "--verify", f"{ref}^{{commit}}")


def read_catalogue(path: Path) -> list[JepRow]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError("empty JEP catalogue")

    result: list[JepRow] = []
    seen: set[int] = set()
    for row in rows:
        release = int(row["release"])
        jep = int(row["jep"])
        if release not in GA_TAGS:
            raise ValueError(f"unsupported release {release} for JEP {jep}")
        if jep in seen:
            raise ValueError(f"duplicate JEP {jep}")
        seen.add(jep)
        result.append(
            JepRow(
                release=release,
                jep=jep,
                title=row["title"].strip(),
                domain=row["domain"].strip(),
                disposition=row["disposition"].strip(),
                reason=row["reason"].strip(),
                superseded_by=row.get("superseded_by", "").strip(),
            )
        )
    return result


def commit_seeds(repo: Path, row: JepRow) -> list[CommitSeed]:
    donor_ref = GA_TAGS[row.release]
    revision_range = f"{BASELINE_REF}..{donor_ref}"
    pattern = rf"JEP[[:space:]-]*{row.jep}([^0-9]|$)"
    raw = _git(
        repo,
        "log",
        revision_range,
        "--regexp-ignore-case",
        "--extended-regexp",
        f"--grep={pattern}",
        "--format=%H%x09%s",
    )

    result: list[CommitSeed] = []
    seen: set[str] = set()
    for line in raw.splitlines():
        if not line.strip():
            continue
        cells = line.split("\t", 1)
        if len(cells) != 2 or not re.fullmatch(r"[0-9a-f]{40,64}", cells[0]):
            raise ValueError(f"invalid git log row for JEP {row.jep}: {line!r}")
        if cells[0] in seen:
            continue
        seen.add(cells[0])
        result.append(
            CommitSeed(
                release=row.release,
                jep=row.jep,
                disposition=row.disposition,
                donor_ref=donor_ref,
                commit=cells[0],
                subject=cells[1],
            )
        )
    result.sort(key=lambda seed: seed.commit)
    return result


def touched_paths(repo: Path, seed: CommitSeed) -> list[TouchedPath]:
    raw = _git(
        repo,
        "diff-tree",
        "--root",
        "--no-commit-id",
        "--name-status",
        "-r",
        "-M",
        seed.commit,
    )
    result: list[TouchedPath] = []
    for line in raw.splitlines():
        if not line:
            continue
        cells = line.split("\t")
        status = cells[0]
        if status.startswith(("R", "C")):
            if len(cells) != 3:
                raise ValueError(f"invalid rename/copy row: {line!r}")
            result.append(
                TouchedPath(seed.release, seed.jep, seed.commit, status + "_OLD", cells[1])
            )
            result.append(
                TouchedPath(seed.release, seed.jep, seed.commit, status + "_NEW", cells[2])
            )
        else:
            if len(cells) != 2:
                raise ValueError(f"invalid path row: {line!r}")
            result.append(
                TouchedPath(seed.release, seed.jep, seed.commit, status, cells[1])
            )
    result.sort(key=lambda item: (item.path, item.status))
    return result


def next_action(row: JepRow, seeds: Sequence[CommitSeed]) -> str:
    if row.disposition == "superseded":
        return "SKIP_SUPERSEDED_USE_FINAL_LINEAGE"
    if row.disposition == "reject-language":
        return "NO_PRODUCT_PACKET_LANGUAGE_LOCK"
    if row.disposition in HOLD_DISPOSITIONS:
        return "HOLD_DEPENDENCY_OR_COMPATIBILITY_REVIEW"
    if seeds:
        return "REVIEW_SEED_COMMITS_AND_CLOSE_DEPENDENCIES"
    return "MANUAL_UPSTREAM_COMMIT_DISCOVERY"


def inventory(
    repo: Path,
    catalogue: Sequence[JepRow],
) -> tuple[list[CommitSeed], list[TouchedPath]]:
    _verify_repo(repo, {row.release for row in catalogue})
    commits: list[CommitSeed] = []
    paths: list[TouchedPath] = []
    for row in catalogue:
        seeds = commit_seeds(repo, row)
        commits.extend(seeds)
        for seed in seeds:
            paths.extend(touched_paths(repo, seed))
    commits.sort(key=lambda item: (item.release, item.jep, item.commit))
    paths.sort(key=lambda item: (item.release, item.jep, item.commit, item.path, item.status))
    return commits, paths


def write_inventory(
    out: Path,
    catalogue: Sequence[JepRow],
    commits: Sequence[CommitSeed],
    paths: Sequence[TouchedPath],
) -> None:
    out.mkdir(parents=True, exist_ok=True)
    path_root = out / "paths"
    path_root.mkdir(parents=True, exist_ok=True)

    with (out / "JEP_UPSTREAM_COMMITS.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            (
                "release",
                "jep",
                "disposition",
                "donor_ref",
                "commit",
                "subject",
                "authority",
            )
        )
        for seed in commits:
            writer.writerow(
                (
                    seed.release,
                    seed.jep,
                    seed.disposition,
                    seed.donor_ref,
                    seed.commit,
                    seed.subject,
                    "DISCOVERY_SEED_ONLY_NOT_DEPENDENCY_CLOSURE",
                )
            )

    with (out / "JEP_UPSTREAM_PATHS.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("release", "jep", "commit", "status", "path"))
        for item in paths:
            writer.writerow(
                (item.release, item.jep, item.commit, item.status, item.path)
            )

    seeds_by_jep: dict[int, list[CommitSeed]] = {}
    for seed in commits:
        seeds_by_jep.setdefault(seed.jep, []).append(seed)
    paths_by_jep: dict[int, set[str]] = {}
    for item in paths:
        paths_by_jep.setdefault(item.jep, set()).add(item.path)

    with (out / "JEP_PACKET_QUEUE.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            (
                "release",
                "jep",
                "title",
                "domain",
                "disposition",
                "seed_commit_count",
                "seed_path_count",
                "next_action",
                "authority",
            )
        )
        for row in catalogue:
            seeds = seeds_by_jep.get(row.jep, [])
            seed_paths = sorted(paths_by_jep.get(row.jep, set()))
            writer.writerow(
                (
                    row.release,
                    row.jep,
                    row.title,
                    row.domain,
                    row.disposition,
                    len(seeds),
                    len(seed_paths),
                    next_action(row, seeds),
                    "INVENTORY_ONLY_NO_COMPATIBILITY_OR_MUTATION_AUTHORITY",
                )
            )
            if seed_paths:
                (path_root / f"jep-{row.jep}.txt").write_text(
                    "\n".join(seed_paths) + "\n",
                    encoding="utf-8",
                )

    total = len(catalogue)
    with_hits = sum(1 for row in catalogue if seeds_by_jep.get(row.jep))
    product_candidates = sum(
        1
        for row in catalogue
        if row.disposition not in NON_PRODUCT_DISPOSITIONS
    )
    candidate_hits = sum(
        1
        for row in catalogue
        if row.disposition not in NON_PRODUCT_DISPOSITIONS
        and seeds_by_jep.get(row.jep)
    )
    (out / "SUMMARY.tsv").write_text(
        "metric\tvalue\n"
        f"catalogue_rows\t{total}\n"
        f"rows_with_jep_named_seed_commits\t{with_hits}\n"
        f"non_rejected_non_superseded_rows\t{product_candidates}\n"
        f"product_rows_with_seed_commits\t{candidate_hits}\n"
        "semantic_authority\tNONE\n",
        encoding="utf-8",
    )


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--catalogue", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument(
        "--release",
        type=int,
        action="append",
        choices=range(22, 28),
        help="limit catalogue rows by release; repeatable",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    catalogue = read_catalogue(args.catalogue)
    if args.release:
        releases = frozenset(args.release)
        catalogue = [row for row in catalogue if row.release in releases]
    if not catalogue:
        raise SystemExit("no JEP catalogue rows selected")

    commits, paths = inventory(args.repo.resolve(), catalogue)
    write_inventory(args.out.resolve(), catalogue, commits, paths)
    print(
        f"inventory: {len(catalogue)} JEP row(s), "
        f"{len(commits)} JEP-named commit seed(s), "
        f"{len(paths)} touched-path row(s)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
