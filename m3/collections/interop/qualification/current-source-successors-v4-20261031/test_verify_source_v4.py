#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Independent negative controls: no synthetic source is counted as executed JUnit."""
import tempfile
from pathlib import Path
import unittest
import verify_source_v4 as gate


class InteropV4AdditiveSourceTest(unittest.TestCase):
    def receipt(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        path = Path(tmp.name) / "ADDITIVE_TEST_SOURCES_V4.tsv"
        path.write_text("# source-only\n" +
                        "".join("\t".join(row) + "\n" for row in gate.EXPECTED),
                        encoding="utf-8")
        return path

    def test_exact_two_sources_six_independent_junit_methods(self):
        rows = gate.receipt_rows(self.receipt())
        self.assertEqual(2, len(rows))
        self.assertEqual(6, sum(len(r[3].split(",")) for r in rows))

    def test_reordered_receipt_refused(self):
        path = self.receipt()
        path.write_text("".join("\t".join(row) + "\n"
                                for row in reversed(gate.EXPECTED)))
        with self.assertRaisesRegex(ValueError, "manifest drift"):
            gate.receipt_rows(path)

    def test_duplicate_and_changed_receipts_refused(self):
        path = self.receipt()
        path.write_text(path.read_text() + "\t".join(gate.EXPECTED[0]) + "\n")
        with self.assertRaisesRegex(ValueError, "manifest drift"):
            gate.receipt_rows(path)
        path = self.receipt()
        path.write_text(path.read_text().replace(gate.EXPECTED[1][1], "0" * 64))
        with self.assertRaisesRegex(ValueError, "manifest drift"):
            gate.receipt_rows(path)

    def test_missing_and_aliased_receipts_refused(self):
        path = self.receipt()
        path.unlink()
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            gate.receipt_rows(path)
        destination = self.receipt()
        path.symlink_to(destination)
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            gate.receipt_rows(path)

    def test_deleted_extra_and_modified_source_refused(self):
        original = {"src/A.java": ("100644", "a" * 40)}
        with self.assertRaisesRegex(ValueError, "deleted"):
            gate.require_additive(original, {}, set())
        with self.assertRaisesRegex(ValueError, "unadmitted"):
            gate.require_additive(original, {**original, "rogue.java": ("100644", "b" * 40)}, set())
        with self.assertRaisesRegex(ValueError, "index drift"):
            gate.require_additive(original, {"src/A.java": ("100644", "b" * 40)}, set())

    def test_missing_or_duplicate_junit_method_refused(self):
        file, _, _, methods = gate.EXPECTED[1]
        source = "// SPDX-License-Identifier: Apache-2.0\n" + "\n".join(
            "@Test void " + name + "() {}" for name in methods.split(","))
        self.assertEqual(2, len(gate.dedicated_methods(file, source.encode(), methods)))
        with self.assertRaisesRegex(ValueError, "method drift"):
            gate.dedicated_methods(file, source.replace("@Test", "@Disabled").encode(), methods)
        with self.assertRaisesRegex(ValueError, "method drift"):
            gate.dedicated_methods(file, (source + "\n@Test void unexpected() {}").encode(), methods)
        with self.assertRaisesRegex(ValueError, "method drift"):
            gate.dedicated_methods(file, (source + "\n@Test void " + methods.split(",")[0] + "() {}").encode(), methods)


if __name__ == "__main__":
    unittest.main(verbosity=2)
