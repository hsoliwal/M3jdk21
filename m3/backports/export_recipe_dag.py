#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
"""Export the M3JDK21 backport queue as a candidate-only, content-addressed recipe DAG.

The export is deliberately not a mutation engine.  It preserves the canonical backport pass
sequence and records recipe bindings when they already exist, while keeping mutation and promotion
authority false.  A source-changing APPLY node becomes execution-ready only when its bound recipe
catalogue row is explicitly VERIFIED in both status and verification fields.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
from dataclasses import dataclass
from pathlib import Path
import re
from typing import Iterable, Mapping, Sequence

SCHEMA = "m3jdk21-backport-recipe-dag/v1"
DAG_ID = "m3jdk21-backport-queue"
VALID_SCOPES = {
    "FILE",
    "VISIBILITY",
    "PACKAGE",
    "MODULE",
    "MULTI_MODULE",
    "LIBRARY_API",
}
VERIFIED_WORDS = {"VERIFIED", "PASS", "GREEN"}

OUTPUT_FIELDS = (
    "schema",
    "dag_id",
    "item_key",
    "source_type",
    "release",
    "identity",
    "title",
    "domain",
    "disposition",
    "node_id",
    "pass_ordinal",
    "pass_id",
    "dependency_node_ids",
    "required_scope",
    "declared_source_changing",
    "manifest_mutation_authority",
    "promotion_authority",
    "state",
    "queue_action",
    "risk",
    "recipe_id",
    "recipe_class",
    "binding_status",
    "recipe_status",
    "proof_status",
    "execution_ready",
    "dependency_or_commit",
    "reason",
    "item_root",
    "node_root",
)

QUEUE_FIELDS = (
    "source_type",
    "release",
    "identity",
    "title",
    "domain",
    "disposition",
    "current_pass",
    "action",
    "required_scope",
    "risk",
    "dependency_or_commit",
    "reason",
)

PASS_FIELDS = ("ordinal", "pass_id", "mutation_authority", "stop_condition")
CATALOGUE_FIELDS = ("recipe_id", "class", "scope", "contract", "status", "verification")


@dataclass(frozen=True)
class Pass:
    ordinal: int
    pass_id: str
    declared_mutation: bool
    stop_condition: str


@dataclass(frozen=True)
class RecipeBinding:
    recipe_id: str
    recipe_class: str
    scope: str
    status: str
    verification: str


def sha256_fields(*values: str) -> str:
    digest = hashlib.sha256()
    for value in values:
        encoded = value.encode("utf-8")
        digest.update(len(encoded).to_bytes(4, "big", signed=False))
        digest.update(encoded)
    return digest.hexdigest()


def clean(value: str, field: str) -> str:
    if value is None:
        raise ValueError(f"{field}: null")
    if "\t" in value or "\r" in value or "\n" in value or "\0" in value:
        raise ValueError(f"{field}: control separator")
    if value != value.strip(" "):
        raise ValueError(f"{field}: edge spaces")
    return value


def read_tsv(path: Path, required_fields: Sequence[str]) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if tuple(reader.fieldnames or ()) != tuple(required_fields):
            raise ValueError(
                f"{path}: expected header {tuple(required_fields)}, found {reader.fieldnames}"
            )
        rows: list[dict[str, str]] = []
        for number, raw in enumerate(reader, 2):
            row = {field: clean(raw[field], f"{path}:{number}:{field}") for field in required_fields}
            rows.append(row)
        return rows


def read_passes(path: Path) -> list[Pass]:
    passes: list[Pass] = []
    for row in read_tsv(path, PASS_FIELDS):
        ordinal = int(row["ordinal"])
        declared = row["mutation_authority"]
        if declared not in {"true", "false"}:
            raise ValueError(f"{path}: invalid mutation_authority {declared}")
        passes.append(Pass(ordinal, row["pass_id"], declared == "true", row["stop_condition"]))
    passes.sort(key=lambda value: value.ordinal)
    if [value.ordinal for value in passes] != list(range(len(passes))):
        raise ValueError(f"{path}: pass ordinals must be contiguous from zero")
    if not passes or passes[-1].pass_id != "promote":
        raise ValueError(f"{path}: canonical pass sequence must end in promote")
    mutation = [value.ordinal for value in passes if value.declared_mutation]
    if mutation != [4] or passes[4].pass_id != "apply":
        raise ValueError(f"{path}: APPLY ordinal 4 must be the sole declared mutation pass")
    return passes


def normalized_identity(value: str) -> str:
    return re.sub(r"[^a-z0-9]", "", value.lower())


def recipe_bindings(path: Path) -> list[RecipeBinding]:
    result: list[RecipeBinding] = []
    for row in read_tsv(path, CATALOGUE_FIELDS):
        result.append(
            RecipeBinding(
                row["recipe_id"],
                row["class"],
                row["scope"],
                row["status"],
                row["verification"],
            )
        )
    return result


def match_recipe(identity: str, recipes: Sequence[RecipeBinding]) -> RecipeBinding | None:
    needle = normalized_identity(identity)
    matches = [
        recipe
        for recipe in recipes
        if needle
        and (
            needle in normalized_identity(recipe.recipe_id)
            or needle in normalized_identity(recipe.recipe_class)
        )
    ]
    if len(matches) > 1:
        raise ValueError(
            f"{identity}: ambiguous recipe binding: "
            + ", ".join(value.recipe_id for value in matches)
        )
    return matches[0] if matches else None


def state_for(disposition: str, current: int, ordinal: int) -> str:
    if ordinal < current:
        return "REACHED"
    if ordinal == current:
        return "CURRENT"
    lowered = disposition.lower()
    if lowered.startswith("reject"):
        return "BLOCKED"
    if lowered.startswith("superseded"):
        return "REDIRECTED"
    if lowered.startswith("hold"):
        return "HELD"
    return "FUTURE"


def item_key(row: Mapping[str, str]) -> str:
    return f"{row['source_type']}:{row['release']}:{row['identity']}"


def item_root(row: Mapping[str, str]) -> str:
    return sha256_fields("m3jdk21-backport-item/v1", *(row[field] for field in QUEUE_FIELDS))


def execution_ready(binding: RecipeBinding | None) -> bool:
    return bool(
        binding
        and binding.status.upper() in VERIFIED_WORDS
        and binding.verification.upper() in VERIFIED_WORDS
    )


def export_rows(
    queue_rows: Sequence[Mapping[str, str]],
    passes: Sequence[Pass],
    recipes: Sequence[RecipeBinding],
) -> list[dict[str, str]]:
    stable_queue = sorted(
        queue_rows,
        key=lambda row: (
            row["source_type"],
            int(row["release"]),
            row["identity"],
        ),
    )
    output: list[dict[str, str]] = []
    node_ids: set[str] = set()

    for queue in stable_queue:
        current = int(queue["current_pass"])
        if current < 0 or current >= len(passes):
            raise ValueError(f"{queue['identity']}: current_pass out of range: {current}")
        scope = queue["required_scope"]
        if scope not in VALID_SCOPES:
            raise ValueError(f"{queue['identity']}: invalid required_scope: {scope}")

        key = item_key(queue)
        base_root = item_root(queue)
        binding = match_recipe(queue["identity"], recipes)
        previous = ""

        for pass_value in passes:
            node_id = (
                f"{queue['source_type'].lower()}-{queue['release']}-"
                f"{queue['identity'].lower().replace('_', '-').replace(':', '-')}"
                f"-p{pass_value.ordinal}-{pass_value.pass_id}"
            )
            node_id = re.sub(r"[^a-z0-9_.-]+", "-", node_id).strip("-")
            if node_id in node_ids:
                raise ValueError(f"duplicate node id: {node_id}")
            node_ids.add(node_id)

            is_apply = pass_value.ordinal == 4
            binding_status = (
                "RECIPE_DECLARED"
                if is_apply and binding
                else "UNBOUND"
                if is_apply
                else "NOT_APPLICABLE"
            )
            proof_status = (
                binding.verification
                if is_apply and binding
                else "UNPROVEN"
                if is_apply
                else "EVIDENCE_ONLY"
            )
            ready = is_apply and execution_ready(binding)

            row = {
                "schema": SCHEMA,
                "dag_id": DAG_ID,
                "item_key": key,
                "source_type": queue["source_type"],
                "release": queue["release"],
                "identity": queue["identity"],
                "title": queue["title"],
                "domain": queue["domain"],
                "disposition": queue["disposition"],
                "node_id": node_id,
                "pass_ordinal": str(pass_value.ordinal),
                "pass_id": pass_value.pass_id,
                "dependency_node_ids": previous,
                "required_scope": scope,
                "declared_source_changing": "true" if is_apply else "false",
                # Exported queue evidence never owns source mutation.
                "manifest_mutation_authority": "false",
                # Promotion is always outside the orchestrator.
                "promotion_authority": "false",
                "state": state_for(queue["disposition"], current, pass_value.ordinal),
                "queue_action": queue["action"],
                "risk": queue["risk"],
                "recipe_id": binding.recipe_id if binding else "",
                "recipe_class": binding.recipe_class if binding else "",
                "binding_status": binding_status,
                "recipe_status": binding.status if binding else "",
                "proof_status": proof_status,
                "execution_ready": "true" if ready else "false",
                "dependency_or_commit": queue["dependency_or_commit"],
                "reason": queue["reason"],
                "item_root": base_root,
                "node_root": "",
            }
            row["node_root"] = sha256_fields(
                "m3jdk21-backport-node/v1",
                *(row[field] for field in OUTPUT_FIELDS if field != "node_root"),
            )
            output.append(row)
            previous = node_id

    return output


def dag_root(rows: Sequence[Mapping[str, str]]) -> str:
    return sha256_fields(
        "m3jdk21-backport-recipe-dag-root/v1",
        *(row["node_root"] for row in rows),
    )


def render(rows: Sequence[Mapping[str, str]]) -> str:
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(
        stream,
        fieldnames=OUTPUT_FIELDS,
        delimiter="\t",
        lineterminator="\n",
        extrasaction="raise",
    )
    writer.writeheader()
    writer.writerows(rows)
    return stream.getvalue()


def root_receipt(rows: Sequence[Mapping[str, str]]) -> str:
    items = len({row["item_key"] for row in rows})
    ready = sum(row["execution_ready"] == "true" for row in rows)
    return (
        f"schema={SCHEMA}\n"
        f"dag_id={DAG_ID}\n"
        f"items={items}\n"
        f"nodes={len(rows)}\n"
        f"execution_ready_nodes={ready}\n"
        f"dag_root={dag_root(rows)}\n"
    )


def build(
    queue_path: Path,
    passes_path: Path,
    catalogue_path: Path,
) -> tuple[str, str]:
    queue = read_tsv(queue_path, QUEUE_FIELDS)
    passes = read_passes(passes_path)
    recipes = recipe_bindings(catalogue_path)
    rows = export_rows(queue, passes, recipes)
    return render(rows), root_receipt(rows)


def write_or_check(path: Path, expected: str, check: bool) -> None:
    if check:
        actual = path.read_text(encoding="utf-8")
        if actual != expected:
            raise SystemExit(f"{path}: generated content drift")
        return
    path.write_text(expected, encoding="utf-8", newline="\n")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--queue", type=Path, default=Path("m3/backports/BACKPORT_WORK_QUEUE.tsv"))
    parser.add_argument("--passes", type=Path, default=Path("m3/backports/BACKPORT_PASSES.tsv"))
    parser.add_argument(
        "--catalogue", type=Path, default=Path("m3/tooling/recipe-catalogue.tsv")
    )
    parser.add_argument(
        "--output", type=Path, default=Path("m3/backports/BACKPORT_RECIPE_DAG.tsv")
    )
    parser.add_argument(
        "--root-output", type=Path, default=Path("m3/backports/BACKPORT_RECIPE_DAG_ROOT.txt")
    )
    parser.add_argument("--check", action="store_true")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    text, receipt = build(args.queue, args.passes, args.catalogue)
    write_or_check(args.output, text, args.check)
    write_or_check(args.root_output, receipt, args.check)
    if not args.check:
        print(receipt, end="")


if __name__ == "__main__":
    main()
