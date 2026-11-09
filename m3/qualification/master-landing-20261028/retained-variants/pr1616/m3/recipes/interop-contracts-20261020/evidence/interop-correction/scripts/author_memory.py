#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""One-off immutable task history authoring; not a deployed memory engine."""
from pathlib import Path
from datetime import datetime, timezone
import copy
import csv
import difflib
import hashlib
import io
import json
import re
import shlex
import shutil

ROOT = Path(__file__).resolve().parent.parent
WORK = Path(__file__).resolve().parent
OUT = WORK / 'memory-packet'
CRATE = ROOT / 'canonical-interop-contracts-20261020'
PREFIX = 'evidence/interop-correction/'
RECIPE = 'com.synexia.m3jdk.InteropContracts20261020'
HOME = 'synexia-openrewrite-recipes/recipe-crates/m3-interop-contracts-20261020'
BASE = '2779aba3f98f31c324be43f731be7914e1a05935'
SET13 = '05f397c6181992c354c6113aca7e2f9039bb8c835bef8e5c14a286ec526176eb'
COPYRIGHT = 'SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project'
HEADER = '<!-- ' + COPYRIGHT + ' -->\n<!-- SPDX-License-Identifier: Apache-2.0 -->\n\n'
exec((OUT / 'memory/PROFILE_REFERENCE.py.txt').read_text(), globals())
NOW = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
targets = json.loads((CRATE / 'provenance/TARGETS.json').read_text())['files']
by_name = {Path(t['path']).name: t for t in targets}

def sha(data):
    return hashlib.sha256(data).hexdigest()

def resolve(path):
    local = OUT / path
    return local if local.is_file() else CRATE / path

def put(path, data):
    target = OUT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, str):
        data = data.encode('utf-8')
    if target.exists():
        if target.read_bytes() != data:
            raise ValueError('Refusing immutable overwrite: ' + path)
    else:
        target.write_bytes(data)

def art(path=None, purpose='', status='UNKNOWN'):
    if path is None:
        return {'path': None, 'sha256': None, 'status': status, 'purpose': purpose}
    data = resolve(path).read_bytes()
    return {'path': path, 'sha256': sha(data), 'status': 'EXACT', 'purpose': purpose}

def ev(name, purpose='Retained attempt evidence'):
    return art(PREFIX + name, purpose)

def resource(name, pre=False):
    t = by_name[name]
    path = 'src/main/resources/com/synexia/rewrite/hash-pinned-' + t['lane'] + '/m3-interop-contracts-20261020/'
    path += ('pre/' if pre else '') + t['resource']
    result = art(path, 'Exact target ' + ('preimage' if pre else 'postimage') + ': ' + t['path'])
    assert result['sha256'] == t['before_sha256' if pre else 'after_sha256']
    return result

def atom(name, contract, before=None, after=None, dependencies=()):
    return {'atom_id': Path(name).stem, 'owner': by_name[name]['path'], 'contract': contract,
            'before': before or resource(name, True), 'after': after or resource(name),
            'dependencies': list(dependencies)}

def trigger(name, needle, expected, scope):
    path = PREFIX + name
    candidates = [line for line in resolve(path).read_text().splitlines() if needle in line]
    if not candidates:
        raise ValueError('Missing literal trigger ' + name + ': ' + needle)
    return {'artifact': art(path, 'Literal observed trigger retained without relabelling'),
            'observed_line': candidates[0], 'expected': expected, 'observation_scope': scope}

