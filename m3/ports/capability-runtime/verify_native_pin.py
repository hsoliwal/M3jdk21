#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Independently refuse a JNI KMP symbol that bypasses pinned LF admission.

This negative test library is intentionally *not* the production JNI library.
Its KMP symbol returns false evidence and lacks the LF owner/ABI. Java must
refuse before invoking it. No process-global loading, materialization, or
untrusted native code is needed in the normal runtime path.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess


def digest(file):
    return hashlib.sha256(file.read_bytes()).hexdigest()


def command(out, name, argv):
    run = subprocess.run([str(x) for x in argv], capture_output=True,
                         text=True, errors='replace', timeout=150, check=False)
    (out / (name + '.log')).write_text(run.stdout + run.stderr, encoding='utf-8')
    if run.returncode != 0:
        raise RuntimeError('%s failed: %s' % (name, (run.stdout + run.stderr)[-500:]))
    return run.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    root, out = args.root.resolve(), args.out.absolute()
    if platform.system() != 'Linux':
        raise RuntimeError('adversarial C11/UBSan JNI admission proof requires Linux')
    if not root.is_dir() or out.exists() or root == out or root in out.parents:
        raise ValueError('fresh external proof directory required')
    if not out.parent.is_dir() or out.parent.is_symlink():
        raise ValueError('existing non-symlink external parent required')
    javac, java, cc = (shutil.which(n) for n in ('javac', 'java', 'cc'))
    if not javac or not java or not cc:
        raise RuntimeError('Java21 and a C11 compiler are required')
    out.mkdir()
    classes, headers = out / 'classes', out / 'headers'
    classes.mkdir(); headers.mkdir()
    java_files = sorted((root / 'src/main/java').rglob('*.java'))
    java_files += sorted((root / 'src/test/java').rglob('*.java'))
    fake = root / 'src/test/native/unverified_kmp_jni.c'
    if not fake.is_file() or fake.is_symlink() or not java_files:
        raise ValueError('owned native fixture and Java sources required')
    tracked = java_files + [fake, root / 'verify_native_pin.py']
    before = {p.relative_to(root).as_posix(): digest(p) for p in tracked}
    command(out, 'javac', [javac, '--release', '21', '-Xlint:all,-module', '-Werror',
                           '-h', headers, '-d', classes, *java_files])
    jdk = Path(os.path.realpath(javac)).parent.parent
    library = out / 'libm3kmp_unverified_fixture.so'
    command(out, 'unverified-fixture-c11', [cc, '-std=c11', '-O2',
            '-Wall', '-Wextra', '-Werror', '-fPIC', '-shared',
            '-fsanitize=undefined', '-fno-sanitize-recover=all',
            '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
            '-include', headers / 'com_m3_runtime_capability_BytePatternSearch.h',
            fake, '-o', library])
    result = command(out, 'unpinned-symbol-refusal', [java, '-Xmx128m',
            '-Xcheck:jni', '--module-path', classes, '--module',
            'com.m3.runtime.capability/com.m3.runtime.capability.NativePinAdmissionChecks',
            library.resolve()])
    if result != 'M3_NATIVE_KMP_PIN_REFUSAL_PASS':
        raise AssertionError('unpinned JNI symbol was not proven refused')
    after = {p.relative_to(root).as_posix(): digest(p) for p in tracked}
    if before != after:
        raise AssertionError('the proof mutated an admitted Java or native source')
    report = {'status': 'M3_NATIVE_KMP_PIN_REFUSAL_PASS',
              'java_release': 21, 'native_profile': 'C11_UBSAN_XCHECK_JNI_LINUX',
              'source_sha256': before, 'fixture_sha256': digest(library),
              'real_pinned_JNI_regression': 'RUN_SEPARATELY',
              'modified_hotspot_jtreg': 'NOT_RUN',
              'openrewrite_maven_sdk': 'NOT_RUN'}
    (out / 'REPORT.json').write_text(json.dumps(report, sort_keys=True, indent=2) + '\n',
                                     encoding='utf-8')
    print(json.dumps({'status': report['status'],
                      'source_files': len(before)}, sort_keys=True))


if __name__ == '__main__':
    main()
