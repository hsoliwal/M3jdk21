#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Verify exact recipe outputs and actual Java/JNI; never transform source."""
import argparse, hashlib, json, os, shutil, subprocess, sys
from pathlib import Path
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for option in ('root', 'deps', 'junit'):
        parser.add_argument('--' + option, type=Path, required=True)
    args = parser.parse_args()
    root, deps, junit = args.root.resolve(), args.deps.resolve(), args.junit.resolve()
    owner = root/'m3/collections'; packet = owner/'bimap'; target = packet/'target/verification'
    target.mkdir(parents=True, exist_ok=True)
    evidence = target/'evidence'; evidence.mkdir(exist_ok=True)
    java = Path(os.environ.get('JAVA_HOME', str(Path(shutil.which('javac')).resolve().parent.parent)))
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    if not sys.platform.startswith('linux') or digest(junit) != '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a':
        raise RuntimeError('Linux and exact JUnit console required')
    def run(name, argv):
        argv = list(map(str, argv))
        p = subprocess.run(argv, cwd=root, text=True, capture_output=True, timeout=300)
        (evidence/(name+'.log')).write_text('$ '+repr(argv)+'\n'+p.stdout+'\nEXIT='+str(p.returncode)+'\n')
        (evidence/(name+'.stderr.log')).write_text(p.stderr)
        if p.returncode or 'WARNING in native method' in p.stdout+p.stderr or 'runtime error:' in p.stderr:
            raise RuntimeError(name+'\n'+(p.stdout+p.stderr)[-14000:])
        print(name+': PASS', flush=True)
        return p.stdout
    expected = {}
    for kind in ('java', 'text'):
        resources = packet/'recipe/src/main/resources/com/synexia/rewrite'/('hash-pinned-'+kind)/'m3-bimap'
        for line in (resources/'manifest.tsv').read_text().splitlines():
            if not line or line.startswith('#'): continue
            path, before, after, filename = line.split('\t')
            full = path if kind == 'java' else 'm3/collections/'+path
            if before != 'ABSENT' or full in expected or digest(resources/filename) != after or digest(root/full) != after:
                raise ValueError('manifest/output drift: '+full)
            expected[full] = after
    receipt = dict(line.split('\t') for line in (packet/'recipe-receipt.tsv').read_text().splitlines() if line and not line.startswith('#'))
    if receipt != expected or len(expected) != 10: raise ValueError('incomplete execution receipt')
    for atom in json.loads((packet/'provenance.json').read_text())['borrowed_atoms']:
        if digest(root/atom['path']) != atom['sha256']: raise ValueError('borrowed owner changed: '+atom['path'])
    run('predecessor', [sys.executable, owner/'counts/totals/verify.py', '--root', root, '--deps', deps, '--junit', junit])
    previous = json.loads((owner/'counts/totals/target/evidence/qualification.json').read_text())
    core = owner/'counts/target/verification/core'; interop = owner/'counts/target/verification/interop'
    tests = target/'tests'; headers = target/'headers'
    for p in (tests, headers): shutil.rmtree(p, ignore_errors=True); p.mkdir()
    jars = sorted(p for p in deps.glob('*.jar') if not p.name.startswith('m3-'))
    cp = os.pathsep.join(map(str, [core, interop, tests]+jars))
    run('test-compile', [java/'bin/javac', '--release', '21', '-proc:none', '-Xlint:all', '-Werror', '-h', headers,
                         '-cp', cp+os.pathsep+str(junit), '-d', tests]+sorted((packet/'src/test/java').rglob('*.java')))
    library = target/'libm3_bimap_consumer.so'
    run('native-compile', [os.environ.get('CC', 'gcc'), '-std=c17', '-O2', '-Wall', '-Wextra', '-Werror', '-shared', '-fPIC',
                          '-fsanitize=undefined', '-fno-sanitize-recover=all', '-I'+str(java/'include'), '-I'+str(java/'include/linux'),
                          '-I'+str(headers), owner/'src/test/native/collections/m3_bimap_consumer.c', '-o', library])
    if 'Java_com_m3_collections_interop_M3BiMapNativeTest_call' not in run('native-symbols', ['nm', '-D', '--defined-only', library]):
        raise RuntimeError('JNI export missing')
    reports = evidence/'runtime'; shutil.rmtree(reports, ignore_errors=True)
    output = run('runtime', [java/'bin/java', '-Xcheck:jni', '-Dm3.bimap.native='+str(library), '-jar', junit,
                            'execute', '--class-path', cp, '--select-class', 'com.m3.collections.interop.M3BiMapTest',
                            '--select-class', 'com.m3.collections.interop.M3BiMapNativeTest', '--fail-if-no-tests', '--reports-dir', reports])
    files = list(reports.glob('TEST-*.xml'))
    totals = {k: sum(int(ET.parse(p).getroot().attrib.get(k, 0)) for p in files) for k in ('tests', 'failures', 'errors', 'skipped')}
    if totals != {'tests':26, 'failures':0, 'errors':0, 'skipped':0} or 'BIMAP_DIFFERENTIAL_OPERATIONS=48000' not in output:
        raise RuntimeError('incomplete runtime proof: '+repr(totals))
    if (evidence/'runtime.stderr.log').read_text(): raise RuntimeError('unexpected native/test stderr')
    run('public-api', [java/'bin/javap', '-public', '-classpath', interop, 'com.m3.collections.interop.M3BiMap', 'com.m3.collections.interop.M3BiMaps'])
    run('catalogue', [java/'bin/java', '-cp', owner/'counts/target/verification/tests', 'com.m3.collections.tooling.M3CountedCatalogue', packet/'catalogue.tsv', 'bijective'])
    meta = interop/'META-INF/m3-bimap'; meta.mkdir(parents=True, exist_ok=True)
    for name in ('README.md', 'THIRD_PARTY_NOTICES.md', 'provenance.json', 'catalogue.tsv', 'name-mapping.tsv', 'recipe-receipt.tsv'):
        shutil.copyfile(packet/name, meta/name)
    jar = target/'m3-collections-interop-with-bimap.jar'
    run('package', [java/'bin/jar', '--create', '--file', jar, '-C', interop, '.'])
    result = {'schema':'M3_BIMAP_VERIFIED_V1', 'runtime':totals, 'seeded_bimap_operations':48000,
              'retained_scalar_runtime':previous['runtime'], 'retained_runtime':previous['predecessor_runtime'],
              'allocation_negative_controls':previous['predecessor_allocation_negative_controls'],
              'allocation_candidate_passes':previous['predecessor_allocation_candidate_passes'],
              'core_main_units':previous['core_main_units'], 'core_runtime_requires':['java.base'],
              'new_jni':'test-only Java BiMap consumer; C17 fatal UBSan; -Xcheck:jni; empty stderr',
              'output_sha256':expected, 'existing_owners_changed':False, 'jar_sha256':digest(jar),
              'full_library_coverage':False, 'eclipse_mutable_bimap':'NOT_IMPLEMENTED; explicit import and MutableMap view only',
              'installed_jdk':'NOT_RUN', 'full_reactor':'NOT_RUN', 'phase_advance':False}
    (evidence/'qualification.json').write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__': main()
