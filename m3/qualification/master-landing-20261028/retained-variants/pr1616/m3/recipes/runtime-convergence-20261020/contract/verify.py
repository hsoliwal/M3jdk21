#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
"""Verify received owners using their existing builds and actual Java/JNI contracts.

This is test orchestration. It neither transforms sources nor admits a JDK phase.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', required=True, type=Path)
    args = parser.parse_args()
    root = args.target.resolve()
    crate = Path(__file__).resolve().parents[1]
    out = crate / 'target/contract-proof'
    out.mkdir(parents=True, exist_ok=True)
    receipt = out / 'RECEIPT.json'
    receipt.unlink(missing_ok=True)
    java = Path(os.environ['JAVA_HOME'])
    records = []

    def run(name, command):
        command = list(map(str, command))
        result = subprocess.run(command, cwd=root, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT)
        (out / (name + '.log')).write_text(repr(command) + '\n' + result.stdout
                                          + '\nEXIT=' + str(result.returncode) + '\n')
        records.append({'name': name, 'command': command, 'exit': result.returncode})
        print(name, 'exit=' + str(result.returncode), flush=True)
        if result.returncode:
            print(result.stdout[-14000:])
            raise SystemExit(result.returncode)
        return result.stdout

    sources = {}
    resources = crate / 'src/main/resources/com/synexia/rewrite'
    for manifest in sorted(resources.glob('hash-pinned-*/*/manifest.tsv')):
        for line in manifest.read_text().splitlines():
            if not line or line.startswith('#'):
                continue
            relative, before, after, template = line.split('\t')
            path = root / relative
            if (not path.is_file() or path.is_symlink() or sha(path) != after
                    or sha(manifest.parent / template) != after):
                raise SystemExit('received postimage is not exact: ' + relative)
            if relative in sources:
                raise SystemExit('duplicate manifest target: ' + relative)
            sources[relative] = after
    if not sources:
        raise SystemExit('no sealed receiving sources')
    version = run('java-version', [java / 'bin/java', '-version'])
    if '"21.' not in version and '"21"' not in version:
        raise SystemExit('this qualification requires Java 21')
    run('owning-collections-java-jni', ['python3', root /
        'm3/collections/qualification/full-java-jni-20261018/verify.py'])

    mvn = [os.environ.get('MVN', 'mvn'), '-B', '-ntp']
    if os.environ.get('M3_MAVEN_OFFLINE') == '1':
        mvn.append('-o')
    if os.environ.get('M2_REPO'):
        mvn.append('-Dmaven.repo.local=' + os.environ['M2_REPO'])
    # The same full module has just passed; install that artifact for Eclipse consumers.
    run('install-tested-collections', mvn + ['-f', root / 'm3/collections/pom.xml',
                                           '-DskipTests', 'install'])
    reports = {}
    for module, count in [('core-runtime', 16), ('framework', 21)]:
        owner = root / 'm3/eclipse' / module
        old = owner / 'target/surefire-reports'
        if old.exists():
            shutil.rmtree(old)
        run('eclipse-' + module, mvn + ['-f', owner / 'pom.xml', 'clean', 'verify'])
        suites = [ET.parse(p).getroot() for p in old.glob('TEST-*.xml')]
        totals = {key: sum(int(suite.get(key, '0')) for suite in suites)
                  for key in ('tests', 'failures', 'errors', 'skipped')}
        if totals != {'tests': count, 'failures': 0, 'errors': 0, 'skipped': 0}:
            raise SystemExit('unexpected Eclipse test discovery/result: ' + str(totals))
        reports[module] = totals
        destination = out / ('eclipse-' + module + '-reports')
        if destination.exists():
            shutil.rmtree(destination)
        shutil.copytree(old, destination)

    # Stage exact current sources so javac cannot discover unrelated java.base sources.
    stage, patch, tests = (out / name for name in ('jdk-source', 'jdk-patch', 'jdk-tests'))
    for directory in (stage, patch, tests):
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir()
    relative_sources = ['java/util/ArrayList.java', 'java/util/Vector.java',
                        'jdk/internal/mindex/M3CollectionMaskCompactor.java']
    copied = []
    runtime_seals = {}
    for relative in relative_sources:
        source = root / 'src/java.base/share/classes' / relative
        destination = stage / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        copied.append(destination)
        runtime_seals[source.relative_to(root).as_posix()] = sha(source)
    run('jdk-owner-compile', [java / 'bin/javac', '-Xlint:all', '-Werror',
                             '--patch-module', 'java.base=' + str(stage), '-d', patch, *copied])
    test_source = root / 'test/jdk/java/util/ArrayList/M3MaskThresholdContracts.java'
    exports = 'java.base/jdk.internal.mindex=ALL-UNNAMED'
    run('jdk-oracle-compile', [java / 'bin/javac', '-Xlint:all', '-Werror',
                              '--patch-module', 'java.base=' + str(patch), '--add-exports', exports,
                              '-d', tests, test_source])
    run('jdk-array-mask-oracle', [java / 'bin/java', '-Xmx768m', '-ea', '-Xcheck:jni',
                                 '--patch-module', 'java.base=' + str(patch), '--add-exports', exports,
                                 '-cp', tests, 'M3MaskThresholdContracts'])
    for relative, expected in {**sources, **runtime_seals}.items():
        if sha(root / relative) != expected:
            raise SystemExit('source changed during qualification: ' + relative)
    result = {'status': 'PASS_DECLARED_RECEIVED_JAVA_JNI_AND_ECLIPSE_SCOPE',
              'scope': 'owning collection module, real Eclipse dependencies, and exact current java.base source patch; not a complete built M3JDK21 image or jtreg run',
              'phase_promotion': False, 'eclipse_tests': reports,
              'source_sha256': sources, 'unchanged_runtime_sha256': runtime_seals,
              'runs': records}
    receipt.write_text(json.dumps(result, indent=2) + '\n')
    print(result['status'], flush=True)


if __name__ == '__main__':
    main()
