#!/usr/bin/env python3
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Successor of the sealed 36+40 interop census: admits the eight provider stream bridges (leaf E5).

The 20261022 verifier runs unmodified (its file is pinned by SHA-256 here) over a view of the product: the tracked
interop sources, minus the successor rows, with the five pins master revised in place after the v1 seal restored to
their predecessor bytes (receipts/REPIN.tsv: the two current-census-20261017 files, taken from the historical
checkout, and three tests, kept under predecessors/). Each revised file must equal its after pin in the product. So
the 36-owner historical proof, the four 10-owner shards, the 13/27 dimensions and the source transitions all still
hold. The one successor shard (receipts/added-04.tsv) adds exactly eight sources, four production and four tests,
each pinned by SHA-256, Git blob and an Apache-2.0 SPDX header.
"""
import argparse
import hashlib
import importlib.util
from pathlib import Path
import shutil
import subprocess
import tempfile

HERE = Path(__file__).resolve().parent
V1_DIR = HERE.parent / 'current-census-superset-20261022'
V1_SHA256 = '1567b80c0d308436112559782091b8398258c5e2cf7609b55e8d5c4d94e9ef30'
SHARD = 'added-04.tsv'
REPIN = 'REPIN.tsv'
PREDECESSORS = HERE / 'predecessors'
INTEROP = 'm3/collections/interop/'
CENSUS_DIR = INTEROP + 'qualification/current-census-20261017/'
FAMILIES = frozenset(('guava', 'apache', 'eclipse-collections', 'spring'))
SUCCESSOR_ROWS = 8
SUCCESSOR_PRODUCTION = 4
REPIN_ROWS = 5


def load_v1(directory: Path = V1_DIR):
    """The sealed 20261022 verifier, refused unless its bytes are the pinned ones."""
    path = directory / 'verify_superset.py'
    if path.is_symlink() or not path.is_file():
        raise ValueError('missing or aliased predecessor census verifier')
    if hashlib.sha256(path.read_bytes()).hexdigest() != V1_SHA256:
        raise ValueError('predecessor census verifier differs from its pinned bytes')
    spec = importlib.util.spec_from_file_location('m3_census_superset_v1', path)
    if spec is None or spec.loader is None:
        raise ValueError('predecessor census verifier not importable')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def successor_rows(v1, receipts: Path) -> list:
    shard = receipts / SHARD
    if shard.is_symlink() or not shard.is_file():
        raise ValueError('missing or aliased successor census shard')
    rows = [line.split('\t') for line in shard.read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')]
    paths = [row[0] for row in rows]
    if len(rows) != SUCCESSOR_ROWS or any(len(row) != 6 for row in rows) or paths != sorted(set(paths)):
        raise ValueError('successor shard must hold eight unique sorted six-column rows')
    production = 0
    for relative, sha, blob, family, kind, license_id in rows:
        main = relative.startswith(v1.MAIN)
        if (not relative.endswith('.java') or not (main or relative.startswith(v1.TEST))
                or '..' in Path(relative).parts or not v1.HEX64.fullmatch(sha) or not v1.HEX40.fullmatch(blob)
                or family not in FAMILIES or license_id != 'Apache-2.0'
                or kind != ('PRODUCTION' if main else 'TEST')):
            raise ValueError('invalid successor scope, kind, family or license: ' + relative)
        production += main
    if production != SUCCESSOR_PRODUCTION:
        raise ValueError('successor production/test dimensions differ')
    return rows


def verify_successor_sources(v1, product: Path, rows: list) -> None:
    product = product.resolve(strict=True)
    for relative, sha, blob, _family, _kind, _license in rows:
        file = product / relative
        if file.is_symlink() or not file.is_file() or not file.resolve().is_relative_to(product):
            raise ValueError('missing or symlinked successor source: ' + relative)
        data = file.read_bytes()
        if (hashlib.sha256(data).hexdigest() != sha or v1.git_blob(data) != blob
                or b'SPDX-License-Identifier: Apache-2.0' not in data[:1800]):
            raise ValueError('successor source SHA, Git blob or license header drift: ' + relative)


def verify_tracked(v1, product: Path, receipts_v1: Path, receipts: Path, older: set, tracked: set):
    """v1's additive proof over the tracked set minus the successor rows, then the successor rows themselves."""
    rows = successor_rows(v1, receipts)
    added = {row[0] for row in rows}
    if added & older:
        raise ValueError('successor shard overwrites an immutable historical owner')
    missing = sorted(added - tracked)
    if missing:
        raise ValueError('successor sources not tracked: ' + repr(missing[:8]))
    extras = v1.verify_additions(product, receipts_v1, older, tracked - added)
    if added & set(extras):
        raise ValueError('successor shard repeats a predecessor additive owner')
    verify_successor_sources(v1, product, rows)
    return extras, rows


