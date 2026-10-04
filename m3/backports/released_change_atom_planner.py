#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Project the complete released-change compatibility queue into reusable exact FILE atoms.

The planner is evidence-only. It never changes compatibility_state, never mutates product source,
and never promotes a packet. For each selected risk cohort it compares exact JDK21 preimages
against the final GA state of the release, deduplicates identical file transformations by
content-addressed atom identity, and records commit -> atom edges.

Transient upstream files that exist in neither JDK21 nor the final GA tree are typed exclusions,
not generator failures.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence


def _load_generator():
    path = Path(__file__).with_name("generate_recipe_crates.py")
    spec = importlib.util.spec_from_file_location(
        "m3_generate_recipe_crates_for_released_changes", path
    )
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


GEN = _load_generator()

VALID_RISKS = {"LOW", "LOW_MEDIUM", "MEDIUM", "HIGH", "CRITICAL"}
PENDING = "PENDING_COMPATIBILITY_PROOF"
AUTHORITY = "INVENTORY_ONLY_NO_COMPATIBILITY_OR_MUTATION_AUTHORITY"


@dataclass(frozen=True)
class QueueRow:
    order: int
    release: int
    commit: str
    jbs_ids: str
    subject: str
    domain: str
    risk: str
    scope_floor: str
    proof_lane: str
    recipe_strategy: str
    compatibility_state: str
    next_action: str
    paths: tuple[str, ...]


@dataclass(frozen=True)
class Atom:
    atom_id: str
    path: str
    kind: str
    status: str
    before_sha256: str
    after_sha256: str


def _split_paths(value: str) -> tuple[str, ...]:
    return tuple(path for path in value.split(",") if path)


def read_queue(path: Path) -> list[QueueRow]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError("empty compatibility queue")

    result: list[QueueRow] = []
    seen_orders: set[int] = set()
    seen_commits: set[str] = set()
    for row in rows:
        order = int(row["order"])
        release = int(row["release"])
        commit = row["commit"]
        risk = row["risk"]
        state = row["compatibility_state"]
        if order in seen_orders:
            raise ValueError(f"duplicate queue order: {order}")
        if commit in seen_commits:
            raise ValueError(f"duplicate queue commit: {commit}")
        if risk not in VALID_RISKS:
            raise ValueError(f"invalid risk: {risk}")
        if state != PENDING:
            raise ValueError(
                f"planner accepts only pending compatibility rows: {commit}={state}"
            )
        seen_orders.add(order)
        seen_commits.add(commit)
        result.append(
            QueueRow(
                order=order,
                release=release,
                commit=commit,
                jbs_ids=row["jbs_ids"],
                subject=row["subject"],
                domain=row["domain"],
                risk=risk,
                scope_floor=row["scope_floor"],
                proof_lane=row["proof_lane"],
                recipe_strategy=row["recipe_strategy"],
                compatibility_state=state,
                next_action=row["next_action"],
                paths=_split_paths(row["paths"]),
            )
        )
    result.sort(key=lambda item: item.order)
    return result


def _frame(digest, value: str) -> None:
    data = value.encode("utf-8")
    digest.update(len(data).to_bytes(4, "big"))
    digest.update(data)


def atom_id(candidate) -> str:
    digest = hashlib.sha256()
    _frame(digest, "M3_RELEASED_CHANGE_FILE_ATOM_V1")
    _frame(digest, candidate.path)
    _frame(digest, candidate.kind)
    _frame(digest, candidate.before_sha256)
    _frame(digest, candidate.after_sha256)
    return digest.hexdigest()


def _selected_rows(
    queue: Sequence[QueueRow],
    risks: frozenset[str],
    max_items: int | None,
) -> tuple[list[QueueRow], set[int]]:
    eligible = [row for row in queue if row.risk in risks]
    if max_items is None:
        selected = eligible
    else:
        if max_items < 1:
            raise ValueError("max_items")
        selected = eligible[:max_items]
    return selected, {row.order for row in selected}


