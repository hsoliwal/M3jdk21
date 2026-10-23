#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run and retain this finite task's actual Maven/OpenRewrite qualification.

The existing recipe owners and JUnit harness perform every transformation and
refusal check. Adapted from qualified integrated SDK003 for this distinct Maven4 packet.
This wrapper captures a fresh bounded attempt, exact input and
compiled-dependency identities, reports, and emitted bytes. It neither modifies
the receiving product nor grants runtime admission.
"""
from __future__ import annotations

import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

from prepare_packet import TASK


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def files(root: Path) -> list[Path]:
    return sorted(path for path in root.rglob('*') if path.is_file())


def input_files(crate: Path) -> list[Path]:
    module = crate.parent.parent
    paths = [crate / name for name in (
        'pom.xml', 'phases.tsv', 'SOURCE_SEALS.json', 'prepare_packet.py',
        'apply_receiver.py', 'verify_sources.py', 'qualify.py', 'select_supported_successor.py')]
    paths += files(crate / 'src') + files(crate / 'preimages') + files(crate / 'attribution')
    paths += [module / 'src/main/java/com/synexia/rewrite' / name for name in (
        'M3HashPinnedJavaSnapshotRecipe.java', 'M3HashPinnedTextSnapshotRecipe.java',
        'M3EditScope.java', 'M3ScopeInference.java')]
    paths += [module / 'src/main/resources/META-INF/rewrite' / (TASK + '.yml')]
    for kind in ('java', 'text'):
        owner = module / 'src/main/resources/com/synexia/rewrite' / ('hash-pinned-' + kind)
        for phase in sorted(owner.glob(TASK + '-*')):
            paths += files(phase)
    if len(set(paths)) != len(paths) or any(not path.is_file() or path.is_symlink() for path in paths):
        raise ValueError('task input files must be distinct existing nonsymbolic files')
    return sorted(paths)


def source_manifest(crate: Path, paths: list[Path]) -> list[dict[str, object]]:
    canonical = crate.parents[2]
    return [{'path': path.relative_to(canonical).as_posix(),
             'sha256': sha(path), 'bytes': path.stat().st_size} for path in paths]


def classpath_manifest(paths: list[Path]) -> list[dict[str, object]]:
    result = []
    for path in paths:
        if path.is_dir():
            members = files(path)
            if not any(member.suffix == '.class' for member in members):
                raise ValueError('actual compiled classes required: ' + str(path))
            result.append({'path': str(path), 'kind': 'compiled-directory', 'files': [
                {'path': member.relative_to(path).as_posix(), 'sha256': sha(member),
                 'bytes': member.stat().st_size} for member in members]})
        elif path.is_file() and path.suffix == '.jar' and path.stat().st_size:
            result.append({'path': str(path), 'kind': 'jar', 'sha256': sha(path),
                           'bytes': path.stat().st_size})
        else:
            raise ValueError('missing compiled classpath owner: ' + str(path))
    return result


def write_json(path: Path, value: object) -> None:
    with path.open('x', encoding='utf-8') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', required=True, type=Path)
    parser.add_argument('--maven', required=True, type=Path)
    parser.add_argument('--maven-repo', required=True, type=Path)
    parser.add_argument('--parser-classpath', required=True, type=Path,
                        help='UTF-8 file containing one real compiled directory/JAR per line')
    parser.add_argument('--attempt', required=True)
    parser.add_argument('--online-bootstrap', action='store_true')
    parser.add_argument('--settings', type=Path)
    parser.add_argument('--truststore', type=Path)
    parser.add_argument('--timeout-seconds', type=int, default=900)
    args = parser.parse_args()
    if re.fullmatch('[a-z0-9][a-z0-9-]{0,79}', args.attempt) is None:
        raise ValueError('bounded lowercase attempt identifier required')
    if not 60 <= args.timeout_seconds <= 1800:
        raise ValueError('task timeout must be in 60..1800 seconds')
    crate = Path(__file__).resolve().parent
    canonical = crate.parents[2]
    paths = input_files(crate)
    classpath = [Path(line).resolve(strict=True) for line in
                 args.parser_classpath.read_text(encoding='utf-8').splitlines() if line.strip()]
    if not classpath or len(set(classpath)) != len(classpath):
        raise ValueError('nonempty distinct compiled classpath required')
    before = source_manifest(crate, paths)
    dependencies = classpath_manifest(classpath)
    out = crate / 'proof' / args.attempt
    out.mkdir(parents=True, exist_ok=False)
    write_json(out / 'SOURCE_INPUTS.json', before)
    write_json(out / 'PARSER_DEPENDENCIES.json', dependencies)
    # Preserve exact task inputs, including any subsequently corrected harness.
    for path in paths:
        dest = out / 'inputs' / path.relative_to(canonical)
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, dest)
    env = os.environ.copy()
    env['JAVA_HOME'] = str(args.java_home.resolve(strict=True))
    env['PATH'] = env['JAVA_HOME'] + '/bin:' + env['PATH']
    env['MAVEN_OPTS'] = '-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData'
    if args.truststore:
        env['MAVEN_OPTS'] += ' -Djavax.net.ssl.trustStore=' + str(args.truststore.resolve(strict=True))
    command = [str(args.maven.resolve(strict=True)), '-B', '-ntp', '-f', str(crate / 'pom.xml'),
               '-Dmaven.repo.local=' + str(args.maven_repo.resolve(strict=True)),
               '-Dparser.classpath=' + os.pathsep.join(map(str, classpath))]
    if not args.online_bootstrap:
        command.append('-o')
    if args.settings:
        command += ['-s', str(args.settings.resolve(strict=True))]
    command.append('test')
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    clock = time.monotonic()
    timed_out = False
    with (out / 'maven.log').open('xb') as log:
        try:
            result = subprocess.run(command, cwd=canonical, env=env, stdout=log,
                                    stderr=subprocess.STDOUT, timeout=args.timeout_seconds)
            exit_code = result.returncode
        except subprocess.TimeoutExpired:
            timed_out = True
            exit_code = 124
    after = source_manifest(crate, paths)
    dependencies_after = classpath_manifest(classpath)
    write_json(out / 'SOURCE_INPUTS_AFTER.json', after)
    write_json(out / 'PARSER_DEPENDENCIES_AFTER.json', dependencies_after)
    reports = crate / 'target/surefire-reports'
    if reports.is_dir():
        shutil.copytree(reports, out / 'surefire-reports')
    suites = []
    for report in sorted((out / 'surefire-reports').glob('TEST-*.xml')):
        tree = ET.parse(report).getroot()
        suites.append({key: tree.attrib.get(key, '') for key in
                       ('name', 'tests', 'failures', 'errors', 'skipped', 'time')})
    expected_suites = {'com.synexia.rewrite.M3Maven4EffectiveRecipeTest'}
    counts = {key: sum(int(suite[key]) for suite in suites) for key in
              ('tests', 'failures', 'errors', 'skipped')}
    proof_status = crate / 'target/proof-status.tsv'
    source_pass = proof_status.is_file() and proof_status.read_text().splitlines()[-1].startswith('PASS\t')
    passed = (exit_code == 0 and before == after and dependencies == dependencies_after
              and {suite['name'] for suite in suites} == expected_suites
              and counts == {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0} and source_pass)
    for name in ('recipe-proof.tsv', 'proof-status.tsv', 'named-recipe.yml'):
        path = crate / 'target' / name
        if path.is_file():
            shutil.copyfile(path, out / name)
    for name in ('phase-receipts', 'receiver'):
        path = crate / 'target' / name
        if passed and path.is_dir():
            shutil.copytree(path, out / name)
    receipt = {'schema': 'M3_MAVEN4_EFFECTIVE_SDK_ATTEMPT/1', 'task': TASK,
               'status': 'PASS' if passed else 'FAIL', 'scope': 'source-replay-only',
               'dependency_lane': 'online-artifact-bootstrap' if args.online_bootstrap else 'offline-cached-artifacts',
               'full_source_vendored_reactor': 'NOT_RUN', 'receiving_runtime_gates': 'SEPARATE',
               'started_at': started, 'duration_seconds': str(round(time.monotonic() - clock, 3)),
               'exit_code': exit_code, 'timed_out': timed_out, 'command': command, 'cwd': str(canonical),
               'java_release_sha256': sha(args.java_home / 'release'),
               'source_inputs_unchanged': before == after,
               'compiled_parser_dependencies_unchanged': dependencies == dependencies_after,
               'suites': suites, 'counts': counts,
               'artifacts': [{'path': path.relative_to(out).as_posix(), 'sha256': sha(path),
                              'bytes': path.stat().st_size} for path in files(out)]}
    write_json(out / 'RESULT.json', receipt)
    print(json.dumps({'status': receipt['status'], 'attempt': args.attempt, 'counts': counts,
                      'duration_seconds': receipt['duration_seconds'], 'receipt': str(out / 'RESULT.json')}))
    raise SystemExit(0 if passed else 1)


if __name__ == '__main__':
    main()
