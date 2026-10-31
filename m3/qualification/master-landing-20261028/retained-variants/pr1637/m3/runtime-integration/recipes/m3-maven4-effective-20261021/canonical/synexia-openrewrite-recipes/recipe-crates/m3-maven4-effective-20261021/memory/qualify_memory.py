#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Independently qualify the fresh Maven4 pending-only correction profile."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time

ROOT = Path('/workspace/scratch/206049dfb270')
MODULE = ROOT / 'synexia/synexia-openrewrite-recipes'
CORE = MODULE / 'recipe-crates/m3-integrated-refinement-20261021'
CRATE = MODULE / 'recipe-crates/m3-maven4-effective-20261021'
JAVA_HOME = Path('/tmp/m3-build-tools/jdk-21.0.12.1+1')
JACKSON = Path('/tmp/m3-build-m2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar')


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def put(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def artifact(relative):
    path = CRATE / relative
    return {'path': relative, 'sha256': sha(path)}


assert sha(CRATE / 'SOURCE_SEALS.json') == 'fb401eeb0467c16ffa4d95d8de2a6c539775f096db81a7a7ac4fc717ad6e74b9'
passed = json.loads((CRATE / 'proof/sdk-002-offline/RESULT.json').read_text())
failed = json.loads((CRATE / 'proof/sdk-001-offline/RESULT.json').read_text())
assert passed['status'] == 'PASS' and failed['status'] == 'FAIL'
inputs = json.loads((CRATE / 'proof/sdk-002-offline/SOURCE_INPUTS.json').read_text())
canonical = MODULE.parent
for row in inputs:
    assert sha(canonical / row['path']) == row['sha256'], row['path']
memory = CRATE / 'memory'
memory.mkdir(exist_ok=False)
source_rows = []
for relative in ('com/synexia/rewrite/memory/M3RecoveryMemory.java',
                 'com/synexia/rewrite/memory/M3RecoveryMemoryTest.java',
                 'com/synexia/rewrite/sealed/SealHash.java'):
    source = CORE / 'src/test/java' / relative
    target = memory / 'tooling/src' / relative
    original = source.read_text()
    current = original.replace('m3-integrated-refinement-20261021/1.0', 'm3-maven4-effective-20261021/1.0')
    current = current.replace('distinct pending-only profile for the new integrated packet.',
                              'distinct pending-only profile for the new Maven4 packet; derived from the qualified integrated helper.')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(current)
    source_rows.append({'source_path': source.relative_to(canonical).as_posix(),
        'source_sha256': sha(source), 'path': target.relative_to(CRATE).as_posix(), 'sha256': sha(target),
        'modification': 'Profile identity and dated comment only' if original != current else 'Exact unchanged source'})
proof = CRATE / 'proof/memory-conformance-001'
proof.mkdir(exist_ok=False)
classes = memory / 'tooling/target/classes'
classes.mkdir(parents=True, exist_ok=False)
sources = [str(CRATE / row['path']) for row in source_rows]
classpath = str(classes) + os.pathsep + str(JACKSON)
commands = [
    [str(JAVA_HOME / 'bin/javac'), '--release', '21', '-Xlint:all', '-Werror', '-cp', str(JACKSON), '-d', str(classes), *sources],
    [str(JAVA_HOME / 'bin/java'), '-ea', '-cp', classpath, 'com.synexia.rewrite.memory.M3RecoveryMemoryTest']]
put(proof / 'INPUTS_AND_COMMANDS.json', {'schema': 'M3_MEMORY_PROFILE_INPUTS/1',
    'profile': 'm3-maven4-effective-20261021/1.0', 'sources': source_rows, 'commands': commands,
    'cwd': str(ROOT), 'java_release_sha256': sha(JAVA_HOME / 'release'),
    'jackson_sha256': sha(JACKSON), 'sdk_source_inputs_modified': False})
results = []
for label, command in zip(('compile', 'conformance'), commands):
    result = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=60)
    log = proof / (label + '.log')
    with log.open('x') as stream:
        stream.write(result.stdout)
    results.append({'label': label, 'command': command, 'exit_code': result.returncode,
                    'log': artifact(log.relative_to(CRATE).as_posix())})
    assert result.returncode == 0, result.stdout
assert 'PASS checks=129 controlCases=32 profile=m3-maven4-effective-20261021/1.0' in (proof / 'conformance.log').read_text()
put(proof / 'RESULT.json', {'schema': 'M3_MEMORY_PROFILE_CONFORMANCE/1', 'status': 'PASS',
    'checks': 129, 'control_cases': 32, 'profile': 'm3-maven4-effective-20261021/1.0',
    'input_receipt': artifact('proof/memory-conformance-001/INPUTS_AND_COMMANDS.json'),
    'commands': results, 'scope': 'Separate direct Java21 helper gate; no Maven SDK test-count inflation or runtime admission.'})

