#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Additive, source-sealed inventory; never rewrite the 36-owner predecessor."""
import argparse
import hashlib
import importlib.util
from pathlib import Path
import re
import subprocess

SHARDS = tuple(f'added-{i:02d}.tsv' for i in range(4))
PREFIX = 'm3/collections/interop/src/'
MAIN = PREFIX + 'main/java/com/m3/collections/interop/'
TEST = PREFIX + 'test/java/com/m3/collections/interop/'
FAMILIES = frozenset(('eclipse-collections', 'five-provider-index', 'guava', 'spring', 'jdk'))
HEX64 = re.compile(r'[0-9a-f]{64}\Z')
HEX40 = re.compile(r'[0-9a-f]{40}\Z')


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()


# The 40-source receipt is immutable. The next generation has exactly one
# source-sealed, Java21-syntax-only test transition and no other valid changes.
COMPOUND_TEST_PATH = 'm3/collections/interop/src/test/java/com/m3/collections/interop/M3FiveLibraryArrayificationTest.java'
COMPOUND_TRANSITION_SHA256 = 'ad81180bc4f5ee73dd008212fb575e62d50f815d673abd063e671c630789c24c'
COMPOUND_TRANSITION = (
    COMPOUND_TEST_PATH,
    '43d670c2278b4db3cb39e8c6d63eff56827c7ef8359e7c85aa8f72b892bba51d',
    '42d78b4fae7525467f0a5d4f34baf45b511e3b55',
    '6d31a53eb94af7c3cfe08ce126ce1be1f8d7da1b09150bd72a12c602d3ab3565',
    'e220622405432368d2c46b32f9f1c197913e7c0a',
    'PINNED_TEST_SYNTAX_ONLY'
)


def exact_compound_transition(receipts: Path) -> tuple[str, ...]:
    file = receipts / 'changed-01.tsv'
    if file.is_symlink() or not file.is_file():
        raise ValueError('missing or aliased one-test source transition receipt')
    data = file.read_bytes()
    if hashlib.sha256(data).hexdigest() != COMPOUND_TRANSITION_SHA256:
        raise ValueError('changed one-test source receipt was tampered')
    rows = [line.split('\t') for line in data.decode('utf-8').splitlines()
            if line and not line.startswith('#')]
    if len(rows) != 1 or tuple(rows[0]) != COMPOUND_TRANSITION:
        raise ValueError('unauthorized one-test source transition')
    return COMPOUND_TRANSITION


def verify_additions(product: Path, receipts: Path,
                     older: set[str], tracked: set[str]) -> list[str]:
    product = product.resolve(strict=True)
    receipts = receipts.resolve(strict=True)
    if len(older) != 36 or any(not p.startswith((MAIN, TEST)) for p in older):
        raise ValueError('original 36-owner admission was not independently established')
    rows = []
    for name in SHARDS:
        shard = receipts / name
        if shard.is_symlink() or not shard.is_file():
            raise ValueError('missing or aliased additive census shard: ' + name)
        lines = [line for line in shard.read_text(encoding='utf-8').splitlines()
                 if line and not line.startswith('#')]
        if len(lines) != 10:
            raise ValueError('additive census shard must contain ten owners: ' + name)
        for line in lines:
            row = line.split('\t')
            if len(row) != 6:
                raise ValueError('invalid additive source identity row: ' + name)
            rows.append(row)
    paths = [row[0] for row in rows]
    if len(rows) != 40 or paths != sorted(set(paths)):
        raise ValueError('additive inventory must be forty unique sorted paths')
    if set(paths) & older:
        raise ValueError('additive census overwrites immutable historical owner')
    expected = older | set(paths)
    if tracked != expected:
        absent = sorted(expected - tracked)
        unadmitted = sorted(tracked - expected)
        raise ValueError('installed interop tracking drift: missing=' + repr(absent[:8])
                         + ', unadmitted=' + repr(unadmitted[:8]))
    # Do not weaken the older forty source identities; the extra receipt names
    # exactly one old/new byte-pinned syntax repair and all other rows stay frozen.
    successor = exact_compound_transition(receipts) if (
        (receipts / 'changed-01.tsv').exists()
        or (receipts / 'changed-01.tsv').is_symlink()) else None
    if successor is not None and successor[0] not in paths:
        raise ValueError('one-test source transition target not admitted')
    production = tests = 0
    for relative, sha, blob, family, kind, license_id in rows:
        if (not relative.endswith('.java') or not (relative.startswith(MAIN)
                or relative.startswith(TEST)) or '..' in Path(relative).parts
                or not HEX64.fullmatch(sha) or not HEX40.fullmatch(blob)
                or family not in FAMILIES or license_id != 'Apache-2.0'):
            raise ValueError('invalid source scope, proof or SPDX admission: ' + relative)
        if kind == 'PRODUCTION' and relative.startswith(MAIN):
            production += 1
        elif kind == 'TEST' and relative.startswith(TEST):
            tests += 1
        else:
            raise ValueError('wrong source kind for original physical path: ' + relative)
        file = product / relative
        if file.is_symlink() or not file.is_file() or not file.resolve().is_relative_to(product):
            raise ValueError('missing or symlinked M3 source: ' + relative)
        data = file.read_bytes()
        if successor is not None and relative == successor[0]:
            if (sha, blob) != (successor[1], successor[2]):
                raise ValueError('invalid one-test predecessor identity')
            sha, blob = successor[3], successor[4]
        if (hashlib.sha256(data).hexdigest() != sha or git_blob(data) != blob
                or b'SPDX-License-Identifier: Apache-2.0' not in data[:1800]):
            raise ValueError('source SHA, Git blob or license header drift: ' + relative)
    if (production, tests) != (13, 27):
        raise ValueError('additive interop production/test dimensions differ')
    return paths


