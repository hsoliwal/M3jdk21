#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Run unmodified upstream tests against the actual source-built API JARs offline."""
import argparse
import base64
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parent
TEST_LOCK_SHA256 = 'c4831679ecabf594e9f25574f72b7e3c162c29417e17fab894e0a06f7979b691'
spec = importlib.util.spec_from_file_location('graal_api_build', ROOT / 'build.py')
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)


def qualify(java_home, build, dependencies, output):
    source_lock = builder.verify_sources()
    java_home = java_home.resolve(strict=True)
    build = build.resolve(strict=True)
    dependencies = dependencies.resolve(strict=True)
    output = output.absolute()
    if output.exists():
        raise ValueError('output already exists')
    if builder.file_digest(ROOT / 'test-dependencies.json') != TEST_LOCK_SHA256:
        raise ValueError('test dependency lock mismatch')
    test_lock = json.loads((ROOT / 'test-dependencies.json').read_text())
    test_jars = []
    for entry in test_lock['entries']:
        jar = dependencies / entry['path']
        if builder.file_digest(jar) != entry['sha256']:
            raise ValueError('test dependency checksum mismatch: ' + entry['path'])
        if builder.file_digest(jar.with_suffix('.pom')) != entry['pom_sha256']:
            raise ValueError('test dependency POM checksum mismatch: ' + entry['path'])
        test_jars.append(jar)
    console = test_jars[0]
    with zipfile.ZipFile(console) as archive:
        bundled = archive.read('META-INF/shadowed-artifacts').decode().splitlines()
    for required in ['junit:junit:4.13.2', 'org.hamcrest:hamcrest-core:1.3',
                     'org.junit.jupiter:junit-jupiter-api:5.13.0']:
        if required not in bundled:
            raise ValueError('JUnit standalone bundle mismatch: ' + required)
    receipt = json.loads((build / 'legal/SOURCE_BUILD.json').read_text())
    if receipt['source_lock_sha256'] != builder.SOURCE_LOCK_SHA256:
        raise ValueError('build is from a different source lock')
    product_jars = []
    for item in receipt['products']:
        jar = build / item['file']
        if builder.file_digest(jar) != item['sha256']:
            raise ValueError('source-built product checksum mismatch: ' + item['file'])
        product_jars.append(jar)
    output.mkdir(parents=True, exist_ok=False)
    env = builder.clean_environment()
    steps = []
    results = []

    def run(name, command, cwd):
        completed = subprocess.run(list(map(str, command)), cwd=cwd, env=env,
                                   capture_output=True, text=True)
        log = output / (name + '.log')
        log.write_text(completed.stdout + completed.stderr)
        steps.append({'name': name, 'argv': list(map(str, command)), 'cwd': str(cwd), 'exit': completed.returncode,
                      'log_sha256': builder.file_digest(log)})
        if completed.returncode:
            raise RuntimeError(name + ' failed; see ' + str(log))

    modules = [('openjson', ROOT / 'upstream/openjson'),
               ('utils', ROOT / 'upstream/native-build-tools/common/utils'),
               ('graalvm-reachability-metadata', ROOT / 'upstream/native-build-tools/common/graalvm-reachability-metadata')]
    for name, module in modules:
        work = output / name
        classes = work / 'test-classes'
        classes.mkdir(parents=True)
        resources = work / 'src/test/resources'
        resources.mkdir(parents=True)
        prefix = module.relative_to(ROOT).as_posix() + '/src/test/resources/'
        for entry in source_lock['entries']:
            if not entry['path'].startswith(prefix):
                continue
            relative = entry['upstream_path'].split('/src/test/resources/', 1)[1] if '/src/test/resources/' in entry['upstream_path'] else entry['upstream_path'][len('src/test/resources/'):]
            destination = resources / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            original = (ROOT / entry['path']).read_bytes()
            if entry['encoding'] == 'base64':
                original = base64.b64decode(original)
            if builder.digest(original) != entry['original_sha256']:
                raise ValueError('decoded fixture checksum mismatch: ' + entry['path'])
            destination.write_bytes(original)
        classpath = os.pathsep.join(map(str, product_jars + test_jars))
        sources = sorted((module / 'src/test/java').rglob('*.java'))
        run(name + '-compile', [java_home / 'bin/javac', '--release', '17', '-encoding', 'UTF-8',
                               '-classpath', classpath, '-d', classes, *sources], work)
        runtime_classpath = os.pathsep.join(map(str, product_jars + test_jars[1:] + [classes, resources]))
        reports = work / 'reports'
        run(name + '-tests', [java_home / 'bin/java', '-Djava.awt.headless=true', '-jar', console,
                             'execute', '--class-path', runtime_classpath, '--scan-class-path=' + str(classes),
                             '--include-classname=.*Test', '--reports-dir', reports, '--details=summary',
                             '--disable-ansi-colors', '--fail-if-no-tests'], work)
        totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
        for report in reports.glob('TEST-*.xml'):
            root = ET.parse(report).getroot()
            for field in totals:
                totals[field] += int(root.attrib.get(field, 0))
        if totals['tests'] == 0 or totals['failures'] or totals['errors']:
            raise ValueError('upstream test report failure: ' + name)
        results.append({'module': name, 'original_test_java_files': len(sources), **totals})
    builder.verify_sources()
    result = {'schema': 'M3_GRAAL_BUILD_API_UPSTREAM_TESTS_V1', 'status': 'PASS',
              'source_lock_sha256': builder.SOURCE_LOCK_SHA256, 'test_lock_sha256': TEST_LOCK_SHA256,
              'java_home': str(java_home), 'java_modules_sha256': builder.file_digest(java_home / 'lib/modules'),
              'products': receipt['products'], 'results': results, 'steps': steps,
              'bundled_test_artifacts': bundled,
              'bounds': ['Original common tests include local loopback HTTP servers and fake URL connections',
                         'No Maven/Gradle invocation or remote dependency resolution',
                         'Default upstream unit-test naming excludes the original OpenJSON performance class ToStringPT',
                         'No Native Image compiler or agent was executed; installed API contract is a separate gate']}
    (output / 'QUALIFICATION.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'status': 'PASS', 'results': results}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--build', type=Path, required=True)
    parser.add_argument('--dependencies', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    qualify(args.java_home, args.build, args.dependencies, args.output)
