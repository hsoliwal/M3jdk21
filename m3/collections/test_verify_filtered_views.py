#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: verify immutable historical receipt output independently of evolved current-source admission.
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


# Frozen 20261017 receipt postimages. Current owners may have qualified successors;
# verify_donation.main separately requires its admitted hashes and exact source set.
HISTORICAL_POSTIMAGES = [
    ('m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java', '0b7f2175dbb34ed087b76a07e93335ee488338d544fd49c332f503acfdd51ca8'),
    ('m3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java', '2b3bb42e411202ecb5998f99c7ca34bf5026842ecab2e4c8b64711400c1f4681'),
    ('m3/collections/src/test/java/com/m3/collections/M3FilteredViewsContract.java', 'fe7960369b1e3fab45dec3eadfadaa13f088e46e9c139de671faccac9fda4575'),
    ('m3/collections/src/test/java/com/m3/collections/M3FilteredViewsTest.java', '5f17bb2a2f2abfd4584b144ff398f6b2359b97cb605fa06e0bbaa8001b81131e'),
    ('m3/collections/src/test/java/com/m3/collections/M3MutableFilteredViewsContract.java', '042f476e027b877b717c9cb6a0877049a1c70ef8425a7fc65f61176f9c372781'),
    ('m3/collections/src/test/java/com/m3/collections/M3MutableFilteredViewsTest.java', '5c9eed13cdf8afc36d74fcf738ffe8cc1fc4724d3f273880df79cf6ac84d0e28'),
]


class FilteredViewsReceivingTest(unittest.TestCase):
    def test_exact_additive_successor_keeps_all_historical_rows(self):
        historical = [('m3/collections/legacy/A.java', 'a' * 64)]
        result = verifier.include_filtered_view_successor(historical)
        self.assertEqual(historical, result[:len(historical)])
        self.assertEqual(7, len(result))
        self.assertEqual(list(verifier._FILTERED_VIEW_PATHS), [row[0] for row in result[1:]])
        self.assertEqual(len(set(path for path, _ in result)), 7)
        self.assertEqual(HISTORICAL_POSTIMAGES, result[1:])
        self.assertEqual(
            '828e24b44e4d0b98c6819bb9dd983d71084ae0661e1d81fe90bd0dd286c50ede',
            verifier.digest(verifier._FILTERED_VIEW_RECEIPT))
        for _, sha in result[1:]:
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
