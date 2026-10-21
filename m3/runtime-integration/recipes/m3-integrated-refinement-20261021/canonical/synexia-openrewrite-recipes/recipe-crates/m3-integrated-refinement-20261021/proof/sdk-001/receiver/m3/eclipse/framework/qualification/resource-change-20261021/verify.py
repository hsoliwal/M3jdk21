#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Qualify the emitted resource owner with real Eclipse APIs and existing mask JNI."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True, help='fresh evidence directory')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[5]
    out = args.out.resolve()
    if out.exists():
        raise SystemExit('Refusing to overwrite prior evidence: ' + str(out))
    if sys.platform != 'linux':
        raise SystemExit('This qualification owns Linux JNI only')
    out.mkdir(parents=True)
    jdk = Path(os.environ['JAVA_HOME']).resolve()
    framework = root / 'm3/eclipse/framework'
    collections = root / 'm3/collections'
    crate = root / 'm3/runtime-integration/recipes/m3-integrated-refinement-20261021/canonical/synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021'
    commands = []
    receipt = {'status': 'RUNNING_OR_INCOMPLETE', 'commands': commands,
               'scope': 'Current framework owner; real Eclipse dependencies; strict-compiled collections; existing Linux JNI. No full image/workspace claim.'}

    def write():
        (out / 'RESULT.json').write_text(json.dumps(receipt, indent=2) + '\n')

    def run(name, command):
        log = out / (name + '.log')
        with log.open('w') as output:
            process = subprocess.run([str(value) for value in command], cwd=root,
                                     stdout=output, stderr=subprocess.STDOUT)
        commands.append({'name': name, 'command': [str(value) for value in command],
                         'exit': process.returncode, 'log': log.name, 'log_sha256': sha(log)})
        write()
        print(name, process.returncode, flush=True)
        if process.returncode:
            receipt['status'] = 'FAILED'
            write()
            raise SystemExit(process.returncode)
        return log

    maven = [os.environ.get('MVN', 'mvn'), '-B', '-ntp']
    if os.environ.get('M3_MAVEN_OFFLINE') == '1':
        maven.append('-o')
    if os.environ.get('M3_MAVEN_SETTINGS'):
        maven += ['-s', os.environ['M3_MAVEN_SETTINGS']]
    if os.environ.get('M2_REPO'):
        maven.append('-Dmaven.repo.local=' + os.environ['M2_REPO'])
    run('source-seals', [sys.executable, crate / 'verify_sources.py', root])
    classes, headers, native, missing = [out / name for name in ('classes', 'headers', 'native', 'missing')]
    for directory in (classes, headers, native, missing):
        directory.mkdir()
    java_sources = sorted((collections / 'src/main/java').rglob('*.java'))
    source_args = out / 'collections.args'
    source_args.write_text('\n'.join(str(path) for path in java_sources) + '\n')
    run('collections-compile', [jdk / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror',
                              '-h', headers, '-d', classes, '@' + str(source_args)])
    jar = out / 'm3-collections.jar'
    run('collections-jar', [jdk / 'bin/jar', '--create', '--file', jar, '-C', classes, '.'])
    run('collections-install', maven + ['org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file',
        '-Dfile=' + str(jar), '-DgroupId=com.m3', '-DartifactId=m3-collections',
        '-Dversion=0.1.0-SNAPSHOT', '-Dpackaging=jar', '-DgeneratePom=true'])
    cc = [os.environ.get('CC', 'cc'), '-std=c17', '-Wall', '-Wextra', '-Werror', '-O2',
          '-fsanitize=undefined', '-fno-sanitize-recover=undefined',
          '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux')]
    source_pairs = [('m3_index_mask_compactor.c', 'M3IndexMaskCompactor'),
                    ('m3_index_mask_window.c', 'M3IndexMaskWindow')]
    objects, expected = [], set()
    for name, owner in source_pairs:
        source = collections / 'src/main/native/collections' / name
        header = headers / ('com_m3_collections_' + owner + '.h')
        expected.update(re.findall(r'\b(Java_com_m3_collections_\w+)\s*\(', header.read_text()))
        obj = native / (name + '.o')
        run('native-' + owner, cc + ['-fPIC', '-include', header, '-c', source, '-o', obj])
        objects.append(obj)
    library = native / 'libm3_collections_mask.so'
    run('native-link', cc + ['-shared', *objects, '-o', library])
    exports = run('native-exports', ['nm', '-D', '--defined-only', library]).read_text()
    actual = set(re.findall(r'\b(Java_com_m3_collections_\w+)\b', exports))
    if not expected or actual != expected:
        raise AssertionError(('generated-header/export mismatch', sorted(expected), sorted(actual)))
    receipt['native_exports'] = sorted(actual)
    receipt['tests'] = {}
    for mode, directory, required in [('java', missing, 'false'), ('native', native, 'true')]:
        reports = framework / 'target/surefire-reports'
        if reports.exists():
            shutil.rmtree(reports)
        vm_args = '-Xcheck:jni -Djava.library.path=' + str(directory) + ' -Dm3.resource.requireNative=' + required
        run('framework-' + mode, maven + ['-f', framework / 'pom.xml', '-DargLine=' + vm_args, 'verify'])
        shutil.copytree(reports, out / (mode + '-reports'))
        xml = list(reports.glob('TEST-*.xml'))
        suites = [ET.parse(path).getroot() for path in xml]
        totals = {key: sum(int(suite.get(key, '0')) for suite in suites)
                  for key in ('tests', 'failures', 'errors', 'skipped')}
        if not totals['tests'] or any(totals[key] for key in ('failures', 'errors', 'skipped')):
            raise AssertionError(('test gate did not pass', mode, totals))
        if not any(suite.get('name', '').endswith('.M3EclipseResourceChangeSnapshotTest') for suite in suites):
            raise AssertionError('new resource contract suite was not executed')
        receipt['tests'][mode] = totals
        write()
    run('classpath', maven + ['-f', framework / 'pom.xml',
        'org.apache.maven.plugins:maven-dependency-plugin:3.11.0:build-classpath',
        '-Dmdep.outputFile=' + str(out / 'classpath.txt')])
    sealed = java_sources + sorted((framework / 'src').rglob('*.java')) + [framework / 'pom.xml']
    sealed += [collections / 'src/main/native/collections' / name for name, _ in source_pairs]
    receipt.update(status='PASS', source_seals={str(path.relative_to(root)): sha(path) for path in sealed},
                   jdk_release_sha256=sha(jdk / 'release'), native_library_sha256=sha(library),
                   collections_jar_sha256=sha(jar), work_mask_pairs_per_mode=65536)
    write()
    print(json.dumps(receipt['tests']), flush=True)


if __name__ == '__main__':
    main()
