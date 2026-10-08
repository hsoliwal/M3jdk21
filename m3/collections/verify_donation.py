#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: qualify concurrent EnumSet in a new exact inventory; preserve all acceptance guards and historical receipts.
# Modified 2026 by Hitesh Soliwal and contributors: restore exact collection receiving provenance and verification while retaining current owners and String phase authority.
"""Verify the installed M3 collection donation with target-native Java/JNI checks.

This host verifies product source. The canonical transformation and SDK replay remain
in Synexia. The earlier verify.py remains the frozen primitive-lane installer.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cost-probe', action='store_true', help='record paired diagnostic allocation/time samples')
    args = parser.parse_args()
    java_home = Path(os.environ['JAVA_HOME']).resolve()
    junit = Path(os.environ['JUNIT_CONSOLE_JAR']).resolve()
    if not junit.is_file():
        raise SystemExit('JUNIT_CONSOLE_JAR must name a real JUnit platform console standalone JAR')
    if digest(junit) != '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a':
        raise SystemExit('Expected exact JUnit console 1.12.2 artifact')
    if not sys.platform.startswith('linux'):
        raise SystemExit('This JNI acceptance host requires Linux; no partial overall PASS')
    target = HERE / 'target/donation-verification'
    shutil.rmtree(target, ignore_errors=True)
    evidence = target / 'evidence'
    evidence.mkdir(parents=True)

    def run(name, command):
        command = [str(x) for x in command]
        result = subprocess.run(command, cwd=REPO, capture_output=True, text=True)
        output = result.stdout + result.stderr
        (evidence / (name + '.log')).write_text(
            '$ ' + repr(command) + '\n' + output + '\nEXIT=' + str(result.returncode) + '\n')
        if result.returncode:
            print(output[-16000:])
            raise SystemExit(result.returncode)
        print(name + ': PASS', flush=True)
        return output

    version = run('01-java-version', [java_home / 'bin/java', '-version'])
    if 'version "21.' not in version and 'version "21"' not in version:
        raise SystemExit('The qualified baseline requires Java 21')
    manifest = HERE / 'qualification/installed-source-enum-set-20261008.tsv'
    packet = json.loads((HERE / 'synexia-donation.json').read_text())
    if digest(manifest) != packet['installed_source_manifest_sha256']:
        raise SystemExit('Installed source manifest drift; reconcile with canonical recipe before verifying')
    rows = [line.split('\t') for line in manifest.read_text().splitlines()
            if line and not line.startswith('#')]
    if len({row[0] for row in rows}) != len(rows):
        raise SystemExit('Duplicate installed source path')
    for path, expected in rows:
        relative = Path(path)
        if relative.is_absolute() or '..' in relative.parts or not path.startswith('m3/collections/'):
            raise SystemExit('Invalid receiver path: ' + path)
        source = REPO / relative
        if not source.is_file() or digest(source) != expected:
            raise SystemExit('Installed source drift: ' + path)
    declared_java = {path for path, _ in rows if path.endswith('.java') and '/src/' in path}
    actual_java = {str(path.relative_to(REPO)) for path in (HERE / 'src').rglob('*.java')}
    if declared_java != actual_java:
        raise SystemExit('Java source set changed; qualify a successor packet')
    mains = sorted(REPO / path for path in declared_java if '/src/main/' in path)
    tests = sorted(REPO / path for path in declared_java if '/src/test/' in path)
    probes = [HERE / 'verification' / (name + '.java') for name in
              ('SegmentedLaneNativeProbe', 'PreparedLaneAllocationProbe')]
    classes, test_classes = target / 'classes', target / 'test-classes'
    classes.mkdir()
    test_classes.mkdir()
    common = [java_home / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror']
    run('02-module-compile', common + ['-d', classes] + mains)
    run('03-test-compile', common + ['-cp', str(classes) + os.pathsep + str(junit),
                                   '-d', test_classes] + tests + probes)
    runtime_classpath = str(classes) + os.pathsep + str(test_classes)
    reports = target / 'junit-reports'
    run('04-all-contract-tests', [java_home / 'bin/java', '-jar', junit, 'execute',
                                '--disable-banner', '--class-path', runtime_classpath,
                                '--scan-class-path', '--fail-if-no-tests', '--reports-dir', reports])
    totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for report in reports.glob('TEST-*.xml'):
        suite = ET.parse(report).getroot()
        for key in totals:
            totals[key] += int(suite.attrib.get(key, '0'))
    if not totals['tests'] or any(totals[key] for key in ('failures', 'errors', 'skipped')):
        raise SystemExit('JUnit acceptance requires nonzero tests and no failures/errors/skips: ' + repr(totals))
    for name in ('M3LazyTreeMapSelfTest', 'M3LazyValueStateSelfTest'):
        run('04-' + name, [java_home / 'bin/java', '-ea', '-cp', runtime_classpath,
                           'com.m3.collections.' + name])
    resources = target / 'module-resources/META-INF'
    resources.mkdir(parents=True)
    names = ('LICENSE', 'NOTICE', 'provenance.json', 'synexia-donation.json', 'SYNEXIA-DONATION-NOTICE.txt', 'name-mapping.json')
    for name in names:
        shutil.copyfile(HERE / name, resources / name)
    jar = target / 'm3-collections.jar'
    run('05-module-package', [java_home / 'bin/jar', '--create', '--file', jar,
                            '-C', classes, '.', '-C', resources.parent, '.'])
    descriptor = run('06-module-descriptor', [java_home / 'bin/jar', '--describe-module', '--file', jar])
    requirements = [line.strip() for line in descriptor.splitlines() if line.strip().startswith('requires ')]
    if requirements != ['requires java.base mandated']:
        raise SystemExit('Unexpected runtime dependency: ' + repr(requirements))
    with zipfile.ZipFile(jar) as archive:
        for name in names:
            if archive.read('META-INF/' + name) != (HERE / name).read_bytes():
                raise SystemExit('Packaged attribution/provenance mismatch: ' + name)
    native = HERE / 'src/main/native/collections/segmented_bit_lane.c'
    native_args = [os.environ.get('CC', 'gcc'), '-std=c17', '-O2', '-Wall', '-Wextra', '-Werror',
                   '-fsanitize=undefined', '-fno-sanitize-recover=all',
                   '-I' + str(java_home / 'include'), '-I' + str(java_home / 'include/linux')]
    library = target / 'libsegmented_bits.so'
    run('07-native-static', native_args + ['-fsyntax-only', native])
    run('08-native-compile', native_args + ['-shared', '-fPIC', native, '-o', library])
    run('09-checked-jni-parity', [java_home / 'bin/java', '-Xcheck:jni', '-cp', runtime_classpath,
                                 'SegmentedLaneNativeProbe', library])
    run('10-prepared-allocation', [java_home / 'bin/java', '-Xbatch', '-cp', runtime_classpath,
                                  'PreparedLaneAllocationProbe'])
    if args.cost_probe:
        cost = run('11-collection-cost-probe', [java_home / 'bin/java', '-Xbatch', '-cp', runtime_classpath,
                                               'com.m3.collections.M3CollectionCostProbe'])
        (evidence / 'collection-cost.csv').write_text(cost)
    receipt = {'copyright': 'Copyright 2026 Hitesh Soliwal and contributors', 'license': 'Apache-2.0', 'schema': 'M3_COLLECTION_RECEIVING_QUALIFICATION_V1',
               'scope': 'SEPARATELY_BUILT_COLLECTION_MODULE',
               'installed_source_manifest_sha256': digest(manifest),
               'java_sources': len(mains), 'test_sources': len(tests), 'junit': totals,
               'main_self_tests': ['M3LazyTreeMapSelfTest:PASS', 'M3LazyValueStateSelfTest:PASS'],
               'java_compile': '--release 21 -Xlint:all,-module -Werror',
               'runtime_requires': ['java.base'], 'native_standard': 'c17',
               'undefined_behavior_sanitizer': 'PASS', 'checked_jni': 'PASS',
               'prepared_lane_allocation': 'PASS', 'packaged_license_provenance': 'PASS',
               'cost_probe': 'DIAGNOSTIC_RECORDED' if args.cost_probe else 'NOT_RUN',
               'full_reactor': 'NOT_RUN', 'openjdk_jtreg_hotspot': 'NOT_RUN',
               'universal_performance_claim': False, 'phase_advance': False,
               'active_phase': 'STRING', 'target_baseline': 'eda1ae0733952394851c3580f8efb7624563b98c'}
    (evidence / 'qualification.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))


if __name__ == '__main__':
    main()
