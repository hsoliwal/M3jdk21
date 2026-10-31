#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Record three actual SWT correction observations without granting admission."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time

ROOT = Path('/workspace/scratch/206049dfb270')
CANONICAL = ROOT / 'synexia'
TASK = 'swt-current-owner-runtime-closure-20261021'
CRATE = CANONICAL / 'synexia-openrewrite-recipes/recipe-crates' / TASK
MEMORY = CRATE / 'memory'
PROOF = CRATE / 'proof/memory-integrity-001'
JAVA = Path('/tmp/m3-build-tools/jdk-21.0.12.1+1/bin/java')
JACKSON = Path('/tmp/m3-build-m2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar')


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(relative: str):
    return json.loads((CRATE / relative).read_text())


def artifact(relative: str):
    path = CRATE / relative
    assert path.resolve().is_relative_to(CRATE.resolve()) and not path.is_symlink()
    return {'path': relative, 'sha256': sha(path)}


def put(path: Path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def gate(name, status, command, *paths):
    assert isinstance(command, list) and command and all(isinstance(x, str) for x in command)
    return {'gate': name, 'status': status, 'command': command,
            'evidence': [artifact(path) for path in paths]}


def execute(label, command):
    put(PROOF / (label + '-COMMAND.json'), {'argv': command, 'cwd': str(ROOT),
        'captured_before_execution': True})
    result = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, timeout=30)
    with (PROOF / (label + '.log')).open('x') as stream:
        stream.write(result.stdout)
    put(PROOF / (label + '-RESULT.json'), {'argv': command, 'exit_code': result.returncode,
        'log': artifact('proof/memory-integrity-001/' + label + '.log')})
    assert result.returncode == 0, result.stdout
    return result.stdout


