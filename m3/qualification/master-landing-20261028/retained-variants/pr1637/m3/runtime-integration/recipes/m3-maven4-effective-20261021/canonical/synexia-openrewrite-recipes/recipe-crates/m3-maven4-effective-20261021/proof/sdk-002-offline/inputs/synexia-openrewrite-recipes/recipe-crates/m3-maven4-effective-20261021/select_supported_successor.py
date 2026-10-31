#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Select the supported seven-file successor after the retained CSV refusal.

This finite metadata operation retains every original phase resource, handoff,
preimage and failed SDK attempt. It neither transforms the receiving product
nor broadens a canonical engine. The unchanged engines perform all application.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import subprocess

from prepare_packet import HEADER, NAMED, SOURCE, TASK, put, safe

ORIGINAL_HANDOFF_SHA = '3e66250f18894ed3704ccd68c9d6227d399ffa8eee787fd7c719ed94e70d7164'
SUCCESSOR_HANDOFF_SHA = '359b2929cce0028a8f15297211159613b599d6db5e2a9767bc18e667f1bc93f6'
FAILED_SDK_SHA = '9aaef3787c1fa569dfef8c0c9d2691a1d32f3b139774491f2e25d2cfa7c01f92'
ORIGINAL_SEALS_SHA = 'a2952be6e6ce39e0a360b1753feac582d4a359f492436eaebf3ac4267173e66b'
CSV = 'm3/tooling/module-packs/parser/real-providers/CATALOGUE.csv'


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('repository', type=Path)
    parser.add_argument('original_handoff', type=Path)
    parser.add_argument('successor_handoff', type=Path)
    args = parser.parse_args()
    root = args.repository.resolve(strict=True)
    crate = Path(__file__).resolve().parent
    module = crate.parent.parent
    failed = crate / 'proof/sdk-001-offline'
    frozen_module = failed / 'inputs/synexia-openrewrite-recipes'
    frozen_crate = frozen_module / 'recipe-crates' / TASK
    receipt_bytes = (failed / 'RESULT.json').read_bytes()
    if sha(receipt_bytes) != FAILED_SDK_SHA or json.loads(receipt_bytes)['status'] != 'FAIL':
        raise ValueError('retained failed SDK receipt drift')
    original_bytes = args.original_handoff.read_bytes()
    successor_bytes = args.successor_handoff.read_bytes()
    if sha(original_bytes) != ORIGINAL_HANDOFF_SHA or sha(successor_bytes) != SUCCESSOR_HANDOFF_SHA:
        raise ValueError('frozen handoff drift')
    original = json.loads(original_bytes)
    successor = json.loads(successor_bytes)
    if len(original) != 8 or len(successor) != 7:
        raise ValueError('exact predecessor/successor sizes required')
    if successor != [row for row in original if row['target_path'] != CSV]:
        raise ValueError('successor may exclude only the rejected CSV; every retained row must be identical')
    sealed_bytes = (frozen_crate / 'SOURCE_SEALS.json').read_bytes()
    if sha(sealed_bytes) != ORIGINAL_SEALS_SHA:
        raise ValueError('retained original source seals drift')
    sealed = json.loads(sealed_bytes)
    if subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip() != SOURCE:
        raise ValueError('receiving revision mismatch')
    original_by_path = {row['target_path']: row for row in original}
    for item in sealed['files']:
        row = original_by_path[item['target_path']]
        if any(item[key] != row[key] for key in ('target_path', 'before_sha256', 'after_sha256')):
            raise ValueError('phase/handoff identity mismatch')
        target = safe(root, item['target_path'])
        observed = sha(target.read_bytes()) if target.exists() else 'ABSENT'
        if observed != item['before_sha256']:
            raise ValueError('receiving preimage drift: ' + item['target_path'])
        if sha(Path(row['candidate_path']).read_bytes()) != item['after_sha256']:
            raise ValueError('original candidate drift: ' + item['target_path'])
        resource_root = Path('src/main/resources/com/synexia/rewrite') / ('hash-pinned-' + item['kind']) / item['phase']
        for name in ('manifest.tsv', item['resource']):
            if (module / resource_root / name).read_bytes() != (frozen_module / resource_root / name).read_bytes():
                raise ValueError('original phase resource drift: ' + str(resource_root / name))
        if item['before_sha256'] != 'ABSENT':
            relative = Path('preimages') / item['target_path']
            if (crate / relative).read_bytes() != (frozen_crate / relative).read_bytes():
                raise ValueError('original preimage drift: ' + item['target_path'])
    active = [row for row in sealed['files'] if row['target_path'] != CSV]
    rejected = next(row for row in sealed['files'] if row['target_path'] == CSV)
    phases = 'id\tkind\n' + ''.join(row['phase'] + '\t' + row['kind'] + '\n' for row in active)
    yaml = HEADER + 'type: specs.openrewrite.org/v1beta/recipe\nname: ' + NAMED
    yaml += '\ndisplayName: M3 Maven4 effective model projection 20261021\n'
    yaml += 'description: Apply independently qualified hash-pinned file phases.\nrecipeList:\n'
    for row in active:
        yaml += '  - com.synexia.rewrite.M3HashPinned' + ('Java' if row['kind'] == 'java' else 'Text')
        yaml += 'SnapshotRecipe:\n      crateName: ' + row['phase'] + '\n'
    sealed['files'] = active
    sealed['input_handoffs'] = [
        {'name': args.original_handoff.name, 'sha256': ORIGINAL_HANDOFF_SHA, 'role': 'retained_predecessor'},
        {'name': args.successor_handoff.name, 'sha256': SUCCESSOR_HANDOFF_SHA, 'role': 'selected_successor'}]
    sealed['inactive_phases'] = [{**rejected,
        'reason': 'Unchanged canonical text engine refuses CSV; retained only as an explicit refusal fixture.',
        'failed_attempt': 'proof/sdk-001-offline/RESULT.json', 'failed_attempt_sha256': FAILED_SDK_SHA}]
    sealed['catalogue_decisions'] = {
        'target_path': 'm3/tooling/module-packs/parser/real-providers/maven4/README.md',
        'section': 'Exact donor and ownership decisions',
        'note': 'Already included in the unchanged supported README candidate and linked from real-providers/README.md.'}
    sealed['qualification'] = 'SUCCESSOR_NOT_RUN; source custody is not recipe or runtime proof'
    final_seals = (json.dumps(sealed, indent=2) + '\n').encode()
    yaml_relative = Path('src/main/resources/META-INF/rewrite') / (TASK + '.yml')
    outputs = [
        (crate / 'phases.tsv', phases.encode(), (frozen_crate / 'phases.tsv').read_bytes()),
        (module / yaml_relative, yaml.encode(), (frozen_module / yaml_relative).read_bytes()),
        (crate / 'SOURCE_SEALS.json', final_seals, sealed_bytes)]
    for path, desired, previous in outputs:
        if path.read_bytes() not in (previous, desired):
            raise ValueError('active metadata changed outside the retained successor: ' + str(path))
    custody = crate / 'provenance/successor-002'
    put(custody / 'HANDOFF-001.json', original_bytes)
    put(custody / 'HANDOFF-002.json', successor_bytes)
    for name in ('SOURCE_SEALS.json', 'phases.tsv'):
        put(custody / ('predecessor-' + name), (frozen_crate / name).read_bytes())
    put(custody / ('predecessor-' + TASK + '.yml'), (frozen_module / yaml_relative).read_bytes())
    record = {
        'schema': 'M3_MAVEN4_SUPPORTED_SUCCESSOR_SELECTION/1', 'status': 'SELECTED_NOT_QUALIFIED',
        'selection_script_sha256': sha(Path(__file__).read_bytes()),
        'failed_sdk_sha256': FAILED_SDK_SHA,
        'predecessor_handoff_sha256': ORIGINAL_HANDOFF_SHA,
        'successor_handoff_sha256': SUCCESSOR_HANDOFF_SHA,
        'predecessor_source_seals_sha256': ORIGINAL_SEALS_SHA,
        'successor_source_seals_sha256': sha(final_seals),
        'retained_phase_resources': 8, 'active_phases': 7, 'inactive_phase': rejected['phase'],
        'historical_csv_target_sha256': rejected['before_sha256'],
        'active_candidates_unchanged': True, 'engines_changed': False, 'target_writes': 0,
        'catalogue_decisions': sealed['catalogue_decisions'],
        'note': 'No predecessor output was applied; all successor preimages remain receiving commit2779.'}
    put(custody / 'SELECTION.json', (json.dumps(record, indent=2) + '\n').encode())
    for path, desired, previous in outputs:
        if path.read_bytes() == previous:
            path.write_bytes(desired)
        if path.read_bytes() != desired:
            raise ValueError('metadata readback mismatch: ' + str(path))
    print(json.dumps(record))


if __name__ == '__main__':
    main()
