#!/usr/bin/env python3
"""Recipe -> clean scoped Java/JNI verification -> optional fenced materialization.

SPDX-License-Identifier: Apache-2.0
Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
This is build orchestration, not a Java transformation engine.
"""
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
MODULE = HERE
RECIPE = HERE / 'recipe'
RESOURCES = REPO / 'm3/tooling/migration-recipes/src/main/resources'
CRATE = 'm3-collection-lanes'
PACKAGE = 'com.m3.collections'
MODULAR = True
PROBES = HERE / 'verification'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else 'ABSENT'


def run(name, args, env=None):
    print(name, flush=True)
    result = subprocess.run([str(x) for x in args], cwd=REPO, env=env,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (HERE / 'target/evidence' / (name + '.log')).write_text(
        '$ ' + repr([str(x) for x in args]) + '\n' + result.stdout + '\nEXIT=' + str(result.returncode) + '\n')
    if result.returncode:
        print(result.stdout[-16000:])
        raise SystemExit(result.returncode)
    print(result.stdout[-2200:], end='', flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', help='materialize tested recipe output after all gates')
    options = parser.parse_args()
    target = HERE / 'target'
    target.mkdir(exist_ok=True)
    evidence = target / 'evidence'
    evidence.mkdir(exist_ok=True)
    java_home = Path(os.environ['JAVA_HOME'])
    junit = Path(os.environ['JUNIT_CONSOLE_JAR']).resolve()
    if not junit.is_file():
        raise SystemExit('JUNIT_CONSOLE_JAR must name the real JUnit platform console standalone JAR')
    java, javac = java_home / 'bin/java', java_home / 'bin/javac'
    crate = RESOURCES / 'com/synexia/rewrite/hash-pinned-java' / CRATE
    rows = [line.split('\t') for line in (crate / 'manifest.tsv').read_text().splitlines()
            if line and not line.startswith('#')]
    if len({r[0] for r in rows}) != len(rows):
        raise SystemExit('duplicate target')
    original = {}
    for path, before, after, template in rows:
        if Path(path).is_absolute() or '..' in Path(path).parts:
            raise SystemExit('unsafe path: ' + path)
        if sha(crate / template) != after:
            raise SystemExit('template drift: ' + path)
        if before != 'ABSENT' and sha(crate / 'pre' / template) != before:
            raise SystemExit('preimage drift: ' + path)
        current = sha(MODULE / path)
        if current not in (before, after):
            raise SystemExit('source drift: ' + path)
        original[path] = current
    native_path = 'src/main/native/collections/segmented_bit_lane.c'
    native_template = RESOURCES / 'com/synexia/rewrite/segmented-lane-native/segmented_bit_lane.c.txt'
    original[native_path] = sha(MODULE / native_path)
    if original[native_path] not in ('ABSENT', sha(native_template)):
        raise SystemExit('native source drift')
    if (REPO / '.git').exists():
        run('01-diff', ['git', 'diff', '--check'])
    (evidence / '02-static.log').write_text(
        f'PASS: {len(rows)} unique fenced Java targets, template/preimage/source hashes, native admission.\n')
    maven = [os.environ.get('MVN', 'mvn'), '-B', '-ntp']
    if os.environ.get('M3_MAVEN_OFFLINE') == '1':
        maven.append('-o')
    if os.environ.get('M2_REPO'):
        maven.append('-Dmaven.repo.local=' + os.environ['M2_REPO'])
    # Never let stale candidates/classes conceal an unsuccessful generation or compile.
    shutil.rmtree(RECIPE / 'target', ignore_errors=True)
    run('03-recipe-compile', maven + ['-f', RECIPE / 'pom.xml', 'test-compile'])
    run('04-recipe-tests', maven + ['-f', RECIPE / 'pom.xml', 'test'])
    candidate = RECIPE / 'target/candidate'
    for path, before, after, template in rows:
        if sha(candidate / path) != after:
            raise SystemExit('generated candidate mismatch: ' + path)
    if sha(candidate / native_path) != sha(native_template):
        raise SystemExit('native candidate mismatch')
    classes = target / 'classes'
    shutil.rmtree(classes, ignore_errors=True)
    classes.mkdir()
    mains = [candidate / r[0] for r in rows if r[0].startswith('src/main/')]
    tests = [candidate / r[0] for r in rows if r[0].startswith('src/test/')]
    if not MODULAR:
        tests.append(MODULE / 'src/test/java/com/synexia/common/collections/SegmentedPrimitiveStorageTest.java')
    probes = [PROBES / (n + '.java') for n in ('SegmentedLaneNativeProbe', 'PreparedLaneAllocationProbe')]
    common_args = [javac, '--release', '21', '-Werror', '-Xlint:all,-module' if MODULAR else '-Xlint:all']
    runtime_classpath = str(classes)
    if MODULAR:
        test_classes = target / 'test-classes'
        shutil.rmtree(test_classes, ignore_errors=True)
        test_classes.mkdir()
        runtime_classpath += os.pathsep + str(test_classes)
        run('05-java-compile', common_args + ['-d', classes] + mains)
        run('06-test-compile', common_args + ['-cp', str(classes) + os.pathsep + str(junit), '-d', test_classes] + tests + probes)
        meta = target / 'module-resources/META-INF'
        meta.mkdir(parents=True, exist_ok=True)
        for name in ('LICENSE', 'NOTICE', 'provenance.json'):
            shutil.copyfile(HERE / name, meta / name)
        run('07-module-package', [java_home / 'bin/jar', '--create', '--file', target / 'm3-collections.jar',
                                 '-C', classes, 'module-info.class', '-C', classes, 'com/m3/collections',
                                 '-C', meta.parent, 'META-INF'])
        run('08-module-descriptor', [java_home / 'bin/jar', '--describe-module', '--file', target / 'm3-collections.jar'])
    else:
        sourcepath = os.pathsep.join(str(p) for p in (candidate / 'src/main/java', MODULE / 'src/main/java'))
        run('05-java-compile', common_args + ['-cp', junit, '-sourcepath', sourcepath, '-d', classes] + mains + tests + probes)
        # Compile the actual sealed preimages, then compare JVM public/protected descriptors.
        baseline_src, baseline_classes = target / 'baseline-src', target / 'baseline-classes'
        shutil.rmtree(baseline_src, ignore_errors=True)
        shutil.rmtree(baseline_classes, ignore_errors=True)
        baseline_classes.mkdir()
        before_files = []
        for path, before, after, template in rows:
            if before == 'ABSENT' or not path.startswith('src/main/'):
                continue
            out = baseline_src / path
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes((crate / 'pre' / template).read_bytes())
            before_files.append(out)
        run('06-api-baseline-compile', common_args + ['-sourcepath', os.pathsep.join(
            str(p) for p in (baseline_src / 'src/main/java', MODULE / 'src/main/java')), '-d', baseline_classes] + before_files)
        report = []
        def signatures(directory, name):
            p = subprocess.run([str(java_home / 'bin/javap'), '-protected', '-s', '-classpath', str(directory), name],
                               capture_output=True, text=True, check=True)
            lines = [x.strip() for x in p.stdout.splitlines()]
            return {lines[i-1] + ' ' + line for i, line in enumerate(lines) if line.startswith('descriptor:')}
        for source in before_files:
            name = str(source.relative_to(baseline_src / 'src/main/java')).replace(os.sep, '.')[:-5]
            missing = signatures(baseline_classes, name) - signatures(classes, name)
            if missing:
                raise SystemExit('removed JVM descriptor: ' + name + repr(missing))
            report.append(name + ': existing public/protected member descriptors retained')
        (evidence / '07-api-descriptors.log').write_text('\n'.join(report) + '\n')
    selections = [PACKAGE + '.' + n for n in ('SegmentedLaneBulkContractTest', 'M3ConcurrentLaneContractTest', 'SegmentedPrimitiveStorageTest')]
    if not MODULAR:
        selections.append(PACKAGE + '.shapes.SegmentedReferenceBulkTest')
    run('09-contract-tests', [java, '-jar', junit, 'execute', '--disable-banner', '--class-path', runtime_classpath,
                             '--fail-if-no-tests'] + [a for s in selections for a in ('--select-class', s)])
    if not sys.platform.startswith('linux'):
        raise SystemExit('JNI verification currently requires Linux; no partial overall PASS')
    library = target / 'libsegmented_bits.so'
    native_args = [os.environ.get('CC', 'gcc'), '-std=c17', '-O2', '-Wall', '-Wextra', '-Werror',
                   '-fsanitize=undefined', '-fno-sanitize-recover=all', '-I' + str(java_home / 'include'),
                   '-I' + str(java_home / 'include/linux')]
    run('10-native-static', native_args + ['-fsyntax-only', candidate / native_path])
    run('11-native-compile', native_args + ['-shared', '-fPIC', candidate / native_path, '-o', library])
    run('12-jni-parity', [java, '-Xcheck:jni', '-cp', runtime_classpath, 'SegmentedLaneNativeProbe', library])
    run('13-prepared-allocation', [java, '-Xbatch', '-cp', runtime_classpath, 'PreparedLaneAllocationProbe'])
    if options.apply:
        # Recheck the entire target set after tests and before any writes.
        for path, before in original.items():
            if sha(MODULE / path) != before:
                raise SystemExit('source changed during verification: ' + path)
        for path in original:
            output = MODULE / path
            output.parent.mkdir(parents=True, exist_ok=True)
            temporary = output.with_name(output.name + '.m3-candidate')
            temporary.write_bytes((candidate / path).read_bytes())
            temporary.replace(output)
        print('Materialized tested recipe output. File replacements are individually atomic; this is not a repository transaction.')
    print('PASS: declared scope only. This is not the full Synexia reactor or OpenJDK/jtreg gate.')


if __name__ == '__main__':
    main()
