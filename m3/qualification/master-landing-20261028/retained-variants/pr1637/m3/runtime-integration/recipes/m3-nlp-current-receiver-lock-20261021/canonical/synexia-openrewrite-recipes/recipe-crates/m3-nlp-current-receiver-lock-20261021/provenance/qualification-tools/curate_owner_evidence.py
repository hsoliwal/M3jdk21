#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Copy a bounded, fully sealed NLP owner handoff into its canonical task."""
from pathlib import Path
import argparse
import hashlib
import json

ROOT = Path('/workspace/scratch/206049dfb270')
CRATE = ROOT / 'synexia/synexia-openrewrite-recipes/recipe-crates/m3-nlp-current-receiver-lock-20261021'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def new(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as stream:
        stream.write(data)
    assert path.read_bytes() == data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('label', choices=['owner-candidate-001', 'receiver-runtime-001'])
    parser.add_argument('handoff', type=Path)
    parser.add_argument('sha256')
    args = parser.parse_args()
    raw = args.handoff.read_bytes()
    assert sha(raw) == args.sha256
    rows = json.loads(raw)
    assert isinstance(rows, list) and 1 <= len(rows) <= 300
    prepared = []
    seen = set()
    for row in rows:
        relative = row['relative_path']
        p = Path(relative)
        assert not p.is_absolute() and p.as_posix() == relative
        assert all(part not in ('', '.', '..') for part in relative.split('/'))
        assert '\\' not in relative and relative not in seen
        seen.add(relative)
        source = Path(row['source_path'])
        assert (source.is_relative_to(ROOT / 'nlp-work') or source == ROOT / 'jobs-work/nlp-lineage-readonly/HANDOFF.json') and not source.is_symlink()
        assert source.suffix not in ('.class', '.pyc', '.jar', '.jmod', '.so', '.dll', '.dylib')
        data = source.read_bytes()
        data.decode('utf-8', errors='strict')
        assert len(data) == row['bytes'] and sha(data) == row['sha256'], source
        target = CRATE / 'proof' / args.label / relative
        assert not target.exists(), target
        prepared.append((source, target, data, row))
    readbacks = []
    for source, target, data, row in prepared:
        new(target, data)
        readbacks.append({'source_path': str(source), 'path': target.relative_to(CRATE).as_posix(),
                          'bytes': len(data), 'sha256': row['sha256']})
    new(CRATE / 'provenance/intake' / (args.label + '-HANDOFF.json'), raw)
    receipt = {'schema': 'M3_NLP_OWNER_EVIDENCE_INTAKE/1', 'label': args.label,
        'status': 'EXACT_READBACK', 'source_handoff': str(args.handoff), 'handoff_sha256': args.sha256,
        'copied_files': len(readbacks), 'bytes': sum(r['bytes'] for r in readbacks), 'files': readbacks,
        'scope': 'Exact text/source/proof custody only; no binary payload or receiving source writes.'}
    new(CRATE / 'provenance' / (args.label + '-INTAKE.json'), (json.dumps(receipt, indent=2) + '\n').encode())
    print(json.dumps({k: receipt[k] for k in ['status', 'copied_files', 'bytes', 'handoff_sha256']}))


if __name__ == '__main__':
    main()