def main():
    assert sha(CRATE / 'SOURCE_SEALS.json') == '8fbab5e40413771d432d5016964f5bd71d95bf4043805bac828d82bd92200bb0'
    sdk = read('proof/sdk-001-offline/RESULT.json')
    source_inputs = read('proof/sdk-001-offline/SOURCE_INPUTS.json')
    for row in source_inputs:
        assert sha(CANONICAL / row['path']) == row['sha256'], row['path']
    assert sdk['status'] == 'PASS' and sdk['counts'] == {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0}
    conformance = read('proof/memory-conformance-001/RESULT.json')
    assert conformance['status'] == 'PASS' and conformance['checks'] == 129 and conformance['control_cases'] == 32
    candidate = read('proof/pom-candidate-001/RESULT.json')
    assert candidate['status'] == 'PASS' and candidate['sdk_tests'] == {'tests': 7, 'failures': 0, 'errors': 0, 'skipped': 0}
    emitted = read('proof/receiver-runtime-001/RESULT.json')
    assert emitted['status'] == 'PASS'
    emitted_sdk = next(step for step in emitted['steps'] if step['name'] == 'sdk')
    emitted_receiver = next(step for step in emitted['steps'] if step['name'] == 'receiver')
    assert emitted_sdk['exit_code'] == 0 and emitted_receiver['exit_code'] == 0
    original_pom = 'preimages/docs/m3/scrollbar-full-range-20261020/recipes/swt-scrollbar-full-range-20261020/pom.xml'
    corrected_pom = 'proof/sdk-001-offline/receiver/docs/m3/scrollbar-full-range-20261020/recipes/swt-scrollbar-full-range-20261020/pom.xml'
    assert sha(CRATE / original_pom) == '7539cf81d2d7a7b30bff97ce2812a7e67f4e3593463a898aa38c8e6b0240dfa0'
    assert sha(CRATE / corrected_pom) == '6553565c8dd14a857e796f741f3f8f8e7cbf3edda11df58b8dcd8a53e78e5600'
    failures = read('provenance/BASELINE_FAILURE_CLASSIFICATION.json')['attempts']
    assert [x['executed_tests'] for x in failures] == [0, 7, 0]
    offline_one = read('proof/incumbent-offline-001/RESULT.json')
    bootstrap_command = read('proof/incumbent-bootstrap-001/COMMAND.json')['argv']
    offline_two = read('proof/incumbent-offline-002/RESULT.json')
    shared_verification = [
        gate('corrected_candidate_existing_owner_sdk', 'PASS', candidate['steps'][0]['argv'],
             'proof/pom-candidate-001/RESULT.json', 'proof/pom-candidate-001/sdk.log'),
        gate('actual_new_canonical_recipe_sdk', 'PASS', sdk['command'],
             'proof/sdk-001-offline/RESULT.json', 'proof/sdk-001-offline/recipe-proof.tsv'),
        gate('actual_emitted_pom_existing_owner_sdk', 'PASS', emitted_sdk['argv'],
             'proof/receiver-runtime-001/RESULT.json', 'proof/receiver-runtime-001/sdk.log'),
        gate('actual_emitted_pom_generated_and_current_receiver', 'PASS', emitted_receiver['argv'],
             'proof/receiver-runtime-001/RESULT.json', 'proof/receiver-runtime-001/receiver.log')]
    descriptions = [
        {
            'id': 'offline-clean-plugin-prerequisite',
            'trigger': 'The original owning Maven clean/test invocation refused an unavailable maven-clean-plugin:3.2.0 in offline mode; zero tests executed.',
            'cause': 'The exact local Maven cache lacked the requested clean plugin. This was an infrastructure prerequisite refusal, not a failed arithmetic or recipe assertion.',
            'change': 'Retain the offline refusal and provision the unchanged owning POM with explicit proxy and verified TLS. The resulting bootstrap reached the existing seven-test suite and exposed the separate SLF4J closure failure. Subsequent corrected-owner proofs ran offline.',
            'lesson': 'Distinguish cache admission from test execution. Provision exact unchanged dependencies before claiming an offline owner result; retain the first refusal and the later actual suite outcome separately.',
            'failure': 'incumbent-offline-001: exit1, missing maven-clean-plugin:3.2.0, zero tests executed.',
            'contract': 'Unavailable offline dependencies refuse without a test-pass claim or changed production source.',
            'verification': [gate('original_offline_prerequisite_refusal', 'FAIL', offline_one['steps'][0]['argv'],
                'proof/incumbent-offline-001/RESULT.json', 'proof/incumbent-offline-001/sdk.log'),
                gate('unchanged_pom_bootstrap_reaches_distinct_closure_failure', 'FAIL', bootstrap_command,
                'proof/incumbent-bootstrap-001/RESULT.json', 'proof/incumbent-bootstrap-001/maven.log')],
            'fixtures': ['proof/incumbent-offline-001/COMMANDS.json', 'proof/incumbent-bootstrap-001/COMMAND.json'],
            'unknowns': ['The complete cache before the failed offline invocation was not sealed; the retained log proves the specific requested artifact was unavailable. No broader cache provenance is invented.']
        },
        {
            'id': 'owning-sdk-slf4j-api-runtime-closure',
            'trigger': 'After dependency bootstrap, the unchanged received SDK executed seven tests with four failures and three errors because Recipe initialization could not load org.slf4j.LoggerFactory.',
            'cause': 'The received proof POM omitted the SLF4J API needed by the existing OpenRewrite runtime. The preserved Surefire report binds all seven actual test outcomes to NoClassDefFoundError or its initialization consequences.',
            'change': 'The one-file canonical text recipe adds org.slf4j:slf4j-api:2.0.13 to that exact received proof POM. Existing recipe engines, original seven-test source, arithmetic source, and receiver verification remain unchanged; generated correction is checked against the original preimage and fixed point.',
            'lesson': 'Declare the existing SDK runtime dependency in its owning POM, then rerun the unchanged owning suite and consume actual generated receiver output. A separately qualified proposal cannot substitute for the received owner.',
            'failure': 'incumbent-bootstrap-001: seven tests, four failures, three errors, zero skips; missing org/slf4j/LoggerFactory.',
            'contract': 'The exact received proof POM supplies its required API; existing recipe/refusal and arithmetic contracts pass without production geometry edits.',
            'verification': [gate('original_received_pom_closure_failure', 'FAIL', bootstrap_command,
                'proof/incumbent-bootstrap-001/RESULT.json', 'proof/incumbent-bootstrap-001/maven.log',
                'proof/incumbent-bootstrap-001/surefire-reports/TEST-com.synexia.rewrite.ScrollbarFullRangeRecipeTest.xml')],
            'fixtures': [original_pom, corrected_pom,
                'proof/incumbent-inputs/docs/m3/scrollbar-full-range-20261020/recipes/swt-scrollbar-full-range-20261020/src/test/java/com/synexia/rewrite/ScrollbarFullRangeRecipeTest.java'],
            'unknowns': ['No native SWT widget, display, platform, full Tycho or performance result follows from this SDK runtime dependency correction.']
        },
        {
            'id': 'fresh-build-discovery-and-stale-report-custody',
            'trigger': 'A later offline retry refused test discovery at com.synexia.rewrite..rsync-tmp.ScrollbarFullRangeRecipeTest; zero tests executed.',
            'cause': 'Unexpected compiled-class residue was discovered under .rsync-tmp in the shared build workspace. The copied XML in that attempt was byte-identical to the earlier bootstrap report and did not describe a second seven-test execution.',
            'change': 'Retain the actual zero-test discovery log and stale-report classification. Run the exact sealed source snapshot in a fresh isolated temporary build root, with original test selection and source bytes preserved. The independently required POM correction is tracked by the previous event.',
            'lesson': 'Bind reports to the invocation that produced them. A stale XML copy cannot inflate tests executed, and fresh build isolation must not silently replace or filter the original test authority.',
            'failure': 'incumbent-offline-002: exit1 during discovery, zero tests; copied bootstrap XML remains historical and is counted only once.',
            'contract': 'Fresh source-isolated builds discover the original tests; unexpected compiled residue and stale report copies remain distinct evidence.',
            'verification': [gate('original_local_discovery_refusal', 'FAIL', offline_two['steps'][0]['argv'],
                'proof/incumbent-offline-002/RESULT.json', 'proof/incumbent-offline-002/sdk.log',
                'provenance/BASELINE_FAILURE_CLASSIFICATION.json')],
            'fixtures': ['proof/incumbent-offline-002/surefire-reports/2026-10-21T03-18-39_578-jvmRun1.dump.txt',
                'proof/incumbent-offline-002/surefire-reports/2026-10-21T03-18-39_578-jvmRun1.dumpstream.txt',
                'proof/incumbent-bootstrap-001/surefire-reports/TEST-com.synexia.rewrite.ScrollbarFullRangeRecipeTest.xml'],
            'unknowns': ['The producer and complete prior state of the .rsync-tmp compiled residue were not sealed. Fresh candidate and emitted builds also contain the separately required POM correction; no isolation-only causal experiment is claimed.']
        }
    ]
    PROOF.mkdir(exist_ok=False)
    classpath = str(MEMORY / 'tooling/target/classes') + ':' + str(JACKSON)
    common = [str(JAVA), '-ea', '-Xmx512m', '-XX:ActiveProcessorCount=2', '-XX:-UsePerfData',
              '-cp', classpath, 'com.synexia.rewrite.memory.M3RecoveryMemory']
    events = []
    commands = []
    references = {}
    for number, item in enumerate(descriptions, 1):
        event = {
            'schema': 'MOP-RCM/1.0', 'profile': TASK + '/1.0',
            'recipe_id': 'com.synexia.swt.CurrentOwnerRuntimeClosure20261021',
            'event_type': 'RECORDED', 'admission': 'PENDING',
            'source': {'repository': 'hsoliwal/eclipse.platform.swt',
                       'revision': 'c0ffa6d76338ed1b06cfe0158b0e04d8930df546'},
            'parents': events[-1:], 'supersedes': [],
            'recipe_before': artifact(original_pom), 'recipe_after': artifact(corrected_pom),
            'source_manifest_before': artifact('proof/incumbent-offline-001/INPUTS.json'),
            'source_manifest_after': artifact('SOURCE_SEALS.json'),
            'observation_epoch_seconds': int(time.time()),
            'author': 'Hitesh Soliwal and Contributors to the Synexia Project', 'license': 'Apache-2.0',
            'trigger': item['trigger'], 'root_cause': {'status': 'observed', 'explanation': item['cause']},
            'change': item['change'], 'lesson': item['lesson'], 'known_failures': [item['failure']],
            'atoms': [{'id': item['id'], 'owner': 'The exact received SWT proof POM and its unchanged existing gates',
                       'contract': item['contract'], 'pattern': item['change'],
                       'applicability': ['Pinned c0ffa6d7 received proof owner and the frozen task recipe source seals.'],
                       'exclusions': ['No test suppression, shared engine change, new geometry owner or native platform admission.']}],
            'regression_fixtures': [artifact(p) for p in item['fixtures']] + [artifact('provenance/BASELINE_FAILURE_CLASSIFICATION.json')],
            'verification': item['verification'] + shared_verification,
            'replay': {'status': 'PASS', 'command': sdk['command'],
                       'evidence': [artifact('proof/sdk-001-offline/RESULT.json'),
                                    artifact('proof/sdk-001-offline/recipe-proof.tsv'),
                                    artifact('proof/copier-qualification-001/RESULT.json')]},
            'environment': 'Actual Java21.0.12.1+1, Maven3.9.9 and OpenRewrite8.17.1; exact commands, dependency custody and source readbacks are retained per attempt. Successful receiver gates use fresh isolated temporary build roots.',
            'applicability': ['This correction targets the current received SWT SDK POM. The old canonical #12544 remains an unmerged separate branch; no absent canonical owner tree is synthesized.'],
            'exclusions': ['No inherited events from earlier core, SWT proposal or Maven4 task memory. The earlier fourteen-test SWT proposal is a separate proof owner.'],
            'pending_gates': ['Hosted admission and any required native SWT, display, Tycho or full-product qualification remain independent gates.'],
            'unknowns': item['unknowns']}
        draft = MEMORY / ('draft-' + str(number).zfill(3) + '.json')
        put(draft, event)
        publish = common + ['publish', str(draft), str(CRATE), str(MEMORY / 'events')]
        output = execute(str(number).zfill(3) + '-publish', publish)
        path = Path(output.split('MOP_MEMORY_PUBLISH ', 1)[1].split(' admission=', 1)[0])
        assert sha(path) == path.stem
        check = common + ['check', str(path), str(CRATE), str(MEMORY / 'events')]
        execute(str(number).zfill(3) + '-check', check)
        events.append(path.stem)
        commands.extend([publish, check])
        def walk(value):
            if isinstance(value, dict):
                if set(value) == {'path', 'sha256'}:
                    assert sha(CRATE / value['path']) == value['sha256']
                    references[value['path']] = value['sha256']
                for child in value.values():
                    walk(child)
            elif isinstance(value, list):
                for child in value:
                    walk(child)
        walk(json.loads(path.read_text()))
    lessons = '''<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Current SWT owner correction retrieval

Read INDEX.json and FINAL_RESULT.json for three linked immutable records. The
records separate missing offline clean-plugin admission (zero tests), the actual
SLF4J runtime closure failure (seven tests: four failures and three errors), and
the compiled-class discovery refusal (zero tests). The third attempt's copied
bootstrap XML is historical evidence, not another suite execution.

The canonical recipe changes one received POM to add the existing SLF4J API.
Its actual SDK proves exact replay and refusals. The emitted corrected POM then
passes the unchanged seven-test owner and generated/current arithmetic receiver
in both tiered and C1 modes. The production geometry and original test source
remain unchanged. Source isolation repairs the local discovery environment;
it does not justify suppressing a test or relabeling a stale report.

The helper's 129 conformance checks, including 32 control-character cases, are
separate from the one new recipe JUnit test and the seven existing owner tests.
All memory events remain RECORDED/PENDING. Native widget, display, Tycho,
platform and hosted admission remain independent. No earlier task events or
unmerged canonical owner tree are imported into this task's history.
'''
    with (MEMORY / 'LESSONS.md').open('x') as stream:
        stream.write(lessons)
    put(MEMORY / 'INDEX.json', {'schema': 'M3_PENDING_CORRECTION_INDEX/1', 'profile': TASK + '/1.0',
        'events': events, 'admission': 'PENDING', 'lessons': artifact('memory/LESSONS.md'),
        'conformance_receipt': artifact('proof/memory-conformance-001/RESULT.json')})
    for row in source_inputs:
        assert sha(CANONICAL / row['path']) == row['sha256'], row['path']
    put(PROOF / 'RESULT.json', {'schema': 'SWT_CURRENT_OWNER_MEMORY_INTEGRITY/1', 'status': 'PASS',
        'profile': TASK + '/1.0', 'events': events, 'commands': commands,
        'event_integrity_checks': len(events), 'unique_event_artifacts_verified': len(references),
        'source_input_readbacks': len(source_inputs), 'source_inputs_unchanged': True,
        'admission': 'PENDING'})
    with (MEMORY / 'record_memory.py').open('xb') as stream:
        stream.write(Path(__file__).read_bytes())
    put(MEMORY / 'FINAL_RESULT.json', {'schema': 'SWT_CURRENT_OWNER_CORRECTION_MEMORY_FINAL/1',
        'status': 'PASS', 'profile': TASK + '/1.0', 'event_count': len(events), 'events': events,
        'conformance_checks': 129, 'control_cases': 32,
        'actual_event_integrity_checks': len(events), 'unique_event_artifacts_verified': len(references),
        'admission': 'PENDING', 'profile_receipt': artifact('memory/PROFILE.md'),
        'retrieval': artifact('memory/LESSONS.md'), 'index': artifact('memory/INDEX.json'),
        'helper_lineage': artifact('memory/HELPER_LINEAGE.json'),
        'profile_conformance': artifact('proof/memory-conformance-001/RESULT.json'),
        'event_integrity': artifact('proof/memory-integrity-001/RESULT.json'),
        'new_canonical_recipe_sdk': artifact('proof/sdk-001-offline/RESULT.json'),
        'actual_emitted_receiver': artifact('proof/receiver-runtime-001/RESULT.json'),
        'qualified_sdk_source_inputs_unchanged': True,
        'scope': 'Independent closed-profile conformance and immutable evidence integrity. Runtime proof remains the actual owning receipts; no product or hosted admission is granted.'})
    print(json.dumps({'status': 'PASS', 'events': len(events), 'artifacts': len(references),
                      'final_receipt_sha256': sha(MEMORY / 'FINAL_RESULT.json')}))


if __name__ == '__main__':
    main()
