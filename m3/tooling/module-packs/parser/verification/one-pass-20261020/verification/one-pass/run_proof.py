#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Compose already-qualified generated JMODs with original SQLite JNI; no production overlays."""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import zipfile


SQLITE_SHA = 'f5f5404fa5a60f9e0b15e7bea2ea2d137e255f01babd0bfcb9dafcd2e3bf9cd2'
SLF4J_SHA = 'e7c2a48e8515ba1f49fa637d57b4e2f590b3f5bd97407ac699c3aa5efb1204a9'
PACKER_SHA = '8acdc46eaa0cac0861384d6caf252d1a2f763c80d09b68932177e1dac598bfaf'
SQLITE_NATIVE = 'org/sqlite/native/Linux/x86_64/libsqlitejdbc.so'


def digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def seal(path):
    path = Path(path).resolve(strict=True)
    return {'path': str(path), 'bytes': path.stat().st_size, 'sha256': digest(path)}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def pinned(path, expected, label):
    path = Path(path).resolve(strict=True)
    require('authored' not in path.parts, label + ': authored input is not a generated artifact')
    require(re.fullmatch('[0-9a-f]{64}', expected or '') is not None, label + ': exact SHA-256 required')
    require(digest(path) == expected, label + ': SHA-256 drift')
    return path


def within(root, relative):
    path = (root / relative).resolve(strict=True)
    require(path.is_relative_to(root) and 'authored' not in path.parts, 'input escapes actual receiving root')
    return path


