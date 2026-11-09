#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prove catalogue queries preserve decisions and source disposition boundaries."""

import csv
import hashlib
import json
import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def query(*args):
    return subprocess.run([sys.executable, str(ROOT / "query_catalogue.py"), *args], text=True, capture_output=True)


class CatalogueContract(unittest.TestCase):
    def test_inventory_receipt_matches_exact_outputs(self):
        receipt = json.loads((ROOT / "INVENTORY_RECEIPT.json").read_text())
        self.assertFalse(receipt["full_repository_inventory"])
        self.assertTrue(receipt["reads_git_blobs_only"])
        for name, seal in receipt["output_sha256"].items():
            self.assertEqual(seal, hashlib.sha256((ROOT / name).read_bytes()).hexdigest())
        with (ROOT / "OWNER_INVENTORY.tsv").open() as source:
            owners = list(csv.DictReader(source, delimiter="\t"))
        self.assertEqual(receipt["owner_count"], len(owners))
        self.assertEqual(len(owners), len({row["source_path"] for row in owners}))
        self.assertTrue(all(row["revision"] == receipt["revision"] for row in owners))

    def test_trie_query_exposes_scaffold_without_promoting_it(self):
        result = query("--table", "owners", "--match", "M3PatriciaTrie.java", "--format", "json")
        self.assertEqual(0, result.returncode, result.stderr)
        rows = json.loads(result.stdout)
        self.assertEqual(2, len(rows))
        self.assertTrue(all("REFUSAL" in row["source_signal"] for row in rows))
        self.assertTrue(all("NOT_BEHAVIORAL_QUALIFICATION" in row["qualification"] for row in rows))

    def test_pending_family_and_non_string_domain_remain_explicit(self):
        result = query("--table", "families", "--where", "upstream_fqcn=com.google.common.collect.ClassToInstanceMap", "--format", "json")
        self.assertEqual(0, result.returncode, result.stderr)
        row, = json.loads(result.stdout)
        self.assertEqual("PENDING", row["disposition"])
        self.assertEqual("NON_STRING_KEY_DOMAIN", row["string_relevance"])
        self.assertEqual("-", row["existing_m3_owner"])

    def test_query_exact_and_text_filters_are_conjunctive(self):
        result = query("--where", "status=REJECTED", "--match", "reference-key", "--format", "json")
        self.assertEqual(0, result.returncode, result.stderr)
        rows = json.loads(result.stdout)
        self.assertEqual(["collections.identity"], [row["category"] for row in rows])
        absent = query("--where", "category=collections.identity", "--match", "not-a-present-category", "--format", "json")
        self.assertEqual([], json.loads(absent.stdout))

    def test_invalid_field_is_refused_and_queries_never_write(self):
        before = {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in ROOT.iterdir() if path.is_file()}
        invalid = query("--where", "promote=QUALIFIED")
        self.assertEqual(2, invalid.returncode)
        self.assertEqual("", invalid.stdout)
        self.assertIn("existing field", invalid.stderr)
        after = {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in ROOT.iterdir() if path.is_file()}
        self.assertEqual(before, after)


if __name__ == "__main__":
    unittest.main()
