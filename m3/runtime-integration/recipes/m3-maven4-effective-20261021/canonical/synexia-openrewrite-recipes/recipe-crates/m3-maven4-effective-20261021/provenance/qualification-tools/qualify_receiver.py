#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Exercise the existing proof-checked copier against isolated exact fixtures.

Adapted 2026-10-21 from canonical-work/qualify_receiver.py for the separate
Maven4 packet. The seven existing guard trials and their algorithms are retained.
The inactive historical catalogue is also required to retain its original bytes.
"""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path('/workspace/scratch/206049dfb270')
CRATE = ROOT / 'synexia/synexia-openrewrite-recipes/recipe-crates/m3-maven4-effective-20261021'
SDK = CRATE / 'proof/sdk-002-offline'
OUT = ROOT / 'jobs-work/maven4-recipe/receiver-qualification-002'
OUT.mkdir(exist_ok=False)
SOURCE = OUT / 'canonical'
shutil.copytree(SDK / 'inputs', SOURCE)
LOCAL = SOURCE / 'synexia-openrewrite-recipes/recipe-crates/m3-maven4-effective-20261021'
TARGET = LOCAL / 'target'
TARGET.mkdir()
for name in ('recipe-proof.tsv', 'proof-status.tsv', 'named-recipe.yml'):
    shutil.copyfile(SDK / name, TARGET / name)
for name in ('phase-receipts', 'receiver'):
    shutil.copytree(SDK / name, TARGET / name)
SEALS = json.loads((LOCAL / 'SOURCE_SEALS.json').read_text())['files']
STEPS = []


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inventory(root):
    return {p.relative_to(root).as_posix(): sha(p)
            for p in sorted(root.rglob('*')) if p.is_file() and not p.is_symlink()}


def baseline(name):
    destination = OUT / name
    shutil.copytree(LOCAL / 'preimages', destination)
    return destination


def run(label, receiver, expected_exit, apply=False, expected_writes=None, signature=None):
    command = ['python3', '-B', str(LOCAL / 'apply_receiver.py'), str(receiver)]
    if apply:
        command.append('--apply')
    before = inventory(receiver)
    result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, timeout=30)
    log = OUT / (label + '.log')
    log.write_text(result.stdout)
    receipt_name = 'application-receipt.json' if apply else 'application-check.json'
    receipt = OUT / (label + '.json')
    shutil.copyfile(TARGET / receipt_name, receipt)
    after = inventory(receiver)
    assert result.returncode == expected_exit, result.stdout
    if signature:
        assert signature in result.stdout, result.stdout
    if expected_exit:
        assert before == after, 'refusal changed fixture content'
    elif expected_writes is not None:
        observed = json.loads(result.stdout)
        assert observed['files'] == len(SEALS) and observed['writes'] == expected_writes, observed
    STEPS.append({'name': label, 'command': command, 'exit_code': result.returncode,
                  'log_sha256': sha(log), 'receipt_sha256': sha(receipt),
                  'before': before, 'after': after})


clean = baseline('receiving')
changed = sum(row['before_sha256'] != row['after_sha256'] for row in SEALS)
run('01-preflight', clean, 0, expected_writes=changed)
run('02-first-apply', clean, 0, apply=True, expected_writes=changed)
for row in SEALS:
    assert sha(clean / row['target_path']) == row['after_sha256']
run('03-fixed-point', clean, 0, apply=True, expected_writes=0)

drift = baseline('receiving-drift')
chosen = next(row['target_path'] for row in SEALS if row['before_sha256'] != 'ABSENT')
(drift / chosen).write_bytes((drift / chosen).read_bytes() + b'\n# intentional isolated refusal fixture\n')
run('04-product-drift', drift, 1, apply=True, signature='product source drift:')

symlinked = baseline('receiving-symbolic')
outside = OUT / 'unchanged-symbolic-owner'
outside.mkdir()
shutil.move(str(symlinked / '.github'), str(outside / '.github'))
(symlinked / '.github').symlink_to(outside / '.github', target_is_directory=True)
external_before = inventory(outside)
run('05-symbolic-destination', symlinked, 1, apply=True, signature='symbolic path refused:')
assert inventory(outside) == external_before

payload = TARGET / 'receiver' / SEALS[0]['target_path']
original = payload.read_bytes()
payload.write_bytes(original + b'\n# intentional isolated emitted-output drift\n')
run('06-emitted-drift', baseline('receiving-emitted-drift'), 1, apply=True,
    signature='emitted output drift:')
payload.write_bytes(original)

owner = SOURCE / 'synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3EditScope.java'
original = owner.read_bytes()
owner.write_bytes(original + b'\n// intentional isolated canonical-owner drift\n')
run('07-owner-drift', baseline('receiving-owner-drift'), 1, apply=True,
    signature='qualification input drift: M3EditScope.java')
owner.write_bytes(original)

catalogue = 'm3/tooling/module-packs/parser/real-providers/CATALOGUE.csv'
assert sha(clean / catalogue) == '6c3f7c2015abfde1e07006ae4175b4494da941ea10c20df25fb0cf32fbcf49e1'

receipt = {'schema': 'M3_MAVEN4_COPIER_QUALIFICATION/1', 'status': 'PASS',
           'scope': 'isolated exact product-source fixtures; no target repository writes',
           'sdk_receipt_sha256': sha(SDK / 'RESULT.json'),
           'script_sha256': sha(Path(__file__)), 'source_seals_sha256': sha(LOCAL / 'SOURCE_SEALS.json'),
           'target_paths': len(SEALS), 'first_writes': changed, 'replay_writes': 0,
           'refusals': 4, 'historical_catalogue_unchanged_sha256': sha(clean / catalogue), 'steps': STEPS}
(OUT / 'RESULT.json').write_text(json.dumps(receipt, indent=2) + '\n')
print(json.dumps({key: receipt[key] for key in ('status', 'target_paths', 'first_writes', 'replay_writes', 'refusals')}))
