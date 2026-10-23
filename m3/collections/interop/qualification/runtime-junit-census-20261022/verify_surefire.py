#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Reject missing, extra or skipped JUnit methods in the M3JDK21 interop module.

Source paths, original Git blobs and @Test identities are sealed in Synexia.
The Maven Surefire test case XML must match the exact source method inventory.
"""
import argparse
import hashlib
from pathlib import Path
import re
import xml.etree.ElementTree as ET

PACKAGE = 'com.m3.collections.interop'
SOURCE_PREFIX = 'm3/collections/interop/src/test/java/com/m3/collections/interop/'
SHARDS = tuple(f'expected-{i:02d}.tsv' for i in range(4))
TEST = re.compile(r'@Test\b\s+(?:(?:public|protected|private|static|final|synchronized)\s+)*'
                  r'[A-Za-z_$][\w$<>\[\]., ?]*\s+([A-Za-z_$][\w$]*)\s*\(')


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()


def expected_methods(product: Path, receipts: Path, *, strict: bool = True) -> set[str]:
    product = product.resolve(strict=True)
    receipts = receipts.resolve(strict=True)
    rows = []
    for shard in SHARDS if strict else ('expected-00.tsv',):
        path = receipts / shard
        if path.is_symlink() or not path.is_file():
            raise ValueError('missing or aliased JUnit source method receipt: ' + shard)
        chunk = [line.split('\t') for line in path.read_text(encoding='utf-8').splitlines()
                 if line and not line.startswith('#')]
        if strict and len(chunk) != 11:
            raise ValueError('exactly eleven source files per JUnit receipt shard required')
        rows.extend(chunk)
    paths = [row[0] for row in rows if len(row) == 3]
    if len(rows) != len(paths) or paths != sorted(set(paths)):
        raise ValueError('missing, duplicated or unordered JUnit source identity')
    if strict and len(rows) != 44:
        raise ValueError('expected 44 exact JUnit source owners')
    source = product / SOURCE_PREFIX
    actual_sources = {str(p.relative_to(product)).replace('\\', '/')
                      for p in source.glob('*.java') if p.is_file() or p.is_symlink()}
    if actual_sources != set(paths):
        raise ValueError('unknown or missing JUnit source file: ' +
                         repr(sorted(actual_sources ^ set(paths))[:8]))
    expected = set()
    for relative, blob, declarations in rows:
        if not relative.startswith(SOURCE_PREFIX) or not relative.endswith('.java'):
            raise ValueError('unapproved JUnit source path: ' + relative)
        path = product / relative
        if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(product):
            raise ValueError('missing or aliased JUnit Java source: ' + relative)
        data = path.read_bytes()
        if not re.fullmatch(r'[a-f0-9]{40}', blob) or git_blob(data) != blob:
            raise ValueError('JUnit method source Git blob drift: ' + relative)
        if b'SPDX-License-Identifier: Apache-2.0' not in data[:1800]:
            raise ValueError('unadmitted JUnit source license: ' + relative)
        text = data.decode('utf-8')
        if '@ParameterizedTest' in text or '@RepeatedTest' in text or '@TestFactory' in text:
            raise ValueError('unadmitted JUnit test form needs an explicit recipe')
        found = TEST.findall(text)
        if len(found) != len(re.findall(r'@Test\b', text)) or len(found) != len(set(found)):
            raise ValueError('unparseable or duplicate JUnit method declaration: ' + relative)
        sealed = [] if declarations == '-' else declarations.split(',')
        if sealed != found:
            raise ValueError('JUnit method declaration drift: ' + relative)
        class_name = PACKAGE + '.' + path.stem
        for name in sealed:
            if not re.fullmatch(r'[A-Za-z_$][\w$]*', name):
                raise ValueError('invalid JUnit method identifier')
            method = class_name + '#' + name
            if method in expected:
                raise ValueError('duplicate JUnit method ID: ' + method)
            expected.add(method)
    if strict and len(expected) != 158:
        raise ValueError('expected exact 158 JUnit test methods')
    return expected


def verify_reports(reports: Path, expected: set[str]) -> int:
    files = sorted(reports.glob('TEST-*.xml'))
    if not files or any(path.is_symlink() for path in files):
        raise ValueError('missing or aliased Maven Surefire XML reports')
    actual = set()
    suites = 0
    for file in files:
        root = ET.parse(file).getroot()
        if root.tag != 'testsuite':
            raise ValueError('non-JUnit Surefire suite: ' + file.name)
        suites += 1
        declared = int(root.get('tests', '-1'))
        cases = list(root.findall('testcase'))
        if declared != len(cases):
            raise ValueError('incomplete Maven Surefire test case inventory: ' + file.name)
        if any(int(root.get(field, '-1')) != 0
               for field in ('errors', 'failures', 'skipped')):
            raise ValueError('failed, errored or skipped JUnit suite: ' + file.name)
        for case in cases:
            if case.find('failure') is not None or case.find('error') is not None \
                    or case.find('skipped') is not None:
                raise ValueError('failed or skipped JUnit testcase: ' + file.name)
            name, klass = case.get('name'), case.get('classname')
            if not name or not klass:
                raise ValueError('Surefire report lacks source test identity')
            identifier = klass + '#' + name
            if identifier in actual:
                raise ValueError('duplicate executed JUnit test: ' + identifier)
            actual.add(identifier)
    if actual != expected:
        missing, added = sorted(expected - actual), sorted(actual - expected)
        raise ValueError('JUnit execution diverged: missing=' + repr(missing[:8])
                         + ', unexpected=' + repr(added[:8]))
    return suites


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--product', type=Path, required=True)
    parser.add_argument('--receipts', type=Path, default=Path(__file__).parent / 'receipts')
    parser.add_argument('--reports', type=Path, required=True)
    args = parser.parse_args()
    expected = expected_methods(args.product, args.receipts)
    suites = verify_reports(args.reports, expected)
    print(f'M3_INTEROP_JUNIT_CENSUS_PASS sources=44 tests={len(expected)} suites={suites}')


if __name__ == '__main__':
    main()
