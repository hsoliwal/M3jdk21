#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Modified 2026-10-21: Execute existing and new module contracts on host and linked images.
"""Compile the existing file module, run its independent contract, and check a linked image."""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--classes', type=Path, help='Reuse the classes already compiled by Maven')
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    toolchain = args.java_home.resolve()
    suffix = '.exe' if os.name == 'nt' else ''
    commands = []
    receipt_path = output / 'receipt.json'
    receipt = {
        'schema': 'M3_FILE_DELTA_CONTRACT_V1',
        'state': 'STARTED',
        'run_id': str(uuid.uuid4()),
        'started_utc': datetime.now(timezone.utc).isoformat(),
        'scope': 'com.m3.file module and direct compiler fixture',
        'class_origin': 'CALLER_PROVIDED' if args.classes else 'COMPILED_BY_THIS_RUN',
        'source_class_correspondence': ('EXTERNAL_BUILD_RESPONSIBILITY' if args.classes
                                        else 'THIS_RUN_JAVAC_COMMAND'),
        'full_m3jdk21_bootstrap': 'NOT_RUN',
        'maven_fork_execution': 'SEPARATE_GATE',
        'semantic_graph_coverage': 'FIXTURE_ONLY_NO_PRODUCTION_COMPLETENESS_CLAIM',
        'hosted_exact_head': 'PENDING',
    }

    def write_receipt():
        # Replace a preceding run's result before starting work, including when
        # this run later fails to launch a tool or times out.
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(mode='w', encoding='utf-8', dir=output,
                                             prefix='.m3-file-receipt-', delete=False) as stream:
                temporary = Path(stream.name)
                json.dump(receipt, stream, indent=2)
                stream.write('\n')
            temporary.replace(receipt_path)
        finally:
            if temporary is not None:
                temporary.unlink(missing_ok=True)

    def write_commands():
        (output / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n', encoding='utf-8')

    def hashes(paths, base):
        return {path.relative_to(base).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
                for path in sorted(paths)}

    def input_paths():
        return [*sorted((root / 'src/main/java').rglob('*.java')),
                *sorted((root / 'verification/src/test/java').rglob('*.java')),
                *sorted((root / 'src/test/java').rglob('*.java')),
                Path(__file__).resolve()]

    def class_hashes(directory):
        return hashes(directory.rglob('*.class'), directory)

    def run(label, command):
        command = [str(part) for part in command]
        entry = {'label': label, 'command': command, 'state': 'RUNNING'}
        commands.append(entry)
        write_commands()
        log = output / (label + '.log')
        log.write_text('', encoding='utf-8')
        try:
            result = subprocess.run(command, cwd=root, text=True, stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT, timeout=180, check=False)
        except BaseException as failure:
            captured = getattr(failure, 'stdout', None) or ''
            if isinstance(captured, bytes):
                captured = captured.decode('utf-8', errors='replace')
            entry.update(state='TIMED_OUT' if isinstance(failure, subprocess.TimeoutExpired)
                         else 'FAILED_TO_START' if isinstance(failure, OSError) else 'INTERRUPTED',
                         exit_code=None, exception_type=type(failure).__name__, error=str(failure))
            log.write_text(captured + '\n' + type(failure).__name__ + ': ' + str(failure) + '\n',
                           encoding='utf-8')
            write_commands()
            raise
        (output / (label + '.log')).write_text(result.stdout, encoding='utf-8')
        entry.update(state='FINISHED', exit_code=result.returncode)
        write_commands()
        if result.returncode:
            raise RuntimeError(label + ' failed:\n' + result.stdout)
        print(label + ': ' + result.stdout.strip())
        return result.stdout

    def tool(name):
        return toolchain / 'bin' / (name + suffix)

    write_receipt()
    try:
        write_commands()
        inputs = hashes(input_paths(), root)
        receipt['inputs'] = inputs
        sources = sorted((root / 'verification/src/test/java').rglob('*.java'))
        if not sources:
            raise RuntimeError('no independent file contracts')
        classes = args.classes.resolve() if args.classes else output / 'classes'
        if args.classes:
            if not (classes / 'module-info.class').is_file():
                raise RuntimeError('missing original com.m3.file module descriptor')
            admitted_classes = class_hashes(classes)
            receipt['class_files'] = admitted_classes
        write_receipt()
        run('java-version', [tool('java'), '-version'])
        if args.classes is None:
            if classes.exists():
                if classes.is_symlink() or classes.parent != output:
                    raise RuntimeError('unsafe module classes path')
                shutil.rmtree(classes)
            classes.mkdir()
            run('compile-module', [tool('javac'), '--release', '21', '-Xlint:all,-module', '-Werror',
                                   '-d', classes, *sorted((root / 'src/main/java').rglob('*.java'))])
            if not (classes / 'module-info.class').is_file():
                raise RuntimeError('missing original com.m3.file module descriptor')
            admitted_classes = class_hashes(classes)
            receipt['class_files'] = admitted_classes
            write_receipt()
        module_jar = output / 'm3-file.jar'
        run('package-module', [tool('jar'), '--create', '--file', module_jar, '-C', classes, '.'])
        admitted_jar = hashlib.sha256(module_jar.read_bytes()).hexdigest()
        receipt['jar_sha256'] = admitted_jar
        descriptor = run('module-descriptor', [tool('jar'), '--describe-module', '--file', module_jar])
        if 'com.m3.file' not in descriptor or 'exports com.m3.file' not in descriptor:
            raise RuntimeError('wrong product module descriptor')
        tests = output / 'tests'
        if tests.exists():
            if tests.is_symlink() or tests.parent != output:
                raise RuntimeError('unsafe contract classes path')
            shutil.rmtree(tests)
        tests.mkdir()
        run('compile-contract', [tool('javac'), '--release', '21', '-Xlint:all', '-Werror',
                                 '--module-path', module_jar, '--add-modules', 'com.m3.file',
                                 '-d', tests, *sources])
        admitted_tests = class_hashes(tests)
        receipt['contract_class_files'] = admitted_tests
        main_class = 'com.m3.file.test.M3FileDeltaContract'
        host = run('host-contract', [tool('java'), '--module-path', module_jar, '--add-modules',
                                    'com.m3.file,jdk.compiler', '-cp', tests, main_class])
        if 'M3FileDeltaContract: PASS;' not in host:
            raise RuntimeError('contract success marker missing')
        receipt['host_result'] = host.strip()
        module_tests = output / 'module-tests'
        if module_tests.exists():
            if module_tests.is_symlink() or module_tests.parent != output:
                raise RuntimeError('unsafe module contract classes path')
            shutil.rmtree(module_tests)
        module_tests.mkdir()
        test_sources = sorted((root / 'src/test/java').rglob('*.java'))
        contracts = {
            'M3FileDeltaLedgerContract': 'M3_FILE_DELTA_PASS',
            'M3FileMoveCandidatesContract': 'M3_FILE_MOVES_PASS',
            'M3FileObservationContract': 'M3_FILE_OBSERVATION_PASS',
            'M3BuildImpactPlanContract': 'M3_BUILD_IMPACT_PASS',
            'M3FileBuildImpactContract': 'M3_FILE_IMPACT_PASS',
            'M3ReactorImpactPlanContract': 'M3_REACTOR_IMPACT_PASS',
            'M3FileStreamsContract': 'M3_FILE_SHADOW_STREAM_PASS',
            'M3ShadowStreamsContract': 'M3_SHADOW_STREAM_PASS',
            'M3FileWatcherContract': 'M3_FILE_WATCHER_PASS',
            'M3FileRangeContract': 'M3_FILE_RANGES_PASS',
            'M3ProjectDeltaPermutationContract': 'M3_PROJECT_DELTA_PERMUTATION_PASS',
            'M3ResourceLocksContract': 'M3_RESOURCE_LOCKS_PASS',
            'M3ResourceLocksTimedContract': 'M3_TIMED_RESOURCE_LOCKS_PASS',
        }
        if {path.stem for path in test_sources} != set(contracts):
            raise RuntimeError('module contract inventory changed; review each executable contract')
        run('compile-module-contracts', [tool('javac'), '--release', '21', '-Xlint:all,-module', '-Werror',
                '--module-path', module_jar, '--patch-module', 'com.m3.file=' + str(root / 'src/test/java'),
                '-d', module_tests, *test_sources])
        module_test_hashes = class_hashes(module_tests)
        receipt['module_contract_class_files'] = module_test_hashes
        temporary_tests = output / 'runtime-temporary'
        temporary_tests.mkdir(exist_ok=True)

        def module_contracts(java, lane, module_path):
            outputs = {}
            for name, marker in contracts.items():
                command = [java, '-Djava.io.tmpdir=' + str(temporary_tests)]
                if module_path:
                    command += ['--module-path', module_path]
                command += ['--add-modules', 'com.m3.file,jdk.compiler,jdk.zipfs', '--patch-module',
                            'com.m3.file=' + str(module_tests), '-m', 'com.m3.file/com.m3.file.' + name]
                text = run(lane + '-' + name, command)
                if marker not in text:
                    raise RuntimeError('contract success marker missing: ' + name)
                outputs[name] = text.strip()
            return outputs

        receipt['host_module_contracts'] = module_contracts(tool('java'), 'host', module_jar)
        write_receipt()
        image = output / 'image'
        if image.exists():
            if image.is_symlink() or image.parent != output:
                raise RuntimeError('unsafe test image path')
            shutil.rmtree(image)
        run('link-image', [tool('jlink'), '--module-path', os.pathsep.join((str(toolchain / 'jmods'),
                                 str(module_jar))), '--add-modules', 'com.m3.file,jdk.compiler,jdk.zipfs',
                                 '--output', image, '--no-header-files', '--no-man-pages'])
        modules = run('image-modules', [image / 'bin' / ('java' + suffix), '--list-modules'])
        if 'com.m3.file' not in {line.partition('@')[0] for line in modules.splitlines()}:
            raise RuntimeError('com.m3.file is absent from linked image')
        linked = run('image-contract', [image / 'bin' / ('java' + suffix), '--add-modules',
                                       'com.m3.file,jdk.compiler', '-cp', tests, main_class])
        if 'M3FileDeltaContract: PASS;' not in linked:
            raise RuntimeError('linked-image contract success marker missing')
        receipt['image_module_contracts'] = module_contracts(image / 'bin' / ('java' + suffix), 'image', None)
        if class_hashes(module_tests) != module_test_hashes:
            raise RuntimeError('compiled module contract classes changed during qualification')
        if hashes(input_paths(), root) != inputs:
            raise RuntimeError('source, contract or verifier changed during qualification')
        if class_hashes(classes) != admitted_classes:
            raise RuntimeError('admitted module class bytes changed during qualification')
        if class_hashes(tests) != admitted_tests:
            raise RuntimeError('compiled contract class bytes changed during qualification')
        if hashlib.sha256(module_jar.read_bytes()).hexdigest() != admitted_jar:
            raise RuntimeError('packaged module bytes changed during qualification')
        receipt.update(state='PASSED', linked_image_result=linked.strip(),
                       inputs_and_classes_unchanged=True,
                       finished_utc=datetime.now(timezone.utc).isoformat())
        write_receipt()
        print('M3_FILE_DELTA_MODULE_PASS')
    except BaseException as failure:
        receipt.update(state='FAILED', error={'type': type(failure).__name__, 'message': str(failure)},
                       finished_utc=datetime.now(timezone.utc).isoformat())
        write_receipt()
        raise


if __name__ == '__main__':
    main()
