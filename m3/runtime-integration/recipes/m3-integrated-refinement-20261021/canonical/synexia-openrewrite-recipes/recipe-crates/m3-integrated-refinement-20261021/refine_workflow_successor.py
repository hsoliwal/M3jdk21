#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Append the observed CI classpath correction to this already sealed task.

The first-stage SDK002 input snapshot and phase002 remain immutable. Phase049
accepts its exact emitted workflow as its preimage. This custody operation does
not edit a product file or execute a recipe; qualify.py and apply_receiver.py
retain those separate responsibilities.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil

from prepare_packet import TASK, safe

OLD_PHASE = TASK + '-002'
NEW_PHASE = TASK + '-049'
TARGET = '.github/workflows/m3-eclipse-framework-forward-20261017.yml'
OLD_SEALS = '046bd17846177a28a970aa936669d7a92e4b617609c4feda61aabadc303c1871'
OLD_WORKFLOW = 'dbceffdfba2dbaee03e71d6d84ad3f9302620201cf86e20ff0f925dc745ff16a'
NEW_WORKFLOW = 'd808a06805622896a543c48b60aa400caa087d699db83dfe158be1487ca9980d'
HANDOFF = 'c5b988d4b136537c6703282ddc9e6db619e56f3da451cfd3f5ecca51e846ab84'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def put_new(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as stream:
        stream.write(data)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('handoff', type=Path)
    args = parser.parse_args()
    crate = Path(__file__).resolve().parent
    canonical = crate.parents[2]
    module = crate.parent.parent
    frozen = crate / 'proof/sdk-002-offline'
    sealed = json.loads((frozen / 'RESULT.json').read_text())
    assert sealed['status'] == 'PASS' and sealed['source_inputs_unchanged']
    source_rows = json.loads((frozen / 'SOURCE_INPUTS.json').read_text())
    source_hashes = {row['path']: row['sha256'] for row in source_rows}
    yaml = module / 'src/main/resources/META-INF/rewrite' / (TASK + '.yml')
    paths = [crate / 'SOURCE_SEALS.json', crate / 'phases.tsv', yaml]
    old = {path: path.read_bytes() for path in paths}
    for path, data in old.items():
        assert sha(data) == source_hashes[path.relative_to(canonical).as_posix()], path
    assert sha(old[paths[0]]) == OLD_SEALS
    handoff_bytes = args.handoff.read_bytes()
    assert sha(handoff_bytes) == HANDOFF
    rows = json.loads(handoff_bytes)
    assert len(rows) == 1
    row = rows[0]
    assert row['target_path'] == TARGET and row['kind'] == 'TEXT'
    assert row['before_sha256'] == OLD_WORKFLOW and row['after_sha256'] == NEW_WORKFLOW
    candidate = Path(row['candidate_path']).read_bytes()
    assert sha(candidate) == NEW_WORKFLOW
    candidate.decode('utf-8', errors='strict')
    before = safe(frozen / 'receiver', TARGET).read_bytes()
    assert sha(before) == OLD_WORKFLOW
    seals = json.loads(old[paths[0]])
    match = [item for item in seals['files'] if item['target_path'] == TARGET]
    assert len(match) == 1 and match[0]['phase'] == OLD_PHASE
    assert match[0]['after_sha256'] == OLD_WORKFLOW
    predecessor = dict(match[0])
    match[0].update(phase=NEW_PHASE, before_sha256=OLD_WORKFLOW, after_sha256=NEW_WORKFLOW)
    phases_before = old[paths[1]].decode()
    yaml_before = old[yaml].decode()
    assert phases_before.count(OLD_PHASE + '\ttext\n') == 1
    assert yaml_before.count('crateName: ' + OLD_PHASE + '\n') == 1
    phases_after = phases_before.replace(OLD_PHASE + '\ttext\n', NEW_PHASE + '\ttext\n')
    yaml_after = yaml_before.replace('crateName: ' + OLD_PHASE + '\n', 'crateName: ' + NEW_PHASE + '\n')
    resource = module / 'src/main/resources/com/synexia/rewrite/hash-pinned-text' / NEW_PHASE
    preimage = safe(crate / 'preimages' / NEW_PHASE, TARGET)
    evidence = crate / 'proof/workflow-successor-001'
    provenance = crate / 'provenance/WORKFLOW_SUCCESSOR.json'
    for destination in (resource, preimage, evidence, provenance):
        assert not destination.exists(), destination
    manifest = '\t'.join((TARGET, OLD_WORKFLOW, NEW_WORKFLOW, 'postimage.txt')) + '\n'
    predecessor_pin = {
        'path': 'proof/sdk-002-offline/RESULT.json',
        'sha256': sha((frozen / 'RESULT.json').read_bytes())}
    seals['input_handoffs'].append({'name': 'workflow-successor/HANDOFF.json', 'sha256': HANDOFF})
    seals['successor'] = {
        'inactive_predecessor_phase': OLD_PHASE, 'active_successor_phase': NEW_PHASE,
        'original_receiving_revision': seals['receiving_revision'],
        'required_workflow_preimage': OLD_WORKFLOW,
        'first_stage': predecessor_pin,
        'reproduction': 'From pristine2779, first execute the retained SDK002 input snapshot; then execute this successor. All other47 file phases retain their exact prior manifests.',
        'runtime_source_changes_since_first_stage': 0}
    seals['qualification'] = 'NOT_RUN for phase049 at seal creation; prior SDK001/002 remain separate immutable attempts'
    new = {paths[0]: (json.dumps(seals, indent=2) + '\n').encode(),
           paths[1]: phases_after.encode(), yaml: yaml_after.encode()}
    record = {'schema': 'M3_INTEGRATED_WORKFLOW_SUCCESSOR/1',
              'task': TASK, 'predecessor_phase': predecessor,
              'successor_phase': match[0], 'first_stage': predecessor_pin,
              'product_edits': False, 'runtime_source_changes': 0,
              'baseline_contract': seals['successor']['reproduction'],
              'handoff_sha256': HANDOFF,
              'derived_files': [{'path': path.relative_to(canonical).as_posix(),
                                 'before_sha256': sha(old[path]), 'after_sha256': sha(data)}
                                for path, data in new.items()],
              'unchanged_owners': 'All four canonical engines, the harness, copier, qualifier and original phase resources.',
              'qualification': 'PENDING fresh SDK execution with the corrected actual CI classpath'}
    # Every input/hash/absence check precedes any canonical write.
    put_new(resource / 'manifest.tsv', manifest.encode())
    put_new(resource / 'postimage.txt', candidate)
    put_new(preimage, before)
    put_new(evidence / 'HANDOFF.json', handoff_bytes)
    put_new(evidence / 'QUALIFICATION.json', (args.handoff.parent / 'QUALIFICATION.json').read_bytes())
    put_new(evidence / 'refine_workflow_successor.py', Path(__file__).read_bytes())
    put_new(provenance, (json.dumps(record, indent=2) + '\n').encode())
    for path, data in new.items():
        assert path.read_bytes() == old[path], 'concurrent derived-file drift: ' + str(path)
        path.write_bytes(data)
    print(json.dumps({'status': 'SEALED_SUCCESSOR_INPUTS', 'active_phases': len(seals['files']),
                      'new_phase': NEW_PHASE, 'inactive_retained_phase': OLD_PHASE,
                      'source_seals_sha256': sha(new[paths[0]]), 'product_edits': False}))


if __name__ == '__main__':
    main()
