#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Compile existing M3JDK21 backport authorities into a deterministic recipe/proof DAG.

This module does not decide compatibility. JEP_CATALOGUE.tsv, UPSTREAM_CHANGE_SEEDS.tsv and
COMMUNITY_CAPABILITY_CANDIDATES.tsv remain the authorities for disposition. The compiler only
projects those decisions plus the physically present m3/backports/recipes directories into small
ordered work atoms suitable for Maven/OpenRewrite/Camel-style orchestration.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

PHASES = (
    "INVENTORY",
    "RECIPE",
    "COMPILE",
    "TEST",
    "RUNTIME_PARITY",
    "FIXED_POINT",
)

EXECUTABLE_STATES = {"MATERIALIZED_RECIPE", "MATERIALIZED_RECIPE_SET", "AUTHOR_RECIPE"}


@dataclass(frozen=True)
class Atom:
    order: int
    work_id: str
    source_kind: str
    title: str
    state: str
    recipe_ref: str
    atom_id: str
    phase: str
    scope: str
    executable: bool
    proof_lane: str
    depends_on: str
    reason: str


def _rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError(f"empty authority: {path}")
    return rows


def _recipe_refs(recipes: Path, work_id: str) -> tuple[str, ...]:
    if not recipes.is_dir():
        return ()
    token = work_id.lower()
    if token.startswith("jep-"):
        number = token.removeprefix("jep-")
        preferred = sorted(
            child.name
            for child in recipes.iterdir()
            if child.is_dir() and child.name.startswith(f"jep-{number}-")
        )
        if preferred:
            return tuple(preferred)
        short = recipes / f"j{number}"
        return (short.name,) if short.is_dir() else ()
    if token.startswith("jdk-"):
        matches = sorted(
            child.name
            for child in recipes.iterdir()
            if child.is_dir() and child.name.startswith(token)
        )
        return tuple(matches)
    return ()


def _state(disposition: str, recipe_refs: Sequence[str]) -> str:
    value = disposition.strip().lower()
    if value.startswith("superseded"):
        return "SUPERSEDED"
    if value.startswith("reject"):
        return "REJECT"
    if value.startswith("hold"):
        return "HOLD"
    if value == "admitted" or value.startswith("candidate"):
        if len(recipe_refs) > 1:
            return "MATERIALIZED_RECIPE_SET"
        if len(recipe_refs) == 1:
            return "MATERIALIZED_RECIPE"
        return "AUTHOR_RECIPE"
    return "REVIEW_ONLY"


def _scope(domain: str) -> str:
    value = domain.lower()
    if any(
        token in value
        for token in (
            "hotspot",
            "gc",
            "vm",
            "runtime",
            "native",
            "compiler",
            "network",
            "jfr",
            "aot",
        )
    ):
        return "MULTI_MODULE"
    if any(
        token in value
        for token in (
            "library",
            "security",
            "tool",
            "javadoc",
            "javac",
            "jlink",
            "launcher",
        )
    ):
        return "MODULE"
    return "FILE"


def _proof_lane(disposition: str, domain: str) -> str:
    value = disposition.lower()
    if "high-risk" in value:
        return "HIGH_RISK_RUNTIME"
    if "adapted" in value or value == "admitted":
        return "ADAPTED_CURRENT_TREE"
    if any(token in domain.lower() for token in ("hotspot", "gc", "vm", "native")):
        return "VM_NATIVE"
    return "STANDARD_JAVA21"


def _runtime_required(disposition: str, domain: str) -> bool:
    return "high-risk" in disposition.lower() or any(
        token in domain.lower()
        for token in ("hotspot", "gc", "vm", "runtime", "native", "jfr", "network", "aot")
    )


def _candidate_atoms(
    *,
    work_id: str,
    source_kind: str,
    title: str,
    disposition: str,
    domain: str,
    reason: str,
    recipe_refs: Sequence[str],
) -> list[tuple[str, str, str, bool, str, str]]:
    state = _state(disposition, recipe_refs)
    recipe_ref = ";".join(recipe_refs)
    scope = _scope(domain)
    lane = _proof_lane(disposition, domain)

    if state not in EXECUTABLE_STATES:
        return [("INVENTORY", scope, state, False, "", reason)]

    phases = ["INVENTORY", "RECIPE", "COMPILE", "TEST"]
    if _runtime_required(disposition, domain):
        phases.append("RUNTIME_PARITY")
    phases.append("FIXED_POINT")

    result: list[tuple[str, str, str, bool, str, str]] = []
    previous = ""
    for phase in phases:
        atom_id = f"{work_id.lower().replace('-', '_')}.{phase.lower()}"
        phase_reason = reason
        if phase == "RECIPE" and state == "AUTHOR_RECIPE":
            phase_reason = "No checked-in recipe packet exists; author/test source-pinned recipe first."
        elif phase == "RECIPE":
            phase_reason = "Reuse checked-in recipe packet(s); do not duplicate implementation ownership."
        result.append((phase, scope, state, True, previous, phase_reason))
        previous = atom_id
    return result


