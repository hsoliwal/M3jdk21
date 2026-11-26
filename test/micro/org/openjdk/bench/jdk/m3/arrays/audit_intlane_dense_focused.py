#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Admission of *measurements* from focused, source-built M3JDK21 JMH.

The review's two geometry keys are intentionally fixed. A valid CSV is evidence,
not proof of performance nonregression, and never authorizes a runtime release.
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import math
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path

BASE = 'org.openjdk.bench.jdk.m3.arrays.IntLaneSparseRange.originalDenseRangeAddMaximum'
CANDIDATE = 'org.openjdk.bench.jdk.m3.arrays.IntLaneSparseRange.wholeRangeAddMaximum'
KEYS = (('RANDOM', '65535', '9000'), ('RANDOM', '65536', '9000'))
FIELDS = ('Benchmark', 'Mode', 'Threads', 'Samples', 'Score',
          'Score Error (99.9%)', 'Unit', 'Param: distribution',
          'Param: universe', 'Param: updates')
EXPECTED_SAMPLES = 50  # exactly 5 forks x 10 measurement iterations
MAX_CSV_BYTES = 2_000_000


class EvidenceError(ValueError):
    """Malformed or incomplete focused JMH evidence, not a performance result."""


@dataclass(frozen=True)
class Measurement:
    score: float
    error: float
    samples: int


def read_csv(path: Path) -> str:
    raw = path.read_bytes()
    if len(raw) > MAX_CSV_BYTES:
        raise EvidenceError('evidence input exceeds size limit')
    if path.suffix.lower() == '.zip':
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            candidates = [name for name in archive.namelist()
                          if name == 'jmh-result.csv' or name.endswith('/jmh-result.csv')]
            if len(candidates) != 1:
                raise EvidenceError('require exactly one JMH result file in ZIP')
            info = archive.getinfo(candidates[0])
            if info.file_size > MAX_CSV_BYTES:
                raise EvidenceError('oversized JMH result in ZIP')
            raw = archive.read(candidates[0])
    try:
        return raw.decode('utf-8-sig', errors='strict')
    except UnicodeError as exc:
        raise EvidenceError('JMH result is not valid UTF-8') from exc


def finite_number(value: str, name: str, *, zero_allowed: bool = False) -> float:
    try:
        number = float(value)
    except (ValueError, TypeError) as exc:
        raise EvidenceError(f'invalid {name}: {value!r}') from exc
    if not math.isfinite(number) or number < 0 or (not zero_allowed and number == 0):
        raise EvidenceError(f'nonpositive or nonfinite {name}: {value!r}')
    return number


def parse_focused_csv(text: str) -> dict[tuple[str, tuple[str, str, str]], Measurement]:
    reader = csv.DictReader(io.StringIO(text, newline=''), strict=True)
    if reader.fieldnames is None or len(reader.fieldnames) != len(FIELDS) or set(reader.fieldnames) != set(FIELDS):
        raise EvidenceError('JMH CSV header must contain exactly ten unique standard fields')
    measurements: dict[tuple[str, tuple[str, str, str]], Measurement] = {}
    try:
        for count, row in enumerate(reader, 1):
            method = row.get('Benchmark')
            key = (row.get('Param: distribution'), row.get('Param: universe'), row.get('Param: updates'))
            if None in row or method not in (BASE, CANDIDATE) or key not in KEYS:
                raise EvidenceError(f'unknown benchmark, parameter key or extra column at row {count}')
            if row['Mode'] != 'avgt' or row['Unit'] != 'us/op' or row['Threads'] != '1':
                raise EvidenceError(f'unexpected JMH mode, units or thread count at row {count}')
            if row['Samples'] != str(EXPECTED_SAMPLES):
                raise EvidenceError(f'expected exactly {EXPECTED_SAMPLES} forked JMH measurements at row {count}')
            pair = method, key
            if pair in measurements:
                raise EvidenceError(f'duplicate benchmark and parameter pair at row {count}')
            measurements[pair] = Measurement(
                finite_number(row['Score'], 'Score'),
                finite_number(row['Score Error (99.9%)'], '99.9% Score Error', zero_allowed=True),
                EXPECTED_SAMPLES,
            )
    except csv.Error as exc:
        raise EvidenceError('malformed JMH RFC-4180 CSV') from exc
    if len(measurements) != 4 or any((method, key) not in measurements
                                     for method in (BASE, CANDIDATE) for key in KEYS):
        raise EvidenceError(f'incomplete two-case baseline/candidate JMH result: {len(measurements)} rows')
    return measurements


