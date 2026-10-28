#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Original source-census compatibility and tamper-refusal oracle."""
import hashlib
import json
import subprocess
from pathlib import Path
import tempfile
import unittest
from audit import inspect


def sha(data):
    return hashlib.sha256(data).hexdigest()


class CurrentSourceCensusTest(unittest.TestCase):
    def root(self, place):
        root = Path(place)
        old = root / 'm3/collections/src/main/java/com/m3/collections/A.java'
        new = root / 'm3/collections/src/test/java/com/m3/collections/T.java'
        old.parent.mkdir(parents=True)
        new.parent.mkdir(parents=True)
        old.write_bytes(b'class A {}\n')
        new.write_bytes(b'class T {}\n')
        manifest = root / 'm3/collections/qualification/historical.tsv'
        manifest.parent.mkdir(parents=True)
        manifest.write_text('m3/collections/src/main/java/com/m3/collections/A.java\t'
                            + sha(old.read_bytes()) + '\n')
        packet = root / 'm3/collections/synexia-donation.json'
        packet.write_text(json.dumps({'installed_source_manifest': 'qualification/historical.tsv',
                                      'installed_source_manifest_sha256': sha(manifest.read_bytes())}))
        return root, old, new, manifest

    def test_exact_historical_and_unlisted_source_are_distinguished(self):
        with tempfile.TemporaryDirectory() as d:
            root, old, new, manifest = self.root(d)
            original = (old.read_bytes(), new.read_bytes(), manifest.read_bytes())
            census = inspect(root)
            self.assertEqual('INVENTORY_ONLY_NOT_QUALIFIED', census['status'])
            self.assertFalse(census['promotion_allowed'])
            self.assertEqual((1, 2, 1), (census['historical_java_rows'],
                                         census['current_java_files'], census['historical_exact_match']))
            self.assertEqual(1, len(census['absent_from_this_historical_manifest']))
            self.assertEqual([], census['historical_digest_mismatch'])
            self.assertEqual(original, (old.read_bytes(), new.read_bytes(), manifest.read_bytes()))

    def test_changed_historical_java_refuses_silent_promotion(self):
        with tempfile.TemporaryDirectory() as d:
            root, old, _, _ = self.root(d)
            old.write_bytes(b'class A { int x; }\n')
            census = inspect(root)
            self.assertFalse(census['promotion_allowed'])
            self.assertEqual(1, len(census['historical_digest_mismatch']))
            self.assertEqual(0, census['historical_exact_match'])

    def test_broken_manifest_and_duplicates_refuse(self):
        with tempfile.TemporaryDirectory() as d:
            root, old, _, manifest = self.root(d)
            manifest.write_text(manifest.read_text() + manifest.read_text())
            with self.assertRaisesRegex(ValueError, 'manifest is missing or changed'):
                inspect(root)
            # Even a newly signed manifest must refuse duplicate owner paths.
            packet = root / 'm3/collections/synexia-donation.json'
            info = json.loads(packet.read_text())
            info['installed_source_manifest_sha256'] = sha(manifest.read_bytes())
            packet.write_text(json.dumps(info))
            with self.assertRaisesRegex(ValueError, 'duplicate historical source path'):
                inspect(root)

    def test_historical_row_order_is_preserved_not_reinterpreted(self):
        with tempfile.TemporaryDirectory() as d:
            root, old, new, manifest = self.root(d)
            # Existing M3 donation records contain chronological, not lexical, rows.
            manifest.write_text('m3/collections/src/test/java/com/m3/collections/T.java\t'
                                + sha(new.read_bytes()) + '\n'
                                + manifest.read_text())
            packet = root / 'm3/collections/synexia-donation.json'
            info = json.loads(packet.read_text())
            info['installed_source_manifest_sha256'] = sha(manifest.read_bytes())
            packet.write_text(json.dumps(info))
            census = inspect(root)
            self.assertEqual(2, census['historical_java_rows'])
            self.assertEqual(2, census['historical_exact_match'])
            self.assertFalse(census['promotion_allowed'])

    def test_git_commit_binding_and_tracked_mutation_refusal(self):
        with tempfile.TemporaryDirectory() as d:
            root, old, _, _ = self.root(d)
            subprocess.run(['git', '-C', str(root), 'init', '-q'], check=True)
            subprocess.run(['git', '-C', str(root), 'config', 'user.name', 'M3 source oracle'], check=True)
            subprocess.run(['git', '-C', str(root), 'config', 'user.email', 'source-oracle@example.invalid'], check=True)
            subprocess.run(['git', '-C', str(root), 'add', '-A'], check=True)
            subprocess.run(['git', '-C', str(root), 'commit', '-qm', 'frozen fixture'], check=True)
            head = subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()
            census = inspect(root, head)
            self.assertEqual(head, census['checked_head_sha'])
            self.assertEqual(2, census['exact_git_java_index_count'])
            self.assertFalse(census['promotion_allowed'])
            with self.assertRaisesRegex(ValueError, 'requested PR head'):
                inspect(root, '0' * 40)
            old.write_text('class A { int changed; }\n')
            with self.assertRaisesRegex(ValueError, 'modified after checkout'):
                inspect(root, head)

    def test_untracked_source_is_not_accepted_as_exact_git_head(self):
        with tempfile.TemporaryDirectory() as d:
            root, old, _, _ = self.root(d)
            subprocess.run(['git', '-C', str(root), 'init', '-q'], check=True)
            subprocess.run(['git', '-C', str(root), 'config', 'user.name', 'M3 source oracle'], check=True)
            subprocess.run(['git', '-C', str(root), 'config', 'user.email', 'source-oracle@example.invalid'], check=True)
            subprocess.run(['git', '-C', str(root), 'add', '-A'], check=True)
            subprocess.run(['git', '-C', str(root), 'commit', '-qm', 'frozen fixture'], check=True)
            head = subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()
            extra = old.with_name('Untracked.java')
            extra.write_text('class Untracked {}\n')
            with self.assertRaisesRegex(ValueError, 'differs from the checked-out Git index'):
                inspect(root, head)

    def test_symlinked_java_refused(self):
        with tempfile.TemporaryDirectory() as d:
            root, _, new, _ = self.root(d)
            target = new.with_name('Alias.java')
            target.symlink_to(new)
            with self.assertRaisesRegex(ValueError, 'symlinked current Java owner'):
                inspect(root)


if __name__ == '__main__':
    unittest.main()