# Source-history commit at which the immutable 36-owner census was authored.
# Checking these bytes at a separate pinned checkout is not a semantic DAG walk.
HISTORICAL_COMMIT = "a720780b1a5ab41ea27182b9b7ad873efdf533b4"
HISTORICAL_CENSUS_BLOB = "9dbfe94767eb36c9371fd931e7513cb11e168768"
HISTORICAL_VERIFIER_BLOB = "efe6d0f959cdc3172d6f6659f06bb486ef825291"


def verify_historical_revisions(product: Path, receipts: Path,
                                historical_product: Path) -> tuple[set[str], int]:
    """Compare two generations without rewriting the historical source receipt."""
    old_script = ('m3/collections/interop/qualification/'
                  'current-census-20261017/verify.py')
    old_census = ('m3/collections/interop/qualification/'
                  'current-census-20261017/current-source-census.tsv')
    historic = historical_product.resolve(strict=True)
    revision = subprocess.check_output(
        ['git', 'rev-parse', 'HEAD'], cwd=historic, text=True).strip()
    if revision != HISTORICAL_COMMIT:
        raise ValueError('wrong historical M3JDK21 source generation: ' + revision)
    for relative, expected_blob in ((old_script, HISTORICAL_VERIFIER_BLOB),
                                    (old_census, HISTORICAL_CENSUS_BLOB)):
        before = historic / relative
        current = product / relative
        if (before.is_symlink() or current.is_symlink() or not before.is_file()
                or not current.is_file()):
            raise ValueError('missing or aliased original census authority')
        if (git_blob(before.read_bytes()) != expected_blob
                or git_blob(current.read_bytes()) != expected_blob):
            raise ValueError('original 36-owner census authority mutated')
    spec = importlib.util.spec_from_file_location('m3_original_census_36', historic / old_script)
    if spec is None or spec.loader is None:
        raise ValueError('historical verifier not importable')
    original = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(original)
    # Run the original verifier UNMODIFIED against its real historical tree.
    admission = original.verify(root=historic, check_tracked=False)
    if len(admission) != 36:
        raise ValueError('original 36-owner baseline proof not established')
    legacy = {row[0]: (row[2], row[3]) for row in original.rows(historic / old_census, 4)}
    if set(legacy) != {path for path, _ in admission}:
        raise ValueError('original 36-owner census and runtime verifier disagree')

    changed = receipts / 'changed-07.tsv'
    if changed.is_symlink() or not changed.is_file():
        raise ValueError('missing or aliased seven-atom revision ledger')
    lines = [line for line in changed.read_text(encoding='utf-8').splitlines()
             if line and not line.startswith('#')]
    rows = [line.split('\t') for line in lines]
    paths = [row[0] for row in rows if len(row) == 8]
    if (len(rows) != 7 or any(len(row) != 8 for row in rows)
            or paths != sorted(set(paths))):
        raise ValueError('seven revised M3 source atoms must be sorted, unique and exact')
    revised = {}
    for path, old_sha, old_blob, new_sha, new_blob, last_commit, license_id, status in rows:
        if (path not in legacy or legacy[path] != (old_sha, old_blob)
                or not HEX64.fullmatch(new_sha) or not HEX40.fullmatch(new_blob)
                or not HEX40.fullmatch(last_commit) or new_sha == old_sha
                or new_blob == old_blob or license_id != 'Apache-2.0'
                or status != 'SOURCE_ONLY_PENDING_CONTRACT'):
            raise ValueError('unadmitted historical source revision: ' + path)
        revised[path] = (new_sha, new_blob)
    for path, historic_identity in legacy.items():
        source = product / path
        if (source.is_symlink() or not source.is_file()
                or not source.resolve().is_relative_to(product)):
            raise ValueError('missing or aliased current historical source: ' + path)
        data = source.read_bytes()
        expected_sha, expected_blob = revised.get(path, historic_identity)
        if hashlib.sha256(data).hexdigest() != expected_sha or git_blob(data) != expected_blob:
            raise ValueError('unadmitted current historical atom drift: ' + path)
        if path in revised and b'SPDX-License-Identifier: Apache-2.0' not in data[:1800]:
            raise ValueError('revised historical atom license mismatch: ' + path)
    return set(legacy), len(revised)


def verify_product_checkout(product: Path, receipts: Path,
                            historical_product: Path) -> tuple[int, int, int]:
    product = product.resolve(strict=True)
    receipts = receipts.resolve(strict=True)
    # A new product receiver must carry the exact additive transformation
    # receipt; the old six-test fixture runs verify_additions without it.
    exact_compound_transition(receipts)
    older, revisions = verify_historical_revisions(product, receipts, historical_product)
    tracked = set(subprocess.check_output(['git', 'ls-files', '--',
        PREFIX + 'main', PREFIX + 'test'], cwd=product, text=True).splitlines())
    extras = verify_additions(product, receipts, older, tracked)
    return len(older), revisions, len(extras)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--product', type=Path, required=True,
                        help='Current M3JDK21 product checkout')
    parser.add_argument('--historical-product', type=Path, required=True,
                        help='Immutable original 36-source M3JDK21 checkout')
    parser.add_argument('--receipts', type=Path, default=Path(__file__).parent / 'receipts')
    args = parser.parse_args()
    original, revisions, additive = verify_product_checkout(
        args.product, args.receipts, args.historical_product)
    print(f'M3_FIVE_LIBRARY_SOURCE_SUPERSET_PASS original={original} revised={revisions} '
          f'additive={additive} total={original + additive}')


if __name__ == '__main__':
    main()
