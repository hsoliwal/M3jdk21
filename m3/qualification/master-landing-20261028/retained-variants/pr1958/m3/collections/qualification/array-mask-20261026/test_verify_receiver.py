# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Product-owned receiver regressions; no recipes or rewrite-side state."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest

SOURCE = Path(__file__).resolve().parent / "verify_receiver.py"
spec = importlib.util.spec_from_file_location("m3jdk21_array_mask_receiver", SOURCE)
receiver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(receiver)


class ReceiverTests(unittest.TestCase):
    def test_real_checkout_exact_postimages_and_native_blobs(self):
        result = receiver.verify()
        self.assertEqual(result["result"], "PASS")
        self.assertEqual(result["exact_postimages"], 3)
        self.assertEqual(result["unchanged_native_sources"], 2)

    def test_changed_bytes_refuse_even_if_size_constant(self):
        raw = b"correct source"
        digest = receiver.blob(raw)
        changed = bytearray(raw)
        changed[0] = ord("X")
        with self.assertRaisesRegex(ValueError, "Git source blob drift"):
            receiver.verify_blob(bytes(changed), digest)

    def test_mismatching_sha256_is_refused(self):
        raw = b"one immutable source"
        with self.assertRaisesRegex(ValueError, "SHA-256 source drift"):
            receiver.verify_blob(raw, receiver.blob(raw), "0" * 64)

    def test_noncanonical_and_traversal_paths_refused(self):
        for item in ("/tmp/file", "../M3ArrayList.java", "m3//collections",
                     "m3/./collections", "m3\\collections", "C:/bad"):
            with self.subTest(path=item), self.assertRaises(ValueError):
                receiver.safe_file(item)

    def test_missing_target_source_refused(self):
        with self.assertRaisesRegex(ValueError, "missing"):
            receiver.safe_file("m3/collections/src/main/java/com/m3/collections/not-an-owner.java")


if __name__ == "__main__":
    unittest.main()
