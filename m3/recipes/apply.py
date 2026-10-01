#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Install the pinned additive M3 files; never edit String or overwrite changes."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil

ROOT = Path(__file__).resolve().parents[2]

def verify(root, hashes):
    for relative, expected in hashes.items():
        path = root / relative
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError('source pin mismatch: ' + relative)

def apply(source, target, check_only=False):
    manifest = json.loads((source / 'm3/recipes/manifest.json').read_text())
    verify(source, manifest['new_files'])
    verify(target, manifest['unchanged_runtime_files'])
    paths = [*manifest['new_files'], 'm3/recipes/manifest.json']
    # Validate every destination before writing anything.
    for relative in paths:
        safe = PurePosixPath(relative)
        if safe.is_absolute() or '..' in safe.parts or safe.parts[0] != 'm3':
            raise ValueError('unsafe destination path')
        path = target / relative
        if path.exists() and path.read_bytes() != (source / relative).read_bytes():
            raise ValueError('refusing modified destination: ' + relative)
    if not check_only:
        for relative in paths:
            path = target / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.exists(): shutil.copy2(source / relative, path)
    return len(paths)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', type=Path, default=ROOT)
    parser.add_argument('--check', action='store_true')
    options = parser.parse_args()
    count = apply(ROOT, options.target.resolve(), options.check)
    print('SOURCE_BOUND_RECIPE_PASS files=' + str(count) + ' check_only=' + str(options.check))
