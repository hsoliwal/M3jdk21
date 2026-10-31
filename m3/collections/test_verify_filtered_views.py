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
    ('m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java', '0b7f2175dbb34ed087b76a07e93335ee488338d544fd49c332f503acfdd51ca8'),
    ('m3/collections/src/main/java/jdk/m3/collections/M3MutableFilteredViews.java', 'c18a5c7f458c3b66fd867549c1e6887f87aa204dc4246104d116d03c24a8de16'),
    ('m3/collections/src/test/java/jdk/m3/collections/M3FilteredViewsContract.java', '1bd7b8a397933e4fb26869f5d50e2f333edda67f0cb7c272dbdd0bad8ecbd20b'),
    ('m3/collections/src/test/java/jdk/m3/collections/M3FilteredViewsTest.java', '67c04de7eaf27eb88fe6546ed4f5034b344f86bba047b58e670525860f6e409b'),
    ('m3/collections/src/test/java/jdk/m3/collections/M3MutableFilteredViewsContract.java', 'd8239655c4be7b39d6da4fd357ddfea15965a4fad968e206b3e312bcdb297938'),
    ('m3/collections/src/test/java/jdk/m3/collections/M3MutableFilteredViewsTest.java', '3f8df58e345813985e9977befd5d152b7adadcb3f9ef01bac2ed1ac753751ff5'),
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
            'e6e70eb2d1ec267edb8bf6647881d157453350702f32f36828c13e82c178454b',
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
