#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Hermetic source-custody tests; test data are NOT LLVM24 or M3JDK21 proof."""
from __future__ import annotations

import hashlib
from pathlib import Path
import subprocess
import tempfile
import unittest
import verify_source as source


def git(root: Path, *args: str) -> str:
    return subprocess.run(['git', '-C', str(root), *args],
                          capture_output=True, text=True, check=True).stdout.strip()


def init(root: Path, files: dict[str, str]) -> tuple[str, dict[str, str]]:
    root.mkdir(parents=True)
    git(root, 'init', '-q')
    git(root, 'config', 'user.email', 'test@example.invalid')
    git(root, 'config', 'user.name', 'M3 Synthetic Source Test')
    for name, content in files.items():
        file = root / name
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(content, encoding='utf-8')
    git(root, 'add', '.')
    git(root, 'commit', '-qm', 'synthetic source baseline')
    return git(root, 'rev-parse', 'HEAD'), {name: git(root, 'rev-parse', 'HEAD:' + name)
                                           for name in files}


class SourceCustodyTests(unittest.TestCase):
    def fixture(self):
        tmp = tempfile.TemporaryDirectory(prefix='m3-llvm-custody-')
        root = Path(tmp.name)
        llvm = root / 'llvm'
        m3 = root / 'm3'
        content = '/* synthetic LLVM JNI; this is NOT LLVM source */\n'
        man = hashlib.sha256(content.encode('utf-8')).hexdigest() + '  proof.c\n'
        revision, files = init(llvm, {
            'm3/llvm-orc-jni/proof.c': content,
            'm3/llvm-orc-jni/SOURCE_PINS.sha256': man,
        })
        owner, m3files = init(m3, {
            'm3/compiler/pom.xml': '<!-- synthetic M3 compiler owner -->\n',
        })
        module = source
        old = (module.LLVM_SOURCE_COMMIT, module.M3_OWNER_BASE,
               module.LLVM_BLOBS, module.M3_BLOBS)
        module.LLVM_SOURCE_COMMIT = revision
        module.M3_OWNER_BASE = owner
        module.LLVM_BLOBS = files
        module.M3_BLOBS = m3files
        self.addCleanup(lambda: (
            setattr(module, 'LLVM_SOURCE_COMMIT', old[0]),
            setattr(module, 'M3_OWNER_BASE', old[1]),
            setattr(module, 'LLVM_BLOBS', old[2]),
            setattr(module, 'M3_BLOBS', old[3]),
        ))
        self.addCleanup(tmp.cleanup)
        return llvm, m3

    def test_two_source_repositories_admitted_without_product_reception(self):
        llvm, m3 = self.fixture()
        receipt = source.verify_original_repositories(m3, llvm)
        self.assertEqual('SOURCE_CUSTODY_PASS_ONLY', receipt['status'])
        self.assertEqual('HOLD', receipt['m3jdk21_product_receiving'])
        self.assertEqual('PASS', receipt['llvm_full_source_sha256'])

    def test_changed_checkout_file_refuses(self):
        llvm, m3 = self.fixture()
        file = llvm / 'm3/llvm-orc-jni/proof.c'
        file.write_text('modified original source\n', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'SOURCE_BLOB_DRIFT'):
            source.verify_original_repositories(m3, llvm)

    def test_symlink_refuses_even_for_identical_bytes(self):
        llvm, m3 = self.fixture()
        file = llvm / 'm3/llvm-orc-jni/proof.c'
        backup = llvm / 'backup.c'
        backup.write_bytes(file.read_bytes())
        file.unlink()
        file.symlink_to(backup)
        with self.assertRaisesRegex(ValueError, 'SYMLINKED_SOURCE_PATH'):
            source.verify_original_repositories(m3, llvm)

    def test_full_sha_manifest_refuses_unpinned_source_change(self):
        llvm, m3 = self.fixture()
        p = llvm / 'm3/llvm-orc-jni/SOURCE_PINS.sha256'
        # A SHA manifest has its own original Git blob, so even a malicious
        # manifest modification is independently refused first.
        p.write_text('0' * 64 + '  proof.c\n', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'SOURCE_BLOB_DRIFT'):
            source.verify_original_repositories(m3, llvm)

    def test_unexpected_moving_llvm_head_refuses(self):
        llvm, m3 = self.fixture()
        (llvm / 'OTHER.md').write_text('new independent commit\n')
        git(llvm, 'add', '.')
        git(llvm, 'commit', '-qm', 'advance source')
        with self.assertRaisesRegex(ValueError, 'LLVM_SOURCE_COMMIT_DRIFT'):
            source.verify_original_repositories(m3, llvm)

    def test_m3_original_owner_change_refuses_even_on_ancestor_branch(self):
        llvm, m3 = self.fixture()
        (m3 / 'm3/compiler/pom.xml').write_text('<!-- changed compiler public source -->\n')
        git(m3, 'add', '.')
        git(m3, 'commit', '-qm', 'source owner changed')
        with self.assertRaisesRegex(ValueError, 'SOURCE_BLOB_DRIFT'):
            source.verify_original_repositories(m3, llvm)

    def test_noncanonical_paths_refuse(self):
        llvm, m3 = self.fixture()
        for text in ('../evil.java', '/tmp/something', 'a\\b.java'):
            with self.assertRaisesRegex(ValueError, 'NONCANONICAL_SOURCE_PATH'):
                source.canonical_regular_file(llvm, text)


if __name__ == '__main__':
    unittest.main()
