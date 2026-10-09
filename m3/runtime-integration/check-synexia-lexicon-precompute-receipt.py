#!/usr/bin/env python3
"""Source proof for the pinned Synexia lexicon/precompute receipt."""

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


_RECEIPT = Path("m3/lexicon/synexia-lexicon-precompute-receipt.tsv")
_RECIPE = Path("m3/runtime-integration/synexia-lexicon-precompute-receipt-20261009.yaml")
_MANIFEST = Path("m3/lexicon/synexia-source-manifest.tsv")
_FIELD_MAP = Path("m3/lexicon/synexia-precompute-field-map.tsv")
_COMMIT = "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a"
_DOCS = {
    "cognix-nlp/docs/SYNEXIA_TO_M3JDK_MAPPING.tsv":
        "dad67bb0fb4ea7141f6f26536ab20e5ec082ddd6",
    "cognix-nlp/docs/SYNEXIA_TO_M3JDK_TYPED_PRECOMPUTE_EXTENSION.tsv":
        "aed9520137bb6fa369dde385c35264b481762661",
    "cognix-nlp/docs/SYNEXIA_TO_M3JDK_PENDING_TARGET_CONTRACT.tsv":
        "ac5bbbb4d390a7ea80bff6dbebc45c47ddce1cba",
}
_SOURCES = {
    "synexia-common/src/main/java/com/synexia/common/language/GlobalLexicon.java":
        "2206d5e467e20889ec73d7a88b9851716e57c1ce",
    "synexia-common/src/main/java/com/synexia/common/language/LanguageDictionary.java":
        "ed1f96a3fc86c5df28d8e635efa9eda4a33c2a83",
    "synexia-common/src/main/java/com/synexia/common/language/M3JdkTranslationProjection.java":
        "bb72c00e36f1835d824a34ab398b1fe5aadb1cb3",
    "synexia-common/src/main/java/com/synexia/common/language/M3JdkTokenFrequencyProjection.java":
        "c1e0908251ea3152562c42ac933a7ec76d0c0570",
    "synexia-common/src/main/java/com/synexia/common/language/MindexTranslationIndex.java":
        "997af79689c4767dc8a2447a29ac8a99fff0b23e",
    "synexia-common/src/main/java/com/synexia/common/language/MindexSpellIndex.java":
        "1e5dca05e8de96e181ed66052fce4c05bc66b93a",
    "synexia-common/src/main/java/com/synexia/common/language/MindexTokenHashIndex.java":
        "c063115da51fba9319b3718568d2408ba9728b4a",
    "synexia-common/src/main/java/com/synexia/common/language/MindexPrefixCounts.java":
        "697c0161728af38e8137e1f8314edb28b98dd0d4",
    "synexia-common/src/main/java/com/synexia/common/language/MindexTokenFrequency.java":
        "b7f3957313e1b7b3ad1929b0087dce48124dd7f3",
}
_EXPECTED_CAPABILITIES = {
    "dictionary", "lexicon-precompute-catalog", "precompute-second-pass-proof",
    "frequency", "langdex", "si-units", "acronyms", "huggingface-frequency",
    "translation-grammar", "numbers-0-10000", "translation-projection",
    "spell-index", "string-facts", "token-hash-precompute", "prefix-counts",
    "token-frequency", "thesaurus", "antonyms", "phrase-rewrite",
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
    manifest_rows = _rows(manifest)
    field_rows = _rows(field_map)
    mapping = _read(root, Path("m3/lexicon/synexia-source-manifest.tsv"))
    return (
        SourceCheck("receiptHeader", receipt.startswith("donor_commit\t")),
        SourceCheck("receiptRows", len(receipt_rows) == 12),
        SourceCheck("allPinnedCommit", all(r["donor_commit"] == _COMMIT for r in receipt_rows)),
        SourceCheck("threeMappingDocuments", sum(r["source_role"] == "mapping-contract" for r in receipt_rows) == 1
                    and sum(r["source_role"] == "typed-precompute-contract" for r in receipt_rows) == 1
                    and sum(r["source_role"] == "target-contract-evidence" for r in receipt_rows) == 1),
        SourceCheck("documentBlobs", all(any(r["donor_path"] == p and r["donor_blob"] == b for r in receipt_rows)
                                        for p, b in _DOCS.items())),
        SourceCheck("sourceBlobs", all(any(r["donor_path"] == p and r["donor_blob"] == b for r in receipt_rows)
                                      for p, b in _SOURCES.items())),
        SourceCheck("sourceRowCount", sum(r["source_role"] == "mapping-contract" or r["source_role"] == "typed-precompute-contract"
                                         or r["source_role"] == "target-contract-evidence" for r in receipt_rows) == 3),
        SourceCheck("nineSourceRows", sum(r["source_role"] not in {"mapping-contract", "typed-precompute-contract", "target-contract-evidence"}
                                         for r in receipt_rows) == 9),
        SourceCheck("manifestRows", len(manifest_rows) == 10),
        SourceCheck("manifestFamilies", {r["source_id"] for r in manifest_rows} == {
            "dictlang.dictionary", "dictlang.frequency", "dictlang.thesaurus", "dictlang.antonyms",
            "dictlang.huggingface", "unicodex.langdex.lexemes", "translate.rows",
            "dictlang.si-units", "dictlang.acronyms", "dictlang.numbers.0-10000"}),
        SourceCheck("fieldMapRows", len(field_rows) == 56),
        SourceCheck("fieldMapMapped", all(r.get("status") == "MAPPED" for r in field_rows)),
        SourceCheck("manifestTargets", all(term in manifest for term in (
            "M3StringFacts", "IndexWordFacts", "IndexWordSignal", "LangDexCoordinate",
            "TranslationMapping", "SiUnitPrecompute", "AcronymPrecompute", "NumberPrecompute",
        ))),
        SourceCheck("recipeReceipt", "donor_receipt: m3/lexicon/synexia-lexicon-precompute-receipt.tsv" in recipe),
        SourceCheck("recipeMapping", "cognix-nlp/docs/SYNEXIA_TO_M3JDK_MAPPING.tsv" in recipe),
        SourceCheck("recipeTyped", "SYNEXIA_TO_M3JDK_TYPED_PRECOMPUTE_EXTENSION.tsv" in recipe),
        SourceCheck("recipePending", "SYNEXIA_TO_M3JDK_PENDING_TARGET_CONTRACT.tsv" in recipe),
        SourceCheck("recipeNoPayload", "without copying lexical payloads into java.lang.String" in recipe),
        SourceCheck("recipeNoAbi", "No public M3String ABI" in recipe),
        SourceCheck("recipeGapProper", "Proper-noun" in recipe),
        SourceCheck("recipeGapTitles", "title" in recipe),
        SourceCheck("recipeGapRapidex", "Rapidex" in recipe),
        SourceCheck("recipeRuntime", "runtime: NOT_RUN" in recipe),
        SourceCheck("recipeStorage", "minimum_free_after_output: 8GiB" in recipe),
        SourceCheck("mappingDictionary", "dictlang.dictionary" in manifest),
        SourceCheck("mappingTranslation", "translate.rows" in manifest),
        SourceCheck("mappingSpell", "deleteToTokenIds" in field_map),
        SourceCheck("mappingTokenHash", "tokenSha256" in field_map),
        SourceCheck("mappingPrefix", "prefixCounts" in field_map),
        SourceCheck("mappingFrequency", "TokenFrequency" in field_map),
        SourceCheck("mappingNumbers", "dictlang.numbers.0-10000" in manifest),
        SourceCheck("mappingUnits", "dictlang.si-units" in manifest),
        SourceCheck("mappingLangDex", "unicodex.langdex.lexemes" in manifest),
        SourceCheck("mappingProvenance", "data_license" in manifest and "data_policy" in manifest),
        SourceCheck("mappingNoFabrication", "downloader is never invoked" in manifest and "mapping-only export" in manifest),
        SourceCheck("fieldMapGuard", "reload_guard: value_fingerprint" in recipe),
        SourceCheck("fieldMapScope", "scope-keyed" in recipe),
        SourceCheck("fieldMapAdmission", "TARGET_CONTRACT_ADMITTED" in _read(root, Path("m3/runtime-integration/synexia-lexicon-precompute-receipt-20261009.yaml")),
    )


def receipt_line(checks: tuple[SourceCheck, ...]) -> str:
    passed = sum(check.passed for check in checks)
    marker = "PASS" if passed == len(checks) else "FAIL"
    failed = tuple(check.name for check in checks if not check.passed)
    suffix = "" if not failed else " failed=" + ",".join(failed)
    return f"M3_LEXICON_PRECOMPUTE_RECEIPT_SOURCE_{marker} checks={passed}/{len(checks)}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", "--root", dest="root", type=Path, default=Path("."))
    args = parser.parse_args()
    try:
        checks = inspect_source(args.root.resolve())
    except (OSError, UnicodeError, csv.Error, KeyError) as error:
        print(f"M3_LEXICON_PRECOMPUTE_RECEIPT_SOURCE_FAIL error={error}", file=sys.stderr)
        return 2
    print(receipt_line(checks))
    return 0 if all(check.passed for check in checks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
