#!/usr/bin/env python3
"""Qualify the complete M3JDK21 collections module with its unchanged Maven POM.

SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
Adapted from m3/collections/verify.py in hsoliwal/M3JDK21 at
a3e88976ee8664d1ef5d0cade96aec1206b2133d. Canonical recipe/tooling ownership:
hsoliwal/com.synexia, M3CollectionRefinement20261019.

This is Python-stdlib build orchestration, not a source transformation engine.
It copies the complete module, excluding its top-level target directory, into
a fresh output directory; no source file or POM in the input module is changed.
The required real JUnit console is pinned by the receiver's verify_donation.py.
Use --offline --maven-repository with a provisioned, provenance-verified cache;
this script neither substitutes dependencies nor changes their coordinates.

--probe-plan names a reviewed JSON file for the current source epoch, with
schema M3_COLLECTION_REFINEMENT_PROBES_V1 and nonempty java, missing and native
arrays. Each case has a class name, an args array, and optional stdout_lines
containing exact frozen observations. Simple class names use com.m3.collections.
Old probe assumptions and callback transcripts must be reconciled before reuse.
"""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
import zipfile


JUNIT_SHA256 = '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a'
PACKAGE = 'com.m3.collections.'
NATIVE_LIBRARIES = (
    ('libm3_collections_mask.so', (
        ('m3_index_mask_compactor.c', 'M3IndexMaskCompactor'),
        ('m3_index_mask_window.c', 'M3IndexMaskWindow'),
    )),
    ('libm3_collections_pathmask.so', (
        ('m3_index_path_masks.c', 'M3IndexPathMasks'),
    )),
)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def identity(path):
    data = path.read_bytes()
    return {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
            'git_blob_sha1': hashlib.sha1(
                b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()}


def inventory(root):
    """Complete module input set; top-level build output and Git metadata are excluded."""
    rows = {}
    for directory, children, files in os.walk(root, followlinks=False):
        here = Path(directory)
        if here == root:
            children[:] = [name for name in children if name not in ('target', '.git')]
            files = [name for name in files if name != '.git']
        children.sort()
        for name in children + sorted(files):
            path = here / name
            require(not path.is_symlink(), 'symbolic link in module input: ' + str(path))
            if name in files:
                require(path.is_file(), 'non-file module input: ' + str(path))
                rows[path.relative_to(root).as_posix()] = identity(path)
    return dict(sorted(rows.items()))


def json_write(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + '\n', encoding='utf-8')


def reject_symlink_ancestors(path):
    for ancestor in (path, *path.parents):
        require(not ancestor.is_symlink(), 'symbolic link in output path: ' + str(ancestor))


def resolve_tool(name):
    found = shutil.which(str(name))
    require(found is not None, 'required executable unavailable: ' + str(name))
    return Path(found).resolve(strict=True)


def load_probe_plan(path):
    plan = json.loads(path.read_text(encoding='utf-8'))
    require(isinstance(plan, dict) and plan.get('schema') == 'M3_COLLECTION_REFINEMENT_PROBES_V1',
            'unsupported probe-plan schema')
    normalized = {}
    for phase in ('java', 'missing', 'native'):
        cases = plan.get(phase)
        require(isinstance(cases, list) and bool(cases), 'probe phase must contain reviewed cases: ' + phase)
        normalized[phase] = []
        seen = set()
        for case in cases:
            require(isinstance(case, dict) and set(case) <= {'class', 'args', 'stdout_lines'},
                    'unsupported probe case fields: ' + phase)
            name, arguments, lines = case.get('class'), case.get('args'), case.get('stdout_lines', [])
            require(isinstance(name, str) and re.fullmatch(r'[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*', name),
                    'invalid Java probe class')
            require(isinstance(arguments, list) and all(isinstance(arg, str) for arg in arguments),
                    'probe args must be an explicit string array')
            require(isinstance(lines, list) and all(isinstance(line, str) and '\n' not in line for line in lines),
                    'frozen stdout_lines must contain complete individual lines')
            if '.' not in name:
                name = PACKAGE + name
            key = (name, tuple(arguments))
            require(key not in seen, 'duplicate probe case: ' + phase + ' ' + name)
            seen.add(key)
            normalized[phase].append({'class': name, 'args': arguments, 'stdout_lines': lines})
    return normalized


def test_reports(directory):
    paths = sorted(directory.glob('TEST-*.xml'))
    require(bool(paths), 'no actual test reports: ' + str(directory))
    result = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0,
              'reports': {}, 'runtime_jars': {}}
    for path in paths:
        suite = ET.parse(path).getroot()
        require(suite.tag == 'testsuite', 'unexpected test report schema: ' + str(path))
        for key in ('tests', 'failures', 'errors', 'skipped'):
            result[key] += int(suite.get(key, '0'))
        result['reports'][path.name] = identity(path)
        for prop in suite.findall('./properties/property'):
            if prop.get('name') not in ('java.class.path', 'surefire.test.class.path'):
                continue
            for entry in prop.get('value', '').split(os.pathsep):
                jar = Path(entry)
                if jar.suffix == '.jar' and jar.is_file():
                    result['runtime_jars'][str(jar.resolve())] = identity(jar)
    require(result['tests'] > 0, 'test discovery ran zero tests: ' + str(directory))
    require(result['tests'] > result['skipped'], 'every discovered test was skipped')
    require(result['failures'] == 0 and result['errors'] == 0, 'failed test reports')
    return result


