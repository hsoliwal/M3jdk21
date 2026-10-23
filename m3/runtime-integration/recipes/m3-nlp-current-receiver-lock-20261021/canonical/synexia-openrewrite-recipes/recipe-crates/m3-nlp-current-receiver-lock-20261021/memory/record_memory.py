#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Retain actual NLP correction evidence under the task's closed memory profile."""
from pathlib import Path
import hashlib
import json
import subprocess
import time

ROOT = Path('/workspace/scratch/206049dfb270')
CANONICAL = ROOT / 'synexia'
TASK = 'm3-nlp-current-receiver-lock-20261021'
CRATE = CANONICAL / 'synexia-openrewrite-recipes/recipe-crates' / TASK
MEMORY = CRATE / 'memory'
PROOF = CRATE / 'proof/memory-integrity-001'
JAVA = '/tmp/m3-build-tools/jdk-21.0.12.1+1/bin/java'
JACKSON = '/tmp/m3-build-m2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar'
OWNER = 'proof/owner-candidate-001/'
RUNTIME = 'proof/receiver-runtime-001/combined-receiver-001-receipts/'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(path):
    return json.loads((CRATE / path).read_text())


def artifact(path):
    file = CRATE / path
    assert file.resolve().is_relative_to(CRATE.resolve()) and not file.is_symlink()
    return {'path': path, 'sha256': sha(file)}


def put(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def gate(label, status, command, *paths):
    assert isinstance(command, list) and command
    return {'gate': label, 'status': status, 'command': command,
            'evidence': [artifact(path) for path in paths]}


def run(label, command):
    put(PROOF / (label + '-COMMAND.json'), {'argv': command, 'cwd': str(ROOT),
        'captured_before_execution': True})
    completed = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, timeout=30)
    with (PROOF / (label + '.log')).open('x') as stream:
        stream.write(completed.stdout)
    put(PROOF / (label + '-RESULT.json'), {'argv': command, 'exit_code': completed.returncode,
        'log': artifact('proof/memory-integrity-001/' + label + '.log')})
    assert completed.returncode == 0, completed.stdout
    return completed.stdout


