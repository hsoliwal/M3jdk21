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

SRC = 'm3/collections/interop/src/test/java/com/m3/collections/interop/'
BASE = 'm3/collections/interop/qualification/runtime-junit-census-20261022/'
CURRENT = 'm3/collections/interop/qualification/current-junit-census-20261026/'
HISTORY = BASE + 'verify_surefire.py'
RECEIPT = CURRENT + 'CURRENT_JUNIT_METHOD_SUCCESSION.tsv'
HISTORY_VERIFIER_GIT_BLOB = '91a438127d48fac9b32104a1e98f1a3eb18bdc5d'
HISTORY_SHARD_GIT_BLOBS = (
    '56bca22881cd0e8ad6b90c3b5582ddbd5ae0ef7d',
    'e1e57850ac92df6e36743561044e134375aa128f',
    'b3c2fd2ffd2c71543950e9f788ecce7e875a03e3',
    '5a3e8a287f14898dfc3b6ba7da5f3e5443b87ad2',
)
CURRENT_RECEIPT_SHA256 = '10e9488a22dea3aa7ad2e0c3037be44b1aebbb953469df52018bc75472a9db2e'
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
    if sources != set(legacy) | new_ids:
        raise ValueError('unrecorded/missing JUnit Java source: '
                         + repr(sorted(sources ^ (set(legacy) | new_ids))[:8]))
    expected = set()
    for relative in sorted(sources):
        data = file_bytes(product, relative)
        if relative in upgrades:
            blob, digest, methods = upgrades[relative]
            if git_blob(data) != blob or hashlib.sha256(data).hexdigest() != digest:
                raise ValueError('V4 JUnit source Git/SHA256 mismatch: ' + relative)
        else:
            blob, methods = legacy[relative]
            if git_blob(data) != blob:
                raise ValueError('unreviewed original JUnit source change: ' + relative)
        names = declared_methods(relative, data, methods)
        for name in names:
            ident = 'com.m3.collections.interop.' + Path(relative).stem + '#' + name
            if ident in expected:
                raise ValueError('duplicate JUnit source method identity: ' + ident)
            expected.add(ident)
    if len(expected) != 167:
        raise ValueError(f'current exact method count {len(expected)} != 167')
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
    if suites != 37:
        raise ValueError(f'wrong exact JUnit suite count {suites} != 37')
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
    print(f'M3_INTEROP_JUNIT_CENSUS_V4_PASS sources=48 methods={len(expected)} suites={suites} '
          'original_sources=44 original_methods=158 changed_or_new=13')


if __name__ == '__main__':
    main()
