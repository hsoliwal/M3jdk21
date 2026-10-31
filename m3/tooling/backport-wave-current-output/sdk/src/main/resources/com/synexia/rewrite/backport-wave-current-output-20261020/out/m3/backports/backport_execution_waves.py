#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Compile the canonical M3JDK21 backport DAG into deterministic execution waves.

This module does not decide compatibility and does not mutate JDK source. It consumes the existing
backport_recipe_dag authority, optionally consumes an explicit atom-level scope approval ledger, and
projects the admitted DAG into scheduler-neutral waves plus Camel/Airflow/Drools metadata.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import json
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

READY = "READY"
EVIDENCE_HOLD = "EVIDENCE_HOLD"
SCOPE_APPROVAL_HOLD = "SCOPE_APPROVAL_HOLD"
DEPENDENCY_HOLD = "DEPENDENCY_HOLD"

FILE_SCOPE = "FILE"

_APPROVAL_COLUMNS = {"atom_id", "approved", "proof_ref"}
_SAFE = re.compile(r"[^a-z0-9_]+")


@dataclass(frozen=True)
class WaveItem:
    order: int
    work_id: str
    atom_id: str
    phase: str
    scope: str
    proof_lane: str
    recipe_ref: str
    depends_on: str
    execution_state: str
    wave: int
    scope_approved: bool
    next_action: str
    camel_route_id: str
    airflow_task_id: str
    drools_fact_id: str


