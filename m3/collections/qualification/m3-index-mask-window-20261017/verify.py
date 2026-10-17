#!/usr/bin/env python3
"""Fail-closed receiver template replay: verified ADDITIONS, never overwrites an existing file.

The Java postimages are owned by the existing Maven/OpenRewrite
M3HashPinnedJavaSnapshotRecipe; this script checks the same manifests and owns
only bounded native source installation and check-mode qualification.
"""
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import argparse
import hashlib
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
OWN = Path(__file__).resolve().parent
JAVA_CRATE = ROOT / 'm3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-index-mask-window-20261017'
NATIVE_CRATE = OWN


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def rows(crate: Path, manifest: Path):
    entries = []
    last = ''
    for line in manifest.read_text(encoding='utf8').splitlines():
        if not line or line.startswith('#'):
            continue
        cells = line.split('\t')
        if len(cells) != 4:
            raise ValueError('invalid recipe manifest row')
        name, before, after, resource = cells
        path = Path(name)
        if (path.is_absolute() or '..' in path.parts or '.' in path.parts
                or name <= last or before != 'ABSENT' or len(after) != 64
                or any(c not in '0123456789abcdef' for c in after)):
            raise ValueError('invalid path/order/source provenance')
        if ('/' in resource or '\\' in resource):
            # Native template uses resources/ explicitly (still below trusted crate).
            if resource != 'resources/m3_index_mask_window.c.txt':
                raise ValueError('invalid native template resource')
        payload = (crate / resource).read_bytes()
        if sha256(payload) != after:
            raise ValueError('tampered recipe template: ' + name)
        entries.append((name, after, payload))
        last = name
    if not entries:
        raise ValueError('empty source recipe')
    return entries


def replay(workspace: Path, targets, apply: bool) -> int:
    edits = []
    workspace = workspace.resolve()
    for name, after, payload in targets:
        dest = workspace / name
        if not dest.resolve().is_relative_to(workspace):
            raise ValueError('workspace traversal/refusal: ' + name)
        if dest.is_file():
            if sha256(dest.read_bytes()) != after:
                raise ValueError('source differs from checked recipe: ' + name)
        elif dest.exists():
            raise ValueError('not a regular file: ' + name)
        else:
            edits.append((dest, payload))
    if not apply and edits:
        raise ValueError(f'missing generated postimages: {len(edits)}')
    for dest, payload in edits:
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(payload)
    return len(edits)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true', help='explicit additive candidate generation')
    parser.add_argument('--workspace', type=Path, default=ROOT)
    args = parser.parse_args()
    targets = rows(JAVA_CRATE, JAVA_CRATE / 'manifest.tsv') + rows(NATIVE_CRATE, NATIVE_CRATE / 'native-manifest.tsv')
    paths = [name for name, _, _ in targets]
    if len(paths) != len(set(paths)):
        raise ValueError('duplicate ownership')
    count = replay(args.workspace, targets, args.apply)
    if not args.apply:
        with tempfile.TemporaryDirectory() as directory:
            sandbox = Path(directory)
            first = replay(sandbox, targets, True)
            second = replay(sandbox, targets, True)
            if first != len(targets) or second != 0:
                raise AssertionError('template did not converge in two passes')
            poison = sandbox / paths[0]
            poison.write_bytes(poison.read_bytes() + b'// drift\n')
            try:
                replay(sandbox, targets, True)
            except ValueError:
                pass
            else:
                raise AssertionError('tampered source must fail closed')
    print(f'PASS INDEX_MASK_WINDOW_RECIPE targets={len(targets)} changes={count} fixed_point=ok drift=refused')


if __name__ == '__main__':
    main()
