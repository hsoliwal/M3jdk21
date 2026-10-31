"""Inspect existing E3 proof files only; never execute project or recipe code."""
from datetime import datetime, timezone
from hashlib import sha1, sha256
from pathlib import Path
import ast
import json
import re
import sys
import xml.etree.ElementTree as ET

BASE = Path('/workspace/scratch/1c68df1bae79/javac-convergence-20261005')
E3 = BASE / 'work/m3jdk21-current/successor-e03'
ROOT = E3 / 'overlay'
PROOF = E3 / 'verification'
OUT = BASE / 'work/publication-current/m3-final-gate-review'
CRATE = ROOT / 'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-final-handoff-20261005'
BUILD = ROOT / 'm3/tooling/migration-recipes/verification/jcc-source-final-handoff/target'
INPUTS = {}


def read(path):
    path = Path(path)
    content = path.read_bytes()
    row = {'path': str(path), 'bytes': len(content), 'sha256': sha256(content).hexdigest(),
           'git_blob_sha1': sha1(b'blob ' + str(len(content)).encode() + b'\0' + content).hexdigest(),
           'mode': oct(path.stat().st_mode & 0o777)}
    if str(path) in INPUTS and INPUTS[str(path)] != row:
        raise ValueError('Input changed during review: ' + str(path))
    INPUTS[str(path)] = row
    return content


def load(path):
    return json.loads(read(path))


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify_identity(row):
    read(row['path'])
    observed = INPUTS[str(Path(row['path']))]
    require(all(observed[key] == value for key, value in row.items()), 'Frozen identity mismatch: ' + row['path'])


def write(name, value):
    (OUT / name).write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')


receipt = load(PROOF / 'receipt.json')
frozen = load(PROOF / 'inputs.json')
plan = load(CRATE / 'plan.json')
require(receipt['source_publication_commit'] == plan['source_commit'] == 'd1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906', 'Wrong source pin')
require(receipt['destination_preimage_commit'] == plan['target_commit'] == 'df06cdee5a8526f573b3ad89622824d0b49e2f25', 'Wrong receiver preimage pin')
require(receipt['plan_sha256'] == frozen['plan_sha256'] == plan['plan_sha256'] ==
        'af65912789780e214f09644b38f542db5f7670b83ee1fada228bce08f8f9b30e', 'Wrong plan seal')
canonical_plan = (json.dumps({k: v for k, v in plan.items() if k != 'plan_sha256'},
                            sort_keys=True, separators=(',', ':'), ensure_ascii=True) + '\n').encode()
require(sha256(canonical_plan).hexdigest() == plan['plan_sha256'], 'Canonical plan seal does not match body')
require(len(frozen['fixed_inputs']) == 166 and len(frozen['external_source_and_proof_inputs']) == 63,
        'Frozen scope differs')
for row in frozen['fixed_inputs'] + frozen['external_source_and_proof_inputs']:
    verify_identity(row)
verify_identity(receipt['inputs'])
require(len(plan['outputs']) == len(frozen['operational_preimages']) == 4 and len(plan['guards']) == 7,
        'Operational scope differs')
require(not any(row['before'] is None or row['after'] is None for row in plan['outputs']),
        'An E3 operation incorrectly assumes creation/deletion')
old_rows = {Path(row['path']).relative_to(ROOT).as_posix(): row for row in frozen['operational_preimages']}
outputs = []
for row in plan['outputs']:
    before = read(CRATE / row['before']['resource'])
    after = read(CRATE / row['after']['resource'])
    actual = read(ROOT / row['path'])
    require(before == read(E3.parent / 'candidate' / row['path']), 'Before resource differs from actual retained E2 bytes')
    require(sha256(before).hexdigest() == row['before']['sha256'] == old_rows[row['path']]['sha256'],
            'Preimage identity differs')
    require(sha256(after).hexdigest() == row['after']['sha256'], 'After resource identity differs')
    require(actual == after and actual != before, 'Operational output is not exact changed afterimage')
    require(oct((ROOT / row['path']).stat().st_mode & 0o777) == old_rows[row['path']]['mode'], 'Operational file mode changed')
    outputs.append({'path': row['path'], 'before_sha256': row['before']['sha256'],
                    'after_sha256': row['after']['sha256'], 'actual_equals_complete_after_bytes': True,
                    'original_mode_preserved': True})