def _load_dag_module(root: Path):
    path = root / "m3" / "backports" / "backport_recipe_dag.py"
    spec = importlib.util.spec_from_file_location("m3_backport_recipe_dag_runtime", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load canonical DAG authority: {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def _truth(value: str) -> bool:
    normalized = value.strip().lower()
    if normalized == "true":
        return True
    if normalized == "false":
        return False
    raise ValueError(f"invalid boolean: {value!r}")


def read_approvals(path: Path | None) -> dict[str, str]:
    if path is None:
        return {}
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="	")
        missing = _APPROVAL_COLUMNS - set(reader.fieldnames or ())
        if missing:
            raise ValueError(f"approval ledger missing columns: {sorted(missing)}")
        rows = list(reader)

    result: dict[str, str] = {}
    seen: set[str] = set()
    for row in rows:
        if None in row or any(row.get(column) is None for column in _APPROVAL_COLUMNS):
            raise ValueError("malformed approval ledger record")
        atom_id = row["atom_id"].strip()
        if not atom_id or atom_id in seen:
            raise ValueError(f"invalid or duplicate approval atom_id: {atom_id!r}")
        seen.add(atom_id)
        approved = _truth(row["approved"])
        proof = row["proof_ref"].strip()
        if approved and not proof:
            raise ValueError(f"approved atom lacks proof_ref: {atom_id}")
        if approved:
            result[atom_id] = proof
    return result


def _safe(value: str) -> str:
    safe = _SAFE.sub("_", value.lower()).strip("_")
    if not safe:
        raise ValueError(f"cannot project scheduler identity: {value!r}")
    return safe


def _scope_approval(atom, approvals: Mapping[str, str]) -> bool:
    # Scheduling eligibility is never granted to non-executable evidence, even at FILE scope.
    return bool(atom.executable and (atom.scope == FILE_SCOPE or atom.atom_id in approvals))


def _base_state(atom, approvals: Mapping[str, str]) -> tuple[str, str]:
    if not atom.executable:
        return EVIDENCE_HOLD, "WAIT_FOR_COMPATIBILITY_OR_RECIPE_ADMISSION"
    if atom.scope != FILE_SCOPE and atom.atom_id not in approvals:
        return SCOPE_APPROVAL_HOLD, "REVIEW_AND_RECORD_SCOPE_PROMOTION"
    return READY, "EXECUTE_RECIPE_ATOM_AND_PROVE"


def plan(atoms: Sequence[object], approvals: Mapping[str, str]) -> list[WaveItem]:
    """Create deterministic waves without changing canonical DAG semantics."""
    if not atoms:
        raise ValueError("empty backport DAG")

    by_id = {atom.atom_id: atom for atom in atoms}
    if len(by_id) != len(atoms):
        raise ValueError("duplicate atom id")
    if [atom.order for atom in atoms] != list(range(len(atoms))):
        raise ValueError("non-contiguous canonical atom order")
    unknown_approvals = sorted(set(approvals) - set(by_id))
    if unknown_approvals:
        raise ValueError(f"scope approvals reference unknown atoms: {unknown_approvals}")

    # Camel/Airflow/Drools task normalization must never collapse distinct canonical atoms.
    normalized: dict[str, str] = {}
    scopes = {"FILE", "VISIBILITY", "PACKAGE", "MODULE", "MULTI_MODULE", "LIBRARY_API"}
    for atom in atoms:
        if atom.scope not in scopes:
            raise ValueError(f"unrecognized edit scope: {atom.scope!r}")
        alias = _safe(atom.atom_id)
        previous = normalized.setdefault(alias, atom.atom_id)
        if previous != atom.atom_id:
            raise ValueError(
                f"scheduler identity collision: {previous!r} and {atom.atom_id!r}"
            )
        if atom.depends_on and atom.depends_on not in by_id:
            raise ValueError(f"unknown dependency: {atom.depends_on}")
        if atom.depends_on and by_id[atom.depends_on].work_id != atom.work_id:
            raise ValueError("cross-work-item dependency lacks canonical authority")

    state: dict[str, str] = {}
    action: dict[str, str] = {}
    for atom in atoms:
        state[atom.atom_id], action[atom.atom_id] = _base_state(atom, approvals)

    # A ready child cannot bypass a held parent.
    changed = True
    while changed:
        changed = False
        for atom in atoms:
            if state[atom.atom_id] != READY or not atom.depends_on:
                continue
            parent_state = state.get(atom.depends_on)
            if parent_state is None:
                raise ValueError(f"unknown dependency: {atom.depends_on}")
            if parent_state != READY:
                state[atom.atom_id] = DEPENDENCY_HOLD
                action[atom.atom_id] = f"WAIT_FOR_DEPENDENCY:{atom.depends_on}"
                changed = True

    waves: dict[str, int] = {}
    unresolved = {atom.atom_id for atom in atoms if state[atom.atom_id] == READY}
    while unresolved:
        progress = False
        for atom in atoms:
            if atom.atom_id not in unresolved:
                continue
            if not atom.depends_on:
                waves[atom.atom_id] = 0
                unresolved.remove(atom.atom_id)
                progress = True
                continue
            if atom.depends_on in waves:
                waves[atom.atom_id] = waves[atom.depends_on] + 1
                unresolved.remove(atom.atom_id)
                progress = True
        if not progress:
            raise ValueError(f"unresolved ready dependency cycle: {sorted(unresolved)}")

    result: list[WaveItem] = []
    for atom in atoms:
        safe = _safe(atom.atom_id)
        result.append(
            WaveItem(
                order=atom.order,
                work_id=atom.work_id,
                atom_id=atom.atom_id,
                phase=atom.phase,
                scope=atom.scope,
                proof_lane=atom.proof_lane,
                recipe_ref=atom.recipe_ref,
                depends_on=atom.depends_on,
                execution_state=state[atom.atom_id],
                wave=waves.get(atom.atom_id, -1),
                scope_approved=_scope_approval(atom, approvals),
                next_action=action[atom.atom_id],
                camel_route_id=f"m3.backport.{safe}",
                airflow_task_id=f"m3_backport_{safe}",
                drools_fact_id=f"M3_BACKPORT_{safe.upper()}",
            )
        )
    return result


def root(items: Sequence[WaveItem]) -> str:
    payload = "".join(
        "".join(
            (
                str(item.order),
                item.work_id,
                item.atom_id,
                item.phase,
                item.scope,
                item.proof_lane,
                item.recipe_ref,
                item.depends_on,
                item.execution_state,
                str(item.wave),
                str(item.scope_approved).lower(),
                item.next_action,
                item.camel_route_id,
                item.airflow_task_id,
                item.drools_fact_id,
            )
        )
        + ""
        for item in items
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def summary(items: Sequence[WaveItem], approvals: Mapping[str, str]) -> dict[str, object]:
    counts: dict[str, int] = {}
    for item in items:
        counts[item.execution_state] = counts.get(item.execution_state, 0) + 1
    ready_waves = sorted({item.wave for item in items if item.wave >= 0})
    digest = root(items)
    return {
        "schema": "M3_BACKPORT_EXECUTION_WAVES_V1",
        "root": digest,
        "atoms": len(items),
        "work_items": len({item.work_id for item in items}),
        "states": dict(sorted(counts.items())),
        "ready_waves": ready_waves,
        "scope_approvals": len(approvals),
        "mutation_authority": False,
        "promotion_authority": False,
        "compatibility_authority": False,
        "scheduler_projections": {
            "camel": {
                "authority": False,
                "route_field": "camel_route_id",
                "semantics": "dependency/readiness projection only",
            },
            "airflow": {
                "authority": False,
                "task_field": "airflow_task_id",
                "semantics": "dependency/readiness projection only",
            },
            "drools": {
                "authority": False,
                "fact_field": "drools_fact_id",
                "semantics": "eligibility/rule projection only",
            },
        },
    }


def write_tsv(items: Sequence[WaveItem], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=list(asdict(items[0]).keys()),
            delimiter="	",
            lineterminator="\n",
        )
        writer.writeheader()
        for item in items:
            writer.writerow(asdict(item))


def write_json(items: Sequence[WaveItem], approvals: Mapping[str, str], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = summary(items, approvals)
    payload["items"] = [asdict(item) for item in items]
    path.write_text(json.dumps(payload, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def compile_root(root_path: Path, approvals: Mapping[str, str]) -> list[WaveItem]:
    dag = _load_dag_module(root_path)
    atoms = dag.compile_root(root_path)
    dag.validate(atoms)
    unknown = sorted(set(approvals) - {atom.atom_id for atom in atoms})
    if unknown:
        raise ValueError(f"scope approvals reference unknown atoms: {unknown}")
    return plan(atoms, approvals)


def _args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--approvals", type=Path)
    parser.add_argument("--tsv-out", type=Path, required=True)
    parser.add_argument("--json-out", type=Path, required=True)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _args(argv)
    root_path = args.root.resolve()
    approvals = read_approvals(args.approvals)
    items = compile_root(root_path, approvals)
    write_tsv(items, args.tsv_out)
    write_json(items, approvals, args.json_out)
    payload = summary(items, approvals)
    print(
        "PASS: "
        f"{payload['work_items']} work items, "
        f"{payload['atoms']} atoms, "
        f"waves={payload['ready_waves']}, "
        f"root={payload['root']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
