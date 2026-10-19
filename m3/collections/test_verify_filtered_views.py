#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed additive source-inventory proof for the Synexia collection receiver."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('m3_collections_verify_donation', ROOT / 'verify_donation.py')
if spec is None or spec.loader is None:
    raise RuntimeError('cannot import collection verifier')
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class FilteredViewsReceivingTest(unittest.TestCase):
    def test_exact_additive_successor_keeps_all_historical_rows(self):
        historical = [('m3/collections/legacy/A.java', 'a' * 64)]
        result = verifier.include_filtered_view_successor(historical)
        self.assertEqual(historical, result[:len(historical)])
        self.assertEqual(7, len(result))
        self.assertEqual(list(verifier._FILTERED_VIEW_PATHS), [row[0] for row in result[1:]])
        self.assertEqual(len(set(path for path, _ in result)), 7)
        for path, sha in result[1:]:
            self.assertEqual(sha, verifier.digest(verifier.REPO / path))
            self.assertEqual(64, len(sha))

    def test_missing_overlay_does_not_admit_arbitrary_java(self):
        with tempfile.TemporaryDirectory() as directory:
            result = verifier.include_filtered_view_successor(
                [('legacy', 'a' * 64)], Path(directory) / 'missing.tsv')
        self.assertEqual([('legacy', 'a' * 64)], result)

    def test_changed_reordered_or_duplicate_receipt_fails_closed(self):
        current = verifier._FILTERED_VIEW_RECEIPT.read_text(encoding='utf-8')
        with tempfile.TemporaryDirectory() as directory:
            bad = Path(directory) / 'manifest.tsv'
            for payload in (current.replace('ABSENT', 'PRESENT', 1),
                            '\n'.join(reversed(current.strip().splitlines())) + '\n',
                            current + current.splitlines()[0] + '\n'):
                bad.write_text(payload, encoding='utf-8')
                with self.assertRaises(SystemExit):
                    verifier.include_filtered_view_successor([], bad)

    def test_symlinked_receipt_is_not_admitted(self):
        with tempfile.TemporaryDirectory() as directory:
            alias = Path(directory) / 'manifest.tsv'
            alias.symlink_to(verifier._FILTERED_VIEW_RECEIPT)
            with self.assertRaises(SystemExit):
                verifier.include_filtered_view_successor([], alias)


if __name__ == '__main__':
    unittest.main()
