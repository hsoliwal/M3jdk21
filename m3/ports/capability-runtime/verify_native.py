#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Real JDK21 owner/JNI parity proof with fresh external output; no WSL requirement."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(out, name, args):
    p = subprocess.run([str(x) for x in args], text=True, capture_output=True,
                       errors='replace', timeout=150, check=False)
    (out / (name + '.log')).write_text(p.stdout + p.stderr, encoding='utf-8')
    if p.returncode:
        raise RuntimeError('%s failed (%s): %s' % (name, p.returncode, p.stderr[-800:]))
    return p.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--java-only', action='store_true')
    parser.add_argument('--developer-image', action='store_true')
    args = parser.parse_args()
    root, out = args.root.resolve(), args.out.absolute()
    if not root.is_dir() or out.exists() or root == out or root in out.parents:
        raise ValueError('fresh proof directory outside source tree required')
    if not out.parent.is_dir() or out.parent.is_symlink():
        raise ValueError('proof parent must exist and not be a symlink')
    javac = shutil.which('javac')
    java = shutil.which('java')
    if not javac or not java:
        raise RuntimeError('installed JDK21 java and javac required')
    out.mkdir()
    classes, headers = out / 'classes', out / 'headers'
    classes.mkdir(); headers.mkdir()
    sources = sorted((root / 'src/main/java').rglob('*.java'))
    tests = sorted((root / 'src/test/java').rglob('*.java'))
    native = root / 'native/lf_count_jni.c'
    tracked = sources + tests + [native]
    expected = {p.relative_to(root).as_posix(): sha(p) for p in tracked}
    run(out, 'javac', [javac, '--release', '21', '-Xlint:all,-module', '-Werror',
                      '-h', headers, '-d', classes, *sources, *tests])
    main_class = 'com.m3.runtime.capability.CapabilityRuntimeChecks'
    options = [java, '-Xmx128m', '-Xcheck:jni', '--module-path', classes,
               '--module', 'com.m3.runtime.capability/' + main_class]
    pure = run(out, 'java-oracle', options)
    if not args.java_only:
        cc = shutil.which('cc')
        if not cc: raise RuntimeError('C11 compiler required for native lane')
        jdk = Path(os.path.realpath(javac)).parent.parent
        os_header = 'win32' if os.name == 'nt' else ('darwin' if os.uname().sysname == 'Darwin' else 'linux')
        library = out / ('m3capability.dll' if os.name == 'nt' else 'libm3capability.so')
        flags = ['-std=c11', '-O2', '-Wall', '-Wextra', '-Werror', '-fPIC', '-shared']
        if os.name != 'nt': flags += ['-fsanitize=undefined', '-fno-sanitize-recover=all']
        run(out, 'native-cc', [cc, *flags, '-I' + str(jdk / 'include'),
                              '-I' + str(jdk / 'include' / os_header),
                              '-include', headers / 'com_m3_runtime_capability_LfCount.h',
                              '-include', headers / 'com_m3_runtime_capability_BytePatternSearch.h',
                              native, '-o', library])
        accelerated = run(out, 'jni-oracle', [*options, library.resolve(), sha(library)])
        if 'kernel=JNI_DIRECT_C11' not in accelerated:
            raise RuntimeError('native proof did not run')
    else:
        accelerated = 'NOT_RUN'
    if 'kernel=JAVA_BYTEBUFFER' not in pure:
        raise RuntimeError('Java-only proof did not run')
    # Source-preserving line-index proof is independent of the preexisting LF
    # count assertions and executes in both stock-JDK21 and JNI configurations.
    line_class = 'com.m3.runtime.capability.ByteLineIndexChecks'
    line_options = [java, '-Xmx128m', '-Xcheck:jni', '--module-path', classes,
                    '--module', 'com.m3.runtime.capability/' + line_class]
    java_lines = run(out, 'line-index-java-oracle', line_options)
    if 'M3_BYTE_LINE_INDEX_PASS assertions=' not in java_lines or 'kernel=JAVA_SOURCE_RANGE' not in java_lines:
        raise RuntimeError('line-index Java contract oracle did not execute')
    native_lines = 'NOT_RUN'
    if not args.java_only:
        native_lines = run(out, 'line-index-jni-oracle', [*line_options,
                           library.resolve(), sha(library)])
        if 'M3_BYTE_LINE_INDEX_PASS assertions=' not in native_lines or 'kernel=JNI_DIRECT_C11' not in native_lines:
            raise RuntimeError('line-index JNI contract oracle did not execute')

    search_class = 'com.m3.runtime.capability.BytePatternSearchChecks'
    search_options = [java, '-Xmx128m', '-Xcheck:jni', '--module-path', classes,
                      '--module', 'com.m3.runtime.capability/' + search_class]
    java_patterns = run(out, 'byte-pattern-java-oracle', search_options)
    if 'M3_BYTE_PATTERN_SEARCH_PASS assertions=' not in java_patterns or 'kernel=JAVA_KMP' not in java_patterns:
        raise RuntimeError('actual source byte-pattern Java oracle not executed')
    native_patterns = 'NOT_RUN'
    if not args.java_only:
        native_patterns = run(out, 'byte-pattern-jni-oracle', [*search_options,
                                 library.resolve(), sha(library)])
        if 'M3_BYTE_PATTERN_SEARCH_PASS assertions=' not in native_patterns or 'kernel=JNI_DIRECT_C11' not in native_patterns:
            raise RuntimeError('actual source byte-pattern JNI oracle not executed')

    evidence_class = 'com.m3.runtime.capability.ByteEvidenceQueryChecks'
    evidence_options = [java, '-Xmx128m', '-Xcheck:jni', '--module-path', classes,
                        '--module', 'com.m3.runtime.capability/' + evidence_class]
    java_evidence = run(out, 'byte-evidence-java-oracle', evidence_options)
    if 'M3_BYTE_EVIDENCE_QUERY_PASS assertions=' not in java_evidence or 'kernel=JAVA_SOURCE_PROOF' not in java_evidence:
        raise RuntimeError('source-backed evidence Java oracle not executed')
    native_evidence = 'NOT_RUN'
    if not args.java_only:
        native_evidence = run(out, 'byte-evidence-jni-oracle', [*evidence_options,
                               library.resolve(), sha(library)])
        if 'M3_BYTE_EVIDENCE_QUERY_PASS assertions=' not in native_evidence or 'kernel=JAVA_JNI_KMP' not in native_evidence:
            raise RuntimeError('source-backed evidence JNI differential not executed')

    image_proof = 'NOT_RUN'
    if args.developer_image:
        jmod, jlink = shutil.which('jmod'), shutil.which('jlink')
        if not jmod or not jlink:
            raise RuntimeError('JDK21 jmod and jlink required for actual developer image')
        production = out / 'production-classes'
        modular = out / 'modular-jmods'
        smoke = out / 'image-smoke-classes'
        production.mkdir(); modular.mkdir(); smoke.mkdir()
        real_main = sorted((root / 'src/main/java').rglob('*.java'))
        run(out, 'module-javac', [javac, '--release', '21', '-Xlint:all,-module',
                                  '-Werror', '-d', production, *real_main])
        module_file = modular / 'com.m3.runtime.capability.jmod'
        run(out, 'jmod-create', [jmod, 'create', '--class-path', production,
                                  '--module-version', '0.1.0', module_file])
        description = run(out, 'jmod-describe', [jmod, 'describe', module_file])
        if not description.startswith('com.m3.runtime.capability@0.1.0\n'):
            raise AssertionError('JMOD target module name/version drift')
        jdk = Path(os.path.realpath(javac)).parent.parent
        image = out / 'developer-image'
        dependencies = 'com.m3.runtime.capability,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jpackage,jdk.jdeps'
        run(out, 'jlink-developer', [jlink, '--module-path',
                                    str(jdk / 'jmods') + os.pathsep + str(modular),
                                    '--add-modules', dependencies, '--output', image,
                                    '--strip-debug', '--no-man-pages', '--no-header-files'])
        exe = '.exe' if os.name == 'nt' else ''
        for binary in ('java', 'javac', 'jmod', 'jlink', 'jpackage', 'jdeps'):
            if not (image / 'bin' / (binary + exe)).is_file():
                raise AssertionError('developer image lacks ' + binary)
        smoke_source = root / 'src/test/java/com/m3/runtime/proof/CapabilityImageSmoke.java'
        run(out, 'image-javac', [image / 'bin' / ('javac' + exe), '--add-modules',
                                 'com.m3.runtime.capability', '-Xlint:all', '-Werror',
                                 '-d', smoke, smoke_source])
        image_proof = run(out, 'image-api', [image / 'bin' / ('java' + exe),
                            '--add-modules', 'com.m3.runtime.capability',
                            '-cp', smoke, 'com.m3.runtime.proof.CapabilityImageSmoke'])
        if image_proof != 'M3_CAPABILITY_DEVELOPER_IMAGE_PASS':
            raise AssertionError('developer image API did not execute')
    actual = {p.relative_to(root).as_posix(): sha(p) for p in tracked}
    if actual != expected:
        raise AssertionError('real sources mutated during verification')
    report = {'java': pure, 'jni': accelerated, 'source_sha256': expected,
              'status': 'JAVA_JNI_PARITY_PASS' if not args.java_only else 'JAVA_ONLY_PASS',
              'full_openjdk_jtreg': 'NOT_RUN', 'maven_openrewrite_sdk': 'NOT_RUN',
              'performance_crossover': 'NOT_RUN',
              'stock_jdk21_developer_image': image_proof,
              'byte_line_java': java_lines, 'byte_line_jni': native_lines,
              'byte_pattern_java': java_patterns, 'byte_pattern_jni': native_patterns,
              'byte_evidence_java': java_evidence, 'byte_evidence_jni': native_evidence}
    (out / 'REPORT.json').write_text(json.dumps(report, indent=2, sort_keys=True) + '\n')
    print(json.dumps({'status': report['status'], 'java': pure, 'jni': accelerated, 'image': image_proof}))


if __name__ == '__main__':
    main()
