#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Independent negative controls: no synthetic source is counted as executed JUnit."""
import tempfile
from pathlib import Path
import hashlib
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


class InteropV4HistoricalSuccessionTest(unittest.TestCase):
    def temporary_root(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        return Path(temporary.name).resolve()

    def rows(self):
        return gate.succession_rows(gate.HERE.parents[4])

    def test_all_historical_owners_and_exact_additions_are_accounted_for(self):
        rows = self.rows()
        self.assertEqual(90, len(rows))
        self.assertEqual(84, len({r[3] for r in rows if r[3] != "ABSENT"}))
        self.assertEqual(gate.LATER_ADDITIONS | {r[0] for r in gate.EXPECTED},
                         {r[0] for r in rows if r[3] == "ABSENT"})
        self.assertEqual(4, sum(r[6] == "NAMESPACE_TYPE_NAMES_AND_COMMENTS" for r in rows))
        self.assertEqual(4, sum(r[6] == "QUALIFIED_SOURCE_SUCCESSOR" for r in rows))

    def test_historical_and_current_authorities_are_not_conflated(self):
        self.assertEqual("48226ca99a17b81deb1848a8e80b15f166819e81", gate.V3_RECEIPT_BLOB)
        self.assertEqual("9fdc61dcf42e190add3458f2f55dd29203f4a540", gate.V3_SCRIPT_BLOB)
        self.assertNotEqual(gate.V3_RECEIPT_BLOB, gate.CURRENT_V3_RECEIPT_BLOB)
        self.assertNotEqual(gate.V3_SCRIPT_BLOB, gate.CURRENT_V3_SCRIPT_BLOB)
        self.assertEqual(6, sum(len(r[3].split(",")) for r in gate.EXPECTED))

    def test_manifest_bytes_cannot_be_recatalogued(self):
        root = self.temporary_root()
        path = root / gate.SUCCESSIONS
        path.parent.mkdir(parents=True)
        original = (gate.HERE.parents[4] / gate.SUCCESSIONS).read_bytes()
        path.write_bytes(original.replace(b"namespace-source-comparison", b"invented-source-comparison", 1))
        with self.assertRaisesRegex(ValueError, "manifest content drift"):
            gate.succession_rows(root)

    def test_duplicate_omitted_reordered_and_escaped_rows_refused(self):
        original = self.rows()
        for rows in (original[:-1], original + original[:1], tuple(reversed(original))):
            with self.subTest(rows=len(rows)), self.assertRaisesRegex(ValueError, "shape, ordering"):
                gate.validate_succession_rows(rows)
        for replacement in ("/tmp/Outside.java", "m3/collections/interop/src/main/java/jdk/m3/collections/interop/../Outside.java"):
            rows = [list(r) for r in original]
            rows[0][0] = replacement
            rows.sort(key=lambda r: r[0])
            with self.subTest(path=replacement), self.assertRaises(ValueError):
                gate.validate_succession_rows(rows)

    def test_wrong_before_path_hash_disposition_or_attribution_refused(self):
        original = self.rows()
        for column, value in ((3, original[0][0]), (4, "g" * 64),
                              (5, "f" * 39), (6, "UNREVIEWED"), (7, "invented-authority")):
            rows = [list(r) for r in original]
            rows[0][column] = value
            with self.subTest(column=column), self.assertRaises(ValueError):
                gate.validate_succession_rows(rows)
        rows = [list(r) for r in original]
        rows[0][6] = "NAMESPACE"
        with self.assertRaisesRegex(ValueError, "cardinality"):
            gate.validate_succession_rows(rows)

    def test_missing_direct_and_parent_aliases_refused(self):
        root, other = self.temporary_root(), self.temporary_root()
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            gate.checked_file(root, "source.java")
        (other / "source.java").write_text("class Source {}")
        (root / "source.java").symlink_to(other / "source.java")
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            gate.checked_file(root, "source.java")
        (root / "alias").symlink_to(other, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            gate.checked_file(root, "alias/source.java")
        with self.assertRaisesRegex(ValueError, "unsafe source path"):
            gate.checked_file(root, "../source.java")

    def test_unverified_historical_code_is_rejected_before_execution(self):
        root = self.temporary_root()
        source = root / gate.V3_SCRIPT
        source.parent.mkdir(parents=True)
        source.write_text("raise AssertionError('unverified code executed')\n")
        (root / gate.v3.RECEIPT_REL).write_text("unverified receipt\n")
        with self.assertRaisesRegex(ValueError, "historical V3 authority or receipt drift"):
            gate.load_original_v3(root)
        source.unlink()
        outside = self.temporary_root() / "verifier.py"
        outside.write_text("raise AssertionError('aliased code executed')\n")
        source.symlink_to(outside)
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            gate.load_original_v3(root)

    def test_unverified_current_helper_cannot_define_its_own_hash_authority(self):
        root = self.temporary_root()
        source = root / gate.V3_SCRIPT
        source.parent.mkdir(parents=True)
        source.write_text("raise AssertionError('unverified current helper executed')\n"
                          "def git_blob(raw): return '" + gate.CURRENT_V3_SCRIPT_BLOB + "'\n")
        with self.assertRaisesRegex(ValueError, "current V3 executable authority drift"):
            gate.load_current_v3(root)

    def source_fixture(self):
        old, now = self.temporary_root(), self.temporary_root()
        before = gate.APPROVED_PREFIX + "main/java/com/m3/collections/interop/Fixture.java"
        after = before.replace("/com/m3/", "/jdk/m3/")
        old_bytes = b"package com.m3.collections.interop; class Fixture {}\n"
        new_bytes = b"package jdk.m3.collections.interop; class Fixture {}\n"
        for root, path, data in ((old, before, old_bytes), (now, after, new_bytes)):
            target = root / path
            target.parent.mkdir(parents=True)
            target.write_bytes(data)
        row = (after, hashlib.sha256(new_bytes).hexdigest(), gate.v3.git_blob(new_bytes),
               before, hashlib.sha256(old_bytes).hexdigest(), gate.v3.git_blob(old_bytes),
               "NAMESPACE", "namespace-source-comparison")
        previous = {before: ("100644", row[5])}
        current = {after: ("100644", row[2])}
        return old, now, (row,), previous, current

    def test_source_worktree_and_index_mutations_are_refused(self):
        old, now, rows, previous, current = self.source_fixture()
        gate.prove_successions(now, old, rows, previous, current)
        (now / rows[0][0]).write_bytes(b"class Mutated {}\n")
        with self.assertRaisesRegex(ValueError, "current succession drift"):
            gate.prove_successions(now, old, rows, previous, current)
        old, now, rows, previous, current = self.source_fixture()
        (old / rows[0][3]).write_bytes(b"class HistoricalMutation {}\n")
        with self.assertRaisesRegex(ValueError, "historical source succession drift"):
            gate.prove_successions(now, old, rows, previous, current)
        for changed in ({}, {**current, "rogue.java": ("100644", "a" * 40)},
                        {rows[0][0]: ("100755", rows[0][2])}):
            with self.assertRaises(ValueError):
                gate.prove_successions(now, old, rows, previous, changed)


if __name__ == "__main__":
    unittest.main(verbosity=2)
