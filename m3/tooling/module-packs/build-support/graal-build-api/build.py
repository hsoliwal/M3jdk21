#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Offline source build; installation remains owned by the existing tool_packs.py."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import tomllib

ROOT = Path(__file__).resolve().parent
SOURCE_LOCK_SHA256 = '459b391ccf9507d7fa7d49cb9b64d382244d8e5b0e7da0c468c7280195850259'
RUNTIME_ROOTS = ['java.base', 'java.desktop', 'java.net.http', 'jdk.zipfs']
JAR_DATE = '2026-10-20T00:00:00Z'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def file_digest(path):
    return digest(Path(path).read_bytes())


def source_path(relative):
    part = PurePosixPath(relative)
    if part.is_absolute() or '..' in part.parts or str(part) != relative:
        raise ValueError('unsafe source path: ' + relative)
    result = ROOT / relative
    if result.is_symlink() or any(parent.is_symlink() for parent in result.parents if parent != ROOT.parent):
        raise ValueError('symlink source: ' + relative)
    return result


def verify_sources():
    raw = (ROOT / 'source-lock.json').read_bytes()
    if digest(raw) != SOURCE_LOCK_SHA256:
        raise ValueError('source lock checksum mismatch')
    lock = json.loads(raw)
    expected = set()
    for entry in lock['entries']:
        path = source_path(entry['path'])
        payload = path.read_bytes()
        if digest(payload) != entry['sha256']:
            raise ValueError('source checksum mismatch: ' + entry['path'])
        original = base64.b64decode(payload, validate=False) if entry['encoding'] == 'base64' else payload
        blob = hashlib.sha1(b'blob ' + str(len(original)).encode() + b'\0' + original).hexdigest()
        if (digest(original) != entry['original_sha256'] or blob != entry['git_blob']
                or len(original) != entry['original_size']):
            raise ValueError('upstream identity mismatch: ' + entry['path'])
        expected.add(entry['path'])
    generated = lock['generated']
    candidate = source_path(generated['path']).read_bytes()
    versions = tomllib.loads(source_path(generated['catalogue']).read_text())['versions']
    constants = {
        'JUNIT_PLATFORM_NATIVE_VERSION': versions['nativeBuildTools'],
        'METADATA_REPO_VERSION': versions['metadataRepository'],
        'NBT_VERSION': versions['nativeBuildTools'],
    }
    expansion = source_path(generated['header']).read_bytes()
    expansion += b'\npackage org.graalvm.buildtools;\n\npublic class VersionInfo {\n'
    for name, value in constants.items():
        expansion += ('    public static final String ' + name + ' = "' + value + '";\n').encode()
    expansion += b'}\n'
    if (candidate != expansion or digest(candidate) != generated['sha256']
            or constants != generated['constants']):
        raise ValueError('generated VersionInfo differs from pinned upstream template/catalogue')
    expected.add(generated['path'])
    actual = set()
    for directory in ['upstream', 'generated-sources']:
        for path in (ROOT / directory).rglob('*'):
            if path.is_symlink():
                raise ValueError('symlink source: ' + str(path))
            if path.is_file():
                actual.add(path.relative_to(ROOT).as_posix())
    if actual != expected:
        raise ValueError('source inventory mismatch: ' + repr(sorted(actual ^ expected)))
    return lock


