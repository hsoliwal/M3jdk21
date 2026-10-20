#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify sealed Java/JNI multi-pass source chains; no parser, rewrite, or output mutation.

The original M3HashPinnedJavaSnapshotRecipe and M3HashPinnedTextSnapshotRecipe
own application. This is their fail-closed custody/continuation verifier.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re

NAME = re.compile(r'[a-z0-9][a-z0-9-]{0,79}\Z')
HASH = re.compile(r'[0-9a-f]{64}\Z')
MAX_STAGES = 1024
MAX_TARGETS = 1024


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def within(root: Path, name: str) -> Path:
    rel = PurePosixPath(name)
    if (not name or str(rel) != name or rel.is_absolute()
            or '..' in rel.parts or '.git' in rel.parts or '\\' in name
            or any(c in name for c in '\0\t\n\r')):
        raise ValueError('invalid source-relative path: ' + repr(name))
    target = root / name
    for item in (target, *target.parents):
        if item == root:
            break
        if item.is_symlink():
            raise ValueError('symlinked recipe input: ' + name)
    if not target.resolve().is_relative_to(root.resolve()):
        raise ValueError('recipe target escaped root: ' + name)
    return target


def records(path: Path, fields: int) -> list[list[str]]:
    lines = path.read_text(encoding='utf-8').splitlines()
    rows = [line.split('\t') for line in lines if line and not line.startswith('#')]
    if not rows or any(len(row) != fields for row in rows):
        raise ValueError('invalid/empty recipe manifest: ' + str(path))
    return rows


def signed(root: Path, name: str, expected: str) -> bytes:
    raw = within(root, name).read_bytes()
    raw.decode('utf-8', errors='strict')
    if digest(raw) != expected:
        raise ValueError('source SHA-256 drift: ' + name)
    return raw


def verify(root: Path, dataset: str, require_terminal: bool = True) -> dict:
    if not NAME.fullmatch(dataset) or len(dataset) > 32:
        raise ValueError('invalid recipe dataset')
    root = root.resolve(strict=True)
    data = root / 'm3/tooling/shared-frontend/recipe-custody/datasets' / dataset
    meta = json.loads((data / 'DATASET.json').read_text(encoding='utf-8'))
    if meta.get('dataset') != dataset or meta.get('targets', 0) > MAX_TARGETS:
        raise ValueError('dataset identity or size drift')
    crate = meta['crate_name']
    if not NAME.fullmatch(crate):
        raise ValueError('invalid aggregate crate')
    resources = data / 'resources'
    passes = records(data / 'PASSES.tsv', 6)
    if len(passes) > MAX_STAGES:
        raise ValueError('too many recipe passes')
    yaml = within(resources, 'META-INF/rewrite/' + crate + '.yml').read_text(encoding='utf-8')
    named = re.findall(r'^\s+crateName:\s*(\S+)\s*$', yaml, flags=re.M)
    if named != [row[1] for row in passes]:
        raise ValueError('ordered OpenRewrite stage list differs from pass plan')
    aggregate: dict[str, tuple[str, str, str]] = {}
    for kind in ('java', 'text'):
        path = resources / 'com/synexia/rewrite' / ('hash-pinned-' + kind) / crate
        manifest = path / 'manifest.tsv'
        if not manifest.exists():
            continue
        for target, original, final, template in records(manifest, 4):
            within(root, target)
            if (target.endswith('.java') != (kind == 'java') or target in aggregate):
                raise ValueError('duplicate/cross-kind aggregate target')
            if original != 'ABSENT' and not HASH.fullmatch(original):
                raise ValueError('invalid initial hash')
            if not HASH.fullmatch(final):
                raise ValueError('invalid final hash')
            signed(path, template, final)
            if original != 'ABSENT':
                signed(path, 'pre/' + template, original)
            aggregate[target] = (original, final, kind)
    if not aggregate or len(aggregate) != meta['targets']:
        raise ValueError('incomplete aggregate target coverage')
    if [row[0] for row in records(data / 'MODES.tsv', 2)] != sorted(aggregate):
        raise ValueError('aggregate source mode/ownership mismatch')

    # One observed generation vector: no O(stages x files) snapshot table.
    current = {path: (digest(within(root, path).read_bytes())
                      if within(root, path).exists() else 'ABSENT') for path in aggregate}
    states = {path: orig for path, (orig, _, _) in aggregate.items()}
    checkpoint = 0 if states == current else -1
    generations = set(states.items())
    seen = set()
    for i, row in enumerate(passes, 1):
        ordinal, stage, target, template, atom, category = row
        if ordinal != str(i) or not NAME.fullmatch(stage) or stage in seen:
            raise ValueError('stage order or identity violation')
        if not atom.strip() or not category.strip() or target not in aggregate:
            raise ValueError('unclassified/foreign recipe target')
        seen.add(stage)
        kind = aggregate[target][2]
        stage_root = resources / 'com/synexia/rewrite' / ('hash-pinned-' + kind) / stage
        manifest = records(stage_root / 'manifest.tsv', 4)
        if len(manifest) != 1:
            raise ValueError('stage must own exactly one source file')
        path, before, after, resource_name = manifest[0]
        if (target, template) != (path, resource_name):
            raise ValueError('stage target/template identity drift')
        if before != states[target]:
            raise ValueError('noncontiguous source-generation transition at pass ' + str(i))
        if after == 'ABSENT' or not HASH.fullmatch(after):
            raise ValueError('invalid stage output hash')
        signed(stage_root, template, after)
        if before != 'ABSENT':
            signed(stage_root, 'pre/' + template, before)
        states[target] = after
        generations.add((target, after))
        if states == current:
            checkpoint = i
    for path, (_, final, _) in aggregate.items():
        if states[path] != final:
            raise ValueError('not all stages reach their final aggregate postimages')

    # An interruption is admissible only at a global stage boundary, not
    # at an arbitrary mixture of unrelated file generations.
    if checkpoint < 0:
        raise ValueError('product is neither sealed original nor a reachable pass boundary')
    if require_terminal and checkpoint != len(passes):
        raise ValueError('product not yet at terminal generation')
    return {'schema': 'M3_MULTI_PASS_RECEIPT_V1', 'dataset': dataset,
            'stages': len(passes), 'targets': len(aggregate),
            'terminal': checkpoint == len(passes), 'checkpoint': checkpoint,
            'remaining_stages': len(passes) - checkpoint,
            'source_generations': len(generations),
            'plan_sha256': digest((data / 'PASSES.tsv').read_bytes())}


def main() -> None:
    arg = argparse.ArgumentParser(description=__doc__)
    arg.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[5])
    arg.add_argument('--dataset', required=True)
    arg.add_argument('--allow-checkpoint', action='store_true')
    arg.add_argument('--optional', action='store_true',
                     help='skip historical datasets without PASSES.tsv')
    opts = arg.parse_args()
    plan = (opts.root / 'm3/tooling/shared-frontend/recipe-custody/datasets'
            / opts.dataset / 'PASSES.tsv')
    if opts.optional and not plan.is_file():
        print('M3_STAGE_CHAIN_SKIP ' + opts.dataset + ': no multi-pass plan')
        return
    print('M3_STAGE_CHAIN_PASS', json.dumps(
        verify(opts.root, opts.dataset, not opts.allow_checkpoint), sort_keys=True))


if __name__ == '__main__':
    main()
