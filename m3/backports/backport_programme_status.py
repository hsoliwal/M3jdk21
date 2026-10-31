#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Compile M3JDK21 JEP catalogue + recipe DAG/waves into one conservative status ledger."""

from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import json
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

AUTHOR_RECIPE = "AUTHOR_RECIPE"
FILE_READY = "FILE_READY"
SCOPE_READY = "SCOPE_READY"
SCOPE_HOLD = "SCOPE_HOLD"
EVIDENCE_HOLD = "EVIDENCE_HOLD"
HOLD = "HOLD"
REJECT = "REJECT"
SUPERSEDED = "SUPERSEDED"
VALID_WAVE_STATES = {"READY", "EVIDENCE_HOLD", "SCOPE_APPROVAL_HOLD", "DEPENDENCY_HOLD"}


@dataclass(frozen=True)
class ProgrammeRow:
    release: int
    jep: int
    title: str
    domain: str
    disposition: str
    recipe_state: str
    recipe_refs: str
    atom_count: int
    ready_atoms: int
    held_atoms: int
    programme_state: str
    next_action: str
    reason: str


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


def _aligned_wave_evidence(work_atoms: Sequence[object], work_waves: Sequence[object]) -> bool:
    """Fail closed on partial, duplicate, stale, or cross-scope wave projections.

    A ready scheduling atom is *not* compiler/test proof, but it must still be
    the exact canonical atom from the recipe DAG. Cardinality alone is insufficient.
    """
    if not work_atoms or len(work_atoms) != len(work_waves):
        return False
    atoms = {str(atom.atom_id): atom for atom in work_atoms}
    waves = {str(item.atom_id): item for item in work_waves}
    if len(atoms) != len(work_atoms) or len(waves) != len(work_waves):
        return False
    if set(atoms) != set(waves):
        return False
    for atom_id, item in waves.items():
        atom = atoms[atom_id]
        if (
            item.execution_state not in VALID_WAVE_STATES
            or item.scope != atom.scope
            or (item.execution_state == "READY" and not atom.executable)
            or (
                item.execution_state == "READY"
                and item.scope != "FILE"
                and not item.scope_approved
            )
        ):
            return False
    return True


def classify(
    catalogue_row: Mapping[str, str],
    atoms: Sequence[object],
    wave_items: Sequence[object],
) -> ProgrammeRow:
    disposition = catalogue_row["disposition"].strip().lower()
    work_id = f"JEP-{catalogue_row['jep']}"
    work_atoms = [atom for atom in atoms if atom.work_id == work_id]
    work_waves = [item for item in wave_items if item.work_id == work_id]
    recipe_refs = sorted(
        {
            value
            for atom in work_atoms
            for value in str(atom.recipe_ref).split(";")
            if value
        }
    )
    recipe_state = work_atoms[0].state if work_atoms else "MISSING_DAG"
    ready = sum(item.execution_state == "READY" for item in work_waves)
    held = len(work_waves) - ready
    aligned = _aligned_wave_evidence(work_atoms, work_waves)

    if disposition.startswith("superseded"):
        state = SUPERSEDED
        successor = catalogue_row.get("superseded_by", "").strip()
        next_action = f"FOLLOW_JEP_{successor}" if successor else "FOLLOW_SUCCESSOR_LINEAGE"
    elif disposition.startswith("reject"):
        state = REJECT
        next_action = "KEEP_JAVA21_COMPATIBILITY_BASELINE"
    elif disposition.startswith("hold"):
        state = HOLD
        next_action = "WAIT_FOR_EXPLICIT_COMPATIBILITY_OR_PREVIEW_DECISION"
    elif recipe_state == "AUTHOR_RECIPE":
        state = AUTHOR_RECIPE
        next_action = "AUTHOR_TEST_AND_FIXED_POINT_RECIPE"
    elif not aligned:
        state = EVIDENCE_HOLD
        next_action = "REPAIR_BACKPORT_DAG_WAVE_ALIGNMENT"
    elif any(item.execution_state in {"SCOPE_APPROVAL_HOLD", "DEPENDENCY_HOLD"} for item in work_waves):
        state = SCOPE_HOLD
        next_action = "REVIEW_SCOPE_PROMOTION_AND_RECORD_PROOF"
    elif any(item.execution_state == "EVIDENCE_HOLD" for item in work_waves):
        state = EVIDENCE_HOLD
        next_action = "WAIT_FOR_COMPATIBILITY_OR_RECIPE_ADMISSION"
    elif ready != len(work_atoms):
        state = EVIDENCE_HOLD
        next_action = "REPAIR_BACKPORT_DAG_WAVE_ALIGNMENT"
    elif any(atom.scope != "FILE" for atom in work_atoms):
        state = SCOPE_READY
        next_action = "EXECUTE_SCOPE_APPROVED_ATOMS_AND_PROVE_JAVA21_CONTRACT"
    else:
        state = FILE_READY
        next_action = "EXECUTE_READY_ATOMS_AND_PROVE_JAVA21_CONTRACT"

    if "high-risk" in disposition and state in {FILE_READY, SCOPE_READY}:
        next_action = "EXECUTE_READY_ATOMS_THEN_VM_RUNTIME_JNI_PARITY"

    return ProgrammeRow(
        release=int(catalogue_row["release"]),
        jep=int(catalogue_row["jep"]),
        title=catalogue_row["title"].strip(),
        domain=catalogue_row["domain"].strip(),
        disposition=catalogue_row["disposition"].strip(),
        recipe_state=recipe_state,
        recipe_refs=";".join(recipe_refs),
        atom_count=len(work_atoms),
        ready_atoms=ready,
        held_atoms=held,
        programme_state=state,
        next_action=next_action,
        reason=catalogue_row["reason"].strip(),
    )


