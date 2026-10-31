#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Preserve bounded Maven4 authoring evidence and rights without compiled payloads."""
from pathlib import Path
import difflib
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[2]
CRATE = ROOT / 'synexia/synexia-openrewrite-recipes/recipe-crates/m3-maven4-effective-20261021'
RECORDS = []


def sha(data):
    return hashlib.sha256(data).hexdigest()


def put(path, data):
    if path.exists():
        assert path.read_bytes() == data, 'immutable evidence drift: ' + str(path)
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open('xb') as stream:
            stream.write(data)
    assert path.read_bytes() == data


def copy(source, destination):
    assert source.is_file() and not source.is_symlink(), source
    data = source.read_bytes()
    data.decode('utf-8', errors='strict')
    assert re.search(rb'(?:ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,}|Authorization:\s*Bearer\s+\S+)', data) is None, source
    put(CRATE / destination, data)
    assert source.read_bytes() == data, 'source changed during intake: ' + str(source)
    RECORDS.append({'source_path': str(source), 'crate_path': destination,
                    'bytes': len(data), 'sha256': sha(data)})


for source in sorted((ROOT / 'maven-work/evidence').iterdir()):
    if source.is_file() and source.suffix in ('.json', '.log', '.txt'):
        copy(source, 'proof/runtime-authoring/evidence/' + source.name)
for folder in ('qualified-candidate-001', 'qualified-candidate-002'):
    for source in sorted((ROOT / 'maven-work' / folder).iterdir()):
        if source.is_file() and source.suffix in ('.json', '.log', '.txt', '.xml'):
            copy(source, 'proof/runtime-authoring/' + folder + '/' + source.name)
for source in sorted((ROOT / 'maven-work/evidence/source-build-001-reports').iterdir()):
    if source.is_file() and source.suffix in ('.xml', '.txt'):
        copy(source, 'proof/runtime-authoring/evidence/source-build-001-reports/' + source.name)
for source in sorted((ROOT / 'jobs-work/maven4-recipe/receiver-qualification-002').iterdir()):
    if source.is_file() and source.suffix in ('.json', '.log'):
        copy(source, 'proof/receiver-qualification-002/' + source.name)
for name in ('RESULT.json', 'PARSER_CLASSPATH.txt'):
    copy(ROOT / 'jobs-work/maven4-recipe/parser-dependencies-001' / name,
         'proof/parser-custody-001/' + name)
for name in ('author-scaffold-001.log', 'parser-snapshot-001.log', 'prepare-001.log',
             'qualify-sdk-001.log', 'select-successor-002.log', 'qualify-sdk-002.log',
             'qualify-receiver-002.log'):
    copy(ROOT / 'jobs-work/maven4-recipe' / name, 'proof/authoring-commands/' + name)
for name in ('author_packet.py', 'snapshot_parser_dependencies.py', 'curate_evidence.py'):
    copy(ROOT / 'jobs-work/maven4-recipe' / name, 'provenance/qualification-tools/' + name)
license_pins = {
    'LICENSE': 'cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30',
    'NOTICE': '633cdd68388defe137dd4b6a02bc9656cefae3b33c64b371e31b104aad40adac'}
for name, expected in license_pins.items():
    source = ROOT / 'maven-work/fork' / name
    assert sha(source.read_bytes()) == expected
    copy(source, 'licenses/Maven-Fork-' + name)

# The initial lineage remains immutable; its successor records the actual source
# changes made after the retained SDK001 refusal, without replacing its history.
original_root = CRATE / 'proof/sdk-001-offline/inputs/synexia-openrewrite-recipes/recipe-crates/m3-maven4-effective-20261021'
adaptations = []
for name in ('qualify.py', 'src/test/java/com/synexia/rewrite/M3Maven4EffectiveRecipeTest.java'):
    original = (original_root / name).read_bytes()
    current = (CRATE / name).read_bytes()
    diff = ''.join(difflib.unified_diff(original.decode().splitlines(keepends=True),
        current.decode().splitlines(keepends=True), fromfile='sdk-001/' + name, tofile='sdk-002/' + name))
    relative = 'provenance/successor-002/' + Path(name).name + '.diff'
    put(CRATE / relative, diff.encode())
    adaptations.append({'path': name, 'before_sha256': sha(original), 'after_sha256': sha(current),
                        'diff': relative, 'diff_sha256': sha(diff.encode())})
lineage = {
    'schema': 'M3_MAVEN4_EFFECTIVE_SUCCESSOR_LINEAGE/1',
    'initial_lineage_path': 'provenance/RECIPE_LINEAGE.json',
    'initial_lineage_sha256': sha((CRATE / 'provenance/RECIPE_LINEAGE.json').read_bytes()),
    'selection_receipt': 'provenance/successor-002/SELECTION.json',
    'selection_sha256': sha((CRATE / 'provenance/successor-002/SELECTION.json').read_bytes()),
    'adaptations': adaptations,
    'new_selection_tool': {'path': 'select_supported_successor.py',
                           'sha256': sha((CRATE / 'select_supported_successor.py').read_bytes())},
    'algorithm_change': 'Retain all existing gates; add exact inactive CSV admission-refusal regression and seal successor-selection tool in SDK source inputs.',
    'shared_engine_changes': False, 'active_target_candidate_changes': False,
    'sdk001': {'path': 'proof/sdk-001-offline/RESULT.json', 'status': 'FAIL',
               'sha256': sha((CRATE / 'proof/sdk-001-offline/RESULT.json').read_bytes())},
    'sdk002': {'path': 'proof/sdk-002-offline/RESULT.json', 'status': 'PASS',
               'sha256': sha((CRATE / 'proof/sdk-002-offline/RESULT.json').read_bytes())}}
put(CRATE / 'provenance/successor-002/LINEAGE.json', (json.dumps(lineage, indent=2) + '\n').encode())
manifest = {'schema': 'M3_MAVEN4_EVIDENCE_INTAKE/1', 'scope': 'Preserved authoring/source-build/recipe/copier evidence; no actual receiver runtime promotion',
            'files': RECORDS, 'count': len(RECORDS),
            'compiled_payloads_copied': False, 'all_source_and_destination_readbacks_match': True}
put(CRATE / 'provenance/EVIDENCE_INTAKE.json', (json.dumps(manifest, indent=2) + '\n').encode())
print(json.dumps({'status': 'PASS', 'files': len(RECORDS),
                  'bytes': sum(row['bytes'] for row in RECORDS),
                  'manifest_sha256': sha((CRATE / 'provenance/EVIDENCE_INTAKE.json').read_bytes())}))
