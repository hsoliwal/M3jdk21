# SPDX-License-Identifier: Apache-2.0
"""Discoverable source-identity guard in the existing M3 collections CI suite."""
import contextlib
import importlib.util
import io
from pathlib import Path
import unittest


class StreamCurrentContractReceiptTest(unittest.TestCase):
    def test_exact_java_receipts_and_public_erasures(self):
        path = (Path(__file__).resolve().parent / "qualification"
                / "stream-current-compatible-20261118" / "verify.py")
        spec = importlib.util.spec_from_file_location("m3_stream_compat_verify", path)
        self.assertIsNotNone(spec)
        self.assertIsNotNone(spec.loader)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        capture = io.StringIO()
        with contextlib.redirect_stdout(capture):
            module.verify()
        self.assertIn("M3_STREAM_CURRENT_SOURCE_RECEIPT_OK", capture.getvalue())


if __name__ == "__main__":
    unittest.main()
