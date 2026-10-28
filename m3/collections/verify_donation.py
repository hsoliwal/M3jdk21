#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: qualify concurrent EnumSet in a new exact inventory; preserve all acceptance guards and historical receipts.
# Modified 2026 by Hitesh Soliwal and contributors: restore exact collection receiving provenance and verification while retaining current owners and String phase authority.
"""Verify the installed M3 collection donation with target-native Java/JNI checks.

This host verifies product source. The canonical transformation and SDK replay remain
in Synexia. The earlier verify.py remains the frozen primitive-lane installer.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile
from m3_object_state_offset_receipt import include_successor as include_object_state_offset_successor

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def unique_json_object(pairs):
    """Reject duplicate metadata keys before historical fields can shadow live pins."""
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate donation metadata key: ' + key)
        result[key] = value
    return result


def read_donation_packet(path=HERE / 'synexia-donation.json'):
    """Read all nested objects without silently accepting duplicate JSON keys."""
    return json.loads(path.read_text(), object_pairs_hook=unique_json_object)



# The next exact successor applies the validated stored-representative atom
# without rewriting the six original filtered/sorted receiver source receipts.
_REPRESENTATIVE_RECEIPT = HERE / 'recipe/stored-representative-20261017/manifest.tsv'
_REPRESENTATIVE_SHA256 = 'a62827ffafc18bd6d710d7898741d2114a0068b8ac9ee63aac02e55038e1f293'
_REPRESENTATIVE_PATHS = (
    'm3/collections/src/main/java/com/m3/collections/M3FilteredViews.java',
    'm3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java',
    'm3/collections/src/test/java/com/m3/collections/M3StoredRepresentativeContract.java',
    'm3/collections/src/test/java/com/m3/collections/M3StoredRepresentativeTest.java',
)


def include_stored_representative_successor(previous_rows,
        receipt=_REPRESENTATIVE_RECEIPT):
    """Source-sealed additive correction for exactly two owners and two tests.

    Verify the full previous receipt first, then only replace those two
    exact old digests and append the two new Java21 test owners. No historical
    manifest rewriting, unknown source admission or permissive wildcards.
    """
    if not receipt.exists() and not receipt.is_symlink():
        return list(previous_rows)
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != _REPRESENTATIVE_SHA256:
        raise SystemExit('Stored-representative successor missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != len(_REPRESENTATIVE_PATHS):
        raise SystemExit('Stored-representative successor must declare exactly four paths')
    current = {path: expected for path, expected in previous_rows}
    if len(current) != len(previous_rows):
        raise SystemExit('Duplicate prior stored-representative source path')
    extras = []
    for cells, expected_path in zip(entries, _REPRESENTATIVE_PATHS):
        if len(cells) != 4:
            raise SystemExit('Malformed stored-representative successor')
        path, before, after, template = cells
        if (path != expected_path or len(after) != 64
                or any(c not in '0123456789abcdef' for c in after)
                or template != path.rsplit('/', 1)[1] + '.after'):
            raise SystemExit('Unapproved stored-representative source: ' + path)
        if before == 'ABSENT':
            if path in current:
                raise SystemExit('Occupied new stored-representative source: ' + path)
            extras.append((path, after))
        else:
            if current.get(path) != before:
                raise SystemExit('Stored-representative source preimage drift: ' + path)
            current[path] = after
    return [(path, current[path]) for path, _ in previous_rows] + extras


# The historical installed-source receipt remains immutable. This optional
# additive overlay is authored by the Synexia OpenRewrite collection receiver.
_FILTERED_VIEW_RECEIPT = HERE / 'recipe/filtered-sorted-views-20261017/manifest.tsv'
_FILTERED_VIEW_SHA256 = '828e24b44e4d0b98c6819bb9dd983d71084ae0661e1d81fe90bd0dd286c50ede'
_FILTERED_VIEW_PATHS = (
    'm3/collections/src/main/java/com/m3/collections/M3FilteredViews.java',
    'm3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java',
    'm3/collections/src/test/java/com/m3/collections/M3FilteredViewsContract.java',
    'm3/collections/src/test/java/com/m3/collections/M3FilteredViewsTest.java',
    'm3/collections/src/test/java/com/m3/collections/M3MutableFilteredViewsContract.java',
    'm3/collections/src/test/java/com/m3/collections/M3MutableFilteredViewsTest.java',
)


def include_filtered_view_successor(historical_rows, receipt=_FILTERED_VIEW_RECEIPT):
    """Admit only the exact six source-sealed, additive receiver files.

    Missing overlay preserves historical behavior; unlisted Java files still fail
    below. An invalid/aliased overlay is never treated as silently absent.
    """
    if not receipt.exists() and not receipt.is_symlink():
        return list(historical_rows)
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != _FILTERED_VIEW_SHA256:
        raise SystemExit('Filtered-view successor manifest missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != len(_FILTERED_VIEW_PATHS):
        raise SystemExit('Filtered-view successor must declare exactly six paths')
    extra = []
    for entry, expected_path in zip(entries, _FILTERED_VIEW_PATHS):
        if len(entry) != 4:
            raise SystemExit('Malformed filtered-view successor row')
        path, before, after, template = entry
        if (path != expected_path or before != 'ABSENT'
                or len(after) != 64 or any(c not in '0123456789abcdef' for c in after)
                or template != path.rsplit('/', 1)[1] + '.after'):
            raise SystemExit('Unapproved filtered-view receiver source: ' + path)
        extra.append((path, after))
    return [*historical_rows, *extra]


# Additive M3IndexPathMasks receiving overlay. The pre-existing source manifest
# and filtered/sorted receiver seal are immutable; this successor is exact and opt-in.
_PATH_MASK_RECEIPT = HERE / 'recipe/index-path-masks-20261017/manifest.tsv'
_PATH_MASK_SHA256 = '1cba7c9ed681fbe65653b75949558640ea5aa31a1e3c8f5d83d27f9197518020'
_PATH_MASK_PATHS = (
    'm3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java',
    'm3/collections/src/main/native/collections/m3_index_path_masks.c',
    'm3/collections/src/test/java/com/m3/collections/M3IndexPathMasksSelfTest.java',
    'm3/collections/src/test/java/com/m3/collections/M3IndexPathMasksTest.java',
)


def include_path_mask_successor(historical_rows, receipt=_PATH_MASK_RECEIPT):
    """Admit only the four SHA-256 sealed Java/JNI path-mask receiver sources."""
    if not receipt.exists() and not receipt.is_symlink():
        return list(historical_rows)
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != _PATH_MASK_SHA256:
        raise SystemExit('Index-path-mask successor manifest missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != len(_PATH_MASK_PATHS):
        raise SystemExit('Index-path-mask successor must declare exactly four paths')
    extra = []
    for entry, expected_path in zip(entries, _PATH_MASK_PATHS):
        if len(entry) != 4:
            raise SystemExit('Malformed index-path-mask successor row')
        path, before, after, template = entry
        if (path != expected_path or before != 'ABSENT'
                or len(after) != 64 or any(c not in '0123456789abcdef' for c in after)
                or template != path.rsplit('/', 1)[1] + '.after'):
            raise SystemExit('Unapproved index-path-mask receiver source: ' + path)
        extra.append((path, after))
    return [*historical_rows, *extra]

# Exact successor of the four unchanged historical receiving receipts.
_TOOLS4J_RECEIPT = HERE / 'qualification/tools4j-unix-source-successor-20261019.tsv'
_TOOLS4J_RECEIPT_SHA256 = 'a802b36b031d2280c72e148baf93e0977a422ef1f07525f349526a2e2b31e46b'
_TOOLS4J_PREDECESSOR_SHA256 = '104bd2f6582f4ce496ff13524a7a8f33e7f4c10fb18981ae9e3229c6da5ba7b9'


def include_tools4j_refinement_successor(previous_rows, receipt=_TOOLS4J_RECEIPT):
    """Reconcile only the sealed current inventory and qualified source refinements.

    Each row keeps the inherited manifest hash, exact pinned-master preimage,
    and final received hash distinct. Historical receipts remain unchanged.
    """
    prior = list(previous_rows)
    if (len(prior) != 113 or any(len(row) != 2 for row in prior)
            or len({row[0] for row in prior}) != len(prior)):
        raise SystemExit('Tools4j predecessor rows missing, duplicate or malformed')
    predecessor = ''.join(path + '\t' + expected + '\n' for path, expected in prior)
    if hashlib.sha256(predecessor.encode('utf-8')).hexdigest() != _TOOLS4J_PREDECESSOR_SHA256:
        raise SystemExit('Tools4j predecessor receipt chain drift')
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != _TOOLS4J_RECEIPT_SHA256:
        raise SystemExit('Tools4j successor receipt missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != 418:
        raise SystemExit('Tools4j successor must declare exactly418 source transitions')
    current = dict(prior)
    extra = []
    seen = set()
    for cells in entries:
        if len(cells) != 4:
            raise SystemExit('Tools4j malformed source transition')
        path, inherited, master, after = cells
        relative = Path(path)
        java_source = path.endswith('.java') and path.startswith((
            'm3/collections/src/main/java/', 'm3/collections/src/test/java/'))
        native_source = path == 'm3/collections/src/main/native/collections/m3_long_address_kernel.c'
        if (relative.is_absolute() or '..' in relative.parts or '\\' in path
                or not (java_source or native_source) or path in seen):
            raise SystemExit('Tools4j duplicate or out-of-scope source: ' + path)
        seen.add(path)
        hashes = (master, after) if inherited == 'ABSENT' else (inherited, master, after)
        if any(len(value) != 64 or any(c not in '0123456789abcdef' for c in value) for value in hashes):
            raise SystemExit('Tools4j invalid source hash: ' + path)
        if inherited == after:
            raise SystemExit('Tools4j unchanged source is not a transition: ' + path)
        if inherited == 'ABSENT':
            if path in current:
                raise SystemExit('Tools4j occupied source admission: ' + path)
            extra.append((path, after))
        else:
            if current.get(path) != inherited:
                raise SystemExit('Tools4j inherited source preimage drift: ' + path)
            current[path] = after
    result = [(path, current[path]) for path, _ in prior] + extra
    source_java = {path for path, _ in result if path.endswith('.java') and '/src/' in path}
    if len(result) != 510 or len(source_java) != 503:
        raise SystemExit('Tools4j exact current source membership drift')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cost-probe', action='store_true', help='record paired diagnostic allocation/time samples')
    args = parser.parse_args()
    java_home = Path(os.environ['JAVA_HOME']).resolve()
    junit = Path(os.environ['JUNIT_CONSOLE_JAR']).resolve()
    if not junit.is_file():
        raise SystemExit('JUNIT_CONSOLE_JAR must name a real JUnit platform console standalone JAR')
    if digest(junit) != '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a':
        raise SystemExit('Expected exact JUnit console 1.12.2 artifact')
    if not sys.platform.startswith('linux'):
        raise SystemExit('This JNI acceptance host requires Linux; no partial overall PASS')
    target = HERE / 'target/donation-verification'
    shutil.rmtree(target, ignore_errors=True)
    evidence = target / 'evidence'
    evidence.mkdir(parents=True)

    def run(name, command):
        command = [str(x) for x in command]
        result = subprocess.run(command, cwd=REPO, capture_output=True, text=True)
        output = result.stdout + result.stderr
        (evidence / (name + '.log')).write_text(
            '$ ' + repr(command) + '\n' + output + '\nEXIT=' + str(result.returncode) + '\n')
        if result.returncode:
            print(output[-16000:])
            raise SystemExit(result.returncode)
        print(name + ': PASS', flush=True)
        return output

    version = run('01-java-version', [java_home / 'bin/java', '-version'])
    if 'version "21.' not in version and 'version "21"' not in version:
        raise SystemExit('The qualified baseline requires Java 21')
    packet = read_donation_packet()
    manifest_name = packet.get('installed_source_manifest', 'qualification/installed-source-enum-set-20261008.tsv')
    manifest_relative = Path(manifest_name)
    if manifest_relative.is_absolute() or '..' in manifest_relative.parts:
        raise SystemExit('Invalid installed source manifest path')
    manifest = HERE / manifest_relative
    if digest(manifest) != packet['installed_source_manifest_sha256']:
        raise SystemExit('Installed source manifest drift: actual=' + digest(manifest) + ' expected=' + packet['installed_source_manifest_sha256'] + '; reconcile with canonical recipe before verifying')
    rows = [line.split('\t') for line in manifest.read_text().splitlines()
            if line and not line.startswith('#')]
    rows = include_path_mask_successor(include_stored_representative_successor(include_filtered_view_successor(rows)))
    rows = include_object_state_offset_successor(rows)
    rows = include_tools4j_refinement_successor(rows)
    if len({row[0] for row in rows}) != len(rows):
        raise SystemExit('Duplicate installed source path')
    for path, expected in rows:
        relative = Path(path)
        if relative.is_absolute() or '..' in relative.parts or not path.startswith('m3/collections/'):
            raise SystemExit('Invalid receiver path: ' + path)
        source = REPO / relative
        if (not source.is_file()
                or (path.endswith("/M3ObjectStateSlots28.java")
                    or path.endswith("/M3ObjectStateSlots28SelfTest.java")) and source.is_symlink()
                or digest(source) != expected):
            raise SystemExit('Installed source drift: ' + path)
    declared_java = {path for path, _ in rows if path.endswith('.java') and '/src/' in path}
    actual_java = {str(path.relative_to(REPO)) for path in (HERE / 'src').rglob('*.java')}
    if declared_java != actual_java:
        raise SystemExit('Java source set changed; qualify a successor packet')
    mains = sorted(REPO / path for path in declared_java if '/src/main/' in path)
    tests = sorted(REPO / path for path in declared_java if '/src/test/' in path)
    probes = [HERE / 'verification' / (name + '.java') for name in
              ('SegmentedLaneNativeProbe', 'PreparedLaneAllocationProbe')]
    classes, test_classes = target / 'classes', target / 'test-classes'
    classes.mkdir()
    test_classes.mkdir()
    common = [java_home / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror']
    run('02-module-compile', common + ['-d', classes] + mains)
    run('03-test-compile', common + ['-cp', str(classes) + os.pathsep + str(junit),
                                   '-d', test_classes] + tests + probes)
    runtime_classpath = str(classes) + os.pathsep + str(test_classes)
    reports = target / 'junit-reports'
    run('04-all-contract-tests', [java_home / 'bin/java', '-jar', junit, 'execute',
                                '--disable-banner', '--class-path', runtime_classpath,
                                '--scan-class-path', '--fail-if-no-tests', '--reports-dir', reports])
    totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for report in reports.glob('TEST-*.xml'):
        suite = ET.parse(report).getroot()
        for key in totals:
            totals[key] += int(suite.attrib.get(key, '0'))
    if not totals['tests'] or any(totals[key] for key in ('failures', 'errors', 'skipped')):
        raise SystemExit('JUnit acceptance requires nonzero tests and no failures/errors/skips: ' + repr(totals))
    for name in ('M3LazyTreeMapSelfTest', 'M3LazyValueStateSelfTest'):
        run('04-' + name, [java_home / 'bin/java', '-ea', '-cp', runtime_classpath,
                           'com.m3.collections.' + name])
    resources = target / 'module-resources/META-INF'
    resources.mkdir(parents=True)
    names = ('LICENSE', 'NOTICE', 'provenance.json', 'synexia-donation.json', 'SYNEXIA-DONATION-NOTICE.txt', 'name-mapping.json')
    for name in names:
        shutil.copyfile(HERE / name, resources / name)
    jar = target / 'm3-collections.jar'
    run('05-module-package', [java_home / 'bin/jar', '--create', '--file', jar,
                            '-C', classes, '.', '-C', resources.parent, '.'])
    descriptor = run('06-module-descriptor', [java_home / 'bin/jar', '--describe-module', '--file', jar])
    requirements = [line.strip() for line in descriptor.splitlines() if line.strip().startswith('requires ')]
    if requirements != ['requires java.base mandated']:
        raise SystemExit('Unexpected runtime dependency: ' + repr(requirements))
    with zipfile.ZipFile(jar) as archive:
        for name in names:
            if archive.read('META-INF/' + name) != (HERE / name).read_bytes():
                raise SystemExit('Packaged attribution/provenance mismatch: ' + name)
    native = HERE / 'src/main/native/collections/segmented_bit_lane.c'
    native_args = [os.environ.get('CC', 'gcc'), '-std=c17', '-O2', '-Wall', '-Wextra', '-Werror',
                   '-fsanitize=undefined', '-fno-sanitize-recover=all',
                   '-I' + str(java_home / 'include'), '-I' + str(java_home / 'include/linux')]
    library = target / 'libsegmented_bits.so'
    run('07-native-static', native_args + ['-fsyntax-only', native])
    run('08-native-compile', native_args + ['-shared', '-fPIC', native, '-o', library])
    run('09-checked-jni-parity', [java_home / 'bin/java', '-Xcheck:jni', '-cp', runtime_classpath,
                                 'SegmentedLaneNativeProbe', library])
    run('10-prepared-allocation', [java_home / 'bin/java', '-Xbatch', '-cp', runtime_classpath,
                                  'PreparedLaneAllocationProbe'])
    if args.cost_probe:
        cost = run('11-collection-cost-probe', [java_home / 'bin/java', '-Xbatch', '-cp', runtime_classpath,
                                               'com.m3.collections.M3CollectionCostProbe'])
        (evidence / 'collection-cost.csv').write_text(cost)
    receipt = {'copyright': 'Copyright 2026 Hitesh Soliwal and contributors', 'license': 'Apache-2.0', 'schema': 'M3_COLLECTION_RECEIVING_QUALIFICATION_V1',
               'scope': 'SEPARATELY_BUILT_COLLECTION_MODULE',
               'installed_source_manifest_sha256': digest(manifest),
               'java_sources': len(mains), 'test_sources': len(tests), 'junit': totals,
               'main_self_tests': ['M3LazyTreeMapSelfTest:PASS', 'M3LazyValueStateSelfTest:PASS'],
               'java_compile': '--release 21 -Xlint:all,-module -Werror',
               'runtime_requires': ['java.base'], 'native_standard': 'c17',
               'undefined_behavior_sanitizer': 'PASS', 'checked_jni': 'PASS',
               'prepared_lane_allocation': 'PASS', 'packaged_license_provenance': 'PASS',
               'cost_probe': 'DIAGNOSTIC_RECORDED' if args.cost_probe else 'NOT_RUN',
               'full_reactor': 'NOT_RUN', 'openjdk_jtreg_hotspot': 'NOT_RUN',
               'universal_performance_claim': False, 'phase_advance': False,
               'active_phase': 'STRING', 'target_baseline': 'eda1ae0733952394851c3580f8efb7624563b98c'}
    (evidence / 'qualification.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))


if __name__ == '__main__':
    main()
