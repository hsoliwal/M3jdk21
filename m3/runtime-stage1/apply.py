#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Apply/reverse the exact singleton-join patch after checking VM source fences."""
import argparse
import hashlib
import json
from pathlib import Path
import os
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]

def digest(data):
    return hashlib.sha256(data).hexdigest()

def checked(path, expected):
    if path.is_symlink() or not path.is_file() or digest(path.read_bytes()) != expected:
        raise ValueError('source pin mismatch: ' + str(path))

def apply(root, reverse=False, check=False):
    manifest = json.loads((HERE / 'manifest.json').read_text())
    insertion = (HERE / 'insertion.txt').read_bytes()
    if digest(insertion) != manifest['insertion_sha256']:
        raise ValueError('recipe insertion changed')
    for relative, expected in manifest['runtime_fences'].items():
        checked(root / relative, expected)
    path = root / manifest['path']
    if path.is_symlink():
        raise ValueError('symlink destination')
    before = path.read_bytes()
    actual = digest(before)
    if actual not in (manifest['before'], manifest['after']):
        raise ValueError('String source drift')
    state = 'before' if actual == manifest['before'] else 'after'
    desired = 'before' if reverse else 'after'
    if check:
        return state
    if state == desired:
        return state
    anchor = manifest['anchor'].encode()
    old, new = (anchor + insertion, anchor) if reverse else (anchor, anchor + insertion)
    if before.count(old) != 1:
        raise ValueError('ambiguous patch anchor')
    after = before.replace(old, new)
    if digest(after) != manifest[desired]:
        raise ValueError('unexpected patch output')
    fd, name = tempfile.mkstemp(dir=path.parent)
    try:
        with os.fdopen(fd, 'wb') as out:
            out.write(after)
        os.chmod(name, path.stat().st_mode)
        os.replace(name, path)
    finally:
        if os.path.exists(name):
            os.unlink(name)
    return desired

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', type=Path, default=ROOT)
    parser.add_argument('--reverse', action='store_true')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    print('RUNTIME_GATE_PASS state=' + apply(args.target, args.reverse, args.check))
