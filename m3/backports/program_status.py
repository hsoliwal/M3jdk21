#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Deterministic M3JDK21 backport-program status from repository-owned evidence."""

from __future__ import annotations

import argparse
import csv
import json
from collections import Counter
from pathlib import Path

import release_jep_authority

PENDING_JEP = {"candidate", "candidate-high-risk", "hold-compat", "hold-jit", "hold-preview"}
DECIDED_JEP = {"reject-compat", "reject-language", "superseded", "superseded-high-risk", "superseded-jit"}
PENDING_SEED = {"candidate", "candidate-adapted", "candidate-high-risk", "hold-dependency", "hold-javac"}

def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))

def classify_jep(disposition: str) -> str:
    if disposition in PENDING_JEP:
        return "PENDING_PROOF_OR_IMPLEMENTATION"
    if disposition in DECIDED_JEP:
        return "DECIDED_NO_DIRECT_BACKPORT"
    raise ValueError(f"unclassified JEP disposition: {disposition}")

def classify_seed(disposition: str) -> str:
    if disposition == "admitted":
        return "ADMITTED"
    if disposition in PENDING_SEED:
        return "PENDING_PROOF_OR_IMPLEMENTATION"
    raise ValueError(f"unclassified seed disposition: {disposition}")

def snapshot(root: Path) -> dict[str, object]:
    backports = root / "m3" / "backports"
    authority = release_jep_authority.verify_repository_authority(root)
    jeps = read_tsv(backports / "JEP_CATALOGUE.tsv")
    priorities = read_tsv(backports / "POST21_PRIORITY_COMPATIBILITY.tsv")
    seeds = read_tsv(backports / "UPSTREAM_CHANGE_SEEDS.tsv")
    forks = read_tsv(backports / "COMMUNITY_FORKS.tsv")
    recipe_root = backports / "recipes"
    packets = sorted(
        path.name for path in recipe_root.iterdir()
        if path.is_dir() and (path / "README.md").is_file()
    )
    jep_ids = [row["jep"] for row in jeps]
    release_jep_pairs = [(int(row["release"]), int(row["jep"])) for row in jeps]
    if any(release < 22 or release > 27 for release, _jep in release_jep_pairs):
        raise ValueError("released JEP catalogue contains a row outside JDK 22..27")
    if release_jep_pairs != sorted(release_jep_pairs):
        raise ValueError("released JEP catalogue must be sorted by release then JEP")
    duplicate_jeps = sorted(
        jep for jep, count in Counter(jep_ids).items() if count > 1
    )
    priority_missing = sorted(
        row["jep"] for row in priorities if row["jep"] not in set(jep_ids)
    )
    if duplicate_jeps:
        raise ValueError(f"duplicate JEP catalogue rows: {duplicate_jeps}")
    if priority_missing:
        raise ValueError(
            f"priority JEP rows missing from full catalogue: {priority_missing}"
        )
    jep_states = Counter(classify_jep(row["disposition"]) for row in jeps)
    seed_states = Counter(classify_seed(row["disposition"]) for row in seeds)
    return {
        "schema": 1,
        "jep_rows": len(jeps),
        "jep_authority_rows": sum(len(values) for values in authority.values()),
        "jep_states": dict(sorted(jep_states.items())),
        "jep_dispositions": dict(sorted(Counter(row["disposition"] for row in jeps).items())),
        "jep_unique_rows": len(set(jep_ids)),
        "priority_jep_rows": len(priorities),
        "priority_missing_from_catalogue": priority_missing,
        "released_jdk_floor": min(release for release, _jep in release_jep_pairs),
        "released_jdk_ceiling": max(release for release, _jep in release_jep_pairs),
        "seed_rows": len(seeds),
        "seed_states": dict(sorted(seed_states.items())),
        "seed_dispositions": dict(sorted(Counter(row["disposition"] for row in seeds).items())),
        "community_fork_rows": len(forks),
        "community_fork_planes": dict(sorted(Counter(row["plane"] for row in forks).items())),
        "community_fork_source_copy_authority": any(
            row["source_copy_authority"] != "false" for row in forks
        ),
        "community_fork_selected_for_distribution": any(
            row["selected_for_distribution"] != "false" for row in forks
        ),
        "materialized_packets": packets,
        "materialized_packet_count": len(packets),
        "completion_claim": False,
    }

def render_tsv(data: dict[str, object]) -> str:
    rows = [
        ("jep", "total", str(data["jep_rows"])),
        ("jep", "unique", str(data["jep_unique_rows"])),
        ("jep-priority", "total", str(data["priority_jep_rows"])),
        *(( "jep-state", key, str(value)) for key, value in data["jep_states"].items()),
        ("seed", "total", str(data["seed_rows"])),
        *(( "seed-state", key, str(value)) for key, value in data["seed_states"].items()),
        ("community-fork", "total", str(data["community_fork_rows"])),
        *(( "community-fork-plane", key, str(value))
          for key, value in data["community_fork_planes"].items()),
        ("packet", "materialized", str(data["materialized_packet_count"])),
    ]
    return "kind\tstate\tcount\n" + "".join(
        f"{kind}\t{state}\t{count}\n" for kind, state, count in rows
    )

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--tsv-out", type=Path)
    return parser.parse_args()

def main() -> int:
    args = parse_args()
    data = snapshot(args.root.resolve())
    encoded = json.dumps(data, indent=2, sort_keys=True) + "\n"
    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(encoded, encoding="utf-8")
    if args.tsv_out:
        args.tsv_out.parent.mkdir(parents=True, exist_ok=True)
        args.tsv_out.write_text(render_tsv(data), encoding="utf-8")
    print(encoded, end="")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
