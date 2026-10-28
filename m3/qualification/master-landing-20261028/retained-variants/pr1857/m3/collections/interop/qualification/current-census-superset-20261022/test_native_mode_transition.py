#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed tests for the separately appended native-mode source transition."""
import hashlib
from pathlib import Path
import shutil
import tempfile
import unittest
import verify_superset as census


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
RECEIPTS = HERE / 'receipts'
PINNED = RECEIPTS / 'changed-native-01.tsv'


class NativeModeSourceTransitionTest(unittest.TestCase):
    def fixture(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        receipts = Path(temporary.name) / 'receipts'
        receipts.mkdir()
        shutil.copyfile(PINNED, receipts / 'changed-native-01.tsv')
        return receipts

    def test_exact_one_source_successor_and_native_mode_postimage(self):
        admitted = census.exact_native_mode_transition(RECEIPTS)
        self.assertEqual((census.NATIVE_MODE_PATH,), tuple(admitted))
        self.assertEqual(census.NATIVE_MODE_ATOM[1:5],
                         admitted[census.NATIVE_MODE_PATH])
        source = ROOT / census.NATIVE_MODE_PATH
        self.assertTrue(source.is_file() and not source.is_symlink())
        data = source.read_bytes()
        self.assertEqual(census.NATIVE_MODE_ATOM[3],
                         hashlib.sha256(data).hexdigest())
        self.assertEqual(census.NATIVE_MODE_ATOM[4], census.git_blob(data))
        self.assertIn(b'if (M3IndexPathMasks.nativeAvailable())', data)
        self.assertIn(b'native required fail closed when absent', data)
        self.assertIn(b'native indexed full 32-bit selection', data)

    def test_tampered_missing_and_alias_receipts_are_refused(self):
        receipts = self.fixture()
        row = receipts / 'changed-native-01.tsv'
        row.write_bytes(row.read_bytes().replace(
            b'CANDIDATE_JAVA21_NATIVE_DUAL_158_PASS_JTREG_HOLD',
            b'CANDIDATE_JAVA21_ALL_PLATFORM_PASS'))
        with self.assertRaisesRegex(ValueError, 'tampered'):
            census.exact_native_mode_transition(receipts)
        row.unlink()
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            census.exact_native_mode_transition(receipts)
        row.symlink_to(PINNED)
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            census.exact_native_mode_transition(receipts)

    def test_immediate_predecessor_and_unknown_path_refusal(self):
        admitted = census.exact_native_mode_transition(RECEIPTS)
        target = census.NATIVE_MODE_PATH
        before = admitted[target][:2]
        after = admitted[target][2:]
        self.assertEqual(after, census.advance_expected_image(
            target, before, admitted))
        with self.assertRaisesRegex(ValueError, 'immediate predecessor'):
            census.advance_expected_image(target, ('0' * 64, before[1]), admitted)
        self.assertEqual(('x', 'y'), census.advance_expected_image(
            'unreviewed/NewTest.java', ('x', 'y'), admitted))

    def test_earlier_nine_compound_and_forty_source_receipts_unchanged(self):
        self.assertEqual(9, len(census.exact_gate_transitions(RECEIPTS)))
        self.assertEqual(census.COMPOUND_TEST_PATH,
                         census.exact_compound_transition(RECEIPTS)[0])
        existing = {line.split('\t')[0] for name in census.SHARDS
                    for line in (RECEIPTS / name).read_text(encoding='utf-8').splitlines()
                    if line and not line.startswith('#')}
        self.assertEqual(40, len(existing))
        self.assertIn(census.NATIVE_MODE_PATH, existing)


if __name__ == '__main__':
    unittest.main()
