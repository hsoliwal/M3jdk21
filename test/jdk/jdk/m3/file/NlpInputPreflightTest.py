#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Refusal, artifact, and existing-admission contracts for the NLP input preflight."""
import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock

ROOT = Path(__file__).resolve().parents[5]
OWNER = ROOT / "m3/tooling/module-packs/nlp-search"
sys.path.insert(0, str(OWNER))
SPEC = importlib.util.spec_from_file_location("nlp_input_build", OWNER / "build.py")
BUILD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BUILD)


class NlpInputPreflightTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.owner = self.root / "owner"
        self.owner.mkdir()
        self.out = self.root / "out"
        self.out.mkdir()
        for name, text in {"java/A.java": "class A {}\n", "java/B.java": "class B {}\n",
                           "native/a.c": "int value;\n", "native/a.h": "extern int value;\n"}.items():
            path = self.root / name
            path.parent.mkdir(exist_ok=True)
            path.write_text(text)
        self.lock = {"files": {name: hashlib.sha256((self.root / name).read_bytes()).hexdigest()
                              for name in ("java/A.java", "java/B.java", "native/a.c", "native/a.h")},
                     "java_source_sets": {"java": ["java/A.java", "java/B.java"]},
                     "native_source_sets": {"native": ["native/a.c", "native/a.h"]}}
        self.write_lock()

    def write_lock(self):
        self.lock_path = self.owner / "INPUTS.json"
        self.lock_path.write_text(json.dumps(self.lock) + "\n")

    def verify(self, accepted=False):
        before = self.lock_path.read_bytes() if self.lock_path.exists() else None
        with contextlib.redirect_stdout(io.StringIO()) as output:
            if accepted:
                result = BUILD.verify_input_lock(self.root, self.owner, self.out)
            else:
                with self.assertRaisesRegex(ValueError, "input custody failed"):
                    BUILD.verify_input_lock(self.root, self.owner, self.out)
                result = json.loads((self.out / "INPUT_CHECK.json").read_text())
        self.assertEqual(before, self.lock_path.read_bytes() if self.lock_path.exists() else None)
        self.assertEqual(result, json.loads((self.out / "INPUT_CHECK.json").read_text()))
        self.assertIn("M3_NLP_INPUT_CUSTODY_" + result["state"], output.getvalue())
        self.assertIn("M3_NLP_INPUT_CUSTODY_" + result["state"], (self.out / "input-check.log").read_text())
        self.assertEqual(accepted, result["state"] == "PASS")
        self.assertEqual("NLP_INPUT_CUSTODY_ONLY", result["gate"])
        return result

    def test_exact_inputs_pass_with_complete_receipt_and_no_source_mutation(self):
        before = {str(p.relative_to(self.root)): p.read_bytes() for p in self.root.rglob("*") if p.is_file()}
        result = self.verify(accepted=True)
        self.assertEqual(4, len(result["files"]))
        self.assertEqual(2, len(result["source_inventories"]))
        for path, data in before.items():
            self.assertEqual(data, (self.root / path).read_bytes())

    def test_multiple_failures_are_reported_without_short_circuiting(self):
        (self.root / "java/A.java").write_text("changed")
        (self.root / "java/B.java").unlink()
        (self.root / "native/z.c").write_text("unexpected")
        result = self.verify()
        self.assertEqual(["HASH_DRIFT", "MISSING", "MATCH", "MATCH"],
                         [row["state"] for row in result["files"]])
        self.assertEqual(["java/B.java"], result["source_inventories"][0]["missing"])
        self.assertEqual(["native/z.c"], result["source_inventories"][1]["unexpected"])

    def test_unreadable_file_is_refused_and_later_rows_are_checked(self):
        original = BUILD.digest
        def observed(path):
            if path.name == "A.java":
                raise PermissionError("blocked fixture")
            return original(path)
        with mock.patch.object(BUILD, "digest", side_effect=observed):
            result = self.verify()
        self.assertEqual(["UNREADABLE", "MATCH", "MATCH", "MATCH"],
                         [row["state"] for row in result["files"]])

    def test_missing_source_root_cannot_pass_an_empty_inventory(self):
        self.lock["java_source_sets"]["absent"] = []
        self.write_lock()
        result = self.verify()
        self.assertEqual("MISSING", result["source_inventories"][0]["state"])

    def test_duplicate_or_unsorted_inventory_is_still_refused(self):
        for expected in (["java/B.java", "java/A.java"], ["java/A.java", "java/A.java", "java/B.java"]):
            with self.subTest(expected=expected):
                self.lock["java_source_sets"]["java"] = expected
                self.write_lock()
                self.assertEqual("INVENTORY_DRIFT", self.verify()["source_inventories"][0]["state"])

    def test_java_named_directory_retains_original_inventory_refusal(self):
        (self.root / "java/Unexpected.java").mkdir()
        self.assertEqual(["java/Unexpected.java"], self.verify()["source_inventories"][0]["unexpected"])

    def test_native_headers_are_part_of_the_exact_inventory(self):
        (self.root / "native/extra.h").write_text("int header;\n")
        self.assertEqual(["native/extra.h"], self.verify()["source_inventories"][1]["unexpected"])

    def test_unrelated_resources_keep_original_admission(self):
        (self.root / "java/readme.txt").write_text("resource")
        (self.root / "native/readme.txt").write_text("resource")
        self.verify(accepted=True)

    def test_lock_syntax_failure_has_artifacts_and_exact_lock_digest(self):
        self.lock_path.write_text("{broken")
        result = self.verify()
        self.assertEqual("JSONDecodeError", result["lock_error"]["type"])
        self.assertEqual(hashlib.sha256(self.lock_path.read_bytes()).hexdigest(), result["input_lock_sha256"])

    def test_missing_lock_has_failure_artifacts(self):
        self.lock_path.unlink()
        result = self.verify()
        self.assertEqual("FileNotFoundError", result["lock_error"]["type"])
        self.assertIsNone(result["input_lock_sha256"])

    def test_invalid_lock_shapes_fail_closed(self):
        for invalid in ([], {}, {**self.lock, "files": []},
                        {**self.lock, "files": {"java/A.java": None}},
                        {**self.lock, "java_source_sets": {"java": "java/A.java"}},
                        {**self.lock, "native_source_sets": {"native": [1]}}):
            with self.subTest(invalid=invalid):
                self.lock_path.write_text(json.dumps(invalid))
                self.assertIn("lock_error", self.verify())

    def test_receipt_order_is_deterministic(self):
        first = self.verify(accepted=True)
        self.lock["files"] = dict(reversed(list(self.lock["files"].items())))
        self.write_lock()
        second = self.verify(accepted=True)
        first.pop("input_lock_sha256")
        second.pop("input_lock_sha256")
        self.assertEqual(first, second)

    def test_utf8_decode_failure_has_artifacts(self):
        self.lock_path.write_bytes(b"\xff")
        self.assertEqual("UnicodeDecodeError", self.verify()["lock_error"]["type"])

    def test_workflow_runs_regression_before_build_and_keeps_failure_artifacts(self):
        workflow = (ROOT / ".github/workflows/m3sdk-jmod-receiver.yml").read_text()
        start = workflow.index("  full-tool-images:")
        end = workflow.index("  jackson-source-jmods:", start)
        job = workflow[start:end]
        self.assertLess(job.index("NlpInputPreflightTest.py"), job.index("nlp-search/build.py"))
        self.assertIn("if: always()", job)
        self.assertIn("build/m3-nlp-search/**/*.json", job)
        self.assertIn("build/m3-nlp-search/**/*.log", job)
        self.assertNotIn("continue-on-error", job)
        filters = json.loads((ROOT / ".github/checks-superset/filters.json").read_text())
        self.assertIn("test/jdk/jdk/m3/file/NlpInputPreflightTest.py",
                      filters["members"]["m3sdk-jmod-receiver"]["pull_request"]["paths"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