class Verification:
    def __init__(self, output, env, timeout):
        self.output, self.env, self.timeout = output, env, timeout
        self.steps = []
        self.receipt = {
            'schema': 'M3_COLLECTION_REFINEMENT_TARGET_VERIFICATION_V1',
            'status': 'RUNNING', 'started_utc': datetime.now(timezone.utc).isoformat(),
            'scope': 'Complete collections-module Maven/Java compilation and JUnit execution; '
                     'declared Java, missing-backend and two-library JNI probes.',
            'not_run_by_this_tool': ['Synexia recipe/SDK qualification', 'public-API baseline comparison',
                                     'performance qualification', 'full Synexia parent reactor',
                                     'OpenJDK/HotSpot build and jtreg'],
            'steps': self.steps,
        }
        (output / 'logs').mkdir()
        self.save()

    def save(self):
        json_write(self.output / 'receipt.json', self.receipt)

    def run(self, name, args, cwd):
        command = [str(arg) for arg in args]
        print(name, flush=True)
        started = time.monotonic()
        log = self.output / 'logs' / (name + '.log')
        step = {'name': name, 'command': command, 'cwd': str(cwd), 'status': 'RUNNING'}
        self.steps.append(step)
        self.save()
        process = None
        output = ''
        try:
            process = subprocess.Popen(command, cwd=cwd, env=self.env, text=True,
                                       encoding='utf-8', errors='replace',
                                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                       start_new_session=True)
            try:
                output, _ = process.communicate(timeout=self.timeout)
            except (subprocess.TimeoutExpired, KeyboardInterrupt):
                try:
                    os.killpg(process.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                output, _ = process.communicate()
                raise
            step['exit_code'] = process.returncode
            require(process.returncode == 0, name + ' exited ' + str(process.returncode))
            step['status'] = 'PASS'
            print(output[-1800:], end='' if output.endswith('\n') else '\n', flush=True)
            return output
        except BaseException as failure:
            step['status'] = 'FAILED'
            step['error'] = type(failure).__name__ + ': ' + str(failure)
            if process is not None:
                step['exit_code'] = process.returncode
            print(output[-12000:], flush=True)
            raise
        finally:
            log.write_text('$ ' + repr(command) + '\nCWD=' + str(cwd) + '\n' + output
                           + '\nEXIT=' + str(step.get('exit_code', 'NOT_STARTED')) + '\n',
                           encoding='utf-8')
            step['elapsed_seconds'] = time.monotonic() - started
            step['log'] = {'path': str(log.relative_to(self.output)), **identity(log)}
            self.save()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    location = parser.add_mutually_exclusive_group()
    location.add_argument('--module', type=Path, help='current collections module directory')
    location.add_argument('--source-root', type=Path, help='current M3JDK21 checkout; uses m3/collections')
    parser.add_argument('--output', type=Path, help='fresh output directory; existing paths are refused')
    parser.add_argument('--probe-plan', type=Path, required=True, help='reviewed probe cases for the current source epoch')
    parser.add_argument('--pom-sha256', help='optional current POM source seal; the POM is never rewritten')
    parser.add_argument('--java-home', type=Path, default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--maven', default=os.environ.get('MVN', 'mvn'))
    parser.add_argument('--maven-repository', type=Path)
    parser.add_argument('--offline', action='store_true')
    parser.add_argument('--junit-console', type=Path, default=os.environ.get('JUNIT_CONSOLE_JAR'))
    parser.add_argument('--cc', default=os.environ.get('CC', 'gcc'))
    parser.add_argument('--ubsan', action='store_true', help='build JNI with fatal undefined-behavior sanitizer')
    parser.add_argument('--timeout-seconds', type=float, default=900,
                        help='maximum duration of each subprocess; default 900 seconds')
    options = parser.parse_args()
    require(sys.platform.startswith('linux'), 'this complete JNI qualification requires Linux')
    require(options.timeout_seconds > 0, '--timeout-seconds must be positive')
    require(options.java_home is not None, 'set JAVA_HOME to a real JDK21 or use --java-home')
    require(options.junit_console is not None, 'provide the pinned real JUnit console JAR')
    module = options.module or (options.source_root / 'm3/collections' if options.source_root is not None
                                else Path(__file__).resolve().parents[2])
    module = module.expanduser().resolve(strict=True)
    java_home = options.java_home.expanduser().resolve(strict=True)
    console = options.junit_console.expanduser().resolve(strict=True)
    probe_plan = options.probe_plan.expanduser().resolve(strict=True)
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    output = (options.output or module / 'target' / 'collection-refinement-20261019' / stamp)
    output = output.expanduser().absolute()
    reject_symlink_ancestors(output)
    output = output.resolve(strict=False)
    require(output != module and not module.is_relative_to(output), 'output may not contain the input module')
    require(not output.is_relative_to(module) or output.is_relative_to(module / 'target'),
            'output inside the input module must be beneath its top-level target directory')
    output.mkdir(parents=True, exist_ok=False)
    env = dict(os.environ, JAVA_HOME=str(java_home), LC_ALL='C', TZ='UTC')
    env['PATH'] = str(java_home / 'bin') + os.pathsep + env.get('PATH', '')
    verification = Verification(output, env, options.timeout_seconds)
    receipt = verification.receipt
    frozen = None
    snapshot = output / 'module'
    try:
        for variable in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH'):
            require(not env.get(variable), 'unset implicit Java injection before qualification: ' + variable)
        java, javac = java_home / 'bin/java', java_home / 'bin/javac'
        maven, cc, nm = resolve_tool(options.maven), resolve_tool(options.cc), resolve_tool('nm')
        tool_paths = {'runner': Path(__file__).resolve(), 'python': Path(sys.executable).resolve(),
                      'java': java, 'javac': javac, 'jdk_release': java_home / 'release',
                      'jni_header': java_home / 'include/jni.h',
                      'jni_platform_header': java_home / 'include/linux/jni_md.h',
                      'jvm': java_home / 'lib/server/libjvm.so', 'maven': maven, 'gcc': cc, 'nm': nm,
                      'junit_console': console, 'probe_plan': probe_plan}
        receipt['toolchain'] = {name: {'path': str(path), **identity(path)} for name, path in tool_paths.items()}
        probes = load_probe_plan(probe_plan)
        receipt['declared_probes'] = probes
        receipt['python_version'] = sys.version
        receipt['ubsan'] = 'REQUESTED' if options.ubsan else 'NOT_REQUESTED'
        require(identity(console)['sha256'] == JUNIT_SHA256, 'JUnit console does not match the receiver pin')
        with zipfile.ZipFile(console) as jar:
            require(jar.testzip() is None, 'JUnit console ZIP integrity failure')
            require('org/junit/platform/console/ConsoleLauncher.class' in jar.namelist(), 'missing real console launcher')
        java_version = verification.run('01-java-version', [java, '-version'], output)
        javac_version = verification.run('02-javac-version', [javac, '-version'], output)
        require(re.search(r'version "21(?:[.\-+"]|$)', java_version), 'runtime must be JDK21')
        require(re.search(r'\bjavac 21(?:[.\-+]|$)', javac_version), 'compiler must be JDK21')
        maven_version = verification.run('03-maven-version', [maven, '-version'], output)
        require(re.search(r'Java version: 21(?:[.\-+,]|$)', maven_version), 'Maven must use JDK21')
        gcc_version = verification.run('04-gcc-version', [cc, '--version'], output)
        require('gcc' in gcc_version.splitlines()[0].lower(), '--cc must select GCC')
        verification.run('05-nm-version', [nm, '--version'], output)
        frozen = inventory(module)
        require('pom.xml' in frozen, 'current collections POM required')
        if options.pom_sha256 is not None:
            require(frozen['pom.xml']['sha256'] == options.pom_sha256,
                    'the collections POM differs from the supplied current source seal')
        require('src/main/java/module-info.java' in frozen, 'complete modular source input required')
        receipt['module'] = str(module)
        receipt['source_file_count'] = len(frozen)
        json_write(output / 'sources-before.json', frozen)
        receipt['sources_before'] = identity(output / 'sources-before.json')
        for relative, expected in frozen.items():
            source = module / relative
            require(identity(source) == expected, 'source drift before copying: ' + relative)
            destination = snapshot / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
            require(identity(destination) == expected, 'snapshot differs: ' + relative)
        require(inventory(module) == frozen and inventory(snapshot) == frozen, 'source drift during snapshot')
        missing, native = output / 'missing-native', output / 'native'
        missing.mkdir()
        native.mkdir()
        maven_args = [maven, '-B', '-ntp']
        if options.offline:
            maven_args.append('-o')
        if options.maven_repository is not None:
            repository = options.maven_repository.expanduser().resolve(strict=True)
            require(repository.is_dir(), '--maven-repository must be a provisioned directory')
            maven_args.append('-Dmaven.repo.local=' + str(repository))
            receipt['maven_repository'] = str(repository)
        receipt['maven_offline'] = options.offline
        verification.run('06-maven-test', maven_args + ['-f', snapshot / 'pom.xml', 'test'], snapshot)
        receipt['maven_tests'] = test_reports(snapshot / 'target/surefire-reports')
        main_sources = sorted((snapshot / 'src/main/java').rglob('*.java'))
        test_sources = sorted((snapshot / 'src/test/java').rglob('*.java'))
        require(bool(main_sources) and bool(test_sources), 'complete main and test source trees required')
        receipt['java_source_counts'] = {'main': len(main_sources), 'test': len(test_sources)}
        classes, test_classes, headers = output / 'classes', output / 'test-classes', output / 'headers'
        for directory in (classes, test_classes, headers):
            directory.mkdir()
        javac_args = [javac, '--release', '21', '-Xlint:all,-module', '-Werror']
        verification.run('07-javac-main-and-headers',
                         javac_args + ['-h', headers, '-d', classes] + main_sources, snapshot)
        verification.run('08-javac-all-tests',
                         javac_args + ['-cp', str(classes) + os.pathsep + str(console), '-d', test_classes]
                         + test_sources, snapshot)
        # Preserve the unchanged POM's generated resources without reusing Maven class files.
        receipt['maven_resources'] = {}
        for name, destination in (('classes', classes), ('test-classes', test_classes)):
            resource_root = snapshot / 'target' / name
            for resource in sorted(resource_root.rglob('*')):
                require(not resource.is_symlink(), 'symbolic link in Maven resources: ' + str(resource))
                if not resource.is_file() or resource.suffix == '.class':
                    continue
                relative = resource.relative_to(resource_root)
                copied = destination / relative
                require(not copied.exists() or identity(copied) == identity(resource),
                        'Maven resource conflicts with independent compiler output: ' + str(relative))
                copied.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(resource, copied)
                receipt['maven_resources'][name + '/' + relative.as_posix()] = identity(copied)
        classpath = str(classes) + os.pathsep + str(test_classes)
        verification.run('09-junit-console',
                         [java, '-ea', '-Djava.library.path=' + str(missing), '-jar', console,
                          'execute', '--disable-banner', '--disable-ansi-colors', '--details=summary',
                          '--class-path', classpath, '--scan-class-path', '--fail-if-no-tests',
                          '--reports-dir', output / 'junit-reports'], snapshot)
        receipt['console_tests'] = test_reports(output / 'junit-reports')
        for phase in ('java', 'missing'):
            for ordinal, case in enumerate(probes[phase], start=1):
                name, arguments = case['class'], case['args']
                observed = verification.run(phase + '-' + str(ordinal) + '-' + name,
                                            [java, '-ea', '-Djava.library.path=' + str(missing),
                                             '-cp', classpath, name, *arguments], snapshot)
                for line in case['stdout_lines']:
                    require(line in observed.splitlines(), 'frozen observation changed: ' + name)
        receipt['native_libraries'] = {}
        for library_name, owners in NATIVE_LIBRARIES:
            library = native / library_name
            flags = [cc, '-std=c17', '-O2', '-Wall', '-Wextra', '-Werror', '-fPIC',
                     '-I', java_home / 'include', '-I', java_home / 'include/linux']
            if options.ubsan:
                flags += ['-fsanitize=undefined', '-fno-sanitize-recover=all']
            sources, declarations = [], []
            for filename, owner in owners:
                source = snapshot / 'src/main/native/collections' / filename
                header = headers / ('com_m3_collections_' + owner + '.h')
                verification.run('native-syntax-' + owner,
                                 flags + ['-include', header, '-fsyntax-only', source], snapshot)
                sources.append(source)
                declarations.append(header)
            included = [argument for header in declarations for argument in ('-include', header)]
            verification.run('native-build-' + library_name,
                             flags + included + ['-shared', *sources, '-o', library], snapshot)
            symbols = verification.run('native-exports-' + library_name,
                                       [nm, '-D', '--defined-only', library], snapshot)
            actual = sorted(line.split()[-1] for line in symbols.splitlines()
                            if line.split() and line.split()[-1].startswith('Java_'))
            expected = sorted({symbol for header in declarations for symbol in
                               re.findall(r'\b(Java_[A-Za-z0-9_]+)\s*\(', header.read_text(encoding='utf-8'))})
            require(bool(expected) and actual == expected,
                    'JNI exports differ from generated declarations: ' + library_name)
            receipt['native_libraries'][library_name] = {
                'sources': {source.name: identity(source) for source in sources},
                'headers': {header.name: identity(header) for header in declarations},
                'library': identity(library), 'exports': actual}
        # This instrumented function table proves region-call behavior separately
        # from the actual JVM/JNI executions below; it does not emulate a JVM.
        table_source = snapshot / 'src/test/native/m3_mask_empty_regions.c'
        require(table_source.is_file(), 'reviewed native region fixture required')
        table_binary = native / 'm3_mask_empty_regions'
        table_flags = [cc, '-std=c17', '-O2', '-Wall', '-Wextra', '-Wpedantic', '-Werror',
                       '-I', java_home / 'include', '-I', java_home / 'include/linux']
        if options.ubsan:
            table_flags += ['-fsanitize=undefined', '-fno-sanitize-recover=all']
        verification.run('native-table-build', table_flags + [
            snapshot / 'src/main/native/collections/m3_index_mask_compactor.c',
            table_source, '-o', table_binary], snapshot)
        verification.run('native-table-regions', [table_binary, 'candidate'], snapshot)
        receipt['native_region_probe'] = {
            'scope': 'Instrumented JNI function table; not a JVM execution',
            'source': identity(table_source), 'binary': identity(table_binary)}
        for ordinal, case in enumerate(probes['native'], start=1):
            name, arguments = case['class'], case['args']
            observed = verification.run('native-runtime-' + str(ordinal) + '-' + name,
                                         [java, '-ea', '-Xcheck:jni', '-Djava.library.path=' + str(native),
                                          '-cp', classpath, name, *arguments], snapshot)
            for line in case['stdout_lines']:
                require(line in observed.splitlines(), 'frozen native observation changed: ' + name)
        after = inventory(module)
        json_write(output / 'sources-after.json', after)
        receipt['sources_after'] = identity(output / 'sources-after.json')
        require(after == frozen and inventory(snapshot) == frozen, 'module sources changed during qualification')
        for name, path in tool_paths.items():
            require(identity(path)['sha256'] == receipt['toolchain'][name]['sha256'], 'toolchain drift: ' + name)
        receipt['source_freshness'] = 'PASS'
        receipt['generated_headers'] = {path.name: identity(path) for path in sorted(headers.glob('*.h'))}
        receipt['status'] = 'PASS'
    except BaseException as failure:
        receipt['status'] = 'INTERRUPTED' if isinstance(failure, KeyboardInterrupt) else 'FAILED'
        receipt['failure'] = type(failure).__name__ + ': ' + str(failure)
        print(receipt['failure'], file=sys.stderr, flush=True)
    finally:
        receipt['finished_utc'] = datetime.now(timezone.utc).isoformat()
        verification.save()
    print('M3_COLLECTION_REFINEMENT_' + receipt['status'] + ' receipt=' + str(output / 'receipt.json')
          + ' sha256=' + identity(output / 'receipt.json')['sha256'], flush=True)
    return 0 if receipt['status'] == 'PASS' else 1


if __name__ == '__main__':
    def interrupted(signum, frame):
        raise KeyboardInterrupt('received signal ' + str(signum))
    signal.signal(signal.SIGTERM, interrupted)
    sys.exit(main())
