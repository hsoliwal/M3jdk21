#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Project JEP discovery seeds into exact candidate FILE atoms.

This is a planning bridge only. The input path sets come from JEP-numbered upstream commit seeds,
which are explicitly not dependency closure. Generated hash-pinned crates remain
CANDIDATE_UNVERIFIED and carry no compatibility, equivalence, mutation, or promotion authority.
"""

from __future__ import annotations

import argparse
import csv
import importlib.util
import sys
from pathlib import Path
from typing import Sequence


def _load_generator():
    path = Path(__file__).with_name("generate_recipe_crates.py")
    spec = importlib.util.spec_from_file_location("m3_generate_recipe_crates_for_jep_seeds", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


GEN = _load_generator()

REVIEW_ACTION = "REVIEW_SEED_COMMITS_AND_CLOSE_DEPENDENCIES"


def _read_queue(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError("empty JEP packet queue")
    return rows


def _selected(path: Path) -> set[str]:
    values = {
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }
    if not values:
        raise ValueError(f"empty JEP seed path list: {path}")
    return values


def plan(
    repo: Path,
    queue_path: Path,
    paths_root: Path,
    out: Path,
) -> list[tuple[str, ...]]:
    queue = _read_queue(queue_path)
    out.mkdir(parents=True, exist_ok=True)

    rows: list[tuple[str, ...]] = []
    for item in queue:
        release = int(item["release"])
        jep = int(item["jep"])
        action = item["next_action"]
        path_file = paths_root / f"jep-{jep}.txt"

        if action != REVIEW_ACTION:
            rows.append(
                (
                    str(release),
                    str(jep),
                    item["disposition"],
                    action,
                    "0",
                    "0",
                    "0",
                    "SKIPPED_NON_REVIEW_ACTION",
                    "INVENTORY_ONLY_NO_COMPATIBILITY_OR_MUTATION_AUTHORITY",
                )
            )
            continue

        if not path_file.is_file():
            rows.append(
                (
                    str(release),
                    str(jep),
                    item["disposition"],
                    action,
                    "0",
                    "0",
                    "0",
                    "NO_SEED_PATH_LIST",
                    "INVENTORY_ONLY_NO_COMPATIBILITY_OR_MUTATION_AUTHORITY",
                )
            )
            continue

        selected = _selected(path_file)
        candidates, exclusions = GEN.candidates(
            repo,
            release,
            selected=selected,
            all_candidates=False,
            include_text=True,
        )
        jep_out = out / f"jep-{jep}"

        if candidates:
            crates = GEN.materialize(
                jep_out,
                release,
                candidates,
                exclusions,
                crate_size=1,
            )
            state = "SEED_FILE_ATOMS_GENERATED"
        else:
            jep_out.mkdir(parents=True, exist_ok=True)
            with (jep_out / "EXCLUSIONS.tsv").open(
                "w", encoding="utf-8", newline=""
            ) as handle:
                writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
                writer.writerow(("path", "reason"))
                writer.writerows(exclusions)
            crates = []
            state = (
                "NO_FINAL_GA_DELTA"
                if not exclusions
                else "ONLY_TYPED_EXCLUSIONS"
            )

        rows.append(
            (
                str(release),
                str(jep),
                item["disposition"],
                action,
                str(len(selected)),
                str(len(candidates)),
                str(len(exclusions)),
                state,
                "INVENTORY_ONLY_NO_COMPATIBILITY_OR_MUTATION_AUTHORITY",
            )
        )

    with (out / "JEP_SEED_PACKET_STATUS.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            (
                "release",
                "jep",
                "disposition",
                "source_action",
                "seed_path_count",
                "candidate_atom_count",
                "typed_exclusion_count",
                "state",
                "authority",
            )
        )
        writer.writerows(rows)
    return rows


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--queue", type=Path, required=True)
    parser.add_argument("--paths-root", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    rows = plan(
        args.repo.resolve(),
        args.queue.resolve(),
        args.paths_root.resolve(),
        args.out.resolve(),
    )
    generated = sum(
        1 for row in rows if row[7] == "SEED_FILE_ATOMS_GENERATED"
    )
    print(
        f"planned {len(rows)} JEP row(s); "
        f"{generated} row(s) emitted seed FILE atoms"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