old = 'proof/sdk-001-offline/inputs/'
new = 'proof/sdk-002-offline/inputs/'
crate_path = 'synexia-openrewrite-recipes/recipe-crates/m3-maven4-effective-20261021/'
yaml_path = 'synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3-maven4-effective-20261021.yml'
phase = 'synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-maven4-effective-20261021-002/'
event = {'schema': 'MOP-RCM/1.0', 'profile': 'm3-maven4-effective-20261021/1.0',
    'recipe_id': 'com.synexia.m3.Maven4Effective20261021', 'event_type': 'RECORDED', 'admission': 'PENDING',
    'source': {'repository': 'hsoliwal/com.synexia', 'revision': 'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07'},
    'parents': [], 'supersedes': [], 'recipe_before': artifact(old + yaml_path),
    'recipe_after': artifact(new + yaml_path),
    'source_manifest_before': artifact(old + crate_path + 'SOURCE_SEALS.json'),
    'source_manifest_after': artifact(new + crate_path + 'SOURCE_SEALS.json'),
    'observation_epoch_seconds': int(time.time()),
    'author': 'Hitesh Soliwal and Contributors to the Synexia Project', 'license': 'Apache-2.0',
    'trigger': 'Actual Maven4 SDK001 failed because the unchanged text engine refused a CSV target manifest row.',
    'root_cause': {'status': 'observed', 'explanation': 'The task preparation included an existing CATALOGUE.csv mutation, but CSV is outside the canonical text snapshot engine admission set.'},
    'change': 'Retain the original CSV target unchanged, preserve inactive phase002 and all failed inputs, and select the seven unchanged supported candidate paths. The already admitted README contains the complete category decisions. Add an exact refusal regression to the task harness; preserve shared engines.',
    'lesson': 'Respect the existing engine source-kind boundary. Preserve the original catalogue and store task rationale in an already admitted existing documentation owner; prove the refused candidate stays refused.',
    'known_failures': ['SDK001: one JUnit failure with invalid hash-pinned text manifest row; no emitted product output or target writes.'],
    'atoms': [{'id': 'csv-admission-and-existing-catalogue-preservation',
               'owner': 'M3HashPinnedTextSnapshotRecipe and the Maven4 task descriptor',
               'contract': 'Unsupported CSV manifests refuse; supported seven-path output remains exact and reaches fixed point.',
               'pattern': 'Keep the original CSV and inactive phase, select existing supported candidates, and retain the exact negative test alongside normal source qualification.',
               'applicability': ['This pinned Maven4 task and the current unchanged canonical text engine.'],
               'exclusions': ['No CSV rewrite feature, engine widening, catalogue replacement, unsupported-kind relabeling or product admission.']}],
    'regression_fixtures': [artifact(old + phase + 'manifest.tsv'), artifact(old + phase + 'postimage.txt'),
        artifact(new + crate_path + 'src/test/java/com/synexia/rewrite/M3Maven4EffectiveRecipeTest.java'),
        artifact('provenance/successor-002/SELECTION.json')],
    'verification': [
        {'gate': 'actual_sdk001_failure', 'status': 'FAIL', 'command': failed['command'],
         'evidence': [artifact('proof/sdk-001-offline/RESULT.json'), artifact('proof/sdk-001-offline/maven.log')]},
        {'gate': 'actual_sdk002_successor', 'status': 'PASS', 'command': passed['command'],
         'evidence': [artifact('proof/sdk-002-offline/RESULT.json'), artifact('proof/sdk-002-offline/maven.log')]},
        {'gate': 'separate_closed_memory_profile', 'status': 'PASS', 'command': commands[1],
         'evidence': [artifact('proof/memory-conformance-001/RESULT.json'), artifact('proof/memory-conformance-001/conformance.log')]}],
    'replay': {'status': 'PASS', 'command': passed['command'],
               'evidence': [artifact('proof/sdk-002-offline/RESULT.json')]},
    'environment': 'Actual Microsoft OpenJDK21.0.12.1+1, Maven3.9.9, OpenRewrite8.17.1; exact cached artifacts and source-qualified parser owners recorded per SDK attempt.',
    'applicability': ['Exact receiving2779 preimages; no first-stage output was applied, so the seven-path successor is directly applicable to pristine pinned receiving files.'],
    'exclusions': ['No inherited integrated-task event history, full-JDK/source-vendored reactor proof, full Maven distribution result or hosted Actions claim.'],
    'pending_gates': ['Actual receiving application/runtime gates and publication remain separate owner receipts.'],
    'unknowns': ['This is the first event for the fresh Maven4 task; no prior task-memory lineage is asserted.']}
