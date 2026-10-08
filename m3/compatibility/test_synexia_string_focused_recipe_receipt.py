# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECEIPT = ROOT / "compatibility" / "synexia-string-focused-recipe-receipt-20261008.tsv"
RECIPE_ROOT = (
    ROOT
    / "tooling"
    / "migration-recipes"
    / "src"
    / "main"
    / "resources"
    / "META-INF"
    / "rewrite"
)


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


class SynexiaFocusedStringRecipeReceiptTest(unittest.TestCase):
    def test_receipt_pins_synexia_and_current_runtime_blobs(self) -> None:
        with RECEIPT.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(2, len(rows))
        by_lane = {row["lane"]: row for row in rows}
        self.assertEqual(
            {"adaptive-prepared-literal-search", "exact-utf16-position-masks"},
            set(by_lane),
        )

        for row in rows:
            self.assertEqual(
                "M3JDK21_SYNEXIA_STRING_FOCUSED_RECIPE_RECEIPT_V1",
                row["schema"],
            )
            self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
            self.assertEqual("9859", row["canonical_pr"])
            self.assertEqual(
                "8473dabb08419af60458fdb1b4458ccb5a4e3854",
                row["canonical_revision"],
            )
            self.assertEqual("NO_TARGET_CANONICAL_DUPLICATE", row["target_recipe_policy"])
            self.assertIn(
                "ALREADY_ABSORBED_MASTER@c9b07049c57ecdf43175f5885e11998918416a00",
                row["runtime_state"],
            )
            runtime = ROOT.parent / row["runtime_path"]
            self.assertTrue(runtime.is_file(), row["runtime_path"])
            self.assertEqual(row["runtime_git_blob"], git_blob(runtime), row["runtime_path"])

        self.assertEqual(
            "com.synexia.rewrite.M3Jdk21StringAdaptivePreparedSearch",
            by_lane["adaptive-prepared-literal-search"]["canonical_recipe"],
        )
        self.assertEqual(
            "com.synexia.rewrite.M3Jdk21StringExactPositionMasks",
            by_lane["exact-utf16-position-masks"]["canonical_recipe"],
        )

    def test_runtime_contains_absorbed_focused_lanes(self) -> None:
        search = (
            ROOT.parent
            / "src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java"
        ).read_text(encoding="utf-8")
        for required in (
            "int[] skip256 = new int[256];",
            "private static int adaptiveBmh(",
            "return kmp(source, pattern, plan, at, endIndex);",
        ):
            self.assertIn(required, search)

        positions = (
            ROOT.parent
            / "src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java"
        ).read_text(encoding="utf-8")
        for required in (
            "final AtomicReferenceArray<ExactBlock> exact;",
            "private static ExactBlock exactBlock(",
            "Arrays.binarySearch(units, unit)",
        ):
            self.assertIn(required, positions)

    def test_target_does_not_claim_the_new_synexia_recipe_names(self) -> None:
        forbidden_files = (
            "m3jdk21-string-adaptive-search.yml",
            "m3jdk21-string-position-masks.yml",
        )
        for name in forbidden_files:
            self.assertFalse((RECIPE_ROOT / name).exists(), name)

        for path in RECIPE_ROOT.glob("*.yml"):
            text = path.read_text(encoding="utf-8")
            self.assertNotIn(
                "com.synexia.rewrite.M3Jdk21StringAdaptivePreparedSearch",
                text,
                path.name,
            )
            self.assertNotIn(
                "com.synexia.rewrite.M3Jdk21StringExactPositionMasks",
                text,
                path.name,
            )


if __name__ == "__main__":
    unittest.main()
