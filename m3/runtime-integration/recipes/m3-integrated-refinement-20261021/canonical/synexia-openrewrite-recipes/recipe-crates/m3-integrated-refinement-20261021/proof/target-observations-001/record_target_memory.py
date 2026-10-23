#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Retain real target correction observations and append closed-profile records."""
import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path('/workspace/scratch/206049dfb270')
CRATE = ROOT / 'synexia/synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021'
JAVA = Path('/tmp/m3-build-tools/jdk-21.0.12.1+1/bin/java')
JACKSON = Path('/tmp/m3-build-m2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar')
CLASSES = CRATE / 'proof/memory-conformance-001/classes'
OBS = CRATE / 'proof/target-observations-001'
OBS.mkdir(exist_ok=False)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def put_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def copy(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    assert not destination.exists(), destination
    shutil.copyfile(source, destination)
    assert sha(source) == sha(destination)


def artifact(path):
    path = Path(path)
    if not path.is_absolute():
        path = CRATE / path
    return {'path': path.relative_to(CRATE).as_posix(), 'sha256': sha(path)}


seals = json.loads((CRATE / 'SOURCE_SEALS.json').read_text())
before, after = [], []
for row in seals['files']:
    entry = {'target_path': row['target_path'], 'sha256': row['before_sha256']}
    if row['before_sha256'] != 'ABSENT':
        source = CRATE / 'preimages' / row['target_path']
        assert sha(source) == row['before_sha256']
        entry['retained_artifact'] = artifact(source)
    before.append(entry)
    emitted = CRATE / 'proof/sdk-002-offline/receiver' / row['target_path']
    assert sha(emitted) == row['after_sha256']
    after.append({'target_path': row['target_path'], 'sha256': row['after_sha256'],
                  'retained_artifact': artifact(emitted)})
put_json(OBS / 'SOURCE_BEFORE.json', {'repository': 'hsoliwal/M3jdk21',
         'revision': seals['receiving_revision'], 'files': before})
put_json(OBS / 'SOURCE_AFTER.json', {'repository': 'hsoliwal/M3jdk21',
         'revision': 'UNCOMMITTED_RECIPE_EMISSION', 'files': after,
         'sdk_result': artifact('proof/sdk-002-offline/RESULT.json')})

# Exact noncanonical observations remain intact, including their historical pending text.
collection = ROOT / 'collections-work'
copy(collection / 'evidence/CORRECTION_OBSERVATIONS.json', OBS / 'collections/CORRECTION_OBSERVATIONS.json')
collection_observations = json.loads((OBS / 'collections/CORRECTION_OBSERVATIONS.json').read_text())
col_fixtures = []
for observation in collection_observations['records']:
    for pin in observation['fixture_seals']:
        source = collection / pin['path']
        assert sha(source) == pin['sha256']
        target = OBS / 'collections' / pin['path']
        if not target.exists():
            copy(source, target)
            col_fixtures.append(artifact(target))
for name in ('BASELINE_DIRECT.json', 'BASELINE_INPUTS.json'):
    copy(collection / 'evidence' / name, OBS / 'collections/evidence' / name)

jobs = ROOT / 'jobs-work'
for attempt in ('authoring-proof-01', 'authoring-proof-02', 'emitted-target-proof-01'):
    for path in sorted((jobs / attempt).iterdir()):
        if path.is_file() and (path.name == 'RESULT.json' or path.suffix == '.log'):
            copy(path, OBS / 'scheduling' / attempt / path.name)
for path in sorted((jobs / 'history/authoring-proof-01').rglob('*')):
    if path.is_file():
        copy(path, OBS / 'scheduling/history/authoring-proof-01' / path.relative_to(jobs / 'history/authoring-proof-01'))

for attempt in ('eclipse-contract-donor-01', 'eclipse-contract-candidate-02', 'eclipse-current-candidate-03'):
    owner = jobs / attempt
    manifest = json.loads((owner / 'FROZEN_INPUTS.json').read_text())
    for record in manifest['files']:
        source = owner / record['path']
        assert sha(source) == record['sha256']
        copy(source, OBS / 'eclipse' / attempt / record['path'])
    for name in ('RESULT.json', 'FROZEN_INPUTS.json', 'maven.log'):
        copy(owner / name, OBS / 'eclipse' / attempt / name)
    for path in sorted((owner / 'target/surefire-reports').glob('*')):
        if path.is_file():
            copy(path, OBS / 'eclipse' / attempt / 'surefire-reports' / path.name)
for name in ('ECLIPSE_SOURCE_PIN.json', 'ECLIPSE_DONOR_CORRECTION.diff', 'ECLIPSE_API_COMPARISON.json', 'ECLIPSE_EXECUTED_COMMANDS.json'):
    copy(jobs / name, OBS / 'eclipse' / name)
for name in ('RESULT.json', 'maven-original-donor.log'):
    copy(jobs / 'eclipse-evaluation' / name, OBS / 'eclipse/eclipse-evaluation' / name)

index = json.loads((CRATE / 'memory/INDEX.json').read_text())
previous = index['events'][-1]
sdk = json.loads((CRATE / 'proof/sdk-002-offline/RESULT.json').read_text())


def base():
    return {'schema': 'MOP-RCM/1.0', 'profile': 'm3-integrated-refinement-20261021/1.0',
        'recipe_id': 'com.synexia.m3.IntegratedRefinement20261021',
        'event_type': 'RECORDED', 'admission': 'PENDING',
        'source': {'repository': 'hsoliwal/com.synexia',
                   'revision': 'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07'},
        'parents': [previous], 'supersedes': [], 'recipe_before': None,
        'recipe_after': artifact('proof/sdk-002-offline/inputs/synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3-integrated-refinement-20261021.yml'),
        'source_manifest_before': artifact(OBS / 'SOURCE_BEFORE.json'),
        'source_manifest_after': artifact(OBS / 'SOURCE_AFTER.json'),
        'observation_epoch_seconds': int(datetime.datetime.now(datetime.timezone.utc).timestamp()),
        'author': 'Hitesh Soliwal and Contributors to the Synexia Project', 'license': 'Apache-2.0',
        'applicability': ['The exact receiving source revision and finite task templates named by SOURCE_SEALS.json.'],
        'exclusions': ['No whole-repository, full-JDK, native-display or wider-family admission.',
                       'No reconstruction of missing October 20 recipes or receipts.'],
        'pending_gates': ['Independent receiving build/runtime/platform gates and review remain their owners authority.'],
        'unknowns': ['No earlier executable version of this new integrated task recipe was retained before these target authoring observations; recipe_before is unknown.',
                     'The new task and product candidate are uncommitted when this event is recorded.'],
        'verification': [{'gate': 'actual_offline_canonical_recipe', 'status': 'PASS',
                          'command': sdk['command'],
                          'evidence': [artifact('proof/sdk-002-offline/RESULT.json'), artifact('proof/sdk-002-offline/maven.log')]}],
        'replay': {'status': 'PASS', 'command': sdk['command'],
                   'evidence': [artifact('proof/sdk-001/RESULT.json'), artifact('proof/sdk-002-offline/RESULT.json')]},
        'environment': 'Actual Microsoft OpenJDK 21.0.12.1+1; Maven 3.9.9; Linux; exact pinned artifacts and source-built owners as retained by each gate.'}


def publish(number, event):
    draft = ROOT / 'canonical-work/memory-drafts' / (number + '.json')
    put_json(draft, event)
    command = [str(JAVA), '-cp', str(CLASSES) + ':' + str(JACKSON),
               'com.synexia.rewrite.memory.M3RecoveryMemory', 'publish', str(draft),
               str(CRATE), str(CRATE / 'memory/events')]
    result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=30)
    log = CRATE / 'proof' / ('memory-publication-' + number + '.log')
    with log.open('x') as stream:
        stream.write(result.stdout)
    assert result.returncode == 0, result.stdout
    path = Path(result.stdout.split('MOP_MEMORY_PUBLISH ', 1)[1].split(' admission=', 1)[0])
    assert sha(path) == path.stem
    index['events'].append(path.stem)
    return path.stem


