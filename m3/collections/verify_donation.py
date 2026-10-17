#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: seal exact-master admission gaps and the five-provider count/flag successor without rewriting historical receipts.
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
from m3_object_state_offset_receipt import approved_successor as read_object_state_offset_successor

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

# The original packet and its four successor receipts remain immutable. This
# separate ledger reconciles only files already present at the exact cloned
# master. It records an existing admission gap, not a change made by adapters.
_MASTER_SOURCE_COMMIT = 'f3c615693c41bac1bf6ff49ba53e83f85425bca0'
_MASTER_SOURCE_RECEIPT = HERE / 'recipe/five-provider-array-interop-20261017/master-source-manifest.tsv'
_MASTER_SOURCE_RECEIPT_SHA256 = 'd5ae548c58ae31ea92898e90961a94743439956279bbd5dfb3293c7af052d163'
_MASTER_SOURCE_PATHS_SHA256 = '5be1200c09ecfc74f779f44f7d0a920c713fcf808fd7f8e8b98c9a7a38e385b0'
_HISTORICAL_SOURCE_CENSUS_SHA256 = '59aede4f036038b8ac6dbadadf6b668cc769601284519b8ce0218289d69abdd5'
_MASTER_SOURCE_CENSUS_SHA256 = 'cf32fd1f8363b5cb8c25b712667dec4f541c14a2e9717ceb0f39ab607a5edb6a'
_OBJECT_STATE_OFFSET_HELPER_SHA256 = '5c238acdff67e47e0284eabfc1ea7a585c8f3e44d9d78e98f272df9935d84fd8'
_OBJECT_STATE_OFFSET_RECEIPT_SHA256 = 'f371b00237b1219cad8d131703929c32a06a56df567943411c40549e2351d4d5'

# This task changes exactly six core source paths: a new count/flag owner and
# its tests, two empty explicit constructors, and two corrected test oracles.
_FIVE_PROVIDER_SOURCE_RECEIPT = HERE / 'recipe/five-provider-array-interop-20261017/source-manifest.tsv'
_FIVE_PROVIDER_SOURCE_RECEIPT_SHA256 = '0cc34cf912f12928fa0b9a7b5aafdb48981140ea2fd99c70360caa5bb367a441'
_FIVE_PROVIDER_SOURCE_PATHS_SHA256 = 'cc6617301df5aee5af4b4046810ac5cd9d5201386eaafff559608588d982915b'
_FIVE_PROVIDER_SOURCE_CENSUS_SHA256 = '373cae4d211269549ea0092a74e6fb5359c28ad255d45dd8c90120863b6f1d6e'


def _is_hex(value, length):
    return (isinstance(value, str) and len(value) == length
            and all(character in '0123456789abcdef' for character in value))


def _receiver_relative_path(value):
    if not isinstance(value, str):
        raise SystemExit('Receiver path must be text')
    relative = Path(value)
    if (relative.is_absolute() or str(relative) != value or '..' in relative.parts
            or '\\' in value or '\0' in value or not value.startswith('m3/collections/')):
        raise SystemExit('Invalid receiver path: ' + value)
    return relative


def _require_unaliased_file(path, label):
    path = Path(path).absolute()
    if '..' in path.parts or any(part.is_symlink() for part in (path, *path.parents)):
        raise SystemExit(label + ' is aliased: ' + str(path))
    if not path.is_file():
        raise SystemExit(label + ' is missing or not a regular file: ' + str(path))
    return path


def _checked_source_rows(rows, label):
    current = {}
    for cells in rows:
        if not isinstance(cells, (list, tuple)) or len(cells) != 2:
            raise SystemExit('Malformed ' + label + ' source row')
        path, expected = cells
        _receiver_relative_path(path)
        if path in current:
            raise SystemExit('Duplicate ' + label + ' source path: ' + path)
        if not _is_hex(expected, 64):
            raise SystemExit('Invalid ' + label + ' source digest: ' + path)
        current[path] = expected
    return current


def _source_census_digest(rows):
    return hashlib.sha256(''.join(path + '\t' + expected + '\n'
        for path, expected in sorted(rows)).encode('utf-8')).hexdigest()


