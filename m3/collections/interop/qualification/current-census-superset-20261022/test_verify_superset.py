#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Finite negative-control gates for source-sealed additive 40-owner admission."""
import hashlib
from pathlib import Path
import tempfile
import unittest
import verify_superset as owner


class SupersetContract(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.receipts = self.root / 'receipts'
        self.receipts.mkdir()
        self.older = {owner.MAIN + f'Old{i:02d}.java' for i in range(18)}
        self.older |= {owner.TEST + f'OldTest{i:02d}.java' for i in range(18)}
        self.paths = sorted([owner.MAIN + f'M3AddMain{i:02d}.java' for i in range(13)]
                            + [owner.TEST + f'M3AddTest{i:02d}.java' for i in range(27)])
        self.tracked = self.older | set(self.paths)
        self.rows = []
        for relative in self.paths:
            path = self.root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            data = b'// SPDX-License-Identifier: Apache-2.0\npackage com.m3.collections.interop;\n'
            path.write_bytes(data)
            kind = 'PRODUCTION' if relative.startswith(owner.MAIN) else 'TEST'
            self.rows.append([relative, hashlib.sha256(data).hexdigest(),
                              owner.git_blob(data), 'five-provider-index', kind, 'Apache-2.0'])
        self.rebuild()

    def rebuild(self):
        for at, name in enumerate(owner.SHARDS):
            ten = self.rows[at * 10:at * 10 + 10]
            (self.receipts / name).write_text(
                ''.join('\t'.join(row) + '\n' for row in ten), encoding='utf-8')

    def check(self):
        return owner.verify_additions(self.root, self.receipts, self.older, self.tracked)

    def test_exact_36_plus_40(self):
        self.assertEqual(self.paths, self.check())

    def test_mutated_bytes_fail_closed(self):
        (self.root / self.paths[0]).write_text('unexpected edit\n')
        with self.assertRaisesRegex(ValueError, 'source SHA'):
            self.check()

    def test_added_unadmitted_file_rejected(self):
        self.tracked.add(owner.TEST + 'Unreviewed.java')
        with self.assertRaisesRegex(ValueError, 'tracking drift'):
            self.check()

    def test_duplicate_and_unsorted_rows_refused(self):
        self.rows[1][0] = self.rows[0][0]
        self.rebuild()
        with self.assertRaisesRegex(ValueError, 'unique sorted'):
            self.check()

    def test_header_license_and_forged_sha_refused(self):
        self.rows[0][1] = 'f' * 64
        self.rebuild()
        with self.assertRaisesRegex(ValueError, 'source SHA'):
            self.check()
        self.rows[0][1] = hashlib.sha256((self.root / self.paths[0]).read_bytes()).hexdigest()
        self.rows[0][5] = 'UNKNOWN'
        self.rebuild()
        with self.assertRaisesRegex(ValueError, 'SPDX'):
            self.check()

    def test_symlink_and_shard_loss_refused(self):
        target = self.root / self.paths[0]
        target.unlink()
        target.symlink_to(self.root / self.paths[1])
        with self.assertRaisesRegex(ValueError, 'symlinked'):
            self.check()
        (self.receipts / owner.SHARDS[0]).unlink()
        with self.assertRaisesRegex(ValueError, 'missing or aliased additive'):
            self.check()


if __name__ == '__main__':
    unittest.main()
