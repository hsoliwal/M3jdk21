#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Compile and run the existing viewport owner contracts with mandatory JNI."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk', required=True, type=Path)
    parser.add_argument('--out', required=True, type=Path)
    parser.add_argument('--cc', default='gcc')
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    out, jdk = args.out.absolute(), args.jdk.resolve()
    if out.exists() or out.is_symlink():
        parser.error('--out must be fresh')
    out.mkdir(parents=True)
    java = [root / 'src/main/java/com/m3/collections' / (name + '.java')
            for name in ('M3MaskViewportKernel', 'M3MaskViewportNative')]
    tests = [root / 'src/test/java/com/m3/collections' / (name + '.java')
             for name in ('M3MaskViewportKernelContract', 'M3MaskViewportWindowContract')]
    native = root / 'src/main/c/m3maskviewport.c'
    regions = root / 'src/test/native/m3_mask_viewport_regions.c'
    records = []

    def run(name, command):
        command = [str(x) for x in command]
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (out / (name + '.log')).write_text(result.stdout)
        records.append({'name': name, 'argv': command, 'exit': result.returncode})
        (out / 'commands.json').write_text(json.dumps(records, indent=2) + '\n')
        if result.returncode:
            raise SystemExit(name + ' failed: ' + result.stdout)
        return result.stdout

    run('java-version', [jdk / 'bin/java', '-version'])
    run('cc-version', [args.cc, '--version'])
    run('java-compile', [jdk / 'bin/javac', '-Xlint:all', '-Werror', '-d', out / 'classes', *java, *tests])
    flags = ['-std=c11', '-Wall', '-Wextra', '-Werror', '-Wconversion', '-O2',
             '-fsanitize=undefined', '-fno-sanitize-recover=all',
             '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux')]
    library = out / 'libm3maskviewport.so'
    run('jni-compile', [args.cc, *flags, '-fPIC', '-shared', native, '-o', library])
    run('regions-compile', [args.cc, *flags, regions, '-o', out / 'regions'])
    region_output = run('regions', [out / 'regions'])
    if 'PASS viewport JNI regions:' not in region_output:
        raise SystemExit('missing region completion marker')
    for test in tests:
        output = run(test.stem, [jdk / 'bin/java', '-Xcheck:jni', '-Dm3.native.path=' + str(library),
                                '-cp', out / 'classes', 'com.m3.collections.' + test.stem])
        if 'PASS checks=' not in output or 'jni=true' not in output:
            raise SystemExit('missing mandatory JNI completion marker')
    receipt = {'schema': 'M3_MASK_VIEWPORT_VERIFICATION_V1', 'status': 'PASS',
               'sources': {p.relative_to(root).as_posix(): sha(p) for p in [*java, *tests, native, regions]},
               'jdk_modules_sha256': sha(jdk / 'lib/modules'),
               'native_library_sha256': sha(library),
               'scope': 'Finite Java/JNI contracts on the supplied JDK; not full product-image qualification',
               'performance_claim': 'NONE'}
    (out / 'RESULT.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print('PASS mandatory JNI, bounded regions, Java differential, and UBSan')


if __name__ == '__main__':
    main()
