#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only source-custody gate for Synexia MIndex->M3JDK migration delta.

This verifies exact pinned Git blob identities and non-admission status. It
does not assert remote availability, source compilation or JDK runtime proof.
"""
from __future__ import annotations

import csv
import io
import re
import sys
from pathlib import Path

LEDGER = Path(__file__).with_name("mindex-rename-synexia-source-delta-20261016.tsv")
COLUMNS = [
    "kind", "source_revision", "source_path", "source_blob_sha1",
    "synexia_pr", "semantic_role", "intake_mode", "status", "target_admitted",
]
CANONICAL = "13303baa6ccb04e025e388b6dc19391e3822681b"
INVENTORY = "5d64e8324cf0079944d6fff3c0a2e8ee01ba1ccb"
CALLER = "811ff993a4b8ad58acfc621d097c12fe98b7eec6"
SOURCE_PREFIX = "synexia-openrewrite-recipes/"
# sourcePath => revision, blob SHA, PR, kind, role, status; exact review baseline.
EXPECTED = {
    "src/main/java/com/synexia/rewrite/M3MIndexNamePolicy.java":
        (CANONICAL, "ed07e542d8092241e50e0a2ce8b7db3b1e676b1d",
         "9706", "CANONICAL_POLICY", "NAME_MAP_AUTHORITY", "EXISTING_UPSTREAM_AUTHORITY"),
    "src/main/java/com/synexia/rewrite/M3CopyMIndexDeclarationRecipe.java":
        (CANONICAL, "641856310dded5746f7ba6e3a04a47ff3fc5b607",
         "7827", "CANONICAL_RECIPE", "ADDITIVE_COPY_ONLY", "EXISTING_UPSTREAM_AUTHORITY"),
    "src/main/java/com/synexia/rewrite/M3MIndexPortfolioInventory.java":
        (INVENTORY, "721ebeef0507f76b27611ef98367dc0aaec8ee0b",
         "11918", "DRAFT_INVENTORY", "PORTFOLIO_READ_ONLY_INTAKE", "UNQUALIFIED_DRAFT"),
    "src/test/java/com/synexia/rewrite/M3MIndexPortfolioInventorySmoke.java":
        (INVENTORY, "3777a3a6e3445114c403be027a8187ec872f361f",
         "11918", "DRAFT_TEST", "STANDALONE_JAVA21_SMOKE", "UNQUALIFIED_DRAFT"),
    "src/main/java/com/synexia/rewrite/M3MIndexExactCallerRetargetRecipe.java":
        (CALLER, "0d361e633a58bdb85f2948ab0be628e81015327c",
         "11921", "DRAFT_RECIPE", "SOURCE_SEALED_CALLER_IMPORT_REWRITE", "UNQUALIFIED_DRAFT"),
    "src/main/java/com/synexia/rewrite/M3MIndexCallerRetargetReceiptTable.java":
        (CALLER, "13889756050dd7190bb9f7e1e418fb4fdebfef70",
         "11921", "DRAFT_RECEIPT", "MIGRATION_RECEIPT", "UNQUALIFIED_DRAFT"),
    "src/test/java/com/synexia/rewrite/M3MIndexExactCallerRetargetRecipeTest.java":
        (CALLER, "aa91a643bc713e9a5401a998c4716b2faa289eae",
         "11921", "DRAFT_TEST", "ATTRIBUTED_IN_MEMORY_REWRITE_TEST", "UNQUALIFIED_DRAFT"),
}


def verify(contents: str) -> int:
    lines = contents.splitlines()
    if not lines or lines[0] != "# SPDX-License-Identifier: Apache-2.0":
        raise ValueError("M3_MINDEX_RECEIVER_MISSING_SPDX")
    reader = csv.DictReader(io.StringIO("\n".join(lines[1:]) + "\n"), delimiter="\t")
    if reader.fieldnames != COLUMNS:
        raise ValueError("M3_MINDEX_RECEIVER_HEADER_DRIFT")
    seen: set[str] = set()
    count = 0
    for row in reader:
        if None in row or set(row) != set(COLUMNS) or any(v is None for v in row.values()):
            raise ValueError("M3_MINDEX_RECEIVER_BAD_ROW")
        path = row["source_path"]
        if not path.startswith(SOURCE_PREFIX) or ".." in Path(path).parts:
            raise ValueError("M3_MINDEX_RECEIVER_PATH_ESCAPE")
        if path in seen or path not in (SOURCE_PREFIX + key for key in EXPECTED):
            raise ValueError("M3_MINDEX_RECEIVER_UNKNOWN_OR_DUPLICATE:" + path)
        seen.add(path)
        expected = EXPECTED[path.removeprefix(SOURCE_PREFIX)]
        facts = tuple(row[col] for col in (
            "source_revision", "source_blob_sha1", "synexia_pr", "kind",
            "semantic_role", "status"
        ))
        if facts != expected:
            raise ValueError("M3_MINDEX_RECEIVER_SOURCE_DRIFT:" + path)
        if not re.fullmatch("[0-9a-f]{40}", row["source_revision"]):
            raise ValueError("M3_MINDEX_RECEIVER_BAD_REVISION")
        if not re.fullmatch("[0-9a-f]{40}", row["source_blob_sha1"]):
            raise ValueError("M3_MINDEX_RECEIVER_BAD_BLOB")
        if row["target_admitted"] != "false":
            raise ValueError("M3_MINDEX_RECEIVER_FORBIDDEN_ADMISSION")
        if row["intake_mode"] != "EXTERNAL_RECIPE_TOOL":
            raise ValueError("M3_MINDEX_RECEIVER_RUNTIME_LEAK")
        count += 1
    if count != len(EXPECTED) or seen != {SOURCE_PREFIX + key for key in EXPECTED}:
        raise ValueError("M3_MINDEX_RECEIVER_MISSING_SOURCE")
    return count


def self_test(contents: str) -> None:
    verify(contents)
    for broken in (
        contents.replace("target_admitted\n", "target_admitted\n", 1).replace("\tfalse\n", "\ttrue\n", 1),
        contents.replace("ed07e542d8092241e50e0a2ce8b7db3b1e676b1d", "0" * 40),
        contents.replace("EXTERNAL_RECIPE_TOOL", "JAVA_BASE_DEPENDENCY", 1),
        contents.replace("UNQUALIFIED_DRAFT", "PRODUCTION_ADMITTED", 1),
        contents + contents.splitlines()[-1] + "\n",
        contents.replace(
            "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3MIndexNamePolicy.java",
            "synexia-openrewrite-recipes/../evil.java"),
    ):
        try:
            verify(broken)
        except ValueError:
            continue
        raise AssertionError("M3_MINDEX_RECEIVER_REFUSAL_MISSING")
    print("M3_MINDEX_RECEIVER_SELFTEST_PASS cases=6")


if __name__ == "__main__":
    contents = LEDGER.read_text(encoding="utf-8")
    count = verify(contents)
    print(f"M3_MINDEX_RECEIVER_VALID rows={count} admission=false")
    if "--self-test" in sys.argv[1:]:
        self_test(contents)
