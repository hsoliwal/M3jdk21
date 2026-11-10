#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Hermetic source-closure contract tests: no downloads and no real Maven."""
import tempfile
import unittest
from pathlib import Path

from source_closure import admit, closure, load_inventory, ROOTS


def pom(group, artifact, version, dependencies=()):
    deps = ''.join(
        f'<dependency><groupId>{g}</groupId><artifactId>{a}</artifactId>'
        f'<version>{v}</version></dependency>' for g, a, v in dependencies
    )
    return (
        '<project xmlns="http://maven.apache.org/POM/4.0.0">'
        f'<groupId>{group}</groupId><artifactId>{artifact}</artifactId>'
        f'<version>{version}</version><dependencies>{deps}</dependencies></project>'
    )


class SourceClosureTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.vendor = self.root / 'm3/vendor/third_party'
        self.vendor.mkdir(parents=True)

    def write(self, rows):
        entries = ''.join(f'<module>{folder}</module>' for folder, *_ in rows)
        (self.vendor / 'pom.xml').write_text(
            '<project xmlns="http://maven.apache.org/POM/4.0.0"><modules>'
            + entries + '</modules></project>', encoding='utf-8')
        for folder, group, artifact, version, dependencies in rows:
            path = self.vendor / folder
            path.mkdir(parents=True)
            (path / 'pom.xml').write_text(
                pom(group, artifact, version, dependencies), encoding='utf-8')
            (path / 'verify_source.py').write_text('print("verified")\n', encoding='utf-8')

    def test_transitive_build_inputs_and_topological_closure(self):
        self.write([
            ('dependency', 'g', 'dependency', '1-m3-source-1', ()),
            ('consumer', 'g', 'consumer', '2-m3-source-1',
             [('g', 'dependency', '1-m3-source-1'), ('binary', 'adapter', '3')]),
        ])
        roots = {('g', 'consumer', '2-m3-source-1'): 'consumer'}
        self.assertEqual(set(closure(load_inventory(self.root), roots)),
                         {('g', 'consumer', '2-m3-source-1'),
                          ('g', 'dependency', '1-m3-source-1')})

    def test_refuse_unvendored_pin(self):
        self.write([('consumer', 'g', 'consumer', '2-m3-source-1',
                     [('g', 'missing', '1-m3-source-1')])])
        with self.assertRaisesRegex(ValueError, 'unvendored'):
            closure(load_inventory(self.root),
                    {('g', 'consumer', '2-m3-source-1'): 'consumer'})

    def test_refuse_wrong_root_coordinates(self):
        self.write([('consumer', 'g', 'consumer', '2-m3-source-1', ())])
        with self.assertRaisesRegex(ValueError, 'missing exact'):
            closure(load_inventory(self.root), ROOTS)

    def test_refuse_cycle(self):
        self.write([
            ('alpha', 'g', 'alpha', '1-m3-source-1', [('g', 'beta', '1-m3-source-1')]),
            ('beta', 'g', 'beta', '1-m3-source-1', [('g', 'alpha', '1-m3-source-1')]),
        ])
        with self.assertRaisesRegex(ValueError, 'cycle'):
            closure(load_inventory(self.root), {('g', 'alpha', '1-m3-source-1'): 'alpha'})

    def test_refuse_duplicate_gav(self):
        self.write([('one', 'g', 'same', '1-m3-source-1', ()),
                    ('two', 'g', 'same', '1-m3-source-1', ())])
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            load_inventory(self.root)

    def test_refuse_unverified_module(self):
        self.write([('consumer', 'g', 'consumer', '2-m3-source-1', ())])
        (self.vendor / 'consumer/verify_source.py').unlink()
        # Admit only the fixture owner; a missing verifier must fail closed.
        roots = {('g', 'consumer', '2-m3-source-1'): 'consumer'}
        with self.assertRaisesRegex(ValueError, 'integrity verifier'):
            admit(self.root, run_checks=False, roots=roots)

    def test_refuse_unsafe_module_name(self):
        (self.vendor / 'pom.xml').write_text(
            '<project xmlns="http://maven.apache.org/POM/4.0.0">'
            '<modules><module>../escape</module></modules></project>', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'unsafe'):
            load_inventory(self.root)


if __name__ == '__main__':
    unittest.main(verbosity=2)
