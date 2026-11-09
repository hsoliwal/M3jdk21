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
        self.assertEqual("JACOCO_XML", raw["format"])
        self.assertEqual(("com/m3/Example", "run", "()Z"),
                         (raw["atoms"][0]["jacoco_class"],
                          raw["atoms"][0]["jacoco_method"],
                          raw["atoms"][0]["jacoco_descriptor"]))
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
        self.inventory.write_text(self.inventory.read_text().replace(
            "Example.java\t\n", "Example.java\tNo branches\n"))
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


    def test_source_path_refuses_missing_backslash_and_symlink(self):
        for bad in ("", "m3\\file.java", "m3/missing.java", "/m3/file.java"):
            with self.subTest(path=bad), self.assertRaises(ValueError):
                importer.source_file(self.root, bad)
        link = self.root / "m3/linked.java"
        try:
            link.symlink_to(self.root / "m3/core/src/com/m3/Example.java")
        except (OSError, NotImplementedError):
            return  # Windows environments without symlink privileges
        with self.assertRaisesRegex(ValueError, "symlink"):
            importer.source_file(self.root, "m3/linked.java")

    def test_jacoco_rejects_missing_and_duplicate_method_fields(self):
        original = self.report.read_text()
        cases = (
            ("<report/>", "no measured methods"),
            (original.replace(' desc="()Z"', ''), "method key"),
            (original.replace('</class>', '<method name="run" desc="()Z"/></class>'), "method key"),
            (original.replace('<counter type="BRANCH" missed="1" covered="19"/>', ''), "branchless"),
            (original.replace('covered="19"', 'covered="-1"', 1), "invalid JaCoCo count"),
            (original.replace('</method>', '<counter type="LINE" covered="1" missed="0"/></method>'),
             "duplicate JaCoCo counter"),
        )
        for xml, reason in cases:
            self.report.write_text(xml)
            with self.subTest(reason=reason), self.assertRaisesRegex(ValueError, reason):
                self.build()
        self.report.write_text(original)
        self.report.write_text(original.replace('<method name="run"',
            '<method name="unused" desc="()V"><counter type="INSTRUCTION" covered="1" missed="0"/>'
            '<counter type="LINE" covered="1" missed="0"/>'
            '<counter type="BRANCH" covered="0" missed="0"/></method><method name="run"'))
        self.assertEqual(1, self.build()["measured_count"])

    def test_inventory_format_and_missing_line_proofs(self):
        old = self.inventory.read_text()
        for content, reason in (
            ("not-a-column\n", "columns"),
            (old.splitlines()[0] + "\n", "empty coverage inventory"),
        ):
            self.inventory.write_text(content)
            with self.subTest(reason=reason), self.assertRaisesRegex(ValueError, reason):
                self.build()
        self.inventory.write_text(old)
        xml = self.report.read_text()
        self.report.write_text(xml.replace('type="LINE" missed="1" covered="19"',
                                          'type="LINE" missed="0" covered="0"'))
        with self.assertRaisesRegex(ValueError, "no instrumented lines"):
            self.build()

    def test_tool_identity_and_distinct_artifacts(self):
        with self.assertRaisesRegex(ValueError, "tool identity"):
            importer.build(self.root, self.report, self.inventory, "x", "y", "", False)
        with self.assertRaisesRegex(ValueError, "distinct report"):
            importer.build(self.root, self.report, self.inventory, "x", "x", "tool", False)

    def test_many_atoms_reuse_one_source(self):
        self.report.write_text(self.report.read_text().replace(
            '</class>', '<method name="second" desc="()V">'
            '<counter type="LINE" missed="0" covered="3"/>'
            '<counter type="BRANCH" missed="0" covered="0"/>'
            '</method></class>'))
        with self.inventory.open("a") as out:
            out.write("example.second\tcom/m3/Example\tsecond\t()V\t"
                      "m3/core/src/com/m3/Example.java\tNo branches\n")
        result = self.build(True)
        self.assertEqual(2, result["measured_count"])
        self.assertEqual(1, len(set(x["source_sha256"] for x in result["atoms"])))

    def test_cli_success_and_input_failure(self):
        import contextlib
        import io
        import json
        out = self.root / "result.json"
        args = ["--root", str(self.root), "--report", str(self.report),
                "--inventory", str(self.inventory),
                "--report-artifact", "coverage.xml", "--inventory-artifact", "atoms.tsv",
                "--tool", "JaCoCo synthetic", "--out", str(out)]
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(0, importer.main(args))
        self.assertFalse(json.loads(out.read_text())["inventory_complete"])
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(0, importer.main(args + ["--reviewed-complete-inventory"]))
        self.assertTrue(json.loads(out.read_text())["inventory_complete"])
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as exit_info:
                importer.main(args[:-1] + [str(self.root / "bad.json"), "--tool", ""])
        self.assertEqual(2, exit_info.exception.code)


    def test_main_script_entrypoint(self):
        import contextlib
        import io
        import runpy
        import sys
        from unittest.mock import patch
        out = self.root / "entrypoint.json"
        argv = [str(MODULE), "--root", str(self.root), "--report", str(self.report),
                "--inventory", str(self.inventory), "--report-artifact", "coverage.xml",
                "--inventory-artifact", "atoms.tsv", "--tool", "JaCoCo fixture",
                "--out", str(out)]
        with patch.object(sys, "argv", argv):
            with contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaises(SystemExit) as exited:
                    runpy.run_path(str(MODULE), run_name="__main__")
        self.assertEqual(0, exited.exception.code)
        self.assertTrue(out.is_file())


    def test_omitted_branch_counter_requires_a_branchless_explanation(self):
        self.report.write_text(self.report.read_text().replace(
            '<counter type="BRANCH" missed="1" covered="19"/>', ''))
        inventory = self.inventory.read_text()
        for explanation in ("", "   "):
            self.inventory.write_text(inventory.replace(
                "Example.java\t\n", "Example.java\t" + explanation + "\n"))
            with self.subTest(explanation=explanation):
                with self.assertRaisesRegex(ValueError, "branchless method requires explanation"):
                    self.build()
        self.inventory.write_text(inventory.replace(
            "Example.java\t\n", "Example.java\tNo control flow in branchless fixture\n"))
        atom = self.build()["atoms"][0]
        self.assertEqual((0, 0), (atom["branch_covered"], atom["branch_missed"]))
        self.assertEqual((19, 1), (atom["line_covered"], atom["line_missed"]))
        self.assertEqual("No control flow in branchless fixture", atom["branchless_explanation"])

    def test_unselected_method_may_omit_its_branch_counter(self):
        self.report.write_text(self.report.read_text().replace(
            '</class>', '<method name="unused" desc="()V">'
            '<counter type="LINE" covered="3" missed="0"/>'
            '</method></class>'))
        receipt = self.build()
        self.assertEqual(1, receipt["measured_count"])
        self.assertEqual(["example.run"], [atom["id"] for atom in receipt["atoms"]])
        self.assertEqual((19, 1), (receipt["atoms"][0]["branch_covered"],
                                  receipt["atoms"][0]["branch_missed"]))


if __name__ == "__main__":
    unittest.main()
