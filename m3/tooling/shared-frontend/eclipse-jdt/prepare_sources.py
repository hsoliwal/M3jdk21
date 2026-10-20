#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Seal existing source owners for real-parser tests; no substitute parser or network."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil


def inside(root, value):
    relative = PurePosixPath(value)
    if (relative.is_absolute() or '..' in relative.parts or str(relative) != value
            or '\\' in value or not value.endswith('.java')):
        raise ValueError('invalid source path: ' + value)
    path = root / value
    at = path
    while at != root:
        if at.is_symlink():
            raise ValueError('symlink source: ' + value)
        at = at.parent
    if not path.is_file():
        raise ValueError('missing source: ' + str(path))
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--synexia', type=Path, required=True)
    parser.add_argument('--product', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    base = Path(__file__).resolve().parent
    # This helper owns only this Maven module's build directory.
    out = args.out.absolute()
    if out != base / 'target' or out.is_symlink():
        parser.error('--out must be this module target directory')
    roots = {'hsoliwal/com.synexia': args.synexia.resolve(),
             'hsoliwal/M3jdk21': args.product.resolve()}
    manifest = json.loads((base / 'sources.json').read_text())
    staged = {}
    for entry in manifest['sources']:
        if entry['role'] not in ('main', 'test'):
            raise ValueError('unknown source role')
        path = inside(roots[entry['repository']], entry['path'])
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != entry['sha256']:
            raise ValueError('source drift: ' + str(path))
        destination = PurePosixPath(entry['destination'])
        if destination.is_absolute() or '..' in destination.parts or '\\' in str(destination):
            raise ValueError('invalid destination')
        key = (entry['role'], str(destination))
        if key in staged:
            raise ValueError('duplicate source destination')
        staged[key] = data
    for path in sorted((base / 'src/test/java').rglob('*.java')):
        key = ('test', path.relative_to(base / 'src/test/java').as_posix())
        if path.is_symlink() or key in staged:
            raise ValueError('invalid test source')
        staged[key] = path.read_bytes()
    # No output changes until every original source seal has passed.
    out.mkdir(exist_ok=True)
    for role in ('main', 'test'):
        directory = out / ('sealed-' + role)
        if directory.is_symlink():
            raise ValueError('symlink output')
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir()
    for (role, name), data in staged.items():
        path = out / ('sealed-' + role) / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    (out / 'SOURCE_RECEIPT.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('SEALED real JDT source closure:', len(manifest['sources']), 'original files')


if __name__ == '__main__':
    main()
