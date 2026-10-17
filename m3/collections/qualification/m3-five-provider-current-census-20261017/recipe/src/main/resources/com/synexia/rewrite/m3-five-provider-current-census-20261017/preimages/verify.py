#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed M3 interop installed-source successor; never relax the historical recipe.

Historical five-file postimages remain immutable. This receipt admits exactly the
current thirteen first-party source/test owners and their SHA-256 and Git blobs.
"""
import hashlib
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4] if len(HERE.parents) > 4 else Path.cwd()
CENSUS = HERE / 'current-source-census.tsv'
LEGACY = ROOT / 'm3/collections/interop/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/collections-library-interop/manifest.tsv'
RECEIPT_SHA256 = 'ba783e04a32200f951c16e0df1b6d9cc1b78b1499bc8bc3fff6ac9aaf6909a63'
LEGACY_GIT_BLOB = 'bdc0cab41cd4033421388fbd44f37203d765f57e'
BASE = 'm3/collections/interop/src/'
MAIN = BASE + 'main/java/com/m3/collections/interop/'
TEST = BASE + 'test/java/com/m3/collections/interop/'
ALLOWED_PATHS = tuple(sorted([
    MAIN + name for name in (
        'M3ApacheCollections.java', 'M3ApacheListValuedMap.java',
        'M3ApacheSetValuedMap.java', 'M3LibraryBinding.java',
        'M3LibraryBindings.java', 'M3LibraryCollections.java',
        'M3LibraryCollectors.java', 'M3SpringMultiValueMap.java')
] + [
    TEST + name for name in (
        'M3ApacheCollectionsTest.java', 'M3LibraryBindingsSelfTest.java',
        'M3LibraryClosureTest.java', 'M3LibraryCollectionsTest.java',
        'M3SdkFamiliesTest.java')
]))

def sha256(data):
    return hashlib.sha256(data).hexdigest()

def git_blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\x00' + data).hexdigest()

def rows(path, width):
    if path.is_symlink() or not path.is_file():
        raise ValueError('source seal missing or aliased: ' + str(path))
    result = []
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        parts = line.split('\t')
        if len(parts) != width:
            raise ValueError('malformed sealed source row: ' + line[:160])
        result.append(parts)
    if len({r[0] for r in result}) != len(result):
        raise ValueError('duplicate source path in ' + str(path))
    return result

def verify(root=ROOT, census=CENSUS, legacy=LEGACY,
           receipt_sha=RECEIPT_SHA256, legacy_blob=LEGACY_GIT_BLOB,
           check_tracked=True):
    root = Path(root)
    census = Path(census)
    legacy = Path(legacy)
    if census.is_symlink() or not census.is_file() or sha256(census.read_bytes()) != receipt_sha:
        raise ValueError('drifted, missing or aliased current-source receipt')
    if legacy.is_symlink() or not legacy.is_file() or git_blob(legacy.read_bytes()) != legacy_blob:
        raise ValueError('historical five-file receipt changed')
    previous = rows(legacy, 4)
    if len(previous) != 5:
        raise ValueError('original recipe is not the exact five-file historical owner')
    old = {path: after for path, before, after, template in previous}
    for path, before, after, template in previous:
        if before != 'ABSENT' or not template.endswith('.java.txt'):
            raise ValueError('historical receipt record corrupted: ' + path)
    current = rows(census, 4)
    if len(current) != len(ALLOWED_PATHS) or tuple(r[0] for r in current) != ALLOWED_PATHS:
        raise ValueError('current interop file census not exact/reordered')
    if check_tracked:
        tracked = set(subprocess.check_output(
            ['git', 'ls-files', '--', BASE + 'main', BASE + 'test'],
            cwd=root, text=True).splitlines())
        if tracked != set(ALLOWED_PATHS):
            raise ValueError('installed interop files missing or not admitted: ' +
                             repr(sorted(tracked ^ set(ALLOWED_PATHS))))
    report = []
    for path, before, after, blob in current:
        if ((before != old[path] if path in old else before != 'ABSENT')
                or len(after) != 64 or any(v not in '0123456789abcdef' for v in after)
                or len(blob) != 40 or any(v not in '0123456789abcdef' for v in blob)):
            raise ValueError('invalid exact predecessor/current identity: ' + path)
        if path in old and path != MAIN + 'M3LibraryCollections.java' and after != old[path]:
            raise ValueError('changed unapproved historical implementation: ' + path)
        source = root / path
        if source.is_symlink() or not source.is_file():
            raise ValueError('missing or aliased current source: ' + path)
        data = source.read_bytes()
        if sha256(data) != after or git_blob(data) != blob:
            raise ValueError('current interop source drift: ' + path)
        report.append((path, after))
    return report

if __name__ == '__main__':
    matched = verify()
    for path, sha in matched:
        print('SEALED', path, sha)
    print('PASS exact current interop successor', len(matched))
