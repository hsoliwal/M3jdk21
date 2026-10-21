#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only custody proof for existing OpenRewrite's ordered, single-file M3String recipes.

The real transformations are M3HashPinnedJavaSnapshotRecipe stages executed by
M3TaskRecipeTest/Maven. This verifier neither parses source nor rewrites files.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re

DATASET = 'm3-prefix-cursor-20261020'
CRATE = 'm3-ast-build-reverse-20261017-' + DATASET
PREFIX = Path('m3/tooling/shared-frontend/recipe-custody/datasets') / DATASET
RESOURCE = PREFIX / 'resources/com/synexia/rewrite/hash-pinned-java'
DESCRIPTOR = PREFIX / 'resources/META-INF/rewrite' / (CRATE + '.yml')
PASS_PATHS = (
    'src/java.base/share/classes/java/lang/M3String.java',
    'src/java.base/share/classes/java/lang/M3StringPrecomputeBridge.java',
    'test/jdk/java/lang/String/M3StringCompositePrefixCursorTest.java',
)
STAGES = tuple(DATASET + '-p' + str(n) for n in (1, 2, 3))


def digest(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def safe_file(root: Path, name: str) -> Path:
    relative = PurePosixPath(name)
    if (not name or str(relative) != name or relative.is_absolute()
            or any(s in ('', '.', '..', '.git') for s in relative.parts)
            or '\\' in name or any(c in name for c in '\0\r\n\t')):
        raise ValueError('unsafe source path: ' + repr(name))
    target = root / name
    if target.is_symlink() or not target.resolve().is_relative_to(root.resolve()):
        raise ValueError('escaped/symlinked source: ' + name)
    for parent in target.parents:
        if parent == root:
            break
        if parent.is_symlink():
            raise ValueError('symlinked parent: ' + name)
    return target


def lines(path: Path, count: int) -> list[list[str]]:
    result = []
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        parts = line.split('\t')
        if len(parts) != count:
            raise ValueError('malformed manifest row: ' + str(path))
        result.append(parts)
    return result


def verify(root: Path) -> dict[str, object]:
    root = root.resolve(strict=True)
    data = root / PREFIX
    manifest = lines(root / RESOURCE / CRATE / 'manifest.tsv', 4)
    plan = lines(data / 'PASSES.tsv', 6)
    modes = lines(data / 'MODES.tsv', 2)
    metadata = json.loads((data / 'DATASET.json').read_text(encoding='utf-8'))
    descriptor = (root / DESCRIPTOR).read_text(encoding='utf-8')
    expected_stages = list(STAGES)
    actual_stages = re.findall(r'^\s+crateName:\s+(\S+)\s*$', descriptor, re.M)
    if actual_stages != expected_stages or descriptor.count('M3HashPinnedJavaSnapshotRecipe:') != 3:
        raise ValueError('OpenRewrite ordered stage composition drift')
    if not descriptor.count('name: com.synexia.m3jdk.AstBuildReverse20261017') == 1:
        raise ValueError('canonical recipe identity drift')
    if len(manifest) != 3 or [row[0] for row in manifest] != list(PASS_PATHS):
        raise ValueError('aggregate target identity/order drift')
    if len(plan) != 3 or len(modes) != 3:
        raise ValueError('expected exactly three single-file passes')
    if metadata['crate_name'] != CRATE or metadata['targets'] != 3 or metadata['java_targets'] != 3:
        raise ValueError('aggregate dataset identity drift')
    if modes != [[path, '0644'] for path in PASS_PATHS]:
        raise ValueError('target mode/ordering drift')

    original: dict[str, bytes | None] = {}
    expected: dict[str, bytes] = {}
    for ordinal, row in enumerate(manifest):
        path, pre_sha, post_sha, resource = row
        phase, stage, selected, stage_resource, atom, category = plan[ordinal]
        if (phase != str(ordinal + 1) or stage != STAGES[ordinal]
                or path != selected or resource != stage_resource
                or not atom.strip() or not category.strip()):
            raise ValueError('nonserial or unclassified behavioral-atom pass')
        if not re.fullmatch(r'[0-9a-f]{64}', post_sha):
            raise ValueError('invalid target digest')
        if not (pre_sha == 'ABSENT' or re.fullmatch(r'[0-9a-f]{64}', pre_sha)):
            raise ValueError('invalid preimage digest')

        aggregate = root / RESOURCE / CRATE
        single = root / RESOURCE / stage
        if lines(single / 'manifest.tsv', 4) != [row]:
            raise ValueError('stage must change exactly one admitted file: ' + stage)
        output = safe_file(aggregate, resource).read_bytes()
        candidate = safe_file(single, resource).read_bytes()
        if output != candidate or digest(candidate) != post_sha:
            raise ValueError('stage output drift: ' + path)
        old: bytes | None = None
        if pre_sha != 'ABSENT':
            old = safe_file(aggregate, 'pre/' + resource).read_bytes()
            stage_old = safe_file(single, 'pre/' + resource).read_bytes()
            if old != stage_old or digest(old) != pre_sha:
                raise ValueError('stage preimage drift: ' + path)
        if digest(safe_file(root, path).read_bytes()) != post_sha:
            raise ValueError('product source not equal to qualified postimage: ' + path)
        original[path], expected[path] = old, output

    # Proof model only. Actual Java LST transformations must execute through Maven.
    state = dict(original)
    edits = 0
    for path in PASS_PATHS:
        if state[path] != expected[path]:
            state[path] = expected[path]
            edits += 1
    if edits != 3 or any(state[p] != expected[p] for p in PASS_PATHS):
        raise ValueError('first serial replay did not reach exact output')
    fixed_point = sum(state[path] != expected[path] for path in PASS_PATHS)
    if fixed_point:
        raise ValueError('second replay is not fixed point')
    # Catch foreign single-file additions disguised as another stage.
    crates = root / RESOURCE
    actual_manifests = {p.parent.name for p in crates.glob('*/manifest.tsv')}
    if actual_manifests != {CRATE, *STAGES}:
        raise ValueError('extra or missing OpenRewrite stage manifest')
    return {'phases': ['inventory', 'atomize', 'compare', 'rewrite-file-1',
                       'rewrite-file-2', 'write-oracle', 'contract', 'fixed-point'],
            'file_local_recipes': len(plan), 'source_changes': edits,
            'fixed_point_changes': fixed_point,
            'aggregate_sha256': digest((root / RESOURCE / CRATE / 'manifest.tsv').read_bytes())}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path,
                        default=Path(__file__).resolve().parents[3])
    args = parser.parse_args()
    print('M3_MULTI_PASS_CUSTODY_PASS ' + json.dumps(verify(args.root), sort_keys=True))


if __name__ == '__main__':
    main()