def commands(*names):
    result = []
    def walk(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key == 'command' and isinstance(child, list) and all(isinstance(x, str) for x in child):
                    result.append(shlex.join(child))
                else:
                    walk(child)
        elif isinstance(value, list):
            for child in value:
                walk(child)
    for name in names:
        walk(json.loads(resolve(PREFIX + name).read_text()))
    return list(dict.fromkeys(result))

def diff_for(attempt, atoms):
    chunks = []
    for a in atoms:
        before, after = a['before'], a['after']
        if before['status'] == after['status'] == 'EXACT':
            old = resolve(before['path']).read_text().splitlines(keepends=True)
            new = resolve(after['path']).read_text().splitlines(keepends=True)
            chunks += difflib.unified_diff(old, new, fromfile='before/' + a['owner'], tofile='after/' + a['owner'])
    if not chunks:
        return art(purpose='No source mutation in this evidence-only attempt; preserved inputs are separately sealed', status='ABSENT')
    path = 'memory/diffs/' + attempt + '.patch'
    put(path, ''.join(chunks))
    return art(path, 'Mechanically derived exact per-file before/after difference')

EVENTS = []
ENVIRONMENT = ('Java 21.0.12.1+1; Maven 3.9.9; unchanged compiler 3.13.0, '
               '-proc:none -Xlint:all,-processing -Werror; Surefire 3.2.5; '
               '-ea -Xcheck:jni; pinned local M3 collection JAR and 11 native library seals in INPUTS.json. '
               'No POM, product owner, native implementation, or strict gate changed by this follow-up.')

def add(attempt, kind, category, atoms, observed, cause, change, lesson,
        status, outcome, evidence, fixtures=(), command_files=(), rejected=(),
        supersedes=(), root_status='ESTABLISHED', matches=(), exclusions=()):
    argv = commands(*command_files)
    e = {
      'schema': 'MOP-RCM/1.0', 'event_schema': 'm3-interop-contracts-correction-event/1',
      'attempt_id': attempt, 'event_type': kind, 'recipe_id': RECIPE,
      'recorded_utc': NOW, 'correction_utc': None,
      'correction_utc_status': 'UNKNOWN exact authoring instant; original execution timestamps remain in retained logs/receipts',
      'authors': ['Hitesh Soliwal and Contributors to the Synexia Project'], 'license': 'Apache-2.0',
      'repository': 'hsoliwal/com.synexia', 'canonical_path': HOME,
      'target_repository': 'hsoliwal/M3JDK21', 'target_source_revision': BASE,
      'publication_commit': None,
      'publication_status': 'HISTORICAL_ATTEMPT_CAPTURE; canonical publication/receiving observations are appended separately',
      'recipe_versions': {
        'before': art(purpose='UNKNOWN complete historical SDK input closure: this diagnostic attempt preceded canonical follow-up qualification; exact target preimages retained'),
        'after': art(purpose='UNKNOWN complete SDK input closure at this historical attempt; exact candidate targets and later local qualification are retained separately'),
        'after_version': 'm3-interop-contracts-20261020/' + attempt,
        'dependency_artifacts': [ev('INPUTS.json', 'Owning JAR/native/input SHA256 dependency pins'), art('catalogue/DONOR_EVALUATION_20261020.json', 'Pinned donor compatibility evaluation')]
      },
      'atoms': atoms,
      'pattern': {'category': category,
          'matching': list(matches) or ['Exact sealed owning source/test preimage and recorded failure signature', 'Per-file replacement only after the matching contract is established'],
          'exclusions': list(exclusions) or ['Unpinned donor versions', 'Public owner/API changes', 'Suppressing warnings, assertions, native requirements, or failIfNoTests'],
          'refusal': 'Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.',
          'catalogue': ['catalogue/INTEROP_EVALUATION_20261020.tsv', 'catalogue/DONOR_EVALUATION_20261020.json', 'catalogue/PRIOR_PROBLEMS_20261020.tsv']},
      'trigger': observed,
      'root_cause': {'status': root_status, 'explanation': cause},
      'change': change, 'lesson': lesson,
      'rejected_approaches': [{'approach': a, 'reason': b} for a, b in rejected],
      'verification': {'status': status, 'outcome': outcome, 'environment': ENVIRONMENT,
          'commands': argv, 'evidence': [ev(x) for x in evidence]},
      'regression_fixtures': [art(x, 'Retained exact fixture or independent probe') for x in fixtures],
      'replay': {'status': 'BOUNDED_BY_RETAINED_INPUTS', 'commands': argv,
          'limitations': 'Original scratch paths in argv are historical. Recreate an isolated source/POM closure and use the pinned dependencies; do not reuse stale target classes. Exact earlier full SDK closures and exact mutation instants were not captured. This record is not an executable generic transformation engine.'},
      'source_diff': diff_for(attempt, atoms),
      'parents': [EVENTS[-1]['digest']] if EVENTS else [],
      'supersedes': sorted(EVENTS[i - 1]['digest'] for i in supersedes),
      'admission': {'status': 'NO_ADMISSION_FROM_HISTORICAL_CAPTURE',
          'scope': 'Only the explicitly reported diagnostic/source/local-owner verification result is asserted; canonical SDK and actual receiving admission remain separate observations.',
          'pending_gates': ['Canonical unchanged five-test SDK qualification at the actual final input closure', 'Actual product receiving, zero-change repeat, and owning Java/native verification', 'Canonical publication provenance and remote readback'],
          'merge_authority': False, 'phase_promotion': False},
      'documentation': 'memory/README.md;memory/LESSONS.md;RECIPE_MEMORY.tsv'
    }
    schema_check(e, SCHEMA)
    digest = sha(raw(e))
    put('memory/events/' + digest + '.json', raw(e))
    EVENTS.append({'event': e, 'digest': digest})

# Preserve exact current independent reviews without altering their historical statuses.
for name in ['CANONICAL_13_TARGET_CUSTODY_20261020.md', 'CANONICAL_13_TARGET_CUSTODY_20261020.json',
             'PUBLICATION_ADAPTER_REVIEW_20261020.md', 'PUBLICATION_ADAPTER_REVIEW_20261020.json']:
    source = WORK / 'independent' / name
    if source.is_file():
        put(PREFIX + 'independent/' + name, source.read_bytes())

donors = {
 'schema': 'm3-interop-donor-evaluation/1', 'recipe': RECIPE,
 'selection_scope': 'Contract-preserving test/metadata correction only; no donor production source copied, no new production algorithm or runtime integration layer.',
 'commons_collections': {
  'coordinate': 'org.apache.commons:commons-collections4:4.5.0', 'license': 'Apache-2.0',
  'jar_sha256': '00f93263c267be201b8ae521b44a7137271b16688435340bf629db1bac0a5845',
  'repository': 'https://github.com/apache/commons-collections', 'revision': '7f7fb0244abc940a2e9dd28b67508c0483a58c3e',
  'sources': [
    {'path': 'src/main/java/org/apache/commons/collections4/Bag.java', 'git_blob': 'b06061a016d4a4f1dc43e0ba0803e42ca94b6506'},
    {'path': 'src/main/java/org/apache/commons/collections4/bag/AbstractMapBag.java', 'git_blob': '9da508f625f5ab6050c56b2da465e8aac4e448e2'}],
  'contract': 'M3ApacheBag explicitly retains min(ownedCount,inputCount), consistent with cardinality wording in the pinned Bag interface.',
  'comparison': 'Pinned HashBag removes a key when retained input count exceeds the owned count. Unconditional implementation parity conflicts with the explicit M3 contract.',
  'selected': 'Keep M3 owner unchanged; consume independent input occurrences to derive the expected retain result; keep real HashBag comparisons on the equivalent domain and add an explicit owned2/input3 divergence fixture.',
  'rejected': 'Changing M3 to reproduce the pinned HashBag quirk; skipping excess-multiplicity input; allowing a diverged donor state to contaminate later random operations.',
  'correctness_evidence': ['apache-review/APACHE_PROBE_RECEIPT.json', 'apache-review/apache-contract-probe.log', 'reports-13/java/TEST-com.m3.collections.interop.M3ApacheBagContractTest.xml'],
  'complexity': 'Independent fixture occurrence budget may take O(n*m) time and O(m+n) space for small bounded test states. It deliberately does not clone the production counted algorithm; no runtime speed claim.'
 },
 'spring': {
  'coordinate': 'org.springframework:spring-core:6.2.0', 'license': 'Apache-2.0',
  'jar_sha256': '3b82629da38717dd616a70ec1055f9a8e208d58132071a6314a32159f8bce162',
  'repository': 'https://github.com/spring-projects/spring-framework', 'revision': '5024bb72279188f1d0dcfe19e8abdd4bdb9887c8',
  'source': {'path': 'spring-core/src/main/java/org/springframework/util/MultiValueMapAdapter.java', 'git_blob': '4c7c2f1d133cccef1eab060524a71cd3de4b45a9'},
  'contract': 'Both adapters preserve caller-provided fixed-size list behavior and may throw UnsupportedOperationException when an add operation attempts to grow it.',
  'selected': 'Execute both counterparts for the three void add branches; compare RuntimeException class or successful null outcome; preserve all seeded operations, return/state checks, and fixed-list inputs.',
  'rejected': 'Copying fixed-size inputs into mutable lists, suppressing errors, discarding failing operations, changing production semantics, or retaining the larger all-operations Outcome helper.',
  'correctness_evidence': ['independent/SPRING_RECEIPT.json', 'independent/spring-1.log', 'reports-13/native/TEST-com.m3.collections.interop.M3SpringArrayContractTest.xml'],
  'complexity': 'Only test observation changes; one execution of each donor/owner operation. No production complexity change or measured performance claim.',
  'derived_evidence': {'path': PREFIX + 'independent/Spring-MultiValueMapAdapter-javap.log', 'sha256': '9a3e94a883baa9c858394822323eaedd1fe9659c36506401c7662de1fb8555aa', 'source_path': 'org/springframework/util/MultiValueMapAdapter.class', 'notice': 'licenses/Spring-6.2.0-NOTICE.txt', 'license_text': 'licenses/Spring-6.2.0-LICENSE.txt'}
 },
 'synexia_reuse': {
  'production': 'Existing M3LibraryCollections factories, M3LibraryBindings resource loader, M3ArrayifiedIndexOps stable merge, M3IndexPathMasks/M3IndexMaskCompactor and M3 Apache/Spring owners remain unchanged by these13 targets.',
  'transformation': 'Existing canonical four engines, M3TaskRecipeTest and receive.py are reused byte-for-byte; new named recipe identity and exact13 target resources only.',
  'owner_source_revision_note': 'Seven of eight copied owner-source witnesses equal baseline2779; the received main recipe M3IndexMaskCompactor witness is pinned at d1b9aa9f35cf6f8cf7af1fb581471c2551bd7e97. The finite evidence source-origin map records exact revisions.'
 },
 'catalogue_discovery': {
  'prior_catalogue': 'catalogue/PRIOR_PROBLEMS_20261020.tsv',
  'sources': ['GitHub', 'LeetCode', 'HackerRank', 'GeeksforGeeks'],
  'scope': 'Reuse the existing task catalogue problem taxonomy and links. LeetCode/HackerRank/GeeksforGeeks approaches here remain metadata-only candidates, not newly copied challenge solutions, source licenses, or performance evidence.',
  'selection': 'The observed defects are compilation, contract oracles, stable ordering, backend applicability, and factual metadata. Existing exact owners and pinned primary donor contracts settle these cases; no unrelated algorithm substitution is justified.'
 },
 'performance_status': 'NOT_BENCHMARKED; no product speedup is claimed',
 'evidence_prefix': PREFIX
}
put('catalogue/DONOR_EVALUATION_20261020.json', json.dumps(donors, indent=2) + '\n')

split = art('memory/fixtures/attempt-01-split-var.java.txt', 'Split-only source mechanically reconstructed from exact preimage and checked against the originally captured SHA256')
add('I01', 'FAILED_CANDIDATE', 'java/local-type-inference/strict-compilation',
 [atom('M3FiveLibraryArrayificationTest.java', 'Preserve both slice variables and evaluation order; Java var has one declarator.', after=split)],
 trigger('baseline-red.log', "'var' is not allowed in a compound declaration", 'Owning tests compile under unchanged Java21 strict compiler settings.', 'Original product owning-module compilation'),
 'The source used a compound local var declaration forbidden by Java syntax.',
 'Split the declaration into two adjacent var statements, preserving order and expressions.',
 'Fix the earliest owning compile error and rerun the same strict gate; a parse fix can expose independent downstream defects.',
 'FAIL_COMPILE_OTHER_DEFECTS', 'Split-only candidate removed the parse error but exposed deprecated Predicate under Werror, a missing Map import, and a nonexistent factory class.',
 ['baseline-red.log', 'attempt-01-split-var-red.log', 'INPUTS.json'],
 fixtures=['memory/fixtures/attempt-01-split-var.java.txt'], command_files=['candidate-command.json'],
 rejected=[('Disable Werror or skip test compilation', 'Would weaken the existing owning contract gate.')])

add('I02', 'FAILED_CANDIDATE', 'java/api-resolution/strict-compilation',
 [atom('M3ApacheCollectionsTest.java', 'Retain predicate meaning without deprecated explicitly typed Commons Predicate.', dependencies=['Existing M3LibraryCollections predicate factory']),
  atom('M3FiveLibraryKeyRangesTest.java', 'Use java.util.Map for the existing entry operation.'),
  atom('M3FiveProviderFlagSelectionTest.java', 'Use the existing springMultiValueMap public factory.', dependencies=['M3LibraryCollections'])],
 trigger('attempt-01-split-var-red.log', '[ERROR]', 'All existing test sources compile with strict warnings retained.', 'Split-only owning compile failure'),
 'Tests referenced one deprecated functional interface, omitted a required import, and referred to a class absent from the current owner API.',
 'Use a local nonnegative method reference, import Map, and call the existing M3LibraryCollections factory; no owner or build changes.',
 'Resolve against the compiled current public owner before proposing a missing runtime implementation.',
 'COMPILE_PASS_RUNTIME_FAIL', 'The unchanged owning gate compiled32 main and44 test sources, then ran158 tests with6 failures and1 error; all are retained.',
 ['attempt-01-split-var-red.log', 'attempt-02-compile-fixes.log'],
 rejected=[('Add a replacement M3FiveProviderDirectSdk class or stub', 'The current existing M3LibraryCollections API already supplies the operation.')])

correction_atoms = [
 atom('M3FiveLibraryArrayificationTest.java', 'Equal keys in stable merging consume the left run before the right run.', before=split),
 atom('M3GuavaListMultimapContract.java', 'Native-required selection succeeds and agrees when available, and fails closed when absent.'),
 atom('M3LibraryClosureTest.java', 'Factory-produced Spring instance has the actual existing array owner class; separately explicit old-owner tests remain.'),
 atom('M3LibraryBindingsSelfTest.java', 'Exact existing binding corpus is25; added valid overload yields26; original bad-input assertions remain.'),
 atom('M3SdkFamiliesTest.java', 'All25 existing resource declarations and owner classes must be discoverable.'),
 atom('M3SpringCaseInsensitiveSdkFactoryTest.java', 'Retain all Spring case/factory assertions with the exact current25 binding total.'),
 atom('SDK_FAMILIES.md', 'Describe all existing25 bindings and previously omitted GuavaBiMap and ApacheBidiMap.', after=ev('attempt-08-metadata/SDK_FAMILIES-12-target.md'))]
add('I03', 'CANDIDATE_REFINEMENT', 'test-oracle/stability-backend-and-owner-metadata', correction_atoms,
 trigger('attempt-02-compile-fixes.log', 'Tests run: 158, Failures: 6, Errors: 1', 'Existing owners satisfy their declared contracts under applicable backend conditions.', 'First fully compiled owning test run'),
 'Several assertions described outdated metadata, assumed native absence during a native-enabled run, or interleaved equal runs contrary to the explicit stable merge contract.',
 'Correct the precise expected order, conditional native contract, factory class and current binding counts/overview. Preserve the original random corpus and every applicable assertion.',
 'Classify a discrepancy against the exact owner contract and environment before changing production behavior.',
 'PARTIAL_NOT_QUALIFIED', 'Attempt03 still failed3 tests; two exercised stale classes and one exposed a real donor/contract mismatch. Later fresh12 and fresh13 receipts validate these exact final fixtures.',
 ['attempt-02-compile-fixes.log', 'attempt-03-fixture-fixes-native.log', 'attempt-03-source-bytecode/snapshot.json', 'attempt-05-fresh-native.json', 'attempt-09-fresh-java.json'],
 rejected=[('Sort or weaken the stable-order assertion', 'Would conceal the explicit stable-left-run contract.'), ('Skip the native-required branch', 'Both available and absent modes are part of the existing contract.')])

spring_initial = ev('attempt-03-spring-postimage.java', 'Retained larger Outcome-helper attempt, later superseded')
add('I04', 'SUPERSEDED_CANDIDATE', 'differential-testing/exception-and-borrowed-list-contract',
 [atom('M3SpringArrayContractTest.java', 'Observe both donor and M3 outcomes for fixed-size borrowed lists without altering the inputs.', after=spring_initial)],
 trigger('attempt-02-compile-fixes.log', 'M3SpringArrayContractTest.realSpringSixTwoDifferentialSeeded', 'Both sides execute so the test can compare legitimate RuntimeException outcomes.', 'Original seeded4500-operation Spring oracle stopped at first UOE'),
 'At step23 addAll attempts to grow a borrowed fixed-size list. Both existing M3 and pinned Spring6.2 throw UOE; the old fixture stopped before running the counterpart.',
 'First attempt wrapped all13 random operations in a general Outcome helper and compared outcome types.',
 'Retain failed and superseded fixture designs even when a narrower observation helper is selected.',
 'PARTIAL_PASS_SUPERSEDED', 'Spring four-test group passed in attempt03 and independent4500-operation probe observed209 matching UOEs; the full attempt03 suite did not qualify.',
 ['attempt-03-fixture-fixes-native.log', 'independent/SPRING_RECEIPT.json', 'independent/spring-1.log'],
 fixtures=[PREFIX + 'attempt-03-spring-postimage.java', PREFIX + 'independent/SpringExceptionParityProbe.java'])

add('I05', 'VALIDATED_REFINEMENT', 'differential-testing/minimal-exception-observer',
 [atom('M3SpringArrayContractTest.java', 'Compare RuntimeException class for only the three void add branches; preserve all random draws, return checks and post-state checks.', before=spring_initial)],
 trigger('independent/SPRING_DIAGNOSIS.md', 'UnsupportedOperationException', 'Use the smallest observation change that executes both legitimate outcomes.', 'Independent pinned-donor diagnosis of original seeded failure'),
 'The general Outcome helper was broader than needed; only add, addIfAbsent and addAll void branches require paired exception observation.',
 'Replace the larger helper with failureOf and assertSameFailure for branches0/2/4; preserve Error propagation and all other assertions verbatim.',
 'Minimize the changed behavioral atom after the contract is understood; exception parity adds coverage without mutating borrowed inputs.',
 'LOCAL_OWNER_VALIDATED', 'Both final fresh Java/native159-test suites pass; the independent probe retains4500 original operations and209 matching UOEs.',
 ['independent/SPRING_RECEIPT.json', 'independent/spring-1.log', 'attempt-05-fresh-java.json', 'attempt-09-fresh-native.json'],
 fixtures=[resource('M3SpringArrayContractTest.java')['path'], PREFIX + 'independent/SpringExceptionParityProbe.java'],
 supersedes=[4], rejected=[('Convert supplied fixed-size lists to mutable copies', 'Changes the borrowed-list behavior under test.'), ('Catch all Throwable or drop operations', 'Would suppress real defects and alter the original corpus.')])

add('I06', 'VALIDATED_REFINEMENT', 'multiset/retention-cardinality/independent-oracle',
 [atom('M3ApacheBagContractTest.java', 'M3 retention keeps min(owned,input); preserve original6000 random operations and all state/count/equality/hash checks.')],
 trigger('attempt-02-compile-fixes.log', 'size step=702 ==> expected: <0> but was: <2>', 'The existing M3 cardinality contract and explicit pinned donor divergence are both verified.', 'Original unconditional HashBag oracle'),
 'Commons4.5 HashBag removes a key when retained input multiplicity exceeds owned multiplicity; M3 explicitly keeps min(owned,input). At step702 owned6=2 and input[6,6,6] demonstrate the difference.',
 'Derive retain results by consuming an independent occurrence budget, compare a fresh donor on the equivalent domain, carry the contract-derived state forward, and add the exact owned2/input3 divergence fixture.',
 'A donor is a candidate whose compatibility must be checked; preserve the established M3 contract instead of copying an implementation quirk.',
 'LOCAL_OWNER_VALIDATED', 'Independent original6000 operations:51079 checks;294 exhaustive retention cases with90 explicit donor divergences. Final159 tests pass in both modes; the added fixture accounts for the158-to159 increase.',
 ['apache-review/APACHE_PROBE_RECEIPT.json', 'apache-review/apache-contract-probe.log', 'apache-review/DONOR_SOURCE_PINS.json', 'attempt-09-fresh-native.json', 'attempt-09-fresh-java.json'],
 fixtures=[resource('M3ApacheBagContractTest.java')['path'], PREFIX + 'apache-review/ApacheBagContractProbe.java'],
 rejected=[('Modify production M3 retainAll to match HashBag quirk', 'Violates the explicit existing M3 contract.'), ('Skip excess-count cases or retain a diverged donor as the next reference state', 'Loses coverage or contaminates subsequent comparisons.')])

add('I07', 'REJECTED_QUALIFICATION', 'verification/source-class-custody', [],
 trigger('attempt-03-fixture-fixes-native.log', 'Tests run: 158, Failures: 3', 'A clean owning gate must execute classes compiled from the sealed intended sources.', 'Observed attempt03 full run'),
 'The Arrayification and Guava class hashes/mtimes and javap output reflect earlier fixture source despite new source hashes and logged compilation. The external mechanism remains unresolved.',
 'Reject attempt03 as qualification, retain its failure log, exact source/class metadata and disassembly, and inspect actual compiled artifacts before any pass claim.',
 'A clean-build banner is insufficient when artifact evidence conflicts; source and class custody are separate facts.',
 'REJECTED_STALE_ARTIFACTS', 'Attempt03 remains158 tests/3 failures. Original compiled binary bodies remain in scratch; durable packet preserves their SHA256/mtime metadata, relevant source bodies and javap output.',
 ['attempt-03-fixture-fixes-native.log', 'attempt-03-source-bytecode/snapshot.json', 'attempt-03-source-bytecode/M3FiveLibraryArrayificationTest-javap.log', 'attempt-03-source-bytecode/M3GuavaListMultimapContract-javap.log'],
 root_status='PARTIAL_MECHANISM_UNRESOLVED', rejected=[('Relabel this run as passed based on intended source text', 'It demonstrably exercised different class bytes.'), ('Claim a proven Maven cache bug', 'The external cause was not established.')])

add('I08', 'COMPILE_ONLY_OBSERVATION', 'verification/fresh-compiler-output', [],
 trigger('attempt-04-verbose-recompile.log', 'BUILD SUCCESS', 'Strict verbose test-compile emits current fixture bytecode while source hashes stay fixed.', 'Compile-only adaptive follow-up'),
 'The prior stale artifact observation required direct recompilation and bytecode inspection; a full runtime conclusion does not follow from compilation alone.',
 'Run unchanged strict clean test-compile with verbose compiler output, inspect the affected class bodies and compare unchanged source hashes.',
 'State the scope of each successful gate precisely; compile-only success cannot erase a prior runtime failure.',
 'PASS_COMPILE_ONLY', 'Updated Arrayification/Guava bytecode is observed and source hashes remain stable; no full-suite pass is attributed to attempt04.',
 ['attempt-04-verbose-recompile.json', 'attempt-04-verbose-recompile.log'], command_files=['attempt-04-verbose-recompile.json'],
 rejected=[('Use compile-only success as owning verification', 'The full corpus still had to execute against fresh classes.')])

add('I09', 'ENVIRONMENT_BLOCKED_ATTEMPT', 'verification/capacity-refusal-and-isolated-output', [],
 trigger('ENVIRONMENT_REFUSALS.json', 'No space left on device', 'An authoring or cleanup command must actually execute before its result is claimed.', 'Retrospectively recorded tool refusal, original stdout file unavailable'),
 'The sandbox mount setup refused two commands with exit101 during zero available disk capacity. Scoped scratch cleanup succeeded but old target files later reappeared; that mechanism is unresolved.',
 'Retain the refused command observations; clean only authorized reproducible scratch output, retain installed toolchain, and move qualification to never-used directories once capacity is available.',
 'A pre-execution capacity refusal is not a Java test failure or a successful mutation. Never infer a cache repair from unexplained file reappearance.',
 'BLOCKED_THEN_RETRIED_SEPARATELY', 'Retained exact diagnostic fields identify the two unexecuted commands and later cleanup observation. Fresh-path qualifications are separate later attempts.',
 ['ENVIRONMENT_REFUSALS.json', 'attempt-03-source-bytecode/snapshot.json'], root_status='CAPACITY_ESTABLISHED_FILE_REAPPEARANCE_UNRESOLVED',
 rejected=[('Delete product or unrelated workspace files', 'Not required or authorized by this isolated task.'), ('Claim the failed authoring command changed source', 'The tool refused before command execution.')])

add('I10', 'VALIDATED_LOCAL_CANDIDATE', 'verification/fresh-java-jni-corpus', [],
 trigger('attempt-05-fresh-native.log', 'Tests run: 159, Failures: 0, Errors: 0, Skipped: 0', 'All existing owning tests and the added donor-divergence fixture execute on fresh sealed classes in both modes.', 'Frozen12 target local candidate verification'),
 'Fresh directory isolation and explicit source/class checks remove ambiguity in this run without claiming to explain or fix the earlier external artifact mechanism.',
 'Create never-used native and Java directories, copy only the required owning source/POM/resources, assert no preexisting target/classes, run clean verify, seal all sources/classes, then run existing standalone binding and Guava backend corpora.',
 'Freeze exact source inputs and verify actual backend availability before qualifying a reused implementation.',
 'PASS_LOCAL_12_TARGETS', 'Each mode159 tests/0 failures/errors/skips; standalone bindings152 checks across5 families/25 bindings; observed both native availability flags true in native and false in Java, with existing Guava26421 checks/6400 steps in each.',
 ['FROZEN_FRONTIER.json', 'attempt-05-fresh-native.json', 'attempt-05-fresh-java.json', 'attempt-06-bindings-standalone.json', 'attempt-07-backend-observations.json'],
 fixtures=[PREFIX + 'independent/BackendAvailabilityProbe.java'],
 command_files=['attempt-05-fresh-native.json', 'attempt-05-fresh-java.json', 'attempt-06-bindings-standalone.json', 'attempt-07-backend-observations.json'])

add('I11', 'REVIEW_HOLD', 'metadata/factory-owner-source-consistency', [],
 trigger('independent/CANDIDATE_REVIEW_20261020.md', 'M3LinkedList', 'Guava owner metadata must agree with the unchanged factory implementation.', 'Independent12-target review; adjacent metadata scope initially held'),
 'The current factory creates Guava list-multimap values with M3ArrayList::new while both overview and TSV named M3LinkedList. The initial12 candidate left that existing mismatch uncorrected.',
 'Retain the12-target review and qualification unchanged; record the concrete source witness and await the separately authorized scope extension.',
 'A passing test set does not make an incorrect owner description true; record the discrepancy and explicit successor scope.',
 'HOLD_METADATA_SCOPE_AT_CAPTURE', 'Exact12 candidate source review is retained, including the historical metadata finding; final13 successor handles it separately.',
 ['independent/CANDIDATE_REVIEW_20261020.json', 'independent/CANDIDATE_REVIEW_20261020.md', 'attempt-08-metadata/SOURCE_WITNESS.json'],
 rejected=[('Call the existing metadata accurate because the tests pass', 'The exact factory line contradicts it.')])

add('I12', 'AUTHORIZED_METADATA_SUCCESSOR', 'metadata/factory-owner-source-consistency',
 [atom('SDK_FAMILIES.md', 'Describe the actual existing Guava value factory M3ArrayList::new.', before=ev('attempt-08-metadata/SDK_FAMILIES-12-target.md')),
  atom('sdk-family-bindings.tsv', 'Change only the GUAVA/guavaListMultimap owner cell; preserve25 entries, order, factory signatures, versions and all other fields.')],
 trigger('attempt-08-metadata/SOURCE_WITNESS.json', 'return Multimaps.newListMultimap', 'The two owner descriptions agree with the unchanged compiled factory.', 'Exact source witness for authorized13-target successor'),
 'Only the metadata named the former list owner; the current implementation and resource loader already provide the correct behavior.',
 'After explicit task scope extension, change M3LinkedList to M3ArrayList in the single TSV owner cell and corresponding overview label, and freeze a13-target successor while preserving12-target attempts.',
 'Correct factual source metadata mechanically; never add a new owner or change runtime behavior to fit outdated prose.',
 'SOURCE_REVIEW_PASS_RUNTIME_PENDING_AT_CAPTURE', 'The independent13 metadata review verifies all11 Java postimages unchanged from12, one TSV cell plus one overview owner label changed, and exact source/factory/resource loading witnesses. Later final13 receipts contain runtime requalification.',
 ['FROZEN_FRONTIER.json', 'FROZEN_FRONTIER_13.json', 'attempt-08-metadata/SOURCE_WITNESS.json', 'independent/CANDIDATE_REVIEW_13_METADATA_20261020.json'],
 supersedes=[11], rejected=[('Change M3LibraryCollections.guavaListMultimap to M3LinkedList', 'Would modify production behavior solely to satisfy obsolete metadata.')])

add('I13', 'VALIDATED_LOCAL_SUCCESSOR', 'verification/exact-expanded-closure', [],
 trigger('attempt-09-fresh-native.log', 'Tests run: 159, Failures: 0, Errors: 0, Skipped: 0', 'The exact13-target successor is requalified on fresh owning Java/native classes and packaged metadata.', 'Final13 local qualification; no canonical admission inferred'),
 'The extra TSV resource changes the final input closure, so prior12 qualification is preserved as history and the exact13 successor is verified independently.',
 'Run the same owning clean verify in new native and Java directories; repeat standalone binding and observed backend/Guava checks in both modes; compare packaged TSV to its final source and class hashes to the12-target qualification.',
 'A metadata successor must carry its own exact closure and receipts, even when all Java class bytes remain identical.',
 'PASS_LOCAL_13_TARGETS', 'Each mode159 tests/0 failures/errors/skips; each binding run152 checks/5 families/25 bindings; each Guava corpus26421 checks/6400 steps with asserted backend availability. All Java class hashes match the previous12 build; packaged TSV SHA95075c5f... matches the final resource.',
 ['FROZEN_FRONTIER_13.json', 'attempt-09-fresh-native.json', 'attempt-09-fresh-java.json', 'attempt-10-final13-additional-gates.json', 'independent/CANONICAL_13_TARGET_CUSTODY_20261020.json'],
 command_files=['attempt-09-fresh-native.json', 'attempt-09-fresh-java.json', 'attempt-10-final13-additional-gates.json'])

def strict_load(data):
    def pairs(items):
        value = {}
        for key, child in items:
            if key in value:
                raise ValueError('duplicate JSON key')
            value[key] = child
        return value
    def reject_constant(value):
        raise ValueError('nonfinite JSON constant')
    return json.loads(data.decode('utf-8'), object_pairs_hook=pairs, parse_constant=reject_constant)

def validate(e, known):
    schema_check(e, SCHEMA)
    if e['schema'] != 'MOP-RCM/1.0' or e['event_schema'] != 'm3-interop-contracts-correction-event/1':
        raise ValueError('unsupported event version')
    for key in ('parents', 'supersedes'):
        if e[key] != sorted(set(e[key])):
            raise ValueError('nonunique or unsorted links')
        if any(not re.fullmatch('[0-9a-f]{64}', x) or x not in known for x in e[key]):
            raise ValueError('unknown parent/supersedes')
    for a in walk_artifacts(e):
        if a['status'] == 'EXACT':
            if not a['path'] or not a['sha256'] or sha(resolve(a['path']).read_bytes()) != a['sha256']:
                raise ValueError('artifact hash mismatch')
        elif a['status'] not in ('UNKNOWN', 'ABSENT') or a['path'] is not None or a['sha256'] is not None:
            raise ValueError('invalid unknown/absent artifact')
    tr = e['trigger']
    if tr['observed_line'] not in resolve(tr['artifact']['path']).read_text().splitlines():
        raise ValueError('unobserved trigger line')
    if e['admission']['phase_promotion'] or e['admission']['merge_authority']:
        raise ValueError('history cannot grant admission')
    raw(e)

checks = []
def check(name, f):
    assert f(), name
    checks.append({'name': name, 'result': 'PASS'})

def rejected(f):
    try:
        f()
    except (ValueError, TypeError, UnicodeError):
        return True
    return False

check('all32 control codepoints use six-character lowercase escapes', lambda: raw(''.join(map(chr, range(32)))) == ('"' + ''.join('\\u%04x' % i for i in range(32)) + '"').encode())
check('quote backslash escaped; solidus literal', lambda: raw('"\\/') == b'"\\"\\\\/"')
check('Unicode scalars retained without normalization', lambda: raw('é/e\u0301/😀') == '"é/e\u0301/😀"'.encode())
check('signed64 extremes accepted', lambda: raw([-(1 << 63), (1 << 63) - 1]) == b'[-9223372036854775808,9223372036854775807]')
for value, name in [('\ud800', 'high surrogate'), ('\udfff', 'low surrogate'), (1 << 63, 'int64 overflow'), (-(1 << 63) - 1, 'int64 underflow'), (1.0, 'float'), (float('nan'), 'NaN'), (float('inf'), 'infinity'), ({'é': 1}, 'nonASCII key')]:
    check('reject ' + name, lambda value=value: rejected(lambda: raw(value)))
check('recursive ASCII key ordering', lambda: raw({'z': {'b': 1, 'a': 2}, 'a': 0}) == b'{"a":0,"z":{"a":2,"b":1}}')
check('duplicate key parse refused', lambda: rejected(lambda: strict_load(b'{"a":1,"a":2}')))
check('negative zero is noncanonical on readback', lambda: raw(strict_load(b'-0')) != b'-0')
check('short control escape is noncanonical on readback', lambda: raw(strict_load(b'"\\n"')) != b'"\\n"')
check('BOM refused', lambda: rejected(lambda: strict_load(b'\xef\xbb\xbf{}')))
known = set()
for record in EVENTS:
    e, digest = record['event'], record['digest']
    data = (OUT / ('memory/events/' + digest + '.json')).read_bytes()
    validate(e, known)
    assert data == raw(strict_load(data)) and sha(data) == digest and not data.endswith(b'\n')
    known.add(digest)
check('all13 event schema/canonical bytes/hash/known links/artifact seals/literal triggers', lambda: len(known) == 13)
sample = copy.deepcopy(EVENTS[-1]['event'])
for name, mutate in [
 ('unknown top-level field', lambda x: x.update(extra=True)),
 ('unknown nested field', lambda x: x['pattern'].update(extra=True)),
 ('unknown event version', lambda x: x.update(event_schema='unknown')),
 ('missing parent', lambda x: x.update(parents=['0' * 64])),
 ('duplicate parents', lambda x: x.update(parents=[EVENTS[0]['digest']] * 2)),
 ('unsorted parents', lambda x: x.update(parents=sorted([EVENTS[0]['digest'], EVENTS[1]['digest']], reverse=True))),
 ('duplicate supersedes', lambda x: x.update(supersedes=[EVENTS[0]['digest']] * 2)),
 ('artifact tampering', lambda x: x['trigger']['artifact'].update(sha256='0' * 64))]:
    altered = copy.deepcopy(sample)
    mutate(altered)
    check('reject ' + name, lambda altered=altered: rejected(lambda: validate(altered, known)))

schema = json_schema(SCHEMA)
schema.update({'$schema': 'https://json-schema.org/draft/2020-12/schema', '$id': 'urn:synexia:m3-interop-contracts-correction-event:1', 'title': 'Bounded interop recipe correction event'})
schema['properties']['schema']['const'] = 'MOP-RCM/1.0'
schema['properties']['event_schema']['const'] = 'm3-interop-contracts-correction-event/1'
for key in ('parents', 'supersedes'):
    schema['properties'][key].update({'uniqueItems': True, 'items': {'type': 'string', 'pattern': '^[0-9a-f]{64}$'}})
put('memory/EVENT.schema.json', json.dumps(schema, indent=2) + '\n')
put('memory/EVENTS.sha256.tsv', 'attempt_id\tevent_sha256\tpath\tparents\tsupersedes\n' + ''.join(
    '\t'.join([r['event']['attempt_id'], r['digest'], 'memory/events/' + r['digest'] + '.json', ';'.join(r['event']['parents']), ';'.join(r['event']['supersedes'])]) + '\n' for r in EVENTS))
rows = ['utc\trecipe\ttrigger\tobserved\tcorrection\tcorrected_by\tguard\tlesson\n']
for r in EVENTS:
    e = r['event']
    values = [NOW, RECIPE, e['attempt_id'] + ':' + e['trigger']['artifact']['path'], e['verification']['status'] + ': ' + e['verification']['outcome'], e['change'], 'memory/events/' + r['digest'] + '.json', e['pattern']['refusal'], e['lesson']]
    rows.append('\t'.join(v.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ') for v in values) + '\n')
put('RECIPE_MEMORY.tsv', ''.join(rows))

categories = {
 'M3ApacheBagContractTest.java': ('multiset/retention-cardinality', 'Independent occurrence budget with explicit pinned donor divergence', 'Preserve min(owned,input); six branches, seed and all per-step assertions retained', 'O(n*m) independent bounded fixture; production unchanged', 'I06'),
 'M3SpringArrayContractTest.java': ('differential-testing/borrowed-list-exceptions', 'Pair only three void add outcomes; compare RuntimeException classes', '4500 operations and all remaining return/state assertions retained', 'No production complexity change; paired test observations', 'I04;I05'),
 'M3FiveLibraryArrayificationTest.java': ('array/stable-merge-and-java-local-inference', 'Split var declarations; equal runs retain left-run order', 'Exact stable ordering [0,1,0,1,2,2]; slice/evaluation order unchanged', 'Production merge unchanged; constant fixture edit', 'I01;I03'),
 'M3GuavaListMultimapContract.java': ('selection/backend-required-contract', 'Branch on observed native availability and assert each mode', 'Native success/equality when present; UnsatisfiedLinkError when absent;6400 steps retained', 'Existing selection algorithms unchanged', 'I03;I10;I13'),
 'M3ApacheCollectionsTest.java': ('java/functional-api-compatibility', 'Reuse existing factory through nonnegative method reference', 'Predicate semantics and all assertions unchanged; no warning suppression', 'No production complexity change', 'I02'),
 'M3FiveLibraryKeyRangesTest.java': ('java/import-resolution', 'Import current java.util.Map', 'All entry/range assertions unchanged', 'No algorithm change', 'I02'),
 'M3FiveProviderFlagSelectionTest.java': ('java/current-factory-resolution', 'Use existing M3LibraryCollections.springMultiValueMap', 'Existing factory/public interface retained; no substitute runtime API', 'No algorithm change', 'I02'),
 'M3LibraryClosureTest.java': ('metadata/current-factory-owner', 'Assert actual existing M3SpringArrayMultiValueMap factory class', 'Explicit legacy owner tests and collectors remain unchanged', 'No algorithm change', 'I03'),
 'M3LibraryBindingsSelfTest.java': ('metadata/exact-resource-cardinality', '25 total, Guava7, Apache3, overload26', 'All five families, bad-input and collision assertions retained', 'Existing parsing and lookup unchanged', 'I03'),
 'M3SdkFamiliesTest.java': ('metadata/exact-resource-cardinality', 'Assert all25 current bindings', 'Exact names and available owners remain checked', 'Existing discovery unchanged', 'I03'),
 'M3SpringCaseInsensitiveSdkFactoryTest.java': ('metadata/exact-resource-cardinality', 'Assert current25 count', 'Existing case behavior, factory and descriptor assertions retained', 'Existing discovery unchanged', 'I03'),
 'SDK_FAMILIES.md': ('metadata/source-owner-and-catalogue-coverage', 'Describe25 bindings and exact M3ArrayList Guava factory', 'Facts match unchanged current owners; one final13 owner label correction', 'Metadata only', 'I03;I11;I12'),
 'sdk-family-bindings.tsv': ('metadata/source-owner-consistency', 'Only one Guava m3_owners cell changes', 'All25 row order/signatures/versions/other owner cells preserved', 'Metadata only; Java class bytes identical12-to13', 'I12;I13')
}
buf = io.StringIO(newline='')
writer = csv.writer(buf, delimiter='\t', lineterminator='\n')
writer.writerow(['problem_category', 'target_file', 'before_sha256', 'after_sha256', 'selected', 'contract', 'complexity_and_performance', 'alternatives', 'status', 'events', 'evidence', 'license'])
for t in targets:
    category, selected, contract, complexity, events = categories[Path(t['path']).name]
    writer.writerow([category, t['path'], t['before_sha256'], t['after_sha256'], selected, contract, complexity + '; no speedup claim', 'Existing owner reuse selected; public behavior change, warning suppression and dropped assertions rejected', 'LOCAL13_JAVA_NATIVE_VALIDATED; canonical admission is a separate observation', events, PREFIX + 'FROZEN_FRONTIER_13.json;' + PREFIX + 'attempt-09-fresh-native.json;' + PREFIX + 'attempt-09-fresh-java.json', 'Apache-2.0 first-party target; exact donor qualifications in DONOR_EVALUATION_20261020.json'])
put('catalogue/INTEROP_EVALUATION_20261020.tsv', buf.getvalue())

put('memory/README.md', HEADER + '''# Interop recipe correction memory

This is the recipe-owned immutable history for `com.synexia.m3jdk.InteropContracts20261020`, under `synexia-openrewrite-recipes/recipe-crates/m3-interop-contracts-20261020`. It applies the Synexia M3 invariant `SYNEXIA-MOP-RECIPE-MEMORY-1` to the concrete owning-module failures discovered after the main runtime recipe. It introduces no global store, deployed serializer, runtime integration layer, or authority to merge or promote a phase.

The final local candidate has **13 files: 11 existing Java test sources, one overview, and one binding TSV**. No production owner, public API, POM, native implementation or existing strict gate is changed by this follow-up. Its frozen candidate-set SHA256 is `05f397c6181992c354c6113aca7e2f9039bb8c835bef8e5c14a286ec526176eb`. The digest is over sorted UTF-8 records `postimageSha256 + "  " + repositoryRelativePath + LF`. The original baseline is `2779aba3f98f31c324be43f731be7914e1a05935`; this is not a claim that the later actual receiving parent remains that commit.

## Four required facets

**Atomize.** Each source-changing event identifies semantically meaningful per-file atoms, exact before/after artifacts, unchanged contracts and dependencies. The general Spring Outcome helper and its smaller successor are both preserved. A syntax-only intermediate is mechanically reconstructed from its exact preimage and checked against the SHA256 originally captured in INPUTS.json; it is labelled as a reconstruction. No arbitrary atom-count limit is imposed.

**Patternize.** Events include problem categories, exact matching conditions, exclusions and refusal rules. `../catalogue/INTEROP_EVALUATION_20261020.tsv` covers every one of the 13 target operations. `../catalogue/DONOR_EVALUATION_20261020.json` compares pinned donor behavior against the existing M3 contract. `../catalogue/PRIOR_PROBLEMS_20261020.tsv` reuses earlier GitHub/LeetCode/HackerRank/GeeksforGeeks discovery metadata; challenge links do not confer copying rights and no challenge implementation is imported here.

**Annotate.** `../RECIPE_MEMORY.tsv` is the required searchable index. `EVENTS.sha256.tsv` links all 13 events by digest, parents and supersedes. Every event retains its trigger, observed outcome, root-cause certainty, correction, rejected alternatives, source differences, commands where captured, dependency pins and bounded replay conditions. The event's own SHA256 exists only in its filename/index, never in its hashed content.

**Document.** `LESSONS.md` records reusable conclusions and limits. All known compile/runtime failures, the superseded Spring helper, the initial12 review HOLD, the13 successor, stale-class evidence and pre-execution disk refusals are retained. Failures remain failures; compile-only success remains compile-only; later successful observations never rewrite earlier events.

## Verification and admission boundaries

The final **local** Java and required-native owning runs each executed159 tests with zero failures, errors or skips. Both modes separately passed the152-check binding main, and each ran the existing Guava26421-check/6400-step corpus after explicitly verifying expected backend availability. Source/class freshness, dependency seals and packaged TSV equality are recorded under `../evidence/interop-correction/attempt-09-*` and `attempt-10-final13-additional-gates.json`. Every Java class hash is identical to the prior12-target qualification; the13 successor changes a packaged metadata resource.

These events capture their historical scope with canonical SDK, actual receiving and publication admission ungranted. Later SDK and receiving results belong in separate immutable `memory/observations/` receipts authored by those gate owners. Reading a successful local record grants no merge authority or phase promotion. The baseline and final per-file resources are canonical crate-relative references; historical absolute argv/report paths remain unchanged inside original receipts.

## Serialization and append rules

The event bytes use the bounded `MOP-RCM/1.0` canonical JSON profile and the closed schema in `EVENT.schema.json`: UTF-8, no BOM or final LF, ASCII keys sorted recursively, no duplicate/unknown fields, scalar Unicode without normalization, lowercase six-character control escapes, int64-only numbers and sorted unique digest links. `PROFILE_REFERENCE.py.txt` contains the selected existing one-off authoring functions reused for this packet. `PROFILE_VALIDATION.json` reports the concrete positive/negative checks applied to these 13 records. This is static task evidence, not a claim that a general-purpose serializer has been deployed.

Append a new digest-named event for a correction. Link known parents/superseded events and leave their bytes intact. Record exact pins when observed; use UNKNOWN with reasons for uncaptured full SDK closures or precise mutation instants. Append separately captured gate observations instead of changing historical admission fields. The original12 frontier, reports and independent review remain available beside the13 successor.

## Retained evidence and donor custody

All original textual logs, Surefire reports, inspected source fixtures and disassembly are under `../evidence/interop-correction/`. `RETAINED_EVIDENCE_INDEX.json` records the initially staged evidence subset; `PACKET_SHA256SUMS` seals the final packet excluding itself. Compiled class binary bodies are retained in scratch and are not published in this text packet; their exact SHA256/mtime metadata and relevant disassembly are preserved. Two sandbox refusal messages were retained as tool-result observations and recorded retrospectively in `ENVIRONMENT_REFUSALS.json`; original stdout files were not available. The cause of old target files reappearing remains unresolved.

The Spring bytecode disassembly is donor-derived Apache-2.0 evidence from the pinned spring-core6.2 JAR and retains the exact donor LICENSE and NOTICE in the crate. Copied M3 owners remain first-party preserved history with exact source-origin mapping, including the main-recipe-received M3IndexMaskCompactor revision. Newly authored probes are isolated evidence only. No donor production source body was adapted into product code in this follow-up, and no performance improvement is asserted.
''')

lesson_lines = [HEADER + '# Reusable lessons from the interop correction passes\n\n']
for record in EVENTS:
    e = record['event']
    lesson_lines += ['## ' + e['attempt_id'] + ' — ' + e['pattern']['category'] + '\n\n', e['lesson'] + '\n\n',
                    '**Observed result:** ' + e['verification']['status'] + '. ' + e['verification']['outcome'] + '\n\n',
                    '**Guard:** ' + e['pattern']['refusal'] + '\n\n',
                    '**Immutable record:** [event](events/' + record['digest'] + '.json).\n\n']
lesson_lines += ['## Shared claim limits\n\nThe Apache donor implementation is not a universal oracle for the explicit M3 retention contract. The Spring borrowed-list exceptions are legitimate outcomes to compare. No test corpus was pruned, no strict flags were removed, and no native implementation or production collection owner was changed. The unexplained stale-file mechanism remains open; fresh-path evidence qualifies the observed runs only. No timing benchmark or general speed claim follows from these correctness results. Later canonical gates are independent observations.\n']
put('memory/LESSONS.md', ''.join(lesson_lines))

receipt = {'schema': 'm3-interop-memory-profile-validation/1', 'recorded_utc': NOW,
           'result': 'PASS', 'event_count': len(EVENTS), 'checks': checks,
           'events': [{'attempt_id': r['event']['attempt_id'], 'sha256': r['digest']} for r in EVENTS],
           'serializer_scope': 'Existing bounded one-off authoring helpers applied to this immutable packet; no deployed generic serializer',
           'schema_sha256': sha((OUT / 'memory/EVENT.schema.json').read_bytes()),
           'profile_reference_sha256': sha((OUT / 'memory/PROFILE_REFERENCE.py.txt').read_bytes()),
           'target_count': len(targets), 'candidate_set_sha256': SET13,
           'admission_granted': False, 'phase_promotion': False}
put('memory/PROFILE_VALIDATION.json', json.dumps(receipt, indent=2) + '\n')
put(PREFIX + 'scripts/author_memory.py', Path(__file__).read_bytes())
manifest_lines = []
for p in sorted(OUT.rglob('*')):
    if p.is_file() and p.name != 'PACKET_SHA256SUMS':
        manifest_lines.append(sha(p.read_bytes()) + '  ' + p.relative_to(OUT).as_posix() + '\n')
put('PACKET_SHA256SUMS', ''.join(manifest_lines))
print(json.dumps({'result': 'PASS', 'events': len(EVENTS), 'checks': len(checks), 'packet_files': len(manifest_lines) + 1,
                  'packet_sha256sums_sha256': sha((OUT / 'PACKET_SHA256SUMS').read_bytes()), 'packet': str(OUT)}))
