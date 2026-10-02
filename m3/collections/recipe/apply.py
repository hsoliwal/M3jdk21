#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Install the exact reviewed M3 primitive-collections postimages; never edit java.util."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil

ROOT = Path(__file__).resolve().parents[3]
MODULE = ROOT / 'm3/collections'

def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def verify(root: Path, hashes: dict[str, str]) -> None:
    for relative, expected in hashes.items():
        path = root / relative
        if not path.is_file() or digest(path) != expected:
            raise ValueError('source pin mismatch: ' + relative)

def apply(source_root: Path, target_root: Path, check_only: bool = False) -> int:
    module = source_root / 'm3/collections'
    manifest = json.loads((module / 'recipe/manifest.json').read_text(encoding='utf-8'))
    verify(module, manifest['new_files'])
    verify(target_root, manifest['unchanged_jdk_guards'])
    paths = list(manifest['new_files'])
    for relative in paths:
        safe = PurePosixPath(relative)
        if safe.is_absolute() or '..' in safe.parts:
            raise ValueError('unsafe source-relative path: ' + relative)
        destination = target_root / 'm3/collections' / relative
        source = module / relative
        if destination.exists() and destination.read_bytes() != source.read_bytes():
            raise ValueError('refusing modified destination: ' + destination.as_posix())
    if not check_only:
        for relative in paths:
            destination = target_root / 'm3/collections' / relative
            source = module / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            if not destination.exists():
                shutil.copy2(source, destination)
    return len(paths)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', type=Path, default=ROOT)
    parser.add_argument('--check', action='store_true')
    options = parser.parse_args()
    count = apply(ROOT, options.target.resolve(), options.check)
    print('M3_COLLECTIONS_RECIPE_PASS files=' + str(count) + ' check_only=' + str(options.check))
