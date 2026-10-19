#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real JDK21/optional C11 JNI histogram proof in a new external output directory."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(out, label, command):
    result = subprocess.run([str(part) for part in command], text=True,
                            capture_output=True, errors='replace', timeout=180,
                            check=False)
    (out / (label + '.log')).write_text(result.stdout + result.stderr,
                                       encoding='utf-8')
    if result.returncode:
        raise RuntimeError(label + ' failed: ' + result.stderr[-1200:])
    return result.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--java-only', action='store_true')
    args = parser.parse_args()
    root = args.root.resolve()
    out = args.out.absolute()
    if not root.is_dir() or out.exists() or root == out or root in out.parents:
        raise ValueError('fresh output directory outside owner root required')
    if not out.parent.is_dir() or out.parent.is_symlink():
        raise ValueError('output parent must exist and not be a symlink')
    javac, java = shutil.which('javac'), shutil.which('java')
    if not javac or not java:
        raise RuntimeError('JDK21 javac/java required')
    out.mkdir()
    classes = out / 'classes'
    headers = out / 'headers'
    classes.mkdir()
    headers.mkdir()
    productions = sorted((root / 'src/main/java').rglob('*.java'))
    tests = [root / 'src/test/java/com/m3/runtime/capability/ByteHistogram256Checks.java']
    native = [root / 'native/lf_count_jni.c', root / 'native/byte_histogram_jni.c']
    tracked = productions + tests + native
    expected = {path.relative_to(root).as_posix(): sha(path) for path in tracked}
    run(out, 'javac', [javac, '--release', '21', '-Xlint:all,-module', '-Werror',
                       '-h', headers, '-d', classes, *productions, *tests])
    target = 'com.m3.runtime.capability.ByteHistogram256Checks'
    java_cmd = [java, '-Xmx64m', '-Xcheck:jni', '--module-path', classes,
                '--module', 'com.m3.runtime.capability/' + target]
    oracle = run(out, 'java', java_cmd)
    if 'M3_BYTE_HISTOGRAM_PASS' not in oracle or 'kernel=JAVA_SOURCE_RANGE' not in oracle:
        raise RuntimeError('Java byte histogram oracle did not execute')
    accelerated = 'NOT_RUN'
    if not args.java_only:
        cc = shutil.which('cc')
        if not cc:
            raise RuntimeError('C11 compiler required for JNI parity')
        jdk = Path(os.path.realpath(javac)).parent.parent
        if os.name == 'nt':
            os_header = 'win32'
        elif os.uname().sysname == 'Darwin':
            os_header = 'darwin'
        else:
            os_header = 'linux'
        library = out / ('m3capability.dll' if os.name == 'nt'
                         else 'libm3capability.so')
        flags = ['-std=c11', '-O2', '-Wall', '-Wextra', '-Werror', '-fPIC', '-shared']
        if os.name != 'nt':
            flags += ['-fsanitize=undefined', '-fno-sanitize-recover=all']
        run(out, 'native-cc', [cc, *flags, '-I' + str(jdk / 'include'),
                              '-I' + str(jdk / 'include' / os_header),
                              '-include', headers / 'com_m3_runtime_capability_LfCount.h',
                              '-include', headers / 'com_m3_runtime_capability_BytePatternSearch.h',
                              '-include', headers / 'com_m3_runtime_capability_ByteHistogram256.h',
                              *native, '-o', library])
        accelerated = run(out, 'jni', [*java_cmd, library.resolve(), sha(library)])
        if 'M3_BYTE_HISTOGRAM_PASS' not in accelerated or 'kernel=JNI_DIRECT_C11' not in accelerated:
            raise RuntimeError('JNI byte histogram oracle did not execute')
    actual = {path.relative_to(root).as_posix(): sha(path) for path in tracked}
    if actual != expected:
        raise RuntimeError('source changed during proof')
    receipt = {'java': oracle, 'jni': accelerated, 'source_sha256': expected,
               'status': 'JAVA_JNI_PARITY_PASS' if not args.java_only else 'JAVA_ONLY_PASS',
               'full_modified_jdk_jtreg': 'NOT_RUN',
               'synexia_openrewrite_sdk': 'NOT_RUN',
               'setup_inclusive_performance': 'NOT_RUN'}
    (out / 'REPORT.json').write_text(json.dumps(receipt, sort_keys=True, indent=2) + '\n',
                                     encoding='utf-8')
    print(receipt['status'], oracle, accelerated)


if __name__ == '__main__':
    main()
