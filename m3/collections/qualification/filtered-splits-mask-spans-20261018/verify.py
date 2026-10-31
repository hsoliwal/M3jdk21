#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Target-native proof; no recipe/source mutation and no downloaded dependencies."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
MODULE = ROOT / 'm3/collections'
OUT = MODULE / 'target/filtered-splits-mask-spans'
JAVA = Path(os.environ['JAVA_HOME'])
JUNIT = Path(os.environ['JUNIT_CONSOLE_JAR']).resolve()

def run(args):
    print('+ ' + ' '.join(map(str, args)), flush=True)
    subprocess.run(list(map(str, args)), cwd=ROOT, check=True)

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name in ('classes', 'test-classes', 'headers', 'native', 'reports'):
        (OUT / name).mkdir(exist_ok=True)
    sources = sorted((MODULE / 'src/main/java').rglob('*.java'))
    tests = sorted((MODULE / 'src/test/java').rglob('*.java'))
    for label, paths in [('main', sources), ('test', tests)]:
        (OUT / (label + '.args')).write_text('\n'.join(str(p) for p in paths) + '\n')
    run([JAVA / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror',
         '-h', OUT / 'headers', '-d', OUT / 'classes', '@' + str(OUT / 'main.args')])
    classpath = str(OUT / 'classes') + os.pathsep + str(JUNIT)
    run([JAVA / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror',
         '-cp', classpath, '-d', OUT / 'test-classes', '@' + str(OUT / 'test.args')])
    cp = str(OUT / 'classes') + os.pathsep + str(OUT / 'test-classes')
    cc = [os.environ.get('CC', 'cc'), '-std=c17', '-Wall', '-Wextra', '-Werror',
          '-fsanitize=undefined', '-fno-sanitize-recover=undefined',
          '-I' + str(JAVA / 'include'), '-I' + str(JAVA / 'include/linux'),
          '-include', OUT / 'headers/com_m3_collections_M3IndexMaskWindow.h']
    kernel = MODULE / 'src/main/native/collections/m3_index_mask_window.c'
    run(cc + ['-shared', '-fPIC', '-O2', '-include',
              OUT / 'headers/com_m3_collections_M3IndexMaskCompactor.h', kernel,
              MODULE / 'src/main/native/collections/m3_index_mask_compactor.c',
              '-o', OUT / 'native/libm3_collections_mask.so'])
    run(cc + [kernel, MODULE / 'src/test/native/m3_mask_window_spans.c',
              '-o', OUT / 'native/spans'])
    run([OUT / 'native/spans'])
    run(cc + [kernel, MODULE / 'src/test/native/m3_mask_window_regions.c',
              '-o', OUT / 'native/regions'])
    run([OUT / 'native/regions'])
    junit = [JAVA / 'bin/java', '-Xcheck:jni', '-Djava.library.path=' + str(OUT / 'native'),
             '-Dm3.mask.spans.native.required=true', '-jar', JUNIT, 'execute',
             '--class-path', cp, '--fail-if-no-tests', '--disable-banner', '--details=summary',
             '--reports-dir', OUT / 'reports']
    for name in ['M3FilteredSplitsMaskSpansTest', 'M3FilteredViewsTest',
                 'M3MutableFilteredViewsTest', 'M3FilteredNavigableSetTest',
                 'M3FilteredNavigableSetRangeTest', 'M3StoredRepresentativeTest',
                 'M3BoundedFilteredNavigationTest']:
        junit += ['--select-class', 'com.m3.collections.' + name]
    run(junit)
    for name, native_arg in [('M3MaskWindowSparseContract', 'native'),
                             ('M3BudgetedMaskWindowContract', '--native')]:
        run([JAVA / 'bin/java', '-Xcheck:jni', '-Djava.library.path=' + str(OUT / 'native'),
             '-cp', cp, 'com.m3.collections.' + name, native_arg])
    run([JAVA / 'bin/java', '-Djava.library.path=' + str(OUT / 'missing'), '-cp', cp,
         'com.m3.collections.M3BudgetedMaskWindowContract', '--missing'])
    proof = {'status': 'PASS_SELECTED_TARGET_GATES', 'java_sources_compiled': len(sources),
             'test_sources_compiled': len(tests), 'native': 'actual -Xcheck:jni plus fatal UBSan',
             'scope': 'Independent m3/collections; no JDK or receiving-phase promotion',
             'source_sha256': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
                               for p in sources + tests + [kernel]},
             'jdk': subprocess.check_output([JAVA / 'bin/java', '-version'], stderr=subprocess.STDOUT, text=True),
             'junit_sha256': hashlib.sha256(JUNIT.read_bytes()).hexdigest()}
    (OUT / 'PROOF.json').write_text(json.dumps(proof, indent=2) + '\n')
    print('PASS selected contracts, full source compilation, real JNI and native span instrumentation')

if __name__ == '__main__':
    main()
