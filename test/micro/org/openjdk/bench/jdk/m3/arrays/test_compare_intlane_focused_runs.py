#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Hermetic negative/positive tests for fixed-source focused JMH cross-run audit."""
from __future__ import annotations

import csv
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

import compare_intlane_focused_runs as audit
from audit_intlane_dense_focused import BASE, CANDIDATE, KEYS, FIELDS, EvidenceError


def rows(candidate_multiplier: float = 1.01, baseline_multiplier: float = 1.0):
    items = []
    for key in KEYS:
        for method, score in [(BASE, 100 * baseline_multiplier),
                              (CANDIDATE, 100 * baseline_multiplier * candidate_multiplier)]:
            items.append({'Benchmark': method, 'Mode': 'avgt', 'Threads': '1',
                          'Samples': '50', 'Score': str(score),
                          'Score Error (99.9%)': '1.0', 'Unit': 'us/op',
                          'Param: distribution': key[0], 'Param: universe': key[1],
                          'Param: updates': key[2]})
    return items


def encoded(items):
    s = io.StringIO(newline='')
    w = csv.DictWriter(s, fieldnames=FIELDS, lineterminator='\r\n')
    w.writeheader()
    w.writerows(items)
    return s.getvalue().encode('utf-8')


class CrossRunTest(unittest.TestCase):
    def setUp(self):
        self.one = encoded(rows(1.01))
        self.two = encoded(rows(1.07, 0.70))

    def test_two_valid_source_built_runs_remain_review_hold(self):
        d = audit.admit_runs(self.one, self.two, verify_hashes=False)
        self.assertEqual('PERFORMANCE_REVIEW_HOLD', d['status'])
        self.assertEqual(2, d['run_count'])
        self.assertEqual(2, len(d['case_summaries']))
        self.assertTrue(d['no_repeated_twenty_percent_slowdown_in_focused_runs'])
        self.assertTrue(all(not c['both_runs_over_five_percent'] for c in d['case_summaries']))
        with tempfile.TemporaryDirectory() as root:
            path = Path(root)
            audit.write_report(d, path)
            with (path/'focused-crossrun.csv').open(newline='',encoding='utf-8') as stream:
                self.assertEqual(4, len(list(csv.DictReader(stream))))
            self.assertIn('PERFORMANCE_REVIEW_HOLD', (path/'focused-crossrun.md').read_text())
            self.assertEqual(2, json.loads((path/'focused-crossrun.json').read_text())['run_count'])

    def test_repeated_big_regression_detected_for_review(self):
        d = audit.admit_runs(encoded(rows(1.35)), encoded(rows(1.40, 0.91)),verify_hashes=False)
        self.assertFalse(d['no_repeated_twenty_percent_slowdown_in_focused_runs'])
        self.assertTrue(all(c['both_runs_over_20percent_and_five_microseconds'] for c in d['case_summaries']))
        self.assertEqual('PERFORMANCE_REVIEW_HOLD', d['status'])

    def test_unpinned_source_rejected(self):
        with self.assertRaisesRegex(EvidenceError, 'SHA-256 drift'):
            audit.admit_runs(self.one, self.two)

    def test_repeated_identical_bytes_rejected(self):
        with self.assertRaisesRegex(EvidenceError, 'independent'):
            audit.admit_runs(self.one, self.one, verify_hashes=False)

    def test_missing_parameter_key_rejected(self):
        x = rows(); x.pop()
        with self.assertRaises(EvidenceError):
            audit.admit_runs(encoded(x), self.two, verify_hashes=False)

    def test_duplicate_parameter_key_rejected(self):
        x = rows(); x.append(dict(x[0]))
        with self.assertRaises(EvidenceError):
            audit.admit_runs(encoded(x), self.two, verify_hashes=False)

    def test_bad_samples_rejected(self):
        x = rows(); x[0]['Samples'] = '49'
        with self.assertRaises(EvidenceError):
            audit.admit_runs(encoded(x), self.two, verify_hashes=False)

    def test_wrong_mode_or_units_rejected(self):
        x = rows(); x[0]['Unit'] = 'ns/op'
        with self.assertRaises(EvidenceError):
            audit.admit_runs(encoded(x), self.two, verify_hashes=False)

    def test_nonfinite_score_rejected(self):
        x = rows(); x[1]['Score'] = 'NaN'
        with self.assertRaises(EvidenceError):
            audit.admit_runs(encoded(x), self.two, verify_hashes=False)

    def test_zip_requires_one_jmh_csv(self):
        with tempfile.TemporaryDirectory() as root:
            path=Path(root)/'artifact.zip'
            with zipfile.ZipFile(path,'w') as z:
                z.writestr('foo/jmh-result.csv', self.one)
                z.writestr('bar/jmh-result.csv', self.two)
            with self.assertRaisesRegex(EvidenceError,'exactly one'):
                audit.extract_csv(path)

    def test_single_archive_csv_source_reads_correctly(self):
        with tempfile.TemporaryDirectory() as root:
            path=Path(root)/'artifact.zip'
            with zipfile.ZipFile(path,'w') as z:
                z.writestr('tests/jmh-result.csv', self.one)
            self.assertEqual(self.one, audit.extract_csv(path))

    def test_zip_bomb_preflight_rejected(self):
        with tempfile.TemporaryDirectory() as root:
            path=Path(root)/'artifact.zip'
            with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED) as z:
                z.writestr('tests/jmh-result.csv', b'A'*(audit.MAX_CSV_BYTES+1))
            with self.assertRaisesRegex(EvidenceError,'oversized'):
                audit.extract_csv(path)

    def test_invalid_utf8_rejected(self):
        with self.assertRaisesRegex(EvidenceError, 'not UTF-8'):
            audit.admit_runs(b'\xff\xfe',self.two,verify_hashes=False)

    def test_raw_csv_file_supported(self):
        with tempfile.TemporaryDirectory() as root:
            path=Path(root)/'source.csv';path.write_bytes(self.one)
            self.assertEqual(self.one,audit.extract_csv(path))

if __name__ == '__main__':
    unittest.main()
