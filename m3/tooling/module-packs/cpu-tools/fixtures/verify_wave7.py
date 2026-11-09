#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Check only the wave7 Java target postimages and independent runtime oracles."""
import argparse
import hashlib
import json
import subprocess
import tempfile
from pathlib import Path

SUBDIR = Path('m3/tooling/module-packs/cpu-tools')
JAVA = SUBDIR / 'src/jdk.m3.tools.cpu/jdk/m3/tools/cpu'
FIXTURES = SUBDIR / 'fixtures'
FILES = ('M3DifferenceConstraints.java', 'M3FunctionalDependency.java', 'M3MerkleDag.java')
ORACLES = {
    'M3ConstraintFDContract': 'M3_WAVE7_CONSTRAINT_FD_PASS checks=498148',
    'M3MerkleDagContract': 'M3_WAVE7_MERKLE_DAG_PASS checks=7965',
}

def run(args, timeout):
    result = subprocess.run(args, text=True, capture_output=True, timeout=timeout, check=False)
    if result.returncode != 0 or result.stderr.strip():
        raise RuntimeError(f'nonzero or diagnostic exit for {args}: {result.stderr[:3000]}')
    return result.stdout.strip()

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--root', required=True, type=Path)
    p.add_argument('--out', required=True, type=Path)
    a = p.parse_args()
    root = a.root.resolve(strict=True)
    out = a.out.absolute()
    if out.exists() or not out.parent.is_dir() or out.is_relative_to(root):
        raise ValueError('fresh output directory outside checked source root required')
    paths = [*(root / JAVA / f for f in FILES),
             *(root / FIXTURES / (f + '.java') for f in ORACLES)]
    if any(not f.is_file() or f.is_symlink() for f in paths):
        raise ValueError('missing or symlinked Java receiving source')
    before = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
    with tempfile.TemporaryDirectory(prefix='m3-wave7-product-', dir=out.parent) as directory:
        classes = Path(directory) / 'classes'
        classes.mkdir()
        run(['javac', '--release', '21', '-proc:none', '-Xlint:all', '-Werror',
             '-d', str(classes), *(str(f) for f in paths)], 90)
        outputs = {}
        for mode in ('jit', 'interpreted'):
            for klass, expected in ORACLES.items():
                args = ['java', *(['-Xint'] if mode == 'interpreted' else []),
                        '-cp', str(classes), klass]
                actual = run(args, 180)
                if actual != expected:
                    raise RuntimeError(f'unexpected oracle result for {klass}: {actual!r}')
                outputs[f'{mode}/{klass}'] = actual
    after = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
    if before != after:
        raise RuntimeError('receiving source mutated during verification')
    out.mkdir()
    report = {'schema': 'm3-cpu-wave7-receiver-scoped/1',
              'status': 'PRODUCT_SCOPED_JAVA21_JIT_XINT_PASS',
              'source_sha256': after, 'actual_executions': outputs,
              'checks_per_mode': 506113, 'maven_openrewrite_sdk': 'NOT_RUN',
              'linked_jmod_jtreg': 'NOT_RUN', 'full_91_family_coverage': False}
    (out / 'REPORT.json').write_text(json.dumps(report, sort_keys=True, indent=2) + '\n')
    print(json.dumps({'status': report['status'], 'checks_per_mode': 506113}, sort_keys=True))

if __name__ == '__main__': main()
