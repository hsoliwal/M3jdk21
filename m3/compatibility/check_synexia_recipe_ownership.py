#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed ownership check for Synexia-canonical reusable recipe mirrors/residue."""

from __future__ import annotations

import csv
import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEDGER = Path(__file__).with_name("synexia-recipe-ownership.tsv")
PIN = Path(__file__).with_name("synexia-recipe-home-pin.tsv")
RESIDUE = Path(__file__).with_name("synexia-canonical-residue-gitblobs.tsv")
HEX40 = re.compile(r"[0-9a-f]{40}")


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def load_pin(path: Path = PIN) -> dict[str, str]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        expected = [
            "schema",
            "canonical_repository",
            "canonical_revision",
            "canonical_manifest_path",
            "canonical_manifest_git_blob",
            "convergence_invariant_path",
            "convergence_invariant_git_blob",
            "license",
            "state",
        ]
        if reader.fieldnames != expected:
            raise ValueError("invalid Synexia recipe-home pin header")
        rows = list(reader)
    if len(rows) != 1:
        raise ValueError("Synexia recipe-home pin must contain exactly one row")
    row = rows[0]
    if any(not row[field] for field in expected):
        raise ValueError("blank Synexia recipe-home pin field")
    if row["schema"] != "M3JDK21_SYNEXIA_RECIPE_HOME_PIN_V1":
        raise ValueError("invalid Synexia recipe-home pin schema")
    if row["canonical_repository"] != "hsoliwal/com.synexia":
        raise ValueError("unexpected Synexia canonical repository")
    if not HEX40.fullmatch(row["canonical_revision"]):
        raise ValueError("invalid Synexia canonical revision")
    if row["canonical_manifest_path"] != "synexia-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv":
        raise ValueError("unexpected Synexia canonical manifest")
    if row["convergence_invariant_path"] != (
        "docs/M3-SCALE/invariants/SYNEXIA-PUBLIC-TARGET-CONVERGENCE-1.json"
    ):
        raise ValueError("unexpected Synexia convergence invariant")
    for field in ("canonical_manifest_git_blob", "convergence_invariant_git_blob"):
        if not HEX40.fullmatch(row[field]):
            raise ValueError(f"invalid pinned Git blob: {field}")
    if row["license"] != "Apache-2.0":
        raise ValueError("canonical Synexia recipe fast lane must remain Apache-2.0")
    if row["state"] != "PINNED_CANONICAL_SOURCE":
        raise ValueError("invalid Synexia recipe-home pin state")
    return row


def load(path: Path = LEDGER) -> list[dict[str, str]]:
    """Validate the already-borrowed pure-int mirror ledger."""
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        expected = [
            "family",
            "legacy_path",
            "legacy_git_blob",
            "canonical_path",
            "synexia_revision",
            "synexia_git_blob",
            "state",
        ]
        if reader.fieldnames != expected:
            raise ValueError("invalid Synexia recipe ownership header")
        rows = list(reader)

    if not rows:
        raise ValueError("empty Synexia recipe ownership ledger")

    families: set[str] = set()
    revisions: set[str] = set()
    legacy_roots = (
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/",
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/",
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/",
    )
    canonical_roots = (
        "m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/atom/",
        "m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/scope/",
        "m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/semantic/",
    )
    legacy_exact = {
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java",
    }
    canonical_exact = {
        "m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/M3Java21FileConvergenceRecipe.java",
    }
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in expected):
            raise ValueError(f"blank ownership field at row {physical}")
        if row["family"] in families:
            raise ValueError(f"duplicate recipe family {row['family']}")
        families.add(row["family"])
        if row["state"] != "BORROWED_CANONICAL_WITH_FROZEN_LEGACY":
            raise ValueError(f"invalid recipe ownership state at row {physical}")
        if not HEX40.fullmatch(row["synexia_revision"]):
            raise ValueError(f"invalid Synexia revision at row {physical}")
        revisions.add(row["synexia_revision"])
        for field in ("legacy_git_blob", "synexia_git_blob"):
            if not HEX40.fullmatch(row[field]):
                raise ValueError(f"invalid Git blob {field} at row {physical}")
        if not (
            row["legacy_path"].startswith(legacy_roots)
            or row["legacy_path"] in legacy_exact
        ):
            raise ValueError(f"legacy recipe escaped frozen namespace at row {physical}")
        if not (
            row["canonical_path"].startswith(canonical_roots)
            or row["canonical_path"] in canonical_exact
        ):
            raise ValueError(f"canonical recipe escaped borrowed namespace at row {physical}")

        legacy = ROOT / row["legacy_path"]
        canonical = ROOT / row["canonical_path"]
        if not legacy.is_file() or git_blob(legacy) != row["legacy_git_blob"]:
            raise ValueError(f"frozen legacy recipe drift: {row['legacy_path']}")
        if not canonical.is_file() or git_blob(canonical) != row["synexia_git_blob"]:
            raise ValueError(f"Synexia canonical mirror drift: {row['canonical_path']}")

    if len(revisions) != 1:
        raise ValueError("one handoff must bind one exact Synexia revision")
    pin = load_pin()
    if revisions != {pin["canonical_revision"]}:
        raise ValueError("borrowed recipe mirrors do not match pinned Synexia canonical revision")
    return rows