event = base()
event.update({
    'trigger': 'Retained current-baseline endpoint failures, a stale native export allowlist, selected-directory allocation, and source-launcher verification failures were observed during authoring.',
    'root_cause': {'status': 'observed', 'explanation': 'Original strict read-only endpoints inherit empty-state exceptions; the pack compared nine emitted compactor symbols to one expected symbol and omitted two existing window symbols and the separate pathmask library; the launcher verifier uses the wrong repository root and receipt key. Allocation observations remain bounded to their probe.'},
    'change': 'Seal exact proved endpoint donor atoms, reuse the existing owned selection directory and stable sorter, package the existing full eleven-plus-one JNI closure with exact header/export refusals, and reuse the exact pinned launcher verifier. Preserve all C implementations and native declarations.',
    'lesson': 'Inventory every existing Java loader and native peer before repairing an ABI allowlist. Keep strict read-only endpoint semantics and compare against independent observed contracts; do not present compilation or a reduced symbol list as complete runtime proof.',
    'known_failures': ['Original private and public endpoint probes fail with NoSuchElementException.',
                       'Original native packaging refuses its actual nine compactor exports against a one-symbol allowlist.',
                       'The initial owning Maven launch resolved no tests before source launcher dependencies were admitted.'],
    'atoms': [{'id': record['atom'], 'owner': 'existing collections or module-pack owner',
               'contract': record['limits'], 'pattern': record['correction'],
               'applicability': [record['trigger']], 'exclusions': ['Observation does not grant product admission.']}
              for record in collection_observations['records']],
    'regression_fixtures': [artifact(OBS / 'collections/CORRECTION_OBSERVATIONS.json'), *col_fixtures],
})
event['unknowns'].append('Some initial collections probe logs retain outputs and source identities but no complete historical argv; no command is reconstructed for those observations.')
previous = publish('003', event)

