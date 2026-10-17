# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Hostile source-seal tests for exact SDK recovery."""
import shutil
import tempfile
import unittest
from pathlib import Path
import verify

class SdkRecoverySourceAdmissionTest(unittest.TestCase):
    def test_exact_installed_sources(self):
        self.assertEqual(verify.verify(), 5)

    def test_manifest_tamper_refused(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "RECEIPT.tsv"
            path.write_bytes(verify.RECEIPT.read_bytes() + b"# extra\n")
            with self.assertRaisesRegex(ValueError, "receipt"):
                verify.verify(receipt=path)

    def test_source_drift_and_alias_refused(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for entry in verify.TARGETS:
                dest = root / entry
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(verify.ROOT / entry, dest)
            self.assertEqual(verify.verify(root=root), 5)
            first = root / verify.TARGETS[0]
            first.write_bytes(first.read_bytes() + b"// drift\n")
            with self.assertRaisesRegex(ValueError, "drifted"):
                verify.verify(root=root)
            first.unlink()
            first.symlink_to(verify.ROOT / verify.TARGETS[0])
            with self.assertRaisesRegex(ValueError, "aliased"):
                verify.verify(root=root)

if __name__ == "__main__":
    unittest.main()
