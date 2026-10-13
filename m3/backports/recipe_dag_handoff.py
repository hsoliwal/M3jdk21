#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Validate the proof-bound Synexia/Nebula recipe-DAG handoff for M3JDK21."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
from pathlib import Path
from typing import Iterable, Mapping, Sequence

SCHEMA = "M3_SYNEXIA_NEBULA_RECIPE_DAG_HANDOFF_V1"
REQUIRED_COLUMNS = (
    "id",
    "kind",
    "repository",
    "ref",
    "commit",
    "path",
    "contentHashAlgorithm",
    "contentHash",
    "license",
    "authorship",
    "delivery",
    "targetRepository",
    "applicationPolicy",
    "mutationAuthority",
    "promotionAuthority",
)
REQUIRED_ASSETS = {
    "recipe-dag": (
        "synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/RecipeDag.java",
        "881e598dc80ba25444df63d82e1f4fe43b2ca836378b68b855f9c9ebd0c80ac3",
    ),
    "openrewrite-recipe-dag-plan": (
        "synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/OpenRewriteRecipeDagPlan.java",
        "156201feb19943bfe235bafef714b448c4be40139ce36b332cd967b129681971",
    ),
    "recipe-first-invariant": (
        "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3RecipeFirstInvariant.java",
        "050e43f3f6db2d5a99b109147a6728edefb2323d698e82ebc7356bc23dfeab0c",
    ),
}
SYNEXIA_ASSET_COMMIT = "850f5b0c2546ee6f645d91e3c341fd744d883480"
TARGET = "hsoliwal/M3jdk21"
NEBULA_RECEIVER_SCHEMA = "M3_NEBULA_MASTERED_DAG_RECEIVER_V2"
NEBULA_RECEIVER_PATH = (
    Path(__file__).resolve().parents[1]
    / "synexia-import"
    / "nebula-mastered-dag-receiver-v2.tsv"
)


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if tuple(reader.fieldnames or ()) != REQUIRED_COLUMNS:
            raise ValueError("handoff header mismatch")
        rows = list(reader)
    if not rows:
        raise ValueError("empty handoff")
    return rows


def _bool(value: str, field: str) -> bool:
    if value not in {"true", "false"}:
        raise ValueError(f"{field} must be true/false")
    return value == "true"


def _hash(algorithm: str, value: str) -> None:
    if algorithm == "SHA-256":
        if not re.fullmatch(r"[0-9a-f]{64}", value):
            raise ValueError("invalid SHA-256")
        return
    if algorithm == "GIT-BLOB-SHA1":
        if not re.fullmatch(r"[0-9a-f]{40}", value):
            raise ValueError("invalid Git blob SHA-1")
        return
    raise ValueError(f"unsupported content hash algorithm: {algorithm}")


