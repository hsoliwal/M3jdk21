#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Counterexample tests for receipt-only vs actual recorded source in M3JDK21."""

from __future__ import annotations

import csv
import hashlib
import importlib.util
import io
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("jep_source_witness.py")
    spec = importlib.util.spec_from_file_location("m3_jep_source_witness", path)
    module = importlib.util.module_from_spec(spec)
    import sys
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class WitnessTest(unittest.TestCase):
    def setUp(self):
        self.mod = load_module()
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.row = dict(release="27", jep="523", title="Default G1",
                        disposition="candidate")
        self.packet = self.root / "m3/backports/recipes/jep-523-g1-default"
        self.packet.mkdir(parents=True)

    def write_receipt(self, text):
        (self.packet / "CURRENT_TREE_RECEIPT.tsv").write_text(
            "field\tvalue\n" + text, encoding="utf-8")

    def witness(self):
        return self.mod.inspect(self.root, [self.row])[0]

    def test_exact_recorded_product_hash_is_only_a_witness_not_acceptance(self):
        product = self.root / "src/hotspot/share/gc/shared/gcConfig.cpp"
        product.parent.mkdir(parents=True)
        payload = b"/* upstream derived GPLv2 HotSpot code */\n"
        product.write_bytes(payload)
        self.write_receipt("source_materialized\ttrue\n"
                           "product_path\tsrc/hotspot/share/gc/shared/gcConfig.cpp\n"
                           f"product_sha256\t{hashlib.sha256(payload).hexdigest()}\n"
                           "promotion\tAUTHORIZED\n")
        before = product.read_bytes()
        got = self.witness()
        self.assertEqual("RECORDED_PRODUCT_HASH_MATCHED", got.witness_state)
        self.assertEqual((1, 1), (got.product_paths_checked, got.product_hashes_matched))
        self.assertEqual("NOT_PROVEN_BY_WITNESS", got.java21_compatibility)
        self.assertEqual("NOT_AUTHORIZED_BY_WITNESS", got.promotion)
        self.assertEqual(before, product.read_bytes())
        self.assertEqual(self.mod.render_tsv([got]), self.mod.render_tsv([got]))
        self.assertEqual(before, product.read_bytes())

    def test_stale_source_detected_not_mistaken_for_backport(self):
        product = self.root / "src/a.java"
        product.parent.mkdir(parents=True)
        product.write_text("modified", encoding="utf-8")
        self.write_receipt("product_path\tsrc/a.java\n"
                           f"product_sha256\t{hashlib.sha256(b'old').hexdigest()}\n")
        self.assertEqual("SOURCE_DRIFT", self.witness().witness_state)

    def test_truthy_receipt_without_path_hash_is_not_code_proof(self):
        self.write_receipt("source_materialized\ttrue\n"
                           "current_tree_state\tREVIEWED_POSTIMAGES_ALREADY_PRESENT\n"
                           "total_targets\t26\n")
        got = self.witness()
        self.assertEqual("RECEIPT_WITHOUT_PRODUCT_HASH", got.witness_state)
        self.assertEqual(0, got.product_hashes_matched)

    def test_test_hash_only_is_not_product_witness(self):
        self.write_receipt("test_path\ttest/hotspot/Foo.java\n"
                           "test_sha256\t" + "a" * 64 + "\n")
        self.assertEqual("RECEIPT_WITHOUT_PRODUCT_HASH", self.witness().witness_state)

    def test_missing_packet_and_packet_without_receipt_are_separate(self):
        self.assertEqual("PACKET_WITHOUT_RECEIPT", self.witness().witness_state)
        self.packet.rmdir()
        self.assertEqual("NO_PACKET", self.witness().witness_state)

    def test_rejects_path_escape_even_if_matching_hash_is_supplied(self):
        self.write_receipt("product_path\t../../outside.java\n"
                           "product_sha256\t" + "a" * 64 + "\n")
        self.assertEqual("INVALID_RECEIPT", self.witness().witness_state)

    def test_rejects_symlinked_product_path(self):
        out = self.root / "outside.java"
        out.write_text("out", encoding="utf-8")
        sub = self.root / "src"
        sub.mkdir()
        (sub / "link.java").symlink_to(out)
        self.write_receipt("product_path\tsrc/link.java\n"
                           f"product_sha256\t{hashlib.sha256(b'out').hexdigest()}\n")
        self.assertEqual("INVALID_RECEIPT", self.witness().witness_state)

    def test_duplicate_field_is_not_silently_last_wins(self):
        self.write_receipt("product_path\tsrc/A.java\n"
                           "product_path\tsrc/B.java\n"
                           "product_sha256\t" + "a" * 64 + "\n")
        self.assertEqual("INVALID_RECEIPT", self.witness().witness_state)

    def test_missing_counterpart_and_bad_hash_are_invalid(self):
        self.write_receipt("product_path\tsrc/A.java\n")
        self.assertEqual("INVALID_RECEIPT", self.witness().witness_state)
        self.write_receipt("product_path\tsrc/A.java\nproduct_sha256\tshort\n")
        self.assertEqual("INVALID_RECEIPT", self.witness().witness_state)

    def test_exact_names_no_cross_jep_alias(self):
        other = self.root / "m3/backports/recipes/jep-5234-fake"
        other.mkdir(parents=True)
        self.assertEqual(1, self.witness().packet_count)

    def test_output_confined_and_does_not_follow_alias(self):
        safe = self.root / "m3/backports/target/receipts/audit.tsv"
        self.assertEqual(safe, self.mod.checked_output_path(self.root, safe))
        with self.assertRaisesRegex(ValueError, "must be under"):
            self.mod.checked_output_path(self.root, self.root / "src/Product.java")
        with self.assertRaisesRegex(ValueError, "traversal"):
            self.mod.checked_output_path(self.root, safe.parent / "../audit.tsv")
        target = self.root / "m3/backports/target"
        target.mkdir(parents=True)
        external = self.root / "external"
        external.mkdir()
        (target / "aliased").symlink_to(external, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "symlink alias"):
            self.mod.checked_output_path(self.root, target / "aliased/audit.tsv")

    def test_hard_link_receipt_is_not_overwritten(self):
        target = self.root / "m3/backports/target"
        target.mkdir(parents=True)
        original = self.root / "source.java"
        original.write_text("preserve", encoding="utf-8")
        linked = target / "audit.tsv"
        linked.hardlink_to(original)
        with self.assertRaisesRegex(ValueError, "hard-linked"):
            self.mod.checked_output_path(self.root, linked)
        self.assertEqual("preserve", original.read_text(encoding="utf-8"))

    def test_real_tsv_physical_rows(self):
        rows = [self.witness(), self.mod.Witness(22, 423, "Region Pinning", "candidate", 0, 0, 0, 0, "NO_PACKET")]
        encoded = self.mod.render_tsv(rows)
        self.assertEqual(3, len(encoded.splitlines()))
        parsed = list(csv.DictReader(io.StringIO(encoded), delimiter="\t"))
        self.assertEqual(["523", "423"], [x["jep"] for x in parsed])
        self.assertTrue(encoded.endswith("\n"))


if __name__ == "__main__":
    unittest.main()