def plan(
    repo: Path,
    queue: Sequence[QueueRow],
    risks: Iterable[str],
    out: Path,
    max_items: int | None = None,
) -> list[tuple[str, ...]]:
    risk_set = frozenset(risks)
    if not risk_set:
        raise ValueError("at least one risk cohort is required")
    unknown = risk_set - VALID_RISKS
    if unknown:
        raise ValueError(f"invalid risk cohort(s): {sorted(unknown)}")

    selected, selected_orders = _selected_rows(queue, risk_set, max_items)
    out.mkdir(parents=True, exist_ok=True)

    rows_by_release: dict[int, list[QueueRow]] = {}
    for row in selected:
        rows_by_release.setdefault(row.release, []).append(row)

    candidates_by_release: dict[int, dict[str, object]] = {}
    exclusions_by_release: dict[int, dict[str, str]] = {}
    transient_by_release: dict[int, set[str]] = {}

    for release, release_rows in sorted(rows_by_release.items()):
        selected_paths = {
            path
            for row in release_rows
            for path in row.paths
        }
        if not selected_paths:
            candidates_by_release[release] = {}
            exclusions_by_release[release] = {}
            transient_by_release[release] = set()
            continue

        deltas = GEN.DELTA.compare(repo, (release,))
        known_paths = {delta.path for delta in deltas}
        transient = selected_paths - known_paths
        replayable = selected_paths & known_paths

        if replayable:
            candidates, exclusions = GEN.candidates(
                repo,
                release,
                selected=replayable,
                all_candidates=False,
                include_text=True,
            )
        else:
            candidates, exclusions = [], []

        candidates_by_release[release] = {
            candidate.path: candidate for candidate in candidates
        }
        exclusions_by_release[release] = dict(exclusions)
        transient_by_release[release] = set(transient)

    atoms: dict[str, Atom] = {}
    edges: list[tuple[str, ...]] = []
    exclusion_rows: list[tuple[str, ...]] = []
    packet_rows: list[tuple[str, ...]] = []

    for row in queue:
        if row.risk not in risk_set:
            packet_rows.append(
                _packet_status(row, 0, 0, 0, "SKIPPED_RISK_FILTER")
            )
            continue
        if row.order not in selected_orders:
            packet_rows.append(
                _packet_status(row, 0, 0, 0, "SKIPPED_BATCH_LIMIT")
            )
            continue
        if not row.paths:
            packet_rows.append(
                _packet_status(row, 0, 0, 0, "NO_TOUCHED_PATHS")
            )
            continue

        candidate_map = candidates_by_release[row.release]
        exclusion_map = exclusions_by_release[row.release]
        transient = transient_by_release[row.release]

        candidate_count = 0
        exclusion_count = 0
        no_delta_count = 0
        for path in row.paths:
            candidate = candidate_map.get(path)
            if candidate is not None:
                identifier = atom_id(candidate)
                atoms.setdefault(
                    identifier,
                    Atom(
                        atom_id=identifier,
                        path=candidate.path,
                        kind=candidate.kind,
                        status=candidate.status,
                        before_sha256=candidate.before_sha256,
                        after_sha256=candidate.after_sha256,
                    ),
                )
                edges.append(
                    (
                        str(row.order),
                        str(row.release),
                        row.commit,
                        row.risk,
                        row.scope_floor,
                        row.proof_lane,
                        identifier,
                        path,
                        AUTHORITY,
                    )
                )
                candidate_count += 1
                continue

            if path in transient:
                reason = "TYPED_EXCLUSION_TRANSIENT_PATH_NOT_IN_JDK21_OR_FINAL_GA"
                exclusion_rows.append(
                    (
                        str(row.order),
                        str(row.release),
                        row.commit,
                        path,
                        reason,
                        AUTHORITY,
                    )
                )
                exclusion_count += 1
                continue

            reason = exclusion_map.get(path)
            if reason is not None:
                exclusion_rows.append(
                    (
                        str(row.order),
                        str(row.release),
                        row.commit,
                        path,
                        reason,
                        AUTHORITY,
                    )
                )
                exclusion_count += 1
                continue

            no_delta_count += 1

        if candidate_count:
            state = "FILE_ATOMS_IDENTIFIED"
        elif exclusion_count:
            state = "ONLY_TYPED_EXCLUSIONS"
        else:
            state = "NO_FINAL_GA_DELTA"

        packet_rows.append(
            _packet_status(
                row,
                candidate_count,
                exclusion_count,
                no_delta_count,
                state,
            )
        )

    atoms_sorted = sorted(
        atoms.values(),
        key=lambda atom: (atom.path, atom.before_sha256, atom.after_sha256, atom.atom_id),
    )
    edges.sort(key=lambda value: (int(value[0]), value[7], value[6]))
    exclusion_rows.sort(key=lambda value: (int(value[0]), value[3], value[4]))
    packet_rows.sort(key=lambda value: int(value[0]))

    with (out / "FILE_ATOMS.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            (
                "atom_id",
                "path",
                "kind",
                "status",
                "before_sha256",
                "after_sha256",
                "authority",
            )
        )
        for atom in atoms_sorted:
            writer.writerow(
                (
                    atom.atom_id,
                    atom.path,
                    atom.kind,
                    atom.status,
                    atom.before_sha256,
                    atom.after_sha256,
                    AUTHORITY,
                )
            )

    with (out / "COMMIT_ATOM_EDGES.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            (
                "queue_order",
                "release",
                "commit",
                "risk",
                "scope_floor",
                "proof_lane",
                "atom_id",
                "path",
                "authority",
            )
        )
        writer.writerows(edges)

    with (out / "COMMIT_EXCLUSIONS.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            (
                "queue_order",
                "release",
                "commit",
                "path",
                "reason",
                "authority",
            )
        )
        writer.writerows(exclusion_rows)

    with (out / "PACKET_STATUS.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            (
                "queue_order",
                "release",
                "commit",
                "jbs_ids",
                "risk",
                "scope_floor",
                "proof_lane",
                "candidate_atom_count",
                "typed_exclusion_count",
                "no_final_ga_delta_count",
                "state",
                "authority",
            )
        )
        writer.writerows(packet_rows)

    unique_edges = len({edge[6] for edge in edges})
    (out / "SUMMARY.tsv").write_text(
        "metric\tvalue\n"
        f"queue_rows\t{len(queue)}\n"
        f"selected_rows\t{len(selected)}\n"
        f"unique_file_atoms\t{len(atoms_sorted)}\n"
        f"commit_atom_edges\t{len(edges)}\n"
        f"edge_unique_atom_ids\t{unique_edges}\n"
        f"typed_exclusions\t{len(exclusion_rows)}\n"
        f"risk_filter\t{','.join(sorted(risk_set))}\n"
        f"authority\t{AUTHORITY}\n",
        encoding="utf-8",
    )
    return packet_rows


