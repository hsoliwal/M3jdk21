#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify the existing closed memory helper for one current-owner correction."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys

ROOT = Path('/workspace/scratch/206049dfb270')
CANON = ROOT / 'synexia'
MODULE = CANON / 'synexia-openrewrite-recipes'
CORE = MODULE / 'recipe-crates/m3-integrated-refinement-20261021'
TASKS = {'swt-current-owner-runtime-closure-20261021', 'm3-nlp-current-receiver-lock-20261021'}
task = sys.argv[1]
assert task in TASKS
crate = MODULE / 'recipe-crates' / task
memory = crate / 'memory'
profile = task + '/1.0'
java_home = Path('/tmp/m3-build-tools/jdk-21.0.12.1+1')
jackson = Path('/tmp/m3-build-m2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar')
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
expected = {
    'com/synexia/rewrite/memory/M3RecoveryMemory.java': 'e6352756fca8abcea531761b2f32eb76a9e98bf3f259950d558ccc46072a0c32',
    'com/synexia/rewrite/memory/M3RecoveryMemoryTest.java': '4406ab706ba83c9dcd8618358477c6fd9094108c00bc62dd10735c4d181702b1',
    'com/synexia/rewrite/sealed/SealHash.java': '17910b8b92573d6673be22b1d79d3db81c2c65fdff02251b0d4b8defd5b7373a'}


def put(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x') as stream:
        json.dump(value, stream, indent=2)
        stream.write('\n')


def ref(path):
    return dict(path=path.relative_to(crate).as_posix(), sha256=sha(path))


assert crate.is_dir() and not memory.exists()
rows = []
for relative, digest in expected.items():
    source = CORE / 'src/test/java' / relative
    assert sha(source) == digest
    original = source.read_text()
    current = original.replace('m3-integrated-refinement-20261021/1.0', profile)
    current = current.replace('distinct pending-only profile for the new integrated packet.',
        'distinct pending-only profile for this current-owner correction; derived from published core #12648.')
    target = memory / 'tooling/src' / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open('x') as stream:
        stream.write(current)
    rows.append(dict(source_path=source.relative_to(CANON).as_posix(), source_sha256=digest,
        source_revision='a2b50cfe926c5c5f400c7f659a947a5020fb8d4d',
        path=target.relative_to(crate).as_posix(), sha256=sha(target),
        modification='Only profile identity and dated comment' if original != current else 'Exact unchanged source'))
classes = memory / 'tooling/target/classes'
classes.mkdir(parents=True, exist_ok=False)
proof = crate / 'proof/memory-conformance-001'
proof.mkdir(parents=True, exist_ok=False)
classpath = str(classes) + os.pathsep + str(jackson)
commands = [
    [str(java_home / 'bin/javac'), '--release', '21', '-Xlint:all', '-Werror', '-cp', str(jackson),
     '-d', str(classes), *[str(crate / r['path']) for r in rows]],
    [str(java_home / 'bin/java'), '-ea', '-Xmx512m', '-XX:ActiveProcessorCount=2', '-XX:-UsePerfData',
     '-cp', classpath, 'com.synexia.rewrite.memory.M3RecoveryMemoryTest']]
put(proof / 'INPUTS_AND_COMMANDS.json', dict(schema='M3_CURRENT_OWNER_MEMORY_PROFILE_INPUTS/1',
    profile=profile, sources=rows, commands=commands, cwd=str(ROOT),
    java_release_sha256=sha(java_home / 'release'), jackson_sha256=sha(jackson),
    source_receipt_before_execution=True, recipe_source_or_templates_modified=False))
results = []
for label, command in zip(('compile', 'conformance'), commands):
    result = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, timeout=60)
    log = proof / (label + '.log')
    with log.open('x') as stream:
        stream.write(result.stdout)
    results.append(dict(label=label, command=command, exit_code=result.returncode, log=ref(log)))
    if result.returncode:
        put(proof / 'RESULT.json', dict(status='FAIL', profile=profile, commands=results,
            input_receipt=ref(proof / 'INPUTS_AND_COMMANDS.json')))
        raise SystemExit(result.returncode)
assert 'PASS checks=129 controlCases=32 profile=' + profile in (proof / 'conformance.log').read_text()
for row in rows:
    assert sha(crate / row['path']) == row['sha256']
put(proof / 'RESULT.json', dict(schema='M3_CURRENT_OWNER_MEMORY_PROFILE_CONFORMANCE/1', status='PASS',
    profile=profile, checks=129, control_cases=32, sources_unchanged=True,
    input_receipt=ref(proof / 'INPUTS_AND_COMMANDS.json'), commands=results,
    scope='Separate direct Java21 closed-profile gate; no SDK test-count inflation or product admission.'))
put(memory / 'HELPER_LINEAGE.json', dict(schema='M3_CURRENT_OWNER_MEMORY_HELPER_LINEAGE/1',
    profile=profile, source_repository='hsoliwal/com.synexia',
    source_revision='a2b50cfe926c5c5f400c7f659a947a5020fb8d4d',
    source_pull_request='https://github.com/hsoliwal/com.synexia/pull/12648',
    sources=rows, inherited_task_events=False,
    event_admission='Closed to RECORDED/PENDING; no validated or product-admitted events.'))
profile_text = '''<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Current-owner correction memory profile

Profile `{profile}` is closed to `RECORDED/PENDING`. The helper is derived from
published core #12648 with only its profile identity and dated comment changed.
The independent golden-byte conformance test and SHA-256 helper remain exact.
No previous task event history is inherited.

The canonical encoding uses recursively sorted ASCII keys, strict UTF-8 scalar
strings, signed int64 integers and lowercase control escapes. Ordinary arrays
retain order; parent/supersedes digest sets must be sorted and unique. Unknown or
duplicate keys, floats, surrogates, noncanonical storage, invalid history and
missing/mismatched artifact references refuse. Event bytes have no BOM, optional
whitespace or trailing newline. SHA-256 names each create-new immutable event.

The separate actual Java21 helper gate passed 129 conformance checks, including
32 control cases. This tooling is outside the product and recipe runtime. Event
integrity is distinct from runtime behavior, publication and product admission.
'''.format(profile=profile)
with (memory / 'PROFILE.md').open('x') as stream:
    stream.write(profile_text)
with (memory / 'initialize_memory.py').open('xb') as stream:
    stream.write(Path(__file__).read_bytes())
print(json.dumps(dict(status='PASS', profile=profile, conformance_checks=129, control_cases=32,
                      event_count=0, receipt=str(proof / 'RESULT.json')), indent=2))
