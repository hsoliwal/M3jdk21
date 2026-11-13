# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
"""Behavior, drift and refusal tests for exact SDK JMOD blob receiving."""
import tempfile
import hashlib
from unittest import mock
import unittest
from pathlib import Path

from verify_sdk_receiver import (
    HEADER, SOURCE_COMMIT, SUCCESSOR_COMMIT, SUCCESSOR_COMMITS, SUCCESSORS,
    CURRENT_SOURCE_SUCCESSORS, ORIGINAL_LEDGER_RELATIVE, ORIGINAL_LEDGER_GIT_BLOB,
    ORIGINAL_LEDGER_SHA256, git_blob_sha1, rows_from_text, verify,
)


class SdkReceiverTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.names = (
            "make/m3/tooling/jmod-recipes/proof.txt",
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
            rows_from_text(self.ledger.read_text(), 52)
        text = self.ledger.read_text()
        with self.assertRaisesRegex(ValueError, "DUPLICATE"):
            rows_from_text(text + text.splitlines()[1] + "\n", 3)

    def test_path_escape_and_foreign_origin_are_refused(self):
        text = self.ledger.read_text()
        for bad in ("make/m3/tooling/jmod-recipes/../escape", "/m3/tooling/jmod-recipes/x"):
            with self.subTest(path=bad):
                tampered = text.replace(self.names[0], bad)
                with self.assertRaisesRegex(ValueError, "INVALID_IDENTITY"):
                    rows_from_text(tampered, 2)
        with self.assertRaisesRegex(ValueError, "INVALID_IDENTITY"):
            rows_from_text(text.replace(SOURCE_COMMIT, "0" * 40), 2)

    def test_exact_qualified_successors_and_drift_refusal(self):
        repo = Path(__file__).resolve().parents[5]
        for name, (predecessor, successor, canonical) in SUCCESSORS.items():
            with self.subTest(path=name):
                source = repo / name if (repo / name).is_file() else repo / canonical
                payload = source.read_bytes()
                current = CURRENT_SOURCE_SUCCESSORS.get(name)
                self.assertEqual(current[1] if current else successor,
                                 git_blob_sha1(payload))
                target = self.root / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(payload)
                text = HEADER + "\n" + "\t".join((name, predecessor, "788", SOURCE_COMMIT)) + "\n"
                self.ledger.write_text(text)
                result = verify(self.root, self.ledger, 1)
                self.assertEqual(SUCCESSOR_COMMITS.get(name, SUCCESSOR_COMMIT),
                                 result["qualified_source_successors"][0]["canonical_commit"])
                self.assertFalse(result["product_accepted"])
                if current:
                    self.assertEqual(1, len(result["current_source_successors"]))
                    self.assertEqual(current[1], result["current_source_successors"][0]["git_blob"])
                    self.assertEqual(successor,
                                     result["current_source_successors"][0]["previous_git_blob"])
                target.write_bytes(payload + b"\n")
                with self.assertRaisesRegex(ValueError, "BLOB_DRIFT"):
                    verify(self.root, self.ledger, 1)
                target.write_bytes(payload)
                self.ledger.write_text(text.replace(predecessor, successor))
                with self.assertRaisesRegex(ValueError, "SUCCESSOR_LINEAGE_DRIFT"):
                    verify(self.root, self.ledger, 1)


    def test_three_current_source_owners_are_not_relicensed_or_promoted(self):
        self.assertEqual(3, len(CURRENT_SOURCE_SUCCESSORS))
        self.assertEqual(
            SUCCESSORS["make/m3/tooling/jmod-recipes/tools/jmod_pack.py"][1],
            CURRENT_SOURCE_SUCCESSORS["make/m3/tooling/jmod-recipes/tools/jmod_pack.py"][0],
        )
        for name, (prior, current, commit, gate) in CURRENT_SOURCE_SUCCESSORS.items():
            with self.subTest(path=name):
                self.assertTrue(name.startswith("make/m3/tooling/jmod-recipes/"))
                for field in (prior, current, commit):
                    self.assertRegex(field, r"^[a-f0-9]{40}$")
                self.assertNotEqual(prior, current)
                self.assertTrue(gate.startswith("PENDING_"))

    def test_current_successor_reuses_exact_ledger_without_weakening_byte_guard(self):
        name = self.names[0]
        old = b"predecessor\n"
        new = b"current source\n"
        previous = git_blob_sha1(old)
        current = git_blob_sha1(new)
        self.ledger.write_text(HEADER + "\n" +
            "\t".join((name, previous, "788", SOURCE_COMMIT)) + "\n")
        (self.root / name).write_bytes(new)
        with mock.patch.dict(CURRENT_SOURCE_SUCCESSORS, {
                name: (previous, current, "a" * 40, "PENDING_TEST")}, clear=True):
            received = verify(self.root, self.ledger, 1)
            self.assertEqual([], received["qualified_source_successors"])
            proof = received["current_source_successors"]
            self.assertEqual(1, len(proof))
            self.assertEqual("hsoliwal/M3jdk21", proof[0]["source_repository"])
            self.assertEqual(previous, proof[0]["previous_git_blob"])
            self.assertEqual(current, proof[0]["git_blob"])
            self.assertFalse(received["product_accepted"])
            self.assertFalse(received["jdk_runtime_verified"])
            self.assertEqual(received, verify(self.root, self.ledger, 1))
            (self.root / name).write_bytes(new + b"tamper")
            with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_BLOB_DRIFT"):
                verify(self.root, self.ledger, 1)
            (self.root / name).write_bytes(old)
            with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_BLOB_DRIFT"):
                verify(self.root, self.ledger, 1)
            (self.root / name).write_bytes(new)
            self.ledger.write_text(HEADER + "\n" +
                "\t".join((name, current, "788", SOURCE_COMMIT)) + "\n")
            with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_CURRENT_LINEAGE_DRIFT"):
                verify(self.root, self.ledger, 1)

    def test_two_stage_successor_preserves_original_m3_history(self):
        name = self.names[0]
        old = git_blob_sha1(b"old source")
        middle = git_blob_sha1(b"prior qualified source")
        current_payload = b"current validated source"
        current = git_blob_sha1(current_payload)
        target = self.root / name
        target.write_bytes(current_payload)
        self.ledger.write_text(HEADER + "\n" +
            "\t".join((name, old, "788", SOURCE_COMMIT)) + "\n")
        with mock.patch.dict(SUCCESSORS, {name: (old, middle, "canonical-source")},
                             clear=True), mock.patch.dict(
                CURRENT_SOURCE_SUCCESSORS,
                {name: (middle, current, "b" * 40, "PENDING_TEST")}, clear=True):
            result = verify(self.root, self.ledger, 1)
            old_receipt = result["qualified_source_successors"][0]
            new_receipt = result["current_source_successors"][0]
            self.assertEqual(old, old_receipt["predecessor_git_blob"])
            self.assertEqual(middle, old_receipt["git_blob"])
            self.assertEqual(middle, new_receipt["previous_git_blob"])
            self.assertEqual(current, new_receipt["git_blob"])
            self.assertFalse(result["product_accepted"])
            self.assertFalse(result["jdk_runtime_verified"])
            target.write_bytes(current_payload + b"!")
            with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_BLOB_DRIFT"):
                verify(self.root, self.ledger, 1)

    def test_missing_or_symlinked_current_source_refuses_without_update(self):
        name = self.names[0]
        before = b"old\n"
        now = b"new\n"
        self.ledger.write_text(HEADER + "\n" +
            "\t".join((name, git_blob_sha1(before), "788", SOURCE_COMMIT)) + "\n")
        target = self.root / name
        target.unlink()
        with mock.patch.dict(CURRENT_SOURCE_SUCCESSORS, {name: (
                git_blob_sha1(before), git_blob_sha1(now), "c" * 40,
                "PENDING_TEST")}, clear=True):
            with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_MISSING_OR_UNSAFE_SOURCE"):
                verify(self.root, self.ledger, 1)
            outside = self.root / "outside.txt"
            outside.write_bytes(now)
            target.symlink_to(outside)
            with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_MISSING_OR_UNSAFE_SOURCE"):
                verify(self.root, self.ledger, 1)



    def test_full_52_row_census_remains_immutable_with_three_current_successors(self):
        rows = []
        synthetic_current = {}
        synthetic_prior = {}
        names = []
        for index in range(52):
            name = "m3/vendor/third_party-m3/fixture-%02d.txt" % index
            old_payload = ("history %02d\n" % index).encode("utf-8")
            latest_payload = ("current %02d\n" % index).encode("utf-8")
            initial = git_blob_sha1(old_payload)
            payload = old_payload
            if index in (0, 1):
                payload = latest_payload
                synthetic_current[name] = (
                    initial, git_blob_sha1(payload), "a" * 40, "PENDING_TEST")
            elif index == 2:
                intermediate = git_blob_sha1(b"prior source")
                synthetic_prior[name] = (initial, intermediate, "prior-source")
                payload = latest_payload
                synthetic_current[name] = (
                    intermediate, git_blob_sha1(payload), "b" * 40, "PENDING_TEST")
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(payload)
            names.append(name)
            rows.append("\t".join((name, initial, "788", SOURCE_COMMIT)))
        # Exercise all 52 source rows without granting a production test ledger.
        self.ledger = self.root / ORIGINAL_LEDGER_RELATIVE
        self.ledger.parent.mkdir(parents=True, exist_ok=True)
        self.ledger.write_text(HEADER + "\n" + "\n".join(rows) + "\n")
        synthetic_ledger = self.ledger.read_bytes()
        with mock.patch.dict(SUCCESSORS, synthetic_prior, clear=True), \
                mock.patch.dict(CURRENT_SOURCE_SUCCESSORS, synthetic_current, clear=True), \
                mock.patch("verify_sdk_receiver.ORIGINAL_LEDGER_GIT_BLOB",
                           git_blob_sha1(synthetic_ledger)), \
                mock.patch("verify_sdk_receiver.ORIGINAL_LEDGER_SHA256",
                           hashlib.sha256(synthetic_ledger).hexdigest()):
            first = verify(self.root, self.ledger, 52)
            self.assertEqual(first, verify(self.root, self.ledger, 52))
            self.assertEqual(52, first["files"])
            self.assertEqual(1, len(first["qualified_source_successors"]))
            self.assertEqual(3, len(first["current_source_successors"]))
            self.assertFalse(first["product_accepted"])
            self.assertFalse(first["jdk_runtime_verified"])
            target = self.root / names[50]
            target.write_bytes(target.read_bytes() + b"tampered")
            with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_BLOB_DRIFT"):
                verify(self.root, self.ledger, 52)


    def test_raw_dot_and_double_slash_aliases_refused_before_git_resolution(self):
        original = self.names[1]
        text = self.ledger.read_text(encoding="utf-8")
        for alias in (
                original.replace("/proof.txt", "/./proof.txt"),
                original.replace("/proof.txt", "//proof.txt"),
                original.replace("/proof.txt", "/../third_party-m3/proof.txt"),
                original.replace("/proof.txt", "/\x00proof.txt")):
            with self.subTest(alias=repr(alias)):
                with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_INVALID_IDENTITY"):
                    rows_from_text(text.replace(original, alias), 2)

    def test_distinct_ledger_rows_must_not_alias_by_casefold(self):
        one = "m3/vendor/third_party-m3/Case.txt"
        two = "m3/vendor/third_party-m3/case.txt"
        blob = git_blob_sha1(b"same literal source")
        manifest = HEADER + "\n" + "".join(
            "\t".join((name, blob, "788", SOURCE_COMMIT)) + "\n"
            for name in (one, two))
        with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_DUPLICATE_PATH"):
            rows_from_text(manifest, 2)

    def test_internal_parent_symlink_does_not_qualify_as_original_source(self):
        name = self.names[1]
        original_parent = (self.root / name).parent
        moved = original_parent.with_name(original_parent.name + "-actual")
        original_parent.rename(moved)
        original_parent.symlink_to(moved, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_MISSING_OR_UNSAFE_SOURCE"):
            verify(self.root, self.ledger, 2)

    def test_original_regular_paths_unchanged_by_stricter_alias_check(self):
        self.assertEqual("SOURCE_BLOBS_VERIFIED", verify(self.root, self.ledger, 2)["status"])


    def test_any_fabricated_52_row_ledger_refused_before_source_lookup(self):
        names = []
        records = []
        for index in range(52):
            name = "m3/vendor/third_party-m3/counterfeit_%02d.txt" % index
            content = ("fabricated %02d\n" % index).encode()
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(content)
            names.append(name)
            records.append("\t".join((name, git_blob_sha1(content), "788", SOURCE_COMMIT)))
        self.ledger = self.root / ORIGINAL_LEDGER_RELATIVE
        self.ledger.parent.mkdir(parents=True, exist_ok=True)
        self.ledger.write_text(HEADER + "\n" + "\n".join(records) + "\n")
        with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_LEDGER_IDENTITY_DRIFT"):
            verify(self.root, self.ledger, 52)

    def test_real_denominator_must_use_one_canonical_ledger_path(self):
        # Even if an alternate path's bytes hypothetically matched the original,
        # its origin is not the canonical PR #788 receipt.
        with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_LEDGER_PATH_DRIFT"):
            verify(self.root, self.ledger, 52)

    def test_canonical_ledger_symlink_refused_even_when_target_is_internal(self):
        path = self.root / ORIGINAL_LEDGER_RELATIVE
        path.parent.mkdir(parents=True, exist_ok=True)
        ledger = self.root / "ledger-content.tsv"
        ledger.write_bytes(self.ledger.read_bytes())
        path.symlink_to(ledger)
        with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_LEDGER_UNSAFE_PATH"):
            verify(self.root, path, 52)

    def test_canonical_ledger_parent_symlink_refused(self):
        path = self.root / ORIGINAL_LEDGER_RELATIVE
        parent = path.parent
        other = self.root / "actual-tools"
        parent.rename(other)
        parent.symlink_to(other, target_is_directory=True)
        (other / path.name).write_bytes(self.ledger.read_bytes())
        with self.assertRaisesRegex(ValueError, "SDK_RECEIVER_LEDGER_UNSAFE_PATH"):
            verify(self.root, path, 52)

    def test_synthetic_lower_denominator_marks_nonproduction_authority(self):
        result = verify(self.root, self.ledger, 2)
        self.assertEqual("TEST_ONLY_NONPRODUCTION_DENOMINATOR", result["ledger_authority"])
        self.assertNotEqual(ORIGINAL_LEDGER_GIT_BLOB, result["source_ledger_git_blob"])
        self.assertFalse(result["product_accepted"])
        self.assertFalse(result["jdk_runtime_verified"])

    def test_original_ledger_pin_has_both_independent_digests(self):
        self.assertEqual("e1f463b9fa54f9c92d0bbeacc79e7902be00bd2c",
                         ORIGINAL_LEDGER_GIT_BLOB)
        self.assertEqual("db9c42ea9b0ec9af0986009a8022064eb419ed7e57104fa229b97dd625970046",
                         ORIGINAL_LEDGER_SHA256)


if __name__ == "__main__":
    unittest.main()