def _packet_status(
    row: QueueRow,
    candidate_count: int,
    exclusion_count: int,
    no_delta_count: int,
    state: str,
) -> tuple[str, ...]:
    return (
        str(row.order),
        str(row.release),
        row.commit,
        row.jbs_ids,
        row.risk,
        row.scope_floor,
        row.proof_lane,
        str(candidate_count),
        str(exclusion_count),
        str(no_delta_count),
        state,
        AUTHORITY,
    )


def materialize_atom(
    repo: Path,
    release: int,
    path: str,
    out: Path,
) -> list[str]:
    """Materialize one previously identified exact FILE atom as a reusable recipe crate."""
    candidates, exclusions = GEN.candidates(
        repo,
        release,
        selected={path},
        all_candidates=False,
        include_text=True,
    )
    if exclusions:
        raise ValueError(f"atom path is no longer replayable: {exclusions}")
    if len(candidates) != 1:
        raise ValueError(
            f"expected one replayable atom for {path}, found {len(candidates)}"
        )
    return GEN.materialize(
        out,
        release,
        candidates,
        exclusions,
        crate_size=1,
    )


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--queue", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument(
        "--risk",
        action="append",
        choices=sorted(VALID_RISKS),
        help="risk cohort to project; repeatable; default is all cohorts",
    )
    parser.add_argument(
        "--max-items",
        type=int,
        help="bounded prefix of eligible compatibility-queue rows",
    )
    parser.add_argument(
        "--materialize-release",
        type=int,
        choices=range(22, 28),
    )
    parser.add_argument("--materialize-path")
    parser.add_argument("--materialize-out", type=Path)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    repo = args.repo.resolve()

    materialize_fields = (
        args.materialize_release,
        args.materialize_path,
        args.materialize_out,
    )
    if any(value is not None for value in materialize_fields):
        if not all(value is not None for value in materialize_fields):
            raise SystemExit(
                "materialize mode requires --materialize-release, "
                "--materialize-path and --materialize-out"
            )
        crates = materialize_atom(
            repo,
            args.materialize_release,
            args.materialize_path,
            args.materialize_out.resolve(),
        )
        print(f"materialized {len(crates)} exact FILE atom crate(s)")
        return 0

    queue = read_queue(args.queue.resolve())
    risks = args.risk or sorted(VALID_RISKS)
    rows = plan(
        repo,
        queue,
        risks,
        args.out.resolve(),
        args.max_items,
    )
    selected = sum(
        row[10]
        not in {"SKIPPED_RISK_FILTER", "SKIPPED_BATCH_LIMIT"}
        for row in rows
    )
    print(
        f"planned {len(rows)} compatibility row(s); "
        f"{selected} selected for FILE-atom projection"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
