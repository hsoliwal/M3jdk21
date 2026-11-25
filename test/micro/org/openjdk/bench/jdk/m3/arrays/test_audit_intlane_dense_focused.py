#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Hermetic tests for focused JMH evidence, errors, and interval semantics."""
from __future__ import annotations

import csv
import io
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

import audit_intlane_dense_focused as audit


def fixture():
    rows = []
    for key in audit.KEYS:
        for method, score in [(audit.BASE, 100.0), (audit.CANDIDATE, 105.0)]:
            rows.append({'Benchmark': method, 'Mode': 'avgt', 'Threads': '1',
                         'Samples': '50', 'Score': str(score),
                         'Score Error (99.9%)': '10.0', 'Unit': 'us/op',
                         'Param: distribution': key[0], 'Param: universe': key[1],
                         'Param: updates': key[2]})
    return rows


def csv_string(rows):
    buffer = io.StringIO(newline='')
    writer = csv.DictWriter(buffer, fieldnames=audit.FIELDS, lineterminator='\r\n')
    writer.writeheader()
    writer.writerows(rows)
    return buffer.getvalue()


class FocusedEvidenceTest(unittest.TestCase):
    def test_full_two_case_evidence_and_receipt(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / 'jmh-result.csv'
            path.write_bytes(csv_string(fixture()).encode())
            output = Path(d) / 'evidence'
            self.assertEqual(0, audit.main([str(path), '--output', str(output)]))
            result = json.loads((output / 'focused-jmh.json').read_text())
            self.assertEqual('PERFORMANCE_REVIEW_HOLD', result['status'])
            self.assertEqual([65535, 65536], [r['universe'] for r in result['source_built_cases']])
            self.assertEqual([True, True], [r['overlapping_99_9_intervals'] for r in result['source_built_cases']])

    def test_zero_error_disjoint_slow_candidate(self):
        rows = fixture()
        rows[1]['Score'] = '140'
        rows[0]['Score Error (99.9%)'] = rows[1]['Score Error (99.9%)'] = '0'
        cases = audit.analyze(audit.parse_focused_csv(csv_string(rows)))
        self.assertFalse(cases[0]['overlapping_99_9_intervals'])
        self.assertTrue(cases[1]['overlapping_99_9_intervals'])

    def test_two_sided_interval_disjoint_faster_candidate(self):
        rows = fixture()
        rows[1]['Score'] = '60'
        rows[0]['Score Error (99.9%)'] = rows[1]['Score Error (99.9%)'] = '5'
        cases = audit.analyze(audit.parse_focused_csv(csv_string(rows)))
        self.assertFalse(cases[0]['overlapping_99_9_intervals'])
        self.assertEqual('candidate_faster', cases[0]['observed_direction'])

    def test_interval_boundary_touch_is_overlap(self):
        rows = fixture()
        rows[1]['Score'] = '120'
        cases = audit.analyze(audit.parse_focused_csv(csv_string(rows)))
        self.assertTrue(cases[0]['overlapping_99_9_intervals'])

    def test_missing_row_rejected(self):
        with self.assertRaises(audit.EvidenceError): audit.parse_focused_csv(csv_string(fixture()[:-1]))

    def test_duplicate_pair_rejected(self):
        rows = fixture()
        with self.assertRaises(audit.EvidenceError): audit.parse_focused_csv(csv_string(rows + [rows[0]]))

    def test_wrong_geometry_rejected(self):
        rows = fixture()
        rows[0]['Param: universe'] = '65537'
        with self.assertRaises(audit.EvidenceError): audit.parse_focused_csv(csv_string(rows))

    def test_wrong_method_rejected(self):
        rows = fixture()
        rows[0]['Benchmark'] = 'some.other.Method'
        with self.assertRaises(audit.EvidenceError): audit.parse_focused_csv(csv_string(rows))

    def test_missing_fork_sample_rejected(self):
        rows = fixture()
        rows[0]['Samples'] = '49'
        with self.assertRaises(audit.EvidenceError): audit.parse_focused_csv(csv_string(rows))

    def test_invalid_score_nan_rejected(self):
        rows = fixture()
        rows[1]['Score'] = 'NaN'
        with self.assertRaises(audit.EvidenceError): audit.parse_focused_csv(csv_string(rows))

    def test_invalid_error_negative_rejected(self):
        rows = fixture()
        rows[1]['Score Error (99.9%)'] = '-1'
        with self.assertRaises(audit.EvidenceError): audit.parse_focused_csv(csv_string(rows))

    def test_wrong_unit_thread_or_mode_rejected(self):
        for field, bad in [('Unit','ns/op'),('Threads','2'),('Mode','thrpt')]:
            rows = fixture()
            rows[1][field] = bad
            with self.subTest(field=field), self.assertRaises(audit.EvidenceError):
                audit.parse_focused_csv(csv_string(rows))

    def test_extra_header_rejected(self):
        with self.assertRaises(audit.EvidenceError):
            audit.parse_focused_csv(csv_string(fixture()).replace('Unit,','Extra,Unit,'))

    def test_corrupt_utf8_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / 'jmh-result.csv'
            path.write_bytes(b'\xff\xfe')
            with self.assertRaises(audit.EvidenceError): audit.read_csv(path)

    def test_zip_one_result_and_duplicate_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / 'artifact.zip'
            contents = csv_string(fixture())
            with zipfile.ZipFile(path, 'w') as z:
                z.writestr('test-results/micro/jmh-result.csv', contents)
            self.assertEqual(4, len(audit.parse_focused_csv(audit.read_csv(path))))
            with zipfile.ZipFile(path, 'a') as z:
                z.writestr('another/jmh-result.csv', contents)
            with self.assertRaises(audit.EvidenceError): audit.read_csv(path)

    def test_bom_with_quoted_fields(self):
        text = '\ufeff' + csv_string(fixture())
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'jmh-result.csv'
            p.write_bytes(text.encode('utf-8'))
            self.assertEqual(4, len(audit.parse_focused_csv(audit.read_csv(p))))


if __name__ == '__main__':
    unittest.main()