def compile_jep_rows(
    rows: Iterable[Mapping[str, str]], recipes: Path
) -> list[Atom]:
    atoms: list[Atom] = []
    order = 0
    for row in rows:
        work_id = f"JEP-{row['jep']}"
        refs = _recipe_refs(recipes, work_id)
        for phase, scope, state, executable, depends_on, atom_reason in _candidate_atoms(
            work_id=work_id,
            source_kind="JEP",
            title=row["title"],
            disposition=row["disposition"],
            domain=row["domain"],
            reason=row["reason"],
            recipe_refs=refs,
        ):
            atom_id = f"{work_id.lower().replace('-', '_')}.{phase.lower()}"
            atoms.append(
                Atom(
                    order=order,
                    work_id=work_id,
                    source_kind="JEP",
                    title=row["title"],
                    state=state,
                    recipe_ref=";".join(refs),
                    atom_id=atom_id,
                    phase=phase,
                    scope=scope,
                    executable=executable,
                    proof_lane=_proof_lane(row["disposition"], row["domain"]),
                    depends_on=depends_on,
                    reason=atom_reason,
                )
            )
            order += 1
    return atoms


def compile_seed_rows(
    rows: Iterable[Mapping[str, str]], recipes: Path, start_order: int
) -> list[Atom]:
    atoms: list[Atom] = []
    order = start_order
    for row in rows:
        work_id = row["jbs"]
        refs = _recipe_refs(recipes, work_id)
        for phase, scope, state, executable, depends_on, atom_reason in _candidate_atoms(
            work_id=work_id,
            source_kind="UPSTREAM_CHANGE",
            title=row["title"],
            disposition=row["disposition"],
            domain=row["component"],
            reason=row["reason"],
            recipe_refs=refs,
        ):
            atom_id = f"{work_id.lower().replace('-', '_')}.{phase.lower()}"
            atoms.append(
                Atom(
                    order=order,
                    work_id=work_id,
                    source_kind="UPSTREAM_CHANGE",
                    title=row["title"],
                    state=state,
                    recipe_ref=";".join(refs),
                    atom_id=atom_id,
                    phase=phase,
                    scope=_scope(row["component"]),
                    executable=executable,
                    proof_lane=_proof_lane(row["disposition"], row["component"]),
                    depends_on=depends_on,
                    reason=atom_reason,
                )
            )
            order += 1
    return atoms


def compile_community_rows(
    rows: Iterable[Mapping[str, str]], start_order: int
) -> list[Atom]:
    atoms: list[Atom] = []
    order = start_order
    for row in rows:
        work_id = row["capability_id"]
        reason = row["next_proof"]
        atoms.append(
            Atom(
                order=order,
                work_id=work_id,
                source_kind="COMMUNITY_CAPABILITY",
                title=row["candidate"],
                state=row["status"],
                recipe_ref="",
                atom_id=f"{work_id.lower().replace('-', '_')}.inventory",
                phase="INVENTORY",
                scope="MULTI_MODULE" if row["packaging_candidate"].startswith("JDK_") else "MODULE",
                executable=False,
                proof_lane="COMMUNITY_REVIEW",
                depends_on="",
                reason=reason,
            )
        )
        order += 1
    return atoms


def compile_root(root: Path) -> list[Atom]:
    backports = root / "m3" / "backports"
    recipes = backports / "recipes"
    atoms = compile_jep_rows(_rows(backports / "JEP_CATALOGUE.tsv"), recipes)
    seeds = compile_seed_rows(
        _rows(backports / "UPSTREAM_CHANGE_SEEDS.tsv"), recipes, len(atoms)
    )
    atoms.extend(seeds)
    community = compile_community_rows(
        _rows(backports / "COMMUNITY_CAPABILITY_CANDIDATES.tsv"), len(atoms)
    )
    atoms.extend(community)
    return atoms


