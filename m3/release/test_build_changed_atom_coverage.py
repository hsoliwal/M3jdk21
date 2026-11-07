# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""JaCoCo coverage importer tests using synthetic XML; not product coverage proof."""
from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("build_changed_atom_coverage.py")
spec = importlib.util.spec_from_file_location("m3_coverage_import", MODULE)
importer = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(importer)


class ImporterTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        source = self.root / "m3/core/src/com/m3/Example.java"
        source.parent.mkdir(parents=True)
        source.write_text("package com.m3; class Example { boolean run() { return true; } }\n")
        self.report = self.root / "coverage.xml"
        self.report.write_text(
            '<report name="fixture"><package name="com/m3">'
            '<class name="com/m3/Example"><method name="run" desc="()Z">'
            '<counter type="LINE" missed="1" covered="19"/>'
            '<counter type="BRANCH" missed="1" covered="19"/>'
            '</method></class></package></report>')
        self.inventory = self.root / "atoms.tsv"
        self.inventory.write_text(
            "id\tclass\tmethod\tdescriptor\tsource_path\tbranchless_explanation\n"
            "example.run\tcom/m3/Example\trun\t()Z\tm3/core/src/com/m3/Example.java\t\n")

    def build(self, reviewed=False):
        return importer.build(self.root, self.report, self.inventory,
                              "coverage.xml", "atoms.tsv", "JaCoCo fixture", reviewed)

    def test_import_is_not_an_inventory_proof(self):
        raw = self.build()
        self.assertFalse(raw["inventory_complete"])
        self.assertTrue(self.build(True)["inventory_complete"])
        self.assertEqual(1, raw["measured_count"])
        self.assertEqual(19, raw["atoms"][0]["line_covered"])
        self.assertEqual(64, len(raw["inventory_sha256"]))
        self.assertEqual(64, len(raw["atoms"][0]["source_sha256"]))

    def test_missing_reported_method_is_refused(self):
        self.inventory.write_text(self.inventory.read_text().replace("run\t()Z", "other\t()Z"))
        with self.assertRaisesRegex(ValueError, "missing instrumented method"):
            self.build()

    def test_duplicate_atom_is_refused(self):
        line = self.inventory.read_text().splitlines()[1]
        with self.inventory.open("a") as out:
            out.write(line + "\n")
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.build()

    def test_missing_branchless_explanation_is_refused(self):
        self.report.write_text(self.report.read_text().replace(
            '<counter type="BRANCH" missed="1" covered="19"/>',
            '<counter type="BRANCH" missed="0" covered="0"/>'))
        with self.assertRaisesRegex(ValueError, "branchless"):
            self.build()
        self.inventory.write_text(self.inventory.read_text().rstrip() + "No branches\n")
        self.assertEqual(0, self.build()["atoms"][0]["branch_missed"])

    def test_source_escape_or_symlink_is_refused(self):
        self.inventory.write_text(self.inventory.read_text().replace(
            "m3/core/src/com/m3/Example.java", "m3/../../escape.java"))
        with self.assertRaisesRegex(ValueError, "invalid source path"):
            self.build()

    def test_invalid_coverage_report_is_refused(self):
        self.report.write_text("<other/>")
        with self.assertRaisesRegex(ValueError, "JaCoCo XML"):
            self.build()


if __name__ == "__main__":
    unittest.main()
