# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECEIPT = ROOT / "compatibility" / "synexia-string-focused-recipe-receipt-20261008.tsv"
ESTATE_BINDING = ROOT / "compatibility" / "synexia-string-focused-estate-binding-20261008.tsv"
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

        self.assertEqual(3, len(rows))
        by_lane = {row["lane"]: row for row in rows}
        self.assertEqual(
            {
                "adaptive-prepared-literal-search",
                "exact-utf16-position-masks",
                "focused-search-superset-acceptance",
            },
            set(by_lane),
        )

        expected_artifacts = {
            "adaptive-prepared-literal-search": (
                "synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3jdk21-string-adaptive-search.yml",
                "a16743de4db9fb63fb857939ce23393fc7adc293",
                "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-string-adaptive-search-20261008/manifest.tsv",
                "158fcb491ab53e9b262c1da636b8c3ef27bb3c0d",
            ),
            "exact-utf16-position-masks": (
                "synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3jdk21-string-position-masks.yml",
                "fdff9654232b9a02171a547ce391367073cdc81a",
                "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-string-position-masks-20261008/manifest.tsv",
                "74632a5be565f065677ade1c8d7cbd3f30ce5cda",
            ),
            "focused-search-superset-acceptance": (
                "synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3jdk21-string-focused-search-invariant.yml",
                "ff96a228535371451ee0c049c17564009832a465",
                "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3Jdk21StringFocusedSearchInvariantRecipe.java",
                "37e970abe4f0e45478ff00338086972e5c3708c2",
            ),
        }

        for row in rows:
            self.assertEqual(
                "M3JDK21_SYNEXIA_STRING_FOCUSED_RECIPE_RECEIPT_V2",
                row["schema"],
            )
            self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
            self.assertEqual("9859", row["canonical_pr"])
            self.assertEqual(
                "e89630d3fb2b5f77526f408fc0d2389d67964b31",
                row["canonical_revision"],
            )
            self.assertEqual("NO_TARGET_CANONICAL_DUPLICATE", row["target_recipe_policy"])
            self.assertRegex(row["canonical_recipe_git_blob"], r"^[0-9a-f]{40}$")
            self.assertRegex(row["canonical_evidence_git_blob"], r"^[0-9a-f]{40}$")
            self.assertTrue(row["canonical_recipe_path"].startswith("synexia-openrewrite-recipes/"))
            self.assertTrue(row["canonical_evidence_path"].startswith("synexia-openrewrite-recipes/"))
            (
                recipe_path,
                recipe_blob,
                evidence_path,
                evidence_blob,
            ) = expected_artifacts[row["lane"]]
            self.assertEqual(recipe_path, row["canonical_recipe_path"])
            self.assertEqual(recipe_blob, row["canonical_recipe_git_blob"])
            self.assertEqual(evidence_path, row["canonical_evidence_path"])
            self.assertEqual(evidence_blob, row["canonical_evidence_git_blob"])
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
        self.assertEqual(
            "com.synexia.rewrite.M3Jdk21StringFocusedSearchInvariant",
            by_lane["focused-search-superset-acceptance"]["canonical_recipe"],
        )

    def test_focused_receipt_is_bound_to_full_text_estate_without_family_completion(self) -> None:
        with ESTATE_BINDING.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(1, len(rows))
        row = rows[0]
        self.assertEqual("M3JDK21_STRING_ESTATE_RECEIPT_V2", row["schema"])
        self.assertEqual("TEXT_INDEXSTRING", row["family"])
        self.assertEqual(
            "296323958b1019edd59b60b9c05cb148d024cfe5",
            row["source_snapshot_revision"],
        )
        self.assertEqual(
            "ee8a03de09f7a2de04bc5ee39aab65d31e85cde0",
            row["estate_manifest_revision"],
        )
        self.assertEqual("9860", row["estate_source_pr"])
        self.assertEqual(
            ".m3/m3jdk21-full-borrow-estate.tsv",
            row["estate_manifest_path"],
        )
        self.assertEqual(
            "292bc31a96cd3d20c36d617045c6b01a7a7c8faf",
            row["estate_manifest_git_blob"],
        )
        self.assertEqual(
            ".m3/m3jdk21-full-borrow-dag.tsv",
            row["estate_dag_path"],
        )
        self.assertEqual(
            "3e6c56b8555231fb36d0ac620ab112aa5970d45a",
            row["estate_dag_git_blob"],
        )
        self.assertEqual(
            "3d1bbf3bc944e827b8b82aa6841bc0720d618e93",
            row["source_family_tree"],
        )
        self.assertEqual(
            "e89630d3fb2b5f77526f408fc0d2389d67964b31",
            row["focused_recipe_revision"],
        )
        self.assertEqual("9859", row["focused_recipe_pr"])
        self.assertEqual(
            "m3/compatibility/synexia-string-focused-recipe-receipt-20261008.tsv",
            row["focused_receipt_path"],
        )
        self.assertEqual(
            "c9b07049c57ecdf43175f5885e11998918416a00",
            row["target_master"],
        )
        self.assertEqual(
            "THREE_FOCUSED_RUNTIME_LANES_ALREADY_ABSORBED",
            row["target_lane_state"],
        )
        self.assertEqual("NOT_COMPLETE", row["family_acceptance"])

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
            "m3jdk21-string-focused-search-invariant.yml",
            "m3jdk21-string-focused-search-convergence.yml",
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
            self.assertNotIn(
                "com.synexia.rewrite.M3Jdk21StringFocusedSearchInvariant",
                text,
                path.name,
            )
            self.assertNotIn(
                "com.synexia.rewrite.M3Jdk21StringFocusedSearchConvergence",
                text,
                path.name,
            )


if __name__ == "__main__":
    unittest.main()
