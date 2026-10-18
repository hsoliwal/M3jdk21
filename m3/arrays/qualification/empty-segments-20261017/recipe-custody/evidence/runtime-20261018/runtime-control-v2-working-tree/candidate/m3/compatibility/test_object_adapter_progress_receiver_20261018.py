#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Current-source and negative custody tests for the original receiver's explicit successor."""
from __future__ import annotations

import contextlib
import hashlib
import importlib.util
import io
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

sys.dont_write_bytecode = True
REPOSITORY = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("object_progress_receiver",
        Path(__file__).with_name("check_object_adapter_progress_receiver_20261017.py"))
CHECKER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CHECKER)
TEMPLATE_SPEC = importlib.util.spec_from_file_location("default_template_receiver",
        Path(__file__).with_name("verify_m3_default_template_20261017.py"))
TEMPLATE = importlib.util.module_from_spec(TEMPLATE_SPEC)
TEMPLATE_SPEC.loader.exec_module(TEMPLATE)


class ReceiverSuccessorTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="m3-receiver-successor-")
        self.root = Path(self.temp.name) / "receiver"
        self.root.mkdir()
        self.original = CHECKER.read_manifest(REPOSITORY, CHECKER.BASE_MANIFEST,
                                               CHECKER.BASE_MANIFEST_SHA256, 11)
        self.successor = CHECKER.read_manifest(REPOSITORY, CHECKER.NEXT_MANIFEST,
                                                CHECKER.NEXT_MANIFEST_SHA256, 5)
        self.documentation = CHECKER.read_manifest(REPOSITORY, CHECKER.DOCS_MANIFEST,
                                                   CHECKER.DOCS_MANIFEST_SHA256, 8)
        self.functional = CHECKER.read_manifest(REPOSITORY, CHECKER.FUNCTIONAL_MANIFEST,
                                                CHECKER.FUNCTIONAL_MANIFEST_SHA256, 3)
        paths = set(CHECKER.current_targets(REPOSITORY))
        paths.update((CHECKER.BASE_MANIFEST, CHECKER.NEXT_MANIFEST, CHECKER.DOCS_MANIFEST, CHECKER.FUNCTIONAL_MANIFEST))
        for relative in paths:
            destination = self.root / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(REPOSITORY / relative, destination)

    def tearDown(self):
        self.temp.cleanup()

    def run_check(self):
        with contextlib.redirect_stdout(io.StringIO()) as output:
            count = CHECKER.verify(self.root)
        return count, output.getvalue()

    def test_complete_current_receiver_passes(self):
        count, output = self.run_check()
        self.assertEqual(14, count)
        self.assertIn("Object compatibility and runtime admission remain HOLD", output)

    def test_verifier_does_not_write_sources_or_manifests(self):
        before = {p.relative_to(self.root): (p.read_bytes(), p.stat().st_mtime_ns)
                  for p in self.root.rglob("*") if p.is_file()}
        self.run_check()
        self.assertEqual(before, {p.relative_to(self.root): (p.read_bytes(), p.stat().st_mtime_ns)
                                 for p in self.root.rglob("*") if p.is_file()})

    def test_both_replaced_owners_reject_arbitrary_drift(self):
        for relative in sorted(CHECKER.REPLACED):
            with self.subTest(path=relative):
                path = self.root / relative
                before = path.read_bytes()
                path.write_bytes(before + b"\n// unexpected source\n")
                with self.assertRaisesRegex(RuntimeError, "source drift"):
                    self.run_check()
                path.write_bytes(before)

    def test_unchanged_original_owner_still_required(self):
        (self.root / "src/java.base/share/classes/java/lang/Object.java").write_text("class Object {}\n")
        with self.assertRaisesRegex(RuntimeError, "source drift"):
            self.run_check()

    def test_every_successor_addition_is_required(self):
        for relative in sorted(CHECKER.ADDED):
            with self.subTest(path=relative):
                path = self.root / relative
                before = path.read_bytes()
                path.unlink()
                with self.assertRaisesRegex(RuntimeError, "missing"):
                    self.run_check()
                path.write_bytes(before)

    def test_historical_manifests_cannot_be_resealed_silently(self):
        for relative in (CHECKER.BASE_MANIFEST, CHECKER.NEXT_MANIFEST, CHECKER.DOCS_MANIFEST, CHECKER.FUNCTIONAL_MANIFEST):
            with self.subTest(path=relative):
                path = self.root / relative
                before = path.read_bytes()
                path.write_bytes(before + b"# rewritten history\n")
                with self.assertRaisesRegex(RuntimeError, "historical manifest custody drift"):
                    self.run_check()
                path.write_bytes(before)

    def test_missing_successor_manifest_refuses(self):
        (self.root / CHECKER.NEXT_MANIFEST).unlink()
        with self.assertRaisesRegex(RuntimeError, "missing"):
            self.run_check()

    def test_exact_content_symlink_is_not_admitted(self):
        path = self.root / "src/java.base/share/classes/java/lang/Object.java"
        outside = Path(self.temp.name) / "Object.java"
        outside.write_bytes(path.read_bytes())
        path.unlink()
        path.symlink_to(outside)
        with self.assertRaisesRegex(RuntimeError, "symlink"):
            self.run_check()

    def test_symlink_parent_is_not_admitted(self):
        directory = self.root / "src/java.base/share/classes/java/lang"
        outside = Path(self.temp.name) / "lang"
        directory.rename(outside)
        directory.symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(RuntimeError, "symlink"):
            self.run_check()

    def test_unrelated_repository_files_do_not_widen_scope(self):
        (self.root / "unrelated.txt").write_text("not a receiver owner\n")
        self.assertEqual(14, self.run_check()[0])

    def fixture_manifest(self, lines, count):
        path = self.root / "fixture.tsv"
        data = ("\n".join(lines) + "\n").encode()
        path.write_bytes(data)
        return CHECKER.read_manifest(self.root, "fixture.tsv", hashlib.sha256(data).hexdigest(), count)

    def test_manifest_duplicate_and_unsafe_paths_refuse(self):
        row = "src/java.base/share/classes/A.java\tABSENT\t" + "a" * 64 + "\ta.txt"
        with self.assertRaisesRegex(RuntimeError, "duplicate"):
            self.fixture_manifest([row, row], 2)
        for path in ("src/java.base/share/classes/../A.java", "test/jdk/../A.java", "/A.java"):
            with self.subTest(path=path), self.assertRaisesRegex(RuntimeError, "invalid"):
                self.fixture_manifest([path + "\tABSENT\t" + "a" * 64 + "\ta.txt"], 1)

    def test_manifest_shape_hash_and_coverage_refuse(self):
        for line in ("only\ttwo", "test/jdk/A.java\tBAD\t" + "a" * 64 + "\ta.txt",
                     "test/jdk/A.java\tABSENT\tbad\ta.txt",
                     "test/jdk/A.java\tABSENT\t" + "a" * 64 + "\t../a.txt"):
            with self.subTest(line=line), self.assertRaisesRegex(RuntimeError, "invalid"):
                self.fixture_manifest([line], 1)
        with self.assertRaisesRegex(RuntimeError, "coverage"):
            self.fixture_manifest(["test/jdk/A.java\tABSENT\t" + "a" * 64 + "\ta.txt"], 2)

    def test_successor_must_start_at_the_published_postimage(self):
        next_rows = dict(self.successor)
        path = sorted(CHECKER.REPLACED)[0]
        _, after, template = next_rows[path]
        next_rows[path] = ("f" * 64, after, template)
        with self.assertRaisesRegex(RuntimeError, "broken historical transition"):
            CHECKER.compose_targets(self.original, next_rows)

    def test_additions_must_be_explicitly_absent_before(self):
        next_rows = dict(self.successor)
        path = sorted(CHECKER.ADDED)[0]
        _, after, template = next_rows[path]
        next_rows[path] = ("f" * 64, after, template)
        with self.assertRaisesRegex(RuntimeError, "not absent"):
            CHECKER.compose_targets(self.original, next_rows)

    def test_unknown_successor_owner_refuses(self):
        next_rows = dict(self.successor)
        next_rows["test/jdk/Unknown.java"] = ("ABSENT", "a" * 64, "unknown.txt")
        with self.assertRaisesRegex(RuntimeError, "successor closure"):
            CHECKER.compose_targets(self.original, next_rows)

    def test_missing_original_overlap_refuses(self):
        original = dict(self.original)
        original["test/jdk/Unexpected.java"] = original.pop(sorted(CHECKER.REPLACED)[0])
        with self.assertRaisesRegex(RuntimeError, "overlap"):
            CHECKER.compose_targets(original, self.successor)

    def test_documentation_must_follow_the_current_published_source(self):
        rows = dict(self.documentation)
        path = sorted(CHECKER.DOCUMENTED)[0]
        _, after, template = rows[path]
        rows[path] = ("f" * 64, after, template)
        with self.assertRaisesRegex(RuntimeError, "broken documentation transition"):
            CHECKER.compose_documentation(CHECKER.compose_targets(self.original, self.successor), rows)

    def test_documentation_cannot_add_an_arbitrary_owner(self):
        rows = dict(self.documentation)
        rows["test/jdk/Unexpected.java"] = rows.pop(sorted(rows)[0])
        with self.assertRaisesRegex(RuntimeError, "documentation successor closure"):
            CHECKER.compose_documentation(CHECKER.compose_targets(self.original, self.successor), rows)

    def test_each_documented_owner_rejects_source_drift(self):
        for relative in sorted(CHECKER.DOCUMENTED):
            with self.subTest(path=relative):
                path = self.root / relative
                before = path.read_bytes()
                path.write_bytes(before + b"\n// unqualified documentation change\n")
                with self.assertRaisesRegex(RuntimeError, "source drift"):
                    self.run_check()
                path.write_bytes(before)

    def test_default_template_checker_accepts_the_exact_successor(self):
        with contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(5, TEMPLATE.run(self.root))
        self.assertIn("PASS 5 exact current M3 Java/template receiver digests", output.getvalue())
        self.assertIn("remain HOLD", output.getvalue())

    def test_default_template_checker_rejects_each_owner_drift(self):
        for relative in sorted(CHECKER.REPLACED | CHECKER.ADDED):
            with self.subTest(path=relative):
                path = self.root / relative
                before = path.read_bytes()
                path.write_bytes(before + b"\n// drift\n")
                with self.assertRaisesRegex(RuntimeError, "source drift"):
                    TEMPLATE.run(self.root)
                path.write_bytes(before)

    def test_default_template_checker_requires_documentation_manifest(self):
        (self.root / CHECKER.DOCS_MANIFEST).unlink()
        with self.assertRaisesRegex(RuntimeError, "missing"):
            TEMPLATE.run(self.root)

    def test_default_template_checker_preserves_original_manifest_custody(self):
        path = self.root / CHECKER.NEXT_MANIFEST
        path.write_bytes(path.read_bytes() + b"# unqualified amendment\n")
        with self.assertRaisesRegex(RuntimeError, "historical manifest custody drift"):
            TEMPLATE.run(self.root)

    def test_functional_documentation_must_follow_the_exact_current_source(self):
        rows = dict(self.functional)
        path = sorted(CHECKER.FUNCTIONAL)[0]
        _, after, template = rows[path]
        rows[path] = ("f" * 64, after, template)
        with self.assertRaisesRegex(RuntimeError, "broken functional transition"):
            CHECKER.compose_functional(CHECKER.current_targets(REPOSITORY), rows)

    def test_functional_documentation_cannot_add_an_arbitrary_owner(self):
        rows = dict(self.functional)
        rows["test/jdk/Unexpected.java"] = rows.pop(sorted(rows)[0])
        with self.assertRaisesRegex(RuntimeError, "functional successor closure"):
            CHECKER.compose_functional(CHECKER.current_targets(REPOSITORY), rows)

    def test_each_functional_owner_rejects_source_drift(self):
        for relative in sorted(CHECKER.FUNCTIONAL):
            with self.subTest(path=relative):
                path = self.root / relative
                before = path.read_bytes()
                path.write_bytes(before + b"\n// unqualified functional change\n")
                with self.assertRaisesRegex(RuntimeError, "source drift"):
                    self.run_check()
                path.write_bytes(before)

    def test_default_template_checker_requires_functional_manifest(self):
        (self.root / CHECKER.FUNCTIONAL_MANIFEST).unlink()
        with self.assertRaisesRegex(RuntimeError, "missing"):
            TEMPLATE.run(self.root)


if __name__ == "__main__":
    unittest.main()
