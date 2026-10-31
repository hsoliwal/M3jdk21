#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Replay the unchanged PR 1442 oracle; never normalize case observations."""
import hashlib
import argparse
import os
import json
import pathlib
import subprocess
import sys

OPTIONS = argparse.ArgumentParser(description=__doc__)
OPTIONS.add_argument('stage', choices=['before', 'after'])
OPTIONS.add_argument('--repository', type=pathlib.Path, required=True)
OPTIONS.add_argument('--output', type=pathlib.Path, required=True)
ARGS = OPTIONS.parse_args()
ROOT = ARGS.repository.resolve()
JAVA_HOME = pathlib.Path(os.environ['JAVA_HOME']).resolve()
OLD = ROOT / 'm3/collections/qualification/tree-empty-lookup-admission-20261019'
OUT = ARGS.output.resolve()
OUT.mkdir(parents=True, exist_ok=True)
CLASSES = ROOT / 'm3/collections/target/classes'

def run(argv, output):
    result = subprocess.run([str(a) for a in argv], stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=90, check=False)
    output.with_suffix(output.suffix + '.command.json').write_text(json.dumps({
        'argv': [str(a) for a in argv], 'exitCode': result.returncode,
        'stdoutSha256': hashlib.sha256(result.stdout).hexdigest(),
        'stderrSha256': hashlib.sha256(result.stderr).hexdigest()}, indent=2) + '\n')
    output.write_bytes(result.stdout)
    output.with_suffix(output.suffix + '.stderr').write_bytes(result.stderr)
    if result.returncode:
        raise RuntimeError((argv, result.returncode, result.stderr.decode()))

def cases(path):
    rows = [json.loads(line) for line in path.read_text().splitlines()]
    assert rows[0]['kind'] == 'identity'
    assert rows[-1] == {'kind': 'complete', 'caseCount': 640}
    result = [r for r in rows if r['kind'] == 'case']
    assert len(result) == len({r['id'] for r in result}) == 640
    return result

def main():
    stage = ARGS.stage
    fixture = OUT / 'fixture-classes'
    fixture.mkdir(parents=True, exist_ok=True)
    if stage == 'before' or not (fixture / 'EmptyRootLookupOracle.class').exists():
        run([JAVA_HOME / 'bin/javac', '--release', '21', '-d', fixture,
             OLD / 'CanonicalPutAllCalibration3.java', OLD / 'EmptyRootLookupOracle.java'],
            OUT / 'oracle-compile.log')
    for provider, name in [('java.util.TreeMap', 'jdk'), ('com.m3.collections.M3TreeMap', 'm3')]:
        origin = 'jrt:/java.base' if name == 'jdk' else 'file:' + str(CLASSES) + '/'
        run([JAVA_HOME / 'bin/java', '-Xmx256m', '-Duser.language=en',
             '-DexpectedJavaHome=' + str(JAVA_HOME), '-DexpectedProviderOrigin=' + origin,
             '-cp', str(fixture) + ':' + str(CLASSES), 'EmptyRootLookupOracle', provider],
            OUT / (name + '-' + stage + '.jsonl'))
    reference = cases(OLD / 'observations/jdk-original.jsonl')
    jdk = cases(OUT / ('jdk-' + stage + '.jsonl'))
    candidate = cases(OUT / ('m3-' + stage + '.jsonl'))
    assert reference == jdk, 'Current JDK 21 case bodies differ from frozen JDK original'
    differences = [{'id': a['id'], 'jdk': a, 'm3': b}
                   for a, b in zip(jdk, candidate) if a != b]
    report = {'stage': stage, 'cases': len(jdk), 'exactMatches': 640 - len(differences),
              'frozenJdkOriginalMatches': reference == jdk, 'differences': differences}
    if stage == 'before':
        assert candidate == cases(OLD / 'observations/m3-after.jsonl')
        assert len(differences) == 10
    else:
        before = cases(OUT / 'm3-before.jsonl')
        report['fixedCases'] = [a['id'] for a,b,c in zip(jdk,before,candidate) if a!=b and a==c]
        report['introducedDifferences'] = [a['id'] for a,b,c in zip(jdk,before,candidate) if a==b and a!=c]
        empty = [i for i,r in enumerate(jdk) if r['id'].startswith('NATURAL/')
                 and '/NONEMPTY/' not in r['id'] and any('/'+k+'/' in r['id']
                     for k in ['COUNTING','SELF_THROW','ALWAYS_THROW'])]
        report['preservedEmptyCases'] = sum(jdk[i] == candidate[i] for i in empty)
        assert len(empty) == 144 and report['preservedEmptyCases'] == 144
        assert not differences and len(report['fixedCases']) == 10
    report['status'] = 'BASELINE_REPRODUCED' if stage == 'before' else 'PASS_640_EXACT_CASES'
    (OUT / (stage + '-report.json')).write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({k:v for k,v in report.items() if k not in ['differences','fixedCases']}))

if __name__ == '__main__':
    main()
