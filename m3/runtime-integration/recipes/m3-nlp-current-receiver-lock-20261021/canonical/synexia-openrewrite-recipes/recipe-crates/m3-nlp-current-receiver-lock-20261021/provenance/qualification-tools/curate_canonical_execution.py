#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Retain exact finite execution evidence for the new NLP canonical owner."""
from pathlib import Path
import hashlib
import json

ROOT = Path('/workspace/scratch/206049dfb270')
CANONICAL = ROOT / 'synexia'
CRATE = CANONICAL / 'synexia-openrewrite-recipes/recipe-crates/m3-nlp-current-receiver-lock-20261021'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def put(relative, data):
    destination = CRATE / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open('xb') as stream:
        stream.write(data)
    assert destination.read_bytes() == data


def main():
    pairs = []
    for source, destination in [
        ('canonical-work/nlp-preparation-001', 'proof/preparation-refusal-001'),
        ('canonical-work/nlp-owner-sdk-001', 'proof/isolated-sdk-custody-001'),
        ('canonical-work/nlp-receiver-qualification-001', 'proof/copier-qualification-001'),
        ('integration-work/nlp-baseline-hydration-001', 'proof/unchanged-baseline-hydration-001'),
        ('integration-work/nlp-receiver-001', 'proof/receiving-application-001')]:
        for file in sorted((ROOT / source).iterdir()):
            if file.is_file():
                assert not file.is_symlink() and file.suffix in ('.json', '.log', '.txt')
                pairs.append((file, destination + '/' + file.name))
    pairs.append((Path(__file__), 'provenance/qualification-tools/curate_canonical_execution.py'))
    pairs.append((ROOT / 'canonical-work/run_nlp_owner_sdk.py', 'provenance/qualification-tools/run_isolated_sdk.py'))
    rows = []
    for source, relative in pairs:
        data = source.read_bytes()
        data.decode('utf-8')
        put(relative, data)
        rows.append({'source_path': str(source), 'path': relative, 'bytes': len(data), 'sha256': sha(data)})
    observation = {
        'schema': 'M3_NLP_PREPARATION_FOLLOWUP/1',
        'original_failure': 'proof/preparation-refusal-001/RESULT.json',
        'classification': 'Exact preimage guard refusal while the parent was hydrating immutable baseline files. No canonical postimages were emitted by that failed preparation.',
        'custody_limit': 'The first invocation did not record the transient file bytes before refusal. The later observation finds both exact expected hashes; root hydration receipts independently bind unchanged baseline files. The overlap is a timing inference, not a fabricated historical preimage hash.',
        'correction': 'Wait for baseline hydration, then repeat the identical preparation command with unchanged source and guard.',
        'successful_command': ['python3', '-B', 'synexia/synexia-openrewrite-recipes/recipe-crates/m3-nlp-current-receiver-lock-20261021/prepare_packet.py', 'M3jdk21', 'nlp-work/source-lock-correction/HANDOFF.json'],
        'command_custody': 'Retrospective exact transcription from this session tool call; not claimed to have been stored before execution.',
        'result': {'status': 'SEALED_INPUTS', 'phases': 2, 'target_paths': 2,
                   'source_seals_sha256': '6cc232bd392dd188b227a0a7c304d0a6e6d25ed91dd3641fefb335afdfc8574e',
                   'recipe_executed': False},
        'later_actual_recipe_result': 'proof/sdk-001-offline/RESULT.json',
        'hydration_scope': 'Unchanged baseline source custody only; actual modifications were applied later by the qualified canonical recipe copier.'}
    put('proof/preparation-refusal-001/FOLLOWUP.json', (json.dumps(observation, indent=2) + '\n').encode())
    receipt = {'schema': 'M3_NLP_CANONICAL_EXECUTION_INTAKE/1', 'status': 'EXACT_READBACK',
               'files': rows, 'copied_files': len(rows),
               'bytes': sum(row['bytes'] for row in rows),
               'scope': 'Finite text evidence; no compiled classes, JARs, native payloads or receiving source writes.'}
    put('provenance/CANONICAL_EXECUTION_INTAKE.json', (json.dumps(receipt, indent=2) + '\n').encode())
    print(json.dumps({'status': 'PASS', 'copied_files': len(rows), 'bytes': receipt['bytes']}))


if __name__ == '__main__':
    main()
