#!/usr/bin/env python3
"""Source-bound fail-closed proof for the unresolved Rapidex family."""

from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path

RECEIPT = Path("m3/runtime-integration/synexia-rapidex-gap-receipt.tsv")
RECIPE = Path("m3/runtime-integration/synexia-rapidex-gap-recipe-20261010.yaml")
MANIFEST = Path("m3/lexicon/synexia-source-manifest.tsv")
FIELD_MAP = Path("m3/lexicon/synexia-precompute-field-map.tsv")

EXPECTED = {
    "source_repo": "hsoliwal/com.synexia",
    "source_pr": "10041",
    "source_head": "b2db7b868435f1dc9217061a3610eb74a228de04",
    "coverage_path": "cognix-nlp/docs/SYNEXIA_DONOR_COVERAGE.tsv",
    "coverage_blob": "164ea23596f692a8a105234f465affe851632ba2",
    "surface_id": "rapidex",
    "status": "NO_CANONICAL_OWNER",
    "target_surface": "no target surface identified",
}

EXPECTED_BLOBS = {
    MANIFEST: "750fdfc70dd897abb767e89b5bf1a58b6cfb8e98",
    FIELD_MAP: "1b55d56aad7cf52a117fba7266862931777965f5",
}

def git_blob_sha(value: str) -> str:
    raw = value.encode("utf-8")
    return hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()

def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))

def check(root: Path) -> tuple[str, ...]:
    receipt = root / RECEIPT
    recipe = root / RECIPE
    manifest = root / MANIFEST
    field_map = root / FIELD_MAP
    receipt_rows = rows(receipt)
    recipe_text = recipe.read_text(encoding="utf-8")
    manifest_text = manifest.read_text(encoding="utf-8")
    field_map_text = field_map.read_text(encoding="utf-8")
    row = receipt_rows[0] if len(receipt_rows) == 1 else {}
    checks: list[tuple[str, bool]] = [
        ("receipt_count", len(receipt_rows) == 1),
        ("receipt_identity", all(row.get(k) == v for k, v in EXPECTED.items())),
        ("receipt_preservation", "Do not fabricate" in row.get("preservation", "")),
        ("receipt_admission", "canonical Rapidex owner" in row.get("admission", "")),
        ("manifest_blob", git_blob_sha(manifest_text) == EXPECTED_BLOBS[MANIFEST]),
        ("field_map_blob", git_blob_sha(field_map_text) == EXPECTED_BLOBS[FIELD_MAP]),
        ("manifest_no_rapidex", "rapidex" not in manifest_text.lower()),
        ("field_map_no_rapidex", "rapidex" not in field_map_text.lower()),
        ("recipe_source", EXPECTED["source_head"] in recipe_text and EXPECTED["coverage_blob"] in recipe_text),
        ("recipe_no_owner", "target_owner: none" in recipe_text),
        ("recipe_no_payload", "payload: NOT_ADMITTED" in recipe_text),
        ("recipe_fail_closed", "without fabricating an owner" in recipe_text),
        ("recipe_runtime", "runtime: NOT_RUN" in recipe_text),
        ("recipe_hosted", "hosted_ci: NOT_CLAIMED" in recipe_text),
    ]
    return tuple(name for name, passed in checks if not passed), tuple(name for name, _ in checks)

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", "--root", dest="root", type=Path, default=Path("."))
    args = parser.parse_args()
    failed, all_checks = check(args.root.resolve())
    marker = "PASS" if not failed else "FAIL"
    suffix = "" if not failed else " failed=" + ",".join(failed)
    print(f"M3_RAPIDEX_GAP_SOURCE_{marker} checks={len(all_checks)-len(failed)}/{len(all_checks)}{suffix}")
    return 0 if not failed else 1

if __name__ == "__main__":
    raise SystemExit(main())
