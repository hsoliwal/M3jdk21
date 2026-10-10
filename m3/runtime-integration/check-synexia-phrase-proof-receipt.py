#!/usr/bin/env python3
"""Source proof for the pinned Synexia phrase receiver boundary."""

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


_RECEIPT = Path("m3/runtime-integration/synexia-phrase-proof-receipt.tsv")
_RECIPE = Path("m3/runtime-integration/synexia-phrase-proof-recipe-20261009.yaml")
_MANIFEST = Path("m3/lexicon/synexia-source-manifest.tsv")
_FIELD_MAP = Path("m3/lexicon/synexia-precompute-field-map.tsv")
_OWNER = Path("m3/core/src/com/m3/text/M3PhrasePrecompute.java")
_TEST = Path("m3/core/test/M3PhrasePrecomputeTest.java")
_BUILD = Path("m3/build.sh")
_COMMIT = "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a"
_DONOR_BLOB = "a06f5493c9ac07de3f881dbef701c72255141eb9"
_TARGET_BLOBS = {
    "manifest": "f00804fe35759e2a44be7ae9ee6d43bbc43437e8",
    "field_map": "b62e90876aab11164ce5468068fc451fc9dfc10c",
    "owner": "4c91f0f7b3ff02668b49d5bfbb911df20794f8ce",
    "test": "f11e48ac03b344645cd329f8691ad1a28e06e8b9",
    "build": "4966190e61ac786b3c31fe6a2aaee53d88c3249a",
}
_RECIPE_MANIFEST = Path("m3/recipes/manifest.json")
_RECIPE_MANIFEST_BLOB = "5bc51b2dc42a9b8ce509d4a6c1208631842734e8"


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
    owner = _read(root, _OWNER)
    test = _read(root, _TEST)
    build = _read(root, _BUILD)
    recipe_manifest = _read(root, _RECIPE_MANIFEST)
    receipt_rows = _rows(receipt)
    manifest_rows = [r for r in _rows(manifest) if r.get("source_id") == "translate.index-phrases"]
    return (
        SourceCheck("receiptHeader", receipt.startswith("donor_commit\t")),
        SourceCheck("receiptRows", len(receipt_rows) == 1),
        SourceCheck("pinnedCommit", receipt_rows[0].get("donor_commit") == _COMMIT),
        SourceCheck("pinnedDonorPath", receipt_rows[0].get("donor_path", "").endswith("IndexPhraseTable.java")),
        SourceCheck("pinnedDonorBlob", receipt_rows[0].get("donor_blob") == _DONOR_BLOB),
        SourceCheck("manifestPhraseRow", len(manifest_rows) == 1),
        SourceCheck("manifestSourcePath", "IndexPhraseTable.java" in manifest_rows[0].get("synexia_path", "")),
        SourceCheck("manifestFields", manifest_rows[0].get("mapping_fields") == "source_token_ids,target_token_ids"),
        SourceCheck("manifestOwner", "M3PhrasePrecompute.Scope" in manifest_rows[0].get("precompute_target", "")),
        SourceCheck("manifestLicense", manifest_rows[0].get("data_license") == "Apache-2.0"),
        SourceCheck("manifestScope", "vocabulary-space identity" in manifest_rows[0].get("data_policy", "")),
        SourceCheck("manifestTargetFields", "vocabulary_fingerprint" in manifest_rows[0].get("precompute_fields", "")),
        SourceCheck("fieldSpace", "IndexPhraseTable\tspace\tIndexStringSpace\tvocabulary_fingerprint" in field_map),
        SourceCheck("fieldScope", "M3PhrasePrecompute.Scope\tsourceId" in field_map and "M3PhrasePrecompute.Scope\tvocabularyFingerprint" in field_map),
        SourceCheck("fieldPhrase", "M3PhrasePrecompute.Phrase\tsourceTokenIds" in field_map and "M3PhrasePrecompute.Phrase\ttargetTokenIds" in field_map),
        SourceCheck("fieldMapped", all("MAPPED" in line for line in field_map.splitlines() if "M3PhrasePrecompute." in line or "IndexPhraseTable\tspace" in line)),
        SourceCheck("ownerScope", "record Scope" in owner and "vocabularyFingerprint" in owner),
        SourceCheck("ownerPhrase", "public static final class Phrase" in owner and "targetTokenIds" in owner),
        SourceCheck("ownerLongest", "longestMatchAt" in owner),
        SourceCheck("ownerScopedLookup", "Scope scope" in owner),
        SourceCheck("testScopeIsolation", "foreign" in test or "other" in test),
        SourceCheck("testEmptyReplacement", "empty" in test),
        SourceCheck("testLongest", "longest" in test.lower()),
        SourceCheck("buildModes", "for mode in jit int nocompact c2" in build),
        SourceCheck("buildTest", "M3PhrasePrecomputeTest" in build),
        SourceCheck("manifestBlob", _git_blob_sha(manifest) == _TARGET_BLOBS["manifest"]),
        SourceCheck("fieldMapBlob", _git_blob_sha(field_map) == _TARGET_BLOBS["field_map"]),
        SourceCheck("ownerBlob", _git_blob_sha(owner) == _TARGET_BLOBS["owner"]),
        SourceCheck("testBlob", _git_blob_sha(test) == _TARGET_BLOBS["test"]),
        SourceCheck("buildBlob", _git_blob_sha(build) == _TARGET_BLOBS["build"]),
        SourceCheck("recipeManifestBlob", _git_blob_sha(recipe_manifest) == _RECIPE_MANIFEST_BLOB),
        SourceCheck("recipeDonor", "IndexPhraseTable.java" in recipe and "a06f5493c9ac07de3f881dbef701c72255141eb9" in recipe),
        SourceCheck("recipePending", "target_status: OPEN_DRAFT" in recipe),
        SourceCheck("recipeRuntime", "Runtime: NOT_RUN" in recipe),
        SourceCheck("recipeModes", "jit, int, nocompact, and c2" in recipe),
        SourceCheck("recipeNoCorpus", "No external phrase corpus" in recipe),
        SourceCheck("recipeNoString", "No phrase payload is stored in java.lang.String." in recipe),
        SourceCheck("recipeMarker", "M3JDK_PHRASE_PRECOMPUTE_PASS" in recipe),
    )


def receipt_line(checks: tuple[SourceCheck, ...]) -> str:
    passed = sum(check.passed for check in checks)
    marker = "PASS" if passed == len(checks) else "FAIL"
    failed = ",".join(check.name for check in checks if not check.passed)
    suffix = "" if not failed else " failed=" + failed
    return f"M3_PHRASE_PROOF_RECEIPT_SOURCE_{marker} checks={passed}/{len(checks)}{suffix}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", "--root", dest="root", type=Path, default=Path("."))
    args = parser.parse_args()
    try:
        checks = inspect_source(args.root.resolve())
    except (OSError, UnicodeError, csv.Error, KeyError) as error:
        print(f"M3_PHRASE_PROOF_RECEIPT_SOURCE_FAIL error={error}", file=sys.stderr)
        return 2
    print(receipt_line(checks))
    return 0 if all(check.passed for check in checks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
