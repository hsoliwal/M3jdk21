#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Build the source-JUnit closure, owning collection module and every collection JNI library."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[4]
MODULE = ROOT / 'm3/collections'
ARRAYS = ROOT / 'm3/arrays'
OUT = MODULE / 'target/full-java-jni'
JAVA = Path(os.environ['JAVA_HOME'])
VENDOR = ROOT / 'm3/vendor/third_party'
DEPENDENCIES = ('apiguardian-api', 'opentest4j', 'univocity-parsers',
                'junit-platform-commons-1.10.2', 'junit-platform-engine-1.10.2',
                'junit-jupiter-api-5.10.2', 'junit-jupiter-params-5.10.2',
                'junit-jupiter-engine-5.10.2', 'junit-jupiter-5.10.2')
# Existing library names and generated JNI headers; no second native owner.
LIBRARIES = {
    'm3_collections_mask': [('m3_index_mask_compactor.c', 'M3IndexMaskCompactor'),
                            ('m3_index_mask_window.c', 'M3IndexMaskWindow')],
    'm3_collections_pathmask': [('m3_index_path_masks.c', 'M3IndexPathMasks')],
    'm3_collections_long_path_masks': [('m3_long_index_path_masks_jni.c', 'M3LongIndexPathMasksJNI')],
    'm3_collections_address': [('m3_long_address_kernel.c', 'M3LongAddressKernel')],
    'm3_collections_span': [('m3_span_index_jni.c', 'M3SpanIndex')],
    'm3_collections_statehist': [('m3_state_mask_histogram.c', 'M3StateMaskHistogram')],
    'm3rankbatch': [('m3_long_rank_batch_jni.c', 'M3LongRankBatchNative')],
    'm3signalorder': [('m3_signal_order_jni.c', 'M3SignalOrderNative')],
    'm3_residency_state_mask': [('m3_residency_state_mask.c', 'M3ResidencyStateMaskIndex')],
    'm3bitlane': [('segmented_bit_lane.c', 'M3BitLane28')],
}

def run(args):
    print('+ ' + ' '.join(map(str, args)), flush=True)
    subprocess.run(list(map(str, args)), cwd=ROOT, check=True)

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def maven(pom, *args):
    command = [os.environ.get('MVN', 'mvn'), '-B', '-ntp']
    if os.environ.get('M3_MAVEN_OFFLINE') == '1': command.append('-o')
    if os.environ.get('M2_REPO'): command.append('-Dmaven.repo.local=' + os.environ['M2_REPO'])
    run(command + ['-f', pom, *args])

