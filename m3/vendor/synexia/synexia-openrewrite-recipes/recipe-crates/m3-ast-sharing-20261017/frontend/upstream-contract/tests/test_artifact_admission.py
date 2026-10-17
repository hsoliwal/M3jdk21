# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Finite metadata admission tests; these do not execute or qualify a compiler."""
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import artifact_admission as admission


class ArtifactAdmissionTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="m3-artifact-admission-test-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.patch = self.root / "unit-only-patch-bytes"
        self.patch.write_bytes(b"metadata admission unit fixture; not a compiler artifact\n")
        self.receipt = self.root / "ARTIFACTS.json"
        self.value = {"compiler_sources": 4,
                      "compiler_source_sha256": admission.compiler_sources(),
                      "patch_sha256": admission.file_sha(self.patch), "plugin_sha256": "12" * 32}

    def write(self):
        self.receipt.write_text(json.dumps(self.value), encoding="utf-8")
        return self.receipt

    def test_sealed_receipt_admits_its_actual_digest_and_all_four_sources(self):
        result = admission.admit_patch(self.patch, self.write())
        self.assertEqual("sealed-build-receipt", result["mode"])
        self.assertEqual(self.value["patch_sha256"], result["patch_sha256"])
        self.assertEqual(admission.COMPILER_MANIFEST_SHA, result["compiler_manifest_sha256"])
        self.assertEqual(admission.file_sha(self.receipt), result["receipt_sha256"])
        self.assertEqual(self.value["compiler_source_sha256"], result["compiler_source_sha256"])

    def test_default_exact_pin_is_preserved(self):
        with mock.patch.object(admission, "file_sha", return_value=admission.PATCH_SHA):
            self.assertEqual({"mode": "exact-artifact-pin", "patch_sha256": admission.PATCH_SHA},
                             admission.admit_patch(self.patch))
        with self.assertRaisesRegex(ValueError, "unqualified compiler patch bytes"):
            admission.admit_patch(self.patch)

    def test_digest_mismatch_refuses(self):
        self.value["patch_sha256"] = "00" * 32
        with self.assertRaisesRegex(ValueError, "patch digest"):
            admission.admit_patch(self.patch, self.write())

    def test_missing_source_map_refuses_digest_only_override(self):
        del self.value["compiler_source_sha256"]
        with self.assertRaisesRegex(ValueError, "source pins"):
            admission.admit_patch(self.patch, self.write())

    def test_source_hash_mismatch_refuses(self):
        owner = next(iter(self.value["compiler_source_sha256"]))
        self.value["compiler_source_sha256"][owner] = "00" * 32
        with self.assertRaisesRegex(ValueError, "source pins"):
            admission.admit_patch(self.patch, self.write())

    def test_source_set_mismatch_refuses(self):
        owner = next(iter(self.value["compiler_source_sha256"]))
        self.value["compiler_source_sha256"]["com/sun/tools/javac/Unexpected.java"] = self.value["compiler_source_sha256"].pop(owner)
        with self.assertRaisesRegex(ValueError, "source pins"):
            admission.admit_patch(self.patch, self.write())

    def test_count_mismatch_refuses(self):
        for value in (True, 3, 5, "4"):
            with self.subTest(value=value):
                self.value["compiler_sources"] = value
                with self.assertRaisesRegex(ValueError, "exactly four"):
                    admission.admit_patch(self.patch, self.write())

    def test_manifest_drift_refuses(self):
        resource = self.root / "resource"
        shutil.copytree(admission.COMPILER_RESOURCE, resource)
        manifest = resource / "manifest.tsv"
        manifest.write_bytes(manifest.read_bytes() + b"\n")
        with self.assertRaisesRegex(ValueError, "manifest drift"):
            admission.admit_patch(self.patch, self.write(), resource)

    def test_template_drift_refuses(self):
        resource = self.root / "resource"
        shutil.copytree(admission.COMPILER_RESOURCE, resource)
        template = next(resource.glob("*.java.txt"))
        template.write_bytes(template.read_bytes() + b"\n// changed\n")
        with self.assertRaisesRegex(ValueError, "template drift"):
            admission.admit_patch(self.patch, self.write(), resource)

    def test_duplicate_receipt_keys_refuse(self):
        self.write()
        text = self.receipt.read_text()
        self.receipt.write_text('{"compiler_sources":4,' + text[1:])
        with self.assertRaisesRegex(ValueError, "duplicate artifact receipt key"):
            admission.admit_patch(self.patch, self.receipt)


if __name__ == "__main__":
    unittest.main()
