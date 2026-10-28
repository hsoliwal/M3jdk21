# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Adversarial refusal tests for exactly three Java21 source deltas."""
from pathlib import Path
import tempfile
import unittest
import verify_superset as authority

ORIGIN = Path(__file__).resolve().parent / 'receipts' / 'changed-compiler-03.tsv'


class ThreeCompilerRevisionTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.folder = Path(tmp.name)
        self.ledger = self.folder / 'changed-compiler-03.tsv'
        self.ledger.write_bytes(ORIGIN.read_bytes())

    def admitted(self):
        return authority.admitted_compiler_revisions(self.folder)

    def test_exact_three_are_pinned_without_wildcard(self):
        result = self.admitted()
        self.assertEqual(set(authority.COMPILER_TEST_PATHS), set(result))
        self.assertEqual(3, len(result))
        self.assertEqual('9edf50007e098846f0d677204a445b6bc52b7f0d',
                         result[authority.APACHE_TEST][1])

    def test_missing_or_symlinked_ledger_refused(self):
        self.ledger.unlink()
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            self.admitted()
        self.ledger.symlink_to(ORIGIN)
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            self.admitted()

    def test_forged_source_hash_or_blob_refused(self):
        content = self.ledger.read_text()
        self.ledger.write_text(content.replace(
            '218516ad3d49f471a20c8b5dcfbdcb88ea8e278b0466dcaf900247ff7398c8d7',
            'f' * 64), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'blob drift'):
            self.admitted()
        self.ledger.write_text(content.replace(
            'c2307832df8ef7e1f429af6a2532dc8f7ec04596',
            '0' * 40), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'blob drift'):
            self.admitted()

    def test_extra_path_and_missing_row_refused(self):
        original = self.ledger.read_text()
        self.ledger.write_text(original + original.splitlines()[0] + '\n')
        with self.assertRaisesRegex(ValueError, 'blob drift'):
            self.admitted()
        self.ledger.write_text('\n'.join(original.splitlines()[:-1]) + '\n')
        with self.assertRaisesRegex(ValueError, 'blob drift'):
            self.admitted()

    def test_wrong_recipe_or_qualification_status_refused(self):
        original = self.ledger.read_text()
        self.ledger.write_text(original.replace(
            authority.COMPILER_RECIPE, 'unreviewed-recipe'), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'blob drift'):
            self.admitted()
        self.ledger.write_text(original.replace(
            'PENDING_FULL_VENDOR_CI', 'PROVEN_WITHOUT_TESTS'), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'blob drift'):
            self.admitted()

    def test_reordered_rows_refused(self):
        original = self.ledger.read_text().splitlines()
        original[0], original[1] = original[1], original[0]
        self.ledger.write_text('\n'.join(original) + '\n', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'blob drift'):
            self.admitted()


if __name__ == '__main__':
    unittest.main()
