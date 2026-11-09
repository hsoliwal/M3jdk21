# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Positive and hostile-source tests for the read-only M3JDK21 receiving gate."""
from __future__ import annotations

from copy import deepcopy
import importlib.util
import json
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("m3_receiver_verifier", HERE / "verify_receipt.py")
receiver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(receiver)


class SourceSealedReceiverTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.receipt = json.loads(receiver.RECEIPT.read_text(encoding="utf-8"),
                                 object_pairs_hook=receiver.no_duplicate_keys)

    def test_exact_current_product_postimages(self):
        self.assertEqual(receiver.verify(receiver.ROOT, self.receipt), (3, 10))

    def test_source_hash_drift_refused(self):
        changed = deepcopy(self.receipt)
        changed["source_files"][0]["sha256_after"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "source SHA-256 drift"):
            receiver.verify(receiver.ROOT, changed)

    def test_donor_catalogue_blob_drift_refused(self):
        changed = deepcopy(self.receipt)
        changed["catalogue"]["git_blob_sha1"] = "0" * 40
        with self.assertRaisesRegex(ValueError, "donor catalogue Git blob drift"):
            receiver.verify(receiver.ROOT, changed)

    def test_repeated_source_mapping_refused(self):
        changed = deepcopy(self.receipt)
        changed["source_files"].append(deepcopy(changed["source_files"][-1]))
        with self.assertRaisesRegex(ValueError, "exactly three"):
            receiver.verify(receiver.ROOT, changed)

    def test_untrusted_relative_path_refused(self):
        for name in ("../outside.java", "m3/../escape.java", "m3//bad.java",
                     "C:/m3/out.java", "/m3/out.java", "m3\\x.java"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                receiver.exact_file(receiver.ROOT, name)

    def test_legacy_native_source_mutation_refused(self):
        changed = deepcopy(self.receipt)
        changed["native_sources_unchanged"][0]["git_blob_sha1"] = "0" * 40
        with self.assertRaisesRegex(ValueError, "source Git blob drift"):
            receiver.verify(receiver.ROOT, changed)

    def test_duplicate_json_field_refused(self):
        with self.assertRaisesRegex(ValueError, "duplicate receipt JSON key"):
            json.loads('{"schema":"first","schema":"shadow"}',
                       object_pairs_hook=receiver.no_duplicate_keys)


if __name__ == "__main__":
    unittest.main()
