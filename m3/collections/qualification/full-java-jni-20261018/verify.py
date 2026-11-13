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
OUT = MODULE / 'target/full-java-jni'
JAVA = Path(os.environ['JAVA_HOME'])
VENDOR = ROOT / 'm3/vendor/third_party'
DEPENDENCIES = ('apiguardian-api', 'opentest4j', 'univocity-parsers',
                'junit-platform-commons-1.10.2', 'junit-platform-engine-1.10.2',
                'junit-jupiter-api-5.10.2', 'junit-jupiter-params-5.10.2',
                'junit-jupiter-engine-5.10.2', 'junit-jupiter-5.10.2')
# Existing library names and generated JNI headers; no second native owner.
LIBRARIES = {'m3_disjoint_index': {'sources': ['collections/m3_disjoint_index.c'],
                       'owners': ['jdk.internal.m3.collections.DisjointIndex'],
                       'requires': None},
 'm3_collections_address': {'sources': ['collections/m3_long_address_kernel.c'],
                            'owners': ['jdk.internal.m3.collections.LaneAlgorithms',
                                       'jdk.m3.collections.LongAddressKernel'],
                            'requires': None},
 'm3_collections_long_path_masks': {'sources': ['collections/m3_long_index_path_masks_jni.c'],
                                    'owners': ['jdk.internal.m3.collections.LongIndexPathMasksJNI'],
                                    'requires': None},
 'm3rankbatch': {'sources': ['collections/m3_long_rank_batch_jni.c'],
                 'owners': ['jdk.internal.m3.collections.LongRankBatchNative'],
                 'requires': None},
 'm3signalorder': {'sources': ['collections/m3_object_signal_jni.c', 'collections/m3_signal_order_jni.c'],
                   'owners': ['jdk.internal.m3.collections.ObjectSignalOps',
                              'jdk.m3.collections.SignalOrderNative'],
                   'requires': None},
 'm3_residency_state_mask': {'sources': ['collections/m3_residency_state_mask.c'],
                             'owners': ['jdk.internal.m3.collections.ResidencyStateMaskIndex'],
                             'requires': None},
 'm3_collections_statehist': {'sources': ['collections/m3_state_mask_histogram.c'],
                              'owners': ['jdk.internal.m3.collections.sketch.StateMaskHistogram'],
                              'requires': None},
 'm3_collections_mask': {'sources': ['collections/m3_index_mask_compactor.c',
                                     'collections/m3_index_mask_window.c',
                                     'collections/m3_index_mask_search.c'],
                         'owners': ['jdk.m3.collections.IndexMaskCompactor',
                                    'jdk.m3.collections.IndexMaskWindow'],
                         'requires': None},
 'm3_collections_pathmask': {'sources': ['collections/m3_index_path_masks.c'],
                             'owners': ['jdk.m3.collections.IndexPathMasks'],
                             'requires': None},
 'm3_collections_span': {'sources': ['collections/m3_span_index_jni.c'],
                         'owners': ['jdk.m3.collections.SpanIndex'],
                         'requires': None},
 'm3bitlane': {'sources': ['collections/segmented_bit_lane.c'],
               'owners': ['jdk.m3.collections.BitLane28'],
               'requires': None},
 'm3_packed_integer': {'sources': ['collections/m3_packed_integer_jni.c'],
                       'owners': ['jdk.internal.m3.collections.packed.PackedNative'],
                       'requires': None},
 'm3_big_longs': {'sources': ['collections/m3_big_longs.c'],
                  'owners': ['jdk.internal.m3.collections.packed.PrimitiveBigNative'],
                  'requires': None},
 'm3_doop_frozen_mask': {'sources': ['collections/m3_doop_frozen_mask.c'],
                         'owners': ['jdk.internal.m3.collections.DoopFrozenMaskedViewport'],
                         'requires': None},
 'm3_doop_mask_window': {'sources': ['collections/m3_doop_mask_window.c'],
                         'owners': ['jdk.internal.m3.collections.DoopMaskedWindowScan'],
                         'requires': None},
 'm3_count_lanes': {'sources': ['collections/m3_count_lanes.c'],
                    'owners': ['jdk.internal.m3.collections.CountLanes'],
                    'requires': None},
 'm3_count_totals': {'sources': ['collections/m3_count_totals.c'],
                     'owners': ['jdk.internal.m3.collections.CountTotals'],
                     'requires': None},
 'm3maskviewport': {'sources': ['m3maskviewport.c'],
                    'owners': ['jdk.internal.m3.collections.MaskViewportNative'],
                    'requires': None},
 'm3_value_bits': {'sources': ['collections/m3_value_bits.c'],
                   'owners': ['jdk.internal.m3.collections.PrimitiveFloat16'],
                   'requires': 'M3_C_IEC_559'},
 'm3_primitive_order': {'sources': ['collections/primitive_order_jni.c'],
                        'owners': ['jdk.m3.collections.PrimitivePartialSort$LongOrder'],
                        'requires': None},
 'm3_collections_packed': {'sources': ['collections/packed_array_bulk.c',
                                       'collections/packed_heap_selection.c',
                                       'collections/packed_sorted_sets.c',
                                       'collections/packed_stable_filter.c',
                                       'collections/packed_long_mask_page.c',
                                       'collections/packed_swiss_probe_jni.c',
                                       'collections/segmented_short_lane.c',
                                       'collections/segmented_bit_lane.c'],
                           'owners': ['jdk.internal.m3.collections.packed.PackedArrayNative',
                                      'jdk.internal.m3.collections.packed.PackedSelectionNative',
                                      'jdk.internal.m3.collections.packed.PackedLongMaskPageNative',
                                      'jdk.internal.m3.collections.packed.PackedSwissNative',
                                      'jdk.internal.m3.collections.range.SegmentedShortLane28',
                                      'jdk.m3.collections.BitLane28'],
                           'requires': None},
 'm3_mat_csr': {'sources': ['collections/m3csrtranspose.c'],
                'owners': ['jdk.internal.m3.collections.mat.MatCsrGraph'],
                'requires': None},
 'm3_matfilter': {'sources': ['collections/mat_int_filter_jni.c'],
                  'owners': ['jdk.internal.m3.collections.mat.MatIntFilterKernel'],
                  'requires': None},
 'm3_marker_topk': {'sources': ['collections/m3_marker_topk_jni.c'],
                    'owners': ['jdk.internal.m3.collections.sketch.MarkerTopKNative'],
                    'requires': None},
 'm3_mat_row': {'sources': ['collections/mat_int_row_jni.c'],
                'owners': ['jdk.internal.m3.collections.mat.MatIntRowKernel'],
                'requires': None},
 'm3_mat_bitmatrix': {'sources': ['collections/mat_bit_matrix.c'],
                       'owners': ['jdk.internal.m3.collections.mat.MatBitMatrix'],
                       'requires': None},
 'm3_packed_int_width': {'sources': ['collections/packed_int_width.c'],
                         'owners': ['jdk.internal.m3.collections.packed.PackedIntNative'],
                         'requires': None}}

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
    # Reuse the existing source-owned jdk.m3.arrays module (Eclipse/Synexia
    # collection donor), rather than compiling jdk.m3.collections as an
    # unresolved JPMS module or placing arrays classes on the class path.
    maven(ROOT / 'm3/arrays/pom.xml', '-Dmaven.test.skip=true', 'install')
    array_jar = ROOT / 'm3/arrays/target/m3-arrays-0.1.0-SNAPSHOT.jar'
    if not array_jar.is_file():
        raise RuntimeError('Source-built jdk.m3.arrays module jar is absent')
    with zipfile.ZipFile(array_jar) as archive:
        if 'module-info.class' not in archive.namelist():
            raise RuntimeError('jdk.m3.arrays module descriptor is missing')
    # Allocation atoms require the existing arrays JNI owner in both collection
    # algorithm modes. Keep it outside the collection-library search directory
    # so the missing-collection-JNI controls continue to exercise their contract.
    array_native_build = OUT / 'array-native'
    run(['cmake', '-S', ROOT / 'make/m3/arrays', '-B', array_native_build,
         '-DCMAKE_BUILD_TYPE=Release', '-DBUILD_TESTING=ON', '-DJAVA_HOME=' + str(JAVA)])
    run(['cmake', '--build', array_native_build, '--config', 'Release', '--parallel', '2'])
    run(['ctest', '--test-dir', array_native_build, '--output-on-failure', '-C', 'Release'])
    array_native = array_native_build / 'libm3_arrays.so'
    if not array_native.is_file():
        raise RuntimeError('Source-built arrays JNI allocation library is absent')
    array_native_option = '-Dm3.arrays.native.path=' + str(array_native)
    # This existing suite includes fresh-VM missing-library and silent-null
    # controls; providing the library must not weaken allocation failure rules.
    maven(ROOT / 'm3/arrays/pom.xml', '-Dm3.arrays.native.required=true',
          '-DargLine=-Xcheck:jni ' + array_native_option,
          '-Dtest=ArrayAllocationAtomsTest', '-DfailIfNoTests=true', 'test')
    dependency_proof['jdk.m3.arrays'] = {
        'jar_sha256': sha(array_jar), 'native_sha256': sha(array_native),
        'allocation_contract': 'ArrayAllocationAtomsTest'}
    sources = sorted({path for base in (MODULE / 'src/main/java',
        ROOT / 'src/jdk.m3.collections/share/classes/jdk/internal/m3/collections',
        ROOT / 'src/jdk.m3.collections/share/classes/jdk/m3/collections')
        for path in base.rglob('*.java')})
    tests = sorted({path for base in (MODULE / 'src/test/java',
        ROOT / 'test/jdk/jdk/internal/m3/collections', ROOT / 'test/jdk/jdk/m3/collections')
        for path in base.rglob('*.java')})
    (OUT / 'main.args').write_text('\n'.join(map(str, sources)) + '\n')
    run([JAVA / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror', '-Xpkginfo:always',
         '--module-path', array_jar, '-h', OUT / 'headers', '-d', OUT / 'classes',
         '@' + str(OUT / 'main.args')])
    native = ROOT / 'src/jdk.m3.collections/share/native/libm3collections'
    admitted = {native / name for entry in LIBRARIES.values() for name in entry['sources']}
    actual = set(native.rglob('*.c'))
    if admitted != actual:
        missing = sorted(str(path.relative_to(ROOT)) for path in admitted - actual)
        unexpected = sorted(str(path.relative_to(ROOT)) for path in actual - admitted)
        raise RuntimeError('Native source inventory changed; missing=' + repr(missing)
                           + '; unexpected=' + repr(unexpected))
    cc = [os.environ.get('CC', 'cc'), '-std=c17', '-Wall', '-Wextra', '-Werror', '-Wconversion', '-Wshadow', '-Wpedantic',
          '-fsanitize=undefined', '-fno-sanitize-recover=undefined', '-O2',
          '-I' + str(JAVA / 'include'), '-I' + str(JAVA / 'include/linux'),
          '-I' + str(OUT / 'headers'), '-I' + str(ROOT / 'm3/native/include')]
    for library, entry in LIBRARIES.items():
        objects = []
        headers = [OUT / 'headers' / (owner.replace('.', '_').replace('$', '_') + '.h')
                   for owner in entry['owners']]
        for name in entry['sources']:
            obj = OUT / 'native' / (Path(name).name + '.o')
            includes = [argument for header in headers for argument in ('-include', header)]
            run(cc + ['-fPIC', *includes, '-c', native / name, '-o', obj])
            objects.append(obj)
        run(cc + ['-shared', *objects, '-o', OUT / 'native' / ('lib' + library + '.so')])
    # Existing navigation fixture has its own test-only JNI ABI; keep it out of the product library.
    run([JAVA / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror',
         '-cp', OUT / 'classes', '-h', OUT / 'headers', '-d', OUT / 'fixture-classes',
         ROOT / 'test/jdk/jdk/m3/collections/BitLaneJumpProof.java'])
    run(cc + ['-shared', '-fPIC', '-include', OUT / 'headers/jdk_m3_collections_BitLaneJumpProof.h',
              ROOT / 'test/jdk/jdk/m3/collections/native/m3_bitlane_jump_jni.c', '-o',
              OUT / 'native/libm3_bitlane_jump_test.so'])
    for fixture in ('spans', 'regions'):
        executable = OUT / 'native' / fixture
        run(cc + [native / 'collections/m3_index_mask_window.c', ROOT / 'test/jdk/jdk/m3/collections/native' /
                  ('m3_mask_window_' + fixture + '.c'), '-o', executable])
        run([executable])
    gather_fixture = OUT / 'native/gather-spans'
    run(cc + [native / 'collections/m3_long_address_kernel.c', ROOT / 'test/jdk/jdk/m3/collections/native/m3_gather_spans.c',
              '-o', gather_fixture])
    run([gather_fixture])
    native_properties = [
        '-Dm3.matmatrix.native.path=' + str(OUT / 'native/libm3_mat_bitmatrix.so'),
        '-Dm3.bitmatrix.requireNative=true',
        '-Dm3.counted.native=' + str(OUT / 'native/libm3_count_lanes.so'),
        '-Dm3.totals.native=' + str(OUT / 'native/libm3_count_totals.so'),
        '-Dm3.native.path=' + str(OUT / 'native/libm3_collections_packed.so'),
        '-Dlint.native.path=' + str(OUT / 'native/libm3_packed_int_width.so'),
        '-Dm3.collections.rank.pairdistance.native=true',
        '-Dm3.collections.lane.native=true',
    ]
    common = ['-Xmx768m', '-XX:ActiveProcessorCount=2', '-Xcheck:jni', array_native_option,
              '-Dm3.collections.signal.native=true']
    results = {}
    for mode, required in [('java', 'false'), ('native', 'true')]:
        library = OUT / ('native' if mode == 'native' else 'missing')
        vm = common + ['-Djava.library.path=' + str(library),
                       '-Dm3.mask.spans.native.required=' + required,
                       '-Dm3.collections.all.native.required=' + required,
                       '-Dm3.collections.rank.native=' + required]
        if mode == 'native':
            vm += native_properties
        old_reports = MODULE / 'target/surefire-reports'
        if old_reports.exists(): shutil.rmtree(old_reports)
        maven(MODULE / 'pom.xml', '-Dtest=*', '-DargLine=' + ' '.join(vm), '-DfailIfNoTests=true', 'verify')
        results[mode] = reports(mode)
    jar = MODULE / 'target/m3-collections-0.1.0-SNAPSHOT.jar'
    with zipfile.ZipFile(jar) as archive:
        expected_classes = {str(p.relative_to(OUT / 'classes')) for p in (OUT / 'classes').rglob('*.class')}
        actual_classes = {name for name in archive.namelist() if name.endswith('.class')}
        if expected_classes != actual_classes: raise RuntimeError('Packaged class inventory differs from fresh compilation')
        # The POM's current product owners are authoritative; NOTICE is not a source owner.
        packaged_resources = {
            'META-INF/LICENSE': MODULE / 'LICENSE',
            'META-INF/provenance.json': MODULE / 'provenance.json',
            'META-INF/m3-donation.json': MODULE / 'm3-donation.json',
            'META-INF/name-mapping.json': MODULE / 'name-mapping.json',
            'META-INF/M3-DONATION-NOTICE.txt': MODULE / 'M3-DONATION-NOTICE.txt',
            'META-INF/NETBEANS-NOTICE.txt': MODULE / 'NETBEANS-NOTICE.txt',
            'META-INF/m3collections.md': ROOT / 'src/jdk.m3.collections/share/legal/m3collections.md',
            'jdk/m3/collections/M3_SDK_FAMILY_CATALOG.tsv':
                ROOT / 'src/jdk.m3.collections/share/classes/jdk/m3/collections/M3_SDK_FAMILY_CATALOG.tsv',
        }
        packaged_names = archive.namelist()
        for name, source in packaged_resources.items():
            if packaged_names.count(name) != 1 or archive.read(name) != source.read_bytes():
                raise RuntimeError('Packaged module resource missing, duplicate or drifted: ' + name)
        test_resource_root = ROOT / 'test/jdk/jdk/m3/collections/resources'
        test_resources = {path.relative_to(test_resource_root).as_posix()
                          for path in test_resource_root.rglob('*') if path.is_file()}
        leaked_test_resources = sorted(test_resources.intersection(packaged_names))
        if leaked_test_resources:
            raise RuntimeError('Test resources leaked into product jar: ' + repr(leaked_test_resources))
    runtime_modules = subprocess.check_output(
        [JAVA / 'bin/jdeps', '--module-path', array_jar, '--print-module-deps', jar],
        text=True).strip()
    if sorted(runtime_modules.split(',')) != ['java.base', 'jdk.m3.arrays']:
        raise RuntimeError('Unexpected JPMS dependency closure: ' + runtime_modules)
    cp = os.pathsep.join(map(str, (MODULE / 'target/classes',
                                   MODULE / 'target/test-classes', array_jar)))
    java = [JAVA / 'bin/java', *common, '-Dm3.collections.rank.native=true',
            '-Djava.library.path=' + str(OUT / 'native'), '-cp', cp]
    contracts = [('jdk.m3.collections.MaskWindowSparseContract', ['native']),
                 ('jdk.m3.collections.BudgetedMaskWindowContract', ['--native']),
                 ('jdk.m3.collections.IndexMaskWindowSelfTest', ['--native']),
                 ('jdk.m3.collections.IndexMaskWindowRecoverySelfTest', ['--native']),
                 ('jdk.m3.collections.IndexMaskCompactorSelfTest', ['--native']),
                 ('jdk.m3.collections.IndexPathMasksSelfTest', ['--native']),
                 ('jdk.m3.collections.LongIndexPathMasksJNISelfTest', ['--native']),
                 ('jdk.m3.collections.LongAddressBoundedSelfTest', []),
                 ('jdk.m3.collections.IndexPathMaskWindowSelfTest', ['--native']),
                 ('jdk.m3.collections.IndexPathRankNativeSelfTest', []),
                 ('jdk.m3.collections.NativeIndexRangeSelfTest', ['range', 'sharing']),
                 ('jdk.m3.collections.ObjectStateSlots28SelfTest', ['--native']),
                 ('jdk.m3.collections.IndexedTableSelfTest', ['--native']),
                 ('jdk.m3.collections.BagRankIndexSelfTest', ['--native']),
                 ('jdk.m3.collections.SdkRecoverySelfTest', ['--native']),
                 ('jdk.m3.collections.SignalOrderContract', ['native']),
                 ('jdk.m3.collections.SignalDimensionsNativeContract', []),
                 ('jdk.m3.collections.ResidencyStateMaskProof', ['native']),
                 ('jdk.m3.collections.BitLaneJumpProof', [str(OUT / 'native/libm3_bitlane_jump_test.so')]),
                 ('jdk.internal.m3.collections.DisjointIndexSelfTest', ['--native'])]
    for name, args in contracts: run(java + [name, *args])
    # Exercise collection-kernel fallback while retaining required arrays allocation JNI.
    run([JAVA / 'bin/java', *common, '-Djava.library.path=' + str(OUT / 'missing'), '-cp', cp,
         'jdk.internal.m3.collections.DisjointIndexSelfTest'])
    run([JAVA / 'bin/java', *common, '-Djava.library.path=' + str(OUT / 'missing'), '-cp', cp,
         'jdk.m3.collections.BudgetedMaskWindowContract', '--missing'])
    run([JAVA / 'bin/java', *common, '-Djava.library.path=' + str(OUT / 'missing'), '-cp', cp,
         'jdk.m3.collections.LongIndexPathMasksJNISelfTest'])
    native_fixtures = [ROOT / 'test/jdk/jdk/m3/collections/native' / name for name in
                       ('m3_mask_window_spans.c', 'm3_mask_window_regions.c', 'm3_bitlane_jump_jni.c', 'm3_gather_spans.c')]
    proof = {'status': 'PASS_OWNING_COLLECTION_JAVA_AND_JNI', 'head_sha': head,
             'scope': 'independent m3/collections; no JDK phase or challenge-corpus promotion',
             'owning_test_selection': '-Dtest=* overrides default includes/excludes',
             'native_host_properties': 'existing native bindings plus explicit MatBitMatrix provider and required gate, native mode only',
             'worktree_dirty': bool(subprocess.check_output(['git', 'status', '--porcelain', '--untracked-files=no'], cwd=ROOT)),
             'jar_sha256': sha(jar), 'jar_classes': len(expected_classes), 'runtime_modules': runtime_modules,
             'java_sources': len(sources), 'test_sources': len(tests), 'junit': results,
             'source_dependencies': dependency_proof, 'native_libraries': sorted(LIBRARIES),
             'explicit_native_contracts': [name.rsplit('.', 1)[-1] for name, _ in contracts],
             'explicit_native_contract_binaries': [name for name, _ in contracts],
             'native': 'generated headers; C17 strict; fatal UBSan; actual -Xcheck:jni',
             'source_sha256': {str(p.relative_to(ROOT)): sha(p) for p in sources + tests + sorted(admitted) + native_fixtures
                 + sorted(native.rglob('*.h')) + sorted((ROOT / 'm3/native/include').rglob('*.h'))
                 + [MODULE / 'pom.xml', ROOT / 'm3/arrays/pom.xml', ROOT / 'make/m3/collections/CMakeLists.txt']}}
    (OUT / 'PROOF.json').write_text(json.dumps(proof, indent=2) + '\n')
    print(json.dumps({k: proof[k] for k in ('status', 'head_sha', 'junit', 'native_libraries')}, indent=2))

if __name__ == '__main__': main()
