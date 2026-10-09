#!/usr/bin/env python3
"""Source proof for the pinned Synexia SI-unit receiver boundary."""

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


_RECEIPT = Path("m3/runtime-integration/synexia-si-unit-proof-receipt.tsv")
_TARGET_MAP = Path("m3/lexicon/synexia-si-unit-target-map.tsv")
_FIELD_MAP = Path("m3/lexicon/synexia-precompute-field-map.tsv")
_CATALOG = Path("m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java")
_OWNER = Path("m3/core/src/com/m3/text/M3LexiconPrecompute.java")
_CATALOG_TEST = Path("m3/core/test/M3SiUnitPrecomputeCatalogTest.java")
_DECODER_TEST = Path("m3/core/test/SynexiaSiUnitDecoderTest.java")
_BUILD = Path("m3/build.sh")
_RECIPE_MANIFEST = Path("m3/recipes/manifest.json")
_RECIPE = Path("m3/runtime-integration/synexia-si-unit-proof-recipe-20261009.yaml")
_COMMIT = "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a"
_BLOB = "3d778837b23b81d4ec3d6cb9383023ef04139e2a"
_TARGET_BLOBS = {
    "target_map": "98e491cc391d212c13b732c7b7acfebf722a2f36",
    "field_map": "6490b29ba92ae7f246f65a2b47ce85ea7be051b4",
    "build": "3db12fd87065b99081fafd4425daf3ee02d38374",
    "decoder": "5ca5f5d2e8833ff7c9f6f11349a289b630d9f45e",
    "recipe_manifest": "d0dd2119cfc339b09604cd2a363a84632283713a",
}


def _read(root: Path, path: Path) -> str:
    return (root / path).read_text(encoding="utf-8")


def _git_blob_sha(content: str) -> str:
    payload = content.encode("utf-8")
    return hashlib.sha1(b"blob " + str(len(payload)).encode("ascii") + b"\\0" + payload).hexdigest()


def _rows(content: str) -> list[dict[str, str]]:
    return list(csv.DictReader(io.StringIO(content), delimiter="\t"))