for row in plan['guards']:
    require(sha256(read(ROOT / row['path'])).hexdigest() == row['sha256'], 'Guard drift')
manifest = [line.split('\t') for line in read(CRATE / 'manifest.tsv').decode().splitlines() if line and not line.startswith('#')]
require(manifest == [[row['path'], row['before']['sha256'], row['after']['sha256'], row['after']['resource']] for row in plan['outputs']],
        'Manifest and plan differ')

expected_ids = ['01-check-E2-preimages', '02-actual-text-recipe-maven', '03-actual-packet-contract',
                '04-apply', '05-apply-fixed-point', '06-rollback', '07-replay', '08-check-final',
                '09-whole-map-validate', '10-completion-remains-blocked']
require([row['id'] for row in receipt['runs']] == expected_ids, 'Execution set/order differs')
steps = []
prior_end = frozen['frozen_at_utc']
for row in receipt['runs']:
    require(load(PROOF / (row['id'] + '.json')) == row, 'Step receipt differs')
    require(prior_end <= row['started_utc'] <= row['ended_utc'], 'Execution order/timing differs')
    prior_end = row['ended_utc']
    expected_exit = 2 if row['id'] == '10-completion-remains-blocked' else 0
    require(row['exit_code'] == row['expected_exit_code'] == expected_exit, 'Unexpected step exit')
    verify_identity(row['stdout'])
    verify_identity(row['stderr'])
    require(row['cwd'] == str(ROOT), 'Wrong operational root')
    steps.append({'id': row['id'], 'exit_code': row['exit_code'], 'expected_exit_code': expected_exit})
installer_expected = {'01-check-E2-preimages': ('before', 0), '04-apply': ('after', 4),
                      '05-apply-fixed-point': ('after', 0), '06-rollback': ('before', 4),
                      '07-replay': ('after', 4), '08-check-final': ('after', 0)}
installer_results = []
for name, (state, writes) in installer_expected.items():
    value = load(PROOF / (name + '.stdout.log'))
    require(value == {'files': 4, 'plan_sha256': plan['plan_sha256'], 'recipe': plan['recipe_id'],
                      'state': state, 'writes': writes}, 'Installer output differs: ' + name)
    installer_results.append({'step': name, **value})
for name in ('09-whole-map-validate', '10-completion-remains-blocked'):
    require(read(PROOF / (name + '.stdout.log')).decode().strip() == 'MIGRATION_MANIFEST_VALID completion=INCOMPLETE',
            'Validator/completion result differs')
require(read(PROOF / '10-completion-remains-blocked.stderr.log'), 'Completion blockers absent')

xml_copy = read(PROOF / 'JccSourceFinalHandoffTest.xml')
require(xml_copy == read(BUILD / 'surefire-reports/TEST-com.m3.rewrite.backport.JccSourceFinalHandoffTest.xml'),
        'JUnit copied report differs from actual build report')
x = ET.fromstring(xml_copy)
counts = {key: int(x.get(key)) for key in ('tests', 'failures', 'errors', 'skipped')}
require(counts == {'tests': 11, 'failures': 0, 'errors': 0, 'skipped': 0}, 'JUnit result differs')
cases = x.findall('testcase')
require(len(cases) == 11 and all(not any(t.find(k) is not None for k in ('failure', 'error', 'skipped')) for t in cases),
        'JUnit testcase details differ')
java_source = ROOT / 'm3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/JccSourceFinalHandoffTest.java'
java_methods = re.findall(r'@Test\s+void\s+(\w+)\(', read(java_source).decode())
require(set(java_methods) == {c.get('name') for c in cases} and len(java_methods) == 11, 'Executed Java test scope differs')
for category, expected in [('compile', ROOT / 'm3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java'),
                            ('testCompile', java_source)]:
    filename = 'default-compile' if category == 'compile' else 'default-testCompile'
    input_list = BUILD / ('maven-status/maven-compiler-plugin/' + category + '/' + filename + '/inputFiles.lst')
    actual = [Path(v).resolve() for v in read(input_list).decode().splitlines() if v]
    require(actual == [expected.resolve()], 'Unexpected compiled owner/test set')