def clean_environment():
    env = dict(os.environ)
    for key in ['CLASSPATH', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS']:
        env.pop(key, None)
    return env


def build(java_home, output):
    lock = verify_sources()  # Refuse drift before creating output or starting a compiler.
    java_home = Path(java_home).resolve(strict=True)
    output = Path(output).absolute()
    if output.exists():
        raise ValueError('output already exists: ' + str(output))
    for name in ['java', 'javac', 'jar', 'jdeps']:
        if not (java_home / 'bin' / name).is_file():
            raise ValueError('missing tool: ' + name)
    env = clean_environment()
    version = subprocess.run([str(java_home / 'bin/javac'), '-version'], env=env,
                             capture_output=True, text=True, check=True)
    if not version.stdout.startswith('javac 21'):
        raise ValueError('Java 21 compiler required: ' + version.stdout + version.stderr)
    output.mkdir(parents=True, exist_ok=False)
    (output / 'logs').mkdir()
    (output / 'jars').mkdir()
    steps = []

    def run(name, args):
        completed = subprocess.run([str(arg) for arg in args], cwd=output, env=env,
                                   capture_output=True, text=True)
        log = completed.stdout + completed.stderr
        (output / 'logs' / (name + '.log')).write_text(log)
        steps.append({'name': name, 'argv': [str(arg) for arg in args], 'exit': completed.returncode})
        if completed.returncode:
            raise RuntimeError(name + ' failed; see ' + str(output / 'logs' / (name + '.log')))
        return completed.stdout

    run('runtime-version', [java_home / 'bin/java', '-version'])
    specifications = [
        ('openjson', '1.0.13', ROOT / 'upstream/openjson', [], 'LICENSE.txt', 'com.github.openjson'),
        ('utils', '1.1.13', ROOT / 'upstream/native-build-tools/common/utils', ['openjson'], 'LICENSE.txt', None),
        ('graalvm-reachability-metadata', '1.1.13', ROOT / 'upstream/native-build-tools/common/graalvm-reachability-metadata',
         ['openjson', 'utils'], 'LICENSE.txt', None),
    ]
    jars = {}
    products = []
    for name, version, module, dependencies, license_name, automatic_name in specifications:
        classes = output / 'classes' / name
        classes.mkdir(parents=True)
        sources = sorted((module / 'src/main/java').rglob('*.java'))
        if name == 'utils':
            sources.append(ROOT / lock['generated']['path'])
        args = [java_home / 'bin/javac', '--release', '17', '-encoding', 'UTF-8', '-g:none', '-d', classes]
        if dependencies:
            args += ['-classpath', os.pathsep.join(str(jars[item]) for item in dependencies)]
        run(name + '-compile', args + sources)
        resources = module / 'src/main/resources'
        if resources.exists():
            shutil.copytree(resources, classes, dirs_exist_ok=True)
        metadata = classes / 'META-INF'
        metadata.mkdir(exist_ok=True)
        shutil.copyfile(module / license_name, metadata / 'LICENSE')
        if (module / 'NOTICE.txt').is_file():
            shutil.copyfile(module / 'NOTICE.txt', metadata / 'NOTICE')
        manifest = 'Manifest-Version: 1.0\nImplementation-Title: ' + name + '\nImplementation-Version: ' + version + '\n'
        if automatic_name:
            manifest += 'Automatic-Module-Name: ' + automatic_name + '\n'
        manifest += '\n'
        manifest_path = output / (name + '-MANIFEST.MF')
        manifest_path.write_text(manifest)
        jar = output / 'jars' / (name + '-' + version + '.jar')
        run(name + '-jar', [java_home / 'bin/jar', '--create', '--file', jar, '--manifest', manifest_path,
                           '--date=' + JAR_DATE, '-C', classes, '.'])
        jars[name] = jar
        products.append({'name': name, 'version': version, 'source_files': len(sources),
                         'file': str(jar.relative_to(output)), 'sha256': file_digest(jar)})
    detected = run('jdeps', [java_home / 'bin/jdeps', '--multi-release', '17', '--recursive',
                            '--print-module-deps', '--class-path', os.pathsep.join(map(str, jars.values())),
                            *jars.values()]).strip().split(',')
    if set(detected) != {'java.base', 'java.desktop', 'java.net.http'}:
        raise ValueError('unexpected runtime module graph: ' + repr(detected))
    verify_sources()  # A concurrent source edit cannot acquire a successful build receipt.
    legal = output / 'legal'
    legal.mkdir()
    copies = {
        'native-build-tools-LICENSE': 'upstream/native-build-tools/LICENSE.txt',
        'utils-LICENSE': 'upstream/native-build-tools/common/utils/LICENSE.txt',
        'reachability-metadata-LICENSE': 'upstream/native-build-tools/common/graalvm-reachability-metadata/LICENSE.txt',
        'openjson-LICENSE': 'upstream/openjson/LICENSE.txt',
        'openjson-NOTICE': 'upstream/openjson/NOTICE.txt',
        'source-lock.json': 'source-lock.json',
    }
    for destination, source in copies.items():
        shutil.copyfile(ROOT / source, legal / destination)
    receipt = {
        'schema': 'M3_GRAAL_BUILD_API_BUILD_V1', 'status': 'SOURCE_BUILD_PASS',
        'upstream': lock['upstream'], 'dependency': lock['dependency'],
        'source_lock_sha256': SOURCE_LOCK_SHA256, 'builder_sha256': file_digest(__file__),
        'java_home': str(java_home), 'java_release': 17,
        'runtime': {name: file_digest(java_home / name) for name in ['bin/java', 'bin/javac', 'lib/modules']},
        'products': products, 'runtime_roots': RUNTIME_ROOTS, 'jdeps_roots': detected,
        'dynamic_provider_root': {'jdk.zipfs': 'Original JarScanner uses FileSystems.newFileSystem for JAR traversal'},
        'steps': steps,
        'bounds': ['No Maven/Gradle adapter runtime', 'No native-image compiler, hosted SDK or agent native library',
                   'No external repository classifier downloaded or bundled', 'Whole JDK bootstrap and AOT execution NOT_RUN',
                   'Tests and installed-image execution are separate gates'],
    }
    (legal / 'SOURCE_BUILD.json').write_text(json.dumps(receipt, indent=2) + '\n')
    assets = []
    for path in list(jars.values()) + sorted(legal.iterdir()):
        target = ('lib/m3-tools/graal-build-api/' if path.parent.name == 'jars' else 'legal/m3-graal-build-api/') + path.name
        assets.append({'file': str(path.relative_to(output)), 'target': target,
                       'sha256': file_digest(path), 'executable': False})
    tool_lock = {'schema_version': 1, 'packs': [{
        'name': 'graal-build-api', 'version': '1.1.13',
        'source': 'graalvm/native-build-tools@' + lock['upstream']['commit'] + '; openjson/openjson@' + lock['dependency']['commit'],
        'license': 'UPL-1.0 AND Apache-2.0', 'roots': RUNTIME_ROOTS, 'modules': [], 'assets': assets,
    }]}
    (output / 'tool-packs.lock.json').write_text(json.dumps(tool_lock, indent=2) + '\n')
    print(json.dumps({'status': 'SOURCE_BUILD_PASS', 'products': products,
                      'tool_pack_lock': str(output / 'tool-packs.lock.json')}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    arguments = parser.parse_args()
    build(arguments.java_home, arguments.output)