def _include_source_successor(previous_rows, receipt, expected_receipt_digest,
        row_count, expected_paths_digest, before_census, after_census, schema, label,
        master_blobs=False):
    """Require exact receipt bytes, exact ordered paths, and every prior digest."""
    previous = _checked_source_rows(previous_rows, label + ' prior')
    if _source_census_digest(previous.items()) != before_census:
        raise SystemExit(label + ' predecessor census drift')
    receipt = _require_unaliased_file(receipt, label + ' receipt')
    if digest(receipt) != expected_receipt_digest:
        raise SystemExit(label + ' receipt SHA-256 drift')
    lines = receipt.read_text(encoding='utf-8').splitlines()
    if (lines.count('# schema: ' + schema) != 1
            or lines.count('# target_commit: ' + _MASTER_SOURCE_COMMIT) != 1):
        raise SystemExit(label + ' receipt identity drift')
    entries = [line.split('\t') for line in lines if line and not line.startswith('#')]
    if len(entries) != row_count:
        raise SystemExit(label + ' receipt source count drift')
    current, paths = dict(previous), []
    for cells in entries:
        if len(cells) != 4:
            raise SystemExit('Malformed ' + label + ' receipt row')
        path, before, after, origin = cells
        _receiver_relative_path(path)
        if not path.startswith('m3/collections/src/') or path in paths:
            raise SystemExit('Unapproved or duplicate ' + label + ' receipt path: ' + path)
        if not _is_hex(after, 64) or (before != 'ABSENT' and not _is_hex(before, 64)):
            raise SystemExit('Invalid ' + label + ' receipt digest: ' + path)
        if ((master_blobs and not _is_hex(origin, 40))
                or (not master_blobs and origin != path.rsplit('/', 1)[1] + '.txt')):
            raise SystemExit('Invalid ' + label + ' source origin: ' + path)
        if before == 'ABSENT':
            if path in current:
                raise SystemExit('Occupied new ' + label + ' source: ' + path)
        elif current.get(path) != before:
            raise SystemExit(label + ' source preimage drift: ' + path)
        if before == after:
            raise SystemExit('Unchanged ' + label + ' successor source: ' + path)
        paths.append(path)
        current[path] = after
    path_digest = hashlib.sha256(''.join(path + '\n' for path in paths).encode('utf-8')).hexdigest()
    if paths != sorted(paths) or path_digest != expected_paths_digest:
        raise SystemExit(label + ' approved path census drift')
    if _source_census_digest(current.items()) != after_census:
        raise SystemExit(label + ' installed hash census drift')
    return list(current.items())


def include_master_source_reconciliation(previous_rows, receipt=_MASTER_SOURCE_RECEIPT):
    """Account for the exact existing master gap without altering old receipts."""
    return _include_source_successor(previous_rows, receipt, _MASTER_SOURCE_RECEIPT_SHA256,
        133, _MASTER_SOURCE_PATHS_SHA256, _HISTORICAL_SOURCE_CENSUS_SHA256,
        _MASTER_SOURCE_CENSUS_SHA256, 'M3_EXACT_MASTER_SOURCE_RECONCILIATION_V1',
        'Exact-master reconciliation', master_blobs=True)


def include_five_provider_source_successor(previous_rows, receipt=_FIVE_PROVIDER_SOURCE_RECEIPT):
    """Admit only the six sealed task paths after exact-master reconciliation."""
    return _include_source_successor(previous_rows, receipt, _FIVE_PROVIDER_SOURCE_RECEIPT_SHA256,
        6, _FIVE_PROVIDER_SOURCE_PATHS_SHA256, _MASTER_SOURCE_CENSUS_SHA256,
        _FIVE_PROVIDER_SOURCE_CENSUS_SHA256, 'M3_FIVE_PROVIDER_SOURCE_SUCCESSOR_V1',
        'Five-provider source successor')


def verify_source_census(rows, repo=REPO):
    """Reject missing, changed, duplicate, unknown or symlinked source custody."""
    current = _checked_source_rows(rows, 'installed')
    if _source_census_digest(current.items()) != _FIVE_PROVIDER_SOURCE_CENSUS_SHA256:
        raise SystemExit('Installed source hash census drift')
    for path, expected in current.items():
        source = _require_unaliased_file(repo / _receiver_relative_path(path), 'Installed source')
        if digest(source) != expected:
            raise SystemExit('Installed source drift: ' + path)
    declared = {path for path in current if path.startswith('m3/collections/src/')}
    actual = set()
    source_root = repo / 'm3/collections/src'
    if not source_root.is_dir() or any(part.is_symlink() for part in (source_root, *source_root.parents)):
        raise SystemExit('Source census root missing or aliased')
    for directory, names, files in os.walk(source_root, followlinks=False):
        for name in names:
            if (Path(directory) / name).is_symlink():
                raise SystemExit('Symlinked source census directory: ' + str(Path(directory) / name))
        for name in files:
            source = _require_unaliased_file(Path(directory) / name, 'Source census member')
            actual.add(source.relative_to(repo).as_posix())
    if actual != declared:
        raise SystemExit('Source set changed; qualify a successor packet: missing='
                         + repr(sorted(declared - actual)) + ' unknown=' + repr(sorted(actual - declared)))
    return {path for path in declared if path.endswith('.java')}


