# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
"""Behavior, drift and refusal tests for exact SDK JMOD blob receiving."""
import tempfile
import unittest
from pathlib import Path

from verify_sdk_receiver import (
    HEADER, SOURCE_COMMIT, SUCCESSOR_COMMIT, SUCCESSORS, git_blob_sha1, rows_from_text, verify,
)


class SdkReceiverTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.names = (
            "m3/tooling/jmod-recipes/proof.txt",
            "m3/vendor/third_party-m3/proof.txt",
        )
        self.blobs = (b"recipe source\n", b"descriptor source\n")
        for name, blob in zip(self.names, self.blobs):
            path = self.root / name
            path.parent.mkdir(parents=True)
            path.write_bytes(blob)
        self.ledger = self.root / "manifest.tsv"
        self.ledger.write_text(
            HEADER + "\n" + "".join(
                "\t".join((name, git_blob_sha1(blob), "788", SOURCE_COMMIT)) + "\n"
                for name, blob in zip(self.names, self.blobs)
            ), encoding="utf-8")

    def test_git_blob_hash_uses_nul_framed_git_object(self):
        self.assertEqual(
            "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391",
            git_blob_sha1(b""),
        )

    def test_exact_custody_proves_two_distinct_file_families(self):
        first = verify(self.root, self.ledger, 2)
        second = verify(self.root, self.ledger, 2)
        self.assertEqual(first, second)
        self.assertEqual("SOURCE_BLOBS_VERIFIED", first["status"])
        self.assertEqual(2, first["files"])
        self.assertFalse(first["product_accepted"])
        self.assertFalse(first["jdk_runtime_verified"])

    def test_one_mutated_byte_refused(self):
        (self.root / self.names[0]).write_bytes(b"altered")
        with self.assertRaisesRegex(ValueError, "BLOB_DRIFT"):
            verify(self.root, self.ledger, 2)

    def test_missing_file_refused(self):
        (self.root / self.names[1]).unlink()
        with self.assertRaisesRegex(ValueError, "MISSING_OR_UNSAFE"):
            verify(self.root, self.ledger, 2)

    def test_bad_count_and_duplicate_are_refused(self):
        with self.assertRaisesRegex(ValueError, "DENOMINATOR"):
            verify(self.root, self.ledger, 52)
        text = self.ledger.read_text()
        with self.assertRaisesRegex(ValueError, "DUPLICATE"):
            rows_from_text(text + text.splitlines()[1] + "\n", 3)

    def test_path_escape_and_foreign_origin_are_refused(self):
        text = self.ledger.read_text()
        for bad in ("m3/tooling/jmod-recipes/../escape", "/m3/tooling/jmod-recipes/x"):
            with self.subTest(path=bad):
                tampered = text.replace(self.names[0], bad)
                with self.assertRaisesRegex(ValueError, "INVALID_IDENTITY"):
                    rows_from_text(tampered, 2)
        with self.assertRaisesRegex(ValueError, "INVALID_IDENTITY"):
            rows_from_text(text.replace(SOURCE_COMMIT, "0" * 40), 2)

    def test_exact_qualified_successors_and_drift_refusal(self):
        repo = Path(__file__).resolve().parents[4]
        for name, (predecessor, successor, canonical) in SUCCESSORS.items():
            with self.subTest(path=name):
                source = repo / name if (repo / name).is_file() else repo / canonical
                payload = source.read_bytes()
                self.assertEqual(successor, git_blob_sha1(payload))
                target = self.root / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(payload)
                text = HEADER + "\n" + "\t".join((name, predecessor, "788", SOURCE_COMMIT)) + "\n"
                self.ledger.write_text(text)
                result = verify(self.root, self.ledger, 1)
                self.assertEqual(SUCCESSOR_COMMIT, result["qualified_source_successors"][0]["canonical_commit"])
                self.assertFalse(result["product_accepted"])
                target.write_bytes(payload + b"\n")
                with self.assertRaisesRegex(ValueError, "BLOB_DRIFT"):
                    verify(self.root, self.ledger, 1)
                target.write_bytes(payload)
                self.ledger.write_text(text.replace(predecessor, successor))
                with self.assertRaisesRegex(ValueError, "SUCCESSOR_LINEAGE_DRIFT"):
                    verify(self.root, self.ledger, 1)


if __name__ == "__main__":
    unittest.main()
