# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
import capture


class CaptureTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='capture-fixture-', dir=Path(__file__).parent)
        self.root = Path(self.temp.name)
        self.table = Path(__file__).with_name('INPUTS.tsv').read_text()
    def tearDown(self):
        self.temp.cleanup()
    def input(self, text):
        path = self.root / 'input.tsv'; path.write_text(text)
        return path
    def test_all_explicit_sources_admitted(self):
        rows = capture.read_inputs(self.input(self.table))
        self.assertEqual(8, len(rows))
        self.assertEqual(sorted(row['repository'] for row in rows), [row['repository'] for row in rows])
    def test_missing_source_refused(self):
        with self.assertRaises(ValueError):
            capture.read_inputs(self.input('\n'.join(self.table.splitlines()[:-1])+'\n'))
    def test_duplicate_refused(self):
        with self.assertRaises(ValueError):
            capture.read_inputs(self.input(self.table+self.table.splitlines()[1]+'\n'))
    def test_unapproved_repository_refused(self):
        with self.assertRaises(ValueError):
            capture.read_inputs(self.input(self.table.replace('hsoliwal/guava', 'someone/guava')))
    def test_branch_in_place_of_pin_refused(self):
        line = self.table.splitlines()[1]
        with self.assertRaises(ValueError):
            capture.read_inputs(self.input(self.table.replace(line.split('\t')[1], 'master')))
    def test_reference_role_cannot_claim_fork(self):
        with self.assertRaises(ValueError):
            capture.read_inputs(self.input(self.table.replace('UPSTREAM_REFERENCE_ONLY', 'PUBLIC_FORK_SOURCE')))
    def test_schema_refused(self):
        with self.assertRaises(ValueError):
            capture.read_inputs(self.input(self.table.replace('repository\tcommit\trole', 'repo\tsha\trole')))
    def test_empty_table_refused(self):
        with self.assertRaises(ValueError):
            capture.read_inputs(self.input('repository\tcommit\trole\n'))
    def test_git_commands_never_push_or_execute_donor_build(self):
        for row in capture.read_inputs(self.input(self.table)):
            commands = capture.checkout_commands(row)
            for command, _ in commands:
                self.assertIn(command[0], ('init', 'remote', 'config', 'sparse-checkout', 'fetch', 'checkout'))
                self.assertNotIn('push', command)
            self.assertEqual(['checkout', '--detach', row['commit']], commands[-1][0])
    def test_archive_keeps_hidden_files_and_is_byte_deterministic(self):
        packet = self.root / 'packet'; packet.mkdir()
        (packet / '.notice').write_text('original data\n')
        (packet / 'RESULT.json').write_text(json.dumps({'product_ready': False}))
        first = self.root / 'first.zip'; second = self.root / 'second.zip'
        self.assertEqual(capture.archive_packet(packet, first), capture.archive_packet(packet, second))
        with zipfile.ZipFile(first) as archive:
            self.assertEqual(b'original data\n', archive.read('.notice'))
        with self.assertRaises(FileExistsError):
            capture.archive_packet(packet, first)
    def test_packet_symlink_refused(self):
        packet = self.root / 'packet'; packet.mkdir()
        (packet / 'link').symlink_to(self.root / 'outside')
        with self.assertRaises(ValueError):
            capture.archive_packet(packet, self.root / 'refused.zip')


if __name__ == '__main__':
    unittest.main()
