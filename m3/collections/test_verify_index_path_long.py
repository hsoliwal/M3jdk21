#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed 64-bit path-mask overlay contract tests; no product source mutation."""
import tempfile
import unittest
from pathlib import Path

from verify_donation import (
    _PATH_LONG_RECEIPT,
    _PATH_LONG_PATHS,
    include_path_long_successor,
)


class LongPathMaskReceiptTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(_PATH_LONG_RECEIPT.is_file())
        entries = [line.split('\t') for line in _PATH_LONG_RECEIPT.read_text(
            encoding='utf-8').splitlines()]
        self.assertEqual(len(entries), 4)
        self.assertEqual([row[0] for row in entries], list(_PATH_LONG_PATHS))
        self.old = [(row[0], row[1]) for row in entries[:2]]
        self.new = [(row[0], row[2]) for row in entries]

    def test_reconcile_exact_predecessor_and_new_tests(self):
        self.assertEqual(include_path_long_successor(self.old), self.new)
        extra = ('m3/collections/src/main/java/com/m3/collections/Unrelated.java',
                 '0' * 64)
        self.assertEqual(include_path_long_successor(self.old + [extra]),
                         self.new[:2] + [extra] + self.new[2:])

    def test_absent_optional_receipt_keeps_predecessor(self):
        with tempfile.TemporaryDirectory() as temp:
            self.assertEqual(include_path_long_successor(
                self.old, Path(temp) / 'missing.tsv'), self.old)

    def test_bad_hash_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            filename = Path(temp) / 'corrupt.tsv'
            filename.write_text(_PATH_LONG_RECEIPT.read_text() + '# altered\n')
            with self.assertRaises(SystemExit):
                include_path_long_successor(self.old, filename)

    def test_occupied_source_and_stale_preimage_fail(self):
        with self.assertRaises(SystemExit):
            include_path_long_successor(self.old + [self.new[2]])
        bad = self.old.copy()
        bad[0] = (bad[0][0], '1' * 64)
        with self.assertRaises(SystemExit):
            include_path_long_successor(bad)

    def test_symlink_receipt_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'linked.tsv'
            path.symlink_to(_PATH_LONG_RECEIPT)
            with self.assertRaises(SystemExit):
                include_path_long_successor(self.old, path)


if __name__ == '__main__':
    unittest.main()
