#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed, standard-library-only paired JMH performance admission.

Reads real JMH CSV or the original GitHub Actions evidence ZIP. Compares the
64 two-method cases per independent run, produces auditable CSV/JSON/Markdown,
and labels significant repeated *observational* slowdowns as PERF_HOLD.

Never treats a green JMH completion as evidence of universal speedup. An
observed slowdown is an admission blocker for review, not a diagnosis of cause.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import math
import statistics
import sys
import zipfile
from dataclasses import dataclass
from itertools import product
from pathlib import Path

PREFIX = 'org.openjdk.bench.jdk.m3.arrays.IntLaneSparseRange.'
BASE = PREFIX + 'originalDenseRangeAddMaximum'
CANDIDATE = PREFIX + 'wholeRangeAddMaximum'
DISTRIBUTIONS = ('RANDOM', 'COLLIDING', 'TAIL_NEGATIVE', 'FULL_RANGE')
UNIVERSES = ('65535', '65536', '65537', '2000000')
UPDATES = ('0', '16', '256', '9000')
EXPECTED = set(product(DISTRIBUTIONS, UNIVERSES, UPDATES))
FIELDS = ('Benchmark', 'Mode', 'Threads', 'Samples', 'Score',
          'Score Error (99.9%)', 'Unit', 'Param: distribution',
          'Param: universe', 'Param: updates')


class JmhAdmissionError(ValueError):
    pass


@dataclass(frozen=True)
class Measurement:
    mean: float
    error: float
    samples: int


def load_csv(path: Path) -> tuple[str, str]:
    data = path.read_bytes()
    if path.suffix.lower() == '.zip':
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            hits = [n for n in z.namelist() if n.endswith('/jmh-result.csv')]
            if len(hits) != 1:
                raise JmhAdmissionError(f'{path.name}: require exactly one JMH CSV, found {len(hits)}')
            info = z.getinfo(hits[0])
            if info.file_size > 2_000_000:
                raise JmhAdmissionError(f'{path.name}: oversized JMH CSV')
            data = z.read(hits[0])
    if len(data) > 2_000_000:
        raise JmhAdmissionError(f'{path.name}: oversized JMH CSV')
    try:
        return data.decode('utf-8-sig', errors='strict'), hashlib.sha256(data).hexdigest()
    except UnicodeError as exc:
        raise JmhAdmissionError(f'{path.name}: invalid UTF-8') from exc


def positive_float(value: str, label: str, *, allow_zero: bool = False) -> float:
    try:
        number = float(value)
    except (TypeError, ValueError) as exc:
        raise JmhAdmissionError(f'invalid {label}: {value!r}') from exc
    if not math.isfinite(number) or number < 0 or (not allow_zero and number == 0):
        raise JmhAdmissionError(f'non-finite/non-positive {label}: {value!r}')
    return number


def parse_jmh(text: str) -> dict[str, dict[tuple[str, str, str], Measurement]]:
    reader = csv.DictReader(io.StringIO(text, newline=''), strict=True)
    fieldnames = reader.fieldnames or []
    if len(fieldnames) != len(set(fieldnames)) or not set(FIELDS).issubset(fieldnames):
        raise JmhAdmissionError(f'JMH header missing or duplicated columns: {fieldnames}')
    result = {BASE: {}, CANDIDATE: {}}
    count = 0
    try:
        for row in reader:
            count += 1
            if None in row or row.get('Benchmark') not in result:
                raise JmhAdmissionError(f'unknown benchmark, extra columns, or damaged CSV row {count}')
            if row['Mode'] != 'avgt' or row['Unit'] != 'us/op' or row['Threads'] != '1':
                raise JmhAdmissionError(f'unsupported JMH mode/unit/threads at row {count}')
            try:
                samples = int(row['Samples'])
            except (ValueError, TypeError) as exc:
                raise JmhAdmissionError(f'invalid Samples at row {count}') from exc
            if samples < 1 or str(samples) != row['Samples']:
                raise JmhAdmissionError(f'non-positive/invalid Samples at row {count}')
            key = (row['Param: distribution'], row['Param: universe'], row['Param: updates'])
            if key not in EXPECTED:
                raise JmhAdmissionError(f'unexpected JMH parameters at row {count}: {key}')
            by_key = result[row['Benchmark']]
            if key in by_key:
                raise JmhAdmissionError(f'duplicate benchmark/parameter pair at row {count}: {key}')
            by_key[key] = Measurement(
                positive_float(row['Score'], 'Score'),
                positive_float(row['Score Error (99.9%)'], '99.9% error', allow_zero=True),
                samples,
            )
    except csv.Error as exc:
        raise JmhAdmissionError('malformed RFC-4180 CSV') from exc
    if count != 128 or any(set(data) != EXPECTED for data in result.values()):
        raise JmhAdmissionError(f'incomplete matched 64+64 JMH matrix: {count} rows'
                                f' baseline={len(result[BASE])} candidate={len(result[CANDIDATE])}')
    return result


