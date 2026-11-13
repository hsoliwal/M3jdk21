#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify wave8 product postimages independently; does not run Maven/OpenRewrite."""
import argparse
import hashlib
import json
import os
import subprocess
import tempfile
from pathlib import Path

SUBDIR = Path('m3/tooling/module-packs/cpu-tools')
JAVA = SUBDIR / 'src/jdk.m3.tools.cpu/jdk/m3/tools/cpu'
FIXTURES = SUBDIR / 'fixtures'
FILES = ('M3ArcConsistency.java', 'M3DagReduction.java', 'M3TwoSat.java')
ORACLES = {
    'M3Wave8ConstraintContract': 'M3_WAVE8_CONSTRAINTS_PASS checks=153108',
    'M3Wave8DagContract': 'M3_WAVE8_DAG_PASS checks=171178',
}

def command(args, seconds):
    env = dict(os.environ)
    for name in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'MAVEN_OPTS'):
        env.pop(name, None)
    result = subprocess.run(args, capture_output=True, text=True, timeout=seconds,
                            check=False, env=env)
    if result.returncode != 0 or result.stderr.strip():
        raise RuntimeError(f'Java verification failed: {args} exit={result.returncode} '
                           f'stderr={result.stderr[:2000]}')
    return result.stdout.strip()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    a = parser.parse_args()
    root = a.root.resolve(strict=True)
    out = a.out.absolute()
    if out.exists() or not out.parent.is_dir() or out.is_relative_to(root):
        raise ValueError('fresh outside-receiver receipt directory required')
    paths = [*(root / JAVA / name for name in FILES),
             *(root / FIXTURES / (name + '.java') for name in ORACLES)]
    if any(not path.is_file() or path.is_symlink() for path in paths):
        raise ValueError('receiving Java source missing or symlinked')
    def seals():
        return {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                for p in paths}
    before = seals()
    with tempfile.TemporaryDirectory(prefix='m3-wave8-received-', dir=out.parent) as location:
        classes = Path(location) / 'classes'
        classes.mkdir()
        command(['javac', '--release', '21', '-proc:none', '-Xlint:all', '-Werror',
                 '-d', str(classes), *(str(p) for p in paths)], 90)
        actual = {}
        for mode in ('jit', 'interpreted'):
            for klass, expected in ORACLES.items():
                label = f'{mode}/{klass}'
                args = ['java', '-Xcheck:jni',
                        *(['-Xint'] if mode == 'interpreted' else []),
                        '-cp', str(classes), klass]
                result = command(args, 180)
                if result != expected:
                    raise ValueError(f'wrong oracle output {label}: {result!r}')
                actual[label] = result
    if before != seals():
        raise ValueError('receiving source mutated during qualification')
    report = {'schema': 'm3-cpu-wave8-receiving/1',
              'status': 'PRODUCT_SCOPED_JAVA21_JIT_XINT_PASS',
              'checks_per_mode': 324286,
              'source_sha256': before, 'actual_executions': actual,
              'maven_openrewrite_sdk': 'NOT_RUN',
              'm3jdk21_jmod_jtreg': 'NOT_RUN',
              'full_91_family_coverage': False}
    out.mkdir()
    (out / 'REPORT.json').write_text(json.dumps(report, sort_keys=True, indent=2) + '\n')
    print(json.dumps({'status':report['status'], 'checks_per_mode':324286}, sort_keys=True))

if __name__ == '__main__':
    main()
