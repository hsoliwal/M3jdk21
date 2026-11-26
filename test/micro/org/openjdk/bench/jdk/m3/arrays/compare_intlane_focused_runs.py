#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Reconcile two *completed source-built* 50-sample M3JDK21 dense-range JMH runs.

Pinned to the real GitHub run IDs and unmodified raw CSV bytes. This generates
reviewable evidence, NOT a statistical nonregression or product release gate.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import math
import sys
import zipfile
from pathlib import Path

from audit_intlane_dense_focused import (
    BASE, CANDIDATE, KEYS, MAX_CSV_BYTES,
    EvidenceError, analyze, parse_focused_csv,
)

EXPECTED_RUNS = (
    (45593591175, 'f4c035a1d8fc77747f581ad2bf7067c09ec472ef9769e1b878f31fec3b9e30a5',
     'abbcbd2bfb3a9f1e81ea8da04c2905906d3333e7'),
    (45597337526, '891d987673f06710148a30ef2559cab3be4c344349c5d268919cd835c6937504',
     'd9f43afb48987a81ac59d3bb22aea49d681f5996'),
)
PINNED_OWNER_SHA256 = 'd3f78a046a3ff1d1bc27dfebdfd0e82f34de3bc8e9b604fb722c9a728ab77fd8'
MAX_ZIP_BYTES = 8_000_000


def extract_csv(path: Path) -> bytes:
    if path.stat().st_size > MAX_ZIP_BYTES:
        raise EvidenceError('focused JMH archive exceeds 8 MB input budget')
    if path.suffix.lower() != '.zip':
        raw = path.read_bytes()
    else:
        try:
            with zipfile.ZipFile(path) as archive:
                matches = [name for name in archive.namelist()
                           if name == 'jmh-result.csv' or name.endswith('/jmh-result.csv')]
                if len(matches) != 1:
                    raise EvidenceError('exactly one jmh-result.csv required in focused archive')
                member = archive.getinfo(matches[0])
                if member.file_size > MAX_CSV_BYTES:
                    raise EvidenceError('oversized JMH CSV member')
                raw = archive.read(member)
        except zipfile.BadZipFile as exc:
            raise EvidenceError('invalid focused JMH ZIP') from exc
    if len(raw) > MAX_CSV_BYTES:
        raise EvidenceError('oversized focused JMH CSV')
    return raw


def admit_runs(first: bytes, second: bytes, *, verify_hashes: bool = True) -> dict:
    if first == second:
        raise EvidenceError('two independent run inputs must not be identical')
    input_bytes = (first, second)
    measurements = []
    for (run_id, expected_sha, head_sha), raw in zip(EXPECTED_RUNS, input_bytes, strict=True):
        sha = hashlib.sha256(raw).hexdigest()
        if verify_hashes and sha != expected_sha:
            raise EvidenceError(f'run {run_id} CSV source SHA-256 drift: {sha}')
        try:
            text = raw.decode('utf-8-sig', errors='strict')
        except UnicodeError as exc:
            raise EvidenceError(f'run {run_id} is not UTF-8') from exc
        parsed = parse_focused_csv(text)
        results = analyze(parsed)
        measurements.append({
            'run_id': run_id, 'commit': head_sha, 'csv_sha256': sha,
            'cases': results,
        })
    summaries = []
    for key in KEYS:
        per_run = [next(c for c in run['cases'] if c['universe'] == int(key[1]))
                   for run in measurements]
        ratios = [case['candidate_to_baseline_ratio'] for case in per_run]
        # A value-only threshold flags review; it cannot prove ratio significance.
        over_five_percent = [ratio >= 1.05 for ratio in ratios]
        over_twenty_percent = [ratio >= 1.20 and case['absolute_delta_us'] >= 5.0
                               for ratio, case in zip(ratios, per_run, strict=True)]
        summaries.append({
            'distribution': key[0], 'universe': int(key[1]), 'updates': int(key[2]),
            'ratios': ratios, 'over_five_percent_each_run': over_five_percent,
            'both_runs_over_five_percent': all(over_five_percent),
            'both_runs_over_20percent_and_five_microseconds': all(over_twenty_percent),
            'reported_99_9_error_intervals_overlap': [
                c['overlapping_99_9_intervals'] for c in per_run],
            'absolute_baseline_run2_to_run1': (
                per_run[1]['baseline_us'] / per_run[0]['baseline_us']),
        })
    return {
        'schema': 'm3jdk.intlane.focused-jmh-crossrun.v1',
        'status': 'PERFORMANCE_REVIEW_HOLD',
        'source_owner_sha256': PINNED_OWNER_SHA256,
        'run_count': len(measurements),
        'source_built_runs': measurements,
        'case_summaries': summaries,
        'no_repeated_twenty_percent_slowdown_in_focused_runs': not any(
            x['both_runs_over_20percent_and_five_microseconds'] for x in summaries),
        'limits': ('JMH Score Error 99.9% intervals are not confidence intervals '
                   'for ratios. Test order, host variation and JIT/GC remain unisolated. '
                   'Historical 64-pair PERF_HOLD is not lifted by this report.'),
    }