event = base()
failed = json.loads((OBS / 'scheduling/authoring-proof-01/RESULT.json').read_text())
passed = json.loads((OBS / 'scheduling/emitted-target-proof-01/RESULT.json').read_text())
failed_command = next(command for command in failed['commands'] if command['label'] == 'mixed-candidate-M3ScheduledProgress')
passed_command = next(command for command in passed['commands'] if command['label'] == 'mixed-candidate-M3ScheduledProgress')
event.update({
    'trigger': 'The first scheduled-default authoring test failed with original declaration changed while the independent public javap descriptor check passed.',
    'root_cause': {'status': 'observed', 'explanation': 'The reflection oracle counted private compiler-generated lambda methods as public interface obligations.'},
    'change': 'Restrict the reflection census to public declared methods. Preserve the production candidate and independent binary/descriptor/behavior checks.',
    'lesson': 'A public API reflection oracle must distinguish public declarations from private synthetic implementation details; retain the false oracle and correct only its domain.',
    'known_failures': ['authoring-proof-01/mixed-candidate-M3ScheduledProgress: AssertionError original declaration changed.'],
    'atoms': [{'id': 'public-default-reflection-census', 'owner': 'M3ScheduledProgress.apiShape',
               'contract': 'Original four public scheduler declarations remain and exactly four new public default methods are additive.',
               'pattern': 'Filter declared methods by public access before comparing callable interface obligations; keep independently compiled legacy-client checks.',
               'applicability': ['Java interfaces whose default methods compile lambda bodies.'],
               'exclusions': ['No production change to satisfy a test.', 'No claim that reflection shape is unchanged after additive APIs.']}],
    'regression_fixtures': [artifact(OBS / 'scheduling/history/authoring-proof-01/test/jdk/java/util/concurrent/ProgressJob/M3ScheduledProgress.java'),
                            artifact(OBS / 'scheduling/history/authoring-proof-01/MEMORY_EVENT.json'),
                            artifact('proof/sdk-002-offline/receiver/test/jdk/java/util/concurrent/ProgressJob/M3ScheduledProgress.java')],
})
event['verification'] += [
    {'gate': 'retained_initial_authoring_failure', 'status': 'FAIL', 'command': failed_command['command'],
     'evidence': [artifact(OBS / 'scheduling/authoring-proof-01/RESULT.json'),
                  artifact(OBS / 'scheduling/authoring-proof-01/mixed-candidate-M3ScheduledProgress.stderr.log')]},
    {'gate': 'actual_emitted_target_scheduling', 'status': 'PASS', 'command': passed_command['command'],
     'evidence': [artifact(OBS / 'scheduling/emitted-target-proof-01/RESULT.json'),
                  artifact(OBS / 'scheduling/emitted-target-proof-01/mixed-candidate-M3ScheduledProgress.stdout.log')]},
]
previous = publish('004', event)