props = {p.get('name'): p.get('value') for p in x.findall('properties/property')}
require(props['java.version'] == '21.0.2', 'Unexpected test JVM version')
require(not any('/provider/' in p for p in props['java.class.path'].split(':')), 'Source provider leaked into receiving runner')
maven_output = read(PROOF / '02-actual-text-recipe-maven.stdout.log').decode()
require('[INFO] BUILD SUCCESS' in maven_output and 'BUILD FAILURE' not in maven_output, 'Maven did not pass')
maven_command = receipt['runs'][1]['command']
require('-o' in maven_command and maven_command[-2:] == ['clean', 'verify'], 'Wrong Maven gate')
read(ROOT / 'm3/tooling/migration-recipes/verification/jcc-source-final-handoff/pom.xml')
resource_copies = []
for path in sorted((BUILD / 'classes').rglob('*')):
    if path.is_file() and path.suffix != '.class':
        rel = path.relative_to(BUILD / 'classes')
        source = ROOT / 'm3/tooling/migration-recipes/src/main/resources' / rel
        require(read(source) == read(path), 'Test-loaded resource differs from sealed input')
        resource_copies.append(str(rel))
require(len(resource_copies) == 11, 'Unexpected test-loaded resource count')

python_path = ROOT / 'm3/migration/test/test_jcc_source_final_handoff_packet.py'
python_source = read(python_path).decode()
python_tree = ast.parse(python_source)
python_methods = [n.name for n in ast.walk(python_tree) if isinstance(n, ast.FunctionDef) and n.name.startswith('test_')]
python_output = read(PROOF / '03-actual-packet-contract.stderr.log').decode()
logged_methods = re.findall(r'^(test_\w+) \(__main__\.[^)]+\) \.\.\. ok$', python_output, re.M)
require(set(logged_methods) == set(python_methods) and len(logged_methods) == len(python_methods) == 6,
        'Executed Python test scope differs')
require(re.search(r'Ran 6 tests in [0-9.]+s\s+OK\s*$', python_output), 'Python summary not successful')
require(receipt['python_tests'] == 6 and receipt['python_refusal_checks'] == 132, 'Receipt Python count differs')

mapping = load(ROOT / 'm3/docs/name-mapping.json')
records = mapping['migration']['records']
jcc_records = [r for r in records if r['id'] in ('synexia.jcc-recipe-laboratory', 'synexia.jcc-java-jni-regression')]
require(len(records) == 46 and len(jcc_records) == 2 and all(r['status'] == 'blocked' and r['tests'] == [] for r in jcc_records),
        'Blocked capability status was changed')
binding = load(ROOT / 'm3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json')
require(set(binding['acceptance']) == {'source_export_admitted', 'destination_materialized', 'destination_gates_passed', 'remote_read_back_delivered'}
        and all(type(v) is bool and not v for v in binding['acceptance'].values()), 'Capability acceptance changed')
require(not receipt['source_export_admitted'] and not receipt['receiver_behavior_rerun'] and not receipt['jdk_acceptance'],
        'Scope receipt incorrectly promotes acceptance')
read(E3 / 'run_verification.py')
validator = read(ROOT / 'm3/migration/migration.py').decode()
require("implemented = record['status'] in {'implemented-tested', 'implemented-unverified'}" in validator,
        'Validator implementation condition changed')

