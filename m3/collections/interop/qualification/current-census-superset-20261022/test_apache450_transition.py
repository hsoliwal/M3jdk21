# SPDX-License-Identifier: Apache-2.0
"""Fail-closed exact Apache 4.5.0 Bag source/test successor controls."""
from pathlib import Path
import tempfile
import unittest

import verify_superset as census


class Apache450TransitionTest(unittest.TestCase):
    def setUp(self):
        self.original = Path(__file__).parent / 'receipts'
        self.filename = 'changed-apache-bag-retain450-02.tsv'

    def test_exact_dual_transition_and_original_source_census_immutable(self):
        rows = census.exact_apache450_transitions(self.original)
        self.assertEqual(2, len(rows))
        self.assertTrue(all(path in rows for path in (
            census.MAIN + 'M3ApacheBag.java',
            census.TEST + 'M3ApacheBagContractTest.java')))
        self.assertEqual(7, len([
            line for line in (self.original / 'changed-07.tsv').read_text().splitlines()
            if line and not line.startswith('#')]))
        self.assertEqual(40, sum(
            len([line for line in (self.original / filename).read_text().splitlines()
                 if line and not line.startswith('#')])
            for filename in census.SHARDS))

    def test_changed_sha_and_path_are_refused(self):
        original = (self.original / self.filename).read_text()
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            target = folder / self.filename
            for tampered in (
                original.replace('bd03c0a1', 'fd03c0a1', 1),
                original.replace('M3ApacheBagContractTest.java',
                                 'M3ApacheBagRuntimeTest.java', 1),
                original.replace('PENDING_REAL_DONOR_JUNIT', 'PASS', 1),
                original.replace('ac35841fb84a28d86390843e5a3dadcb390f069a',
                                 '9' * 40, 1),
            ):
                target.write_text(tampered, encoding='utf-8')
                with self.assertRaisesRegex(
                        ValueError, 'unadmitted Apache450 source or test successor'):
                    census.exact_apache450_transitions(folder)

    def test_missing_and_symlinked_receipts_are_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            with self.assertRaisesRegex(ValueError, 'missing or aliased'):
                census.exact_apache450_transitions(folder)
            (folder / self.filename).symlink_to(
                (self.original / self.filename).resolve())
            with self.assertRaisesRegex(ValueError, 'missing or aliased'):
                census.exact_apache450_transitions(folder)


if __name__ == '__main__':
    unittest.main()
