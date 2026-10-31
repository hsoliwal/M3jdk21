# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Synthetic adversarial tests for exact JUnit source and report inventory."""
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET
import verify_surefire as census


class SourceSuiteContract(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.product = Path(self.tmp.name) / 'product'
        self.receipts = Path(self.tmp.name) / 'receipts'
        self.reports = Path(self.tmp.name) / 'reports'
        self.receipts.mkdir()
        self.reports.mkdir()
        self.source = self.product / census.SOURCE_PREFIX
        self.source.mkdir(parents=True)
        self.body = ('// SPDX-License-Identifier: Apache-2.0\n'
                     'package com.m3.collections.interop;\n'
                     'import org.junit.jupiter.api.Test;\n'
                     'final class M3FixtureTest { @Test void checksCase() {} }\n')
        self.java = self.source / 'M3FixtureTest.java'
        self.java.write_text(self.body, encoding='utf-8')
        self.expected = 'com.m3.collections.interop.M3FixtureTest#checksCase'
        self.row = [str(self.java.relative_to(self.product)),
                    census.git_blob(self.java.read_bytes()), 'checksCase']
        self.write_shard()
        self.write_report()

    def write_shard(self):
        (self.receipts / 'expected-00.tsv').write_text('\t'.join(self.row) + '\n')

    def write_report(self, *, names=('checksCase',), skipped=0, failures=0):
        suite = ET.Element('testsuite', name='com.m3.collections.interop.M3FixtureTest',
                           tests=str(len(names)), skipped=str(skipped),
                           failures=str(failures), errors='0')
        for name in names:
            case = ET.SubElement(suite, 'testcase',
                                 classname='com.m3.collections.interop.M3FixtureTest', name=name)
            if skipped:
                ET.SubElement(case, 'skipped')
        ET.ElementTree(suite).write(self.reports / 'TEST-com.m3.collections.interop.M3FixtureTest.xml')

    def expected_methods(self):
        return census.expected_methods(self.product, self.receipts, strict=False)

    def test_valid_exact_identity_and_one_report(self):
        self.assertEqual({self.expected}, self.expected_methods())
        self.assertEqual(1, census.verify_reports(self.reports, {self.expected}))

    def test_source_blob_mutation_refused(self):
        self.java.write_text(self.body + '// mutation\n')
        with self.assertRaisesRegex(ValueError, 'Git blob drift'):
            self.expected_methods()

    def test_missing_source_or_added_source_refused(self):
        self.java.unlink()
        with self.assertRaisesRegex(ValueError, 'unknown or missing'):
            self.expected_methods()
        self.java.write_text(self.body)
        (self.source / 'UnreviewedTest.java').write_text(self.body)
        with self.assertRaisesRegex(ValueError, 'unknown or missing'):
            self.expected_methods()

    def test_unrecorded_method_refused(self):
        self.row[2] = 'unrelatedMethod'
        self.write_shard()
        with self.assertRaisesRegex(ValueError, 'method declaration drift'):
            self.expected_methods()

    def test_missing_report_or_missing_method_refused(self):
        self.write_report(names=())
        with self.assertRaisesRegex(ValueError, 'execution diverged'):
            census.verify_reports(self.reports, {self.expected})
        for path in self.reports.iterdir():
            path.unlink()
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            census.verify_reports(self.reports, {self.expected})

    def test_extra_executed_or_duplicate_report_refused(self):
        self.write_report(names=('checksCase', 'unexpected'))
        with self.assertRaisesRegex(ValueError, 'execution diverged'):
            census.verify_reports(self.reports, {self.expected})
        self.write_report(names=('checksCase', 'checksCase'))
        with self.assertRaisesRegex(ValueError, 'duplicate executed'):
            census.verify_reports(self.reports, {self.expected})

    def test_failure_or_skip_refused(self):
        self.write_report(skipped=1)
        with self.assertRaisesRegex(ValueError, 'failed, errored or skipped'):
            census.verify_reports(self.reports, {self.expected})
        self.write_report(skipped=0, failures=1)
        with self.assertRaisesRegex(ValueError, 'failed, errored or skipped'):
            census.verify_reports(self.reports, {self.expected})

    def test_duplicate_static_method_receipt_refused(self):
        self.row[2] = 'checksCase,checksCase'
        self.write_shard()
        with self.assertRaisesRegex(ValueError, 'method declaration drift'):
            self.expected_methods()


if __name__ == '__main__':
    unittest.main()
