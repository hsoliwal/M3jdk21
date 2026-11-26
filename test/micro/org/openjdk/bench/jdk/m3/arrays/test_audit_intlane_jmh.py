#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Dependency-free, hermetic mutation tests for the JMH CSV admission gate."""
import csv
import io
import math
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import audit_intlane_jmh as audit


def example_csv() -> str:
    stream = io.StringIO(newline='')
    writer = csv.DictWriter(stream, fieldnames=audit.FIELDS)
    writer.writeheader()
    for bench in (audit.BASE, audit.CANDIDATE):
        for d, u, n in sorted(audit.EXPECTED):
            writer.writerow({
                'Benchmark': bench, 'Mode': 'avgt', 'Threads': '1', 'Samples': '3',
                'Score': '100.0' if bench == audit.BASE else '90.0',
                'Score Error (99.9%)': '1.0', 'Unit': 'us/op',
                'Param: distribution': d, 'Param: universe': u, 'Param: updates': n,
            })
    return stream.getvalue()


def modify_rows(text: str, modify):
    reader = csv.DictReader(io.StringIO(text))
    records = list(reader)
    modify(records)
    result = io.StringIO(newline='')
    writer = csv.DictWriter(result, fieldnames=reader.fieldnames)
    writer.writeheader()
    writer.writerows(records)
    return result.getvalue()