def analyze(measurements: dict[tuple[str, tuple[str, str, str]], Measurement]) -> list[dict]:
    result = []
    for key in KEYS:
        baseline, candidate = measurements[(BASE, key)], measurements[(CANDIDATE, key)]
        a_lower, a_upper = baseline.score - baseline.error, baseline.score + baseline.error
        b_lower, b_upper = candidate.score - candidate.error, candidate.score + candidate.error
        # Interval intersection must check BOTH endpoints. A one-sided check
        # mislabels a large candidate speedup as "overlapping".
        overlap = max(a_lower, b_lower) <= min(a_upper, b_upper)
        result.append({
            'distribution': key[0], 'universe': int(key[1]), 'updates': int(key[2]),
            'baseline_us': baseline.score, 'candidate_us': candidate.score,
            'baseline_error_99_9_us': baseline.error,
            'candidate_error_99_9_us': candidate.error,
            'candidate_to_baseline_ratio': candidate.score / baseline.score,
            'absolute_delta_us': candidate.score - baseline.score,
            'overlapping_99_9_intervals': overlap,
            'samples_each': EXPECTED_SAMPLES,
            'observed_direction': ('candidate_slower' if candidate.score > baseline.score else
                                   'candidate_faster' if candidate.score < baseline.score else 'same_score'),
        })
    return result


def write_report(results: list[dict], destination: Path) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    data = {'schema': 'm3jdk.intlane.focused-jmh.v1', 'status': 'PERFORMANCE_REVIEW_HOLD',
            'sample_count_each': EXPECTED_SAMPLES, 'source_built_cases': results,
            'disclaimer': ('These are JMH score error intervals, not a confidence interval for the ratio. '
                           'Source, JIT, GC, host and fork effects require reviewer analysis.')}
    (destination / 'focused-jmh.json').write_text(json.dumps(data, sort_keys=True, indent=2) + '\n', encoding='utf-8')
    lines = ['# Focused M3JDK21 dense-range JMH evidence', '',
             '**Status: PERFORMANCE_REVIEW_HOLD.** Successful execution is not release acceptance.', '',
             '| Rows | Baseline µs/op | Candidate µs/op | Ratio | 99.9% error bars overlap? |',
             '|---:|---:|---:|---:|---|']
    for case in results:
        lines.append(f"| {case['universe']} | {case['baseline_us']:.6f} | "
                     f"{case['candidate_us']:.6f} | {case['candidate_to_baseline_ratio']:.4f}× | "
                     f"{str(case['overlapping_99_9_intervals']).lower()} |")
    lines += ['', 'These score-error intervals do not establish significance for a ratio.',
              'Compare independent runs, JIT/GC, trial order and additional forks before any source change.', '']
    (destination / 'focused-jmh.md').write_text('\n'.join(lines), encoding='utf-8')


def main(argv: list[str] | None = None) -> int:
    args = argparse.ArgumentParser(description=__doc__)
    args.add_argument('source', type=Path, help='focused JMH CSV or single-result ZIP')
    args.add_argument('--output', type=Path, default=Path('focused-jmh-review'))
    parsed = args.parse_args(argv)
    reviewed = analyze(parse_focused_csv(read_csv(parsed.source)))
    write_report(reviewed, parsed.output)
    for row in reviewed:
        print('M3_INTLANE_FOCUSED_DENSE_JMH_RESULT',
              f"universe={row['universe']}", f"updates={row['updates']}",
              f"baseline_us={row['baseline_us']:.6f}",
              f"candidate_us={row['candidate_us']:.6f}",
              f"ratio={row['candidate_to_baseline_ratio']:.4f}",
              f"samples=50/50", f"overlapping_99_9_errors={str(row['overlapping_99_9_intervals']).lower()}")
    print('M3_INTLANE_FOCUSED_DENSE_JMH_EVIDENCE_PASS pairs=2 forks=5 '
          'measured_iterations_per_fork=10 PERFORMANCE_REVIEW_HOLD')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (EvidenceError, OSError, zipfile.BadZipFile) as exc:
        print('M3_INTLANE_FOCUSED_EVIDENCE_HOLD:', exc, file=sys.stderr)
        sys.exit(3)
