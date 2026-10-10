#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Source-bound proof for the Synexia lexicon provenance contract.

This is intentionally independent of the exporter.  It checks only the reviewed
manifest/provenance metadata shipped in M3JDK; it never downloads or redistributes
the operator-owned corpora.
"""
from __future__ import annotations

import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "m3/lexicon/synexia-source-manifest.tsv"
PROVENANCE = ROOT / "m3/lexicon/synexia-dictlang-DATA_PROVENANCE.tsv"
DONOR_REVIEW = ROOT / "m3/ports/precompute/DONOR_REVIEW.tsv"
EXPORT_DOC = ROOT / "m3/lexicon/SYNEXIA_EXPORT.md"

MANIFEST_COLUMNS = (
    "source_id", "canonical_name", "synexia_path", "record_id_field",
    "mapping_fields", "precompute_target", "data_license", "data_policy",
    "precompute_fields",
)
PROVENANCE_COLUMNS = (
    "resource", "synexia_path", "bytes", "lines", "sha256", "origin",
    "origin_id", "data_license", "obligations", "synexia_manifest_correction",
)
DONOR_COLUMNS = ("repository", "commit", "metadata_license", "role", "disposition")
OPENHINGLISH_REPOSITORY = "shankarmishra/openhinglish"
OPENHINGLISH_COMMIT = "0019fe84d1e98c4eaaed6e3a8d21a63b21ab90ea"
STALE_MARKERS = ("WRONG", "manifest says operator-supplied", "refine to")


def fail(message: str) -> None:
    raise SystemExit(f"SYNEXIA_LEXICON_PROVENANCE_FAIL|{message}")


def read_rows(path: Path, columns: tuple[str, ...]) -> list[dict[str, str]]:
    if not path.is_file():
        fail(f"missing={path.relative_to(ROOT).as_posix()}")
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if tuple(reader.fieldnames or ()) != columns:
            fail(f"columns={path.name}")
        rows = list(reader)
    if any(None in row or any(value is None for value in row.values()) for row in rows):
        fail(f"malformed={path.name}")
    return rows


def main() -> None:
    manifest = read_rows(MANIFEST, MANIFEST_COLUMNS)
    provenance = read_rows(PROVENANCE, PROVENANCE_COLUMNS)
    provenance_by_path = {row["synexia_path"]: row for row in provenance}
    if len(provenance_by_path) != len(provenance):
        fail("duplicate_provenance_path")

    source_rows = [
        row for row in manifest
        if row["synexia_path"].startswith("synexia-dictlang/src/main/resources/")
    ]
    if not source_rows:
        fail("no_dictlang_resource_rows")
    source_paths = [row["synexia_path"] for row in source_rows]
    if len(set(source_paths)) != len(source_paths):
        fail("duplicate_manifest_path")

    matched = 0
    gutenberg = 0
    mit = 0
    for source in source_rows:
        path = source["synexia_path"]
        row = provenance_by_path.get(path)
        if row is None:
            fail(f"missing_provenance={path}")
        if not re.fullmatch(r"[0-9a-f]{64}", row["sha256"]):
            fail(f"sha256={path}")
        try:
            if int(row["bytes"]) <= 0 or int(row["lines"]) <= 0:
                raise ValueError
        except ValueError:
            fail(f"dimensions={path}")
        if not row["origin"] or not row["origin_id"] or not row["data_license"]:
            fail(f"origin_license={path}")
        if any(marker in row["synexia_manifest_correction"] for marker in STALE_MARKERS):
            fail(f"stale_correction={path}")

        manifest_license = source["data_license"]
        provenance_license = row["data_license"]
        for license_marker in ("Public domain", "Project Gutenberg", "MIT"):
            if license_marker in manifest_license and license_marker not in provenance_license:
                fail(f"license_mismatch={path}|marker={license_marker}")
        if "Project Gutenberg" in manifest_license:
            gutenberg += 1
        if "MIT" in manifest_license:
            mit += 1
        matched += 1

    if matched != len(provenance):
        fail(f"unmapped_provenance_rows={len(provenance) - matched}")

    donor_rows = read_rows(DONOR_REVIEW, DONOR_COLUMNS)
    proper_name_rows = [
        row for row in donor_rows
        if row["repository"] == OPENHINGLISH_REPOSITORY
    ]
    if len(proper_name_rows) != 1:
        fail("openhinglish_donor_row")
    proper_name = proper_name_rows[0]
    if (
        proper_name["commit"] != OPENHINGLISH_COMMIT
        or proper_name["disposition"] != "REFERENCE_ONLY_NO_SOURCE_COPY"
        or "proper-name" not in proper_name["role"]
    ):
        fail("openhinglish_donor_boundary")
    if not EXPORT_DOC.is_file():
        fail("missing_export_boundary_doc")
    export_doc = EXPORT_DOC.read_text(encoding="utf-8")
    for marker in (
        "## Proper-name donor boundary",
        "not copied into the M3LEX image",
        "Synexia remains the canonical source owner",
    ):
        if marker not in export_doc:
            fail(f"export_boundary_marker={marker}")

    print(
        "SYNEXIA_LEXICON_PROVENANCE_PASS"
        f"|rows={matched}|gutenberg={gutenberg}|mit={mit}|"
        "source_manifest=true|stale_markers=false|bulk_data=false|"
        "proper_name_reference_only=true"
    )


if __name__ == "__main__":
    main()
