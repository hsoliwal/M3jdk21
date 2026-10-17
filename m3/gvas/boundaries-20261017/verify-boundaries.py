#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Bounded Linux Java21/JNI proof: exact shared address, UTF-8 paths, failed-admission retry."""
import argparse
import hashlib
import os
from pathlib import Path
import selectors
import shutil
import subprocess
import tempfile
import time

BASE = 0x600000000000


def run(args, **kwargs):
    return subprocess.run(args, check=True, timeout=60, **kwargs)


def ready(child, timeout=20):
    deadline = time.monotonic() + timeout
    data = bytearray()
    with selectors.DefaultSelector() as selector:
        selector.register(child.stdout, selectors.EVENT_READ)
        while b'\n' not in data:
            remaining = deadline - time.monotonic()
            if remaining <= 0 or not selector.select(remaining):
                raise TimeoutError('child readiness deadline exceeded')
            block = os.read(child.stdout.fileno(), 4096)
            if not block:
                raise RuntimeError('child exited before readiness: ' + data.decode('utf-8', errors='replace'))
            data.extend(block)
            if len(data) > 65536:
                raise RuntimeError('unexpected child output before readiness')
    text = data.decode('utf-8').strip()
    if text != f'M3_GVAS_BOUNDARY_READY base=0x{BASE:x}':
        raise AssertionError(text)
    return text


def mapping(pid, image):
    stat = image.stat()
    page = os.sysconf('SC_PAGE_SIZE')
    end = BASE + (stat.st_size + page - 1) // page * page
    candidates = []
    for line in Path(f'/proc/{pid}/maps').read_text().splitlines():
        fields = line.split(maxsplit=5)
        if len(fields) < 5:
            continue
        begin, finish = (int(part, 16) for part in fields[0].split('-'))
        major, minor = (int(part, 16) for part in fields[3].split(':'))
        if (major, minor, int(fields[4]), int(fields[2], 16)) == (
                os.major(stat.st_dev), os.minor(stat.st_dev), stat.st_ino, 0):
            candidates.append((begin, finish, fields[1], line))
    assert len(candidates) == 1, candidates
    begin, finish, permissions, line = candidates[0]
    assert (begin, finish, permissions) == (BASE, end, 'r--s'), line
    return line


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path.cwd())
    parser.add_argument('--fixture', type=Path)
    args = parser.parse_args()
    if not Path('/proc/self/maps').exists() or os.name != 'posix':
        raise RuntimeError('Linux /proc mapping qualification required')
    root = args.root.resolve()
    fixture = (args.fixture or root / 'm3/core/test/com/m3/text/M3FixedAddressBoundaryProof.java').resolve()
    jdk = Path(os.environ['JAVA_HOME']) if 'JAVA_HOME' in os.environ else Path(shutil.which('javac')).resolve().parent.parent
    with tempfile.TemporaryDirectory(prefix='m3-gvas-boundaries-') as folder:
        work = Path(folder)
        classes = work / 'classes'; classes.mkdir()
        headers = work / 'headers'; headers.mkdir()
        owner = root / 'm3/core/src/com/m3/text/M3FixedAddressMapping.java'
        native = root / 'm3/lexicon/native/m3_fixed_address_mapping_jni.c'
        run([str(jdk / 'bin/javac'), '--release', '21', '-proc:none', '-Xlint:all', '-Werror',
             '-h', str(headers), '-d', str(classes), str(owner), str(fixture)])
        # Include the generated JNI header AFTER the implementation's feature-test
        # macros and system includes. Pre-including it hides GNU open/mmap flags.
        unit = work / 'abi.c'
        unit.write_text(f'#include "{native}"\n#include "{headers / "com_m3_text_M3FixedAddressMapping.h"}"\n')
        library = work / 'libm3gvas.so'
        run([os.environ.get('CC', 'cc'), '-std=c17', '-Wall', '-Wextra', '-Werror',
             '-fPIC', '-shared', '-fsanitize=undefined', '-fno-sanitize-recover=all',
             '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'), str(unit), '-o', str(library)])
        payload = bytearray(4096); payload[128:130] = b'Z6'
        digest = hashlib.sha256(payload).hexdigest()
        files = [work / 'canonical.bin', work / '\u0938\u093e\u091d\u093e-\U0001f600-\U00010437.bin']
        java = [str(jdk / 'bin/java'), '-Xcheck:jni', '-ea', '-cp', str(classes)]
        for image in files:
            image.write_bytes(payload); image.chmod(0o444)
            for mode in ('unicode', 'cleanup', 'digest', 'collision'):
                result = run([*java, 'com.m3.text.M3FixedAddressBoundaryProof', mode,
                              str(library), str(image), digest], text=True, capture_output=True)
                assert f'M3_GVAS_BOUNDARY_PASS mode={mode}' in result.stdout, result.stdout
                assert not result.stderr, result.stderr
                print(result.stdout.strip())
        image = files[-1]
        children = []
        diagnostics = []
        try:
            for index, vm_mode in enumerate(('-Xint', '-Xbatch')):
                error_file = (work / f'child-{index}.stderr').open('w+b')
                diagnostics.append(error_file)
                child = subprocess.Popen([java[0], '-Xcheck:jni', vm_mode, *java[2:],
                        'com.m3.text.M3FixedAddressBoundaryProof', 'child', str(library), str(image), digest],
                        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=error_file, bufsize=0)
                children.append(child)
                print(ready(child))
            assert all(child.poll() is None for child in children)
            rows = [mapping(child.pid, image) for child in children]
            assert all(child.poll() is None for child in children)
            for child in children:
                output, _ = child.communicate(b'\n', timeout=20)
                assert child.returncode == 0, output
                assert b'M3_GVAS_BOUNDARY_PASS mode=child' in output, output
                print(output.decode().strip())
            for error_file in diagnostics:
                error_file.seek(0)
                assert not error_file.read(), 'checked JNI diagnostics'
            print(f'M3_GVAS_BOUNDARIES_STRICT_PASS address=0x{BASE:x} processes=2 device={image.stat().st_dev} inode={image.stat().st_ino} sha256={digest}')
            for row in rows:
                print(row)
        finally:
            for child in children:
                if child.poll() is None:
                    child.kill()
                child.wait(timeout=10)
                if child.stdin: child.stdin.close()
                if child.stdout: child.stdout.close()
            for error_file in diagnostics:
                error_file.close()


if __name__ == '__main__':
    main()
