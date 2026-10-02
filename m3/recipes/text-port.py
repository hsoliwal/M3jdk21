#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0
"""Validate/replay the selected private donor closure using raw Git pins and existing safety."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import apply as recipe

ROOT = Path(__file__).resolve().parents[2]
PORT = 'm3/ports/text'
def sha(data): return hashlib.sha256(data).hexdigest()
def load(root, path): return json.loads(recipe.safe_path(root, path).read_text(encoding='utf-8'))

def validate(root=ROOT, source=None):
    provenance = load(root, PORT + '/provenance.json')
    mappings = load(root, 'm3/docs/name-mapping.json')
    if mappings.get('schema') != 2 or mappings.get('mapping_schema') != 'mindex-to-m3/v2':
        raise ValueError('mapping schema drift')
    patch = recipe.safe_path(root, provenance['adapter_patch']).read_bytes()
    if sha(patch) != provenance['adapter_patch_sha256']: raise ValueError('adapter patch drift')
    ids = [row['mapping_id'] for row in mappings['mappings']]
    if len(set(ids)) != len(ids): raise ValueError('duplicate mapping identity')
    mapped = {endpoint['path']: (row, endpoint) for row in mappings['mappings']
              for endpoint in row['destinations']}
    blobs = {}
    for row in provenance['files']:
        path = row['target_path']
        if path not in mapped: raise ValueError('unmapped selected port: ' + path)
        mapping, endpoint = mapped[path]
        if not mapping['tests']['evidence'] or not mapping['recipe']['rollback']:
            raise ValueError('port without evidence/rollback: ' + path)
        for evidence in mapping['tests']['evidence']:
            if not recipe.safe_path(root, evidence).is_file(): raise ValueError('missing evidence: ' + evidence)
            proof = load(root, evidence)
            relative = path.removeprefix(PORT + '/')
            if (proof.get('source_hashes', {}).get(relative) != row['target_sha256'] or
                    not proof.get('records') or any(run.get('exit') != 0 for run in proof['records'])):
                raise ValueError('unbound candidate evidence: ' + path)
        actual = recipe.safe_path(root, path).read_bytes()
        if sha(actual) != row['target_sha256'] or sha(actual) != endpoint['sha256']:
            raise ValueError('target divergence: ' + path)
        if (mapping['sources'][0]['sha256'] != row['sha256'] or
                mapping['sources'][0]['commit'] != provenance['source_commit']):
            raise ValueError('source mapping drift: ' + path)
        if source is not None:
            blob = subprocess.check_output(['git', '-C', str(source), 'show',
                                             provenance['source_commit'] + ':' + row['source_path']])
            if sha(blob) != row['sha256']: raise ValueError('donor source drift: ' + path)
            blobs[path] = blob
    return provenance, blobs

def replay(root, source, destination):
    provenance, blobs = validate(root, source)
    # All source and destination checks precede effects. Reject directory aliases.
    for relative in blobs:
        target = recipe.safe_path(destination, relative)
        expected = recipe.safe_path(root, relative).read_bytes()
        if target.exists() and (not target.is_file() or target.read_bytes() != expected):
            raise ValueError('refusing modified destination: ' + relative)
    with tempfile.TemporaryDirectory(prefix='m3-text-port-') as scratch:
        staged = Path(scratch)
        for relative, blob in blobs.items():
            target = staged / relative; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(blob)
        patch = recipe.safe_path(root, provenance['adapter_patch']).resolve()
        for flags in [['--check'], []]:
            subprocess.run(['git', '-c', 'core.autocrlf=false', 'apply', *flags, str(patch)],
                           cwd=staged, check=True, capture_output=True)
        for row in provenance['files']:
            if sha((staged / row['target_path']).read_bytes()) != row['target_sha256']:
                raise ValueError('adapter postimage drift')
        for relative in blobs:
            target = recipe.safe_path(destination, relative)
            if not target.exists():
                target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes((staged / relative).read_bytes())
    return len(blobs)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path)
    parser.add_argument('--replay-to', type=Path)
    options = parser.parse_args()
    if options.replay_to:
        if not options.source_root: parser.error('--replay-to requires --source-root')
        count = replay(ROOT, options.source_root, options.replay_to.resolve())
    else:
        provenance, _ = validate(source=options.source_root); count = len(provenance['files'])
    print('M3_SELECTED_TEXT_PORT_PASS files=' + str(count))