def verify_source_admission(here=HERE, repo=REPO):
    """Verify the complete immutable historical chain and its two exact successors."""
    packet_path = _require_unaliased_file(here / 'synexia-donation.json', 'Donation packet')
    packet = read_donation_packet(packet_path)
    manifest_name = packet.get('installed_source_manifest', 'qualification/installed-source-enum-set-20261008.tsv')
    if not isinstance(manifest_name, str):
        raise SystemExit('Invalid installed source manifest path')
    relative = _receiver_relative_path('m3/collections/' + manifest_name)
    manifest = _require_unaliased_file(repo / relative, 'Historical installed source manifest')
    if digest(manifest) != packet['installed_source_manifest_sha256']:
        raise SystemExit('Installed source manifest drift; reconcile with canonical recipe before verifying')
    rows = [line.split('\t') for line in manifest.read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')]
    historical_receipts = []
    for original, expected, include in (
            (_FILTERED_VIEW_RECEIPT, _FILTERED_VIEW_SHA256, include_filtered_view_successor),
            (_REPRESENTATIVE_RECEIPT, _REPRESENTATIVE_SHA256, include_stored_representative_successor),
            (_PATH_MASK_RECEIPT, _PATH_MASK_SHA256, include_path_mask_successor)):
        receipt = _require_unaliased_file(here / original.relative_to(HERE), 'Historical successor receipt')
        rows = include(rows, receipt=receipt)
        historical_receipts.append({'path': receipt.relative_to(repo).as_posix(), 'sha256': expected})
    offset_helper = _require_unaliased_file(here / 'm3_object_state_offset_receipt.py',
                                            'Historical object-state admission helper')
    if digest(offset_helper) != _OBJECT_STATE_OFFSET_HELPER_SHA256:
        raise SystemExit('Historical object-state admission helper SHA-256 drift')
    offset_receipt = _require_unaliased_file(here / 'qualification/object-state-offset-jni-20261017.tsv',
                                             'Historical object-state successor receipt')
    approved_offsets = read_object_state_offset_successor(receipt=offset_receipt)
    rows = include_object_state_offset_successor(rows, successor=approved_offsets)
    historical_receipts.append({'path': offset_receipt.relative_to(repo).as_posix(),
                                 'sha256': _OBJECT_STATE_OFFSET_RECEIPT_SHA256})
    master_receipt = here / _MASTER_SOURCE_RECEIPT.relative_to(HERE)
    task_receipt = here / _FIVE_PROVIDER_SOURCE_RECEIPT.relative_to(HERE)
    rows = include_master_source_reconciliation(rows, receipt=master_receipt)
    rows = include_five_provider_source_successor(rows, receipt=task_receipt)
    declared_java = verify_source_census(rows, repo=repo)
    admission = {
        'schema': 'M3_SOURCE_ADMISSION_SUCCESSION_V1',
        'historical_installed_manifest': {'path': manifest.relative_to(repo).as_posix(), 'sha256': digest(manifest)},
        'historical_successors': historical_receipts,
        'historical_admission_helper': {'path': offset_helper.relative_to(repo).as_posix(),
                                         'sha256': _OBJECT_STATE_OFFSET_HELPER_SHA256},
        'master_reconciliation': {
            'target_commit': _MASTER_SOURCE_COMMIT,
            'path': master_receipt.relative_to(repo).as_posix(),
            'sha256': _MASTER_SOURCE_RECEIPT_SHA256,
            'source_census_sha256': _MASTER_SOURCE_CENSUS_SHA256,
            'existing_admission_gap': {'unlisted_java_sources': 115, 'unlisted_native_sources': 9,
                                       'unlisted_resources': 1, 'stale_java_digests': 8},
        },
        'task_successor': {'path': task_receipt.relative_to(repo).as_posix(),
                           'sha256': _FIVE_PROVIDER_SOURCE_RECEIPT_SHA256, 'changed_or_added_paths': 6},
        'installed_source_census_sha256': _FIVE_PROVIDER_SOURCE_CENSUS_SHA256,
        'installed_receipt_rows': len(rows),
        'java_sources': len(declared_java),
        'native_sources': sum(path.startswith('m3/collections/src/') and path.endswith('.c') for path, _ in rows),
        'resource_files': sum(path.startswith('m3/collections/src/main/resources/') for path, _ in rows),
    }
    return manifest, rows, declared_java, admission


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
    manifest, rows, declared_java, admission = verify_source_admission()
    mains = sorted(REPO / path for path in declared_java if '/src/main/' in path)
    tests = sorted(REPO / path for path in declared_java if '/src/test/' in path)
    probes = [HERE / 'verification' / (name + '.java') for name in
              ('SegmentedLaneNativeProbe', 'PreparedLaneAllocationProbe')]
    classes, test_classes = target / 'classes', target / 'test-classes'
    classes.mkdir()
    test_classes.mkdir()
    common = [java_home / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror']
    run('02-module-compile', common + ['-d', classes] + mains)
    collection_resources = [(path, expected) for path, expected in rows
                            if path.startswith('m3/collections/src/main/resources/')]
    for path, expected in collection_resources:
        relative_resource = Path(path).relative_to('m3/collections/src/main/resources')
        destination = classes / relative_resource
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(REPO / path, destination)
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
        for path, expected in collection_resources:
            name = Path(path).relative_to('m3/collections/src/main/resources').as_posix()
            if hashlib.sha256(archive.read(name)).hexdigest() != expected:
                raise SystemExit('Packaged collection resource drift: ' + path)
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
    # The ordinary core scan exercises the Java backend. This separately counted
    # gate requires the real path-mask JNI backend for count/flag window parity.
    path_mask_native = HERE / 'src/main/native/collections/m3_index_path_masks.c'
    path_mask_library = target / 'libm3_collections_pathmask.so'
    run('10b-path-mask-native-static', native_args + ['-fsyntax-only', path_mask_native])
    run('10c-path-mask-native-compile', native_args + ['-shared', '-fPIC', path_mask_native, '-o', path_mask_library])
    native_count_reports = target / 'object-counts-native-junit-reports'
    run('10d-object-counts-checked-jni', [java_home / 'bin/java', '-Xcheck:jni',
        '-Dm3.interop.requireNative=true', '-Djava.library.path=' + str(target), '-jar', junit,
        'execute', '--disable-banner', '--class-path', runtime_classpath,
        '--select-class', 'com.m3.collections.M3ObjectCountsTest', '--fail-if-no-tests',
        '--reports-dir', native_count_reports])
    native_count_totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for report in native_count_reports.glob('TEST-*.xml'):
        suite = ET.parse(report).getroot()
        for key in native_count_totals:
            native_count_totals[key] += int(suite.attrib.get(key, '0'))
    if (not native_count_totals['tests']
            or any(native_count_totals[key] for key in ('failures', 'errors', 'skipped'))):
        raise SystemExit('Object-counts JNI acceptance requires nonzero tests and no failures/errors/skips: '
                         + repr(native_count_totals))

    if args.cost_probe:
        cost = run('11-collection-cost-probe', [java_home / 'bin/java', '-Xbatch', '-cp', runtime_classpath,
                                               'com.m3.collections.M3CollectionCostProbe'])
        (evidence / 'collection-cost.csv').write_text(cost)
    receipt = {'copyright': 'Copyright 2026 Hitesh Soliwal and contributors', 'license': 'Apache-2.0', 'schema': 'M3_COLLECTION_RECEIVING_QUALIFICATION_V1',
               'scope': 'SEPARATELY_BUILT_COLLECTION_MODULE',
               'installed_source_manifest_sha256': digest(manifest),
               'source_admission': admission,
               'packaged_collection_resources': [path for path, _ in collection_resources],
               'object_counts_checked_jni': {'status': 'PASS', 'junit': native_count_totals,
                   'native_library': 'libm3_collections_pathmask.so',
                   'native_source_sha256': digest(path_mask_native),
                   'required_property': 'm3.interop.requireNative=true',
                   'jvm_check': '-Xcheck:jni', 'undefined_behavior_sanitizer': 'PASS'},
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
