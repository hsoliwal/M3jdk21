#!/usr/bin/env python3
"""Source proof for unresolved Synexia lexicon families staged in M3JDK."""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class SourceCheck:
    name: str
    passed: bool


_RECEIPT = Path("m3/runtime-integration/synexia-lexicon-gap-pending-receipt.tsv")
_RECIPE = Path("m3/runtime-integration/synexia-lexicon-gap-pending-recipe-20261009.yaml")
_MANIFEST = Path("m3/lexicon/synexia-source-manifest.tsv")
_FIELD_MAP = Path("m3/lexicon/synexia-precompute-field-map.tsv")
_TARGET_MANIFEST_BLOB = "da5a6d92c1a578a904b0b089c4c52b914d727200"
_TARGET_FIELD_MAP_BLOB = "c4b5313c6af5aa0eafa5b5774a2e8eae9d745b4b"
_COVERAGE_COMMIT = "b2db7b868435f1dc9217061a3610eb74a228de04"
_COVERAGE_BLOB = "164ea23596f692a8a105234f465affe851632ba2"


def _read(root: Path, path: Path) -> str:
    return (root / path).read_text(encoding="utf-8")


def _rows(content: str) -> list[dict[str, str]]:
    return list(csv.DictReader(io.StringIO(content), delimiter="\t"))


def _git_blob_sha(content: str) -> str:
    payload = content.encode("utf-8")
    return hashlib.sha1(
        b"blob " + str(len(payload)).encode("ascii") + b"\0" + payload
    ).hexdigest()


def inspect_source(root: Path) -> tuple[SourceCheck, ...]:
    receipt = _read(root, _RECEIPT)
    recipe = _read(root, _RECIPE)
    manifest = _read(root, _MANIFEST)
    field_map = _read(root, _FIELD_MAP)
    receipt_rows = _rows(receipt)
    by_id = {row.get("surface_id"): row for row in receipt_rows}
    manifest_ids = {row.get("source_id") for row in _rows(manifest)}
    return (
        SourceCheck("receiptHeader", receipt.startswith("coverage_commit\t")),
        SourceCheck("receiptRows", len(receipt_rows) == 3),
        SourceCheck("coverageCommit", all(row.get("coverage_commit") == _COVERAGE_COMMIT for row in receipt_rows)),
        SourceCheck("coveragePath", all(row.get("coverage_path") == "cognix-nlp/docs/SYNEXIA_DONOR_COVERAGE.tsv" for row in receipt_rows)),
        SourceCheck("coverageBlob", all(row.get("coverage_blob") == _COVERAGE_BLOB for row in receipt_rows)),
        SourceCheck("surfaceSet", set(by_id) == {"proper-nouns", "rapidex", "titles"}),
        SourceCheck("properStatus", by_id.get("proper-nouns", {}).get("status") == "SOURCE_ONLY_TARGET_PENDING"),
        SourceCheck("properTarget", by_id.get("proper-nouns", {}).get("target_surface") == "none in pinned target-contract receipt"),
        SourceCheck("properLaw", "instance-index/INSTANCE_OF" in by_id.get("proper-nouns", {}).get("preservation", "")),
        SourceCheck("rapidexStatus", by_id.get("rapidex", {}).get("status") == "NO_CANONICAL_OWNER"),
        SourceCheck("rapidexTarget", by_id.get("rapidex", {}).get("target_surface") == "no target surface identified"),
        SourceCheck("rapidexLaw", "Do not fabricate" in by_id.get("rapidex", {}).get("preservation", "")),
        SourceCheck("titlesStatus", by_id.get("titles", {}).get("status") == "NO_CANONICAL_OWNER"),
        SourceCheck("titlesTarget", by_id.get("titles", {}).get("target_surface") == "no target surface identified"),
        SourceCheck("titlesLaw", "Generic catalog" in by_id.get("titles", {}).get("preservation", "")),
        SourceCheck("targetManifestBlob", _git_blob_sha(manifest) == _TARGET_MANIFEST_BLOB),
        SourceCheck("targetFieldMapBlob", _git_blob_sha(field_map) == _TARGET_FIELD_MAP_BLOB),
        SourceCheck("manifestNoProper", "proper-nouns" not in manifest_ids),
        SourceCheck("manifestNoRapidex", "rapidex" not in manifest_ids),
        SourceCheck("manifestNoTitles", "titles" not in manifest_ids),
        SourceCheck("fieldNoProper", "proper-nouns" not in field_map),
        SourceCheck("fieldNoRapidex", "rapidex" not in field_map.lower()),
        SourceCheck("fieldNoTitles", "\ttitles\t" not in field_map.lower()),
        SourceCheck("recipeSource", "SYNEXIA_DONOR_COVERAGE.tsv" in recipe and _COVERAGE_BLOB in recipe),
        SourceCheck("recipeStatuses", "SOURCE_ONLY_TARGET_PENDING" in recipe and "NO_CANONICAL_OWNER" in recipe),
        SourceCheck("recipeNoArchitecture", "without fabricating an owner or target field" in recipe),
        SourceCheck("recipeRuntime", "Runtime: NOT_RUN" in recipe),
        SourceCheck("recipeMarker", "M3_LEXICON_GAP_PENDING_SOURCE_PASS checks=28/28" in recipe),
    )


def receipt_line(checks: tuple[SourceCheck, ...]) -> str:
    passed = sum(check.passed for check in checks)
    marker = "PASS" if passed == len(checks) else "FAIL"
    failed = ",".join(check.name for check in checks if not check.passed)
    suffix = "" if not failed else " failed=" + failed
    return f"M3_LEXICON_GAP_PENDING_SOURCE_{marker} checks={passed}/{len(checks)}{suffix}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", "--root", dest="root", type=Path, default=Path("."))
    args = parser.parse_args()
    try:
        checks = inspect_source(args.root.resolve())
    except (OSError, UnicodeError, csv.Error, KeyError) as error:
        print(f"M3_LEXICON_GAP_PENDING_SOURCE_FAIL error={error}", file=sys.stderr)
        return 2
    print(receipt_line(checks))
    return 0 if all(check.passed for check in checks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
