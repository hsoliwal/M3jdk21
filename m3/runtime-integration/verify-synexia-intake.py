#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

POLICY = "m3/docs/synexia-intake-policy.tsv"
DIRECT = "m3/docs/synexia-direct-copy-review.tsv"
MAPPING = "m3/docs/name-mapping.json"
FOSS = "m3/runtime-integration/FOSS_REUSE_DECISION.tsv"
LICENSE_INFO = "ADDITIONAL_LICENSE_INFO"

REQUIRED_MODES = {
    "EXTERNAL_RECIPE_TOOL",
    "INDEPENDENT_APACHE_MODULE",
    "DATA_OR_METADATA_EXPORT",
    "TARGET_OWNED_PORT",
    "EXPLICIT_RELICENSE_PORT",
    "EXISTING_DIRECT_COPY_REVIEW",
}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def copied_synexia_runtime_records(mapping: dict) -> dict[str, tuple[str, ...]]:
    result: dict[str, tuple[str, ...]] = {}
    for record in mapping.get("migration", {}).get("records", []):
        provenance = record.get("provenance", {})
        if provenance.get("copied_code") is not True:
            continue
        sources = record.get("sources", [])
        if not any(source.get("repo") == "hsoliwal/com.synexia" for source in sources):
            continue
        targets = tuple(
            target.get("path", "")
            for target in record.get("targets", [])
            if target.get("path", "").startswith("src/")
        )
        if targets:
            result[record["id"]] = targets
    return result


def verify(root: Path) -> None:
    policy_rows = rows(root / POLICY)
    require(policy_rows, "empty Synexia intake policy")
    require(
        set(row["mode"] for row in policy_rows) == REQUIRED_MODES,
        "Synexia intake mode set drift",
    )
    policy = {row["mode"]: row for row in policy_rows}
    require(
        policy["TARGET_OWNED_PORT"]["source_copy"] == "NO_DIRECT_COPY",
        "target-owned port must refuse direct Apache source copy",
    )
    require(
        policy["EXTERNAL_RECIPE_TOOL"]["runtime_dependency"] == "NO",
        "OpenRewrite recipe tooling must not become a JDK runtime dependency",
    )
    require(
        policy["EXPLICIT_RELICENSE_PORT"]["disposition"] == "REVIEW_REQUIRED",
        "direct target source copy must require explicit relicense review",
    )

    additional = (root / LICENSE_INFO).read_text(encoding="utf-8")
    require(
        "programs licensed under the Apache License, Version 2.0" in additional,
        "OpenJDK additional-license Apache boundary text missing",
    )
    require(
        "would not permit you to commingle code under an incompatible license" in additional,
        "OpenJDK no-commingling boundary text missing",
    )

    mapping = json.loads((root / MAPPING).read_text(encoding="utf-8"))
    copied = copied_synexia_runtime_records(mapping)
    review_rows = rows(root / DIRECT)
    review = {row["mapping_id"]: row for row in review_rows}
    require(set(review) == set(copied), "Synexia direct-copy review ledger drift")
    for mapping_id, targets in copied.items():
        row = review[mapping_id]
        require(row["review_status"] == "REVIEW_REQUIRED", f"{mapping_id}: review status drift")
        ledger_targets = tuple(part for part in row["m3jdk21_target_paths"].split(";") if part)
        require(ledger_targets == targets, f"{mapping_id}: target-path review drift")
        require(
            "relicense" in row["required_resolution"].lower()
            and "reimplementation" in row["required_resolution"].lower(),
            f"{mapping_id}: resolution alternatives incomplete",
        )

    foss_rows = rows(root / FOSS)
    synexia = [
        row for row in foss_rows
        if row.get("repository") == "https://github.com/hsoliwal/com.synexia"
    ]
    require(synexia, "Synexia missing from FOSS reuse ledger")
    for row in synexia:
        require(row.get("license") == "Apache-2.0", f"{row.get('capability')}: license drift")
        require(
            row.get("classification") != "DIRECT_IMPORT",
            f"{row.get('capability')}: unreviewed direct import is forbidden",
        )

    print(
        "SYNEXIA_INTAKE_POLICY_PASS "
        f"modes={len(policy_rows)} direct_copy_reviews={len(review_rows)} "
        f"foss_synexia={len(synexia)}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()
    verify(args.repo.resolve())


if __name__ == "__main__":
    main()
