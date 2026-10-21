#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026-10-21 from qualified integrated SDK003 for one SWT proof-POM correction.
"""Verify and install bytes emitted by the qualified canonical OpenRewrite recipe.

Adapted 2026-10-21 from PR12605, preserving its proof and receiver guards.
This copier performs no transformation. The Java/text recipe scheduler remains
the only transformation owner. Any source, proof, manifest or receiver drift
refuses before product writes. Reapplying an exact postimage is a no-op.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import stat
import tempfile

RECEIPT_PATH = None


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def safe(root: Path, relative: str) -> Path:
    path = Path(relative)
    if path.is_absolute() or path.as_posix() != relative or any(
            part in ('', '.', '..') for part in relative.split('/')):
        raise ValueError('non-canonical relative path: ' + relative)
    result = root / path
    for member in [result, *result.parents]:
        if member == root.parent:
            break
        if member.is_symlink():
            raise ValueError('symbolic path refused: ' + str(member))
    return result


def main() -> None:
    global RECEIPT_PATH
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('repository', type=Path)
    parser.add_argument('--apply', action='store_true', help='write verified receiver bytes')
    args = parser.parse_args()
    crate = Path(__file__).resolve().parent
    proof_dir = crate / 'target'
    proof_dir.mkdir(parents=True, exist_ok=True)
    RECEIPT_PATH = proof_dir / ('application-receipt.json' if args.apply else 'application-check.json')
    RECEIPT_PATH.write_text(json.dumps({'status': 'RUNNING',
                                      'mode': 'APPLY' if args.apply else 'CHECK'}) + '\n')
    root = args.repository.resolve(strict=True)
    proof_bytes = (proof_dir / 'recipe-proof.tsv').read_bytes()
    status = list(csv.DictReader((proof_dir / 'proof-status.tsv').read_text().splitlines(), delimiter='\t'))
    if status != [{'status': 'PASS', 'proof_sha256': sha(proof_bytes)}]:
        raise ValueError('incomplete or stale recipe qualification')
    proof = dict(csv.reader(proof_bytes.decode().splitlines()[1:], delimiter='\t'))
    if proof.get('status') != 'PASS':
        raise ValueError('recipe proof did not pass')
    checks = {
        'phases.tsv': crate / 'phases.tsv', 'pom.xml': crate / 'pom.xml',
        'test.java': crate / 'src/test/java/com/synexia/rewrite/M3SwtCurrentOwnerRuntimeClosureRecipeTest.java',
        'named_yaml_sha256': proof_dir / 'named-recipe.yml',
        'dependency_probe_sha256': crate / 'attribution/DependencyCustody.java',
        'dependency_expectations_sha256': crate / 'attribution/expected-methods.txt',
    }
    owners = crate / '../../src/main/java/com/synexia/rewrite'
    for name in ['M3EditScope.java', 'M3ScopeInference.java',
                 'M3HashPinnedJavaSnapshotRecipe.java', 'M3HashPinnedTextSnapshotRecipe.java']:
        checks[name] = owners / name
    for key, path in checks.items():
        if sha(path.read_bytes()) != proof[key]:
            raise ValueError('qualification input drift: ' + key)
    phases = list(csv.DictReader((crate / 'phases.tsv').read_text().splitlines(), delimiter='\t'))
    resources = crate / '../../src/main/resources/com/synexia/rewrite'
    targets: dict[str, tuple[str, str, bytes]] = {}
    for phase in phases:
        phase_id = phase['id']
        receipt = (proof_dir / 'phase-receipts' / (phase_id + '.tsv')).read_bytes()
        if sha(receipt) != proof['phase_receipt_' + phase_id]:
            raise ValueError('phase receipt drift: ' + phase_id)
        row = list(csv.DictReader(receipt.decode().splitlines(), delimiter='\t'))
        if len(row) != 1:
            raise ValueError('invalid phase receipt: ' + phase_id)
        manifest = resources / ('hash-pinned-' + phase['kind']) / phase_id / 'manifest.tsv'
        raw = manifest.read_bytes()
        if sha(raw) != row[0]['manifest_sha256']:
            raise ValueError('manifest drift: ' + phase_id)
        for line in raw.decode().splitlines():
            if not line or line.startswith('#'):
                continue
            path, before, after, template = line.split('\t')
            data = safe(proof_dir / 'receiver', path).read_bytes()
            if sha(data) != after or data != safe(manifest.parent, template).read_bytes():
                raise ValueError('emitted output drift: ' + path)
            if path in targets:
                raise ValueError('duplicate file owner: ' + path)
            targets[path] = before, after, data
    identity = ''.join(path + '\t' + targets[path][1] + '\n' for path in sorted(targets)).encode()
    if sha(identity) != proof['receiver_root']:
        raise ValueError('receiver root drift')
    plan = []
    for path, (before, after, data) in targets.items():
        destination = safe(root, path)
        actual = sha(destination.read_bytes()) if destination.exists() else 'ABSENT'
        if actual not in (before, after):
            raise ValueError('product source drift: ' + path)
        plan.append((path, destination, actual, after, data))
    records = []
    for path, destination, actual, after, data in plan:
        action = 'ALREADY_APPLIED' if actual == after else 'WRITE'
        if args.apply and action == 'WRITE':
            current = sha(destination.read_bytes()) if destination.exists() else 'ABSENT'
            if current != actual:
                raise ValueError('concurrent product change: ' + path)
            mode = stat.S_IMODE(destination.stat().st_mode) if destination.exists() else 0o644
            destination.parent.mkdir(parents=True, exist_ok=True)
            temporary = None
            try:
                with tempfile.NamedTemporaryFile(dir=destination.parent, delete=False) as stream:
                    temporary = Path(stream.name)
                    stream.write(data)
                temporary.chmod(mode)
                os.replace(temporary, destination)
            finally:
                if temporary is not None and temporary.exists():
                    temporary.unlink()
            if sha(destination.read_bytes()) != after:
                raise ValueError('written output drift: ' + path)
        records.append({'path': path, 'before_sha256': actual, 'after_sha256': after, 'action': action})
    receipt = {'status': 'PASS', 'mode': 'APPLY' if args.apply else 'CHECK',
               'recipe_proof_sha256': sha(proof_bytes), 'receiver_root': proof['receiver_root'],
               'repository': str(root), 'files': records}
    RECEIPT_PATH.write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({'status': 'PASS', 'mode': receipt['mode'], 'files': len(records),
                      'writes': sum(item['action'] == 'WRITE' for item in records)}))


if __name__ == '__main__':
    try:
        main()
    except BaseException as failure:
        if RECEIPT_PATH is not None:
            RECEIPT_PATH.write_text(json.dumps({'status': 'FAIL',
                'failure': type(failure).__name__ + ': ' + str(failure)}, indent=2) + '\n')
        raise