def inspect_source(root: Path) -> tuple[SourceCheck, ...]:
    receipt = _read(root, _RECEIPT)
    target = _read(root, _TARGET_MAP)
    fields = _read(root, _FIELD_MAP)
    catalog = _read(root, _CATALOG)
    owner = _read(root, _OWNER)
    catalog_test = _read(root, _CATALOG_TEST)
    decoder = _read(root, _DECODER_TEST)
    build = _read(root, _BUILD)
    recipe_manifest = _read(root, _RECIPE_MANIFEST)
    recipe = _read(root, _RECIPE)
    receipt_rows = _rows(receipt)
    target_rows = _rows(target)
    return (
        SourceCheck("receiptHeader", receipt.startswith("donor_commit\t")),
        SourceCheck("receiptRows", len(receipt_rows) == 1),
        SourceCheck("pinnedCommit", receipt_rows[0]["donor_commit"] == _COMMIT),
        SourceCheck("pinnedDonorPath", receipt_rows[0]["donor_path"].endswith("/SiUnit.java")),
        SourceCheck("pinnedDonorBlob", receipt_rows[0]["donor_blob"] == _BLOB),
        SourceCheck("targetMapHeader", target.startswith("schema\tsource_id\t")),
        SourceCheck("targetMapRows", len(target_rows) == 1),
        SourceCheck("targetSourceFamily", "dictlang.si-units" in target),
        SourceCheck("targetSourcePath", "synexia-dictlang/shared/api/src/main/java/com/synexia/dictshared/api/SiUnit.java" in target),
        SourceCheck("targetRecordId", "unit_id" in target),
        SourceCheck("targetFields", all(field in target for field in (
            "decimalExponent", "dimensionPacked", "offset", "prefixable"))),
        SourceCheck("targetOwner", "M3LexiconPrecompute.SiUnitPrecompute" in target),
        SourceCheck("targetLookup", "SharedLexiconPrecomputeCatalog.SiUnitIdentity" in target),
        SourceCheck("targetLicense", "Apache-2.0" in target),
        SourceCheck("targetPending", "PENDING_TARGET_RECEIVER_PROOF" in target),
        SourceCheck("targetMapBlob", _git_blob_sha(target) == _TARGET_BLOBS["target_map"]),
        SourceCheck("fieldMapBlob", _git_blob_sha(fields) == _TARGET_BLOBS["field_map"]),
        SourceCheck("buildBlob", _git_blob_sha(build) == _TARGET_BLOBS["build"]),
        SourceCheck("decoderBlob", _git_blob_sha(decoder) == _TARGET_BLOBS["decoder"]),
        SourceCheck("recipeManifestBlob", _git_blob_sha(recipe_manifest) == _TARGET_BLOBS["recipe_manifest"]),
        SourceCheck("fieldDecimal", "SiUnitPrecompute\tdecimalExponent" in fields and "\tMAPPED\t" in fields),
        SourceCheck("fieldDimension", "SiUnitPrecompute\tdimensionPacked" in fields),
        SourceCheck("fieldOffset", "SiUnitPrecompute\toffset" in fields),
        SourceCheck("fieldPrefixable", "SiUnitPrecompute\tprefixable" in fields),
        SourceCheck("catalogIdentity", "record SiUnitIdentity(String sourceId, String recordId)" in catalog),
        SourceCheck("catalogLookup", "findSiUnitPrecompute" in catalog or "siUnitAt" in catalog),
        SourceCheck("catalogOwnerMap", "Map<SiUnitIdentity, M3LexiconPrecompute.SiUnitPrecompute>" in catalog),
        SourceCheck("catalogDuplicateGuard", 'put(siUnits, identity, value, "SI unit")' in catalog),
        SourceCheck("catalogTestIsolation", "dictlang.acronyms" in catalog_test),
        SourceCheck("catalogTestBounds", "new M3LexiconPrecompute.SiUnitPrecompute(101" in catalog_test
                    and "Double.NaN" in catalog_test),
        SourceCheck("decoderMarker", "M3JDK_SI_UNIT_PASS checks=" in decoder),
        SourceCheck("decoderSourceFamily", "dictlang.si-units" in decoder),
        SourceCheck("buildModes", "for mode in jit int nocompact c2" in build),
        SourceCheck("buildDecoder", "SynexiaSiUnitDecoderTest" in build),
        SourceCheck("buildCatalogTest", "M3SiUnitPrecomputeCatalogTest" in build),
        SourceCheck("recipePending", "PENDING_TARGET_RECEIVER_PROOF" in recipe),
        SourceCheck("recipeRuntime", "runtime: NOT_RUN" in recipe),
        SourceCheck("recipeModes", "jit,int,nocompact,c2" in recipe),
        SourceCheck("recipeNoStringPayload", "No SI-unit payload is stored in java.lang.String." in recipe),
        SourceCheck("recipeNoAbi", "No public M3String ABI" in recipe),
        SourceCheck("recipeTargetEvidence", "target_map_blob: 98e491cc391d212c13b732c7b7acfebf722a2f36" in recipe and "recipe_manifest_blob: d0dd2119cfc339b09604cd2a363a84632283713a" in recipe),
    )


def receipt_line(checks: tuple[SourceCheck, ...]) -> str:
    passed = sum(check.passed for check in checks)
    marker = "PASS" if passed == len(checks) else "FAIL"
    failed = ",".join(check.name for check in checks if not check.passed)
    suffix = "" if not failed else " failed=" + failed
    return f"M3_SI_UNIT_PROOF_RECEIPT_SOURCE_{marker} checks={passed}/{len(checks)}{suffix}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", "--root", dest="root", type=Path, default=Path("."))
    args = parser.parse_args()
    try:
        checks = inspect_source(args.root.resolve())
    except (OSError, UnicodeError, csv.Error, KeyError) as error:
        print(f"M3_SI_UNIT_PROOF_RECEIPT_SOURCE_FAIL error={error}", file=sys.stderr)
        return 2
    print(receipt_line(checks))
    return 0 if all(check.passed for check in checks) else 1


if __name__ == "__main__":
    raise SystemExit(main())
