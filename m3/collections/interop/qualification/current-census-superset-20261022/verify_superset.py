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
        if (hashlib.sha256(data).hexdigest() != sha or git_blob(data) != blob
                or b'SPDX-License-Identifier: Apache-2.0' not in data[:1800]):
            raise ValueError('source SHA, Git blob or license header drift: ' + relative)
    if (production, tests) != (13, 27):
        raise ValueError('additive interop production/test dimensions differ')
    return paths


def verify_product_checkout(product: Path, receipts: Path) -> tuple[int, int]:
    product = product.resolve(strict=True)
    previous_script = (product / 'm3/collections/interop/qualification/'
                       'current-census-20261017/verify.py')
    if not previous_script.is_file() or previous_script.is_symlink():
        raise ValueError('original 36-owner census verifier absent')
    spec = importlib.util.spec_from_file_location('m3_original_census_36', previous_script)
    if spec is None or spec.loader is None:
        raise ValueError('original census verifier unimportable')
    original = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(original)
    # Prove original 36 source bytes and historical five-file predecessor first.
    admitted = original.verify(root=product, check_tracked=False)
    older = {path for path, _ in admitted}
    tracked = set(subprocess.check_output(['git', 'ls-files', '--',
        PREFIX + 'main', PREFIX + 'test'], cwd=product, text=True).splitlines())
    extras = verify_additions(product, receipts, older, tracked)
    return len(older), len(extras)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--product', type=Path, required=True,
                        help='Source checkout of pinned M3JDK21 commit')
    parser.add_argument('--receipts', type=Path, default=Path(__file__).parent / 'receipts')
    args = parser.parse_args()
    original, additive = verify_product_checkout(args.product, args.receipts)
    print(f'M3_FIVE_LIBRARY_SOURCE_SUPERSET_PASS original={original} additive={additive} '
          f'total={original + additive}')


if __name__ == '__main__':
    main()