reference_review = None
if len(sys.argv) == 2:
    reference_path = Path(sys.argv[1]).resolve()
    audit = load(reference_path)
    require(audit['result'] == 'PASS' and audit['plan_sha256'] == plan['plan_sha256'],
            'Reference audit is not passing for the same plan')
    require(audit['source_publication_commit'] == plan['source_commit'] and
            audit['destination_preimage_commit'] == plan['target_commit'], 'Reference audit has wrong pins')
    fields = load(E3 / 'source-binding-review/SOURCE_FIELDS_AND_PROOFS.json')
    source_paths = {row['path']: row['local_path'] for group in
                    ('production_changes', 'retained_mapped_sources', 'retained_contract_sources', 'proof_references')
                    for row in fields[group]}
    for group, expected in [('source_reference_occurrences', 17), ('source_proof_reference_occurrences', 46)]:
        require(len(audit[group]) == expected, 'Reference audit scope differs')
        for row in audit[group]:
            path = Path(source_paths[row['path']])
            read(path)
            observed = INPUTS[str(path)]
            require(all(observed[k] == row[k] for k in ('bytes', 'sha256', 'git_blob_sha1', 'mode')),
                    'Source reference bytes differ: ' + row['path'])
            require(row['commit'] == plan['source_commit'], 'Source reference is not final-pinned')
    require(len(audit['destination_reference_occurrences']) == 45 and audit['destination_distinct_paths'] == 34,
            'Destination audit scope differs')
    require(len({row['path'] for row in audit['destination_reference_occurrences']}) == 34,
            'Destination distinct reference count differs')
    for row in audit['destination_reference_occurrences']:
        path = ROOT / row['path']
        read(path)
        observed = INPUTS[str(path)]
        require(all(observed[k] == row[k] for k in ('bytes', 'sha256', 'git_blob_sha1', 'mode')),
                'Destination reference bytes differ: ' + row['path'])
    require(len(audit['e2_preserved_payload_files']) == 56, 'E2 preservation audit scope differs')
    for row in audit['e2_preserved_payload_files']:
        require(read(ROOT / row['path']) == read(E3.parent / 'candidate' / row['path']),
                'Preserved E2 payload body differs: ' + row['path'])
    require(audit['ordered_records'] == 46 and audit['other_records_unchanged'] == 44 and
            audit['gates_unchanged'] == 20 and audit['root_objects_checked'] == 343 and
            audit['paired_maven_declarations_checked'] == 201, 'Static audit accounting differs')
    require(audit['unverified_active_hash_references_in_requested_two_rows'] == [],
            'Active requested reference remains unverified')
    require(audit['acceptance_flags_false'] and audit['capability_tests_empty'] and
            not audit['receiver_behavior_rerun'] and not audit['source_export_admitted'] and
            not audit['full_module_or_jdk_acceptance'], 'Reference audit improperly promotes acceptance')
    final_checker = read(E3 / 'check_references.py')
    initial_checker = read(reference_path.parent / 'attempt01-checker.py')
    # Preserve the failed checker and note while protecting the new final report from overwrite.
    corrected_checker = initial_checker.replace(b"'|'.join", b"';'.join").replace(
        b"out=HERE/'reference-audit'; assert not out.exists(); out.mkdir()",
        b"out=HERE/'reference-audit'; out.mkdir(exist_ok=True); assert not (out/'REFERENCE_AUDIT.json').exists()")
    require(corrected_checker == final_checker,
            'Reference checker changed beyond separators and failure-artifact-preserving output guard')
    attempt = read(reference_path.parent / 'attempt01.txt').decode()
    require('Checker-only failed observation' in attempt and 'canonical TSV uses semicolons' in attempt,
            'Initial reference-inspection failure note is not retained')
    reference_review = {'path': str(reference_path), 'sha256': sha256(read(reference_path)).hexdigest(),
                        'result': 'PASS', 'source_reference_occurrences': 17,
                        'source_proof_reference_occurrences': 46, 'destination_reference_occurrences': 45,
                        'destination_distinct_paths': 34, 'e2_payload_files_full_byte_equal': 56,
                        'all_referenced_local_file_identities_rechecked': True,
                        'uses_existing_remote_readbacks_not_new_remote_requests': True,
                        'initial_checker_separator_failure_preserved': True,
                        'failed_checker_has_written_note_not_raw_command_trace': True,
                        'output_guard_adjustment_preserves_prior_failed_artifacts': True}

