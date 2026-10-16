#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Actual-owner Java21/JNI and linked-image proof for the immutable chunk-plan leaf.

Reuse the existing pinned LF JNI and JMOD verifier. The source of truth is the
existing M3JDK21 capability module, not a reconstructed test double.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys


def invoke(args, output, name):
    done = subprocess.run([str(arg) for arg in args], text=True, capture_output=True,
                          errors='replace', check=False, timeout=240)
    (output / (name + '.log')).write_text(done.stdout + done.stderr, encoding='utf-8')
    if done.returncode:
        raise RuntimeError('%s failed (%d): %s' % (name, done.returncode,
                                                  (done.stdout + done.stderr)[-1000:]))
    return done.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--java-only', action='store_true')
    options = parser.parse_args()
    root, out = options.root.resolve(), options.out.resolve()
    if not root.is_dir() or out.exists() or root == out or root in out.parents:
        raise ValueError('new proof directory outside source tree required')
    if not out.parent.is_dir() or out.parent.is_symlink():
        raise ValueError('existing non-symlink parent required')
    sources = sorted((root / 'src/main/java').rglob('*.java'))
    tests = sorted((root / 'src/test/java').rglob('*.java'))
    tracked = sources + tests + [root / 'native/lf_count_jni.c',
                                 root / 'verify_native.py']
    before = {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
              for p in tracked}
    out.mkdir()
    proof = out / 'original-jni-owner'
    cmd = [sys.executable, root / 'verify_native.py', '--root', root,
           '--out', proof, '--developer-image']
    if options.java_only:
        cmd.append('--java-only')
    invoke(cmd, out, 'existing-owner-and-jmod')
    baseline = json.loads((proof / 'REPORT.json').read_text(encoding='utf-8'))
    expected_status = 'JAVA_ONLY_PASS' if options.java_only else 'JAVA_JNI_PARITY_PASS'
    if (baseline['status'] != expected_status or baseline[
            'stock_jdk21_developer_image'] != 'M3_CAPABILITY_DEVELOPER_IMAGE_PASS'):
        raise AssertionError('existing owner, native parity or developer image failed')
    java = shutil.which('java')
    if not java:
        raise RuntimeError('Java21 required')
    main_class = 'com.m3.runtime.capability/com.m3.runtime.capability.SourceChunkPlanChecks'
    command = [java, '-Xmx128m', '-Xcheck:jni', '--module-path', proof / 'classes',
               '--module', main_class]
    pure = invoke(command, out, 'chunk-java-oracle')
    matcher = re.compile(r'^M3_SOURCE_CHUNK_PLAN_PASS assertions=(\d+) kernel=([A-Z_0-9]+)$')
    j = matcher.fullmatch(pure)
    if j is None or j.group(2) != 'JAVA_LF_INDEX' or int(j.group(1)) < 20_000:
        raise AssertionError('Java chunk-plan oracle evidence missing')
    native = 'NOT_RUN'
    if not options.java_only:
        lib = proof / 'libm3capability.so'
        if not lib.is_file():
            raise RuntimeError('expected existing Linux C11 JNI library missing')
        digest = hashlib.sha256(lib.read_bytes()).hexdigest()
        native = invoke(command + [lib, digest], out, 'chunk-jni-oracle')
        k = matcher.fullmatch(native)
        if k is None or k.group(2) != 'JNI_LF_INDEX_C11' or int(k.group(1)) <= int(j.group(1)):
            raise AssertionError('native chunk-plan differential evidence missing')
    linked = proof / 'developer-image'
    exe = '.exe' if os.name == 'nt' else ''
    smoke = root / 'src/test/java/com/m3/runtime/proof/SourceChunkPlanImageSmoke.java'
    dest = out / 'linked-image-smoke-classes'
    dest.mkdir()
    invoke([linked / 'bin' / ('javac' + exe), '--add-modules',
            'com.m3.runtime.capability', '-Xlint:all', '-Werror',
            '-d', dest, smoke], out, 'linked-image-javac')
    linked_output = invoke([linked / 'bin' / ('java' + exe), '--add-modules',
                            'com.m3.runtime.capability', '-cp', dest,
                            'com.m3.runtime.proof.SourceChunkPlanImageSmoke'],
                           out, 'linked-image-chunk-smoke')
    if linked_output != 'M3_SOURCE_CHUNK_PLAN_IMAGE_PASS':
        raise AssertionError('new chunk plan not exercised in linked image')
    after = {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
             for p in tracked}
    if before != after:
        raise AssertionError('verification mutated original source')
    receipt = {'status': 'M3_SOURCE_CHUNK_PLAN_JAVA_PASS' if options.java_only
               else 'M3_SOURCE_CHUNK_PLAN_JAVA_JNI_PASS',
               'java': pure, 'native': native,
               'linked_image': linked_output,
               'base_owner_status': expected_status,
               'source_sha256': before,
               'full_m3_hotspot_jtreg': 'NOT_RUN',
               'actual_openrewrite_sdk': 'NOT_RUN',
               'measured_performance': 'NOT_RUN',
               'new_native_symbols': 0}
    (out / 'REPORT.json').write_text(json.dumps(receipt, indent=2,
                                               sort_keys=True) + '\n', encoding='utf-8')
    print(json.dumps({key: receipt[key] for key in
                      ('status', 'java', 'native', 'linked_image')}, sort_keys=True))


if __name__ == '__main__':
    main()
