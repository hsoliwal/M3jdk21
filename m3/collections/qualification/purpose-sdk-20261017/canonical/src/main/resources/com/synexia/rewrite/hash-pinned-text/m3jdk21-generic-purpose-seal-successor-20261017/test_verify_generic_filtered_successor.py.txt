# SPDX-License-Identifier: Apache-2.0
"""Hostile checks for preserving original M3 wrapper seals plus one exact successor."""
import shutil
import tempfile
import unittest
from pathlib import Path
import verify_generic_filtered_wrappers as subject


class GenericPurposeSuccessorTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="m3-purpose-seal-")
        self.root = Path(self.temp.name)
        self.old = (subject.ROOT, subject.MANIFEST, subject.PURPOSE_RECEIPT)
        original = subject.ROOT
        subject.ROOT = self.root
        subject.MANIFEST = self.root / "m3/collections/qualification/generic-filtered-wrappers-postimages-20261017.tsv"
        subject.PURPOSE_RECEIPT = self.root / "m3/collections/qualification/generic-filtered-purpose-postimages-20261017.tsv"
        subject.MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(self.old[1], subject.MANIFEST)
        shutil.copy2(self.old[2], subject.PURPOSE_RECEIPT)
        covered = {line.split("\t")[0] for line in subject.MANIFEST.read_text(encoding="utf-8").splitlines()
                   if line and not line.startswith("#")}
        covered.update(subject.EXPECTED_SUCCESSOR)
        for rel in covered:
            dst = self.root / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(original / rel, dst)

    def tearDown(self):
        subject.ROOT, subject.MANIFEST, subject.PURPOSE_RECEIPT = self.old
        self.temp.cleanup()

    def test_exact_registered_successor(self):
        subject.main()

    def test_missing_successor_receipt_refused(self):
        subject.PURPOSE_RECEIPT.unlink()
        with self.assertRaises(SystemExit): subject.main()

    def test_poisoned_successor_receipt_refused(self):
        original = subject.PURPOSE_RECEIPT.read_text(encoding="utf-8")
        key = subject.EXPECTED_SUCCESSOR[subject.ORIGINAL_OWNER]
        self.assertIn(key, original)
        subject.PURPOSE_RECEIPT.write_text(original.replace(key, "f"*64), encoding="utf-8")
        with self.assertRaises(SystemExit): subject.main()

    def test_original_manifest_drift_refused(self):
        subject.MANIFEST.write_bytes(subject.MANIFEST.read_bytes() + b"# foreign\n")
        with self.assertRaises(SystemExit): subject.main()

    def test_modified_purpose_map_refused(self):
        target = self.root / subject.ORIGINAL_OWNER
        target.write_bytes(target.read_bytes() + b"// unexpected edit\n")
        with self.assertRaises(SystemExit): subject.main()

    def test_purpose_test_drift_refused(self):
        test_path = next(p for p in subject.EXPECTED_SUCCESSOR if p != subject.ORIGINAL_OWNER)
        target = self.root / test_path
        target.write_bytes(target.read_bytes() + b"// unexpected edit\n")
        with self.assertRaises(SystemExit): subject.main()

    def test_prior_owner_drift_refused(self):
        original = "m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java"
        target = self.root / original
        target.write_bytes(target.read_bytes() + b"// unexpected edit\n")
        with self.assertRaises(SystemExit): subject.main()


if __name__ == "__main__":
    unittest.main()
