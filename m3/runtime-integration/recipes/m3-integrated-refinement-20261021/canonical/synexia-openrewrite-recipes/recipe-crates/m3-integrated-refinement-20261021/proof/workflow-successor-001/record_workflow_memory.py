#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Append the actual framework workflow correction and a new memory checkpoint."""
import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path('/workspace/scratch/206049dfb270')
CRATE = ROOT / 'synexia/synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021'
JAVA = Path('/tmp/m3-build-tools/jdk-21.0.12.1+1/bin/java')
CLASSES = CRATE / 'proof/memory-conformance-001/classes'
JACKSON = Path('/tmp/m3-build-m2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar')
OBS = CRATE / 'proof/workflow-successor-001'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def artifact(path):
    path = Path(path)
    if not path.is_absolute():
        path = CRATE / path
    return {'path': path.relative_to(CRATE).as_posix(), 'sha256': sha(path)}


def put(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def copy(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    assert not destination.exists(), destination
    shutil.copyfile(source, destination)
    assert sha(source) == sha(destination)


for trial in ('ci-classpath-01', 'ci-classpath-plus-core-02'):
    source = ROOT / 'jobs-work/final-review' / trial
    dest = OBS / trial
    for name in ('RESULT.json', 'INPUTS_AND_COMMAND.json', 'maven.log', 'EXIT.json',
                 'classpath.txt', 'recipe-proof.tsv', 'proof-status.tsv', 'named-recipe.yml'):
        if (source / name).is_file():
            copy(source / name, dest / name)
    reports = source / 'canonical/synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021/target/surefire-reports'
    for path in sorted(reports.iterdir()):
        if path.is_file():
            copy(path, dest / 'surefire-reports' / path.name)
    receipt = json.loads((dest / 'RESULT.json').read_text())
    assert receipt['input_receipt_sha256'] == sha(dest / 'INPUTS_AND_COMMAND.json')
    assert receipt['log_sha256'] == sha(dest / 'maven.log')

sdk = CRATE / 'proof/sdk-003-workflow-successor-offline'
sdk_receipt = json.loads((sdk / 'RESULT.json').read_text())
assert sdk_receipt['status'] == 'PASS'
old = CRATE / 'proof/sdk-002-offline'
workflow = '.github/workflows/m3-eclipse-framework-forward-20261017.yml'
changed = []
unchanged = []
for path in sorted((old / 'receiver').rglob('*')):
    if path.is_file():
        relative = path.relative_to(old / 'receiver')
        record = {'path': relative.as_posix(), 'before_sha256': sha(path),
                  'after_sha256': sha(sdk / 'receiver' / relative)}
        (unchanged if record['before_sha256'] == record['after_sha256'] else changed).append(record)
assert len(unchanged) == 47 and [row['path'] for row in changed] == [workflow]
put(OBS / 'SOURCE_READBACK.json', {'schema': 'M3_SUCCESSOR_SOURCE_COMPARISON/1',
    'scope': 'SDK002 versus SDK003 emitted source readback, not a rerun of receiving runtime gates',
    'changed': changed, 'unchanged': unchanged,
    'old_sdk': artifact(old / 'RESULT.json'), 'new_sdk': artifact(sdk / 'RESULT.json')})

index = json.loads((CRATE / 'memory/INDEX.json').read_text())
event = {
    'schema': 'MOP-RCM/1.0', 'profile': 'm3-integrated-refinement-20261021/1.0',
    'recipe_id': 'com.synexia.m3.IntegratedRefinement20261021',
    'event_type': 'RECORDED', 'admission': 'PENDING',
    'source': {'repository': 'hsoliwal/com.synexia', 'revision': 'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07'},
    'parents': [index['events'][-1]], 'supersedes': [],
    'recipe_before': artifact(old / 'named-recipe.yml'),
    'recipe_after': artifact(sdk / 'named-recipe.yml'),
    'source_manifest_before': artifact(old / 'inputs/synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021/SOURCE_SEALS.json'),
    'source_manifest_after': artifact(sdk / 'inputs/synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021/SOURCE_SEALS.json'),
    'observation_epoch_seconds': int(datetime.datetime.now(datetime.timezone.utc).timestamp()),
    'author': 'Hitesh Soliwal and Contributors to the Synexia Project', 'license': 'Apache-2.0',
    'trigger': 'Final local reproduction of the emitted framework workflow failed the actual-owner attribution check although SDK001/002 passed with a complete local classpath.',
    'root_cause': {'status': 'observed', 'explanation': 'The workflow supplied real collections and framework classes and Eclipse JARs, but omitted the existing core-runtime progress facade owner required by the unchanged five-method attribution probe.'},
    'change': 'Add the existing unchanged core-runtime POM verify, its target/classes and its reports to the existing workflow. Append immutable phase049 with the exact SDK002 workflow as preimage; retain inactive phase002 and all original qualification snapshots.',
    'lesson': 'Replay the exact CI dependency closure. Preserve parser attribution requirements and compile the actual missing owner; a broader successful local classpath does not qualify a narrower hosted workflow.',
    'known_failures': ['ci-classpath-01: missing actual owner attribution: com.m3.eclipse.runtime.M3EclipseProgressMonitor#borrowed.',
                       'The first local qualifier launch supplied a colon-joined classpath where its wrapper requires one path per line; it refused before Maven or attempt creation. The exact unchanged members were normalized into a separate input file.'],
    'atoms': [{'id': 'actual-owner-ci-classpath-closure', 'owner': 'Existing framework workflow and core-runtime Maven module',
               'contract': 'The sealed attribution probe resolves all five actual owner methods before source transformation.',
               'pattern': 'Build the existing owner and add its real compiled output to the parser classpath; keep the oracle and engines unchanged.',
               'applicability': ['The retained current framework workflow and the exact source-sealed integrated recipe.'],
               'exclusions': ['No synthetic stand-in, engine widening, runtime owner edit, new workflow, or hosted CI success claim.']}],
    'regression_fixtures': [artifact(old / 'receiver' / workflow), artifact(sdk / 'receiver' / workflow),
                            artifact(OBS / 'SOURCE_READBACK.json'), artifact(OBS / 'INVOCATION_FORMAT_REFUSAL.json')],
    'verification': [],
    'replay': {'status': 'PASS', 'command': sdk_receipt['command'],
               'evidence': [artifact(sdk / 'RESULT.json'), artifact(sdk / 'maven.log')]},
    'environment': 'Actual Microsoft OpenJDK21.0.12.1+1, Maven3.9.9, Linux; offline cached artifacts and exact compiled owner inventories.',
    'applicability': ['Two-stage reproduction: retained pristine2779 SDK002 stage, then current phase049 successor; the final target diff remains based on2779.'],
    'exclusions': ['No whole-JDK, source-vendored reactor, OSGi product, native UI or unrestricted equivalence admission.'],
    'pending_gates': ['Hosted Actions and required admission/review gates remain unexecuted.'],
    'unknowns': ['No hosted CI job ran in this local reproduction.',
                 'The invocation-format refusal receipt is explicitly a retrospective exact transcription from the session tool output, not a pre-execution JSON artifact.']}
for trial, status in (('ci-classpath-01', 'FAIL'), ('ci-classpath-plus-core-02', 'PASS')):
    dest = OBS / trial
    inputs = json.loads((dest / 'INPUTS_AND_COMMAND.json').read_text())
    event['verification'].append({'gate': trial, 'status': status, 'command': inputs['argv'],
        'evidence': [artifact(dest / 'INPUTS_AND_COMMAND.json'), artifact(dest / 'RESULT.json'), artifact(dest / 'maven.log')]})
event['verification'].append({'gate': 'actual_successor_offline_sdk', 'status': 'PASS',
    'command': sdk_receipt['command'], 'evidence': [artifact(sdk / 'RESULT.json'), artifact(sdk / 'maven.log')]})
draft = ROOT / 'canonical-work/memory-drafts/006.json'
put(draft, event)
command = [str(JAVA), '-cp', str(CLASSES) + ':' + str(JACKSON),
           'com.synexia.rewrite.memory.M3RecoveryMemory', 'publish', str(draft), str(CRATE), str(CRATE / 'memory/events')]
result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=30)
with (CRATE / 'proof/memory-publication-006.log').open('x') as stream:
    stream.write(result.stdout)
assert result.returncode == 0, result.stdout
published = Path(result.stdout.split('MOP_MEMORY_PUBLISH ', 1)[1].split(' admission=', 1)[0])
assert sha(published) == published.stem
index['events'].append(published.stem)
index['predecessor_checkpoint'] = artifact('memory/FINAL_RESULT.json')
index['follow_up_receipts'].append(artifact(sdk / 'RESULT.json'))
put(CRATE / 'memory/INDEX-002.json', index)
lessons = (CRATE / 'memory/LESSONS.md').read_text()
lessons += '\n## 006 — Reproduce the actual CI classpath\n\nThe exact framework workflow lacked an existing progress facade in its parser\nclasspath. Its real missing-owner failure, unchanged-source comparison, narrow\nworkflow correction and actual successor SDK replay are retained under\n`proof/workflow-successor-001` and `proof/sdk-003-workflow-successor-offline`.\nCompile the existing owner; preserve the attribution oracle. Phase002 remains\nimmutable and phase049 requires its exact emitted workflow. From pristine2779,\nexecute the retained SDK002 stage before the current successor. The first local\nwrapper launch also refused a colon-joined inventory; only its input formatting\nwas corrected into a new file. No runtime owner or recipe engine changed.\n\nThe original five-event INDEX/LESSONS/FINAL_RESULT remain immutable. This\nsix-event successor is indexed by INDEX-002.json and FINAL_RESULT-002.json.\n'
with (CRATE / 'memory/LESSONS-002.md').open('x') as stream:
    stream.write(lessons)
integrity = CRATE / 'proof/memory-integrity-002'
integrity.mkdir(exist_ok=False)
checks = []
references = {}
for digest in index['events']:
    path = CRATE / 'memory/events' / (digest + '.json')
    cmd = [str(JAVA), '-cp', str(CLASSES) + ':' + str(JACKSON),
           'com.synexia.rewrite.memory.M3RecoveryMemory', 'check', str(path), str(CRATE), str(CRATE / 'memory/events')]
    checked = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=30)
    log = integrity / (digest + '.log')
    with log.open('x') as stream:
        stream.write(checked.stdout)
    assert checked.returncode == 0, checked.stdout
    checks.append({'event': artifact(path), 'command': cmd, 'exit_code': checked.returncode, 'log': artifact(log)})
    def walk(value):
        if isinstance(value, dict):
            if set(value) == {'path', 'sha256'}:
                key = value['path']
                assert sha(CRATE / key) == value['sha256']
                if key in references:
                    assert references[key] == value['sha256']
                references[key] = value['sha256']
            for child in value.values(): walk(child)
        elif isinstance(value, list):
            for child in value: walk(child)
    walk(json.loads(path.read_text()))
