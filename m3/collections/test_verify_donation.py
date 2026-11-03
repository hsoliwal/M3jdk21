#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: select the current exact additive successor inventory named by source_set_successor_current.
# Modified 2026 by Hitesh Soliwal and contributors: accept the sealed transition-receipt successor shape next to the additive-manifest shape.
# Modified 2026 by Hitesh Soliwal and contributors: check the seals of whichever transition-receipt successor is current; unit-test the shared transition atom.
"""Prove current collections manifest custody and reject ambiguous donation JSON."""
import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("verify_donation", HERE / "verify_donation.py")
VERIFIER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFIER)


class DonationPacketTest(unittest.TestCase):
    def test_current_packet_selects_a_sha256_sealed_manifest(self):
        packet = VERIFIER.read_donation_packet()
        self.assertEqual(
            "qualification/installed-source-enum-set-20261014.tsv",
            packet["installed_source_manifest"])
        self.assertEqual(
            "qualification/installed-source-enum-set-20261008.tsv",
            packet["historical_installed_source_manifest_20261008"])
        self.assertNotEqual(
            packet["installed_source_manifest"],
            packet["historical_installed_source_manifest_20261008"])
        manifest = HERE / packet["installed_source_manifest"]
        self.assertEqual(packet["installed_source_manifest_sha256"],
                         VERIFIER.digest(manifest))

    def test_current_successor_is_exact_additive(self):
        packet = VERIFIER.read_donation_packet()
        record = packet[packet["source_set_successor_current"]]
        if "successor_receipt" in record:
            self.assert_transition_receipt_successor(packet["source_set_successor_current"], record)
            return
        self.assertEqual(record["installed_manifest"], packet["installed_source_manifest"])
        old_path = HERE / record["predecessor_manifest"]
        self.assertEqual(record["predecessor_manifest_sha256"], VERIFIER.digest(old_path))
        old = old_path.read_text()
        new = (HERE / packet["installed_source_manifest"]).read_text()
        self.assertTrue(new.startswith(old))
        rows = [line.split("\t") for line in new[len(old):].splitlines()
                if line and not line.startswith("#")]
        self.assertEqual(record["added_sources"], [row[0] for row in rows])
        self.assertEqual(record["installed_rows"], record["predecessor_rows"] + len(rows))
        for path, expected in rows:
            self.assertTrue(path.startswith("m3/collections/src/") and path.endswith(".java"))
            self.assertEqual(64, len(expected))
            self.assertNotIn(path + "\t", old)

    def test_duplicate_selector_is_refused_even_if_values_match(self):
        packet = (HERE / "synexia-donation.json").read_text()
        duplicate = packet.replace(
            '"historical_installed_source_manifest_20261008"',
            '"installed_source_manifest"', 1)
        self.assertNotEqual(duplicate, packet)
        with self.assertRaisesRegex(ValueError,
                                    "Duplicate donation metadata key: installed_source_manifest"):
            json.loads(duplicate, object_pairs_hook=VERIFIER.unique_json_object)

    def test_nested_duplicate_is_refused(self):
        with self.assertRaisesRegex(ValueError,
                                    "Duplicate donation metadata key: source"):
            json.loads('{"history": {"source": 1, "source": 2}}',
                       object_pairs_hook=VERIFIER.unique_json_object)

    def test_loader_refuses_drift_without_changing_file(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "donation.json"
            broken = '{"installed_source_manifest": "new.tsv", "installed_source_manifest": "old.tsv"}'
            source.write_text(broken)
            with self.assertRaisesRegex(ValueError,
                                        "Duplicate donation metadata key"):
                VERIFIER.read_donation_packet(source)
            self.assertEqual(broken, source.read_text())

    def assert_transition_receipt_successor(self, name, record):
        _, receipt_sha256, predecessor_sha256, transitions, result_rows, result_java = (
            VERIFIER.SOURCE_TRANSITION_SEALS[name])
        receipt = HERE / record["successor_receipt"]
        self.assertEqual(record["successor_receipt_sha256"], VERIFIER.digest(receipt))
        self.assertEqual(record["successor_receipt_sha256"], receipt_sha256)
        self.assertEqual(record["inherited_composed_manifest_sha256"],
                         predecessor_sha256)
        self.assertEqual(record["resulting_rows"], result_rows)
        self.assertEqual(record["exact_java_source_membership"], result_java)
        rows = [line.split(chr(9)) for line in receipt.read_text(encoding="utf-8").splitlines()]
        self.assertEqual(record["successor_rows"], len(rows))
        self.assertEqual(record["successor_rows"], transitions)
        for path, inherited, after in rows:
            self.assertTrue(path.startswith("m3/collections/"))
            self.assertEqual(64, len(after))
            self.assertTrue(inherited == "ABSENT" or len(inherited) == 64)

    def test_source_transitions_admit_the_exact_receipt_and_refuse_drift(self):
        tab, newline = chr(9), chr(10)
        path = "m3/collections/src/main/java/com/m3/collections/A.java"
        added = "m3/collections/src/test/java/com/m3/collections/BTest.java"
        old, new, fresh, other = "a" * 64, "b" * 64, "c" * 64, "d" * 64

        def seal(rows):
            return hashlib.sha256("".join(p + tab + e + newline for p, e in rows).encode("utf-8")).hexdigest()

        text = path + tab + old + tab + new + newline + added + tab + "ABSENT" + tab + fresh + newline
        with tempfile.TemporaryDirectory() as directory:
            receipt = Path(directory) / "receipt.tsv"
            receipt.write_bytes(text.encode("utf-8"))
            sealed = VERIFIER.digest(receipt)
            prior = [(path, old)]
            self.assertEqual([(path, new), (added, fresh)], VERIFIER.admit_source_transitions(
                prior, "Unit", receipt, sealed, seal(prior), 2, 2, 2))
            drifted = [(path, other)]
            with self.assertRaisesRegex(SystemExit, "Unit inherited source preimage drift"):
                VERIFIER.admit_source_transitions(drifted, "Unit", receipt, sealed, seal(drifted), 2, 2, 2)
            with self.assertRaisesRegex(SystemExit, "Unit predecessor receipt chain drift"):
                VERIFIER.admit_source_transitions(prior, "Unit", receipt, sealed, seal(drifted), 2, 2, 2)
            receipt.write_bytes(text.replace(new, other).encode("utf-8"))
            with self.assertRaisesRegex(SystemExit, "Unit successor receipt missing, aliased or drifted"):
                VERIFIER.admit_source_transitions(prior, "Unit", receipt, sealed, seal(prior), 2, 2, 2)


if __name__ == "__main__":
    unittest.main()
