#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Turn the complete upstream inventory into a deterministic Java-21 compatibility work queue.

This is planning evidence only. Path/domain heuristics never accept/reject semantic compatibility.
Every row remains PENDING_COMPATIBILITY_PROOF until its dedicated proof packet succeeds.
"""

from __future__ import annotations

import argparse
import csv
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

PENDING = "PENDING_COMPATIBILITY_PROOF"


@dataclass(frozen=True)
class QueueItem:
    order: int
    release: int
    commit: str
    jbs_ids: str
    subject: str
    domain: str
    inventory_disposition: str
    risk: str
    scope_floor: str
    proof_lane: str
    recipe_strategy: str
    priority: int
    compatibility_state: str
    next_action: str
    paths: str


def _truth(value: str) -> bool:
    normalized = value.strip().lower()
    if normalized == "true":
        return True
    if normalized == "false":
        return False
    raise ValueError(f"invalid boolean: {value!r}")


def _split_paths(value: str) -> tuple[str, ...]:
    return tuple(path for path in value.split(",") if path)


def _module(path: str) -> str:
    if path.startswith("src/"):
        parts = path.split("/")
        return parts[1] if len(parts) > 2 else "<src>"
    if path.startswith("make/"):
        parts = path.split("/")
        if len(parts) > 2 and parts[1] == "modules":
            return parts[2]
        return "<build>"
    if path.startswith("test/"):
        return "<tests>"
    return "<other>"


def _scope_floor(paths: Sequence[str]) -> str:
    if not paths:
        return "FILE"
    if len(paths) == 1:
        return "FILE"
    modules = {_module(path) for path in paths}
    if len(modules) > 1:
        return "MULTI_MODULE"
    return "MODULE"


def _recipe_strategy(paths: Sequence[str]) -> str:
    java = sum(path.endswith(".java") for path in paths)
    non_java = len(paths) - java
    if java and non_java:
        return "MIXED_PACKET_OPENREWRITE_PLUS_VERBATIM"
    if java:
        return "OPENREWRITE_OR_HASH_PINNED_JAVA"
    return "HASH_PINNED_VERBATIM_PATCH"


def _classification(row: Mapping[str, str]) -> tuple[str, str, str, int, str]:
    grammar = _truth(row["grammar_touch"])
    javac = _truth(row["javac_touch"])
    hotspot_compiler = _truth(row["hotspot_compiler_touch"])
    compat = _truth(row["compatibility_signal"])
    domain = row["domain"]

    if grammar:
        return (
            "CRITICAL",
            "LANGUAGE_SPLIT_REVIEW",
            "REVIEW_SPLIT_LANGUAGE_FROM_COMPATIBLE_LEAVES",
            90,
            "Language/parser/type-system touch is a hold signal, not automatic rejection.",
        )
    if compat:
        return (
            "CRITICAL",
            "JAVA21_COMPATIBILITY_REVIEW",
            "REVIEW_JAVA21_BEHAVIORAL_COMPATIBILITY",
            85,
            "Removal/deprecation/restriction signal requires explicit Java-21 compatibility proof.",
        )
    if javac:
        return (
            "HIGH",
            "JAVAC_JAVA21",
            "PROVE_JAVAC_FIX_UNDER_SOURCE_21",
            60,
            "Compiler touch may still be compatible; prove it under the locked Java-21 contract.",
        )
    if hotspot_compiler:
        return (
            "HIGH",
            "HOTSPOT_COMPILER",
            "PROVE_HOTSPOT_COMPILER_RUNTIME_PARITY",
            55,
            "JIT/compiler-runtime change requires focused VM/compiler proof.",
        )
    if domain == "hotspot":
        return (
            "HIGH",
            "HOTSPOT_RUNTIME",
            "PROVE_HOTSPOT_RUNTIME_PARITY",
            50,
            "HotSpot runtime change requires VM/JTReg/runtime proof.",
        )
    if domain == "security":
        return (
            "HIGH",
            "SECURITY_LIBRARY",
            "PROVE_SECURITY_LIBRARY_BACKPORT",
            45,
            "Security behavior/test vectors/provider integration require explicit proof.",
        )
    if domain == "core-libs":
        return (
            "MEDIUM",
            "CORE_LIBRARY",
            "PROVE_CORE_LIBRARY_BACKPORT",
            35,
            "Core-library change requires API/binary/behavior compatibility checks.",
        )
    if domain == "other-runtime-or-library":
        return (
            "MEDIUM",
            "RUNTIME_OR_LIBRARY",
            "PROVE_LIBRARY_RUNTIME_BACKPORT",
            30,
            "Runtime/library change requires Java-21 compile/test/runtime proof.",
        )
    if domain == "tools":
        return (
            "LOW_MEDIUM",
            "TOOLING",
            "PROVE_TOOLING_BACKPORT",
            20,
            "Tooling improvements are early candidates when Java-21 contracts remain unchanged.",
        )
    if domain == "build":
        return (
            "LOW",
            "BUILD",
            "PROVE_BUILD_BACKPORT",
            10,
            "Build-only change is an early candidate but still needs target build proof.",
        )
    if domain == "test":
        return (
            "LOW",
            "TEST",
            "PROVE_TEST_BACKPORT",
            11,
            "Test-only change can improve proof coverage without product-contract mutation.",
        )
    return (
        "MEDIUM",
        "GENERAL",
        "PROVE_GENERAL_BACKPORT",
        40,
        "Mixed/other change requires explicit dependency and compatibility review.",
    )


def queue(rows: Iterable[Mapping[str, str]]) -> list[QueueItem]:
    prepared: list[tuple[int, int, Mapping[str, str], tuple[str, ...], tuple[str, str, str, int, str]]] = []
    for source_order, row in enumerate(rows):
        paths = _split_paths(row.get("paths", ""))
        classification = _classification(row)
        priority = classification[3]
        prepared.append((priority, source_order, row, paths, classification))

    prepared.sort(key=lambda item: (item[0], int(item[2]["release"]), item[1], item[2]["commit"]))

    result: list[QueueItem] = []
    for order, (_priority_key, _source_order, row, paths, classification) in enumerate(prepared):
        risk, proof_lane, next_action, priority, _reason = classification
        result.append(
            QueueItem(
                order=order,
                release=int(row["release"]),
                commit=row["commit"],
                jbs_ids=row.get("jbs_ids", ""),
                subject=row["subject"],
                domain=row["domain"],
                inventory_disposition=row["disposition"],
                risk=risk,
                scope_floor=_scope_floor(paths),
                proof_lane=proof_lane,
                recipe_strategy=_recipe_strategy(paths),
                priority=priority,
                compatibility_state=PENDING,
                next_action=next_action,
                paths=",".join(paths),
            )
        )
    return result


def read_inventory(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    required = {
        "release",
        "commit",
        "jbs_ids",
        "subject",
        "domain",
        "javac_touch",
        "grammar_touch",
        "hotspot_compiler_touch",
        "compatibility_signal",
        "disposition",
        "paths",
    }
    if not rows:
        raise ValueError("inventory is empty")
    missing = required - set(rows[0])
    if missing:
        raise ValueError(f"inventory missing columns: {sorted(missing)}")
    return rows


def write_tsv(items: Sequence[QueueItem], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow(
        (
            "order",
            "release",
            "commit",
            "jbs_ids",
            "subject",
            "domain",
            "inventory_disposition",
            "risk",
            "scope_floor",
            "proof_lane",
            "recipe_strategy",
            "priority",
            "compatibility_state",
            "next_action",
            "paths",
        )
    )
    for item in items:
        writer.writerow(
            (
                item.order,
                item.release,
                item.commit,
                item.jbs_ids,
                item.subject,
                item.domain,
                item.inventory_disposition,
                item.risk,
                item.scope_floor,
                item.proof_lane,
                item.recipe_strategy,
                item.priority,
                item.compatibility_state,
                item.next_action,
                item.paths,
            )
        )


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--out", type=Path)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    items = queue(read_inventory(args.inventory))
    if args.out is None:
        write_tsv(items, sys.stdout)
    else:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        with args.out.open("w", encoding="utf-8", newline="") as handle:
            write_tsv(items, handle)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
