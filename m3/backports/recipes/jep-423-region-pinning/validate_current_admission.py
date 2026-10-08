#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Validate current-tree JEP 423 FILE admission and cumulative accounting from packet data."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path


def fields(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows or set(rows[0]) != {"field", "value"}:
        raise ValueError(f"invalid field/value packet: {path}")
    result: dict[str, str] = {}
    for row in rows:
        key = row["field"].strip()
        value = row["value"].strip()
        if not key or key in result:
            raise ValueError(f"duplicate/blank field in {path}: {key!r}")
        result[key] = value
    return result


def integer(data: dict[str, str], key: str) -> int:
    try:
        value = int(data[key])
    except (KeyError, ValueError) as failure:
        raise ValueError(f"invalid integer field {key}") from failure
    if value < 0:
        raise ValueError(f"negative integer field {key}")
    return value


def lines(path: Path) -> list[str]:
    values = [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if values != sorted(values) or len(values) != len(set(values)):
        raise ValueError(f"noncanonical sorted-unique path file: {path}")
    return values


def admission(packet: Path, admission_tsv: Path) -> str:
    request = fields(packet / "CURRENT_TREE_PROOF_REQUEST.tsv")
    with admission_tsv.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))

    candidates = integer(request, "candidate_paths")
    expected_mechanical = integer(request, "expected_mechanical_paths")
    expected_held = integer(request, "expected_held_paths")
    if expected_mechanical + expected_held != candidates:
        raise ValueError("packet candidate/mechanical/held denominator mismatch")
    if len(rows) != candidates:
        raise ValueError(f"admission row count {len(rows)} != {candidates}")

    mechanical = [row for row in rows if row["admission"] == "MECHANICAL_FILE_REPLAY"]
    held = [row for row in rows if row["admission"] == "HOLD_CURRENT_TREE_DRIFT"]
    unknown = [
        row for row in rows
        if row["admission"] not in {"MECHANICAL_FILE_REPLAY", "HOLD_CURRENT_TREE_DRIFT"}
    ]
    if unknown:
        raise ValueError(f"unknown admission states: {unknown}")
    if len(mechanical) != expected_mechanical or len(held) != expected_held:
        raise ValueError(
            "JEP423 admission drift "
            f"rows={len(rows)} mechanical={len(mechanical)} held={len(held)} "
            f"expected_mechanical={expected_mechanical} expected_held={expected_held}"
        )

    expected_held_path = request.get("expected_held_path", "NONE")
    if expected_held:
        if expected_held != 1 or expected_held_path in {"", "NONE"}:
            raise ValueError("held-path packet must name the single expected hold")
        if held[0]["path"] != expected_held_path:
            raise ValueError(f"unexpected held path: {held}")
    elif expected_held_path not in {"", "NONE"}:
        raise ValueError("zero-held packet must use expected_held_path=NONE")

    return (
        f"JEP423_CUMULATIVE_ADMISSION candidates={candidates} "
        f"mechanical={len(mechanical)} held={len(held)}"
    )


def preserved_paths(packet: Path) -> set[str]:
    split = packet / "COMPATIBILITY_SPLIT.tsv"
    with split.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    return {
        row["path"]
        for row in rows
        if row["disposition"] == "PRESERVE_JDK21_PATH"
    }


def accounting(packet: Path, runner: Path) -> str:
    request = fields(packet / "CURRENT_TREE_PROOF_REQUEST.tsv")
    candidates = set(lines(packet / "CUMULATIVE_ADMIT_PATHS.txt"))
    mechanical = set(lines(runner / "MECHANICAL_PATHS.txt"))
    held = candidates - mechanical
    unexpected = mechanical - candidates
    if unexpected:
        raise ValueError(f"mechanical paths outside candidate set: {sorted(unexpected)}")

    expected_candidates = integer(request, "candidate_paths")
    expected_mechanical = integer(request, "expected_mechanical_paths")
    expected_held = integer(request, "expected_held_paths")
    expected_preserved = integer(request, "preserved_java21_deletions")
    cumulative = integer(request, "cumulative_lineage_paths")

    if len(candidates) != expected_candidates:
        raise ValueError("candidate path denominator drift")
    if len(mechanical) != expected_mechanical or len(held) != expected_held:
        raise ValueError(
            f"mechanical/held accounting drift mechanical={len(mechanical)} held={len(held)}"
        )

    preserved = preserved_paths(packet)
    if len(preserved) != expected_preserved:
        raise ValueError(
            f"preserved Java21 path count {len(preserved)} != {expected_preserved}"
        )
    if candidates & preserved:
        raise ValueError("candidate and preserved path sets overlap")
    if len(candidates | preserved) != cumulative:
        raise ValueError("candidate + preserved denominator does not equal lineage closure")

    with (runner / "JDK21_TO_JEP423_FINAL.tsv").open(
        encoding="utf-8", newline=""
    ) as handle:
        delta = {
            row["path"]: row["status"]
            for row in csv.DictReader(handle, delimiter="\t")
            if row["path"] in mechanical
        }
    missing = mechanical - delta.keys()
    if missing:
        raise ValueError(f"mechanical paths absent from donor comparison: {sorted(missing)}")
    same = {path for path, status in delta.items() if status == "SAME"}

    with (runner / "file-atoms" / "CRATES.tsv").open(
        encoding="utf-8", newline=""
    ) as handle:
        crates = list(csv.DictReader(handle, delimiter="\t"))
    if any(int(row["target_count"]) != 1 for row in crates):
        raise ValueError("JEP423 FILE lane emitted a multi-target crate")
    generated = sum(int(row["target_count"]) for row in crates)

    with (runner / "file-atoms" / "EXCLUSIONS.tsv").open(
        encoding="utf-8", newline=""
    ) as handle:
        exclusions = list(csv.DictReader(handle, delimiter="\t"))
    if exclusions:
        raise ValueError(f"JEP423 typed exclusions require review: {exclusions}")

    if generated + len(same) != expected_mechanical:
        raise ValueError(
            f"mechanical accounting mismatch generated={generated} same={len(same)} "
            f"expected={expected_mechanical}"
        )
    if generated + len(same) + len(held) + len(preserved) != cumulative:
        raise ValueError("cumulative denominator accounting mismatch")

    return (
        f"JEP423_ACCOUNTED generated={generated} same={len(same)} "
        f"held={len(held)} preserved={len(preserved)} total={cumulative}"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--packet", type=Path, required=True)
    parser.add_argument("--admission", type=Path)
    parser.add_argument("--runner", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if (args.admission is None) == (args.runner is None):
        raise ValueError("select exactly one of --admission or --runner")
    if args.admission is not None:
        print(admission(args.packet, args.admission))
    else:
        print(accounting(args.packet, args.runner))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
