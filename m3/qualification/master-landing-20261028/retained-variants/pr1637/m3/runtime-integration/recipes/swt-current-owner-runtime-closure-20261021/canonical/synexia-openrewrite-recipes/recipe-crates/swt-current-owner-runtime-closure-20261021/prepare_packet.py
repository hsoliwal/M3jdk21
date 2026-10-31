#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026-10-21 from qualified integrated SDK003 for one SWT proof-POM correction.
"""Seal this task's reviewed fixtures for the existing canonical snapshot engines.

This preparation step writes only the canonical task resources and immutable
input custody. It does not transform or modify the receiving product. Actual
OpenRewrite execution and the proof-checked copier remain separate steps.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

TASK = 'swt-current-owner-runtime-closure-20261021'
NAMED = 'com.synexia.swt.CurrentOwnerRuntimeClosure20261021'
SOURCE = 'c0ffa6d76338ed1b06cfe0158b0e04d8930df546'
CANONICAL = 'a2b50cfe926c5c5f400c7f659a947a5020fb8d4d'
HEADER = '# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project\n# SPDX-License-Identifier: Apache-2.0\n'


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def safe(root: Path, relative: str) -> Path:
    path = Path(relative)
    if path.is_absolute() or path.as_posix() != relative or any(
            part in ('', '.', '..') for part in relative.split('/')) or '\\' in relative:
        raise ValueError('noncanonical task path: ' + relative)
    result = root / path
    for item in [result, *result.parents]:
        if item == root.parent:
            break
        if item.is_symlink():
            raise ValueError('symbolic task path: ' + str(item))
    return result


def put(path: Path, data: bytes) -> None:
    if path.exists():
        if path.read_bytes() != data:
            raise ValueError('prepared fixture drift; use an additive successor: ' + str(path))
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as stream:
        stream.write(data)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('repository', type=Path)
    parser.add_argument('handoff', type=Path, nargs='+')
    args = parser.parse_args()
    root = args.repository.resolve(strict=True)
    crate = Path(__file__).resolve().parent
    module = crate.parent.parent
    head = subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()
    if head != SOURCE:
        raise ValueError('receiving revision mismatch: ' + head)
    rows = []
    handoffs = []
    for path in args.handoff:
        data = path.read_bytes()
        value = json.loads(data)
        files = value if isinstance(value, list) else value['files']
        if not isinstance(files, list):
            raise ValueError('handoff files must be an array')
        rows.extend(files)
        handoffs.append({'name': path.name, 'sha256': sha(data)})
    if not 1 <= len(rows) <= 64:
        raise ValueError('task file phase budget: 1..64')
    rows.sort(key=lambda row: row['target_path'])
    seen = set()
    prepared = []
    for index, row in enumerate(rows, 1):
        required = {'target_path', 'candidate_path', 'before_sha256', 'after_sha256', 'kind'}
        if not required.issubset(row):
            raise ValueError('missing handoff field')
        target = row['target_path']
        if target in seen:
            raise ValueError('duplicate file owner: ' + target)
        seen.add(target)
        kind = row['kind'].lower()
        if kind not in ('java', 'text') or (target.endswith('.java') != (kind == 'java')):
            raise ValueError('source kind mismatch: ' + target)
        before, after = row['before_sha256'], row['after_sha256']
        if before != 'ABSENT' and re.fullmatch('[0-9a-f]{64}', before) is None:
            raise ValueError('invalid preimage hash: ' + target)
        if re.fullmatch('[0-9a-f]{64}', after) is None:
            raise ValueError('postimage digest required: ' + target)
        source_path = safe(root, target)
        baseline = source_path.read_bytes() if source_path.exists() else None
        if (sha(baseline) if baseline is not None else 'ABSENT') != before:
            raise ValueError('receiving preimage drift: ' + target)
        candidate = Path(row['candidate_path']).resolve(strict=True).read_bytes()
        if sha(candidate) != after:
            raise ValueError('candidate hash drift: ' + target)
        candidate.decode('utf-8', errors='strict')
        if baseline is not None:
            baseline.decode('utf-8', errors='strict')
        phase = f'{TASK}-{index:03d}'
        resource = 'postimage.java.txt' if kind == 'java' else 'postimage.txt'
        prepared.append({'target_path': target, 'before_sha256': before, 'after_sha256': after,
                         'kind': kind, 'phase': phase, 'resource': resource,
                         'baseline': baseline, 'candidate': candidate})
    # All input, source, kind, duplicate and byte checks precede any custody writes.
    phases = 'id\tkind\n'
    yaml = HEADER + 'type: specs.openrewrite.org/v1beta/recipe\nname: ' + NAMED
    yaml += '\ndisplayName: SWT current owner proof runtime closure 20261021\n'
    yaml += 'description: Apply independently qualified hash-pinned file phases.\nrecipeList:\n'
    seals = []
    for row in prepared:
        phase, kind, target = row['phase'], row['kind'], row['target_path']
        phases += phase + '\t' + kind + '\n'
        engine = 'Java' if kind == 'java' else 'Text'
        yaml += '  - com.synexia.rewrite.M3HashPinned' + engine + 'SnapshotRecipe:\n      crateName: ' + phase + '\n'
        resources = module / 'src/main/resources/com/synexia/rewrite' / ('hash-pinned-' + kind) / phase
        manifest = '\t'.join((target, row['before_sha256'], row['after_sha256'], row['resource'])) + '\n'
        put(resources / 'manifest.tsv', manifest.encode())
        put(resources / row['resource'], row['candidate'])
        if row['baseline'] is not None:
            put(safe(crate / 'preimages', target), row['baseline'])
        seals.append({key: value for key, value in row.items() if key not in ('baseline', 'candidate')})
    put(crate / 'phases.tsv', phases.encode())
    put(module / 'src/main/resources/META-INF/rewrite' / (TASK + '.yml'), yaml.encode())
    record = {'schema': 'SWT_CURRENT_OWNER_RUNTIME_CLOSURE_SOURCE_SEALS/1', 'task': TASK, 'recipe': NAMED,
              'canonical_source_revision': CANONICAL, 'receiving_repository': 'hsoliwal/eclipse.platform.swt',
              'receiving_revision': SOURCE, 'files': seals, 'input_handoffs': handoffs,
              'qualification': 'NOT_RUN; source custody is not recipe or runtime proof'}
    put(crate / 'SOURCE_SEALS.json', (json.dumps(record, indent=2) + '\n').encode())
    print(json.dumps({'status': 'SEALED_INPUTS', 'phases': len(seals), 'target_paths': len(seen),
                      'source_seals_sha256': sha((crate / 'SOURCE_SEALS.json').read_bytes()),
                      'recipe_executed': False}))


if __name__ == '__main__':
    main()
