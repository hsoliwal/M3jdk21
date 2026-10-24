#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Build current array/collection owners and original Java/JNI contracts.

Invoke only under the existing hosted 8GiB-after-output/512MiB supervisor.
This verifier supplies no independent storage or process-supervision framework.
"""
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
ARRAY_FIXTURES = MODULE / 'qualification/slot-entry-cache-v1/array-fixtures'
ARRAY_FIXTURE_SHA256 = {
    'M3BackingArrayContractV2.java': '5d648e9536a53056fc7a40b4646477196acbfb2c3afbc0ea506d980a7924e5e8',
    'M3ConfiguredArrayStartupContract.java': '3a969a23e1e86f02412761f089dca33ebfc9d89c1d974ac335669bcb9f0a3dd4',
    'fault_backing_arrays.c': '49e473a33dd1f78ea3771d3a3b2f116734f9b9aaec4d65c5d331e0213917478c',
    'legacy_m3_arrays_jni.c': 'd5dd8f71a86e5f527941886a5358eeec5596a0f6a5735c82db8c10b72a129d1a',
    'backing_array_mock.c': '0498ad5cb126a3824aadac73ebaef74ad179be047e3711ce7bb24b3c803203e8',
}
ARRAY_PROVIDER_SOURCE_SHA256 = {
    'src/main/native/m3_arrays_jni.c': 'da4ba8620a190e07f08446d4a1cc6081d860dcd6d98f94ab2633b87643ddcfb7',
    'src/main/native/m3_arrays.c': '0c7be7f8176ca235c40d88aad2cefab15e7ce7363d7fc648bafba4fdb535695d',
    'src/main/native/include/m3_arrays.h': 'f2eec872591c7af40c13af75012fb20efb8376cdb29209c9e8151fd9775d758d',
}
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

# Exact admitted SlotMap candidate and reconciled original suites; no generated runner.
REQUIRED_SOURCE_SHA256 = {
    "m3/collections/src/main/java/com/m3/collections/M3SlotMap.java": "aa7b22477ab1e23c6a93a81c5705f2109a38c735c806203cb59db5e19845687e",
    "m3/collections/src/main/java/com/m3/collections/M3LFUCache.java": "8e404bcd1e6a0efc94e8f1828feaf4d2d3da1375f07b8a64d238390e09d36f27",
    "m3/arrays/src/main/java/com/m3/arrays/M3ArrayNative.java": "bfaef4b69399dbd12647166bed0485d7a5bf9bdc448a6f195ee12fae4cd4b140",
    "m3/collections/src/test/java/com/m3/collections/M3EntryFacadeCacheTest.java": "60880770855fe12197620ca11666eea19ecfe90d04258840ebfed350f3fff1a4",
    "m3/collections/src/test/java/com/m3/collections/M3NoRetainedStructuralEntryTest.java": "dbf960b0a249669adfa193904e29a61b29b0f435b08fbd395ee4edef7decb6a4",
    "m3/collections/src/test/java/com/m3/collections/M3LazyTreeMapSelfTest.java": "e3a123c3a35246bbba505e108cd4b1e3935dd98dedec11b8273bf7c8a27e9d6b",
}
REQUIRED_JUPITER = {
    'M3EntryFacadeCacheTest': (
        'lfuPrefersHotEntriesAndBreaksTiesByRecency',
        'identityKeyingAllowsNullAndEqualButDistinctKeys',
        'strongMapEntryFacadesReuseBoundedLiveCallerProjections',
        'treeMapReusesLiveFacadesAndKeepsDetachedImmutableSnapshots',
        'weakKeyMapHasFreshProjectionsWithoutStrongOwnerRetention',
        'compatibilityHookKeepsCallbackDemandAndExceptionIdentity',
        'falseAndThrowingHooksLeaveColdAndWarmLfuUntouched',
        'backingHashMapRevisionInvalidatesBeforeReuseButValueWritesDoNot',
        'entryProjectionDemandAvoidsSlotLookupAndValueRead',
        'boundedEvictionDoesNotRetargetEscapedProjectionOrItsOwner',
        'graphGuardRejectsAliasesOutsideTheExactApiCacheEdge',
    ),
    'M3NoRetainedStructuralEntryTest': (
        'storageRootsAndNestedOwnersHaveNoRetainedEntryNodeBucketOrCellFields',
        'onlySlotMapDefinesTheExplicitMapEntryCompatibilityProjection',
        'returnedMapEntriesReuseOnlyThroughBoundedApiCaches',
    ),
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
    user_home = os.environ.get('MAVEN_USER_HOME')
    if user_home:
        settings = Path(user_home).resolve(strict=True)
        proof_root = Path(os.environ['M3_PROOF_ROOT']).resolve(strict=True)
        if proof_root not in settings.parents:
            raise RuntimeError('Maven settings must remain under the owned proof root')
        for name in ('settings.xml', 'global-settings.xml'):
            if (settings / name).is_symlink() or not (settings / name).is_file():
                raise RuntimeError('Missing owned Maven settings: ' + name)
        command += ['-s', str(settings / 'settings.xml'), '-gs', str(settings / 'global-settings.xml')]
    run(command + ['-f', pom, *args])

def vm_flags(heap):
    # Reuse the established owned JVM routing shape; the caller owns storage/process supervision.
    return ['-Xmx' + heap, '-XX:MaxMetaspaceSize=384m', '-XX:ReservedCodeCacheSize=64m',
            '-XX:CompressedClassSpaceSize=64m', '-XX:ActiveProcessorCount=2', '-XX:-UsePerfData',
            '-Djava.io.tmpdir=' + os.environ['TMPDIR'], '-Duser.home=' + os.environ['HOME'],
            '-XX:ErrorFile=' + str(OUT / 'hs_err_%p.log'),
            '-XX:ReplayDataFile=' + str(OUT / 'replay_%p.log')]

def array_contract_commands(array_classes, array_native, cc):
    # Compose the existing fixture mains and C sources; never supply or replace an allocator.
    probe = OUT / 'array-probe-classes'
    native = OUT / 'native'
    binaries = {mode: native / ('libm3_arrays_' + mode + '.so')
                for mode in ('legacy', 'silent', 'pending')}
    mock = native / 'array-backing-mock'
    commands = [('compile-array-probes',
                 [JAVA / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror',
                  '--module-path', array_classes, '--add-modules', 'com.m3.arrays',
                  '-classpath', OUT / 'missing', '-sourcepath', OUT / 'missing',
                  '-implicit:none', '-d', probe,
                  ARRAY_FIXTURES / 'M3BackingArrayContractV2.java',
                  ARRAY_FIXTURES / 'M3ConfiguredArrayStartupContract.java'], None)]
    include = '-I' + str(ARRAYS / 'src/main/native/include')
    core = ARRAYS / 'src/main/native/m3_arrays.c'
    commands.append(('compile-array-legacy',
                     cc + ['-shared', '-fPIC', include,
                           ARRAY_FIXTURES / 'legacy_m3_arrays_jni.c', core,
                           '-o', binaries['legacy']], None))
    for mode, pending in (('silent', '0'), ('pending', '1')):
        commands.append(('compile-array-' + mode,
                         cc + ['-shared', '-fPIC', '-DM3_PENDING=' + pending,
                               ARRAY_FIXTURES / 'fault_backing_arrays.c',
                               '-o', binaries[mode]], None))
    commands.append(('compile-current-array-mock',
                     cc + [include, ARRAY_FIXTURES / 'backing_array_mock.c',
                           ARRAYS / 'src/main/native/m3_arrays_jni.c', core,
                           '-o', mock], None))
    commands.append(('current-array-mock', [mock], 'M3_BACKING_ARRAY_MOCK_PASS cases=58 checks='))
    base = [JAVA / 'bin/java', *vm_flags('256m'), '-ea', '-Xcheck:jni',
            '--module-path', array_classes, '--add-modules', 'com.m3.arrays',
            '-Dm3.arrays.native.path=']
    for mode in ('native', 'unloaded', 'legacy', 'silent', 'pending'):
        library = array_native if mode in ('native', 'unloaded') else binaries[mode]
        opens = ['--add-opens', 'com.m3.arrays/com.m3.arrays=ALL-UNNAMED'] if mode == 'native' else []
        commands.append(('array-backing-' + mode,
                         base + opens + ['-cp', probe, 'com.synexia.proof.M3BackingArrayContractV2',
                                         mode, array_classes, library],
                         'M3_BACKING_ARRAY_CONTRACT_V2_PASS mode=' + mode + ' checks='))
    for mode in ('first-objects', 'first-bytes', 'first-ints', 'first-longs'):
        commands.append(('array-configured-' + mode,
                         base + ['-Djava.security.manager=allow', '-cp', probe,
                                 'com.synexia.proof.M3ConfiguredArrayStartupContract',
                                 mode, array_classes, array_native, 'com.m3.arrays'],
                         'M3_CONFIGURED_ARRAY_STARTUP_CONTRACT_PASS mode=' + mode + ' checks='))
    return commands, binaries, mock, probe

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
    for name, methods in REQUIRED_JUPITER.items():
        report = destination / ('TEST-com.m3.collections.' + name + '.xml')
        suite = ET.parse(report).getroot()
        actual = [case.get('name') for case in suite.findall('testcase')]
        if sorted(actual) != sorted(methods):
            raise RuntimeError('Original Jupiter methods differ: ' + name)
        if int(suite.get('tests', '0')) != len(methods) or any(
                int(suite.get(key, '0')) for key in ('failures', 'errors', 'skipped')):
            raise RuntimeError('Original Jupiter suite did not fully pass: ' + name)
    return totals

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'PROOF.json').unlink(missing_ok=True)  # Never leave a prior PASS after a failed rerun.
    for relative, expected in REQUIRED_SOURCE_SHA256.items():
        path = ROOT / relative
        if path.is_symlink() or not path.is_file() or sha(path) != expected:
            raise RuntimeError('Required current source drift: ' + relative)
    for owner, pins in ((ARRAY_FIXTURES, ARRAY_FIXTURE_SHA256), (ARRAYS, ARRAY_PROVIDER_SOURCE_SHA256)):
        for relative, expected in pins.items():
            path = owner / relative
            if path.is_symlink() or not path.is_file() or sha(path) != expected:
                raise RuntimeError('Array qualification input drift: ' + str(path))
    if (ARRAYS / 'target').exists():
        raise RuntimeError('Current arrays build requires fresh owned output')
    for name in ('headers', 'classes', 'fixture-classes', 'array-probe-classes', 'native', 'missing'):
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
    array_sources = sorted((ARRAYS / 'src/main/java').rglob('*.java'))
    array_tests = sorted((ARRAYS / 'src/test/java').rglob('*.java'))
    array_native_sources = sorted(p for p in (ARRAYS / 'src/main/native').rglob('*') if p.is_file())
    array_native_sources.append(ROOT / 'm3/native/include/m3_jni_core.h')
    array_inputs = array_sources + array_tests + array_native_sources + [ARRAYS / 'pom.xml']
    array_input_sha256 = {str(p.relative_to(ROOT)): sha(p) for p in array_inputs}
    # Reuse the existing source-owned CMake/CTest/Maven profile, then install its current artifact.
    maven(ARRAYS / 'pom.xml', '-Pnative-linux',
          '-DargLine=' + ' '.join(vm_flags('768m') + ['-Xcheck:jni']), 'install')
    array_classes = ARRAYS / 'target/classes'
    array_jar = ARRAYS / 'target/m3-arrays-0.1.0-SNAPSHOT.jar'
    array_native = (ARRAYS / 'target/native/libm3_arrays.so').resolve(strict=True)
    array_bridge = array_classes / 'com/m3/arrays/M3ArrayNative.class'
    if not array_native.is_file() or not array_jar.is_file() or not array_bridge.is_file():
        raise RuntimeError('Current arrays provider was not built')
    with zipfile.ZipFile(array_jar) as archive:
        if archive.read('com/m3/arrays/M3ArrayNative.class') != array_bridge.read_bytes():
            raise RuntimeError('Current arrays JAR/bridge classes differ')
    array_report = ARRAYS / 'target/surefire-reports/TEST-com.m3.arrays.M3ArrayNativeTest.xml'
    array_suite = ET.parse(array_report).getroot()
    array_totals = {key: int(array_suite.get(key, '0'))
                    for key in ('tests', 'failures', 'errors', 'skipped')}
    if array_totals != {'tests': 5, 'failures': 0, 'errors': 0, 'skipped': 0}:
        raise RuntimeError('Current array JNI Jupiter suite did not fully pass: ' + str(array_totals))
    sources = sorted((MODULE / 'src/main/java').rglob('*.java'))
    tests = sorted((MODULE / 'src/test/java').rglob('*.java'))
    (OUT / 'main.args').write_text('\n'.join(map(str, sources)) + '\n')
    run([JAVA / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror', '-Xpkginfo:always',
         '--module-path', array_classes,
         '-h', OUT / 'headers', '-d', OUT / 'classes', '@' + str(OUT / 'main.args')])
    native = MODULE / 'src/main/native/collections'
    admitted = {native / name for rows in LIBRARIES.values() for name, _ in rows}
    if admitted != set(native.glob('*.c')):
        raise RuntimeError('Native source inventory changed; review library mapping before qualification')
    cc = [os.environ.get('CC', 'cc'), '-std=c17', '-Wall', '-Wextra', '-Werror',
          '-fsanitize=undefined', '-fno-sanitize-recover=undefined', '-O2',
          '-I' + str(JAVA / 'include'), '-I' + str(JAVA / 'include/linux'),
          '-I' + str(OUT / 'headers')]
    array_commands, array_fault_binaries, array_mock, array_probe = array_contract_commands(
        array_classes, array_native, cc)
    array_execution = []
    for phase, command, marker in array_commands:
        run(command)
        array_execution.append({'phase': phase, 'command': list(map(str, command)),
                                'expected_marker': marker, 'process_exit': 0})
    probe_classes = sorted(array_probe.rglob('*.class'))
    expected_probe_classes = {
        'com/synexia/proof/M3BackingArrayContractV2.class',
        'com/synexia/proof/M3ConfiguredArrayStartupContract.class',
        'com/synexia/proof/M3ConfiguredArrayStartupContract$Action.class',
        'com/synexia/proof/M3ConfiguredArrayStartupContract$AccessGate.class'}
    if {p.relative_to(array_probe).as_posix() for p in probe_classes} != expected_probe_classes:
        raise RuntimeError('Array fixture class inventory differs from the existing sources')
    if any(int.from_bytes(p.read_bytes()[6:8], 'big') != 65 for p in probe_classes):
        raise RuntimeError('Array fixture classes must use Java 21 major 65')
    array_fixture_binaries = [*array_fault_binaries.values(), array_mock, *probe_classes]
    array_fixture_binary_sha256 = {str(p.relative_to(ROOT)): sha(p) for p in array_fixture_binaries}
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
         '-cp', str(OUT / 'classes') + os.pathsep + str(array_classes),
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
    common = [*vm_flags('768m'), '-Xcheck:jni',
              '-Dm3.arrays.native.path=' + str(array_native),
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
        [JAVA / 'bin/jdeps', '--module-path', array_jar, '--print-module-deps', jar], text=True).strip()
    if sorted(runtime_modules.split(',')) != ['com.m3.arrays', 'java.base']:
        raise RuntimeError('Unexpected runtime dependencies: ' + runtime_modules)
    cp = os.pathsep.join(map(str, (MODULE / 'target/classes', MODULE / 'target/test-classes', array_classes)))
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
    # Keep the original full main methods; the tree main also invokes the value-state main.
    lazy_mains = ('M3LazyTreeMapSelfTest', 'M3LazyValueStateSelfTest')
    for name in lazy_mains: run(java + ['-ea', 'com.m3.collections.' + name])
    run([JAVA / 'bin/java', *common, '-Djava.library.path=' + str(OUT / 'missing'), '-cp', cp,
         'com.m3.collections.M3BudgetedMaskWindowContract', '--missing'])
    run([JAVA / 'bin/java', *common, '-Djava.library.path=' + str(OUT / 'missing'), '-cp', cp,
         'com.m3.collections.M3LongIndexPathMasksJNISelfTest'])
    native_fixtures = [MODULE / 'src/test/native' / name for name in
                       ('m3_mask_window_spans.c', 'm3_mask_window_regions.c', 'm3_bitlane_jump_jni.c', 'm3_gather_spans.c')]
    if array_input_sha256 != {str(p.relative_to(ROOT)): sha(p) for p in array_inputs}:
        raise RuntimeError('Current arrays source changed during qualification')
    for relative, expected in REQUIRED_SOURCE_SHA256.items():
        if sha(ROOT / relative) != expected:
            raise RuntimeError('Required current source changed during qualification: ' + relative)
    for name, expected in ARRAY_FIXTURE_SHA256.items():
        if sha(ARRAY_FIXTURES / name) != expected:
            raise RuntimeError('Array fixture source changed during qualification: ' + name)
    proof = {'status': 'PASS_OWNING_COLLECTION_JAVA_AND_JNI', 'head_sha': head,
             'scope': 'independent m3/collections; no JDK phase or challenge-corpus promotion',
             'worktree_dirty': bool(subprocess.check_output(['git', 'status', '--porcelain', '--untracked-files=no'], cwd=ROOT)),
             'jar_sha256': sha(jar), 'jar_classes': len(expected_classes), 'runtime_modules': runtime_modules,
             'java_sources': len(sources), 'test_sources': len(tests), 'junit': results,
             'required_current_sources': REQUIRED_SOURCE_SHA256,
             'array_contracts': {'fixture_source_sha256': ARRAY_FIXTURE_SHA256,
                                 'fixture_binary_sha256': array_fixture_binary_sha256,
                                 'provider_classes': str(array_classes), 'provider_module': 'com.m3.arrays',
                                 'current_c_sha256': sha(ARRAYS / 'src/main/native/m3_arrays_jni.c'),
                                 'commands': array_execution,
                                 'required_backing_modes': ['native', 'unloaded', 'legacy', 'silent', 'pending'],
                                 'configured_startup_covered': ['first-objects', 'first-bytes', 'first-ints', 'first-longs'],
                                 'configured_startup_remaining': 'Other existing modes, path variants, crossloader and distribution remain pending',
                                 'production_c_mock_expected_cases': 58,
                                 'stdout_marker_and_JNI_diagnostic_gate': 'Required in the inherited bounded supervisor log'},
             'required_original_jupiter_methods': REQUIRED_JUPITER, 'original_full_lazy_mains': lazy_mains,
             'array_provider': {'module': 'com.m3.arrays', 'native_profile': 'native-linux',
                                'native_jupiter': array_totals, 'native_jupiter_report_sha256': sha(array_report),
                                'jar_sha256': sha(array_jar), 'bridge_class_sha256': sha(array_bridge),
                                'native_library_sha256': sha(array_native),
                                'native_library': str(array_native.relative_to(ROOT)),
                                'source_sha256': array_input_sha256},
             'source_dependencies': dependency_proof, 'native_libraries': sorted(LIBRARIES),
             'explicit_native_contracts': [name for name, _ in contracts],
             'native': 'generated headers; C17 strict; fatal UBSan; actual -Xcheck:jni',
             'source_sha256': {str(p.relative_to(ROOT)): sha(p) for p in sources + tests + sorted(admitted) + native_fixtures}}
    (OUT / 'PROOF.json').write_text(json.dumps(proof, indent=2) + '\n')
    print(json.dumps({k: proof[k] for k in ('status', 'head_sha', 'junit', 'native_libraries')}, indent=2))

if __name__ == '__main__': main()
