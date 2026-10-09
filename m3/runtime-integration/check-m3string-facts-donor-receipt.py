#!/usr/bin/env python3
"""Source proof for the pinned Synexia fact-owner receipt on M3JDK master."""

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


_RECEIPT = Path("m3/docs/synexia-m3string-facts-donor-receipt.tsv")
_RECIPE = Path("m3/runtime-integration/m3string-facts-donor-receipt-20261009.yaml")
_TARGETS = {
    "facts": Path("src/java.base/share/classes/java/lang/M3StringFacts.java"),
    "owner": Path("src/java.base/share/classes/java/lang/M3StringOwner.java"),
    "tuple": Path("src/java.base/share/classes/java/lang/M3StringTuple.java"),
}


def _read(root: Path, relative: Path) -> str:
    return (root / relative).read_text(encoding="utf-8")


def _rows(receipt: str) -> list[dict[str, str]]:
    return list(csv.DictReader(io.StringIO(receipt), delimiter="\t"))


def inspect_source(root: Path) -> tuple[SourceCheck, ...]:
    receipt = _read(root, _RECEIPT)
    recipe = _read(root, _RECIPE)
    facts = _read(root, _TARGETS["facts"])
    owner = _read(root, _TARGETS["owner"])
    tuple_source = _read(root, _TARGETS["tuple"])
    rows = _rows(receipt)
    return (
        SourceCheck("receiptHeader", receipt.startswith("donor_commit\t")),
        SourceCheck("receiptRows", len(rows) == 5),
        SourceCheck("allPinnedCommit", all(row["donor_commit"] == "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a" for row in rows)),
        SourceCheck("sourceProof", "M3_STRING_FACTS_OWNER_SOURCE_PASS checks=100/100" in recipe),
        SourceCheck("metricsPath", any(row["donor_path"].endswith("IndexTextMetrics.java") for row in rows)),
        SourceCheck("metricsBlob", any(row["donor_blob"] == "58beddc813b3ed4716233d23f0e1909bda4e21b9" for row in rows)),
        SourceCheck("metricsOwner", any(row["m3_target_owner"] == "java.lang.M3StringFacts" for row in rows)),
        SourceCheck("textFactsPath", any(row["donor_path"].endswith("MIndexTextPrecomputedFacts.java") for row in rows)),
        SourceCheck("textFactsBlob", any(row["donor_blob"] == "701c28acf980c409dcfb541c8dbb0438ad72ab85" for row in rows)),
        SourceCheck("textFactsOwner", any(row["m3_target_owner"] == "java.lang.M3StringFacts" for row in rows)),
        SourceCheck("canonicalPath", any(row["donor_path"].endswith("MIndexStringCanonicalFacts.java") for row in rows)),
        SourceCheck("canonicalBlob", any(row["donor_blob"] == "1200c927da62570b5881a9c9667b7e3a44d1d11a" for row in rows)),
        SourceCheck("canonicalOwner", any("M3StringOwner" in row["m3_target_owner"] and "M3StringTuple" in row["m3_target_owner"] for row in rows)),
        SourceCheck("rangePath", any(row["donor_path"].endswith("MIndexUtf16RangeFacts.java") for row in rows)),
        SourceCheck("rangeBlob", any(row["donor_blob"] == "6a10a38f6639d8244a378d484c198c10d349705b" for row in rows)),
        SourceCheck("rangeOwner", any(row["m3_target_owner"] == "java.lang.M3StringOwner.rangeFacts" for row in rows)),
        SourceCheck("precomputationPath", any(row["donor_path"].endswith("MIndexStringPrecomputation.java") for row in rows)),
        SourceCheck("precomputationBlob", any(row["donor_blob"] == "b762518f382b22b0622d4fac57985f47e58adf11" for row in rows)),
        SourceCheck("precomputationOwner", any("caller-owned M3" in row["m3_target_owner"] for row in rows)),
        SourceCheck("factsClass", "final class M3StringFacts {" in facts),
        SourceCheck("factsScan", "static M3StringFacts scan(M3String value)" in facts),
        SourceCheck("factsCompose", "static M3StringFacts compose(M3StringFacts left, M3StringFacts right)" in facts),
        SourceCheck("factsContainment", "boolean mayContain(M3StringFacts needle)" in facts),
        SourceCheck("factsPrefix", "boolean prefixMayMatch(M3StringFacts prefix)" in facts),
        SourceCheck("ownerFacts", "private volatile M3StringFacts facts;" in owner),
        SourceCheck("ownerRange0", "private volatile RangeFact range0;" in owner),
        SourceCheck("ownerRange3", "private volatile RangeFact range3;" in owner),
        SourceCheck("ownerPrepared", "final M3StringFacts factsIfPrepared()" in owner),
        SourceCheck("ownerPreparedRange", "final M3StringFacts rangeFactsIfPrepared(long coordinate)" in owner),
        SourceCheck("ownerFactsAccess", "final M3StringFacts facts()" in owner),
        SourceCheck("ownerRangeAccess", "final M3StringFacts rangeFacts(long coordinate, M3String value)" in owner),
        SourceCheck("ownerComputeFacts", "abstract M3StringFacts computeFacts();" in owner),
        SourceCheck("ownerComputeRange", "abstract M3StringFacts computeRangeFacts(int start, int length);" in owner),
        SourceCheck("tupleCanonicalId", "final long canonicalId;" in tuple_source),
        SourceCheck("tupleFacts", "M3StringFacts compose(left.facts(), right.facts())" in tuple_source),
        SourceCheck("tupleRangeFacts", "M3StringFacts computeRangeFacts(int start, int length)" in tuple_source),
        SourceCheck("recipeNoBody", "do not copy Synexia source bodies" in recipe),
        SourceCheck("recipeNoLexiconPayload", "lexicon payloads" in recipe),
        SourceCheck("recipeRuntime", "runtime: NOT_RUN" in recipe),
        SourceCheck("recipeStorage", "minimum_free_after_output: 8GiB" in recipe),
        SourceCheck("recipeNoAbi", "No public M3String ABI" in recipe),
    )


def receipt_line(checks: tuple[SourceCheck, ...]) -> str:
    passed = sum(check.passed for check in checks)
    marker = "PASS" if passed == len(checks) else "FAIL"
    failed = tuple(check.name for check in checks if not check.passed)
    suffix = "" if not failed else " failed=" + ",".join(failed)
    return f"M3_STRING_FACTS_DONOR_RECEIPT_SOURCE_{marker} checks={passed}/{len(checks)}{suffix}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", "--root", dest="root", type=Path, default=Path("."))
    args = parser.parse_args()
    root = args.root.resolve()
    if not root.is_dir():
        print("M3_STRING_FACTS_DONOR_RECEIPT_SOURCE_FAIL error=invalid-root", file=sys.stderr)
        return 2
    try:
        checks = inspect_source(root)
    except (OSError, UnicodeError, csv.Error, KeyError) as error:
        print(f"M3_STRING_FACTS_DONOR_RECEIPT_SOURCE_FAIL error={error}", file=sys.stderr)
        return 2
    print(receipt_line(checks))
    return 0 if all(check.passed for check in checks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
