#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Append exact actual-receiver text receipts without changing the earlier intake."""
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[2]
CRATE = ROOT / 'synexia/synexia-openrewrite-recipes/recipe-crates/m3-maven4-effective-20261021'
records = []


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


def copy(source, relative):
    assert source.is_file() and not source.is_symlink()
    data = source.read_bytes()
    data.decode('utf-8', errors='strict')
    assert re.search(rb'(?:ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,}|Authorization:\s*Bearer\s+\S+)', data) is None, source
    put(CRATE / relative, data)
    assert source.read_bytes() == data
    records.append({'source_path': str(source), 'crate_path': relative,
                    'bytes': len(data), 'sha256': sha(data)})


proof = ROOT / 'maven-work/qualified-receiver-001/PROOF.json'
assert sha(proof.read_bytes()) == '9b1e3a81d5c37fc36bef68b4ae95554a76ccaf55a0f9464346b920a3fc09e107'
assert json.loads(proof.read_bytes())['status'] == 'PASS'
for source_folder, destination_folder in (
    ('integration-work/maven4-receiver-001', 'proof/receiving-application-001'),
    ('maven-work/qualified-receiver-001', 'proof/receiver-runtime-001')):
    for source in sorted((ROOT / source_folder).iterdir()):
        if source.is_file() and source.suffix in ('.json', '.log', '.txt', '.xml'):
            copy(source, destination_folder + '/' + source.name)
copy(ROOT / 'maven-work/evidence/QUALIFIER_RECEIVER_001.log',
     'proof/receiver-runtime-001/QUALIFIER_RECEIVER_001.log')
copy(Path(__file__), 'provenance/qualification-tools/append_receiver_evidence.py')
manifest = {'schema': 'M3_MAVEN4_RECEIVER_EVIDENCE_INTAKE/1', 'status': 'PASS',
            'earlier_intake_unchanged_sha256': sha((CRATE / 'provenance/EVIDENCE_INTAKE.json').read_bytes()),
            'files': records, 'count': len(records), 'compiled_payloads_copied': False,
            'all_source_and_destination_readbacks_match': True}
put(CRATE / 'provenance/RECEIVER_EVIDENCE_INTAKE.json', (json.dumps(manifest, indent=2) + '\n').encode())
print(json.dumps({'status': 'PASS', 'files': len(records),
                  'bytes': sum(row['bytes'] for row in records),
                  'manifest_sha256': sha((CRATE / 'provenance/RECEIVER_EVIDENCE_INTAKE.json').read_bytes())}))