put(integrity / 'RESULT.json', {'status': 'PASS', 'checks': checks,
    'unique_event_artifacts_verified': len(references), 'admission': 'PENDING'})
final = {'schema': 'M3_INTEGRATED_CORRECTION_MEMORY_FINAL/1', 'status': 'PASS',
         'profile': event['profile'], 'events': index['events'], 'event_count': len(index['events']),
         'conformance_checks': 129, 'actual_event_integrity_checks': len(checks),
         'unique_event_artifacts_verified': len(references), 'admission': 'PENDING',
         'predecessor_checkpoint': artifact('memory/FINAL_RESULT.json'),
         'profile_receipt': artifact('memory/PROFILE.md'), 'retrieval': artifact('memory/LESSONS-002.md'),
         'index': artifact('memory/INDEX-002.json'),
         'verification': [artifact(sdk / 'RESULT.json'), artifact(integrity / 'RESULT.json')],
         'scope': 'Appended sixth correction event; original five-event checkpoint and all original test/source snapshots remain byte-identical.',
         'publication': 'Separate current-base provenance and remote state; no product admission granted.'}
put(CRATE / 'memory/FINAL_RESULT-002.json', final)
with (CRATE / 'memory/README.md').open('x') as stream:
    stream.write('<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->\n<!-- SPDX-License-Identifier: Apache-2.0 -->\n\n# Correction memory retrieval\n\nRead [LESSONS-002.md](LESSONS-002.md), [INDEX-002.json](INDEX-002.json) and\n[FINAL_RESULT-002.json](FINAL_RESULT-002.json) for the current six-event checkpoint.\nThe original five-event `LESSONS.md`, `INDEX.json` and `FINAL_RESULT.json` are\nretained unchanged. Every event remains RECORDED with admission PENDING.\n')
copy(Path(__file__), OBS / 'record_workflow_memory.py')
print(json.dumps({'event': published.stem, 'events': len(checks), 'artifacts': len(references),
                  'receipt': str(CRATE / 'memory/FINAL_RESULT-002.json'),
                  'sha256': sha(CRATE / 'memory/FINAL_RESULT-002.json')}))
