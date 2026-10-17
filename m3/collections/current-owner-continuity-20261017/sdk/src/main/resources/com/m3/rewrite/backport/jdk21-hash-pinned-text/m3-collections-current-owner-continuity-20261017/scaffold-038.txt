#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
"""Read-only verification of recipe-emitted M3 Java/JNI source. No transformation engine here."""
import argparse, hashlib, json, os, shutil, subprocess, sys, xml.etree.ElementTree as ET
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--deps', type=Path, required=True)
    parser.add_argument('--junit', type=Path, required=True)
    args = parser.parse_args()
    root, deps, junit = args.root.resolve(), args.deps.resolve(), args.junit.resolve()
    owner = root / 'm3/collections'; packet = owner / 'counts/recipe'
    target = owner / 'counts/target/verification'; target.mkdir(parents=True, exist_ok=True)
    evidence = target / 'evidence'; evidence.mkdir(exist_ok=True)
    java_home = Path(os.environ.get('JAVA_HOME', str(Path(shutil.which('javac')).resolve().parent.parent)))
    if not sys.platform.startswith('linux'):
        raise SystemExit('Linux JNI qualification required; no partial overall PASS')
    def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
    assert digest(junit) == '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a', 'JUnit identity'
    def run(name, command, failure_text=None):
        result = subprocess.run([str(x) for x in command], cwd=root, capture_output=True, text=True, timeout=180)
        output = result.stdout + result.stderr
        (evidence / (name + '.log')).write_text('$ ' + repr([str(x) for x in command]) + '\n' + output + '\nEXIT=' + str(result.returncode) + '\n')
        if failure_text:
            assert result.returncode != 0 and failure_text in output, (name, output[-6000:])
        elif result.returncode:
            raise RuntimeError(name + ':\n' + output[-12000:])
        print(name + (': EXPECTED ORIGINAL FAILURE' if failure_text else ': PASS'), flush=True)
        return output
    identity = run('java-identity', [java_home / 'bin/java', '-version'])
    assert 'version "21' in identity
    recipes = packet / 'src/main/resources/com/synexia/rewrite'
    expected = {}
    for kind in ['java', 'text']:
        resource = recipes / ('hash-pinned-' + kind) / 'm3-counted-receiver'
        for line in (resource / 'manifest.tsv').read_text().splitlines():
            if not line or line.startswith('#'): continue
            path, before, after, filename = line.split('\t')
            assert digest(resource / filename) == after, ('template drift', path)
            actual = path if kind == 'java' else 'm3/collections/' + path
            assert actual not in expected
            expected[actual] = after
            assert digest(root / actual) == after, ('emitted source drift', actual)
    receipt = dict(line.split('\t') for line in (owner / 'counts/recipe-receipt.tsv').read_text().splitlines() if line and not line.startswith('#'))
    assert receipt == expected, 'not the exact complete OpenRewrite output set'
    providers = {'M3HashPinnedJavaSnapshotRecipe.java': 'd58ca50dfaeb60f581b64f43545e688aca5c6530', 'M3HashPinnedTextSnapshotRecipe.java': '2db1257a40d21be06ba2a67da87aff0311183809'}
    for name, expected_blob in providers.items():
        data = (packet / 'pinned-engine/com/synexia/rewrite' / name).read_bytes()
        assert hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest() == expected_blob, name
    jars = sorted(p for p in deps.glob('*.jar') if not p.name.startswith('m3-'))
    assert any(p.name == 'guava-33.4.8-jre.jar' for p in jars)
    assert any(p.name == 'eclipse-collections-11.1.0.jar' for p in jars)
    cp = os.pathsep.join(map(str, jars))
    core, interop, tests, old_tests, original = [target / p for p in ['core', 'interop', 'tests', 'old-interop-tests', 'original']]
    for directory in [core, interop, tests, old_tests, original]:
        shutil.rmtree(directory, ignore_errors=True); directory.mkdir()
    javac = [java_home / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror']
    main_sources = sorted((owner / 'src/main/java').rglob('*.java'))
    run('core-compile', javac + ['-d', core] + main_sources)
    interop_sources = sorted((owner / 'interop/src/main/java').rglob('*.java'))
    run('interop-compile', javac + ['-cp', str(core) + os.pathsep + cp, '-d', interop] + interop_sources)
    runtime_cp = os.pathsep.join([str(core), str(interop), cp])
    test_sources = sorted((owner / 'counts/src').rglob('*.java'))
    run('counted-tests-compile', javac + ['-cp', runtime_cp + os.pathsep + str(junit), '-d', tests] + test_sources)
    run('old-interop-compile', javac + ['-cp', runtime_cp + os.pathsep + str(junit), '-d', old_tests] + sorted((owner / 'interop/src/test/java').rglob('*.java')))
    core_tests = target / 'core-tests'; shutil.rmtree(core_tests, ignore_errors=True); core_tests.mkdir()
    run('all-core-tests-compile', javac + ['-cp', str(core) + os.pathsep + str(junit), '-d', core_tests] + sorted((owner / 'src/test/java').rglob('*.java')))
    native = owner / 'src/main/native/collections/m3_count_lanes.c'; library = target / 'libm3_count_lanes.so'
    run('native-compile', [os.environ.get('CC', 'gcc'), '-std=c17', '-O2', '-Wall', '-Wextra', '-Werror', '-fsanitize=undefined', '-fno-sanitize-recover=all', '-shared', '-fPIC', '-I' + str(java_home / 'include'), '-I' + str(java_home / 'include/linux'), native, '-o', library])
    def suite(name, classpath, count, extra=None):
        reports = evidence / name; shutil.rmtree(reports, ignore_errors=True)
        run(name, [java_home / 'bin/java', '-Xcheck:jni', '-Dm3.counted.native=' + str(library), '-jar', junit, 'execute', '--class-path', classpath, '--scan-class-path', '--fail-if-no-tests', '--reports-dir', reports] + (extra or []))
        files = list(reports.glob('TEST-*.xml')); assert files, name
        totals = {k: sum(int(ET.parse(p).getroot().attrib.get(k, 0)) for p in files) for k in ['tests', 'failures', 'errors', 'skipped']}
        assert totals == {'tests': count, 'failures': 0, 'errors': 0, 'skipped': 0}, (name, totals)
        return totals
    results = {'new-runtime': suite('new-runtime', runtime_cp + os.pathsep + str(tests), 16),
               'old-interop': suite('old-interop', runtime_cp + os.pathsep + str(old_tests), 42),
               'long-core': suite('long-core', str(core) + os.pathsep + str(core_tests), 13, ['--include-classname', '.*M3Long.*Test'])}
    pre = recipes / 'hash-pinned-java/m3-counted-receiver/pre'; original_source = target / 'original-source'; original_source.mkdir(exist_ok=True)
    baseline_sources = []
    for p in sorted(pre.glob('*.java.txt')):
        q = original_source / p.name.removesuffix('.txt'); shutil.copyfile(p, q); baseline_sources.append(q)
    run('original-compile', javac + ['-cp', core, '-d', original] + baseline_sources)
    for p in baseline_sources:
        symbol = 'com.m3.collections.' + p.stem
        before = run(p.stem + '-api-before', [java_home / 'bin/javap', '-public', '-classpath', original, symbol])
        after = run(p.stem + '-api-after', [java_home / 'bin/javap', '-public', '-classpath', core, symbol])
        assert before == after, ('public API drift', symbol)
    for mode in ['map', 'set', 'linked', 'remove']:
        message = 'removal/aggregate split by optional compaction failure' if mode == 'remove' else 'partially published storage'
        for version in ['original', 'candidate']:
            classpath = os.pathsep.join(([str(original)] if version == 'original' else []) + [str(core), str(tests)])
            run('oom-' + mode + '-' + version, [java_home / 'bin/java', '-Xmx32m', '-XX:+UseSerialGC', '-XX:NewRatio=16', '-Dm3.pressure.mib=12', '-cp', classpath, 'com.m3.collections.M3AllocationFailureProbe', mode], message if version == 'original' else None)
    run('catalogue-query', [java_home / 'bin/java', '-cp', tests, 'com.m3.collections.tooling.M3CountedCatalogue', owner / 'counts/catalogue.tsv', 'frequency'])
    meta = core / 'META-INF/m3-counted'; meta.mkdir(parents=True, exist_ok=True)
    for name in ['LICENSE', 'NOTICE']: shutil.copyfile(owner / name, core / 'META-INF' / name)
    for name in ['provenance.json', 'catalogue.tsv', 'recipe-receipt.tsv', 'M3_RECIPE_FIRST_INVARIANT.md']:
        shutil.copyfile(owner / 'counts' / name, meta / name)
    jar = target / 'm3-collections.jar'; run('package-core', [java_home / 'bin/jar', '--create', '--file', jar, '-C', core, '.'])
    assert run('core-dependencies', [java_home / 'bin/jdeps', '--print-module-deps', jar]).strip() == 'java.base'
    interop_meta = interop / 'META-INF/m3-counted'; interop_meta.mkdir(parents=True, exist_ok=True)
    for p in meta.iterdir(): shutil.copyfile(p, interop_meta / p.name)
    for name in ['LICENSE', 'NOTICE']: shutil.copyfile(owner / name, interop / 'META-INF' / name)
    run('package-interop', [java_home / 'bin/jar', '--create', '--file', target / 'm3-collections-interop.jar', '-C', interop, '.'])
    receipt = {'schema': 'M3_COUNTED_EXECUTION_V1', 'generated_files': len(expected), 'core_main_units': len(main_sources), 'interop_main_units': len(interop_sources), 'runtime_suites': results, 'allocation_negative_controls': 4, 'allocation_candidate_passes': 4, 'public_api_unchanged_owners': 3, 'jni': 'C17 UBSan and -Xcheck:jni against BigInteger oracle', 'full_core_runtime_suite': 'NOT_COMPLETED; only long-core runtime slice claimed', 'full_reactor': 'NOT_RUN', 'installed_jdk': 'NOT_RUN', 'performance_claim': False, 'phase_advance': False, 'output_sha256': expected}
    (evidence / 'qualification.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))

if __name__ == '__main__': main()
