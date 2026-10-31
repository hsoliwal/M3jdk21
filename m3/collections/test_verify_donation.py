#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: select the current exact additive successor inventory named by source_set_successor_current.
# Modified 2026 by Hitesh Soliwal and contributors: accept the sealed transition-receipt successor shape next to the additive-manifest shape.
"""Prove current collections manifest custody and reject ambiguous donation JSON."""
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
            self.assert_transition_receipt_successor(record)
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
        packet = (HERE / "m3-donation.json").read_text()
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

    def assert_transition_receipt_successor(self, record):
        receipt = HERE / record["successor_receipt"]
        self.assertEqual(record["successor_receipt_sha256"], VERIFIER.digest(receipt))
        self.assertEqual(record["successor_receipt_sha256"], VERIFIER._SUPERSET_RECEIPT_SHA256)
        self.assertEqual(record["inherited_composed_manifest_sha256"],
                         VERIFIER._SUPERSET_PREDECESSOR_SHA256)
        self.assertEqual(record["resulting_rows"], VERIFIER._SUPERSET_RESULT_ROWS)
        self.assertEqual(record["exact_java_source_membership"], VERIFIER._SUPERSET_RESULT_JAVA)
        rows = [line.split(chr(9)) for line in receipt.read_text(encoding="utf-8").splitlines()]
        self.assertEqual(record["successor_rows"], len(rows))
        self.assertEqual(record["successor_rows"], VERIFIER._SUPERSET_TRANSITIONS)
        for path, inherited, after in rows:
            self.assertTrue(path.startswith("m3/collections/"))
            self.assertEqual(64, len(after))
            self.assertTrue(inherited == "ABSENT" or len(inherited) == 64)


if __name__ == "__main__":
    unittest.main()