def reusable_residue_paths() -> set[str]:
    result: set[str] = set()
    for exact in (
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java",
    ):
        if (ROOT / exact).is_file():
            result.add(exact)

    for directory in (
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope",
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom",
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic",
        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3",
        "m3/indexdb/src/main/java",
    ):
        start = ROOT / directory
        if not start.is_dir():
            continue
        result.update(
            path.relative_to(ROOT).as_posix()
            for path in start.rglob("*.java")
            if path.is_file()
        )
    return result


def load_residue(path: Path = RESIDUE) -> list[dict[str, str]]:
    """Validate the full reusable target-local migration residue freeze."""
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        expected = [
            "path",
            "git_blob_sha1",
            "disposition",
            "canonical_synexia_owner",
            "license",
        ]
        if reader.fieldnames != expected:
            raise ValueError("invalid Synexia canonical residue header")
        rows = list(reader)

    if not rows:
        raise ValueError("empty Synexia canonical residue ledger")

    seen: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in expected):
            raise ValueError(f"blank residue field at row {physical}")
        target = row["path"]
        if target in seen:
            raise ValueError(f"duplicate residue path: {target}")
        seen.add(target)
        if not HEX40.fullmatch(row["git_blob_sha1"]):
            raise ValueError(f"invalid residue Git blob: {target}")
        if row["disposition"] != "MIGRATION_RESIDUE_NOT_CANONICAL":
            raise ValueError(f"invalid residue disposition: {target}")
        owner = row["canonical_synexia_owner"]
        if not (owner.startswith("com.synexia") or owner.startswith("synexia-")):
            raise ValueError(f"invalid canonical Synexia owner: {target}")
        if row["license"] != "Apache-2.0":
            raise ValueError(f"non-Apache reusable residue: {target}")

        source = ROOT / target
        if not source.is_file():
            raise ValueError(f"frozen reusable residue missing: {target}")
        if git_blob(source) != row["git_blob_sha1"]:
            raise ValueError(
                "frozen reusable residue drift: "
                f"{target}; improve {owner} in hsoliwal/com.synexia and consume a pinned handoff"
            )

    actual = reusable_residue_paths()
    if seen != actual:
        missing = sorted(actual - seen)
        stale = sorted(seen - actual)
        raise ValueError(
            f"reusable residue set drift: unsealed={missing} retired_without_manifest_update={stale}"
        )
    return rows


def main(argv: list[str]) -> int:
    if len(argv) > 1:
        print("usage: check_synexia_recipe_ownership.py", file=sys.stderr)
        return 2
    rows = load()
    residue = load_residue()
    pin = load_pin()
    revision = rows[0]["synexia_revision"]
    print(
        "SYNEXIA_RECIPE_OWNERSHIP_PASS "
        f"borrowed_rows={len(rows)} frozen_residue={len(residue)} revision={revision} "
        f"manifest_blob={pin['canonical_manifest_git_blob']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
