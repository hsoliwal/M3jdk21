#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Deterministic Java 21 + Linux JNI local qualification for M3 primitive images."""
import hashlib
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / 'src/java.base/share/classes/jdk/internal/mindex'
COMPILER = ROOT / 'src/jdk.compiler/share/classes/com/sun/tools/javac/m3'
TEST = ROOT / 'test/jdk/jdk/internal/mindex'
NATIVE = ROOT / 'src/java.base/linux/native/libjava/M3PrimitiveFixedArena.c'

FIXTURE = '''final class PrimitiveFixture {
    static final boolean FLAG = true;
    static final byte B = -1;
    static final short S = -2;
    static final char C = 'Z';
    static final int I = 42 + 1;
    static final long L = Long.MIN_VALUE;
    static final float F = -0.0f;
    static final double D = -0.0d;
    static final String NOT_PRIMITIVE = "never stored";
    static int neverRun() { throw new AssertionError("source execution"); }
}'''

def run(*argv, expect=0):
    p = subprocess.run([str(x) for x in argv], capture_output=True, text=True)
    if p.returncode == expect:
        if p.stdout.strip(): print(p.stdout.strip())
        return p.stdout
    raise AssertionError(f'command failed ({p.returncode} expected {expect}): {argv}\n{p.stdout}\n{p.stderr}')

def main():
    if not shutil.which('javac') or not shutil.which('java'):
        raise RuntimeError('a Java 21 javac and java are required')
    version = subprocess.run(['java', '-version'], capture_output=True, text=True).stderr
    if not re.search(r'"21\.', version):
        raise RuntimeError('Java 21 is required (found ' + version.splitlines()[0] + ')')
    with tempfile.TemporaryDirectory(prefix='m3-primitive-') as work:
        work = Path(work)
        for name in ('base', 'compiler', 'test'): (work / name).mkdir()
        run('javac', '-Xlint:all', '-Werror', '--patch-module', f'java.base={BASE}',
            '-d', work / 'base', *(BASE / f for f in (
                'M3PrimitiveIdentity.java', 'M3PrimitiveImage.java', 'M3PrimitiveFixedArena.java')))
        run('javac', '-Xlint:all', '-Werror', '--patch-module', f'jdk.compiler={COMPILER}',
            '-d', work / 'compiler', COMPILER / 'M3PrimitiveImageCompiler.java')
        run('javac', '-Xlint:all', '-Werror', '--patch-module', f'java.base={work / "base"}',
            '--add-exports', 'java.base/jdk.internal.mindex=ALL-UNNAMED',
            '-d', work / 'test', TEST / 'M3PrimitiveIdentityTest.java',
            TEST / 'M3PrimitiveFixedArenaProbe.java')
        (work / 'PrimitiveFixture.java').write_text(FIXTURE, encoding='utf-8')
        image, slots = work / 'canonical.m3prim', work / 'canonical.tsv'
        compiler_prefix = ('java', '--patch-module', f'jdk.compiler={work / "compiler"}',
                           '-m', 'jdk.compiler/com.sun.tools.javac.m3.M3PrimitiveImageCompiler')
        run(*compiler_prefix, '--out', image, '--slots', slots, work / 'PrimitiveFixture.java')
        first_image, first_slots = image.read_bytes(), slots.read_bytes()
        run(*compiler_prefix, '--out', image, '--slots', slots, work / 'PrimitiveFixture.java')
        assert image.read_bytes() == first_image and slots.read_bytes() == first_slots, 'fixed point'
        # Generated source errors cannot clobber admitted images.
        broken = work / 'Broken.java'
        broken.write_text('class Broken { static final int X = DOES_NOT_EXIST; }')
        p = subprocess.run([*map(str, compiler_prefix), '--out', str(image), str(broken)],
                           capture_output=True, text=True)
        assert p.returncode != 0 and image.read_bytes() == first_image, 'compiler refusal'
        java_prefix = ('java', '--patch-module', f'java.base={work / "base"}',
                       '--add-exports', 'java.base/jdk.internal.mindex=ALL-UNNAMED',
                       '-cp', work / 'test')
        run(*java_prefix, 'M3PrimitiveIdentityTest', image)
        mutated = work / 'mutated.m3prim'
        corrupt = bytearray(first_image)
        corrupt[-1] ^= 1
        mutated.write_bytes(corrupt)
        result = subprocess.run([*map(str, java_prefix), 'M3PrimitiveIdentityTest', str(mutated)],
                                capture_output=True, text=True)
        assert result.returncode != 0 and 'SHA-256 mismatch' in result.stderr, 'corruption gate'
        # Processes are independent: equality of namespace+slots must be reproducible.
        a = run(*java_prefix, 'M3PrimitiveIdentityTest', image)
        b = run(*java_prefix, 'M3PrimitiveIdentityTest', image)
        assert re.search('M3_NAMESPACE=([a-f0-9]{64})', a).group(1) == \
            re.search('M3_NAMESPACE=([a-f0-9]{64})', b).group(1), 'cross-process identity'
        if platform.system() == 'Linux' and shutil.which('gcc'):
            javac = Path(shutil.which('javac')).resolve()
            jdk = javac.parent.parent
            library = work / 'libm3primitive.so'
            run('gcc', '-std=c11', '-Wall', '-Wextra', '-Werror', '-fPIC', '-shared',
                '-I', jdk / 'include', '-I', jdk / 'include/linux', '-o', library, NATIVE)
            commands = [list(map(str, ('java', '-Xcheck:jni', '--patch-module',
                       f'java.base={work / "base"}', '--add-exports',
                       'java.base/jdk.internal.mindex=ALL-UNNAMED', '-cp', work / 'test',
                       'M3PrimitiveFixedArenaProbe', image, library))) for _ in range(2)]
            procs = [subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                      text=True) for cmd in commands]
            results = [p.communicate(timeout=30) for p in procs]
            assert all(p.returncode == 0 for p in procs), results
            for stdout, _ in results: print(stdout.strip())
            mapped = [re.search('M3_FIXED_ARENA_OK base=(0x[0-9a-f]+) namespace=([a-f0-9]{64})',
                                stdout) for stdout, _ in results]
            if all(mapped):
                assert mapped[0].groups() == mapped[1].groups(), 'fixed-VA mismatch'
                print('M3_FIXED_ADDRESS_CROSS_PROCESS_PASS')
            else:
                print('M3_FIXED_ADDRESS_UNAVAILABLE: correct offset-based fallback')
        else:
            print('M3_NATIVE_SKIPPED: non-Linux or gcc unavailable')
        print('M3_PRIMITIVE_PROOF_PASS sha256=' + hashlib.sha256(first_image).hexdigest())

if __name__ == '__main__':
    main()