def main():
    assert sha(CRATE / 'SOURCE_SEALS.json') == '6cc232bd392dd188b227a0a7c304d0a6e6d25ed91dd3641fefb335afdfc8574e'
    sdk = read('proof/sdk-001-offline/RESULT.json')
    runtime = read(RUNTIME + 'RESULT.json')
    source_inputs = read('proof/sdk-001-offline/SOURCE_INPUTS.json')
    assert sdk['status'] == 'PASS' and sdk['counts'] == {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0}
    assert runtime['state'] == 'PASS' and runtime['module_count'] == 62 and runtime['input_count'] == 11373
    assert runtime['inputs_unchanged'] and runtime['all_module_jar_hashes_equal_candidate']
    for row in source_inputs:
        assert sha(CANONICAL / row['path']) == row['sha256'], row['path']
    conformance = read('proof/memory-conformance-001/RESULT.json')
    assert conformance['status'] == 'PASS' and conformance['checks'] == 129 and conformance['control_cases'] == 32
    original = read('proof/receiver-refusal-001/RESULT.json')
    gvas_failed = read(OWNER + 'gvas-owner-001/RESULT.json')
    gvas_passed = read(OWNER + 'gvas-owner-002/RESULT.json')
    rope_failed = read(OWNER + 'rope-owner-001/RESULT.json')
    builder = read(OWNER + 'builder-and-whole-rope-001/RESULT.json')
    preparation = read('proof/preparation-refusal-001/RESULT.json')
    followup = read('proof/preparation-refusal-001/FOLLOWUP.json')
    intake_failed = read('proof/intake-refusal-001/RESULT.json')
    whole_rope = next(row for row in builder['commands'] if row['label'] == 'original-whole-module-rope')
    common = [
        gate('actual_canonical_two_file_sdk', 'PASS', sdk['command'],
             'proof/sdk-001-offline/RESULT.json', 'proof/sdk-001-offline/recipe-proof.tsv'),
        gate('actual_emitted_complete_receiving_image', 'PASS', runtime['command'],
             RUNTIME + 'RESULT.json', RUNTIME + 'command.log',
             'proof/receiver-runtime-001/FINAL_RECEIVER_QUALIFICATION.json')]
    config = [
        ('closed-source-lock-and-exact-received-lineage',
         'The unchanged combined receiver refused one outdated owner hash and two unlisted current source files before creating its output directory.',
         'observed',
         'The closed manifest had not followed three received M3 text owners. Immutable BigString canonical templates also differ from their own manifest and received files; that independent historical mismatch is retained rather than treated as valid regeneration.',
         'Qualify exact received GVAS, BigString and Rope contracts and lineage; replace only the two changed hashes and add the two explicit existing-source entries. Preserve the exact source-set guard, schema, donor pins and all other values.',
         'A source-lock refresh requires source custody and owning behavior proof. Preserve conflicting historical templates and stronger unreceived APIs as separate evidence; never infer correctness merely from merge state or matching names.',
         ['Original combined receiver: exit1, one hash drift, two coverage additions, no output directory.', 'Historical BigString source and test templates do not match the pinned manifest; no historical recipe replay success is claimed.'],
         ['proof/receiver-refusal-001/RESULT.json', OWNER + 'LINEAGE_REVIEW.json', OWNER + 'SOURCE_LOCK_GUARDS.json', OWNER + 'bigstring_template.diff', OWNER + 'bigstring_test_template.diff'],
         [gate('original_closed_source_refusal', 'FAIL', original['command'], 'proof/receiver-refusal-001/RESULT.json', 'proof/receiver-refusal-001/command.log')],
         ['The original source body behind the old GVAS digest is not recreated. Exact current receiving source and its legitimate earlier receiving commit are retained.']),
        ('cross-process-gvas-visibility-prerequisite',
         'The first unchanged GVAS owner gate failed while inspecting cooperating child process mappings in the sandbox.',
         'observed',
         'The current gate requires cross-process /proc visibility to verify common mapping address and inode. The first environment could not provide that required observation; no arithmetic or JNI source correction was made.',
         'Retain the failed invocation and rerun the identical original gate with the required process visibility. The unchanged source then passes three cooperating JVMs and 17,530 checks per child.',
         'An unavailable environment observation is not a passing native contract. Preserve the required observation and qualify the exact unchanged owner in an environment that supports it.',
         ['gvas-owner-001: original cross-process gate exit1; exact log retained.'],
         [OWNER + 'gvas-owner-001/INPUTS.json', OWNER + 'gvas-owner-001/command-01.log', OWNER + 'gvas-owner-002/INPUTS.json'],
         [gate('original_gvas_environment_refusal', 'FAIL', gvas_failed['commands'][0]['argv'], OWNER + 'gvas-owner-001/RESULT.json', OWNER + 'gvas-owner-001/command-01.log'),
          gate('same_original_gvas_gate_with_required_visibility', 'PASS', gvas_passed['commands'][0]['argv'], OWNER + 'gvas-owner-002/RESULT.json', OWNER + 'gvas-owner-002/command-01.log')],
         ['This is finite Linux Java/JNI evidence, not a cross-platform address-layout or whole-JDK guarantee.']),
        ('explicit-equivalent-builder-constructor',
         'The original whole-module rope gate refused -Werror missing-explicit-ctor on the exported SharedLexiconPrecomputeCatalog.Builder.',
         'observed',
         'The public nested Builder used an implicit public no-argument constructor. The existing strict module compiler requires that constructor to be explicit.',
         'Add only public Builder() { } and pin the resulting owner hash in the existing source manifest. Retain all original lint flags and the whole-module script, nine public class descriptors and complete Builder executable disassembly.',
         'Repair a mechanical source declaration while proving the old public descriptor and initialization instructions remain equal. A narrower passing dependency-closure diagnostic cannot replace the original failing whole-module gate.',
         ['rope-owner-001: whole-module compilation exit1 on missing-explicit-ctor.', 'rope-owner-002 is a separately labeled finite diagnostic; it was not accepted as the original whole-module result.'],
         [OWNER + 'SharedLexiconPrecomputeCatalog.java.diff', OWNER + 'builder-and-whole-rope-001/baseline-public-api.log', OWNER + 'builder-and-whole-rope-001/candidate-public-api.log', OWNER + 'builder-and-whole-rope-001/baseline-builder-bytecode.log', OWNER + 'builder-and-whole-rope-001/candidate-builder-bytecode.log'],
         [gate('original_whole_module_lint_refusal', 'FAIL', rope_failed['commands'][0]['argv'], OWNER + 'rope-owner-001/RESULT.json', OWNER + 'rope-owner-001/command-01.log'),
          gate('unchanged_whole_module_gate_after_explicit_constructor', 'PASS', whole_rope['argv'], OWNER + 'builder-and-whole-rope-001/RESULT.json', OWNER + 'builder-and-whole-rope-001/original-whole-module-rope.log')],
         ['Debug source-line tables may differ; full class-file byte identity is not claimed.']),
        ('baseline-materialization-before-preparation',
         'The first task preparation refused the exact SharedLexiconPrecomputeCatalog preimage while baseline hydration was still being completed.',
         'hypothesized',
         'The actual guard refusal is recorded. Later readback matches both expected hashes, and the parent independently records concurrent immutable baseline hydration. The timing explains the refusal, but the transient first-attempt bytes were not captured.',
         'Wait for the complete baseline materialization, then repeat the unchanged preparation command. It seals two phases successfully; actual SDK execution and application remain separate subsequent receipts.',
         'Complete immutable receiving inputs before executing preparation. Keep exact preimage checks and record unknown transient bytes explicitly rather than inventing a historical source seal.',
         ['First prepare_packet invocation: exit1 and no canonical postimages emitted.'],
         ['proof/preparation-refusal-001/RESULT.json', 'proof/preparation-refusal-001/FOLLOWUP.json', 'proof/unchanged-baseline-hydration-001/RESULT.json'],
         [gate('actual_preparation_preimage_refusal', 'FAIL', preparation['invocation'], 'proof/preparation-refusal-001/RESULT.json'),
          gate('same_preparation_after_completed_hydration', 'PASS', followup['successful_command'], 'proof/preparation-refusal-001/FOLLOWUP.json', 'SOURCE_SEALS.json')],
         ['First-attempt transient preimage bytes were not retained. Preparation commands in these two receipts are explicitly retrospective exact transcriptions of observed session tool calls.']),
        ('finite-cross-owner-evidence-custody',
         'The finite evidence copier refused one source path outside its initially admitted nlp-work root before copying any payload.',
         'observed',
         'The sealed 135-file handoff intentionally includes the independent peer lineage receipt jobs-work/nlp-lineage-readonly/HANDOFF.json. The new authoring copier initially omitted that one known source path from its finite intake boundary.',
         'Retain the refused tool bytes and allow only that exact non-symlink receipt in addition to the original NLP root. Preserve all handoff hash, byte count, UTF-8, normalized path, duplicate, symbolic-link and complete-preflight checks. No recipe engine or runtime source admission rule changes.',
         'A finite evidence intake should name actual cooperating source owners. Add a demonstrated exact evidence path instead of accepting arbitrary external paths or mutating the sealed source handoff.',
         ['First owner-candidate intake: AssertionError at source-root check, zero files copied.'],
         ['proof/intake-refusal-001/curate_nlp_owner_evidence.py', 'proof/intake-refusal-001/RESULT.json', 'provenance/intake/owner-candidate-001-HANDOFF.json', 'provenance/owner-candidate-001-INTAKE.json', 'provenance/qualification-tools/curate_owner_evidence.py'],
         [gate('original_finite_intake_refusal', 'FAIL', intake_failed['argv'], 'proof/intake-refusal-001/RESULT.json'),
          gate('exact_handoff_intake_after_single_path_correction', 'PASS', intake_failed['argv'], 'provenance/owner-candidate-001-INTAKE.json')],
         ['The intake command is retained as a retrospective exact transcription; the actual hash checks and copied readbacks are retained by the finite intake receipt.'])
    ]
    PROOF.mkdir(exist_ok=False)
    prefix = [JAVA, '-ea', '-Xmx512m', '-XX:ActiveProcessorCount=2', '-XX:-UsePerfData', '-cp',
              str(MEMORY / 'tooling/target/classes') + ':' + JACKSON,
              'com.synexia.rewrite.memory.M3RecoveryMemory']
    events = []
    references = {}
    commands = []
    yaml = 'proof/sdk-001-offline/named-recipe.yml'
    for number, (key, trigger, cause_status, cause, change, lesson, failures, fixtures, verification, unknowns) in enumerate(config, 1):
        event = {'schema': 'MOP-RCM/1.0', 'profile': TASK + '/1.0',
            'recipe_id': 'com.synexia.m3.NlpCurrentReceiverLock20261021',
            'event_type': 'RECORDED', 'admission': 'PENDING',
            'source': {'repository': 'hsoliwal/M3jdk21', 'revision': '2779aba3f98f31c324be43f731be7914e1a05935'},
            'parents': events[-1:], 'supersedes': [], 'recipe_before': None, 'recipe_after': artifact(yaml),
            'source_manifest_before': artifact('preimages/m3/tooling/module-packs/nlp-search/current-source-lock.json'),
            'source_manifest_after': artifact('SOURCE_SEALS.json'), 'observation_epoch_seconds': int(time.time()),
            'author': 'Hitesh Soliwal and Contributors to the Synexia Project', 'license': 'Apache-2.0',
            'trigger': trigger, 'root_cause': {'status': cause_status, 'explanation': cause},
            'change': change, 'lesson': lesson, 'known_failures': failures,
            'atoms': [{'id': key, 'owner': 'Existing M3 text owners, current-fork source guards and task proof custody',
                'contract': lesson, 'pattern': change,
                'applicability': ['Exact receiving2779 sources and the frozen two-file successor.'],
                'exclusions': ['No guard weakening, test suppression, new runtime abstraction or global product admission.']}],
            'regression_fixtures': [artifact(p) for p in fixtures], 'verification': verification + common,
            'replay': {'status': 'PASS', 'command': sdk['command'],
                'evidence': [artifact('proof/sdk-001-offline/RESULT.json'), artifact('proof/sdk-001-offline/recipe-proof.tsv'), artifact('proof/copier-qualification-001/RESULT.json')]},
            'environment': 'Actual Java21.0.12.1+1 and Maven3.9.9/OpenRewrite8.17.1; pinned current-fork build scripts and cached dependency archives. Source and classpath readbacks remain unchanged. Original Java21 preview/native-access warning behavior is retained.',
            'applicability': ['The three current received text owners have explicit source-lineage and owning-gate proof. Only the constructor declaration and source-lock file change.'],
            'exclusions': ['No earlier core, SWT or Maven4 memory event is inherited; no differing BigString stream API is imported.'],
            'pending_gates': ['Full String/HotSpot/JDK family, upstream Java25 suites, optional native/model, platform, performance and exact hosted admission remain separate.'],
            'unknowns': unknowns + ['This is a new task family; no previously qualified version of this task recipe is asserted. Published-core helper lineage is retained independently.']}
        draft = MEMORY / f'draft-{number:03d}.json'
        put(draft, event)
        publish = prefix + ['publish', str(draft), str(CRATE), str(MEMORY / 'events')]
        output = run(f'{number:03d}-publish', publish)
        path = Path(output.split('MOP_MEMORY_PUBLISH ', 1)[1].split(' admission=', 1)[0])
        assert sha(path) == path.stem
        check = prefix + ['check', str(path), str(CRATE), str(MEMORY / 'events')]
        run(f'{number:03d}-check', check)
        events.append(path.stem)
        commands.extend([publish, check])
        def walk(value):
            if isinstance(value, dict):
                if set(value) == {'path', 'sha256'}:
                    assert sha(CRATE / value['path']) == value['sha256']
                    references[value['path']] = value['sha256']
                for child in value.values(): walk(child)
            elif isinstance(value, list):
                for child in value: walk(child)
        walk(json.loads(path.read_text()))
    lessons = '''<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# NLP current-owner correction retrieval

INDEX.json lists five linked immutable RECORDED/PENDING events. FINAL_RESULT.json
binds the closed-profile test, each event and the actual recipe/receiver proof.

The records preserve: the original source-lock refusal and contradictory older
BigString templates; the GVAS cross-process visibility prerequisite; the strict
whole-module constructor warning and descriptor/bytecode-preserving correction;
the transient preparation refusal during baseline hydration; and the finite
evidence intake's missing exact peer-receipt path. The last two are authoring
custody lessons, not product defects. Unknown transient bytes are stated as
unknown. Narrow diagnostics and historical statuses are not rewritten as passes.

The resulting two-file recipe passes its actual SDK, exact application and
fixed point. The actual receiving full Java21 source/JMOD/image gate passes with
62 pack modules and three installed-image probes. Every one of the 62 module
JAR hashes equals the isolated candidate, and 11,373 input readbacks are unchanged.
This result is separate from the helper's 129 conformance checks and 32 control
cases. Original upstream Java25, platform, optional native/model, performance,
full-JDK and hosted admission remain separate. The image's two original Lucene
warnings remain retained; no launch flag or guard is weakened.
'''
    with (MEMORY / 'LESSONS.md').open('x') as stream: stream.write(lessons)
    put(MEMORY / 'INDEX.json', {'schema': 'M3_PENDING_CORRECTION_INDEX/1', 'profile': TASK + '/1.0',
        'events': events, 'admission': 'PENDING', 'retrieval': artifact('memory/LESSONS.md')})
    for row in source_inputs:
        assert sha(CANONICAL / row['path']) == row['sha256'], row['path']
    put(PROOF / 'RESULT.json', {'schema': 'M3_NLP_MEMORY_INTEGRITY/1', 'status': 'PASS',
        'profile': TASK + '/1.0', 'events': events, 'commands': commands,
        'event_integrity_checks': len(events), 'unique_event_artifacts_verified': len(references),
        'source_inputs_unchanged': True, 'source_input_count': len(source_inputs), 'admission': 'PENDING'})
    with (MEMORY / 'record_memory.py').open('xb') as stream: stream.write(Path(__file__).read_bytes())
    put(MEMORY / 'FINAL_RESULT.json', {'schema': 'M3_NLP_CURRENT_OWNER_CORRECTION_MEMORY_FINAL/1',
        'status': 'PASS', 'profile': TASK + '/1.0', 'event_count': len(events), 'events': events,
        'conformance_checks': 129, 'control_cases': 32, 'actual_event_integrity_checks': len(events),
        'unique_event_artifacts_verified': len(references), 'admission': 'PENDING',
        'profile_receipt': artifact('memory/PROFILE.md'), 'retrieval': artifact('memory/LESSONS.md'),
        'index': artifact('memory/INDEX.json'), 'helper_lineage': artifact('memory/HELPER_LINEAGE.json'),
        'profile_conformance': artifact('proof/memory-conformance-001/RESULT.json'),
        'event_integrity': artifact('proof/memory-integrity-001/RESULT.json'),
        'canonical_sdk': artifact('proof/sdk-001-offline/RESULT.json'),
        'actual_receiver': artifact(RUNTIME + 'RESULT.json'),
        'qualified_sdk_inputs_unchanged': True,
        'scope': 'Closed-profile conformance and actual evidence integrity only; no inherited event, test-count inflation or product admission.'})
    print(json.dumps({'status': 'PASS', 'events': len(events), 'artifacts': len(references),
        'final_receipt_sha256': sha(MEMORY / 'FINAL_RESULT.json')}))


if __name__ == '__main__':
    main()
