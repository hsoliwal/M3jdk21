#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Target-only runtime proof; full transformation replay stays in pinned Synexia."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True, help='New directory outside the checkout')
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    root = here.parents[3]
    out = args.output.resolve()
    if out == root or root in out.parents:
        raise ValueError('proof output must be outside the checkout')
    sources = []
    native = None
    for item in json.loads((here / 'inputs.json').read_text()):
        source = root / item['path']
        if hashlib.sha256(source.read_bytes()).hexdigest() != item['sha256']:
            raise ValueError('source drift: ' + item['path'])
        if source.suffix == '.java':
            sources.append(source)
        elif source.suffix == '.c':
            native = source
    if native is None:
        raise ValueError('missing native owner')
    tools = {name: shutil.which(name) for name in ['java', 'javac', 'gcc']}
    if not all(tools.values()):
        raise RuntimeError('Java 21 JDK and GCC are required; no dependency downloads')
    home = Path(tools['javac']).resolve().parent.parent
    out.mkdir(parents=True, exist_ok=False)
    for name in ['classes', 'headers', 'native', 'absent']:
        (out / name).mkdir()
    commands = []

    def run(name, *command):
        command = [str(x) for x in command]
        commands.append(command)
        result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, timeout=120, cwd=out,
                                env=dict(os.environ, UBSAN_OPTIONS='halt_on_error=1'))
        (out / (name + '.txt')).write_text(result.stdout)
        if result.returncode:
            raise RuntimeError(name + ' failed: ' + result.stdout)
        return result.stdout

    run('compile-java', tools['javac'], '--release', '21', '-Xlint:all', '-Werror',
        '-h', out / 'headers', '-d', out / 'classes', *sources)
    run('compile-native', tools['gcc'], '-std=c17', '-O2', '-fPIC', '-shared',
        '-Wall', '-Wextra', '-Werror', '-Wconversion', '-Wsign-conversion',
        '-fsanitize=undefined', '-fno-sanitize-recover=undefined', '-fstack-usage',
        '-I' + str(home / 'include'), '-I' + str(home / 'include/linux'),
        '-include', out / 'headers/com_m3_collections_M3IndexMaskCompactor.h', native,
        '-o', out / 'native/libm3_collections_mask.so')
    for mode, expected in [('java', 260388), ('native', 260407), ('unavailable', 260390)]:
        library = out / ('absent' if mode == 'unavailable' else 'native')
        text = run(mode, tools['java'], '-Xcheck:jni', '-Djava.library.path=' + str(library),
                   '-cp', out / 'classes', 'com.m3.collections.M3FrequencyMaskChecks', mode)
        expected_text = ('PASS checks=' + str(expected)
                         + ' tape=587f92aa87c2c49e474b3b7c364cf3e1df5d9ec551e095558ea68a6d312ddd31')
        if text.strip() != expected_text:
            raise AssertionError('frozen observation changed: ' + text)
        print(text.strip())
    (out / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    print('PASS target-source runtime checks; not Maven/SDK/full-module/full-JDK qualification')


if __name__ == '__main__':
    main()