def calculate(runs: list[dict[str, dict[tuple[str, str, str], Measurement]]],
              threshold_ratio: float, threshold_delta_us: float):
    entries = []
    for distribution, universe, updates in sorted(EXPECTED):
        key = distribution, universe, updates
        measures = []
        for run in runs:
            a = run[BASE][key]
            b = run[CANDIDATE][key]
            ratio = b.mean / a.mean
            delta = b.mean - a.mean
            measures.append({
                'baseline_us': a.mean, 'candidate_us': b.mean,
                'ratio': ratio, 'delta_us': delta,
                'baseline_error_us': a.error, 'candidate_error_us': b.error,
                'confidence_interval_nonoverlap': b.mean - b.error > a.mean + a.error,
                'above_observation_gate': ratio >= threshold_ratio and delta >= threshold_delta_us,
            })
        entries.append({
            'distribution': distribution, 'universe': universe, 'updates': updates,
            'runs': measures,
            'repeatable_regression': all(m['above_observation_gate'] for m in measures),
        })
    flagged = [r for r in entries if r['repeatable_regression']]
    return entries, flagged


def output_report(out: Path, files: list[Path], digests: list[str],
                  entries: list[dict], flagged: list[dict], threshold_ratio: float,
                  threshold_delta_us: float) -> None:
    out.mkdir(parents=True, exist_ok=True)
    summary = {
        'schema': 'm3jdk.intlane.jmh.paired.v1',
        'input_files': [p.name for p in files], 'csv_sha256': digests,
        'independent_runs': len(files), 'pairs_per_run': len(entries),
        'threshold_ratio': threshold_ratio, 'threshold_delta_us': threshold_delta_us,
        'repeated_observational_regressions': len(flagged),
        'status': 'PERF_HOLD' if flagged else 'NO_REPEATED_THRESHOLD_REGRESSION',
        'cases': entries,
    }
    (out / 'audit.json').write_text(json.dumps(summary, indent=2, sort_keys=True) + '\n', encoding='utf-8')
    with (out / 'paired.csv').open('w', newline='', encoding='utf-8') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(('distribution', 'universe', 'updates', 'run', 'baseline_us',
                         'candidate_us', 'ratio', 'delta_us', 'baseline_error_us',
                         'candidate_error_us', 'nonoverlapping_99_9_interval',
                         'above_observation_gate', 'repeated_regression'))
        for case in entries:
            for i, run in enumerate(case['runs'], 1):
                writer.writerow((case['distribution'], case['universe'], case['updates'],
                                 i, f"{run['baseline_us']:.6f}", f"{run['candidate_us']:.6f}",
                                 f"{run['ratio']:.6f}", f"{run['delta_us']:.6f}",
                                 f"{run['baseline_error_us']:.6f}",
                                 f"{run['candidate_error_us']:.6f}",
                                 str(run['confidence_interval_nonoverlap']).lower(),
                                 str(run['above_observation_gate']).lower(),
                                 str(case['repeatable_regression']).lower()))
    lines = ['# M3JDK21 adaptive sparse-range JMH evidence audit', '',
             f"**Status:** {summary['status']} — {len(files)} real JMH runs, 64 baseline/candidate pairs each.",
             '', 'Original-input CSV SHA-256:', '']
    lines += [f'- `{p.name}`: `{d}`' for p,d in zip(files,digests)]
    lines += ['', f'Observation gate: candidate/baseline ≥ {threshold_ratio:.2f} and '
              f'candidate–baseline ≥ {threshold_delta_us:.1f} µs/op in **every independent run**.',
              'This is a conservative review gate, **not** proof of statistical significance.',
              'The reported JMH 99.9% score errors and fork/sample count must be reviewed.', '',
              '| Distribution | Rows | Updates | Run ratios | Absolute slowdowns (µs/op) |',
              '|---|---:|---:|---:|---:|']
    for case in flagged:
        ratios = ', '.join(f"{r['ratio']:.3f}×" for r in case['runs'])
        deltas = ', '.join(f"{r['delta_us']:.2f}" for r in case['runs'])
        lines.append(f"| {case['distribution']} | {case['universe']} | {case['updates']} | {ratios} | {deltas} |")
    lines += ['', 'Detailed 128×N original/candidate measurement rows are in `paired.csv`; '
              'the machine-readable source is `audit.json`.',
              '', '**Interpretation:** the retained dense path has measured regressions near '
              'the dispatch boundary; diagnose code layout, JIT compilation, memory allocation '
              'and benchmark order. Do not infer a new algorithm bug or a universal speedup.',
              '', '**Production:** this auditor adds no new Java runtime method or native symbol. '
              'Do not weaken source seals or rewrite the source candidate without a new '
              'recipe/tests/benchmark-provenance gate.', '']
    (out / 'AUDIT.md').write_text('\n'.join(lines), encoding='utf-8')


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('runs', metavar='JMH_CSV_OR_ZIP', type=Path, nargs='+')
    ap.add_argument('--output', type=Path, default=Path('jmh-audit'))
    ap.add_argument('--ratio', type=float, default=1.20)
    ap.add_argument('--delta-us', type=float, default=5.0)
    ap.add_argument('--enforce', action='store_true', help='exit 2 on repeated/observed threshold regressions')
    args = ap.parse_args(argv)
    if not math.isfinite(args.ratio) or args.ratio < 1 or not math.isfinite(args.delta_us) or args.delta_us < 0:
        raise JmhAdmissionError('invalid admission threshold')
    if len(args.runs) > 8:
        raise JmhAdmissionError('too many run files')
    runs, hashes = [], []
    for path in args.runs:
        data, sha = load_csv(path)
        runs.append(parse_jmh(data))
        hashes.append(sha)
    if len(hashes) != len(set(hashes)):
        raise JmhAdmissionError('duplicate JMH CSV content is not an independent run')
    entries, flagged = calculate(runs, args.ratio, args.delta_us)
    output_report(args.output, args.runs, hashes, entries, flagged, args.ratio, args.delta_us)
    medians = [statistics.median(r['ratio'] for r in (case['runs'])) for case in entries]
    print(f"M3_INTLANE_JMH_AUDIT {'PERF_HOLD' if flagged else 'PASS'} runs={len(runs)} "
          f'paired_keys=64 flagged={len(flagged)} median_case_ratio={statistics.median(medians):.4f}')
    for case in flagged:
        print('REGRESSION', case['distribution'], case['universe'], case['updates'],
              'ratios=' + ','.join(f"{r['ratio']:.4f}" for r in case['runs']))
    return 2 if args.enforce and flagged else 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (JmhAdmissionError, OSError, zipfile.BadZipFile) as error:
        print('JMH_AUDIT_HOLD:', error, file=sys.stderr)
        sys.exit(3)