def read_json(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


def write_json(path, value):
    Path(path).write_text(json.dumps(value, indent=2, sort_keys=True) + '\n', encoding='utf-8')


def class_parity(jar, jmod):
    with zipfile.ZipFile(jar) as source, zipfile.ZipFile(jmod) as target:
        classes = [name for name in source.namelist()
                   if name.endswith('.class') and name != 'module-info.class'
                   and not name.startswith('META-INF/versions/')]
        require(classes, 'no actual production classes in ' + str(jar))
        for name in classes:
            require(source.read(name) == target.read('classes/' + name), 'JAR/JMOD class drift: ' + name)
        return len(classes)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk', required=True, type=Path)
    parser.add_argument('--receiving-root', required=True, type=Path)
    parser.add_argument('--fs-qualification', required=True, type=Path)
    parser.add_argument('--fs-sha256', required=True)
    parser.add_argument('--parser-qualification', required=True, type=Path)
    parser.add_argument('--parser-sha256', required=True)
    parser.add_argument('--sqlite-jar', required=True, type=Path)
    parser.add_argument('--slf4j-jar', required=True, type=Path)
    parser.add_argument('--out', required=True, type=Path)
    args = parser.parse_args()
    for name in ('jdk', 'receiving_root', 'fs_qualification', 'parser_qualification'):
        setattr(args, name, getattr(args, name).resolve(strict=True))
    args.out = args.out.absolute()
    require(not args.out.exists() and not args.out.is_symlink(), 'fresh output directory required')
    require('authored' not in args.receiving_root.parts, 'authored receiving root refused')

    root = Path(__file__).resolve().parent
    tracked = {Path(__file__).resolve(), *sorted((root / 'src').rglob('*.java'))}
    fs_receipt_path = args.fs_qualification / 'qualification.json'
    fs_inputs_path = args.fs_qualification / 'inputs.json'
    fs_receipt = read_json(fs_receipt_path)
    fs_inputs = read_json(fs_inputs_path)
    require(fs_receipt['status'] == 'PASS', 'filesystem qualification must pass')
    fs_jar = pinned(fs_receipt['jar']['path'], args.fs_sha256, 'generated filesystem JAR')
    require(fs_receipt['jar']['sha256'] == args.fs_sha256, 'filesystem receipt JAR pin mismatch')
    source_lock = pinned(fs_inputs['source']['source_lock'], fs_inputs['source']['source_lock_sha256'], 'filesystem source lock')
    tracked.update((fs_receipt_path, fs_inputs_path, fs_jar, source_lock))
    with source_lock.open(newline='', encoding='utf-8') as stream:
        for row in csv.DictReader(stream, delimiter='\t'):
            path = within(args.receiving_root / 'm3/fs', row['path'])
            require(digest(path) == row['sha256'], 'generated filesystem source drift: ' + row['path'])
            tracked.add(path)
    for row in fs_inputs['dependencies']:
        tracked.add(pinned(row['jar'], row['sha256'], row['module']))

    parser_state_path = args.parser_qualification / 'STATE.json'
    parser_inputs_path = args.parser_qualification / 'INPUTS.json'
    parser_state = read_json(parser_state_path)
    parser_inputs = read_json(parser_inputs_path)
    require(parser_state['completed'] == ['lint', 'compile', 'test', 'runtime']
            and parser_state['active'] is None and parser_state['failed'] is None,
            'all actual parser phases must pass before composed proof')
    require(parser_inputs['input_sha256'] == parser_state['input_sha256'], 'parser phase input seal mismatch')
    tracked.update((parser_state_path, parser_inputs_path))
    parser_runtime = None
    for phase in parser_state['completed']:
        path = args.parser_qualification / (phase + '-receipt.json')
        pinned(path, parser_state['receipts'][phase], 'parser ' + phase + ' receipt')
        receipt = read_json(path)
        require(receipt['status'] == 'PASS' and receipt['input_sha256'] == parser_inputs['input_sha256'],
                'parser receipt/input mismatch: ' + phase)
        tracked.add(path)
        if phase == 'runtime':
            parser_runtime = receipt
    for row in parser_inputs['inputs']:
        require(row['owner'] in ('repository', 'jdk'), 'unexpected parser input owner')
        base = args.receiving_root if row['owner'] == 'repository' else args.jdk
        path = within(base, row['path'])
        require(digest(path) == row['sha256'], 'qualified parser build input drift: ' + row['path'])
        tracked.add(path)
    parser_jar = pinned(args.parser_qualification / 'jdk.m3.parser.jar', args.parser_sha256, 'generated parser JAR')
    parser_jmod = args.parser_qualification / 'pack/jmods/jdk.m3.parser.jmod'
    for path in (parser_jar, parser_jmod):
        relative = path.relative_to(args.parser_qualification).as_posix()
        expected = [row['sha256'] for row in parser_runtime['artifacts'] if row['path'] == relative]
        require(len(expected) == 1 and digest(path) == expected[0], 'parser runtime artifact drift: ' + relative)
        tracked.add(path)
    sqlite = pinned(args.sqlite_jar, SQLITE_SHA, 'original SQLite JDBC 3.45.1.0')
    slf4j = pinned(args.slf4j_jar, SLF4J_SHA, 'original SLF4J API 2.0.13')
    packer = pinned(args.receiving_root / 'm3/tooling/jmod-recipes/tools/jmod_pack.py', PACKER_SHA, 'existing shared packer')
    tracked.update((sqlite, slf4j, packer))
    fs_jmods = sorted((args.fs_qualification / 'pack/jmods').glob('*.jmod'))
    require({path.stem for path in fs_jmods} == {'jdk.m3fs', 'commons.logging', 'org.apache.commons.codec',
            'org.apache.commons.compress', 'org.apache.commons.io', 'org.apache.commons.lang3'}, 'exact filesystem JMOD closure required')
    tracked.update(fs_jmods)
    parity = {'jdk.m3fs': class_parity(fs_jar, args.fs_qualification / 'pack/jmods/jdk.m3fs.jmod'),
              'jdk.m3.parser': class_parity(parser_jar, parser_jmod)}
    before = [seal(path) for path in sorted(tracked)]
    args.out.mkdir(parents=True, exist_ok=False)
    write_json(args.out / 'INPUTS.json', {'inputs': before, 'class_byte_parity': parity,
               'parser_input_sha256': parser_inputs['input_sha256'], 'fs_source_lock_sha256': digest(source_lock)})
    env = os.environ.copy()
    removed = []
    for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH', 'PYTHONPATH'):
        if key in env:
            removed.append(key)
            env.pop(key)
    env['PYTHONDONTWRITEBYTECODE'] = '1'
    commands = []
    result = {'schema': 'M3_ONE_PASS_QUALIFICATION_V1', 'status': 'RUNNING',
              'scope': 'Finite named-image generated-artifact proof. No production source overlay, new native engine, default Maven switch, or source-built OpenJDK claim.',
              'input_count': len(before), 'class_byte_parity': parity, 'removed_environment_names': removed,
              'source_built_m3jdk21': 'NOT_RUN', 'maven_reactor': 'NOT_RUN'}

    def run(label, argv, timeout=180):
        argv = [str(item) for item in argv]
        started = time.time()
        completed = subprocess.run(argv, cwd=root, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   text=True, encoding='utf-8', errors='replace', timeout=timeout)
        log = args.out / (label + '.log')
        log.write_text(completed.stdout, encoding='utf-8')
        commands.append({'label': label, 'argv': argv, 'cwd': str(root), 'exit_code': completed.returncode,
                         'seconds': round(time.time() - started, 3), 'log': seal(log)})
        write_json(args.out / 'COMMANDS.json', commands)
        print(label + ': exit=' + str(completed.returncode), flush=True)
        if completed.returncode:
            print(completed.stdout[-6000:], flush=True)
            raise RuntimeError(label + ' failed; retained exact command and log')
        return completed.stdout

    try:
        run('toolchain', [args.jdk / 'bin/java', '-version'])
        for label, jar in (('sqlite', sqlite), ('slf4j', slf4j)):
            run('describe-' + label, [args.jdk / 'bin/jar', '--describe-module', '--file', jar, '--release', '21'])
        legal = args.out / 'legal'
        legal.mkdir()
        resource_pins = []
        for module, jar in (('org.xerial.sqlitejdbc', sqlite), ('org.slf4j', slf4j)):
            with zipfile.ZipFile(jar) as archive:
                names = [name for name in archive.namelist() if not name.endswith('/')
                         and any(marker in name.rsplit('/', 1)[-1].upper() for marker in ('LICENSE', 'NOTICE', 'COPYING', 'DEPENDENCIES'))]
                require(names, 'original legal resources required for ' + module)
                for name in names:
                    value = archive.read(name)
                    target = legal / module / name
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(value)
                    resource_pins.append({'module': module, 'entry': name, 'sha256': hashlib.sha256(value).hexdigest(), 'kind': 'legal'})
                if module == 'org.xerial.sqlitejdbc':
                    resource_pins.append({'module': module, 'entry': SQLITE_NATIVE,
                        'sha256': hashlib.sha256(archive.read(SQLITE_NATIVE)).hexdigest(), 'kind': 'original_linux_x86_64_jni'})
        descriptor = args.out / 'EMPTY_DESCRIPTOR_MANIFEST.tsv'
        descriptor.write_text('module\tunit_jars\ttarget\n', encoding='utf-8')
        dependencies = args.out / 'JMOD_IMAGE.dependencies.tsv'
        dependencies.write_text('module\tjar\tdisposition\norg.xerial.sqlitejdbc\t' + str(sqlite)
                               + '\tMODULAR_JAR\norg.slf4j\t' + str(slf4j) + '\tMODULAR_JAR\n', encoding='utf-8')
        dependency_pack = args.out / 'dependency-pack'
        run('pack-original-jdbc-modules', [sys.executable, packer, '--image-tsv', dependencies,
            '--manifest', descriptor, '--layer-root', args.receiving_root, '--out', dependency_pack,
            '--jdk-home', args.jdk, '--release', '21', '--jmods-only', '--legal-notices', legal])
        module_dirs = [args.fs_qualification / 'pack/jmods', args.parser_qualification / 'pack/jmods', dependency_pack / 'jmods']
        module_path = os.pathsep.join(map(str, module_dirs))
        classes = args.out / 'classes'
        sources = sorted((root / 'src').rglob('*.java'))
        run('compile-proof', [args.jdk / 'bin/javac', '--release', '21', '-Xlint:all', '-Werror', '-proc:none',
            '-implicit:none', '--module-path', module_path, '-d', classes, *sources])
        proof_jar = args.out / 'm3fs.onepass.proof.jar'
        run('jar-proof', [args.jdk / 'bin/jar', '--create', '--file', proof_jar, '--main-class', 'proof.OnePassProof', '-C', classes, '.'])
        proof_manifest = args.out / 'JMOD_IMAGE.proof.tsv'
        proof_manifest.write_text('module\tjar\tdisposition\nm3fs.onepass.proof\t' + str(proof_jar) + '\tMODULAR_JAR\n', encoding='utf-8')
        proof_pack = args.out / 'proof-pack'
        run('pack-proof', [sys.executable, packer, '--image-tsv', proof_manifest, '--manifest', descriptor,
            '--layer-root', root, '--out', proof_pack, '--jdk-home', args.jdk, '--release', '21', '--jmods-only'])
        image = args.out / 'image'
        run('jlink-composed-image', [args.jdk / 'bin/jlink', '--module-path', os.pathsep.join(map(str,
            [args.jdk / 'jmods', *module_dirs, proof_pack / 'jmods'])), '--add-modules', 'm3fs.onepass.proof',
            '--output', image, '--no-header-files', '--no-man-pages', '--strip-debug'])
        run('validate-image', [image / 'bin/java', '--validate-modules'])
        listed = run('list-image-modules', [image / 'bin/java', '--list-modules'])
        modules = {line.split('@', 1)[0] for line in listed.splitlines()}
        for expected in ('jdk.m3fs', 'jdk.m3.parser', 'm3fs.onepass.proof', 'jdk.compiler',
                         'java.sql', 'org.xerial.sqlitejdbc', 'org.slf4j'):
            require(expected in modules, 'missing actual linked module: ' + expected)
        result['linked_modules'] = sorted(modules)
        all_jmods = [*fs_jmods, parser_jmod, *sorted((dependency_pack / 'jmods').glob('*.jmod')),
                     proof_pack / 'jmods/m3fs.onepass.proof.jmod']
        legal_count = 0
        for jmod in all_jmods:
            with zipfile.ZipFile(jmod) as archive:
                for name in archive.namelist():
                    if name.startswith('legal/') and not name.endswith('/'):
                        image_file = image / 'legal' / jmod.stem / name.removeprefix('legal/')
                        require(image_file.resolve(strict=True).is_relative_to(image), 'linked legal symlink escapes image')
                        require(image_file.read_bytes() == archive.read(name), 'linked legal resource drift: ' + str(image_file))
                        legal_count += 1
        extracted = args.out / 'resource-check'
        include = ','.join('glob:/' + row['module'] + '/' + row['entry'] for row in resource_pins)
        run('extract-original-jdbc-resources', [args.jdk / 'bin/jimage', 'extract', '--dir', extracted,
            '--include', include, image / 'lib/modules'])
        for row in resource_pins:
            jmod = dependency_pack / 'jmods' / (row['module'] + '.jmod')
            with zipfile.ZipFile(jmod) as archive:
                require(hashlib.sha256(archive.read('classes/' + row['entry'])).hexdigest() == row['sha256'],
                        'JMOD original resource drift: ' + row['entry'])
            require(digest(extracted / row['module'] / row['entry']) == row['sha256'],
                    'linked original resource drift: ' + row['entry'])
        write_json(args.out / 'LEGAL_AND_NATIVE_RESOURCES.json', {'original_resources': resource_pins,
                   'all_input_jmod_legal_entries_verified_in_image': legal_count})
        result['legal_entries_verified'] = legal_count
        result['original_jdbc_resources_verified'] = len(resource_pins)
        native_tmp = args.out / 'native-tmp'
        native_tmp.mkdir()
        output = run('one-pass-image', [image / 'bin/java', '-Xcheck:jni', '-Djava.io.tmpdir=' + str(native_tmp),
            '-m', 'm3fs.onepass.proof/proof.OnePassProof', args.out / 'work'], timeout=180)
        require('M3_ONE_PASS_GENERATED_IMAGE_PASS ' in output, 'actual named-image success marker missing')
        require(re.search(r'WARNING in native method|FATAL ERROR in native method|JNI DETECTED ERROR', output) is None,
                'JNI diagnostic appeared in runtime proof')
        probe = read_json(args.out / 'work/RESULTS.json')
        require(probe['status'] == 'PASS' and probe['sqlite_jni'] == 'ACTIVE', 'real JNI probe must pass')
        result['probe'] = probe
        result['jni_diagnostics'] = 0
        result['artifacts'] = [seal(proof_jar), seal(image / 'lib/modules'),
            seal(args.out / 'work/module/target/m3/facts.sqlite'), seal(args.out / 'work/RESULTS.json')]
        result['status'] = 'PASS'
    except Exception as failure:
        result['status'] = 'FAIL'
        result['failure'] = type(failure).__name__ + ': ' + str(failure)
        print(result['failure'], flush=True)
    finally:
        after = [seal(path) for path in sorted(tracked)]
        result['inputs_unchanged'] = before == after
        if before != after:
            result['status'] = 'FAIL'
            result['input_drift'] = {'before': before, 'after': after}
        write_json(args.out / 'INPUTS.after.json', {'inputs': after})
        result['commands'] = seal(args.out / 'COMMANDS.json') if (args.out / 'COMMANDS.json').exists() else None
        write_json(args.out / 'RESULTS.json', result)
    print('ONE_PASS_QUALIFICATION ' + result['status'], flush=True)
    return 0 if result['status'] == 'PASS' else 1


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except (ValueError, FileNotFoundError) as refused:
        raise SystemExit('ONE_PASS_REFUSED: ' + str(refused)) from refused