def compile_rows(
    catalogue_rows: Iterable[Mapping[str, str]],
    atoms: Sequence[object],
    wave_items: Sequence[object],
) -> list[ProgrammeRow]:
    rows = [
        classify(row, atoms, wave_items)
        for row in catalogue_rows
        if not row["disposition"].strip().lower().startswith("superseded")
    ]
    rows.sort(key=lambda row: (row.release, row.jep))
    if len({row.jep for row in rows}) != len(rows):
        raise ValueError("duplicate active JEP")
    return rows


def root(rows: Sequence[ProgrammeRow]) -> str:
    payload = "".join(
        "\u001f".join(str(value) for value in asdict(row).values()) + "\u001e"
        for row in rows
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def summary(rows: Sequence[ProgrammeRow]) -> dict[str, object]:
    counts: dict[str, int] = {}
    for row in rows:
        counts[row.programme_state] = counts.get(row.programme_state, 0) + 1
    return {
        "schema": "M3_JDK21_BACKPORT_PROGRAMME_STATUS_V1",
        "root": root(rows),
        "jeps": len(rows),
        "states": dict(sorted(counts.items())),
        "mutation_authority": False,
        "promotion_authority": False,
        "compatibility_authority": False,
    }


def compile_root(root_path: Path) -> list[ProgrammeRow]:
    backports = root_path / "m3" / "backports"
    dag = _load(backports / "backport_recipe_dag.py", "m3_backport_programme_dag")
    waves = _load(backports / "backport_execution_waves.py", "m3_backport_programme_waves")
    atoms = dag.compile_root(root_path)
    dag.validate(atoms)
    wave_items = waves.plan(atoms, {})
    return compile_rows(_rows(backports / "JEP_CATALOGUE.tsv"), atoms, wave_items)


def write_tsv(rows: Sequence[ProgrammeRow], path: Path) -> None:
    if not rows:
        raise ValueError("empty programme status")
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


def write_json(rows: Sequence[ProgrammeRow], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = summary(rows)
    payload["rows"] = [asdict(row) for row in rows]
    path.write_text(json.dumps(payload, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def _args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--tsv-out", type=Path, required=True)
    parser.add_argument("--json-out", type=Path, required=True)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _args(argv)
    rows = compile_root(args.root.resolve())
    write_tsv(rows, args.tsv_out)
    write_json(rows, args.json_out)
    payload = summary(rows)
    print(
        "PASS: "
        f"{payload['jeps']} active JEPs, "
        f"states={payload['states']}, "
        f"root={payload['root']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
