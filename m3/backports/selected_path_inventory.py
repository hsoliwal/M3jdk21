#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Join an exact work-item path denominator to integration and final-GA file-delta inventories."""

from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable, Mapping, Sequence


@dataclass(frozen=True)
class SelectedPathRow:
    path: str
    integration_status: str
    integration_recipe_lane: str
    ga_status: str
    ga_recipe_lane: str
    disposition: str
    reason: str


def read_paths(path: Path) -> list[str]:
    rows = [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if not rows:
        raise ValueError("selected path inventory is empty")
    if rows != sorted(rows) or len(rows) != len(set(rows)):
        raise ValueError("selected paths must be sorted and unique")
    for value in rows:
        if value.startswith("/") or "\\" in value or ".." in value.split("/"):
            raise ValueError(f"noncanonical selected path: {value}")
    return rows


def read_delta(path: Path) -> dict[str, dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError(f"empty delta inventory: {path}")
    required = {"path", "status", "recipe_lane", "baseline_ref", "donor_ref"}
    if not required.issubset(rows[0]):
        raise ValueError(f"delta inventory missing columns: {sorted(required - set(rows[0]))}")
    result: dict[str, dict[str, str]] = {}
    for row in rows:
        value = row["path"]
        if value in result:
            raise ValueError(f"duplicate delta path: {value}")
        result[value] = row
    return result


def classify(
    selected_paths: Iterable[str],
    integration: Mapping[str, Mapping[str, str]],
    ga: Mapping[str, Mapping[str, str]],
) -> list[SelectedPathRow]:
    result: list[SelectedPathRow] = []
    for path in selected_paths:
        integrated = integration.get(path)
        if integrated is None:
            raise ValueError(f"integration commit does not account for selected path: {path}")

        ga_row = ga.get(path)
        integration_status = integrated["status"]
        integration_lane = integrated["recipe_lane"]

        if ga_row is None:
            # This can occur only when the path was absent in the Java-21 baseline and was also
            # removed again before the final GA donor: it therefore vanishes from the union tree.
            if integration_status != "ADDED":
                raise ValueError(
                    "GA inventory omitted a path that was not integration-added: " + path
                )
            result.append(
                SelectedPathRow(
                    path,
                    integration_status,
                    integration_lane,
                    "ABSENT",
                    "NO_RECIPE",
                    "GA_REMOVED_HOLD",
                    "integration-added path is absent from both Java21 and final GA",
                )
            )
            continue

        ga_status = ga_row["status"]
        ga_lane = ga_row["recipe_lane"]
        if ga_status == "SAME":
            disposition = "ALREADY_EQUIVALENT"
            reason = "final GA bytes equal Java21 baseline"
        elif ga_status in {"ADDED", "MODIFIED"}:
            disposition = "MATERIALIZE_GA_POSTIMAGE"
            reason = "final GA path differs from Java21 baseline"
        elif ga_status == "REMOVED":
            disposition = "GA_REMOVAL_HOLD"
            reason = "final GA removes a Java21 baseline path; deletion requires explicit review"
        else:
            raise ValueError(f"unknown GA status {ga_status!r} for {path}")

        result.append(
            SelectedPathRow(
                path,
                integration_status,
                integration_lane,
                ga_status,
                ga_lane,
                disposition,
                reason,
            )
        )
    return result


def write_tsv(rows: Sequence[SelectedPathRow], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=list(asdict(rows[0]).keys()),
            delimiter="\t",
            lineterminator="\n",
        )
        writer.writeheader()
        for row in rows:
            writer.writerow(asdict(row))


def summary(rows: Sequence[SelectedPathRow]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for row in rows:
        counts[row.disposition] = counts.get(row.disposition, 0) + 1
    return dict(sorted(counts.items()))


def _args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paths", type=Path, required=True)
    parser.add_argument("--integration-delta", type=Path, required=True)
    parser.add_argument("--ga-delta", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _args(argv)
    paths = read_paths(args.paths)
    rows = classify(
        paths,
        read_delta(args.integration_delta),
        read_delta(args.ga_delta),
    )
    if len(rows) != len(paths):
        raise AssertionError("selected-path row count drift")
    write_tsv(rows, args.out)
    counts = summary(rows)
    print(
        "PASS: selected-path inventory "
        + " ".join(f"{key}={value}" for key, value in counts.items())
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