def reports(mode):
    source = MODULE / 'target/surefire-reports'
    destination = OUT / (mode + '-reports')
    if destination.exists(): shutil.rmtree(destination)
    shutil.copytree(source, destination)
    totals = dict.fromkeys(('tests', 'failures', 'errors', 'skipped'), 0)
    for path in destination.glob('TEST-*.xml'):
        suite = ET.parse(path).getroot()
        for key in totals: totals[key] += int(suite.get(key, '0'))
    if not totals['tests'] or totals['failures'] or totals['errors']:
        raise RuntimeError('Owning suite did not pass: ' + str(totals))
    if mode == 'native' and totals['skipped']:
        raise RuntimeError('Required-native suite skipped tests: ' + str(totals))
    return totals

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'PROOF.json').unlink(missing_ok=True)  # Never leave a prior PASS after a failed rerun.
    for name in ('headers', 'classes', 'fixture-classes', 'native', 'missing'):
        path = OUT / name
        if path.exists(): shutil.rmtree(path)
        path.mkdir()
    head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    expected = os.environ.get('EXPECTED_HEAD')
    if expected and head != expected: raise RuntimeError('Checked-out head differs from expected head')
    dependency_proof = {}
    for module in DEPENDENCIES:
        owner = VENDOR / module
        run(['python3', owner / 'verify_source.py'])
        maven(owner / 'pom.xml', '-DskipTests', 'install')
        ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
        pom = ET.parse(owner / 'pom.xml').getroot()
        artifact = pom.findtext('m:artifactId', namespaces=ns)
        version = pom.findtext('m:version', namespaces=ns)
        jar = owner / 'target' / (artifact + '-' + version + '.jar')
        run(['python3', owner / 'verify_source.py', jar])
        dependency_proof[module] = {'provenance_sha256': sha(owner / 'PROVENANCE.json'),
                                    'jar_sha256': sha(jar)}
    # First-party arrays are a declared Java module dependency, not a
    # classpath stub or a transitive java.base feature. Build their actual
    # source owner after the vendored JUnit dependency closure.
    arrays_sources = sorted((ARRAYS / 'src/main/java').rglob('*.java'))
    arrays_descriptor = ARRAYS / 'src/main/java/module-info.java'
    if not arrays_sources or arrays_descriptor not in arrays_sources:
        raise RuntimeError('Missing original com.m3.arrays module source')
    maven(ARRAYS / 'pom.xml', '-DskipTests', 'install')
    arrays_jar = ARRAYS / 'target/m3-arrays-0.1.0-SNAPSHOT.jar'
    if not arrays_jar.is_file():
        raise RuntimeError('Built original com.m3.arrays module artifact missing')
    with zipfile.ZipFile(arrays_jar) as arrays_package:
        if 'module-info.class' not in arrays_package.namelist():
            raise RuntimeError('M3 arrays built artifact lost its module descriptor')
    array_module = subprocess.check_output(
        [JAVA / 'bin/jar', '--describe-module', '--file', arrays_jar],
        text=True).splitlines()[0].split()[0].split('@')[0]
    if array_module != 'com.m3.arrays':
        raise RuntimeError('Unexpected product arrays module identity: ' + array_module)

    sources = sorted((MODULE / 'src/main/java').rglob('*.java'))
    tests = sorted((MODULE / 'src/test/java').rglob('*.java'))
    (OUT / 'main.args').write_text('\n'.join(map(str, sources)) + '\n')
    run([JAVA / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror', '-Xpkginfo:always',
         '--module-path', arrays_jar,
         '-h', OUT / 'headers', '-d', OUT / 'classes', '@' + str(OUT / 'main.args')])
    native = MODULE / 'src/main/native/collections'
    admitted = {native / name for rows in LIBRARIES.values() for name, _ in rows}
    if admitted != set(native.glob('*.c')):
        raise RuntimeError('Native source inventory changed; review library mapping before qualification')
    cc = [os.environ.get('CC', 'cc'), '-std=c17', '-Wall', '-Wextra', '-Werror',
          '-fsanitize=undefined', '-fno-sanitize-recover=undefined', '-O2',
          '-I' + str(JAVA / 'include'), '-I' + str(JAVA / 'include/linux'),
          '-I' + str(OUT / 'headers')]
    for library, rows in LIBRARIES.items():
        objects = []
        for name, owner in rows:
            obj = OUT / 'native' / (name + '.o')
            run(cc + ['-fPIC', '-include', OUT / 'headers' / ('com_m3_collections_' + owner + '.h'),
                      '-c', native / name, '-o', obj])
            objects.append(obj)
        run(cc + ['-shared', *objects, '-o', OUT / 'native' / ('lib' + library + '.so')])
    # Existing navigation fixture has its own test-only JNI ABI; keep it out of the product library.
    run([JAVA / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror',
         '-cp', os.pathsep.join((str(OUT / 'classes'), str(arrays_jar))),
         '-h', OUT / 'headers', '-d', OUT / 'fixture-classes',
         MODULE / 'src/test/java/com/m3/collections/M3BitLaneJumpProof.java'])
    run(cc + ['-shared', '-fPIC', '-include', OUT / 'headers/com_m3_collections_M3BitLaneJumpProof.h',
              MODULE / 'src/test/native/m3_bitlane_jump_jni.c', '-o',
              OUT / 'native/libm3_bitlane_jump_test.so'])
    for fixture in ('spans', 'regions'):
        executable = OUT / 'native' / fixture
        run(cc + [native / 'm3_index_mask_window.c', MODULE / 'src/test/native' /
                  ('m3_mask_window_' + fixture + '.c'), '-o', executable])
        run([executable])
    gather_fixture = OUT / 'native/gather-spans'
    run(cc + [native / 'm3_long_address_kernel.c', MODULE / 'src/test/native/m3_gather_spans.c',
              '-o', gather_fixture])
    run([gather_fixture])
    common = ['-Xmx768m', '-XX:ActiveProcessorCount=2', '-Xcheck:jni',
              '-Dm3.collections.rank.native=true', '-Dm3.collections.signal.native=true']
    results = {}
    for mode, required in [('java', 'false'), ('native', 'true')]:
        library = OUT / ('native' if mode == 'native' else 'missing')
        vm = common + ['-Djava.library.path=' + str(library),
                       '-Dm3.mask.spans.native.required=' + required,
                       '-Dm3.collections.all.native.required=' + required]
        old_reports = MODULE / 'target/surefire-reports'
        if old_reports.exists(): shutil.rmtree(old_reports)
        maven(MODULE / 'pom.xml', '-DargLine=' + ' '.join(vm), '-DfailIfNoTests=true', 'verify')
        results[mode] = reports(mode)
    jar = MODULE / 'target/m3-collections-0.1.0-SNAPSHOT.jar'
    with zipfile.ZipFile(jar) as archive:
        expected_classes = {str(p.relative_to(OUT / 'classes')) for p in (OUT / 'classes').rglob('*.class')}
        actual_classes = {name for name in archive.namelist() if name.endswith('.class')}
        if expected_classes != actual_classes: raise RuntimeError('Packaged class inventory differs from fresh compilation')
        for name in ('LICENSE', 'NOTICE', 'provenance.json', 'synexia-donation.json', 'name-mapping.json', 'SYNEXIA-DONATION-NOTICE.txt', 'NETBEANS-NOTICE.txt'):
            if archive.read('META-INF/' + name) != (MODULE / name).read_bytes():
                raise RuntimeError('Packaged module legal/provenance bytes differ: ' + name)
    runtime_modules = subprocess.check_output(
        [JAVA / 'bin/jdeps', '--module-path', arrays_jar,
         '--print-module-deps', jar], text=True).strip()
    if set(runtime_modules.split(',')) != {'java.base', 'com.m3.arrays'}:
        raise RuntimeError('Unexpected runtime dependencies: ' + runtime_modules)
    cp = os.pathsep.join((str(MODULE / 'target/classes'),
                          str(MODULE / 'target/test-classes'), str(arrays_jar)))
    java = [JAVA / 'bin/java', *common, '-Djava.library.path=' + str(OUT / 'native'), '-cp', cp]
    contracts = [('M3MaskWindowSparseContract', ['native']),
                 ('M3BudgetedMaskWindowContract', ['--native']),
                 ('M3IndexMaskWindowSelfTest', ['--native']),
                 ('M3IndexMaskWindowRecoverySelfTest', ['--native']),
                 ('M3IndexMaskCompactorSelfTest', ['--native']),
                 ('M3IndexPathMasksSelfTest', ['--native']),
                 ('M3LongIndexPathMasksJNISelfTest', ['--native']),
                 ('M3LongAddressBoundedSelfTest', []),
                 ('M3IndexPathMaskWindowSelfTest', ['--native']),
                 ('M3IndexPathRankNativeSelfTest', []),
                 ('M3NativeIndexRangeSelfTest', ['range', 'sharing']),
                 ('M3ObjectStateSlots28SelfTest', ['--native']),
                 ('M3IndexedTableSelfTest', ['--native']),
                 ('M3BagRankIndexSelfTest', ['--native']),
                 ('M3SdkRecoverySelfTest', ['--native']),
                 ('M3SignalOrderContract', ['native']),
                 ('M3SignalDimensionsNativeContract', []),
                 ('M3ResidencyStateMaskProof', ['native']),
                 ('M3BitLaneJumpProof', [str(OUT / 'native/libm3_bitlane_jump_test.so')])]
    for name, args in contracts: run(java + ['com.m3.collections.' + name, *args])
    run([JAVA / 'bin/java', *common, '-Djava.library.path=' + str(OUT / 'missing'), '-cp', cp,
         'com.m3.collections.M3BudgetedMaskWindowContract', '--missing'])
    run([JAVA / 'bin/java', *common, '-Djava.library.path=' + str(OUT / 'missing'), '-cp', cp,
         'com.m3.collections.M3LongIndexPathMasksJNISelfTest'])
    native_fixtures = [MODULE / 'src/test/native' / name for name in
                       ('m3_mask_window_spans.c', 'm3_mask_window_regions.c', 'm3_bitlane_jump_jni.c', 'm3_gather_spans.c')]
    proof = {'status': 'PASS_OWNING_COLLECTION_JAVA_AND_JNI', 'head_sha': head,
             'scope': 'independent m3/collections; no JDK phase or challenge-corpus promotion',
             'worktree_dirty': bool(subprocess.check_output(['git', 'status', '--porcelain', '--untracked-files=no'], cwd=ROOT)),
             'jar_sha256': sha(jar), 'jar_classes': len(expected_classes), 'runtime_modules': runtime_modules,
             'java_sources': len(sources), 'test_sources': len(tests), 'junit': results,
             'source_dependencies': dependency_proof,
             'first_party_module_dependencies': {'com.m3.arrays': {
                 'module_descriptor_sha256': sha(arrays_descriptor),
                 'artifact_sha256': sha(arrays_jar),
                 'source_sha256': {str(p.relative_to(ROOT)): sha(p) for p in arrays_sources}}},
             'native_libraries': sorted(LIBRARIES),
             'explicit_native_contracts': [name for name, _ in contracts],
             'native': 'generated headers; C17 strict; fatal UBSan; actual -Xcheck:jni',
             'source_sha256': {str(p.relative_to(ROOT)): sha(p) for p in sources + tests + sorted(admitted) + native_fixtures}}
    (OUT / 'PROOF.json').write_text(json.dumps(proof, indent=2) + '\n')
    print(json.dumps({k: proof[k] for k in ('status', 'head_sha', 'junit', 'native_libraries')}, indent=2))

if __name__ == '__main__': main()
