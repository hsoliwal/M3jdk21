#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Capture scoped admission/API/header helper controls for the unchanged runner.

These are synthetic input-boundary fixtures, not collections runtime tests.
The generated runtime receipt remains a separate qualification owner.
"""

import argparse
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.dont_write_bytecode = True
PARSER = argparse.ArgumentParser(description=__doc__)
PARSER.add_argument("--runner", required=True, type=Path)
PARSER.add_argument("--headers", required=True, type=Path)
PARSER.add_argument("--receipt", required=True, type=Path)
ARGS = PARSER.parse_args()
SPEC = importlib.util.spec_from_file_location("qualified_wrapper_verifier", ARGS.runner)
V = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(V)


def digest(data):
    return hashlib.sha256(data).hexdigest()


class Helpers(unittest.TestCase):
    def setUp(self):
        self.scratch = tempfile.TemporaryDirectory(prefix="helper-fixture-", dir=ARGS.receipt.parent)
        self.addCleanup(self.scratch.cleanup)
        self.root = Path(self.scratch.name)
        self.target = self.root / "target"
        self.before = self.root / "before"
        self.target.mkdir()
        self.before.mkdir()
        paths = {V.JAVA + "com/m3/collections/" + n + ".java" for n in V.RUNTIME_OWNERS}
        paths |= {V.NATIVE + n for n in ("m3_index_path_masks.c", "m3_index_mask_compactor.c", "m3_index_mask_window.c")}
        names = set(V.FROZEN_TESTS) | {n for n, _, _ in V.MASK_PROOFS} | {n for n, _ in V.WRAPPER_PROOFS}
        names |= {"M3NavigationFailureTest", "M3BoundedFilteredNavigationTest"}
        paths |= {V.TEST + n + ".java" for n in names}
        paths |= {V.MODULE + n for n in V.RESOURCE_FILES}
        paths.add(V.HERE + "verify.py")
        rows = []
        for relative in sorted(paths):
            script = relative == V.HERE + "verify.py"
            data = ARGS.runner.read_bytes() if script else ("synthetic helper fixture: " + relative + "\n").encode()
            target = self.target / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            if not script:
                old = self.before / relative
                old.parent.mkdir(parents=True, exist_ok=True)
                old.write_bytes(data)
            rows.append({"path": relative, "before_sha256": "ABSENT" if script else digest(data),
                         "after_sha256": digest(data), "role": "synthetic-helper-fixture"})
        self.manifest = {"schema": V.SCHEMA, "provenance": {"kind": "synthetic helper fixture"}, "files": rows}
        self.path = self.root / "SOURCE_MANIFEST.json"
        self.source = V.JAVA + "com/m3/collections/M3IndexPathMasks.java"

    def row(self, relative=None):
        return next(row for row in self.manifest["files"] if row["path"] == (relative or self.source))

    def admit(self):
        self.path.write_text(json.dumps(self.manifest))
        return V.read_manifest(self.path, self.target, self.before)

    def refusal(self, message):
        with self.assertRaisesRegex(V.Refusal, message):
            self.admit()

    def test_01_valid_manifest(self):
        _, rows = self.admit()
        self.assertEqual(set(rows), {r["path"] for r in self.manifest["files"]})
        self.assertLessEqual(V.module_inputs(self.target).keys(), rows.keys())

    def test_02_unsupported_schema(self):
        self.manifest["schema"] = "unqualified"
        self.refusal("unsupported source manifest schema")

    def test_03_empty_provenance(self):
        self.manifest["provenance"] = {}
        self.refusal("source provenance must be a nonempty object")

    def test_04_duplicate_source(self):
        self.manifest["files"].append(copy.deepcopy(self.row()))
        self.refusal("duplicate source")

    def test_05_invalid_preimage_hash(self):
        self.row()["before_sha256"] = "invalid"
        self.refusal("invalid before hash")

    def test_06_invalid_postimage_hash(self):
        self.row()["after_sha256"] = "invalid"
        self.refusal("invalid after hash")

    def test_07_path_traversal(self):
        self.row()["path"] = "m3/collections/../outside"
        self.refusal("unsafe source path")

    def test_08_absolute_path(self):
        self.row()["path"] = "/m3/collections/outside"
        self.refusal("unsafe source path")

    def test_09_unnormalized_path(self):
        self.row()["path"] = "m3/collections//src/input"
        self.refusal("unsafe source path")

    def test_10_file_symlink(self):
        source = self.target / self.source
        source.unlink()
        source.symlink_to(self.before / self.source)
        self.refusal("source alias refused")

    def test_11_parent_symlink(self):
        parent = (self.target / self.source).parent
        moved = self.root / "moved-sources"
        parent.rename(moved)
        parent.symlink_to(moved, target_is_directory=True)
        self.refusal("source alias refused")

    def test_12_missing_postimage(self):
        (self.target / self.source).unlink()
        self.refusal("missing regular source")

    def test_13_postimage_drift(self):
        (self.target / self.source).write_bytes(b"changed\n")
        self.refusal("postimage source drift")

    def test_14_preimage_drift(self):
        (self.before / self.source).write_bytes(b"changed\n")
        self.refusal("preimage source drift")

    def test_15_expected_absent_is_present(self):
        source = self.before / V.HERE / "verify.py"
        source.parent.mkdir(parents=True)
        source.write_bytes(ARGS.runner.read_bytes())
        self.refusal("expected absent preimage")

    def test_16_required_owner_omitted(self):
        self.manifest["files"].remove(self.row())
        self.refusal("required source omitted")

    def test_17_executing_runner_binding(self):
        data = b"different runner\n"
        (self.target / V.HERE / "verify.py").write_bytes(data)
        self.row(V.HERE + "verify.py")["after_sha256"] = digest(data)
        self.refusal("executing runner differs")

    def test_18_empty_source_role(self):
        self.row()["role"] = ""
        self.refusal("missing source role")

    def test_19_api_changed_descriptor_is_reported(self):
        before = {"sample.Owner": {"declaration": "public class sample.Owner {",
                                   "members": ["public int get(); descriptor: ()I"]}}
        after = copy.deepcopy(before)
        after["sample.Owner"]["members"] = ["public long get(); descriptor: ()J"]
        self.assertEqual(len(V.compare_api(before, after)["removed_or_changed"]), 1)

    def test_20_api_retained_with_concrete_addition(self):
        before = {"sample.Owner": {"declaration": "public class sample.Owner {",
                                   "members": ["public int get(); descriptor: ()I"]}}
        after = copy.deepcopy(before)
        after["sample.Owner"]["members"].append("public long getLong(); descriptor: ()J")
        result = V.compare_api(before, after)
        self.assertEqual(result["removed_or_changed"], [])
        self.assertEqual(len(result["added"]), 1)

    def test_21_api_new_abstract_obligation_is_reported(self):
        before = {"sample.Owner": {"declaration": "public interface sample.Owner {", "members": []}}
        after = copy.deepcopy(before)
        after["sample.Owner"]["members"].append("public abstract int get(); descriptor: ()I")
        self.assertEqual(len(V.compare_api(before, after)["removed_or_changed"]), 1)

    def test_22_actual_generated_int_long_jni_header(self):
        path, prototypes = V.jni_prototypes(ARGS.headers, "M3IndexPathMasks")
        self.assertTrue(path.is_file())
        self.assertEqual(set(prototypes), {"Java_com_m3_collections_M3IndexPathMasks_selectNative",
                                         "Java_com_m3_collections_M3IndexPathMasks_selectLongNative"})
        self.assertIn("jintArray", prototypes["Java_com_m3_collections_M3IndexPathMasks_selectNative"])
        self.assertIn("jlongArray", prototypes["Java_com_m3_collections_M3IndexPathMasks_selectLongNative"])


SUITE = unittest.defaultTestLoader.loadTestsFromTestCase(Helpers)
NAMES = [test.id() for test in SUITE]
RESULT = unittest.TextTestRunner(verbosity=2).run(SUITE)
RECEIPT = {"schema": "M3_WRAPPER_RUNNER_HELPER_REPLAY_V1", "status": "PASS" if RESULT.wasSuccessful() else "FAIL",
           "runner": str(ARGS.runner), "runner_sha256": digest(ARGS.runner.read_bytes()),
           "helper_sha256": digest(Path(__file__).read_bytes()), "tests": RESULT.testsRun,
           "failures": len(RESULT.failures), "errors": len(RESULT.errors), "skipped": len(RESULT.skipped),
           "test_names": NAMES,
           "jni_header_sha256": digest((ARGS.headers / "com_m3_collections_M3IndexPathMasks.h").read_bytes()),
           "scope": "Synthetic helper replay captured after proof2 qualification; not a reproduction claim for prior inline checks",
           "product_runtime_tests": "Separate root-owned current-runtime/RECEIPT.json",
           "product_sources_written": False}
ARGS.receipt.write_text(json.dumps(RECEIPT, indent=2) + "\n")
print("M3_WRAPPER_RUNNER_HELPER_REPLAY_" + RECEIPT["status"] + " cases=" + str(RESULT.testsRun))
sys.exit(0 if RESULT.wasSuccessful() and RESULT.testsRun == 22 else 1)
