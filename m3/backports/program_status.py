#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Deterministic M3JDK21 backport-program status from repository-owned evidence."""

from __future__ import annotations

import argparse
import csv
import json
from collections import Counter
from pathlib import Path

PENDING_JEP = {
    "candidate",
    "candidate-adapted",
    "candidate-high-risk",
    "hold-compat",
    "hold-jit",
    "hold-preview",
}
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
    jeps = read_tsv(backports / "JEP_CATALOGUE.tsv")
    seeds = read_tsv(backports / "UPSTREAM_CHANGE_SEEDS.tsv")
    recipe_root = backports / "recipes"
    packets = sorted(
        path.name for path in recipe_root.iterdir()
        if path.is_dir() and (path / "README.md").is_file()
    )
    jep_states = Counter(classify_jep(row["disposition"]) for row in jeps)
    seed_states = Counter(classify_seed(row["disposition"]) for row in seeds)
    return {
        "schema": 1,
        "jep_rows": len(jeps),
        "jep_states": dict(sorted(jep_states.items())),
        "jep_dispositions": dict(sorted(Counter(row["disposition"] for row in jeps).items())),
        "seed_rows": len(seeds),
        "seed_states": dict(sorted(seed_states.items())),
        "seed_dispositions": dict(sorted(Counter(row["disposition"] for row in seeds).items())),
        "materialized_packets": packets,
        "materialized_packet_count": len(packets),
        "completion_claim": False,
    }

def render_tsv(data: dict[str, object]) -> str:
    rows = [
        ("jep", "total", str(data["jep_rows"])),
        *(( "jep-state", key, str(value)) for key, value in data["jep_states"].items()),
        ("seed", "total", str(data["seed_rows"])),
        *(( "seed-state", key, str(value)) for key, value in data["seed_states"].items()),
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
