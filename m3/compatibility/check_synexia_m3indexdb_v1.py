#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed unless the M3IndexDB V1 compatibility mirror matches its Synexia source pin."""

from __future__ import annotations

import csv
import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PIN = Path(__file__).with_name("synexia-m3indexdb-v1-pin.tsv")
RESIDUE = Path(__file__).with_name("synexia-canonical-residue-gitblobs.tsv")
LEGACY = (
    ROOT
    / "m3/indexdb/src/main/java/com/m3/indexdb/"
    "M3IndexDbSemanticFingerprint.java"
)
CANONICAL = (
    ROOT
    / "m3/indexdb/src/main/java/com/synexia/mindex/db/"
    "M3IndexDbFingerprintV1.java"
)
LEGACY_BLOB = "df44495f5e5b48ea1bf4a863df9ca188bd787948"
HEX40 = re.compile(r"^[0-9a-f]{40}$")


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def load_pin() -> dict[str, str]:
    with PIN.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        expected = [
            "schema",
            "canonical_repository",
            "canonical_revision",
            "canonical_class",
            "canonical_path",
            "canonical_git_blob",
            "canonical_manifest_path",
            "canonical_manifest_git_blob",
            "license",
            "state",
        ]
        if reader.fieldnames != expected:
            raise ValueError("invalid M3IndexDB V1 pin header")
        rows = list(reader)

    if len(rows) != 1:
        raise ValueError("M3IndexDB V1 pin must contain exactly one row")
    row = rows[0]
    if any(not row[field] for field in expected):
        raise ValueError("blank M3IndexDB V1 pin field")
    if row["schema"] != "M3JDK21_SYNEXIA_INDEXDB_V1_PIN_V1":
        raise ValueError("invalid M3IndexDB V1 pin schema")
    if row["canonical_repository"] != "hsoliwal/com.synexia":
        raise ValueError("unexpected canonical repository")
    if not HEX40.fullmatch(row["canonical_revision"]):
        raise ValueError("invalid canonical revision")
    if row["canonical_class"] != "com.synexia.mindex.db.M3IndexDbFingerprintV1":
        raise ValueError("unexpected canonical V1 class")
    if row["canonical_path"] != (
        "synexia-mindex/db/src/main/java/com/synexia/mindex/db/"
        "M3IndexDbFingerprintV1.java"
    ):
        raise ValueError("unexpected canonical V1 path")
    if not HEX40.fullmatch(row["canonical_git_blob"]):
        raise ValueError("invalid canonical V1 Git blob")
    if row["canonical_manifest_path"] != (
        "synexia-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv"
    ):
        raise ValueError("unexpected canonical ownership manifest")
    if not HEX40.fullmatch(row["canonical_manifest_git_blob"]):
        raise ValueError("invalid canonical ownership manifest blob")
    if row["license"] != "Apache-2.0":
        raise ValueError("M3IndexDB V1 compatibility must remain Apache-2.0")
    if row["state"] != "PINNED_COMPATIBILITY_SOURCE":
        raise ValueError("invalid M3IndexDB V1 pin state")
    return row


def require_residue_owner() -> None:
    with RESIDUE.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    target = "m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticFingerprint.java"
    matches = [row for row in rows if row["path"] == target]
    if len(matches) != 1:
        raise ValueError("historical M3IndexDB V1 residue row missing or duplicated")
    row = matches[0]
    if row["git_blob_sha1"] != LEGACY_BLOB:
        raise ValueError("historical M3IndexDB V1 source identity drift")
    if row["disposition"] != "MIGRATION_RESIDUE_NOT_CANONICAL":
        raise ValueError("historical M3IndexDB V1 residue became canonical")
    if row["canonical_synexia_owner"] != "com.synexia.mindex.db.M3IndexDbFingerprintV1":
        raise ValueError("historical M3IndexDB V1 residue points at wrong canonical owner")
    if row["license"] != "Apache-2.0":
        raise ValueError("historical M3IndexDB V1 residue license drift")


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_synexia_m3indexdb_v1.py", file=sys.stderr)
        return 2

    pin = load_pin()
    if not LEGACY.is_file() or git_blob(LEGACY) != LEGACY_BLOB:
        raise ValueError("historical M3IndexDB V1 source drift")
    if not CANONICAL.is_file() or git_blob(CANONICAL) != pin["canonical_git_blob"]:
        raise ValueError("Synexia M3IndexDB V1 mirror drift")
    require_residue_owner()

    print(
        "SYNEXIA_M3INDEXDB_V1_PIN_PASS "
        f"revision={pin['canonical_revision']} "
        f"blob={pin['canonical_git_blob']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
