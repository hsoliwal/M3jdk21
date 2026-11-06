#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Strict standalone Java 21 proof for source-sealed wave-4 M3 CPU product files.

This is an independent product test runner, not a replacement OpenRewrite engine
or installed M3JDK21 JMOD proof. Never modifies source or a previously used output.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

CLASSES = ('M3DfaPartitions', 'M3KnapsackSelection', 'M3DominatorLanes',
           'M3Bm25Rows', 'M3RangePatchMerge', 'M3FrequencySketch')
TESTS = ('M3AutomataPlanningContract', 'M3IndexDiffAdmissionContract')
EXPECTED = {'M3AutomataPlanningContract': 328225,
            'M3IndexDiffAdmissionContract': 350224}


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--owner', required=True, type=Path,
                        help='m3/tooling/module-packs/cpu-tools directory')
    parser.add_argument('--out', required=True, type=Path,
                        help='fresh output receipt directory outside source')
    args = parser.parse_args()
    owner = args.owner.resolve(strict=True)
    output = args.out.absolute()
    if (output.exists() or output.is_relative_to(owner) or not output.parent.is_dir()):
        raise ValueError('new external receipt directory required')
    package = owner / 'src/com.m3.tools.cpu/com/m3/tools/cpu'
    files = [package / (name + '.java') for name in CLASSES]
    files += [owner / 'fixtures' / (name + '.java') for name in TESTS]
    for file in files:
        if not file.is_file() or file.is_symlink():
            raise ValueError('ordinary source file missing: ' + str(file))
    seals = {file.relative_to(owner).as_posix(): sha(file) for file in files}
    logs = {}
    with tempfile.TemporaryDirectory(prefix='m3-wave4-') as temp:
        classes = Path(temp) / 'classes'
        classes.mkdir()
        compile_cmd = ['javac', '--release', '21', '-proc:none', '-Xlint:all',
                       '-Werror', '-d', str(classes), *(str(file) for file in files)]
        compiled = subprocess.run(compile_cmd, capture_output=True, text=True, timeout=90)
        if compiled.returncode or compiled.stderr.strip():
            raise RuntimeError('strict Java 21 compilation failed: ' + compiled.stderr)
        for mode in ('jit', 'interpreted'):
            for test in TESTS:
                cmd = ['java', '-Xcheck:jni']
                if mode == 'interpreted':
                    cmd.append('-Xint')
                cmd += ['-cp', str(classes), test]
                run = subprocess.run(cmd, capture_output=True, text=True, timeout=150)
                expected = ('M3_WAVE4_' + ('AUTOMATA_PLANNING' if test.startswith('M3Automata')
                             else 'INDEX_DIFF_ADMISSION') + '_PASS checks=' + str(EXPECTED[test]))
                if run.returncode or run.stdout.strip() != expected or run.stderr.strip():
                    raise RuntimeError(f'{mode}/{test}: code={run.returncode}: '
                                       f'{run.stdout} {run.stderr}')
                logs[mode + '/' + test] = expected
    for file in files:
        if sha(file) != seals[file.relative_to(owner).as_posix()]:
            raise RuntimeError('source changed during test: ' + str(file))
    output.mkdir()
    report = {
        'schema': 'm3-cpu-wave4-standalone/1',
        'status': 'STRICT_JAVA21_JIT_XINT_PASS',
        'source_sha256': seals,
        'compiled_java_sources': len(files),
        'checks_per_mode': sum(EXPECTED.values()),
        'evidence': logs,
        'maven_openrewrite_sdk': 'NOT_RUN',
        'installed_m3jdk21_jmod': 'NOT_RUN',
        'complete_91_family_implementation': False,
    }
    (output / 'REPORT.json').write_text(json.dumps(report, indent=2, sort_keys=True) + '\n', encoding='utf-8')
    print(json.dumps({'status': report['status'], 'checks_per_mode': report['checks_per_mode']},
                     sort_keys=True))

if __name__ == '__main__':
    main()
