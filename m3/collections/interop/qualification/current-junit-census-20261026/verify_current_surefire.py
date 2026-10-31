#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Source-sealed JUnit V4 successor. Original 44-source/158-method census is immutable.

The original four historical shards remain unchanged. Nine changed JUnit sources
(including one new Apache 4.5.0 test) and four new stream source classes are
admitted only under a versioned 13-row receipt. Reuse the historical JUnit XML
verifier and fail closed on extra/missing tests, source byte changes or skips.
"""
import argparse
import hashlib
import importlib.util
from pathlib import Path
import re
import xml.etree.ElementTree as ET

SRC = 'm3/collections/interop/src/test/java/jdk/m3/collections/interop/'
BASE = 'm3/collections/interop/qualification/runtime-junit-census-20261022/'
CURRENT = 'm3/collections/interop/qualification/current-junit-census-20261026/'
HISTORY = BASE + 'verify_surefire.py'
RECEIPT = CURRENT + 'CURRENT_JUNIT_METHOD_SUCCESSION.tsv'
HISTORY_VERIFIER_GIT_BLOB = '0f8f6c50854d53672c3a07d26473b8457d439cbf'
HISTORY_SHARD_GIT_BLOBS = (
    'c34283d303f06188d021c58a8556bdf13f884a5f',
    'da78d98b21c3a1ad26ba6c7724e37730e8788b50',
    '90c39dcd40c8b682c849845f5b8912ddddf11d3f',
    'a9e1b9ba0ffbc2beff2c0152df3430d4a823d2d3',
)
CURRENT_RECEIPT_SHA256 = '319af78cef83f8e4d4dd0028d8b234a6b2237d40fca1b4f24e035b079c83774e'
NEW_SOURCE_NAMES = frozenset((
    'M3ApacheStreamsTest.java', 'M3EclipseStreamsTest.java',
    'M3GuavaStreamsTest.java', 'M3SpringStreamsTest.java',
))
CHANGED_SOURCE_NAMES = frozenset((
    'M3ApacheBagContractTest.java', 'M3ApacheCollectionsTest.java',
    'M3FiveLibraryArrayificationTest.java', 'M3FiveLibraryKeyRangesTest.java',
    'M3FiveProviderFlagSelectionTest.java', 'M3LibraryClosureTest.java',
    'M3SdkFamiliesTest.java', 'M3SpringArrayContractTest.java',
    'M3SpringCaseInsensitiveSdkFactoryTest.java',
))
# Sources added after the V4 receipt, admitted by exact identity: the two provider tests master received through
# superset #2166 (2026-10-30). name -> (git blob, SHA-256, @Test methods in source order). Any other source, or other
# bytes for these names, still fails.
ADMITTED_ADDITIONS = {
    'M3Flat3MapProviderTest.java': (
        '254d85294724b1cdcaa26a2c526da0fa17d623be',
        'c9498ec115a48a9c23c884c62f1011af6bb439b2826a12d454a38f0768b17a47',
        'nullMappingsAndThreeToFourGrowthPreserveReturnIdentity,'
        'collidingKeysAcrossWordBoundariesSurviveDeletionAndReuse,'
        'seededEightMethodDifferentialMatchesProviderAndJdk,'
        'equalValuesAndObservedHashCallbacksDoNotChangeSharedResults'),
    'M3FrozenLongSetProviderTest.java': (
        '4d88d4e3912ab9a5528f8c81a836d501bcf88511',
        'd089236cb463d8b016c01b9c26216b918e7ec497442d6f01850c50216150291e',
        'differentialAgainstGuavaImmutableSortedSet,differentialAgainstEclipseImmutableLongSet'),
}
V4_SOURCES = 48
V4_METHODS = 167
V4_SUITES = 37
EXPECTED_SOURCES = V4_SOURCES + len(ADMITTED_ADDITIONS)
EXPECTED_METHODS = V4_METHODS + sum(len(m.split(',')) for _b, _s, m in ADMITTED_ADDITIONS.values())
EXPECTED_SUITES = V4_SUITES + len(ADMITTED_ADDITIONS)
HEX40 = re.compile(r'[0-9a-f]{40}\Z')
HEX64 = re.compile(r'[0-9a-f]{64}\Z')
TEST = re.compile(r'@Test\b\s+(?:(?:public|protected|private|static|final|synchronized)\s+)*'
                  r'[A-Za-z_$][\w$<>\[\]., ?]*\s+([A-Za-z_$][\w$]*)\s*\(')


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()


def file_bytes(root: Path, relative: str) -> bytes:
    path = root / relative
    if (path.is_symlink() or not path.is_file()
            or not path.resolve().is_relative_to(root)):
        raise ValueError('missing or aliased source/receipt: ' + relative)
    return path.read_bytes()


def declared_methods(path: str, data: bytes, methods: str) -> list[str]:
    if not path.startswith(SRC) or not path.endswith('.java'):
        raise ValueError('out-of-scope JUnit source: ' + path)
    if b'SPDX-License-Identifier: Apache-2.0' not in data[:1800]:
        raise ValueError('unadmitted source license: ' + path)
    source = data.decode('utf-8')
    if any(kind in source for kind in ('@ParameterizedTest', '@RepeatedTest', '@TestFactory')):
        raise ValueError('unadmitted JUnit test form: ' + path)
    found = TEST.findall(source)
    if len(found) != len(re.findall(r'@Test\b', source)) or len(set(found)) != len(found):
        raise ValueError('unparseable/duplicate @Test declaration: ' + path)
    sealed = [] if methods == '-' else methods.split(',')
    if found != sealed:
        raise ValueError('JUnit method declaration drift: ' + path)
    if any(not re.fullmatch(r'[A-Za-z_$][\w$]*', name) for name in found):
        raise ValueError('invalid JUnit method: ' + path)
    return found


def expected_methods(product: Path) -> set[str]:
    product = product.resolve(strict=True)
    original = file_bytes(product, HISTORY)
    if git_blob(original) != HISTORY_VERIFIER_GIT_BLOB:
        raise ValueError('historical original JUnit verifier changed')
    prior = []
    for idx, expected_blob in enumerate(HISTORY_SHARD_GIT_BLOBS):
        raw = file_bytes(product, BASE + f'receipts/expected-{idx:02d}.tsv')
        if git_blob(raw) != expected_blob:
            raise ValueError('historical original JUnit receipt changed')
        rows = [line.split('\t') for line in raw.decode('utf-8').splitlines()
                if line and not line.startswith('#')]
        if len(rows) != 11 or any(len(row) != 3 for row in rows):
            raise ValueError('historical receipt row count or columns')
        prior.extend(rows)
    legacy_paths = [row[0] for row in prior]
    if len(prior) != 44 or legacy_paths != sorted(set(legacy_paths)):
        raise ValueError('historical receipt owner scope/order drift')
    legacy = {row[0]: (row[1], row[2]) for row in prior}
    old_ids = {SRC + name for name in CHANGED_SOURCE_NAMES}
    new_ids = {SRC + name for name in NEW_SOURCE_NAMES}
    if not old_ids.issubset(legacy) or new_ids & set(legacy):
        raise ValueError('incorrect changed/new source generation')
    baseline_count = sum(0 if methods == '-' else len(methods.split(','))
                         for _path, _blob, methods in prior)
    if baseline_count != 158:
        raise ValueError('historical baseline method count drift')

    receipt_data = file_bytes(product, RECEIPT)
    if hashlib.sha256(receipt_data).hexdigest() != CURRENT_RECEIPT_SHA256:
        raise ValueError('V4 JUnit source-succession receipt content drift')
    revision = [line.split('\t') for line in receipt_data.decode('utf-8').splitlines()
                if line and not line.startswith('#')]
    targets = old_ids | new_ids
    if (len(revision) != 13 or any(len(row) != 5 for row in revision)
            or [row[0] for row in revision] != sorted(targets)):
        raise ValueError('V4 source succession scope/order drift')
    upgrades = {}
    for relative, old_blob, now_blob, sha256, methods in revision:
        if not HEX40.fullmatch(now_blob) or not HEX64.fullmatch(sha256):
            raise ValueError('invalid V4 source digest/identity')
        if relative in old_ids:
            if legacy[relative][0] != old_blob or old_blob == now_blob:
                raise ValueError('wrong source predecessor or phantom JUnit change: ' + relative)
        elif old_blob != 'ABSENT':
            raise ValueError('new source must have absent original identity: ' + relative)
        upgrades[relative] = (now_blob, sha256, methods)

    actual_java = product / SRC
    sources = {str(p.relative_to(product)).replace('\\', '/')
               for p in actual_java.glob('*.java') if p.is_file() or p.is_symlink()}
    added_ids = {SRC + name for name in ADMITTED_ADDITIONS}
    if added_ids & (set(legacy) | new_ids):
        raise ValueError('admitted addition collides with a recorded source')
    if sources != set(legacy) | new_ids | added_ids:
        raise ValueError('unrecorded/missing JUnit Java source: '
                         + repr(sorted(sources ^ (set(legacy) | new_ids | added_ids))[:8]))
    expected = set()
    for relative in sorted(sources):
        data = file_bytes(product, relative)
        if relative in upgrades:
            blob, digest, methods = upgrades[relative]
            if git_blob(data) != blob or hashlib.sha256(data).hexdigest() != digest:
                raise ValueError('V4 JUnit source Git/SHA256 mismatch: ' + relative)
        elif relative in added_ids:
            blob, digest, methods = ADMITTED_ADDITIONS[Path(relative).name]
            if git_blob(data) != blob or hashlib.sha256(data).hexdigest() != digest:
                raise ValueError('admitted JUnit source addition drift: ' + relative)
        else:
            blob, methods = legacy[relative]
            if git_blob(data) != blob:
                raise ValueError('unreviewed original JUnit source change: ' + relative)
        names = declared_methods(relative, data, methods)
        for name in names:
            ident = 'jdk.m3.collections.interop.' + Path(relative).stem + '#' + name
            if ident in expected:
                raise ValueError('duplicate JUnit source method identity: ' + ident)
            expected.add(ident)
    if len(expected) != EXPECTED_METHODS:
        raise ValueError(f'current exact method count {len(expected)} != {EXPECTED_METHODS}')
    return expected


def verify_reports(product: Path, reports: Path, expected: set[str]) -> int:
    product = product.resolve(strict=True)
    old = product / HISTORY
    spec = importlib.util.spec_from_file_location('_historical_m3_interop_surefire', old)
    if spec is None or spec.loader is None:
        raise ValueError('unable to bind historical verifier')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    # Original executable JUnit XML semantics, not a simplified replacement.
    suites = module.verify_reports(reports, expected)
    if suites != EXPECTED_SUITES:
        raise ValueError(f'wrong exact JUnit suite count {suites} != {EXPECTED_SUITES}')
    # Historical verifier rejects errors/failures/skipped; reject hidden flaky reruns too.
    for path in sorted(reports.glob('TEST-*.xml')):
        root = ET.parse(path).getroot()
        for testcase in root.findall('testcase'):
            if any(child.tag in ('flakyFailure', 'rerunFailure', 'flakyError', 'rerunError')
                   for child in testcase):
                raise ValueError('hidden flaky/rerun failure: ' + path.name)
    return suites


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--product', type=Path, required=True)
    parser.add_argument('--reports', type=Path, required=True)
    args = parser.parse_args()
    expected = expected_methods(args.product)
    suites = verify_reports(args.product, args.reports, expected)
    print(f'M3_INTEROP_JUNIT_CENSUS_V4_PASS sources={EXPECTED_SOURCES} methods={len(expected)} '
          f'suites={suites} original_sources=44 original_methods=158 changed_or_new=13 '
          f'admitted_additions={len(ADMITTED_ADDITIONS)}')


if __name__ == '__main__':
    main()