put(memory / 'draft-001.json', event)
command = [str(JAVA_HOME / 'bin/java'), '-cp', classpath, 'com.synexia.rewrite.memory.M3RecoveryMemory',
           'publish', str(memory / 'draft-001.json'), str(CRATE), str(memory / 'events')]
result = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=30)
(proof / 'publish.log').write_text(result.stdout)
assert result.returncode == 0, result.stdout
published = Path(result.stdout.split('MOP_MEMORY_PUBLISH ', 1)[1].split(' admission=', 1)[0])
assert sha(published) == published.stem
check = [str(JAVA_HOME / 'bin/java'), '-cp', classpath, 'com.synexia.rewrite.memory.M3RecoveryMemory',
         'check', str(published), str(CRATE), str(memory / 'events')]
result = subprocess.run(check, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=30)
(proof / 'check.log').write_text(result.stdout)
assert result.returncode == 0, result.stdout
references = {}
def walk(value):
    if isinstance(value, dict):
        if set(value) == {'path', 'sha256'}:
            assert sha(CRATE / value['path']) == value['sha256']
            references[value['path']] = value['sha256']
        for child in value.values(): walk(child)
    elif isinstance(value, list):
        for child in value: walk(child)
walk(json.loads(published.read_text()))
profile = '''<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Fresh Maven4 MOP-RCM profile

Profile `m3-maven4-effective-20261021/1.0` is closed to RECORDED/PENDING. Its helper
reuses the qualified integrated-task implementation with only its profile string
and dated comment changed. The independent golden-byte tests and SealHash owner
remain exact. This is separate authoring/proof tooling under memory/tooling;
it is not a product or OpenRewrite runtime dependency.

Recursive sorted ASCII keys, strict UTF-8 scalars, signed int64 numbers, lowercase
control escapes, ordered arrays and sorted unique parent/digest sets are enforced.
Unknown fields, duplicate keys, floats, surrogates, noncanonical storage and
invalid/missing artifact or history pins refuse. Event bytes have no BOM,
optional whitespace or trailing newline. SHA-256 names each immutable event;
create-new publication refuses unsupported or differing existing output.

The separate actual Java21 conformance gate passed129 checks and32 control cases.
The Maven SDK002 result remains one aggregate JUnit test. Memory validation does
not prove runtime behavior, arbitrary command execution, publication or admission.
'''
(memory / 'PROFILE.md').write_text(profile)
(memory / 'LESSONS.md').write_text('''<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Maven4 correction retrieval

Read INDEX.json and FINAL_RESULT.json for this task’s first immutable event.
The actual SDK001 CSV admission failure is retained with its entire input tree.
SDK002 selects seven unchanged supported paths and explicitly confirms the old
CSV phase still refuses without emitted output. The original target catalogue
stays unchanged; existing README decisions retain task rationale. No shared
engine or source-kind rule was widened, and no integrated-task event is inherited.

Both SDK source readbacks remain intact. Receiving runtime execution and hosted
admission have their own receipts; this event remains RECORDED/PENDING.
''')
put(memory / 'INDEX.json', {'schema': 'M3_PENDING_CORRECTION_INDEX/1', 'events': [published.stem],
                          'admission': 'PENDING', 'conformance_receipt': artifact('proof/memory-conformance-001/RESULT.json')})
for row in inputs:
    assert sha(canonical / row['path']) == row['sha256'], row['path']
put(memory / 'FINAL_RESULT.json', {'schema': 'M3_MAVEN4_CORRECTION_MEMORY_FINAL/1', 'status': 'PASS',
    'profile': event['profile'], 'events': [published.stem], 'event_count': 1,
    'conformance_checks': 129, 'control_cases': 32, 'actual_event_integrity_checks': 1,
    'unique_event_artifacts_verified': len(references), 'admission': 'PENDING',
    'profile_receipt': artifact('memory/PROFILE.md'), 'retrieval': artifact('memory/LESSONS.md'),
    'index': artifact('memory/INDEX.json'), 'source_lineage': source_rows,
    'verification': [artifact('proof/memory-conformance-001/RESULT.json'), artifact('proof/memory-conformance-001/check.log')],
    'publish_command': command, 'check_command': check,
    'qualified_sdk_inputs_unchanged': True,
    'scope': 'Independent task-memory conformance/integrity only; no inherited event or runtime admission.'})
shutil.copyfile(Path(__file__), memory / 'qualify_memory.py')
print(json.dumps({'status': 'PASS', 'checks': 129, 'events': 1, 'artifacts': len(references),
                  'event': published.stem, 'receipt_sha256': sha(memory / 'FINAL_RESULT.json')}))
