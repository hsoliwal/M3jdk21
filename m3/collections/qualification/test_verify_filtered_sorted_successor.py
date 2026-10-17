# SPDX-License-Identifier: Apache-2.0
"""Hostile tests for additive original/successor source SHA-256 verification."""
import hashlib
import shutil
import tempfile
import unittest
from pathlib import Path
import verify_filtered_sorted as subject


class M3FilteredSortedSourceSuccessorTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix="m3-filter-seal-")
        self.root = Path(self.tmp.name)
        self.previous_root = subject.ROOT
        self.previous_manifest = subject.MANIFEST
        self.previous_successor = subject.SUCCESSOR_MANIFEST
        self.real_root = self.previous_root
        subject.ROOT = self.root
        sub = self.root / "m3/collections/qualification"
        sub.mkdir(parents=True)
        subject.MANIFEST = sub / "filtered-sorted-postimages-20261016.tsv"
        subject.SUCCESSOR_MANIFEST = sub / "filtered-view-late-union-postimages-20261017.tsv"
        shutil.copy2(self.previous_manifest, subject.MANIFEST)
        shutil.copy2(self.previous_successor, subject.SUCCESSOR_MANIFEST)
        for path in (*self.historical_paths(), *subject.SUCCESSOR_HASHES):
            target = self.root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(self.real_root / path, target)

    def tearDown(self):
        subject.ROOT = self.previous_root
        subject.MANIFEST = self.previous_manifest
        subject.SUCCESSOR_MANIFEST = self.previous_successor
        self.tmp.cleanup()

    def historical_paths(self):
        return [line.split("\t")[0] for line in
                self.previous_manifest.read_text(encoding="utf-8").splitlines()
                if line and not line.startswith("#")]

    def test_exact_approved_successor(self):
        subject.main()

    def test_unrelated_replacement_rejected(self):
        target = self.root / subject.ORIGINAL_OWNER
        target.write_bytes(target.read_bytes() + b"// drift")
        with self.assertRaises(SystemExit):
            subject.main()

    def test_successor_manifest_missing_rejected(self):
        subject.SUCCESSOR_MANIFEST.unlink()
        with self.assertRaises(SystemExit):
            subject.main()

    def test_successor_manifest_row_tampered_rejected(self):
        text = subject.SUCCESSOR_MANIFEST.read_text(encoding="utf-8")
        altered = text.replace(subject.SUCCESSOR_HASHES[subject.ORIGINAL_OWNER],
                               "0" * 64)
        self.assertNotEqual(text, altered)
        subject.SUCCESSOR_MANIFEST.write_text(altered, encoding="utf-8")
        with self.assertRaises(SystemExit):
            subject.main()

    def test_other_successor_test_source_drift_rejected(self):
        path = next(p for p in subject.SUCCESSOR_HASHES if p != subject.ORIGINAL_OWNER)
        source = self.root / path
        source.write_bytes(source.read_bytes() + b"// regression drift")
        with self.assertRaises(SystemExit):
            subject.main()

    def test_original_unmodified_sibling_still_pinned(self):
        sibling = next(p for p in self.historical_paths() if p != subject.ORIGINAL_OWNER)
        source = self.root / sibling
        source.write_bytes(source.read_bytes() + b"tampered")
        with self.assertRaises(SystemExit):
            subject.main()

    def test_historical_unmodified_source_accepted_without_successor(self):
        # Historical hash equality takes precedence. Synthetic bytes let the
        # regression independently test old-state handling without altering Git.
        target = self.root / subject.ORIGINAL_OWNER
        target.write_bytes(b"synthetic historical only")
        expected = hashlib.sha256(target.read_bytes()).hexdigest()
        lines = subject.MANIFEST.read_text(encoding="utf-8").splitlines()
        for i, line in enumerate(lines):
            if line.startswith(subject.ORIGINAL_OWNER + "\t"):
                cols = line.split("\t")
                cols[1] = expected
                lines[i] = "\t".join(cols)
        subject.MANIFEST.write_text("\n".join(lines) + "\n", encoding="utf-8")
        subject.SUCCESSOR_MANIFEST.unlink()
        subject.main()


if __name__ == "__main__":
    unittest.main()
