#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prove current collections manifest custody and reject ambiguous donation JSON."""
import ast
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
            "qualification/installed-source-wrappers-20261017.tsv",
            packet["installed_source_manifest"])
        self.assertEqual(
            "qualification/installed-source-enum-set-20261008.tsv",
            packet["historical_installed_source_manifest_20261008"])
        self.assertNotEqual(
            packet["installed_source_manifest"],
            packet["historical_installed_source_manifest_20261008"])
        predecessor = packet["historical_wrapper_reconciliation_predecessor_20261017"]
        self.assertEqual("qualification/installed-source-enum-set-20261014.tsv",
                         predecessor["installed_source_manifest"])
        self.assertEqual("046e3aa7f4d9523a72657942fac62bcb7d7bb6eaafb928dbe1c1d87afed1252c",
                         predecessor["installed_source_manifest_sha256"])
        self.assertEqual(predecessor["installed_source_manifest_sha256"],
                         VERIFIER.digest(HERE / predecessor["installed_source_manifest"]))
        self.assertEqual("970a34350ebdf6a3f1c5ff5acc8445d0f3515f55",
                         packet["current_target_baseline"])
        manifest = HERE / packet["installed_source_manifest"]
        self.assertEqual(packet["installed_source_manifest_sha256"],
                         VERIFIER.digest(manifest))

    def test_receipt_uses_current_packet_baseline_with_historical_fallback(self):
        module = ast.parse((HERE / "verify_donation.py").read_text())
        receipt = next(node.value for node in ast.walk(module)
                       if isinstance(node, ast.Assign)
                       and any(isinstance(target, ast.Name) and target.id == "receipt"
                               for target in node.targets))
        value = next(value for key, value in zip(receipt.keys, receipt.values)
                     if isinstance(key, ast.Constant) and key.value == "target_baseline")
        expression = compile(ast.Expression(value), "verify_donation_receipt", "eval")
        self.assertEqual("current", eval(expression, {"packet": {
            "current_target_baseline": "current", "target_baseline": "historical"}}))
        self.assertEqual("historical", eval(expression, {"packet": {
            "target_baseline": "historical"}}))

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


if __name__ == "__main__":
    unittest.main()
