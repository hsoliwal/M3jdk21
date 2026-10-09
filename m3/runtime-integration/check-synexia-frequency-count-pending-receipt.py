#!/usr/bin/env python3
"""Source proof for the pending Synexia frequency-count receiver mapping."""

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


_RECEIPT = Path("m3/runtime-integration/synexia-frequency-count-pending-receipt.tsv")
_RECIPE = Path("m3/runtime-integration/synexia-frequency-count-pending-recipe-20261009.yaml")
_MANIFEST = Path("m3/lexicon/synexia-source-manifest.tsv")
_FIELD_MAP = Path("m3/lexicon/synexia-precompute-field-map.tsv")
_TARGET_MANIFEST_BLOB = "da5a6d92c1a578a904b0b089c4c52b914d727200"
_TARGET_FIELD_MAP_BLOB = "c4b5313c6af5aa0eafa5b5774a2e8eae9d745b4b"
_COMMIT = "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a"
_BLOB = "05ccf20cd2e3789ddb849b14f0374aa089d90cc5"
_SHA256 = "7fea67ab954e2c01df6c608c9826e594cf36f8823b3243554f88245fb75dc506"
_SOURCE_MAPPING_PR = "10047"
_SOURCE_MAPPING_BRANCH = "codex/frequency-count-mapping-reconcile-20261009"
_SOURCE_MAPPING_HEAD = "6d38638534c08672bf6d50343e7edb857968e57a"
_SOURCE_MAPPING_RECEIPT_PATH = "cognix-nlp/docs/SYNEXIA_FREQUENCY_COUNT_SOURCE_RECEIPT.tsv"
_SOURCE_MAPPING_RECEIPT_BLOB = "e01f44c0cde27d8fa1c33d0eccb0bf8692a3f515"
_SOURCE_MAPPING_PATH = "cognix-nlp/docs/SYNEXIA_TO_M3JDK_MAPPING.tsv"
_SOURCE_MAPPING_BLOB = "e80b5b934410fe3d0b7da12135c91f5e1550ac65"
_SOURCE_MAPPING_RECIPE_PATH = "synexia-m3-recipe/recipes/frequency-count-mapping-reconcile-20261009.yaml"
_SOURCE_MAPPING_RECIPE_BLOB = "ace136aeba2dd553015da07f246366e1e7555f46"


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
    manifest_rows = [
        row for row in _rows(manifest)
        if row.get("source_id") == "dictlang.frequency"
    ]
    frequency_row = manifest_rows[0] if manifest_rows else {}
    frequency_lines = [
        line for line in field_map.splitlines()
        if "frequency" in line.lower() or "rank" in line.lower()
    ]
    return (
        SourceCheck("receiptHeader", receipt.startswith("donor_commit\t")),
        SourceCheck("receiptRows", len(receipt_rows) == 1),
        SourceCheck("pinnedCommit", receipt_rows[0].get("donor_commit") == _COMMIT),
        SourceCheck("pinnedPath", receipt_rows[0].get("donor_path", "").endswith("en_full_frequency.txt")),
        SourceCheck("pinnedBlob", receipt_rows[0].get("donor_blob") == _BLOB),
        SourceCheck("pinnedContentSha", receipt_rows[0].get("source_sha256") == _SHA256),
        SourceCheck("sample", receipt_rows[0].get("verified_sample") == "you 28787591"),
        SourceCheck("payloadField", receipt_rows[0].get("canonical_payload_field") == "corpus_count"),
        SourceCheck("blocked", receipt_rows[0].get("target_status") == "BLOCKED_UNTIL_SOURCE_PR_ACCEPTED"),
        SourceCheck("preservation", "not frequency_rank" in receipt_rows[0].get("preservation", "")),
        SourceCheck("manifestRow", len(manifest_rows) == 1),
        SourceCheck("manifestSource", "en_full_frequency.txt" in frequency_row.get("synexia_path", "")),
        SourceCheck("manifestStaleRank", frequency_row.get("mapping_fields") == "lexeme,frequency_rank"),
        SourceCheck("manifestNotCount", "corpus_count" not in frequency_row.get("mapping_fields", "")),
        SourceCheck("manifestAttribution", "MIT" in frequency_row.get("data_license", "") and "OpenSubtitles" in frequency_row.get("data_license", "")),
        SourceCheck("fieldRank", any("frequencyRank" in line for line in frequency_lines)),
        SourceCheck("fieldCanonicalRank", any("frequency_rank" in line for line in frequency_lines)),
        SourceCheck("fieldMapped", all("MAPPED" in line for line in frequency_lines)),
        SourceCheck("targetManifestBlob", _git_blob_sha(manifest) == _TARGET_MANIFEST_BLOB),
        SourceCheck("targetFieldMapBlob", _git_blob_sha(field_map) == _TARGET_FIELD_MAP_BLOB),
        SourceCheck("recipeDonor", "en_full_frequency.txt" in recipe and _BLOB in recipe and _SHA256 in recipe),
        SourceCheck("recipeSemantics", "non-negative OpenSubtitles-derived corpus count" in recipe),
        SourceCheck("recipeForbidden", "forbidden_authority: frequency_rank" in recipe),
        SourceCheck("recipeBlocked", "BLOCKED_UNTIL_SOURCE_PR_ACCEPTED" in recipe),
        SourceCheck("recipeNoPayload", "No frequency corpus bytes are copied into java.lang.String storage." in recipe),
        SourceCheck("recipeRuntime", "Runtime: NOT_RUN" in recipe),
        SourceCheck("sourceMappingPR", receipt_rows[0].get("source_mapping_pr") == _SOURCE_MAPPING_PR),
        SourceCheck("sourceMappingBranch", receipt_rows[0].get("source_mapping_branch") == _SOURCE_MAPPING_BRANCH),
        SourceCheck("sourceMappingHead", receipt_rows[0].get("source_mapping_head") == _SOURCE_MAPPING_HEAD),
        SourceCheck("sourceMappingReceiptPath", receipt_rows[0].get("source_mapping_receipt_path") == _SOURCE_MAPPING_RECEIPT_PATH),
        SourceCheck("sourceMappingReceiptBlob", receipt_rows[0].get("source_mapping_receipt_blob") == _SOURCE_MAPPING_RECEIPT_BLOB),
        SourceCheck("sourceMappingPath", receipt_rows[0].get("source_mapping_path") == _SOURCE_MAPPING_PATH),
        SourceCheck("sourceMappingBlob", receipt_rows[0].get("source_mapping_blob") == _SOURCE_MAPPING_BLOB),
        SourceCheck("sourceMappingRecipePath", receipt_rows[0].get("source_mapping_recipe_path") == _SOURCE_MAPPING_RECIPE_PATH),
        SourceCheck("sourceMappingRecipeBlob", receipt_rows[0].get("source_mapping_recipe_blob") == _SOURCE_MAPPING_RECIPE_BLOB),
        SourceCheck("sourceMappingStatus", receipt_rows[0].get("source_mapping_status") == "SOURCE_MAPPING_RECONCILED"),
        SourceCheck("sourceMappingState", receipt_rows[0].get("source_mapping_state") == "OPEN"),
        SourceCheck("recipeMappingPR", "pull_request: 10047" in recipe),
        SourceCheck("recipeMappingSource", _SOURCE_MAPPING_HEAD in recipe and _SOURCE_MAPPING_PR in recipe),
        SourceCheck("recipeMappingReceipts", _SOURCE_MAPPING_RECEIPT_PATH in recipe and _SOURCE_MAPPING_RECEIPT_BLOB in recipe),
        SourceCheck("recipeMappingPaths", _SOURCE_MAPPING_PATH in recipe and _SOURCE_MAPPING_BLOB in recipe),
        SourceCheck("recipeMappingRecipe", _SOURCE_MAPPING_RECIPE_PATH in recipe and _SOURCE_MAPPING_RECIPE_BLOB in recipe),
        SourceCheck("recipeMappingOpen", "state: OPEN" in recipe and "remains OPEN" in recipe),
        SourceCheck("recipeNoPromotion", "BLOCKED_UNTIL_SOURCE_PR_ACCEPTED" in recipe and "remain blocked until Synexia PR #10047 is accepted" in recipe),
        SourceCheck("recipeMarker", "M3_FREQUENCY_COUNT_PENDING_SOURCE_PASS checks=44/44" in recipe),
    )


def receipt_line(checks: tuple[SourceCheck, ...]) -> str:
    passed = sum(check.passed for check in checks)
    marker = "PASS" if passed == len(checks) else "FAIL"
    failed = ",".join(check.name for check in checks if not check.passed)
    suffix = "" if not failed else " failed=" + failed
    return f"M3_FREQUENCY_COUNT_PENDING_SOURCE_{marker} checks={passed}/{len(checks)}{suffix}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", "--root", dest="root", type=Path, default=Path("."))
    args = parser.parse_args()
    try:
        checks = inspect_source(args.root.resolve())
    except (OSError, UnicodeError, csv.Error, KeyError) as error:
        print(f"M3_FREQUENCY_COUNT_PENDING_SOURCE_FAIL error={error}", file=sys.stderr)
        return 2
    print(receipt_line(checks))
    return 0 if all(check.passed for check in checks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