patterns = {
    'token-prefix': re.compile(rb'(?:gh[opsu]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[A-Z0-9]{16})'),
    'private-key-header': re.compile(rb'-----BEGIN [A-Z ]*PRIVATE KEY-----'),
    'environment-secret-key': re.compile(rb'(?i)\benv\.[A-Za-z0-9_.-]*(?:password|passwd|secret|token|credential|api.?key)[A-Za-z0-9_.-]*\s*[=:]'),
    'bearer-token': re.compile(rb'(?i)\bAuthorization\s*[:=]\s*Bearer\s+[^\s<]{16,}'),
}
scan_paths = sorted(set(PROOF.glob('*.log')) | set(PROOF.glob('*.json')) | set(PROOF.glob('*.xml')))
matches = []
for path in scan_paths:
    body = read(path)
    for category, pattern in patterns.items():
        for match in pattern.finditer(body):
            matches.append({'path': path.name, 'line': body[:match.start()].count(b'\n') + 1, 'category': category})
require(not matches, 'Credential-shaped diagnostic content observed; never emit values')
for path, row in INPUTS.items():
    require(sha256(Path(path).read_bytes()).hexdigest() == row['sha256'], 'Reviewed input changed: ' + path)

result = {'schema': 'm3.independent-e03-executed-evidence-review.v1',
          'review_time_utc': datetime.now(timezone.utc).isoformat(),
          'scope': 'Read-only evidence inspection; no project code, recipe, build or test rerun',
          'status': 'PASS_METADATA_PROOF_WITH_CAPABILITY_COMPLETION_BLOCKED',
          'source_commit': receipt['source_publication_commit'],
          'destination_preimage_commit': receipt['destination_preimage_commit'],
          'plan_sha256': plan['plan_sha256'], 'steps': steps, 'installer_results': installer_results,
          'java': {**counts, 'testcase_count': len(cases), 'methods': java_methods,
                   'actual_compiled_owner_sources': 1, 'actual_compiled_test_sources': 1,
                   'copied_xml_matches_actual_build_bytes': True, 'loaded_resource_copies_byte_equal': len(resource_copies)},
          'python': {'tests': 6, 'passed': 6, 'methods': python_methods,
                     'negative_workspace_conditions': 44, 'modes_per_negative_condition': 3,
                     'refused_operation_calls': 132,
                     'count_basis': '8 preimage +14 guard +14 proper mixed +4 edited postimage +4 missing postimage conditions; full six-test PASS confirms loops with check/apply/rollback'},
          'frozen_input_stability': {'fixed': 166, 'external': 63, 'unchanged': 229,
                                     'sha256_git_blob_length_mode_match': True},
          'operational_outputs': outputs,
          'validation': {'mapping_records': 46, 'normal_validator_exit': 0, 'completion': 'INCOMPLETE',
                         'completion_exit': 2, 'completion_refusal_expected': True,
                         'blocked_records': 2, 'blocked_record_tests_empty': True,
                         'normal_validator_checks_blocked_target_recipe_evidence_hashes': False,
                         'explicit_reference_review': reference_review},
          'scope_limits': {'source_export_admitted': False, 'receiver_behavior_rerun': False,
                           'full_owning_module_gate_executed': False, 'new_JNI_execution': False,
                           'jdk_acceptance': False, 'remote_publication_reviewed_here': False,
                           'source_failure_still_required': '67 upstream tests:66 passed/1 error; later source JNI gates unrun'},
          'bounded_diagnostic_scan': {'files': len(scan_paths), 'categories': list(patterns), 'matches': matches},
          'all_reviewed_inputs_stable': True}
write('INSPECTION.json', result)
write('INPUT_SEALS.json', {'schema': 'm3.independent-e03-review-input-seals.v1',
                           'inputs': sorted(INPUTS.values(), key=lambda x: x['path'])})
print(json.dumps({'status': result['status'], 'steps': len(steps), 'java_tests': 11, 'python_tests': 6,
                  'negative_workspace_conditions': 44, 'refusal_calls': 132, 'frozen_inputs_unchanged': 229,
                  'outputs_equal_exact_after_bytes': 4, 'diagnostic_scan_files': len(scan_paths),
                  'inspected_inputs': len(INPUTS), 'reference_review_supplied': reference_review is not None}, indent=2))
