#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Exact selected-source Java21/JNI gate. Does not run Maven, jtreg or a JDK image build."""
import argparse, hashlib, json, os, pathlib, shutil, subprocess, sys, tempfile

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--root', type=pathlib.Path)
    p.add_argument('--java-only', action='store_true')
    args = p.parse_args()
    task = pathlib.Path(__file__).resolve().parent
    root = (args.root or task.parents[3]).resolve()
    receipt = task / 'SOURCE_RECEIPT.tsv'
    data = receipt.read_bytes()
    if hashlib.sha256(data).hexdigest() != '8a5b2a658955c051d3e2f27776ac0d813bb8804bfb7d90740e80925eba6736cf':
        raise RuntimeError('unsealed source receipt')
    entries = [line.split('\t') for line in data.decode().splitlines()[1:]]
    if len(entries) != 8 or len({e[0] for e in entries}) != 8:
        raise RuntimeError('unexpected receipt scope')
    files = {}
    for rel, before, after in entries:
        file = root / rel
        if file.is_symlink() or root not in file.resolve().parents:
            raise RuntimeError('source symlink/outside root: ' + rel)
        for ancestor in file.parents:
            if ancestor == root: break
            if ancestor.is_symlink(): raise RuntimeError('source ancestor symlink: ' + rel)
        if hashlib.sha256(file.read_bytes()).hexdigest() != after:
            raise RuntimeError('source drift: ' + rel)
        files[file.name] = file
    javac = shutil.which('javac')
    java = shutil.which('java')
    if not javac or not java: raise RuntimeError('Java 21 tools required')
    def run(argv):
        print('$ ' + ' '.join(map(str, argv)), flush=True)
        subprocess.run(list(map(str, argv)), check=True, timeout=180)
    with tempfile.TemporaryDirectory(prefix='m3-sdk-filter-') as tmp:
        tmp = pathlib.Path(tmp); classes = tmp / 'classes'; headers = tmp / 'headers'
        classes.mkdir(); headers.mkdir()
        javafiles = [path for name,path in files.items() if name.endswith('.java') and name != 'M3SdkFilterWindowTest.java']
        run([javac, '--release', '21', '-proc:none', '-Xlint:all', '-Werror', '-h', headers, '-d', classes, *javafiles])
        for name in ['M3BulkCaptureContract', 'M3BudgetedMaskWindowContract']:
            run([java, '-Xmx32m', '-cp', classes, 'com.m3.collections.'+name])
        missing = tmp / 'missing'; missing.mkdir()
        run([java, '-Djava.library.path='+str(missing), '-cp', classes,
             'com.m3.collections.M3BudgetedMaskWindowContract', '--missing'])
        native = 'NOT_RUN_REQUESTED_JAVA_ONLY'
        if not args.java_only:
            if not sys.platform.startswith('linux'): raise RuntimeError('this native build driver requires Linux; use platform-specific gate')
            cc = shutil.which(os.environ.get('CC', 'gcc'))
            if not cc: raise RuntimeError('C17 compiler required')
            jhome = pathlib.Path(javac).resolve().parents[1]
            libdir = tmp / 'native'; libdir.mkdir()
            run([cc, '-std=c17', '-Wall', '-Wextra', '-Werror', '-Wconversion', '-Wshadow', '-pedantic',
                '-fPIC', '-shared', '-fsanitize=undefined', '-fno-sanitize-recover=all',
                '-I'+str(jhome/'include'), '-I'+str(jhome/'include/linux'),
                files['m3_index_mask_compactor.c'], files['m3_index_mask_window.c'],
                '-o', libdir/'libm3_collections_mask.so'])
            run([java, '-Xmx32m', '-Xcheck:jni', '-Djava.library.path='+str(libdir), '-cp', classes,
                'com.m3.collections.M3BudgetedMaskWindowContract', '--native'])
            native = 'PASS_LINUX_C17_UBSAN_XCHECK_JNI'
        print(json.dumps({'selected_source_seals':len(entries),'java':'PASS', 'native':native,
                          'maven_openrewrite':'NOT_RUN', 'full_jdk':'NOT_RUN', 'promotion':'HOLD'}))

if __name__ == '__main__': main()
