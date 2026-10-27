# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Strict source-history receipts; historical indexed-view templates are immutable."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location(
    "m3_indexed_live_source_history", HERE / "verify_recipe.py"
)
history = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(history)


class IndexedViewLineageTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.proof = history.require_sealed_history()

    def test_each_exact_historical_successor_is_admitted(self):
        self.assertEqual(len(history.SUCCESSOR_CHAINS), 3)
        for path, chain in sorted(history.SUCCESSOR_CHAINS.items()):
            with self.subTest(path=path):
                history.admit_historical_successor(path, chain[0], chain[-1], self.proof)

    def test_changed_historical_preimage_refused(self):
        for path, chain in history.SUCCESSOR_CHAINS.items():
            with self.subTest(path=path):
                with self.assertRaisesRegex(RuntimeError, "not.*qualified|qualified historical"):
                    history.admit_historical_successor(path, "0" * 64, chain[-1], self.proof)

    def test_any_unreviewed_successor_refused(self):
        for path, chain in history.SUCCESSOR_CHAINS.items():
            with self.subTest(path=path):
                with self.assertRaisesRegex(RuntimeError, "qualified historical"):
                    history.admit_historical_successor(path, chain[0], "f" * 64, self.proof)

    def test_unknown_java_owner_refused(self):
        with self.assertRaisesRegex(RuntimeError, "qualified historical"):
            history.admit_historical_successor(
                "m3/collections/src/main/java/com/m3/collections/M3Unknown.java",
                "0" * 64, "1" * 64, self.proof
            )

    def test_missing_intermediate_history_refused(self):
        for path, chain in history.SUCCESSOR_CHAINS.items():
            with self.subTest(path=path):
                reduced = self.proof.replace(chain[0], "<missing-pin>")
                with self.assertRaisesRegex(RuntimeError, "incomplete source-history"):
                    history.admit_historical_successor(path, chain[0], chain[-1], reduced)

    def test_source_history_document_blob_is_immutable(self):
        with patch.object(history, "HISTORY_BLOB_SHA1", "0" * 40):
            with self.assertRaisesRegex(RuntimeError, "unreviewed source-history"):
                history.require_sealed_history()

    def test_original_manifest_still_has_six_exact_rows(self):
        rows = [row.split("\t") for row in (history.CRATE / "manifest.tsv")
                .read_text(encoding="utf-8").splitlines() if row.strip()
                and not row.startswith("#")]
        self.assertEqual(len(rows), 6)
        self.assertEqual([r[0] for r in rows], sorted(r[0] for r in rows))
        for path, chain in history.SUCCESSOR_CHAINS.items():
            row = next(r for r in rows if r[0] == path)
            self.assertEqual(row[2], chain[0], "historical postimage changed")

    def test_real_receiving_tree_and_all_six_manifest_rows(self):
        history.main()


if __name__ == "__main__":
    unittest.main()
