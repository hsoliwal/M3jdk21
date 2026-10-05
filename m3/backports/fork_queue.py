#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate pinned fork evidence and emit a deterministic proof queue.

This module never admits compatibility or selects a donor for distribution.
"""

from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

ALLOWED_STATES = {
    "PENDING_COMPATIBILITY_PROOF",
    "PENDING_TOOLING_PROOF",
    "TYPED_EXCLUSION_MISSING_BACKEND",
    "TYPED_EXCLUSION_LICENSE",
}
ALLOWED_SCOPES = {"FILE", "PACKAGE", "MODULE", "MULTI_MODULE", "LIBRARY_API"}


@dataclass(frozen=True)
class ForkCandidate:
    candidate_id: str
    donor: str
    commit: str
    capability: str
    plane: str
    license: str
    join_keys: str
    status: str
    priority: int
    scope_floor: str
    next_proof: str


def _token(value: str, field: str) -> str:
    checked = value.strip()
    if not checked or any(ch in checked for ch in "\0\r\n\t"):
        raise ValueError(field)
    return checked


def _commit(value: str) -> str:
    checked = value.strip().lower()
    if len(checked) != 40 or any(ch not in "0123456789abcdef" for ch in checked):
        raise ValueError("commit")
    return checked


def parse(rows: Iterable[Mapping[str, str]]) -> list[ForkCandidate]:
    result: list[ForkCandidate] = []
    seen: set[str] = set()
    for row in rows:
        candidate_id = _token(row["candidate_id"], "candidate_id")
        if candidate_id in seen:
            raise ValueError("duplicate candidate_id: " + candidate_id)
        seen.add(candidate_id)
        status = _token(row["status"], "status")
        if status not in ALLOWED_STATES:
            raise ValueError("unsupported status: " + status)
        scope = _token(row["scope_floor"], "scope_floor")
        if scope not in ALLOWED_SCOPES:
            raise ValueError("unsupported scope: " + scope)
        priority = int(row["priority"])
        if priority < 0 or priority > 100:
            raise ValueError("priority")
        result.append(
            ForkCandidate(
                candidate_id=candidate_id,
                donor=_token(row["donor"], "donor"),
                commit=_commit(row["commit"]),
                capability=_token(row["capability"], "capability"),
                plane=_token(row["plane"], "plane"),
                license=_token(row["license"], "license"),
                join_keys=_token(row["join_keys"], "join_keys"),
                status=status,
                priority=priority,
                scope_floor=scope,
                next_proof=_token(row["next_proof"], "next_proof"),
            )
        )
    if not result:
        raise ValueError("fork candidate inventory is empty")
    return result


def read(path: Path) -> list[ForkCandidate]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return parse(csv.DictReader(handle, delimiter="\t"))


def queue(candidates: Sequence[ForkCandidate]) -> list[ForkCandidate]:
    return sorted(candidates, key=lambda row: (row.priority, row.candidate_id))


def write_tsv(candidates: Sequence[ForkCandidate], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow((
        "order", "candidate_id", "donor", "commit", "capability", "plane",
        "license", "join_keys", "status", "priority", "scope_floor",
        "selected_for_distribution", "next_proof",
    ))
    for order, row in enumerate(queue(candidates)):
        writer.writerow((
            order, row.candidate_id, row.donor, row.commit, row.capability, row.plane,
            row.license, row.join_keys, row.status, row.priority, row.scope_floor,
            "false", row.next_proof,
        ))


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--out", type=Path)
    args = parser.parse_args(argv)
    candidates = read(args.inventory)
    if args.out is None:
        import sys
        write_tsv(candidates, sys.stdout)
    else:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        with args.out.open("w", encoding="utf-8", newline="") as handle:
            write_tsv(candidates, handle)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