class TestIntLaneJmhAudit(unittest.TestCase):
    def setUp(self):
        self.csv = example_csv()

    def test_complete_matrix_passes_and_emits_real_reports(self):
        run = audit.parse_jmh(self.csv)
        cases, bad = audit.calculate([run], 1.20, 5.0)
        self.assertEqual(64, len(cases))
        self.assertEqual([], bad)
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source = root / 'one.csv'
            source.write_text(self.csv, encoding='utf-8')
            second = root / 'two.csv'
            def independently_measured(rows):
                rows[0]['Score'] = '100.1'
            second.write_text(modify_rows(self.csv, independently_measured), encoding='utf-8')
            self.assertEqual(0, audit.main([
                str(source), str(second), '--output', str(root / 'report'), '--enforce']))
            self.assertTrue((root / 'report' / 'paired.csv').is_file())
            self.assertIn('NO_REPEATED_THRESHOLD_REGRESSION', (root / 'report' / 'audit.json').read_text())

    def test_repeated_regression_only_when_both_runs_cross_thresholds(self):
        key = ('RANDOM', '65536', '9000')
        def add_bad(rows):
            for row in rows:
                if row['Benchmark'] == audit.CANDIDATE and (
                    row['Param: distribution'], row['Param: universe'], row['Param: updates']
                ) == key:
                    row['Score'] = '133.4'
        broken = audit.parse_jmh(modify_rows(self.csv, add_bad))
        clean = audit.parse_jmh(self.csv)
        cases, bad = audit.calculate([broken, broken], 1.20, 5.0)
        self.assertEqual([key], [(x['distribution'], x['universe'], x['updates']) for x in bad])
        self.assertEqual([], audit.calculate([broken, clean], 1.20, 5.0)[1])

    def test_mutations_refused(self):
        cases = [
            ('missing-row', lambda rows: rows.pop()),
            ('duplicate-row', lambda rows: rows.append(rows[0].copy())),
            ('nan-score', lambda rows: rows[0].__setitem__('Score', 'nan')),
            ('negative-score', lambda rows: rows[0].__setitem__('Score', '-2')),
            ('zero-score', lambda rows: rows[0].__setitem__('Score', '0')),
            ('bad-error', lambda rows: rows[0].__setitem__('Score Error (99.9%)', 'inf')),
            ('bad-mode', lambda rows: rows[0].__setitem__('Mode', 'thrpt')),
            ('bad-unit', lambda rows: rows[0].__setitem__('Unit', 'ns/op')),
            ('bad-threads', lambda rows: rows[0].__setitem__('Threads', '2')),
            ('bad-count', lambda rows: rows[0].__setitem__('Samples', '0')),
            ('bad-method', lambda rows: rows[0].__setitem__('Benchmark', 'arbitrary')),
            ('bad-universe', lambda rows: rows[0].__setitem__('Param: universe', '123456')),
            ('bad-distribution', lambda rows: rows[0].__setitem__('Param: distribution', 'OTHER')),
            ('bad-updates', lambda rows: rows[0].__setitem__('Param: updates', '1337')),
        ]
        for name, edit in cases:
            with self.subTest(name=name):
                with self.assertRaises(audit.JmhAdmissionError):
                    audit.parse_jmh(modify_rows(self.csv, edit))

    def test_duplicate_header_refused(self):
        damaged = self.csv.replace('Benchmark,', 'Benchmark,Benchmark,', 1)
        with self.assertRaises(audit.JmhAdmissionError):
            audit.parse_jmh(damaged)

    def test_malformed_csv_refused(self):
        damaged = self.csv + '\n"broken,unclosed'
        with self.assertRaises(audit.JmhAdmissionError):
            audit.parse_jmh(damaged)

    def test_single_clean_run_requires_replication_in_enforced_mode(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source = root / 'one.csv'
            source.write_text(self.csv, encoding='utf-8')
            out = root / 'review'
            self.assertEqual(2, audit.main([str(source), '--output', str(out), '--enforce']))
            summary = (out / 'audit.json').read_text(encoding='utf-8')
            self.assertIn('INSUFFICIENT_REPLICATION', summary)
            self.assertIn('"minimum_independent_runs": 2', summary)
            self.assertIn('"repeated_observational_regressions": 0', summary)

    def test_single_slow_run_not_misclassified_as_repeatable(self):
        key = ('RANDOM', '65536', '9000')
        def increase(rows):
            for row in rows:
                if row['Benchmark'] == audit.CANDIDATE and (
                    row['Param: distribution'], row['Param: universe'], row['Param: updates']
                ) == key:
                    row['Score'] = '133.4'
        measured = audit.parse_jmh(modify_rows(self.csv, increase))
        entries, flagged = audit.calculate([measured], 1.20, 5.0)
        self.assertEqual([], flagged)
        target = next(x for x in entries if (
            x['distribution'], x['universe'], x['updates']) == key)
        self.assertTrue(target['runs'][0]['above_observation_gate'])
        self.assertFalse(target['repeatable_regression'])
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source = root / 'one.csv'
            source.write_text(modify_rows(self.csv, increase), encoding='utf-8')
            out = root / 'review'
            self.assertEqual(2, audit.main([str(source), '--output', str(out), '--enforce']))
            summary = (out / 'audit.json').read_text(encoding='utf-8')
            self.assertIn('INSUFFICIENT_REPLICATION', summary)
            self.assertIn('"single_run_observational_warnings": 1', summary)

    def test_nonindependent_duplicated_results_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            f = root / 'one.csv'
            f.write_text(self.csv)
            with self.assertRaises(audit.JmhAdmissionError):
                audit.main([str(f), str(f), '--output', str(root / 'report')])

    def test_enforcement_returns_hold_not_failure_of_correctness(self):
        def extra(rows):
            for row in rows:
                if row['Benchmark'] == audit.CANDIDATE and row['Param: distribution'] == 'RANDOM':
                    row['Score'] = '130'
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            f = root / 'jmh-result.csv'
            f.write_text(modify_rows(self.csv, extra))
            second = root / 'another-jmh-result.csv'
            def second_host(rows):
                extra(rows)
                rows[0]['Score'] = '100.1'
            second.write_text(modify_rows(self.csv, second_host))
            self.assertEqual(2, audit.main([
                str(f), str(second), '--output', str(root / 'review'), '--enforce']))
            self.assertIn('PERF_HOLD', (root / 'review' / 'AUDIT.md').read_text())


if __name__ == '__main__':
    unittest.main()
