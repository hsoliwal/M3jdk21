# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact additive M3 CPU window manifest; no widened source-owner admission."""
import tempfile
import unittest
from pathlib import Path

from verify_donation import (
    REPO, _CPU_WINDOW_PATHS, _CPU_WINDOW_RECEIPT,
    include_cpu_window_stats_successor, digest,
)


class CpuMaskWindowStatsReceiverTest(unittest.TestCase):
    def test_additive_source_set_matches_pinned_implementation(self):
        historical = [('m3/collections/legacy/Prior.java', 'f' * 64)]
        result = include_cpu_window_stats_successor(historical)
        self.assertEqual(historical, result[:1])
        self.assertEqual(tuple(path for path, _ in result[1:]), _CPU_WINDOW_PATHS)
        self.assertEqual(len(result), 3)
        for path, expected in result[1:]:
            self.assertEqual(digest(REPO / path), expected)

    def test_missing_receipt_never_admits_additional_java(self):
        with tempfile.TemporaryDirectory() as directory:
            old = [('previous', 'a' * 64)]
            self.assertEqual(old, include_cpu_window_stats_successor(
                old, Path(directory) / 'absent.tsv'))

    def test_tampered_reordered_or_symlinked_receipt_fails(self):
        content = _CPU_WINDOW_RECEIPT.read_text(encoding='utf-8')
        with tempfile.TemporaryDirectory() as directory:
            corrupted = Path(directory) / 'corrupt.tsv'
            for sample in (content + content.splitlines()[0] + '\n',
                           '\n'.join(reversed(content.strip().splitlines())) + '\n',
                           content.replace('ABSENT', 'PRESENT', 1)):
                corrupted.write_text(sample, encoding='utf-8')
                with self.assertRaises(SystemExit):
                    include_cpu_window_stats_successor([], corrupted)
            alias = Path(directory) / 'alias.tsv'
            alias.symlink_to(_CPU_WINDOW_RECEIPT)
            with self.assertRaises(SystemExit):
                include_cpu_window_stats_successor([], alias)

    def test_rejects_existing_path_even_when_receipt_is_sealed(self):
        with self.assertRaises(SystemExit):
            include_cpu_window_stats_successor([(_CPU_WINDOW_PATHS[0], 'a' * 64)])


if __name__ == '__main__':
    unittest.main()
