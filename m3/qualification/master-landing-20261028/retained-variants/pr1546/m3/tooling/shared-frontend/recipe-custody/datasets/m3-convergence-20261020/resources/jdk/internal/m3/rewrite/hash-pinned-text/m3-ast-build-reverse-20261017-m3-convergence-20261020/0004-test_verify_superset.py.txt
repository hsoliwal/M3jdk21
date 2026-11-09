# SPDX-License-Identifier: Apache-2.0
"""Synthetic Git history proves preservation/refusal, independent of live GitHub."""
import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from verify_superset import verify

ENV = {**os.environ,
       'GIT_AUTHOR_NAME': 'M3 Qualification', 'GIT_AUTHOR_EMAIL': 'm3@example.invalid',
       'GIT_COMMITTER_NAME': 'M3 Qualification', 'GIT_COMMITTER_EMAIL': 'm3@example.invalid'}


def git(root, *args, data=None):
    result = subprocess.run(['git', '-C', str(root), *args], input=data,
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=ENV)
    if result.returncode: raise AssertionError((args, result.stderr))
    return result.stdout.strip()


class SupersetAdmissionTest(unittest.TestCase):
    def fixture(self, overwritten=False, extra=False):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        root = Path(tmp.name)
        git(root, 'init', '-q')
        (root / 'baseline.txt').write_text('base\n')
        git(root, 'add', '.')
        git(root, 'commit', '-qm', 'base')
        base = git(root, 'rev-parse', 'HEAD')
        parents = []
        for n in range(4):
            git(root, 'checkout', '-q', '-B', f'donor{n}', base)
            (root / f'donor{n}.txt').write_text(f'payload-{n}\n')
            git(root, 'add', '.')
            git(root, 'commit', '-qm', f'donor{n}')
            parents.append((f'donor{n}', git(root, 'rev-parse', 'HEAD')))
        git(root, 'checkout', '-q', '-B', 'admission', base)
        admission = root / 'm3/runtime-integration/convergence-20261020'
        admission.mkdir(parents=True)
        (admission / 'README.md').write_text('metadata\n')
        git(root, 'add', '.')
        git(root, 'commit', '-qm', 'recipe-first')
        recipe_parent = git(root, 'rev-parse', 'HEAD')
        git(root, 'checkout', '-q', '-B', 'compose', recipe_parent)
        for n in range(4):
            (root / f'donor{n}.txt').write_text(f'payload-{n}\n')
        if overwritten: (root / 'donor2.txt').write_text('wrong\n')
        if extra: (root / 'unexpected.java').write_text('class X {}\n')
        git(root, 'add', '.')
        tree = git(root, 'write-tree')
        args = ['commit-tree', tree, '-p', recipe_parent]
        for _, sha in parents: args += ['-p', sha]
        merged = git(root, *args, '-m', 'M3 convergence')
        return root, base, tuple(parents), merged

    def test_success_and_repeatable(self):
        root, base, parents, merged = self.fixture()
        first = verify(root, merged, base, parents, expected_paths=4)
        second = verify(root, merged, base, parents, expected_paths=4)
        self.assertEqual(first, second)
        self.assertEqual(first['preserved_source_paths'], 4)
        self.assertEqual(first['qualification_paths'], 1)

    def test_overwritten_donor_refused(self):
        root, base, parents, merged = self.fixture(overwritten=True)
        with self.assertRaisesRegex(ValueError, 'donor content/mode lost'):
            verify(root, merged, base, parents, expected_paths=4)

    def test_unreviewed_extra_refused(self):
        root, base, parents, merged = self.fixture(extra=True)
        with self.assertRaisesRegex(ValueError, 'unreviewed extra'):
            verify(root, merged, base, parents, expected_paths=4)

    def test_wrong_parent_order_refused(self):
        root, base, parents, merged = self.fixture()
        wrong = (parents[1], parents[0], *parents[2:])
        with self.assertRaisesRegex(ValueError, 'parent identity/order'):
            verify(root, merged, base, wrong, expected_paths=4)

    def test_changed_expected_count_refused(self):
        root, base, parents, merged = self.fixture()
        with self.assertRaisesRegex(ValueError, 'source path count'):
            verify(root, merged, base, parents, expected_paths=5)


if __name__ == '__main__':
    unittest.main()
