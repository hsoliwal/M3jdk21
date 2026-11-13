# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Bounded regression for the exact M3 path mask source admission overlay."""
import tempfile
import unittest
from pathlib import Path

from verify_donation import (
    _PATH_MASK_PATHS,
    _PATH_MASK_RECEIPT,
    include_path_mask_successor,
)


class IndexPathMaskOverlayTest(unittest.TestCase):
    def test_admits_only_exact_sealed_sources(self):
        historical = [('unrelated', 'prior-sha')]
        expected = include_path_mask_successor(historical)
        self.assertEqual(len(expected), len(historical) + 4)
        self.assertEqual(expected[:1], historical)
        self.assertEqual([path for path, _ in expected[1:]], list(_PATH_MASK_PATHS))

    def test_missing_overlay_does_not_expand_authority(self):
        with tempfile.TemporaryDirectory() as td:
            self.assertEqual(include_path_mask_successor([('old', 'hash')],
                             Path(td) / 'missing.tsv'), [('old', 'hash')])

    def test_tampered_or_aliased_overlay_is_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            receipt = Path(td) / 'tampered.tsv'
            receipt.write_text(_PATH_MASK_RECEIPT.read_text() + 'extra\n',
                               encoding='utf-8')
            with self.assertRaises(SystemExit):
                include_path_mask_successor([], receipt)
            alias = Path(td) / 'aliased.tsv'
            alias.symlink_to(_PATH_MASK_RECEIPT)
            with self.assertRaises(SystemExit):
                include_path_mask_successor([], alias)


if __name__ == '__main__':
    unittest.main()