def canonical_root(atoms: Sequence[Atom]) -> str:
    payload = "\n".join(
        "\t".join(
            (
                str(atom.order),
                atom.work_id,
                atom.source_kind,
                atom.title,
                atom.state,
                atom.recipe_ref,
                atom.atom_id,
                atom.phase,
                atom.scope,
                str(atom.executable).lower(),
                atom.proof_lane,
                atom.depends_on,
                atom.reason,
            )
        )
        for atom in atoms
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def validate(atoms: Sequence[Atom]) -> None:
    if not atoms:
        raise ValueError("empty backport DAG")
    if [atom.order for atom in atoms] != list(range(len(atoms))):
        raise ValueError("non-contiguous atom order")

    seen: set[str] = set()
    by_id: dict[str, Atom] = {}
    for atom in atoms:
        if atom.atom_id in seen:
            raise ValueError(f"duplicate atom id: {atom.atom_id}")
        seen.add(atom.atom_id)
        by_id[atom.atom_id] = atom
        if atom.phase not in PHASES:
            raise ValueError(f"unknown phase: {atom.phase}")
        if atom.executable and atom.state not in EXECUTABLE_STATES:
            raise ValueError(f"non-admitted state marked executable: {atom.work_id}")
        if not atom.executable and atom.depends_on:
            raise ValueError(f"non-executable atom has dependency: {atom.atom_id}")

    phase_index = {phase: index for index, phase in enumerate(PHASES)}
    for atom in atoms:
        if not atom.depends_on:
            continue
        parent = by_id.get(atom.depends_on)
        if parent is None:
            raise ValueError(f"unknown dependency: {atom.depends_on}")
        if parent.work_id != atom.work_id:
            raise ValueError("cross-work-item dependency requires an explicit dependency authority")
        if phase_index[parent.phase] >= phase_index[atom.phase]:
            raise ValueError("phase order regression")


def _packet_id(work_id: str) -> str:
    value = work_id.lower().replace("_", "-")
    if value.startswith("jep-") or value.startswith("jdk-"):
        checked = value
    else:
        checked = "m3-" + value
    if not checked or len(checked) > 80 or not all(
        ch.islower() or ch.isdigit() or ch == "-" for ch in checked
    ):
        raise ValueError(f"invalid packet id derived from {work_id!r}: {checked!r}")
    if not checked[0].isalnum():
        raise ValueError(f"invalid packet id: {checked!r}")
    return checked


def _local_atom_id(phase: str) -> str:
    return phase.lower().replace("_", "-")


def _work_ref(atom: Atom) -> str:
    if atom.phase == "RECIPE":
        if atom.state.startswith("MATERIALIZED_RECIPE"):
            return "m3/backports/recipes/" + atom.recipe_ref
        return "AUTHOR_RECIPE:" + atom.work_id
    return "PROOF:" + atom.work_id + ":" + atom.phase


def write_packets(atoms: Sequence[Atom], directory: Path) -> list[tuple[str, str, Path]]:
    directory.mkdir(parents=True, exist_ok=True)
    grouped: dict[str, list[Atom]] = {}
    for atom in atoms:
        if atom.executable:
            grouped.setdefault(atom.work_id, []).append(atom)

    index: list[tuple[str, str, Path]] = []
    header = (
        "packet_id\tatom_id\tscope\tscope_promotion_approved\t"
        "work_ref\tdepends_on\n"
    )
    for work_id in sorted(grouped):
        rows = sorted(grouped[work_id], key=lambda atom: atom.order)
        packet_id = _packet_id(work_id)
        local_ids = {atom.atom_id: _local_atom_id(atom.phase) for atom in rows}
        lines = [header]
        for atom in rows:
            dependency = (
                ""
                if not atom.depends_on
                else local_ids[atom.depends_on]
            )
            approved = atom.scope != "FILE"
            lines.append(
                "\t".join(
                    (
                        packet_id,
                        local_ids[atom.atom_id],
                        atom.scope,
                        str(approved).lower(),
                        _work_ref(atom),
                        dependency,
                    )
                )
                + "\n"
            )
        path = directory / f"{packet_id}.tsv"
        path.write_text("".join(lines), encoding="utf-8")
        index.append((work_id, rows[0].state, path))
    return index


def write_packet_index(
    packets: Sequence[tuple[str, str, Path]], path: Path
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("work_id", "state", "packet"))
        for work_id, state, packet in packets:
            writer.writerow((work_id, state, packet.name))


def write_tsv(atoms: Sequence[Atom], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=list(asdict(atoms[0]).keys()),
            delimiter="\t",
            lineterminator="\n",
        )
        writer.writeheader()
        for atom in atoms:
            writer.writerow(asdict(atom))


def summary(atoms: Sequence[Atom]) -> dict[str, object]:
    states: dict[str, int] = {}
    sources: dict[str, int] = {}
    executable = 0
    for atom in atoms:
        states[atom.state] = states.get(atom.state, 0) + 1
        sources[atom.source_kind] = sources.get(atom.source_kind, 0) + 1
        executable += int(atom.executable)
    work_items = sorted({atom.work_id for atom in atoms})
    return {
        "schema": "M3_BACKPORT_RECIPE_DAG_V1",
        "root": canonical_root(atoms),
        "atoms": len(atoms),
        "work_items": len(work_items),
        "executable_atoms": executable,
        "states": dict(sorted(states.items())),
        "sources": dict(sorted(sources.items())),
    }


def _args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root", type=Path, default=Path(__file__).resolve().parents[2]
    )
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--summary", type=Path, required=True)
    parser.add_argument("--packet-dir", type=Path)
    parser.add_argument("--packet-index", type=Path)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _args(argv)
    atoms = compile_root(args.root.resolve())
    validate(atoms)
    write_tsv(atoms, args.out)
    payload = summary(atoms)
    args.summary.parent.mkdir(parents=True, exist_ok=True)
    args.summary.write_text(
        json.dumps(payload, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    if args.packet_dir is not None:
        packets = write_packets(atoms, args.packet_dir)
        if args.packet_index is not None:
            write_packet_index(packets, args.packet_index)
        payload["packets"] = len(packets)
        args.summary.write_text(
            json.dumps(payload, sort_keys=True, indent=2) + "\n", encoding="utf-8"
        )
    print(
        f"PASS: {payload['work_items']} work items, {payload['atoms']} atoms, "
        f"{payload['executable_atoms']} executable atoms, root={payload['root']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
