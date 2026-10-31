#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify the sealed M3JDK21 candidate on a supplied JDK21; no downloads.

This is a bounded patch-module/JNI gate, not a complete M3JDK image build.
The native fixture compiles the actual product bridge source.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--hotspot-jvm', type=Path, help='Built compatible libjvm.so; original Object vtable contract requires six slots')
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[3]
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if (out / 'stages.json').exists():
        raise SystemExit('Use a fresh output directory; previous stage evidence is immutable.')
    stages = []

    def save():
        (out / 'stages.json').write_text(json.dumps(stages, indent=2) + '\n')

    def run(stage, command):
        log = out / (str(len(stages)).zfill(2) + '-' + stage + '.log')
        start = time.monotonic()
        with log.open('w') as stream:
            result = subprocess.run([str(x) for x in command], cwd=repo,
                                    stdout=stream, stderr=subprocess.STDOUT, timeout=180)
        stages.append({'stage': stage, 'command': [str(x) for x in command],
                       'exit_code': result.returncode, 'seconds': time.monotonic() - start,
                       'log': log.name, 'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()})
        save()
        print(stage + ': ' + ('PASSED' if result.returncode == 0 else 'FAILED'), flush=True)
        if result.returncode:
            raise SystemExit(log.read_text())

    seals = Path(__file__).with_name('sources.tsv')
    paths = []
    for row in seals.read_text().splitlines():
        if not row or row.startswith('#'):
            continue
        path, expected = row.split('\t')
        data = (repo / path).read_bytes()
        if hashlib.sha256(data).hexdigest() != expected:
            raise SystemExit('source drift: ' + path)
        if not data.endswith(b'\n') or b'\r' in data:
            raise SystemExit('UTF-8/newline lint failure: ' + path)
        data.decode('utf-8')
        paths.append(path)
    stages.append({'stage': 'patch-admission-and-lint', 'status': 'PASSED',
                   'sources': paths, 'source_manifest_sha256': hashlib.sha256(seals.read_bytes()).hexdigest()})
    save()

    jhome = args.java_home.resolve()
    javac, java, javap = [jhome / 'bin' / name for name in ('javac', 'java', 'javap')]
    src = out / 'patch-source'
    classes, tests, headers = [out / name for name in ('classes', 'tests', 'headers')]
    for directory in (src, classes, tests, headers, out / 'empty'):
        directory.mkdir()
    product = [path for path in paths if path.startswith('src/java.base/share/classes/')]
    donor = 'src/java.base/share/classes/jdk/internal/mindex/M3PrimitiveIdentity.java'
    if donor not in product:
        product.append(donor)
    for path in product:
        relative = Path(path).relative_to('src/java.base/share/classes')
        destination = src / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(repo / path, destination)
    run('compile-java-product', [javac, '-proc:none', '-Xlint:all', '-Werror',
        '--patch-module', 'java.base=' + str(src), '-d', classes,
        *sorted(src.rglob('*.java'))])
    test_sources = [repo / path for path in paths if path.startswith('test/jdk/') and path.endswith('.java')]
    run('compile-java-tests-and-jni-headers', [javac, '-proc:none', '-Xlint:all', '-Werror',
        '--patch-module', 'java.base=' + str(classes), '-sourcepath', str(out / 'empty'),
        '-classpath', str(out / 'empty'), '-d', tests, '-h', headers, *test_sources])
    fixture = repo / 'test/jdk/java/lang/Object/libM3ObjectNativeTest.c'
    library = out / 'libM3ObjectNativeTest.so'
    run('compile-c17-real-product-fixture', ['gcc', '-std=c17', '-Wall', '-Wextra', '-Werror',
        '-fPIC', '-shared', '-I' + str(jhome / 'include'), '-I' + str(jhome / 'include/linux'),
        '-include', headers / 'M3ObjectNativeTest.h', fixture, '-o', library])
    smoke = out / 'header.cpp'
    smoke.write_text('#include "m3_object.h"\nint main() { return M3_OBJECT_REFERENCE == 1 ? 0 : 1; }\n')
    run('compile-cpp20-header', ['g++', '-std=c++20', '-Wall', '-Wextra', '-Werror',
        '-I' + str(jhome / 'include'), '-I' + str(jhome / 'include/linux'),
        '-I' + str(repo / 'src/java.base/share/native/libjava'), '-fsyntax-only', smoke])
    run('abi-declarations', [javap, '-public', '-s',
        *[classes / ('java/lang/' + name + '.class') for name in
          ('Object', 'M3Object', 'M3Object$Field', 'ObjectEvent', 'ObjectChangeListener', 'ObjectSubscription')]])
    vm_options = []
    if args.hotspot_jvm is not None:
        jvm = args.hotspot_jvm.resolve()
        if not jvm.is_file() or jvm.name != 'libjvm.so':
            raise SystemExit('--hotspot-jvm must name the built libjvm.so')
        vm_options.append('-XXaltjvm=' + str(jvm.parent))
        stages.append({'stage': 'compiled-vm-identity', 'path': str(jvm),
                       'sha256': hashlib.sha256(jvm.read_bytes()).hexdigest()})
        save()
    base = [java, *vm_options, '--patch-module', 'java.base=' + str(classes),
            '--add-opens', 'java.base/java.lang=ALL-UNNAMED', '-Xmx64m', '-cp', tests]
    run('test-object-adapter-dispatch', [*base, 'ObjectAdapterDispatchTest'])
    run('runtime-object-adapter-interpreted', [*base, '-Xint', 'ObjectAdapterDispatchTest'])
    run('runtime-object-adapter-c2', [*base, '-Xcomp', '-Xbatch', '-XX:-TieredCompilation',
        '-XX:+PrintCompilation', 'ObjectAdapterDispatchTest'])
    for enabled in (False, True):
        mode = 'enabled' if enabled else 'disabled'
        setting = '-Djdk.m3.objectEvents.enabled=' + str(enabled).lower()
        run('test-java-' + mode, [*base, setting, 'M3ObjectTest', mode])
    run('runtime-java-interpreted', [*base, '-Xint', '-Djdk.m3.objectEvents.enabled=true',
                                   'M3ObjectTest', 'enabled'])
    for enabled in (False, True):
        mode = 'enabled' if enabled else 'disabled'
        run('runtime-jni-' + mode, [*base, '-Xcheck:jni',
            '-Djava.library.path=' + str(out), '-Djdk.m3.objectEvents.enabled=' + str(enabled).lower(),
            'M3ObjectNativeTest', mode])
    run('runtime-jni-interpreted', [*base, '-Xint', '-Xcheck:jni',
        '-Djava.library.path=' + str(out), '-Djdk.m3.objectEvents.enabled=true',
        'M3ObjectNativeTest', 'enabled'])
    print('Bounded patch-module Java/JNI qualification PASSED; full M3JDK build/jtreg NOT_RUN.')


if __name__ == '__main__':
    main()