def write_report(data: dict, out: Path) -> None:
    out.mkdir(parents=True, exist_ok=True)
    (out / 'focused-crossrun.json').write_text(
        json.dumps(data, indent=2, sort_keys=True) + '\n', encoding='utf-8')
    with (out / 'focused-crossrun.csv').open('w', newline='', encoding='utf-8') as fh:
        writer = csv.writer(fh, lineterminator='\r\n')
        writer.writerow(['run_id', 'commit', 'csv_sha256', 'distribution', 'universe',
                         'updates', 'baseline_us', 'baseline_error_99_9_us',
                         'candidate_us', 'candidate_error_99_9_us',
                         'candidate_to_baseline_ratio', 'delta_us', 'samples_each',
                         'reported_error_intervals_overlap'])
        for run in data['source_built_runs']:
            for c in run['cases']:
                writer.writerow([run['run_id'], run['commit'], run['csv_sha256'],
                                 c['distribution'], c['universe'], c['updates'],
                                 f"{c['baseline_us']:.6f}", f"{c['baseline_error_99_9_us']:.6f}",
                                 f"{c['candidate_us']:.6f}", f"{c['candidate_error_99_9_us']:.6f}",
                                 f"{c['candidate_to_baseline_ratio']:.9f}",
                                 f"{c['absolute_delta_us']:.6f}", c['samples_each'],
                                 str(c['overlapping_99_9_intervals']).lower()])
    lines = [
        '# Source-built M3JDK21 focused dense-range JMH: two-run review', '',
        '**Status: PERFORMANCE_REVIEW_HOLD — not production clearance.**', '',
        'Two independent source-built JDK/JMH jobs each collected 50 samples per '
        'method for two dense-random geometries.', '',
        '| Universe | Run 45593591175 candidate/original | Run 45597337526 candidate/original | '
        '99.9% score-error bars overlap (first / second)? |',
        '|---:|---:|---:|---|',
    ]
    for c in data['case_summaries']:
        lines.append(f"| {c['universe']:,} | {c['ratios'][0]:.4f}× | "
                     f"{c['ratios'][1]:.4f}× | "
                     f"{str(c['reported_99_9_error_intervals_overlap'][0]).lower()} / "
                     f"{str(c['reported_99_9_error_intervals_overlap'][1]).lower()} |")
    lines.extend(['', 'Neither focused run repeats the original 20%+ slowdown. '
                  'One focused run shows a 7–9% observed slowdown; the other '
                  'is near parity. Absolute baseline times differ by ~33%, '
                  'so do not compare absolute timings across jobs.', '',
                  '**These error bars are not a significance test for the ratio.** '
                  'The historical 64-pair JMH review remains a separate HOLD '
                  'until JFR/JIT/GC and execution-order effects are evaluated.', ''])
    (out / 'focused-crossrun.md').write_text('\n'.join(lines), encoding='utf-8')


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('first', type=Path, help='raw run 45593591175 ZIP or CSV')
    parser.add_argument('second', type=Path, help='raw run 45597337526 ZIP or CSV')
    parser.add_argument('--output', type=Path, default=Path('focused-crossrun-report'))
    args = parser.parse_args(argv)
    try:
        result = admit_runs(extract_csv(args.first), extract_csv(args.second))
        write_report(result, args.output)
    except (EvidenceError, OSError, ValueError, KeyError) as exc:
        print('FOCUSED_JMH_CROSSRUN_HOLD:', exc, file=sys.stderr)
        return 3
    for c in result['case_summaries']:
        print(f"FOCUSED_CROSSRUN_REVIEW universe={c['universe']} "
              f"run1_ratio={c['ratios'][0]:.4f} run2_ratio={c['ratios'][1]:.4f} "
              f"reported_error_overlap={c['reported_99_9_error_intervals_overlap']}")
    print('FOCUSED_JMH_CROSSRUN_EVIDENCE_PASS runs=2 cases=2 '
          'observed_20pct_repeat=no PERFORMANCE_REVIEW_HOLD')
    return 0


if __name__ == '__main__':
    sys.exit(main())
