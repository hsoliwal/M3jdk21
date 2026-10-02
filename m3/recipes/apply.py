#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Replay pinned M3 files; refuse runtime drift and conflicting target edits."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = 'm3/recipes/manifest.json'


def safe_path(root, relative):
    """Reject path aliases and links before reading or writing a managed file."""
    safe = PurePosixPath(relative)
    if (safe.is_absolute() or '..' in safe.parts or not safe.parts
            or safe.parts[0] not in {'m3', 'src'} or safe.as_posix() != relative
            or '\\' in relative or ':' in relative):
        raise ValueError('unsafe destination path: ' + relative)
    path = root
    for component in safe.parts:
        path = path / component
        try:
            stat = path.lstat()
        except FileNotFoundError:
            continue
        if path.is_symlink() or getattr(stat, 'st_file_attributes', 0) & 0x400:
            raise ValueError('unsafe symlink/reparse path: ' + relative)
    return path


def load_manifest(root):
    manifest = json.loads(safe_path(root, MANIFEST).read_text(encoding='utf-8'))
    if manifest.get('schema') != 1:
        raise ValueError('unsupported recipe schema')
    for section in ['new_files', 'unchanged_runtime_files']:
        if not isinstance(manifest.get(section), dict):
            raise ValueError('invalid recipe section: ' + section)
        for relative, expected in manifest[section].items():
            safe_path(root, relative)
            prefix = 'm3/' if section == 'new_files' else 'src/'
            if (not relative.startswith(prefix) or relative == MANIFEST
                    or not isinstance(expected, str) or len(expected) != 64
                    or any(char not in '0123456789abcdef' for char in expected)):
                raise ValueError('unsafe or invalid recipe pin: ' + relative)
    return manifest

def verify(root, hashes):
    for relative, expected in hashes.items():
        path = safe_path(root, relative)
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError('source pin mismatch: ' + relative)

def apply(source, target, check_only=False, baseline=None, reverse=False):
    source, target = source.resolve(), target.resolve()
    manifest = load_manifest(source)
    verify(source, manifest['new_files'])
    if baseline is not None:
        baseline = baseline.resolve()
        previous = load_manifest(baseline)
        if previous['unchanged_runtime_files'] != manifest['unchanged_runtime_files']:
            raise ValueError('runtime fences differ; use a separate runtime recipe')
        verify(baseline, previous['new_files'])
        verify(target, manifest['unchanged_runtime_files'])
        return replay(source, baseline, target, manifest, previous, check_only, reverse)
    if reverse:
        raise ValueError('reverse replay requires an explicit baseline')
    verify(target, manifest['unchanged_runtime_files'])
    paths = [*manifest['new_files'], MANIFEST]
    # Validate every destination before writing anything.
    for relative in paths:
        path = safe_path(target, relative)
        if path.exists() and path.read_bytes() != (source / relative).read_bytes():
            raise ValueError('refusing modified destination: ' + relative)
    if not check_only:
        for relative in paths:
            path = target / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.exists(): shutil.copy2(source / relative, path)
    return len(paths)


def replay(source, baseline, target, manifest, previous, check_only, reverse):
    """Compare exact baseline/source/target bytes, then delegate patching to Git."""
    paths = sorted(set(manifest['new_files']) | set(previous['new_files']) | {MANIFEST})
    old_root, new_root = (source, baseline) if reverse else (baseline, source)
    old_files, new_files = ((manifest['new_files'], previous['new_files']) if reverse
                            else (previous['new_files'], manifest['new_files']))
    changes = []
    for relative in paths:
        old = safe_path(old_root, relative).read_bytes() if relative in old_files or relative == MANIFEST else None
        desired = safe_path(new_root, relative).read_bytes() if relative in new_files or relative == MANIFEST else None
        path = safe_path(target, relative)
        if path.exists() and not path.is_file():
            raise ValueError('target divergence: ' + relative)
        actual = path.read_bytes() if path.exists() else None
        if actual != old and actual != desired:
            raise ValueError('target divergence: ' + relative)
        if actual != desired:
            changes.append((relative, actual, desired))
    if check_only or not changes:
        return len(paths)
    with tempfile.TemporaryDirectory(prefix='m3-recipe-') as directory:
        scratch = Path(directory)
        for state in ['before', 'after']:
            (scratch / state).mkdir()
        for relative, actual, desired in changes:
            for state, data in [('before', actual), ('after', desired)]:
                if data is not None:
                    path = scratch / state / relative
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(data)
        diff = subprocess.run(['git', '-c', 'core.autocrlf=false', '-c', 'color.ui=false',
                               'diff', '--no-index', '--src-prefix=a/', '--dst-prefix=b/',
                               '--binary', '--no-renames', '--no-ext-diff', '--no-textconv',
                               'before', 'after'], cwd=scratch, capture_output=True, check=False)
        if diff.returncode != 1:
            raise ValueError('could not construct recipe patch: ' + diff.stderr.decode('utf-8', errors='replace'))
        patch = scratch / 'replay.patch'
        patch.write_bytes(diff.stdout)
        command = ['git', '-c', 'core.autocrlf=false', 'apply', '-p2', '--whitespace=nowarn']
        subprocess.run(command + ['--check', str(patch)], cwd=target, check=True, capture_output=True)
        subprocess.run(command + [str(patch)], cwd=target, check=True, capture_output=True)
    for relative, _actual, desired in changes:
        path = safe_path(target, relative)
        actual = path.read_bytes() if path.exists() else None
        if actual != desired:
            raise ValueError('postimage mismatch: ' + relative)
    return len(paths)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', type=Path, default=ROOT)
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--baseline', type=Path,
                        help='reviewed last-synchronized source tree with its hash manifest')
    parser.add_argument('--reverse', action='store_true', help='restore the explicit baseline')
    options = parser.parse_args()
    count = apply(ROOT, options.target.resolve(), options.check, options.baseline, options.reverse)
    print('SOURCE_BOUND_RECIPE_PASS files=' + str(count) + ' check_only=' + str(options.check))