def validate(rows: Iterable[Mapping[str, str]]) -> list[dict[str, str]]:
    checked = [dict(row) for row in rows]
    ids: set[str] = set()
    by_id: dict[str, dict[str, str]] = {}

    for row in checked:
        row_id = row["id"].strip()
        if not row_id or row_id in ids:
            raise ValueError(f"duplicate/blank handoff id: {row_id!r}")
        ids.add(row_id)
        by_id[row_id] = row

        if not re.fullmatch(r"[0-9a-f]{40}", row["commit"]):
            raise ValueError(f"invalid commit for {row_id}")
        _hash(row["contentHashAlgorithm"], row["contentHash"])
        if row["targetRepository"] != TARGET:
            raise ValueError(f"wrong target repository for {row_id}")
        if _bool(row["mutationAuthority"], "mutationAuthority"):
            raise ValueError(f"handoff grants mutation authority: {row_id}")
        if _bool(row["promotionAuthority"], "promotionAuthority"):
            raise ValueError(f"handoff grants promotion authority: {row_id}")

    for asset_id, (path, digest) in REQUIRED_ASSETS.items():
        row = by_id.get(asset_id)
        if row is None:
            raise ValueError(f"missing required Synexia asset: {asset_id}")
        if row["kind"] != "SYNEXIA_RECIPE_DAG_ASSET":
            raise ValueError(f"wrong kind for {asset_id}")
        if row["repository"] != "hsoliwal/com.synexia":
            raise ValueError(f"wrong repository for {asset_id}")
        if row["commit"] != SYNEXIA_ASSET_COMMIT:
            raise ValueError(f"wrong canonical Synexia commit for {asset_id}")
        if row["path"] != path or row["contentHash"] != digest:
            raise ValueError(f"wrong path/hash for {asset_id}")
        if row["contentHashAlgorithm"] != "SHA-256":
            raise ValueError(f"Synexia asset must use SHA-256: {asset_id}")
        if row["license"] != "Apache-2.0":
            raise ValueError(f"reusable Synexia asset must be Apache-2.0: {asset_id}")
        if row["authorship"] != "SYNEXIA_ORIGINAL":
            raise ValueError(f"wrong authorship for {asset_id}")
        if row["delivery"] != "COPY_ADAPT":
            raise ValueError(f"wrong delivery mode for {asset_id}")
        if row["applicationPolicy"] != "PROVEN_RECIPE_DAG_ONLY":
            raise ValueError(f"wrong application policy for {asset_id}")

    target = by_id.get("synexia-target-policy")
    if target is None:
        raise ValueError("missing Synexia target policy evidence")
    if (
        target["repository"] != "hsoliwal/com.synexia"
        or target["path"] != ".m3/donor-convergence/PROJECT_TARGETS.tsv"
        or target["delivery"] != "REFERENCE_ONLY"
        or target["applicationPolicy"] != "PROVEN_RECIPE_DAG_ONLY"
    ):
        raise ValueError("invalid Synexia target policy evidence")

    nebula = by_id.get("nebula-proving-transfer")
    if nebula is None:
        raise ValueError("missing Nebula proving reference")
    if (
        nebula["repository"] != "hsoliwal/nebula"
        or nebula["path"] != "m3/docs/FINAL_PROVEN_TRANSFER_20261006.md"
        or nebula["delivery"] != "REFERENCE_ONLY"
        or nebula["applicationPolicy"] != "PROVING_REFERENCE_ONLY"
    ):
        raise ValueError("invalid Nebula proving reference")

    return checked


def canonical_root(rows: Sequence[Mapping[str, str]]) -> str:
    payload = "\n".join(
        "\t".join(row[column] for column in REQUIRED_COLUMNS)
        for row in rows
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def validate_nebula_receiver(path: Path = NEBULA_RECEIVER_PATH) -> dict[str, str]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        rows = list(reader)
    if len(rows) != 1:
        raise ValueError("Nebula successor receiver must contain exactly one row")
    row = rows[0]
    expected = {
        "schema": NEBULA_RECEIVER_SCHEMA,
        "proof_repository": "hsoliwal/nebula",
        "proof_ref": "master",
        "proof_commit": "7b36ce9e3e0c8d1e91324589b38b9d9d5a164356",
        "recipe_dag_transfer": "ADMITTED",
        "recipe_custody": "CANONICAL_SYNEXIA_APACHE_HANDOFF",
        "runtime_source_equivalence": "GREEN_ZERO_DRIFT_FROM_QUALIFIED_RUNTIME",
        "native_current_head": "PENDING_EXTERNAL_CI",
        "target_repository": TARGET,
        "application_policy": "PROVEN_RECIPE_DAG_ONLY",
        "source_mutation": "false",
        "semantic_equivalence": "false",
        "compatibility_authority": "false",
        "promotion": "false",
    }
    if row != expected:
        raise ValueError(f"Nebula successor receiver drift: {row}")
    return row


def summary(rows: Sequence[Mapping[str, str]]) -> dict[str, object]:
    return {
        "schema": SCHEMA,
        "root": canonical_root(rows),
        "rows": len(rows),
        "target": TARGET,
        "canonicalSynexiaCommit": SYNEXIA_ASSET_COMMIT,
        "mutationAuthority": False,
        "promotionAuthority": False,
        "applicationPolicy": "PROVEN_RECIPE_DAG_ONLY",
        "nebulaReferenceOnly": True,
    }


def _args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--receipt",
        type=Path,
        default=Path(__file__).with_name("SYNEXIA_NEBULA_RECIPE_DAG_HANDOFF.tsv"),
    )
    parser.add_argument("--out", type=Path)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _args(argv)
    rows = validate(read_rows(args.receipt))
    nebula = validate_nebula_receiver()
    payload = summary(rows)
    payload["nebulaReceiverSchema"] = nebula["schema"]
    payload["nebulaNativeCurrentHead"] = nebula["native_current_head"]
    if args.out is not None:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(
            json.dumps(payload, sort_keys=True, indent=2) + "\n",
            encoding="utf-8",
        )
    print(
        f"PASS: {payload['rows']} handoff rows, "
        f"target={payload['target']}, root={payload['root']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
