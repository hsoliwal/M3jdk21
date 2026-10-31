#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compile and execute the retained correction-profile conformance oracle afresh."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--jackson-core', type=Path, required=True)
    parser.add_argument('--attempt', required=True)
    args = parser.parse_args()
    if not re.fullmatch('[a-z0-9][a-z0-9-]{0,63}', args.attempt):
        raise ValueError('bounded lowercase attempt identifier required')
    crate = Path(__file__).resolve().parent.parent
    java_home = args.java_home.resolve(strict=True)
    jackson = args.jackson_core.resolve(strict=True)
    out = crate / 'proof' / args.attempt
    out.mkdir(parents=True, exist_ok=False)
    classes = out / 'classes'
    classes.mkdir()
    source_root = crate / 'src/test/java'
    sources = [source_root / relative for relative in (
        'com/synexia/rewrite/sealed/SealHash.java',
        'com/synexia/rewrite/memory/M3RecoveryMemory.java',
        'com/synexia/rewrite/memory/M3RecoveryMemoryTest.java')]
    pins = {str(path.relative_to(crate)): sha(path) for path in sources}
    commands = [
        [str(java_home / 'bin/java'), '-version'],
        [str(java_home / 'bin/javac'), '--release', '21', '-Xlint:all', '-Werror', '-proc:none',
         '-cp', str(jackson), '-d', str(classes), *map(str, sources)],
        [str(java_home / 'bin/java'), '-ea', '-cp', str(classes) + os.pathsep + str(jackson),
         'com.synexia.rewrite.memory.M3RecoveryMemoryTest'],
    ]
    results = []
    for index, command in enumerate(commands):
        log = out / f'command-{index:02d}.log'
        with log.open('xb') as stream:
            completed = subprocess.run(command, cwd=crate, stdout=stream,
                                       stderr=subprocess.STDOUT, timeout=120)
        results.append({'command': command, 'exit_code': completed.returncode,
                        'log': log.name, 'log_sha256': sha(log)})
        if completed.returncode:
            break
    for path, digest in pins.items():
        assert sha(crate / path) == digest, 'source changed during conformance: ' + path
    text = (out / 'command-02.log').read_text() if (out / 'command-02.log').exists() else ''
    count = re.search(r'MOP_RCM_CONFORMANCE PASS checks=([0-9]+) controlCases=32 profile=m3-integrated-refinement-20261021/1.0', text)
    passed = len(results) == 3 and all(row['exit_code'] == 0 for row in results) and count is not None
    record = {'schema': 'M3_MEMORY_CONFORMANCE_RESULT/1', 'status': 'PASS' if passed else 'FAIL',
              'scope': 'closed RECORDED/PENDING profile; no product admission',
              'observed_at': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'source_sha256': pins, 'jackson_core_sha256': sha(jackson),
              'java_release_sha256': sha(java_home / 'release'), 'commands': results,
              'checks': int(count.group(1)) if count else 0,
              'class_sha256': {str(path.relative_to(classes)): sha(path)
                               for path in sorted(classes.rglob('*.class'))}}
    (out / 'RESULT.json').write_text(json.dumps(record, indent=2) + '\n')
    print(json.dumps({'status': record['status'], 'checks': record['checks'],
                      'receipt': str(out / 'RESULT.json')}))
    raise SystemExit(0 if passed else 1)


if __name__ == '__main__':
    main()
