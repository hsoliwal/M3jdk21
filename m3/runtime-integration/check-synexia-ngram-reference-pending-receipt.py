#!/usr/bin/env python3
"""Source proof for the reference-only Synexia n-gram handoff."""

from __future__ import annotations

import argparse
import csv
import io
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class SourceCheck:
    name: str
    passed: bool


_RECEIPT = Path("m3/runtime-integration/synexia-ngram-reference-pending-receipt.tsv")
_RECIPE = Path("m3/runtime-integration/synexia-ngram-reference-pending-recipe-20261009.yaml")
_MANIFEST = Path("m3/lexicon/synexia-source-manifest.tsv")
_FIELD_MAP = Path("m3/lexicon/synexia-precompute-field-map.tsv")

_EXPECTED = {
    "source_repo": "hsoliwal/com.synexia",
    "source_pr": "10050",
    "source_branch": "codex/m3-ngram-reference-boundary-20261009",
    "source_head": "e838ee73303a61b9f797427c95b9e79f5fc2f384",
    "source_receipt_path": "cognix-nlp/docs/SYNEXIA_NGRAM_REFERENCE_RECEIPT_20261009.tsv",
    "source_receipt_blob": "7bb50a8dd7feea23aeec12a91edd7d6de1779115",
    "mapping_commit": "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a",
    "mapping_path": "cognix-nlp/docs/SYNEXIA_TO_M3JDK_MAPPING.tsv",
    "mapping_blob": "dad67bb0fb4ea7141f6f26536ab20e5ec082ddd6",
    "reference_donor": "taiwan-corpora/twngrams",
    "reference_url": "https://huggingface.co/datasets/taiwan-corpora/twngrams",
    "reference_license": "CC0-1.0",
    "reference_revision": "0.1.0",
    "reference_schema": "hosts+token_1gram..token_4gram",
    "reference_rows": "1033916",
    "reference_count_semantics": "distinct-hosts-not-occurrences",
    "admission": "REFERENCE_ONLY",
    "target_status": "NO_NGRAM_RECEIVER",
}


def _read(root: Path, relative: Path) -> str:
    return (root / relative).read_text(encoding="utf-8")


def _rows(content: str) -> list[dict[str, str]]:
    return list(csv.DictReader(io.StringIO(content), delimiter="\t"))


def inspect_source(root: Path) -> tuple[SourceCheck, ...]:
    receipt = _read(root, _RECEIPT)
    recipe = _read(root, _RECIPE)
    manifest = _read(root, _MANIFEST)
    field_map = _read(root, _FIELD_MAP)
    receipt_rows = _rows(receipt)
    header = receipt.splitlines()[0].split("\t") if receipt.splitlines() else []
    row = receipt_rows[0] if len(receipt_rows) == 1 else {}
    manifest_rows = _rows(manifest)
    field_rows = _rows(field_map)
    manifest_ids = {item.get("source_id", "") for item in manifest_rows}
    field_map_lower = field_map.lower()
    return (
        SourceCheck("receiptHeader", header == list(_EXPECTED)),
        SourceCheck("receiptRows", len(receipt_rows) == 1),
        SourceCheck("receiptPins", all(row.get(key) == value for key, value in _EXPECTED.items())),
        SourceCheck("sourceHeadShape", len(_EXPECTED["source_head"]) == 40 and all(c in "0123456789abcdef" for c in _EXPECTED["source_head"])),
        SourceCheck("sourceReceiptBlobShape", len(_EXPECTED["source_receipt_blob"]) == 40 and all(c in "0123456789abcdef" for c in _EXPECTED["source_receipt_blob"])),
        SourceCheck("mappingBlobShape", len(_EXPECTED["mapping_blob"]) == 40 and all(c in "0123456789abcdef" for c in _EXPECTED["mapping_blob"])),
        SourceCheck("donorUrl", "https://huggingface.co/datasets/taiwan-corpora/twngrams" in receipt),
        SourceCheck("donorRights", "CC0-1.0" in receipt and "0.1.0" in receipt),
        SourceCheck("donorSchema", "hosts+token_1gram..token_4gram" in receipt),
        SourceCheck("donorRows", "1033916" in receipt),
        SourceCheck("donorSemantics", "distinct-hosts-not-occurrences" in receipt),
        SourceCheck("referenceOnly", "REFERENCE_ONLY" in receipt and "reference plane" in receipt),
        SourceCheck("sourceCommitInRecipe", "e838ee73303a61b9f797427c95b9e79f5fc2f384" in recipe and "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a" in recipe),
        SourceCheck("sourceReceiptInRecipe", "m3/runtime-integration/synexia-ngram-reference-pending-receipt.tsv" in recipe),
        SourceCheck("synexiaInputs", "cognix-nlp/docs/SYNEXIA_NGRAM_REFERENCE_RECEIPT_20261009.tsv" in recipe and "cognix-nlp/docs/SYNEXIA_TO_M3JDK_MAPPING.tsv" in recipe),
        SourceCheck("targetInputs", "m3/lexicon/synexia-source-manifest.tsv" in recipe and "m3/lexicon/synexia-precompute-field-map.tsv" in recipe),
        SourceCheck("manifestHasNoNgramId", all("ngram" not in item.lower() for item in manifest_ids)),
        SourceCheck("fieldMapHasNoNgramReceiver", "ngram" not in field_map_lower),
        SourceCheck("noDownloader", "no downloader" in recipe.lower() or "downloader execution" in recipe.lower()),
        SourceCheck("noPayloadCopy", "no external n-gram bytes are copied" in recipe.lower() and "no n-gram bytes enter" in recipe.lower()),
        SourceCheck("noIdentitySubstitution", "identity substitution" in recipe.lower() or "n-gram identity" in recipe.lower()),
        SourceCheck("noReceiverClaim", "NO_NGRAM_RECEIVER" in recipe and "No n-gram receiver is claimed" in recipe),
        SourceCheck("referenceBoundary", "EXTERNAL_REFERENCE_ONLY" in recipe and "REFERENCE_ONLY" in recipe),
        SourceCheck("runtimeBoundary", "Runtime: NOT_RUN" in recipe and "Hosted CI: NOT_CLAIMED" in recipe and "Benchmark: NOT_MEASURED" in recipe),
        SourceCheck("storageBoundary", "minimum_free_after_output: 8GiB" in recipe and "hosted_artifact_limit: 512MiB" in recipe),
        SourceCheck("markerContract", "M3_NGRAM_REFERENCE_PENDING_SOURCE_PASS checks=26/26" in recipe),
    )


def receipt_line(checks: tuple[SourceCheck, ...]) -> str:
    passed = sum(check.passed for check in checks)
    marker = "PASS" if passed == len(checks) else "FAIL"
    return f"M3_NGRAM_REFERENCE_PENDING_SOURCE_{marker} checks={passed}/{len(checks)}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", "--root", dest="root", type=Path, default=Path("."))
    args = parser.parse_args()
    try:
        checks = inspect_source(args.root.resolve())
    except (OSError, UnicodeError, csv.Error, KeyError, IndexError) as error:
        print(f"M3_NGRAM_REFERENCE_PENDING_SOURCE_FAIL error={error}", file=sys.stderr)
        return 2
    print(receipt_line(checks))
    return 0 if all(check.passed for check in checks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
