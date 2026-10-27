#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Source-backed negative controls for the V4 JUnit census and real executed XML."""
import shutil
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

import verify_current_surefire as census

REPO = Path(__file__).resolve().parents[5]
REPORTS = 'm3/collections/interop/target/surefire-reports'
SOURCES = census.SRC
HISTORY = census.BASE
CURRENT = census.CURRENT


class OriginalJUnitV4Contract(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.product = Path(self.temporary.name) / 'product'
        for prefix, match in ((SOURCES, '*.java'),
                              (HISTORY + 'receipts/', '*.tsv'),
                              (REPORTS + '/', 'TEST-*.xml')):
            source = REPO / prefix
            destination = self.product / prefix
            destination.mkdir(parents=True, exist_ok=True)
            if not source.is_dir():
                raise AssertionError('missing real source/census/run: ' + str(source))
            for path in source.glob(match):
                if path.is_symlink() or not path.is_file():
                    raise AssertionError('aliased source/report: ' + str(path))
                shutil.copyfile(path, destination / path.name)
        for name in (census.HISTORY, census.RECEIPT):
            src, dest = REPO / name, self.product / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            if src.is_symlink() or not src.is_file():
                raise AssertionError('missing original source/census: ' + str(src))
            shutil.copyfile(src, dest)

    def expected(self):
        return census.expected_methods(self.product)

    def suite(self):
        return self.product / REPORTS

    def xml(self):
        return next(self.suite().glob('TEST-*.xml'))

    def source(self, name):
        return self.product / SOURCES / name

    def refuse(self, operation, message):
        with self.assertRaisesRegex(ValueError, message):
            operation()

    def test_original_real_167_case_archive_and_source_pass(self):
        expected = self.expected()
        self.assertEqual(167, len(expected))
        self.assertEqual(37, census.verify_reports(self.product, self.suite(), expected))

    def test_new_source_mutation_refused(self):
        source = self.source('M3ApacheStreamsTest.java')
        source.write_bytes(source.read_bytes() + b'// forged\n')
        self.refuse(self.expected, 'V4 JUnit source Git/SHA256 mismatch')

    def test_revised_source_mutation_refused(self):
        source = self.source('M3ApacheBagContractTest.java')
        source.write_bytes(source.read_bytes() + b'// forged\n')
        self.refuse(self.expected, 'V4 JUnit source Git/SHA256 mismatch')

    def test_unchanged_source_mutation_refused(self):
        source = self.source('M3BiMapTest.java')
        source.write_bytes(source.read_bytes() + b'// forged\n')
        self.refuse(self.expected, 'unreviewed original JUnit source change')

    def test_extra_unregistered_source_refused(self):
        self.source('UnreviewedTest.java').write_text('class UnreviewedTest {}')
        self.refuse(self.expected, 'unrecorded/missing JUnit Java source')

    def test_historical_receipt_mutation_refused(self):
        receipt = self.product / HISTORY / 'receipts/expected-00.tsv'
        receipt.write_bytes(receipt.read_bytes() + b'# forged\n')
        self.refuse(self.expected, 'historical original JUnit receipt changed')

    def test_historical_verifier_mutation_refused(self):
        verifier = self.product / HISTORY / 'verify_surefire.py'
        verifier.write_bytes(verifier.read_bytes() + b'# forged\n')
        self.refuse(self.expected, 'historical original JUnit verifier changed')

    def test_v4_receipt_mutation_refused(self):
        receipt = self.product / census.RECEIPT
        receipt.write_bytes(receipt.read_bytes() + b'# forged\n')
        self.refuse(self.expected, 'V4 JUnit source-succession receipt content drift')

    def test_missing_executed_report_refused(self):
        self.xml().unlink()
        expected = self.expected()
        self.refuse(lambda: census.verify_reports(self.product, self.suite(), expected),
                    'JUnit execution diverged')

    def test_forged_executed_summary_refused(self):
        target = self.xml()
        data = ET.parse(target)
        data.getroot().set('tests', '909')
        data.write(target)
        expected = self.expected()
        self.refuse(lambda: census.verify_reports(self.product, self.suite(), expected),
                    'incomplete Maven Surefire test case inventory')

    def test_hidden_flaky_execution_refused(self):
        target = self.xml()
        data = ET.parse(target)
        ET.SubElement(data.getroot().find('testcase'), 'flakyFailure')
        data.write(target)
        expected = self.expected()
        self.refuse(lambda: census.verify_reports(self.product, self.suite(), expected),
                    'hidden flaky/rerun failure')

    def test_java_assertion_failure_refused(self):
        target = self.xml()
        data = ET.parse(target)
        ET.SubElement(data.getroot().find('testcase'), 'failure')
        data.write(target)
        expected = self.expected()
        self.refuse(lambda: census.verify_reports(self.product, self.suite(), expected),
                    'failed or skipped JUnit testcase')


if __name__ == '__main__':
    unittest.main(verbosity=2)
