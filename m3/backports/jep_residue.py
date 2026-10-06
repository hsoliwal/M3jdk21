#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Deterministic released-JEP proof/implementation residue queue for M3JDK21.

Catalogue disposition is evidence, not acceptance. This queue never upgrades a candidate to
compatible. It only joins the released denominator to repository-owned recipe/packet evidence and
orders the remaining proof work.
"""

from __future__ import annotations

import argparse
import csv
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

PENDING = {
    "candidate",
    "candidate-high-risk",
    "hold-compat",
    "hold-jit",
    "hold-preview",
}

PRIORITY = {
    "candidate": 20,
    "candidate-high-risk": 40,
    "hold-compat": 60,
    "hold-preview": 70,
    "hold-jit": 80,
}

ACTION = {
    "candidate": "PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT",
    "candidate-high-risk": "CLOSE_DEPENDENCY_AND_HIGH_RISK_PROOF",
    "hold-compat": "CLOSE_JAVA21_COMPATIBILITY_POLICY",
    "hold-preview": "RETAIN_RESEARCH_ONLY_UNTIL_STABLE_OR_EXPLICIT_OPT_IN",
    "hold-jit": "CLOSE_HOTSPOT_JIT_DEPENDENCY_PROOF",
}


@dataclass(frozen=True)
class Residue:
    order: int
    release: int
    jep: int
    title: str
    domain: str
    disposition: str
    priority: int
    evidence_state: str
    evidence_paths: str
    next_action: str
    default_java21: str
    priority_classification: str
    receipt_state: str
    promotion: str
    receipt_next_action: str


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


def _receipt(packet_dir: Path) -> dict[str, str]:
    path = packet_dir / "CURRENT_TREE_RECEIPT.tsv"
    if not path.is_file():
        return {}
    rows = read_tsv(path)
    return {
        row.get("field", ""): row.get("value", "")
        for row in rows
        if row.get("field", "")
    }


def _recipe_evidence(root: Path, jep: int) -> tuple[str, tuple[str, ...], dict[str, str]]:
    evidence: list[str] = []
    receipt: dict[str, str] = {}

    recipes = root / "m3" / "backports" / "recipes"
    if recipes.is_dir():
        prefix = f"jep-{jep}"
        for candidate in sorted(recipes.iterdir()):
            if candidate.is_dir() and candidate.name.startswith(prefix):
                evidence.append(candidate.relative_to(root).as_posix())
                candidate_receipt = _receipt(candidate)
                if candidate_receipt:
                    if receipt and receipt != candidate_receipt:
                        raise ValueError(f"conflicting current-tree receipts for JEP {jep}")
                    receipt = candidate_receipt

    recipe_java = (
        root
        / "m3"
        / "tooling"
        / "migration-recipes"
        / "src"
        / "main"
        / "java"
        / "com"
        / "m3"
        / "rewrite"
        / "backport"
    )
    if recipe_java.is_dir():
        needle = f"jep{jep}".lower()
        for candidate in sorted(recipe_java.glob("*.java")):
            if needle in candidate.name.lower():
                evidence.append(candidate.relative_to(root).as_posix())

    if any(path.startswith("m3/backports/recipes/") for path in evidence):
        state = "MATERIALIZED_PACKET"
    elif evidence:
        state = "RECIPE_CLASS"
    else:
        state = "NO_RECIPE_EVIDENCE"
    return state, tuple(evidence), receipt


def queue(
    root: Path,
    catalogue: Iterable[Mapping[str, str]],
    priority_rows: Iterable[Mapping[str, str]],
) -> list[Residue]:
    root = root.resolve()
    priority_by_jep = {int(row["jep"]): row for row in priority_rows}
    pending: list[tuple[int, int, int, Mapping[str, str], str, tuple[str, ...]]] = []

    seen: set[int] = set()
    for row in catalogue:
        jep = int(row["jep"])
        if jep in seen:
            raise ValueError(f"duplicate JEP row: {jep}")
        seen.add(jep)

        disposition = row["disposition"]
        if disposition not in PENDING:
            continue
        state, evidence, receipt = _recipe_evidence(root, jep)
        pending.append(
            (
                PRIORITY[disposition],
                int(row["release"]),
                jep,
                row,
                state,
                evidence,
                receipt,
            )
        )

    pending.sort(key=lambda item: (item[0], item[1], item[2]))
    result: list[Residue] = []
    for order, (priority, release, jep, row, state, evidence, receipt) in enumerate(pending):
        priority_row = priority_by_jep.get(jep, {})
        result.append(
            Residue(
                order=order,
                release=release,
                jep=jep,
                title=row["title"],
                domain=row["domain"],
                disposition=row["disposition"],
                priority=priority,
                evidence_state=state,
                evidence_paths=",".join(evidence),
                next_action=ACTION[row["disposition"]],
                default_java21=priority_row.get("default_java21", ""),
                priority_classification=priority_row.get("classification", ""),
                receipt_state=(
                    receipt.get("current_tree_state")
                    or receipt.get("packet_state")
                    or ""
                ),
                promotion=receipt.get("promotion", ""),
                receipt_next_action=receipt.get("next_action", ""),
            )
        )
    return result


def write_tsv(items: Sequence[Residue], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow(
        (
            "order",
            "release",
            "jep",
            "title",
            "domain",
            "disposition",
            "priority",
            "evidence_state",
            "evidence_paths",
            "next_action",
            "default_java21",
            "priority_classification",
            "receipt_state",
            "promotion",
            "receipt_next_action",
        )
    )
    for item in items:
        writer.writerow(
            (
                item.order,
                item.release,
                item.jep,
                item.title,
                item.domain,
                item.disposition,
                item.priority,
                item.evidence_state,
                item.evidence_paths,
                item.next_action,
                item.default_java21,
                item.priority_classification,
                item.receipt_state,
                item.promotion,
                item.receipt_next_action,
            )
        )


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--out", type=Path)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    root = args.root.resolve()
    backports = root / "m3" / "backports"
    items = queue(
        root,
        read_tsv(backports / "JEP_CATALOGUE.tsv"),
        read_tsv(backports / "POST21_PRIORITY_COMPATIBILITY.tsv"),
    )
    if args.out is None:
        write_tsv(items, sys.stdout)
    else:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        with args.out.open("w", encoding="utf-8", newline="") as handle:
            write_tsv(items, handle)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