event = base()
event.update({
    'trigger': 'Four new tests against real Eclipse exposed two cancellation divergences in the pinned donor, despite its original seventeen-test corpus passing.',
    'root_cause': {'status': 'observed', 'explanation': 'Eclipse OperationCanceledException fell through the generic Exception catch and produced ERROR while the actual native Eclipse Job contract produced CANCEL.'},
    'change': 'Add the Eclipse cancellation import and catch alongside CancellationException. Keep original owner APIs, original seventeen tests, current Maven POM, and native Eclipse JobManager/rule scheduling ownership.',
    'lesson': 'Compare adapters to the actual framework cancellation and scheduling implementation. Retain successful original tests and add counterexamples for framework-specific signals, then test both donor and current dependency versions.',
    'known_failures': ['Original donor:21 tests,2 cancellation failures,0 errors,0 skips; native CANCEL8 versus adapter ERROR4.'],
    'atoms': [{'id': 'eclipse-native-cancellation-parity', 'owner': 'M3EclipseJobs.runBody',
               'contract': 'Map both existing Java cancellation and Eclipse cancellation signals to CANCEL while preserving unrelated ERROR handling.',
               'pattern': 'Use native Job and synchronous runNow counterexamples; preserve rule exclusion and never-started cancellation checks.',
               'applicability': ['Pinned Eclipse runtime3.35.0 with tested jobs3.15.700 and current3.15.900.'],
               'exclusions': ['No independent Eclipse scheduler, native callback layer, OSGi/PDE product proof, or installed SWT proof.']}],
    'regression_fixtures': [artifact(OBS / 'eclipse/ECLIPSE_DONOR_CORRECTION.diff'),
                            artifact(OBS / 'eclipse/ECLIPSE_API_COMPARISON.json'),
                            artifact(OBS / 'eclipse/eclipse-contract-donor-01/RESULT.json'),
                            artifact(OBS / 'eclipse/eclipse-contract-donor-01/src/test/java/com/m3/eclipse/runtime/M3EclipseJobsCompatibilityTest.java'),
                            artifact(OBS / 'eclipse/eclipse-contract-candidate-02/RESULT.json'),
                            artifact(OBS / 'eclipse/eclipse-current-candidate-03/RESULT.json')],
})
event['unknowns'].append('Original focused Eclipse RESULT files omitted argv. ECLIPSE_EXECUTED_COMMANDS.json is explicitly a later exact transcription from session tool calls, not a claim that this command supplement existed before execution.')
commands = json.loads((OBS / 'eclipse/ECLIPSE_EXECUTED_COMMANDS.json').read_text())
for trial in commands['trials']:
    owner = OBS / 'eclipse' / trial['trial']
    log = owner / ('maven-original-donor.log' if trial['trial'] == 'eclipse-evaluation' else 'maven.log')
    assert sha(log) == trial['log_sha256']
    assert sha(owner / 'RESULT.json') == trial['result_sha256']
    event['verification'].append({'gate': trial['trial'],
        'status': 'PASS' if trial['exit_code'] == 0 else 'FAIL', 'command': trial['argv'],
        'evidence': [artifact(owner / 'RESULT.json'), artifact(log),
                     artifact(OBS / 'eclipse/ECLIPSE_EXECUTED_COMMANDS.json')]})
previous = publish('005', event)

# Only the derived retrieval index changes; all published events and bound inputs are create-new.
(CRATE / 'memory/INDEX.json').write_text(json.dumps(index, indent=2) + '\n')
copy(Path(__file__), OBS / 'record_target_memory.py')
print(json.dumps({'events': index['events'], 'admission': 'PENDING', 'new_records': 3}))
