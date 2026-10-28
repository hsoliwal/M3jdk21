#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Negative controls for the nine *sequential* source-sealed receiver fixes."""
import hashlib
from pathlib import Path
import shutil
import tempfile
import unittest
import verify_superset as census


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
RECEIPTS = HERE / 'receipts'
PINNED = RECEIPTS / 'changed-09.tsv'
ORIGINAL = ROOT / 'm3/collections/interop/qualification/current-census-20261017/current-source-census.tsv'


class InteropGateSuccessorContract(unittest.TestCase):
    def fixture(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        receipts = Path(temporary.name) / 'receipts'
        receipts.mkdir()
        shutil.copyfile(PINNED, receipts / 'changed-09.tsv')
        return receipts

    def test_all_nine_paths_and_actual_reviewed_postimages(self):
        transitions = census.exact_gate_transitions(RECEIPTS)
        self.assertEqual(census.GATE_PATHS, tuple(transitions))
        self.assertEqual(9, len(transitions))
        for path, (_, _, after_sha, after_blob) in transitions.items():
            source = ROOT / path
            self.assertTrue(source.is_file() and not source.is_symlink(), path)
            data = source.read_bytes()
            self.assertEqual(after_sha, hashlib.sha256(data).hexdigest(), path)
            self.assertEqual(after_blob, census.git_blob(data), path)
            self.assertIn(b'SPDX-License-Identifier: Apache-2.0', data[:1800])

    def test_immediate_predecessor_and_unadmitted_path_refusal(self):
        transitions = census.exact_gate_transitions(RECEIPTS)
        for path, (before_sha, before_blob, after_sha, after_blob) in transitions.items():
            self.assertEqual((after_sha, after_blob),
                             census.advance_expected_image(path, (before_sha, before_blob),
                                                           transitions))
            with self.assertRaisesRegex(ValueError, 'immediate predecessor'):
                census.advance_expected_image(path, ('f' * 64, before_blob), transitions)
        self.assertEqual(('a', 'b'), census.advance_expected_image(
            'unadmitted/file.java', ('a', 'b'), transitions))

    def test_missing_alias_and_byte_tampering_refused(self):
        receipts = self.fixture()
        target = receipts / 'changed-09.tsv'
        target.write_bytes(target.read_bytes().replace(
            b'CANDIDATE_JAVA21_158_PASS_JTREG_HOLD',
            b'CANDIDATE_JAVA21_158_PASS_ALL_PLATFORM_GREEN'))
        with self.assertRaisesRegex(ValueError, 'tampered'):
            census.exact_gate_transitions(receipts)
        target.unlink()
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            census.exact_gate_transitions(receipts)
        target.symlink_to(PINNED)
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            census.exact_gate_transitions(receipts)

    def test_nine_successors_preserve_seven_and_one_previous_receipts(self):
        transitions = census.exact_gate_transitions(RECEIPTS)
        corrected = census.exact_compound_transition(RECEIPTS)
        self.assertEqual(corrected[3:5], transitions[corrected[0]][:2])
        historical = (RECEIPTS / 'changed-07.tsv').read_text(encoding='utf-8').splitlines()
        revised = {parts[0]: tuple(parts[3:5]) for line in historical
                   if line and not line.startswith('#')
                   for parts in [line.split('\t')]}
        self.assertEqual(revised[census.TEST + 'M3SdkFamiliesTest.java'],
                         transitions[census.TEST + 'M3SdkFamiliesTest.java'][:2])
        self.assertEqual(7, len(revised))

    def test_complete_existing_40_source_admission_advances_only_four(self):
        older = {line.split('\t')[0] for line in ORIGINAL.read_text(
            encoding='utf-8').splitlines() if line and not line.startswith('#')}
        self.assertEqual(36, len(older))
        new = {line.split('\t')[0] for name in census.SHARDS
               for line in (RECEIPTS / name).read_text(encoding='utf-8').splitlines()
               if line and not line.startswith('#')}
        self.assertEqual(40, len(new))
        transitions = census.exact_gate_transitions(RECEIPTS)
        self.assertEqual(5, len(set(transitions) & older))
        self.assertEqual(4, len(set(transitions) & new))
        self.assertEqual(40, len(census.verify_additions(
            ROOT, RECEIPTS, older, older | new, transitions)))


if __name__ == '__main__':
    unittest.main()
