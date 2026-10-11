#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify exact Git-blob custody for the pinned Synexia recipe snapshot."""

from __future__ import annotations

import csv
import hashlib
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
INTAKE = ROOT / "m3/synexia-import/intakes/donor-convergence-workspace-20261011"
PROVENANCE = INTAKE / "SOURCE_PROVENANCE.tsv"
TARGET_MAP = INTAKE / "TARGET_MAP.tsv"
EXPECTED_SOURCE = "hsoliwal/com.synexia"
EXPECTED_COMMIT = "2be201d6ff52ac563bee1f6848ff8d2d436fe5c8"
EXPECTED_SCHEMA = "SYNEXIA_RECIPE_CUSTODY_V1"


def git_blob_sha1(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


def main() -> int:
    provenance = read_tsv(PROVENANCE)
    if len(provenance) != 3:
        raise ValueError(f"expected exactly 3 copied source assets, found {len(provenance)}")
    targets: set[str] = set()
    for row in provenance:
        if row.get("schema") != EXPECTED_SCHEMA:
            raise ValueError("provenance schema drift")
        if row.get("source_repository") != EXPECTED_SOURCE:
            raise ValueError("unexpected source repository")
        if row.get("source_commit") != EXPECTED_COMMIT:
            raise ValueError("source commit drift")
        if row.get("license") != "Apache-2.0":
            raise ValueError("non-Apache source entered the automatic custody lane")
        target = row["target_path"]
        if target in targets:
            raise ValueError(f"duplicate target path: {target}")
        targets.add(target)
        path = ROOT / target
        if not path.is_file():
            raise ValueError(f"missing copied source asset: {target}")
        actual = git_blob_sha1(path)
        if actual != row["source_git_blob_sha1"]:
            raise ValueError(
                f"source blob mismatch for {target}: "
                f"expected={row['source_git_blob_sha1']} actual={actual}"
            )
        if target.endswith(".java") and "SPDX-License-Identifier: Apache-2.0" not in path.read_text(encoding="utf-8"):
            raise ValueError(f"missing per-file Apache SPDX marker: {target}")

    mapping = read_tsv(TARGET_MAP)
    if len(mapping) != 3 or {row.get("target_path") for row in mapping} != targets:
        raise ValueError("target map and source provenance do not cover the same exact paths")
    catalogue = ROOT / "m3/vendor/synexia/synexia-openrewrite-recipes/snapshots/20261011/CANONICAL_RECIPE_HOME.tsv"
    workspace = ROOT / "m3/vendor/synexia/synexia-openrewrite-recipes/snapshots/20261011/M3DonorConvergenceWorkspaceProgramRecipe.java"
    text = catalogue.read_text(encoding="utf-8")
    stale_pin = "ac32ad07c9624cb953066fbe06ecb84144a009a9"
    actual_owner = git_blob_sha1(workspace)
    if stale_pin not in text or actual_owner != "b1d0f992e0af861633a37065ab3a5f0a93f9dcde":
        raise ValueError("known stale owner-pin control changed; refresh this verifier with a new source packet")
    holds = [row for row in mapping if row.get("acceptance_state") == "CUSTODY_ONLY_KNOWN_STALE_OWNER_PIN_HOLD"]
    if len(holds) != 1:
        raise ValueError("known stale owner pin must remain an explicit single HOLD")
    print(f"M3_SYNEXIA_RECIPE_CUSTODY_PASS files={len(provenance)} known_holds={len(holds)} runtime_promotion=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
