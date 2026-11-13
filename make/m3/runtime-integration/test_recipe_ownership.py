#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Receiving classification checks; no recipe engine or product code is run.

Fixtures retain a real ledger row and the corresponding current Java bytes.
These tests qualify parsing and source coverage, not canonical recipe execution.
The separate unmodified repository ledger must still pass the production gate.
"""
from __future__ import annotations

import csv
import importlib.util
from pathlib import Path
import tempfile
import unittest


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SPEC = importlib.util.spec_from_file_location("recipe_ownership", HERE / "verify-recipe-ownership.py")
assert SPEC and SPEC.loader
OWNERSHIP = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(OWNERSHIP)


class RecipeOwnershipTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = (ROOT / "m3/docs/m3-recipe-ownership-classification.tsv").read_text()
        reader = csv.DictReader(cls.original.splitlines(), delimiter="\t")
        cls.columns = tuple(reader.fieldnames or ())
        cls.rows = list(reader)
        cls.row = next(
            row for row in cls.rows
            if row["classification"] == "JDK_TARGET_SPECIFIC"
            and row["canonical_synexia_owner"] == "com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner"
            and (ROOT / row["local_path"]).is_file()
        )
        cls.second_path = (
            "m3/tooling/migration-recipes/src/main/java/"
            "jdk/internal/m3/rewrite/M3HashPinnedJavaSnapshotRecipe.java"
        )
        cls.second_bytes = (ROOT / cls.second_path).read_bytes()
        cls.java_bytes = (ROOT / cls.row["local_path"]).read_bytes()

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.ledger = self.root / "m3/docs/m3-recipe-ownership-classification.tsv"
        self.ledger.parent.mkdir(parents=True)
        (self.root / OWNERSHIP.SOURCE_PATH).mkdir(parents=True)
        self.ledger.write_text(self.original, encoding="utf-8")

    def write_rows(self, rows, columns=None):
        with self.ledger.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.DictWriter(stream, fieldnames=columns or self.columns,
                                    delimiter="\t", lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)

    def mutate_row(self, **changes):
        row = dict(self.row)
        row.update(changes)
        self.write_rows([row])

    def add_source(self, path=None, content=None):
        path = self.root / (path or self.row["local_path"])
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(self.java_bytes if content is None else content)
        return path

    def test_original_ledger_schema_and_all_recorded_owners_are_readable(self):
        actual = OWNERSHIP.read_rows(self.ledger)
        self.assertEqual(len(self.rows), len(actual))
        self.assertEqual({row["local_path"] for row in self.rows}, set(actual))
        self.assertTrue(any(row["classification"] == "SYNEXIA_CANONICAL_RESIDUE"
                            for row in actual.values()))
        self.assertEqual("com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner",
                         self.row["canonical_synexia_owner"])

    def test_header_rename_duplicate_and_reordering_are_refused(self):
        header, body = self.original.split("\n", 1)
        variants = (
            header.replace("canonical_synexia_owner", "canonical_m3_owner"),
            header + "\tcanonical_synexia_owner",
            "\t".join(reversed(self.columns)),
        )
        for changed in variants:
            with self.subTest(header=changed):
                self.ledger.write_text(changed + "\n" + body, encoding="utf-8")
                with self.assertRaisesRegex(SystemExit, "unexpected ledger columns"):
                    OWNERSHIP.read_rows(self.ledger)

    def test_extra_and_missing_row_fields_are_refused(self):
        lines = self.original.splitlines()
        for changed in (lines[1] + "\textra", "\t".join(lines[1].split("\t")[:-1])):
            with self.subTest(row=changed):
                self.ledger.write_text(lines[0] + "\n" + changed + "\n", encoding="utf-8")
                with self.assertRaisesRegex(SystemExit, "malformed ledger row"):
                    OWNERSHIP.read_rows(self.ledger)

    def test_schema_and_classification_drift_are_refused(self):
        for changes, error in (
            ({"schema": "M3JDK21_RECIPE_OWNERSHIP_V2"}, "invalid schema"),
            ({"classification": "M3_CANONICAL_RESIDUE"}, "invalid classification"),
            ({"classification": "UNCLASSIFIED"}, "invalid classification"),
        ):
            with self.subTest(changes=changes):
                self.mutate_row(**changes)
                with self.assertRaisesRegex(SystemExit, error):
                    OWNERSHIP.read_rows(self.ledger)

    def test_duplicate_source_path_is_refused(self):
        self.write_rows([self.row, self.row])
        with self.assertRaisesRegex(SystemExit, "duplicate ledger path"):
            OWNERSHIP.read_rows(self.ledger)

    def test_unowned_and_target_renamed_canonical_owners_are_refused(self):
        for owner in ("", "NONE", "org.example.Recipe", "jdk.internal.m3.rewrite.M3JdkHandoff"):
            with self.subTest(owner=owner):
                self.mutate_row(canonical_synexia_owner=owner)
                with self.assertRaisesRegex(SystemExit, "provenance.*owner"):
                    OWNERSHIP.read_rows(self.ledger)

    def test_jdk_specific_owner_must_be_a_canonical_planner_or_proof(self):
        self.mutate_row(canonical_synexia_owner="com.synexia.indexstring.MIndexString")
        with self.assertRaisesRegex(SystemExit, "must name a Synexia planner/proof owner"):
            OWNERSHIP.read_rows(self.ledger)

    def test_empty_reason_is_refused(self):
        self.mutate_row(target_reason="  ")
        with self.assertRaisesRegex(SystemExit, "empty target_reason"):
            OWNERSHIP.read_rows(self.ledger)

    def test_non_source_and_escaping_paths_are_refused(self):
        prefix = OWNERSHIP.SOURCE_PATH.as_posix()
        for path in ("outside.java", "/" + self.row["local_path"],
                     prefix + "/../Outside.java", prefix + "//Owner.java",
                     prefix + "/Owner.txt", prefix + "/jdk\\Owner.java"):
            with self.subTest(path=path):
                self.mutate_row(local_path=path)
                with self.assertRaisesRegex(SystemExit, "invalid recipe source path"):
                    OWNERSHIP.read_rows(self.ledger)

    def test_recorded_current_owner_has_complete_positive_coverage(self):
        self.write_rows([self.row])
        self.add_source()
        counts = OWNERSHIP.verify(self.root, self.ledger)
        self.assertEqual(1, sum(counts.values()))
        self.assertEqual(1, counts["JDK_TARGET_SPECIFIC"])

    def test_source_census_includes_both_current_package_trees(self):
        self.add_source()
        self.add_source(self.second_path, self.second_bytes)
        self.assertEqual({self.row["local_path"], self.second_path},
                         OWNERSHIP.java_files(self.root))

    def test_additional_source_in_either_package_tree_is_refused(self):
        for path in (self.second_path,
                     self.row["local_path"].replace(".java", "Unclassified.java")):
            with self.subTest(path=path):
                self.write_rows([self.row])
                self.add_source()
                addition = self.add_source(path, self.second_bytes)
                with self.assertRaisesRegex(SystemExit, "unclassified reusable/target recipe classes") as error:
                    OWNERSHIP.verify(self.root, self.ledger)
                self.assertIn(path, str(error.exception))
                addition.unlink()

    def test_absent_owner_is_refused_and_history_is_not_silently_dropped(self):
        self.write_rows([self.row])
        with self.assertRaisesRegex(SystemExit, "classification rows without current Java owner") as error:
            OWNERSHIP.verify(self.root, self.ledger)
        self.assertIn(self.row["local_path"], str(error.exception))

    def test_missing_and_stale_owners_are_reported_together(self):
        self.write_rows([self.row])
        self.add_source(self.second_path, self.second_bytes)
        with self.assertRaises(SystemExit) as error:
            OWNERSHIP.verify(self.root, self.ledger)
        message = str(error.exception)
        self.assertIn("unclassified reusable/target recipe classes", message)
        self.assertIn("classification rows without current Java owner", message)
        self.assertIn(self.row["local_path"], message)
        self.assertIn(self.second_path, message)

    def test_missing_source_root_is_not_an_empty_success(self):
        (self.root / OWNERSHIP.SOURCE_PATH).rmdir()
        with self.assertRaisesRegex(SystemExit, "missing or indirect recipe source root"):
            OWNERSHIP.java_files(self.root)

    def test_symlink_file_or_directory_cannot_hide_source_coverage(self):
        source_root = self.root / OWNERSHIP.SOURCE_PATH
        for name, target in (("Indirect.java", ROOT / self.row["local_path"]),
                             ("indirect", ROOT / OWNERSHIP.SOURCE_PATH)):
            with self.subTest(path=name):
                link = source_root / name
                link.symlink_to(target)
                with self.assertRaisesRegex(SystemExit, "indirect recipe source path"):
                    OWNERSHIP.java_files(self.root)
                link.unlink()


if __name__ == "__main__":
    unittest.main()
