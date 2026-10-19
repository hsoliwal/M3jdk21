#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Run the explicitly scoped suite; write new receipts, never replace prior evidence."""
import argparse
from datetime import datetime, timezone
import hashlib
import importlib.metadata
import json
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    out = args.out.absolute()
    if any(p.is_symlink() for p in [out, *out.parents]) or out.exists():
        raise SystemExit('Refusing existing or symlink output directory')
    for name in ('java', 'javac', 'javap'):
        if not shutil.which(name):
            raise SystemExit('Required tool unavailable: ' + name)
    java_version = subprocess.check_output(['java', '-version'], stderr=subprocess.STDOUT, text=True)
    if 'version "21.' not in java_version:
        raise SystemExit('These receipts require a stock Java 21 execution environment')
    pins = json.loads((ROOT / 'm3/migration/pinned-dependencies.json').read_text())
    for path, expected in pins['files'].items():
        if sha(ROOT / path) != expected['sha256']:
            raise SystemExit('Pinned target dependency drift: ' + path)
    out.mkdir(parents=True)
    sources = sorted(list(pins['files']) + [
        'm3/algorithms/src/com/m3/algorithm/M3PrefixZ.java',
        'm3/algorithms/test/PrefixZContractTest.java',
        'm3/algorithms/test/ViewInteropContractTest.java',
        'm3/migration/src/com/m3/migration/ExactFileRecipe.java',
        'm3/migration/test/RecipeContractTest.java'])
    java_inputs = {p: sha(ROOT / p) for p in sources}
    python_paths = ['m3/migration/migration.py', 'm3/migration/test/test_migration.py',
                    'm3/docs/name-mapping.schema.json', 'm3/migration/requirements.txt']
    python_inputs = {p: sha(ROOT / p) for p in python_paths}
    runner_inputs = {p: sha(ROOT / p) for p in ['m3/migration/run-tests.py', 'm3/migration/pinned-dependencies.json']}
    runs = []
    def run(identifier, command, inputs):
        completed = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
        log = out / (identifier + '.log')
        log.write_bytes(completed.stdout)
        result = {'id': identifier, 'command': command, 'exit_code': completed.returncode,
                  'status': 'passed' if completed.returncode == 0 else 'failed',
                  'inputs': dict(sorted({**inputs, **runner_inputs}.items())), 'stdout_sha256': sha(log)}
        if log.is_relative_to(ROOT): result['stdout_path'] = log.relative_to(ROOT).as_posix()
        runs.append(result)
        print(identifier + ': ' + result['status'], flush=True)
        if completed.returncode != 0:
            raise RuntimeError(identifier + ' failed; inspect ' + str(log))
    failure = None
    try:
        with tempfile.TemporaryDirectory(prefix='m3-scoped-tests-') as temp:
            run('javac21', ['javac', '--release', '21', '-Xlint:all', '-Werror', '-d', temp, *sources], java_inputs)
            for mode, flags in [('normal', []), ('interpreter', ['-Xint'])]:
                for test, identifier in [('PrefixZContractTest', 'prefix-z'), ('ViewInteropContractTest', 'view-interop')]:
                    run(identifier + '-' + mode, ['java', *flags, '-ea', '-cp', temp, test], java_inputs)
            run('exact-recipe-contract', ['java', '-ea', '-cp', temp, 'RecipeContractTest'], java_inputs)
        run('migration-validator', [sys.executable, '-m', 'unittest', 'discover', '-s', 'm3/migration/test', '-p', 'test_migration.py', '-v'], python_inputs)
        run('stock-string-surface', ['javap', '-public', '-s', 'java.lang.String'], {})
    except (RuntimeError, OSError) as problem:
        failure = str(problem)
    finally:
        receipt = {'schema': 1, 'created_utc': datetime.now(timezone.utc).isoformat(),
                   'execution': 'local-scoped-classpath', 'repository_checkout': 'five hash-pinned actual target classes; not a complete checkout',
                   'source_baseline': '75fb1abaecb56969bea2914520bbe819f130632b',
                   'target_baseline': 'd6390ea3bb348f0c22afdba0819ca4ec0e97970f',
                   'environment': {'os': platform.platform(), 'architecture': platform.machine(),
                                   'java': java_version, 'python': sys.version, 'jsonschema': importlib.metadata.version('jsonschema')},
                   'runs': runs, 'failure': failure,
                   'not_executed': ['complete source inventory', 'full MIndex source-owner test reactor', 'Maven lifecycle',
                                    'complete OpenJDK image build', 'modified-JDK runtime and JIT acceptance',
                                    'Windows native execution', 'performance/retained-memory benchmarks']}
        (out / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    return 1 if failure else 0

if __name__ == '__main__':
    raise SystemExit(main())
