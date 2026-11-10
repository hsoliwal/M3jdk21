# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Changed-atom inventory derivation tests using synthetic JaCoCo XML; not coverage proof."""
from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("derive_changed_atom_inventory.py")
spec = importlib.util.spec_from_file_location("m3_inventory_derive", MODULE)
derive = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(derive)

IMPORTER = Path(__file__).with_name("build_changed_atom_coverage.py")
importer_spec = importlib.util.spec_from_file_location("m3_inventory_importer", IMPORTER)
importer = importlib.util.module_from_spec(importer_spec)
assert importer_spec.loader is not None
importer_spec.loader.exec_module(importer)


class DeriveInventoryTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        source = self.root / "m3/core/src/com/m3/Example.java"
        source.parent.mkdir(parents=True)
        source.write_text("package com.m3; class Example { }\n")
        self.report = self.root / "jacoco.xml"
        self.report.write_text(
            '<report name="fixture"><package name="com/m3">'
            '<class name="com/m3/Example" sourcefilename="Example.java">'
            '<method name="run" desc="()Z"><counter type="LINE" missed="0" covered="2"/>'
            '<counter type="BRANCH" missed="0" covered="2"/></method>'
            '<method name="&lt;init&gt;" desc="()V"><counter type="LINE" missed="0" covered="1"/></method>'
            '</class></package></report>')

    def test_rows_are_sorted_and_branchless_atoms_explained(self):
        rows = derive.derive(self.report, self.root, "m3/core/src")
        self.assertEqual(["com.m3.Example#<init>()V", "com.m3.Example#run()Z"], [r["id"] for r in rows])
        self.assertEqual("m3/core/src/com/m3/Example.java", rows[0]["source_path"])
        self.assertEqual(derive.BRANCHLESS, rows[0]["branchless_explanation"])
        self.assertEqual("", rows[1]["branchless_explanation"])

    def test_rendering_is_deterministic_and_importable(self):
        out = self.root / "atoms.tsv"
        self.assertEqual(0, derive.main(["--report", str(self.report), "--root", str(self.root),
                                         "--out", str(out)]))
        first = out.read_bytes()
        self.assertTrue(first.endswith(b"\n") and b"\r" not in first)
        self.assertEqual(0, derive.main(["--report", str(self.report), "--root", str(self.root),
                                         "--check", str(out)]))
        receipt = importer.build(self.root, self.report, out, "jacoco.xml", "atoms.tsv",
                                 "JaCoCo fixture", True)
        self.assertEqual(2, receipt["measured_count"])
        out.write_bytes(first.replace(b"run", b"walk"))
        self.assertEqual(1, derive.main(["--report", str(self.report), "--root", str(self.root),
                                         "--check", str(out)]))

    def test_unloaded_compiled_class_is_a_blocker(self):
        classes = self.root / "classes/com/m3"
        classes.mkdir(parents=True)
        (classes / "Example.class").write_bytes(b"")
        (classes / "Missing.class").write_bytes(b"")
        rows = derive.derive(self.report, self.root, "m3/core/src")
        self.assertEqual(["com/m3/Missing"], derive.unreported_classes(rows, self.root / "classes"))

    def test_class_without_source_under_root_is_refused(self):
        with self.assertRaisesRegex(ValueError, "no source under the root"):
            derive.derive(self.report, self.root, "m3/other/src")
        with self.assertRaisesRegex(ValueError, "repository-relative"):
            derive.derive(self.report, self.root, "../m3/core/src")


if __name__ == "__main__":
    unittest.main()
