#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Execute custody boundary discriminators inside an explicit caller-owned temp root."""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parent))
import custody

SCRATCH = None


class CustodyBoundaryTest(unittest.TestCase):
    def setUp(self):
        if SCRATCH is None or not SCRATCH.is_dir():
            raise ValueError('explicit caller-owned temp root required; no fallback')
        self.folder = tempfile.TemporaryDirectory(prefix='collection-custody-', dir=SCRATCH)
        self.addCleanup(self.folder.cleanup)
        self.root = Path(self.folder.name)
        self.source = self.root / 'src/A.java'
        self.source.parent.mkdir()
        self.source.write_bytes(b'class A {}\n')
        self.receipt = self.root / 'packet/recipe-receipt.tsv'
        self.receipt.parent.mkdir()
        self.receipt.write_bytes(b'# actual recipe receipt\n')
        self.pins = {'files': {'src/A.java': self.metadata(self.source)},
                     'sourceRoots': ['src'], 'packetRoots': ['packet'],
                     'receiptPaths': ['packet/recipe-receipt.tsv']}
        self.expected = custody.source_inventory(self.root, self.pins)

    @staticmethod
    def metadata(path):
        data = path.read_bytes()
        return {'sha256': hashlib.sha256(data).hexdigest(), 'bytes': len(data),
                'gitBlob': hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()}

    def reports(self, cases, tests=None, skipped=0):
        root = self.root / 'reports'
        root.mkdir(exist_ok=True)
        count = len(cases) if tests is None else tests
        (root / 'TEST-junit.xml').write_text(
            '<testsuite tests="' + str(count) + '" failures="0" errors="0" skipped="' + str(skipped) + '">' +
            ''.join(cases) + '</testsuite>', encoding='utf-8')
        return root

    def test_valid_run_identity(self):
        custody.validate_identity('39000000000-1-' + 'a' * 12, 'a' * 40)

    def test_malformed_or_foreign_run_identity_refuses(self):
        for run_id, sha in [('1-1-' + 'b' * 12, 'a' * 40),
                            ('../1', 'a' * 40), ('0-1-' + 'a' * 12, 'a' * 40),
                            ('1-1-' + 'a' * 12, 'A' * 40),
                            ('1-1-' + 'a' * 12, 'a' * 39)]:
            with self.subTest(run_id=run_id, sha=sha), self.assertRaises(ValueError):
                custody.validate_identity(run_id, sha)

    def test_source_inventory_positive_baseline(self):
        self.assertEqual({'src/A.java', 'packet/recipe-receipt.tsv'}, set(self.expected))
        self.assertEqual(self.expected, custody.source_inventory(self.root, self.pins))

    def test_source_byte_drift_names_exact_owner_without_write(self):
        self.source.write_bytes(b'class A { int drift; }\n')
        changed = self.source.read_bytes()
        receipt = self.receipt.read_bytes()
        with self.assertRaisesRegex(ValueError, 'current source drift: src/A.java'):
            custody.source_inventory(self.root, self.pins)
        self.assertEqual(changed, self.source.read_bytes())
        self.assertEqual(receipt, self.receipt.read_bytes())

    def test_missing_source_refuses_after_positive_baseline(self):
        self.source.unlink()
        with self.assertRaisesRegex(ValueError, 'current source drift: src/A.java'):
            custody.source_inventory(self.root, self.pins)
        self.assertFalse(self.source.exists())

    def test_unlisted_source_is_not_silently_omitted(self):
        extra = self.root / 'src/Undeclared.java'
        extra.write_bytes(b'class Undeclared {}\n')
        with self.assertRaisesRegex(ValueError, 'current source set drift: src'):
            custody.source_inventory(self.root, self.pins)
        self.assertEqual(b'class Undeclared {}\n', extra.read_bytes())
        extra.unlink()
        hidden = self.root / 'src/target/Hidden.java'
        hidden.parent.mkdir()
        hidden.write_bytes(b'class Hidden {}\n')
        with self.assertRaisesRegex(ValueError, 'current source set drift: src'):
            custody.source_inventory(self.root, self.pins)

    def test_git_blob_and_length_metadata_are_enforced(self):
        for field, value in [('gitBlob', '0' * 40), ('bytes', 0)]:
            original = self.pins['files']['src/A.java'][field]
            self.pins['files']['src/A.java'][field] = value
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, 'Git/blob custody drift: src/A.java'):
                custody.source_inventory(self.root, self.pins)
            self.pins['files']['src/A.java'][field] = original
            self.assertEqual(self.expected, custody.source_inventory(self.root, self.pins))

    def test_symlinked_parent_and_source_refuse(self):
        link = self.root / 'linked'
        link.symlink_to(self.root / 'src', target_is_directory=True)
        with self.assertRaisesRegex(ValueError, 'symlinked proof path'):
            custody.safe_path(self.root, 'linked/A.java')
        self.source.unlink()
        self.source.symlink_to(self.receipt)
        with self.assertRaisesRegex(ValueError, 'symlinked proof path: src/A.java'):
            custody.source_inventory(self.root, self.pins)

    def test_unsafe_paths_and_missing_receipts_refuse(self):
        for relative in ('../outside', '/absolute', 'src\\A.java'):
            with self.subTest(relative=relative), self.assertRaises(ValueError):
                custody.safe_path(self.root, relative)
        self.receipt.unlink()
        with self.assertRaisesRegex(ValueError, 'current source set drift: packet'):
            custody.source_inventory(self.root, self.pins)

    def test_compiled_artifact_drift_is_bound(self):
        classes = self.root / 'classes'
        classes.mkdir()
        artifact = classes / 'A.class'
        artifact.write_bytes(b'actual compiled fixture')
        sealed = custody.inventory(self.root, [classes])
        custody.assert_inventory(self.root, sealed, [classes])
        foreign = classes / 'Foreign.class'
        foreign.write_bytes(b'foreign added class')
        with self.assertRaisesRegex(ValueError, 'compiled proof set drift'):
            custody.assert_inventory(self.root, sealed, [classes])
        foreign.unlink()
        custody.assert_inventory(self.root, sealed, [classes])
        artifact.write_bytes(b'foreign compiled fixture')
        with self.assertRaisesRegex(ValueError, 'compiled proof drift: classes/A.class'):
            custody.assert_inventory(self.root, sealed, [classes])
        with self.assertRaisesRegex(ValueError, 'empty inherited'):
            custody.assert_inventory(self.root, {}, [classes])

    def test_zero_exit_native_diagnostics_cannot_grant_acceptance(self):
        custody.reject_native_diagnostics('PASS actual checked JNI corpus\n')
        custody.reject_native_diagnostics('')
        for output in ('WARNING in native method: JNI call made without checking exceptions',
                       'kernel.c:12: runtime error: signed integer overflow'):
            with self.subTest(output=output), self.assertRaisesRegex(RuntimeError, 'JNI/sanitizer diagnostic'):
                custody.reject_native_diagnostics(output)

    def test_cross_run_receipt_refuses(self):
        context = object.__new__(custody.Context)
        context.binding = {'run_id': '2-1-' + 'a' * 12, 'source_commit': 'a' * 40}
        context.recheck = lambda: None
        with self.assertRaisesRegex(ValueError, 'cross-run/source proof'):
            context.check_stamp({'binding': {'run_id': '1-1-' + 'a' * 12}, 'compiled': {'x.class': '0'}})

    def test_duplicate_json_keys_at_any_depth_refuse(self):
        p = self.root / 'proof.json'
        p.write_text('{"binding": {"run": 1, "run": 1}}', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'Duplicate proof metadata key: run'):
            custody.read_json(p)

    def test_actual_junit_methods_positive_baseline(self):
        reports = self.reports(['<testcase classname="C" name="m()"/>'])
        self.assertEqual({'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0},
                         custody.junit_inventory(reports, ['C#m']))

    def test_duplicate_and_unexpected_junit_methods_refuse(self):
        baseline = self.reports(['<testcase classname="C" name="m()"/>'])
        custody.junit_inventory(baseline, ['C#m'])
        for cases in [2 * ['<testcase classname="C" name="m()"/>'],
                      ['<testcase classname="C" name="other()"/>']]:
            with self.subTest(cases=cases):
                self.reports(cases)
                with self.assertRaisesRegex(ValueError, 'actual JUnit method set mismatch'):
                    custody.junit_inventory(baseline, ['C#m'])

    def test_skipped_or_missing_junit_reports_refuse(self):
        reports = self.reports(['<testcase classname="C" name="m()"><skipped/></testcase>'], skipped=1)
        with self.assertRaisesRegex(ValueError, 'nonpassing actual JUnit case'):
            custody.junit_inventory(reports, ['C#m'])
        (reports / 'TEST-junit.xml').unlink()
        with self.assertRaisesRegex(ValueError, 'missing actual JUnit XML'):
            custody.junit_inventory(reports, ['C#m'])

    def test_junit_counter_cannot_replace_actual_method_cases(self):
        reports = self.reports([], tests=1)
        with self.assertRaisesRegex(ValueError, 'actual JUnit method set mismatch'):
            custody.junit_inventory(reports, ['C#m'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--temp', required=True, type=Path)
    parser.add_argument('--reports', required=True, type=Path)
    args = parser.parse_args()
    global SCRATCH
    SCRATCH = args.temp.resolve()
    if args.temp.is_symlink() or not SCRATCH.is_dir():
        raise ValueError('explicit precreated owned temp directory required')
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(CustodyBoundaryTest)
    expected = sorted(case.id().split('.', 1)[1] for case in suite)
    if len(expected) != 17 or len(set(expected)) != 17:
        raise ValueError('custody discriminator method inventory changed')
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    report = {'schema': 'M3_COLLECTIONS_CUSTODY_BOUNDARY_TESTS_V1', 'actual_methods': expected,
              'tests': result.testsRun, 'failures': len(result.failures), 'errors': len(result.errors),
              'skipped': len(result.skipped), 'expected_failures': len(result.expectedFailures),
              'unexpected_successes': len(result.unexpectedSuccesses), 'product_execution': 'NOT_RUN',
              'phase_advance': False}
    args.reports.parent.mkdir(parents=True, exist_ok=True)
    args.reports.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    if (result.testsRun != 17 or not result.wasSuccessful() or result.skipped or
            result.expectedFailures or result.unexpectedSuccesses):
        raise SystemExit(1)
    print('M3_COLLECTIONS_CUSTODY_BOUNDARY_PASS tests=17')


if __name__ == '__main__':
    main()
