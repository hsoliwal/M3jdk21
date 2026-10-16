#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Compile JEP and non-JEP JBS/tooling work into one fail-closed execution-status table."""

from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import json
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Sequence

FILE_READY = "FILE_READY"
SCOPE_READY = "SCOPE_READY"
AUTHOR_RECIPE = "AUTHOR_RECIPE"
EVIDENCE_HOLD = "EVIDENCE_HOLD"
SCOPE_HOLD = "SCOPE_HOLD"
HOLD = "HOLD"
REJECT = "REJECT"


@dataclass(frozen=True)
class WorkRow:
    order: int
    work_id: str
    source_kind: str
    release: int
    jep: int
    title: str
    recipe_state: str
    recipe_refs: str
    atom_count: int
    ready_atoms: int
    held_atoms: int
    programme_state: str
    next_action: str


def _load(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def _rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError(f"empty authority: {path}")
    return rows


def _jep_rows(root: Path) -> list[WorkRow]:
    backports = root / "m3" / "backports"
    module = _load(backports / "backport_programme_status.py", "m3_jep_programme_status")
    rows = module.compile_root(root)
    return [
        WorkRow(
            order=0,
            work_id=f"JEP-{row.jep}",
            source_kind="JEP",
            release=row.release,
            jep=row.jep,
            title=row.title,
            recipe_state=row.recipe_state,
            recipe_refs=row.recipe_refs,
            atom_count=row.atom_count,
            ready_atoms=row.ready_atoms,
            held_atoms=row.held_atoms,
            programme_state=row.programme_state,
            next_action=row.next_action,
        )
        for row in rows
    ]


def _seed_rows(root: Path) -> list[WorkRow]:
    backports = root / "m3" / "backports"
    dag = _load(backports / "backport_recipe_dag.py", "m3_whole_status_dag")
    waves = _load(backports / "backport_execution_waves.py", "m3_whole_status_waves")
    atoms = dag.compile_root(root)
    dag.validate(atoms)
    items = waves.plan(atoms, {})

    atoms_by_work: dict[str, list[object]] = {}
    waves_by_work: dict[str, list[object]] = {}
    for atom in atoms:
        if atom.source_kind == "UPSTREAM_CHANGE":
            atoms_by_work.setdefault(atom.work_id, []).append(atom)
    for item in items:
        if item.work_id in atoms_by_work:
            waves_by_work.setdefault(item.work_id, []).append(item)

    result: list[WorkRow] = []
    for seed in _rows(backports / "UPSTREAM_CHANGE_SEEDS.tsv"):
        work_id = seed["jbs"].strip()
        work_atoms = atoms_by_work.get(work_id, [])
        work_waves = waves_by_work.get(work_id, [])
        if not work_atoms or len(work_atoms) != len(work_waves):
            raise ValueError(f"JBS atom/wave alignment failure: {work_id}")
        if {atom.atom_id for atom in work_atoms} != {item.atom_id for item in work_waves}:
            raise ValueError(f"JBS atom/wave identity mismatch: {work_id}")

        recipe_state = work_atoms[0].state
        if any(atom.state != recipe_state for atom in work_atoms):
            raise ValueError(f"mixed JBS recipe states: {work_id}")
        recipe_refs = work_atoms[0].recipe_ref
        ready = sum(item.execution_state == "READY" for item in work_waves)
        held = len(work_waves) - ready
        disposition = seed["disposition"].strip().lower()

        if disposition.startswith("reject"):
            state = REJECT
            next_action = "KEEP_JAVA21_COMPATIBILITY_BASELINE"
        elif disposition.startswith("hold"):
            state = HOLD
            next_action = "WAIT_FOR_DEPENDENCY_OR_COMPATIBILITY_DECISION"
        elif recipe_state == "AUTHOR_RECIPE":
            state = AUTHOR_RECIPE
            next_action = "AUTHOR_TEST_AND_FIXED_POINT_RECIPE"
        elif any(item.execution_state == "SCOPE_APPROVAL_HOLD" for item in work_waves):
            state = SCOPE_HOLD
            next_action = "REVIEW_SCOPE_PROMOTION_AND_RECORD_PROOF"
        elif any(item.execution_state in {"EVIDENCE_HOLD", "DEPENDENCY_HOLD"} for item in work_waves):
            state = EVIDENCE_HOLD
            next_action = "REPAIR_JBS_EVIDENCE_OR_DEPENDENCY"
        elif ready == len(work_atoms):
            state = FILE_READY if all(atom.scope == "FILE" for atom in work_atoms) else SCOPE_READY
            next_action = (
                "EXECUTE_READY_JBS_ATOMS_AND_PROVE_JAVA21_CONTRACT"
                if state == FILE_READY
                else "EXECUTE_SCOPE_APPROVED_JBS_ATOMS_AND_PROVE_JAVA21_CONTRACT"
            )
        else:
            state = EVIDENCE_HOLD
            next_action = "REPAIR_JBS_DAG_WAVE_ALIGNMENT"

        result.append(
            WorkRow(
                order=0,
                work_id=work_id,
                source_kind="UPSTREAM_CHANGE",
                release=int(seed["release"]),
                jep=0,
                title=seed["title"].strip(),
                recipe_state=recipe_state,
                recipe_refs=recipe_refs,
                atom_count=len(work_atoms),
                ready_atoms=ready,
                held_atoms=held,
                programme_state=state,
                next_action=next_action,
            )
        )
    return result


def compile_root(root: Path) -> list[WorkRow]:
    rows = _jep_rows(root) + _seed_rows(root)
    rows.sort(key=lambda row: (row.release, row.source_kind, row.work_id))
    work_ids = [row.work_id for row in rows]
    if len(set(work_ids)) != len(work_ids):
        raise ValueError("duplicate whole-programme work_id")
    return [
        WorkRow(
            order=index,
            work_id=row.work_id,
            source_kind=row.source_kind,
            release=row.release,
            jep=row.jep,
            title=row.title,
            recipe_state=row.recipe_state,
            recipe_refs=row.recipe_refs,
            atom_count=row.atom_count,
            ready_atoms=row.ready_atoms,
            held_atoms=row.held_atoms,
            programme_state=row.programme_state,
            next_action=row.next_action,
        )
        for index, row in enumerate(rows)
    ]


def root(rows: Sequence[WorkRow]) -> str:
    payload = "".join(
        "\u001f".join(str(value) for value in asdict(row).values()) + "\u001e"
        for row in rows
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def summary(rows: Sequence[WorkRow]) -> dict[str, object]:
    states: dict[str, int] = {}
    sources: dict[str, int] = {}
    for row in rows:
        states[row.programme_state] = states.get(row.programme_state, 0) + 1
        sources[row.source_kind] = sources.get(row.source_kind, 0) + 1
    return {
        "schema": "M3_JDK21_WHOLE_BACKPORT_PROGRAMME_STATUS_V1",
        "root": root(rows),
        "work_items": len(rows),
        "states": dict(sorted(states.items())),
        "sources": dict(sorted(sources.items())),
        "product_write_authority": False,
        "compatibility_authority": False,
        "promotion_authority": False,
    }


def write_tsv(rows: Sequence[WorkRow], path: Path) -> None:
    if not rows:
        raise ValueError("empty whole-programme status")
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


def write_json(rows: Sequence[WorkRow], path: Path) -> None:
    payload = summary(rows)
    payload["rows"] = [asdict(row) for row in rows]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--tsv-out", type=Path, required=True)
    parser.add_argument("--json-out", type=Path, required=True)
    args = parser.parse_args(argv)

    rows = compile_root(args.root.resolve())
    write_tsv(rows, args.tsv_out)
    write_json(rows, args.json_out)
    payload = summary(rows)
    print(
        "PASS: "
        f"{payload['work_items']} whole-programme work items, "
        f"sources={payload['sources']}, states={payload['states']}, root={payload['root']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
