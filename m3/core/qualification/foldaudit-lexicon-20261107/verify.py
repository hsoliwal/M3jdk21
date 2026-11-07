#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Check FOLDAUDIT_RECORDS v1: the mechanical facts behind DECISION.tsv (fold-close audit, m3/core lexicon family).

Read-only, git only (works in a sparse or blobless checkout: it reads the HEAD tree, not the worktree). For every
row: the origin commit and the dropping merge resolve, and the dropping merge is an ancestor of HEAD. For every cited
master commit: it is an ancestor of HEAD. For every content claim: a pattern is present in, or absent from, a file
(or every file under a directory) of HEAD. Prints one line per failed probe and a summary; exit 1 on any failure.

usage: python -I verify.py [--root DIR]
"""
from __future__ import annotations

import argparse
import csv
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
TEXT = "m3/core/src/jdk/m3/text/"
PAYLOAD = TEXT + "M3PrecomputePayload.java"
EXPORT = "m3/runtime-integration/export-m3-lexicon.py"
VERIFY = "m3/runtime-integration/verify-m3-lexicon.py"
EXPORT_TEST = "m3/runtime-integration/tests/test_m3_export.py"

# commits each decision cites as deliberate master evidence (must be ancestors of HEAD)
EVIDENCE_COMMITS = {
    "1032019bd51": "acronyms, phrases, proper names and titles unadmitted (M3_EXPORT.md)",
    "3b2c672321c": "number family bound to M3NumberSpace without language_tag",
    "47a6ac24890": "generated number export envelope",
    "20866d855e0": "SI-unit precompute decoder (findSiUnitPrecompute)",
    "5761adde29f": "source revision and git blob digests verified (m3-word-source-revisions.tsv)",
    "8957cf61a35": "lexicon data manifest rows with m3_commit/m3_blob",
    "cf6fa2c7261": "M3CorpusSourceReceipt custody record",
    "9b3568b671e": "typed five-family sidecar receiver (SourceScope rows)",
    "2479adf1cf1": "LangDex source envelope, receipt-only intake",
    "3fc51950678": "LangDex precomputes preserved by payload profile owner",
    "5066fa4ad9b": "tested instance-index contract (M3InstanceIndexPrecompute only)",
    "d7e70d93660": "proper-name instance contract mapped (no title catalog, no second interner)",
    "61fbc2f3f51": "n-grams as a sealed corpus plane (M3LanguageCorpusManifest.Plane.WORD_NGRAM)",
    "0b9b0ffd8c5": "m3/lexicon receiver module carries the M3Lexicons family name",
    "26320a320fe": "manifest target labels predate the acronym stack",
    "d58ff13e954": "#2447 origin of the restored field-name pattern",
    "b65fef768d34": "20261106l landing (#2489) cited by the #2424 close",
    "c9a1d41baaf": "JDK-naming fold that kept master's String.matches line",
    "e397b650272": "exporter admits the String payload type (master's own 2026-10-11 fix)",
    "bf04bd059ae": "exporter scopes precompute types to source owners",
    "5f4643c9393": "exporter composite translation identity lineage",
    "3b5db9f5629": "directed related-lexeme sidecar (relation families on master)",
    "8225a2eebda": "relation rows bound to admitted source families",
    "8061d9bd8527": "ancestry-only merge of heads 481-560 (#513, #546, #564)",
    "ddf4bb64dbac": "ancestry-only merge of heads 561-640 (#568)",
    "1ef8401d4e6a": "ancestry-only merge of heads 641-720 (#626, #627, #637, #646, #650)",
}

# (leaf, path or directory/, pattern, expected present?)
PROBES = (
    ("L0002", TEXT, "class M3InstanceIndex {", False), ("L0002", TEXT + "M3InstanceIndexPrecompute.java", "class M3InstanceIndexPrecompute", True),
    ("L0002", "m3/lexicon/M3_EXPORT.md", "No second lexical interner or title catalog is admitted", True),
    ("L0003", TEXT, "class M3Lexicons ", False), ("L0003", "m3/lexicon/receiver/README.md", "M3Lexicons receiver", True),
    ("L0009", TEXT, "M3PhrasePrecompute", False), ("L0022", TEXT, "M3PhraseSidecarCatalog", False),
    ("L0009", "m3/lexicon/M3_EXPORT.md", "proper names, titles, locations, phrases, and", True),
    ("L0009", "m3/lexicon/m3-source-manifest.tsv", "phrase", False),
    ("L0010", TEXT, "M3LanguageNgramPrecompute", False), ("L0010", TEXT + "M3LanguageCorpusManifest.java", "WORD_NGRAM", True),
    ("L0010", "m3/lexicon/m3-source-manifest.tsv", "ngram", False),
    ("L0011", "m3/core/src/", "AcronymSidecarCatalog", False), ("L0016", TEXT, "M3AcronymPrecompute", False),
    ("L0011", "m3/lexicon/M3_EXPORT.md", "records remain unadmitted until each source snapshot has an exact revision,", True),
    ("L0012", TEXT + "SharedLexiconFamilySidecarCatalog.java", "m3.records.tsv", False),
    ("L0012", TEXT + "SharedLexiconFamilySidecarCatalog.java", "record SourceScope(String sourceId, String recordId,", True),
    ("L0012", TEXT + "SharedLexiconFamilySidecarCatalog.java", "CRLF sidecar is not canonical", True),
    ("L0015", TEXT, "M3LangDexPrecompute", False), ("L0019", TEXT, "SharedLangDexPrecomputeCatalog", False),
    ("L0015", TEXT + "M3LangDexSourceEnvelope.java", "class M3LangDexSourceEnvelope", True),
    ("L0015", PAYLOAD, "\"langdex_concept_id\"", True),
    ("L0015", "m3/lexicon/M3_EXPORT.md", "A successful receipt is not catalog admission", True),
    ("L0018", "m3/core/src/", "M3PositionMaskFacts", False), ("L0018", "m3/core/test/", "M3PositionMaskFacts", False),
    ("L0018", "m3/lexicon/", "position-mask", False),
    ("L0021", TEXT, "record AcronymPrecompute", False), ("L0021", TEXT, "record NumberPrecompute", False),
    ("L0021", TEXT, "sourceBlobSha", False), ("L0023", TEXT, "sourceBound()", False),
    ("L0021", "m3/lexicon/m3-word-source-revisions.tsv", "source_id\tsource_revision\tsource_blob_sha", True),
    ("L0021", "m3/lexicon/m3-lexicon-data-manifest-20261018.tsv", "source_id\tsynexia_path\tsynexia_commit\tsynexia_blob", True),
    ("L0021", TEXT + "M3CorpusSourceReceipt.java", "record M3CorpusSourceReceipt", True),
    ("L0023", TEXT, "record AcronymIdentity", False), ("L0023", TEXT, "record SiUnitIdentity", False),
    ("L0023", TEXT + "SharedLexiconCatalog.java", "findSiUnitPrecompute(", True),
    ("L0023", TEXT + "SharedLexiconPrecomputeCatalog.java",
     "public record TranslationIdentity(String sourceId, String recordId,", True),
    ("L0042", TEXT + "M3NumberSpace.java", "forLanguage(", False), ("L0042", TEXT + "M3NumberSpace.java", "SOURCE_BLOB_SHA", False),
    ("L0042", TEXT + "M3NumberSpace.java", "public int precomputedValueCount()", True),
    ("L0042", TEXT + "M3NumberSpace.java", "public StorageIdentity storageIdentity()", True),
    ("L0042", "m3/lexicon/M3_EXPORT.md", "The source itself has no `language_tag` field", True),
    ("L0064", PAYLOAD, "FIELD_NAME_PATTERN.matcher(name).matches()", True),
    ("L0064", PAYLOAD, "Pattern.compile(\"[a-z][a-z0-9_]*\")", True),
    ("L0064", PAYLOAD, "name.matches(", False),
    ("L0403", EXPORT, "def load_family_sidecars", False), ("L0403", EXPORT, "def read_identity_field_map", False),
    ("L0403", EXPORT, "def _is_decimal_int", False), ("L0403", EXPORT, "def unambiguous_field_types", True),
    ("L0403", EXPORT, "def load_relations", True), ("L0403", EXPORT, "def validate_translation_record_identity", True),
    ("L0403", EXPORT, "\"byte[]\"", False),
    ("L0326", VERIFY, "def verify_family_sidecar_scope", False), ("L0326", VERIFY, "def read_identity_map_contract", False),
    ("L0326", VERIFY, "def _fits_any_donor_type", False), ("L0326", VERIFY, "precompute_field_types_by_source", True),
    ("L0083", EXPORT_TEST, "def test_typed_family_sidecars_round_trip_and_reject_partial", False),
    ("L0083", EXPORT_TEST, "def write_family_bundle", False),
    ("L0083", EXPORT_TEST, "def test_composite_owner_shapes_and_overloaded_field_types_are_admitted", False),
    ("L0083", EXPORT_TEST, "def test_directed_related_lexeme_sidecar_round_trips_and_requires_input", True),
    ("L0083", EXPORT_TEST, "def test_java_catalog_opens_actual_python_export", True),
)


def git(root: Path, *args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["git", "-C", str(root), *args], capture_output=True, text=True, encoding="utf-8")


def ancestor(root: Path, commit: str) -> bool:
    return git(root, "merge-base", "--is-ancestor", commit, "HEAD").returncode == 0


def present(root: Path, path: str, pattern: str) -> bool:
    if path.endswith("/"):
        return git(root, "grep", "-q", "-F", "-e", pattern, "HEAD", "--", path).returncode == 0
    shown = git(root, "show", "HEAD:" + path)
    if shown.returncode != 0:
        raise SystemExit("missing file at HEAD: " + path)
    return pattern in shown.stdout


def first_commit(cell: str) -> str:
    return cell.split()[0].split(",")[0]


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    root = Path(parser.parse_args(argv).root).resolve()
    failures, checks = [], 0
    with open(RECIPE_DIR / "DECISION.tsv", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    for row in rows:
        origin, merge = first_commit(row["origin"]), first_commit(row["dropping_merge"])
        for commit in (origin, merge):
            checks += 1
            if git(root, "cat-file", "-e", commit + "^{commit}").returncode != 0:
                failures.append("leaf %s: commit %s does not resolve" % (row["leaf"], commit))
        checks += 1
        if not ancestor(root, merge):
            failures.append("leaf %s: dropping merge %s is not an ancestor of HEAD" % (row["leaf"], merge))
    for commit, why in EVIDENCE_COMMITS.items():
        checks += 1
        if not ancestor(root, commit):
            failures.append("evidence %s (%s) is not an ancestor of HEAD" % (commit, why))
    for leaf, path, pattern, expected in PROBES:
        checks += 1
        if present(root, path, pattern) != expected:
            failures.append("leaf %s: %r %s in %s" % (leaf, pattern, "missing" if expected else "present", path))
    for failure in failures:
        print("FAIL " + failure)
    decided = sorted({row["decision"] for row in rows})
    print("FOLDAUDIT_RECORDS verify v1 rows=%d decisions=%s checks=%d failures=%d" % (
        len(rows), ",".join(decided), checks, len(failures)))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
