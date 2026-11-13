#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""PINNED_EDITS v1: the two follow-up edits of qualification/rank-stack-followup-20261107 (after #2693).

Each edit names its file, the pre-image git blob (the #2693 head blob), the post-image blob, and exact old -> new text
hunks that must each occur exactly once in the pre-image. apply: refuses unless the file is the pre-image (or already
the post-image), applies the hunks, refuses unless the result hashes to the post-image. --check: every file must be its
post-image. CPU only; usage: python -I pinned_edits.py [--check] [--repo <git dir>]
"""
import argparse
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
RECIPE = 'm3/collections/qualification/rank-stack-followup-20261107/pinned_edits.py v1'
OWNER = 'm3/collections/src/main/java/jdk/m3/collections/M3LongRankBatchNative.java'
CONTRACT = 'm3/collections/src/test/java/jdk/m3/collections/M3RankBatchIntoContract.java'


def guard(condition, flag, call_lines, tail, indent='        '):
    old = f'{indent}if ({condition}) {{\n' + ''.join(f'{indent}    {line}\n' for line in call_lines) + tail
    new = (f'{indent}if ({condition.replace("AVAILABLE && ", f"AVAILABLE && !{flag} && ", 1)}) {{\n'
           f'{indent}    try {{\n'
           + ''.join(f'{indent}        {line}\n' for line in call_lines)
           + tail.replace(f'{indent}    return', f'{indent}        return', 1).replace(f'{indent}}}', f'{indent}    }} catch (UnsatisfiedLinkError olderNativeLibrary) {{\n'
                                                                              f'{indent}        {flag} = true;\n'
                                                                              f'{indent}    }}\n{indent}}}', 1))
    return old, new


FLAGS_OLD = '    private static volatile boolean RUN_STATISTICS_ABI_MISSING;\n'
FLAGS_NEW = (FLAGS_OLD
             + '    //FLLDCIM: MRL - follow-up of #2693: per-export ABI flags for the three restored natives whose callers had none,\n'
             + '    // so an older libm3rankbatch (master 7-export build) falls back to Java like every other restored kernel;\n'
             + f'    // the hot path reads one volatile boolean, no allocation; recipe {RECIPE}\n'
             + '    private static volatile boolean BOUND_BATCH_ABI_MISSING;\n'
             + '    private static volatile boolean RANGE_COUNT_ABI_MISSING;\n'
             + '    private static volatile boolean INTERSECTION_COUNT_ABI_MISSING;\n')

LOWER = guard('AVAILABLE && length >= NATIVE_MIN_BATCH', 'BOUND_BATCH_ABI_MISSING',
              ['boundBatchInto0(sorted, queries, queryFrom, length, output, outputFrom, 0);'],
              '            return;\n        }\n        for (int i = 0; i < length; i++) {\n'
              '            output[outputFrom + i] = bound(sorted, queries[queryFrom + i], false);\n')
BOUND = guard('AVAILABLE && length >= NATIVE_MIN_BATCH', 'BOUND_BATCH_ABI_MISSING',
              ['boundBatchInto0(sorted, queries, queryFrom, length, output, outputFrom, mode);'],
              '            return;\n        }\n        for (int i = 0; i < length; i++) {\n'
              '            long target = queries[queryFrom + i];\n')
RANGE = guard('AVAILABLE && length >= NATIVE_MIN_BATCH', 'RANGE_COUNT_ABI_MISSING',
              ['rangeCountInto0(sorted, fromInclusive, toExclusive,', '        queryFrom, length, output, outputFrom);'],
              '            return;\n        }\n')
COUNT = guard('AVAILABLE && Math.min(left.length, right.length) >= NATIVE_MIN_INTERSECTION',
              'INTERSECTION_COUNT_ABI_MISSING', ['return intersectionCount0(left, right);'], '        }\n')
COUNT = (COUNT[0], COUNT[1].replace('!INTERSECTION_COUNT_ABI_MISSING && Math.min',
                                    '!INTERSECTION_COUNT_ABI_MISSING\n                && Math.min', 1))

PROBE_OLD = (
    '        int[] output = {31, 32, 33};\n'
    '        for (int[] input : new int[][] {{0, 1, 0, 0}, {-1, 1, 0, 2}, {0, 4, 0, 2}}) {\n')
PROBE_NEW = (
    '        int[] output = {31, 32, 33};\n'
    '        //FLLDCIM: MRL - follow-up of #2693: efa61222bcc made mode 0 the in-place lower-bound mode of boundBatchInto0\n'
    '        // (native check: mode != 0 && mode != M3_RANK_UPPER && mode != M3_RANK_COUNT), so mode 3 is the invalid mode\n'
    f'        // probed here and mode 0 is probed as valid below; recipe {RECIPE}\n'
    '        for (int[] input : new int[][] {{0, 1, 0, 3}, {-1, 1, 0, 2}, {0, 4, 0, 2}}) {\n')
VALID_OLD = (
    '            same(new int[]{31, 32, 33}, output, "native validation untouched output");\n'
    '        }\n'
    '    }\n')
VALID_NEW = (
    '            same(new int[]{31, 32, 33}, output, "native validation untouched output");\n'
    '        }\n'
    '        rank.invoke(null, new long[]{3, 4}, new long[]{3, 4}, 0, 2, output, 0, 0);\n'
    '        same(new int[]{0, 1, 33}, output, "native mode 0 writes lower bounds (efa61222bcc)");\n'
    '    }\n')

EDITS = [
    {'path': OWNER, 'pre': 'a32f003a68076db0aa9079752e1669362aaf38c3', 'post': '2b34720f00d04ad05d8a164084dbda6af0773def',
     'hunks': [(FLAGS_OLD, FLAGS_NEW), LOWER, BOUND, RANGE, COUNT]},
    {'path': CONTRACT, 'pre': '284248c40cc245ff83091efc3e2ecbf69238a6ca', 'post': 'e8a146d1429d956b04f52002fb893cdab4a78042',
     'hunks': [(PROBE_OLD, PROBE_NEW), (VALID_OLD, VALID_NEW)]},
]


def blob_id(repo, data):
    return subprocess.run(['git', '-C', str(repo), 'hash-object', '--stdin'], input=data, check=True,
                          capture_output=True).stdout.decode().strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--repo', default=str(HERE.parents[3]))
    args = parser.parse_args()
    repo = Path(args.repo)
    changed, report = 0, []
    for edit in EDITS:
        target = repo / edit['path']
        data = target.read_bytes()
        current = blob_id(repo, data)
        if current == edit['post']:
            report.append(f"{Path(edit['path']).name}=post")
            continue
        if args.check or current != edit['pre']:
            sys.exit(f"REFUSE {edit['path']}: blob {current} is neither pre {edit['pre']} nor post {edit['post']}")
        text = data.decode()
        for number, (old, new) in enumerate(edit['hunks'], 1):
            if text.count(old) != 1:
                sys.exit(f"REFUSE {edit['path']} hunk {number}: old text occurs {text.count(old)} times")
            text = text.replace(old, new, 1)
        result = blob_id(repo, text.encode())
        if result != edit['post']:
            sys.exit(f"REFUSE {edit['path']}: result blob {result} != post {edit['post']}")
        with open(target, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
        changed += 1
        report.append(f"{Path(edit['path']).name}={result[:12]}")
    print(f"PINNED_EDITS v1 edits={len(EDITS)} {'check' if args.check else 'apply'} changed={changed} {' '.join(report)}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