def repin_rows(v1, receipts: Path) -> list:
    """The pins master revised in place after the v1 seal, each with its predecessor source."""
    file = receipts / REPIN
    if file.is_symlink() or not file.is_file():
        raise ValueError('missing or aliased re-pin receipt')
    rows = [line.split('\t') for line in file.read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')]
    if len(rows) != REPIN_ROWS or any(len(row) != 7 for row in rows):
        raise ValueError('re-pin receipt must hold five seven-column rows')
    for path, source, before_sha, before_blob, after_sha, after_blob, _commits in rows:
        if (not path.startswith((CENSUS_DIR, v1.MAIN, v1.TEST)) or '..' in Path(path).parts
                or source not in ('historical', 'predecessors') or (source == 'historical') != path.startswith(CENSUS_DIR)
                or not all(v1.HEX64.fullmatch(sha) for sha in (before_sha, after_sha))
                or not all(v1.HEX40.fullmatch(blob) for blob in (before_blob, after_blob))):
            raise ValueError('invalid re-pin row: ' + path)
    return rows


def build_view(v1, product: Path, historical: Path, rows: list, tracked: set, view: Path,
               predecessors: Path = PREDECESSORS) -> Path:
    """The product's tracked interop sources and census files, with each re-pinned file at its predecessor bytes."""
    for relative in sorted(tracked | {CENSUS_DIR + 'verify.py', CENSUS_DIR + 'current-source-census.tsv'}):
        source = product / relative
        if source.is_symlink() or not source.is_file():
            raise ValueError('missing or aliased product file: ' + relative)
        target = view / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    for path, source, before_sha, before_blob, after_sha, after_blob, _commits in rows:
        current = (product / path).read_bytes()
        if hashlib.sha256(current).hexdigest() != after_sha or v1.git_blob(current) != after_blob:
            raise ValueError('re-pinned file differs from its after pin: ' + path)
        origin = (historical / path if source == 'historical'
                  else predecessors / (path[len(INTEROP):].replace('/', '~') + '.txt'))
        if origin.is_symlink() or not origin.is_file():
            raise ValueError('missing or aliased predecessor: ' + path)
        before = origin.read_bytes()
        if hashlib.sha256(before).hexdigest() != before_sha or v1.git_blob(before) != before_blob:
            raise ValueError('predecessor differs from its before pin: ' + path)
        (view / path).write_bytes(before)
    return view


def verify_product_checkout(product: Path, receipts_v1: Path, receipts: Path, historical_product: Path):
    v1 = load_v1()
    product = product.resolve(strict=True)
    historical = historical_product.resolve(strict=True)
    rows = repin_rows(v1, receipts)
    tracked = set(subprocess.check_output(['git', 'ls-files', '--', v1.PREFIX + 'main', v1.PREFIX + 'test'],
                                          cwd=product, text=True).splitlines())
    with tempfile.TemporaryDirectory() as temporary:
        view = build_view(v1, product, historical, rows, tracked, Path(temporary).resolve())
        v1.exact_compound_transition(receipts_v1)
        older, revisions = v1.verify_historical_revisions(view, receipts_v1, historical)
        extras, added = verify_tracked(v1, view, receipts_v1, receipts, older, tracked)
    return len(older), revisions, len(extras), len(added)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--product', type=Path, required=True, help='M3JDK21 checkout under test')
    parser.add_argument('--historical-product', type=Path, required=True,
                        help='Immutable original 36-source M3JDK21 checkout')
    parser.add_argument('--receipts-v1', type=Path, default=V1_DIR / 'receipts')
    parser.add_argument('--receipts', type=Path, default=HERE / 'receipts')
    args = parser.parse_args()
    original, revisions, additive, successor = verify_product_checkout(
        args.product, args.receipts_v1, args.receipts, args.historical_product)
    print(f'M3_FIVE_LIBRARY_SOURCE_SUPERSET_V2_PASS original={original} revised={revisions} '
          f'additive={additive} successor={successor} total={original + additive + successor}')


if __name__ == '__main__':
    main()
