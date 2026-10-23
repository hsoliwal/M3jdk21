#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Seal the finite fourth canonical stage after its actual owner qualifications."""
import collections
import csv
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path('/workspace/scratch/206049dfb270')
CANON = ROOT / 'synexia'
MOD = 'synexia-openrewrite-recipes'
CORE = MOD + '/recipe-crates/m3-integrated-refinement-20261021'
STAGE = CORE + '/proof/current-fork-owners-20261021'
CATALOGUE = CORE + '/catalogue/current-fork-owner-decisions-20261021.tsv'
TASKS = ['m3-nlp-current-receiver-lock-20261021', 'swt-current-owner-runtime-closure-20261021']
NLP, SWT = [MOD + '/recipe-crates/' + task for task in TASKS]
REGISTRY = MOD + '/CANONICAL_RECIPE_HOME.tsv'
LEDGER = 'docs/M3-SCALE/provenance/M3_FILE_PROVENANCE.tsv'
BASE = '3f4c3e1c97ca12b7f0d3efbbc387f40f942fa399'
BASE_TREE = '219eebe67fab908a3a4991a35c3c4f77c2907133'
ENGINE = 'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07'
PUBLISHED_CORE = 'a2b50cfe926c5c5f400c7f659a947a5020fb8d4d'
RECEIVER = '2779aba3f98f31c324be43f731be7914e1a05935'
SWT_RECEIVER = 'c0ffa6d76338ed1b06cfe0158b0e04d8930df546'
OUT = ROOT / 'canonical-work/publication-004/current-owners-002'
FIELDS = ['path', 'classification', 'source_url', 'source_revision', 'source_path',
          'license_id', 'notice_path', 'disposition', 'evidence']


def sha(data):
    return hashlib.sha256(data).hexdigest()


def blob(data):
    return hashlib.sha1(('blob ' + str(len(data)) + '\0').encode() + data).hexdigest()


def write_new(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        assert path.is_file() and not path.is_symlink() and path.read_bytes() == data, path
        return
    with path.open('xb') as stream:
        stream.write(data)


def put(path, value):
    write_new(path, (json.dumps(value, indent=2) + '\n').encode())


def read(relative):
    return json.loads((CANON / relative).read_text())


def verify_inventory(path):
    data = json.loads(path.read_text())
    for row in data['files']:
        raw = Path(row['local_path']).read_bytes()
        assert len(raw) == row['bytes'] and sha(raw) == row['after_sha256'] and blob(raw) == row['after_git_blob'], row['path']
    return data


def eligible(path):
    relative = path.relative_to(CANON)
    return path.is_file() and not any(x in ('target', '__pycache__', '.rsync-tmp') for x in relative.parts)


prior_inventories = [
    ROOT / 'canonical-work/publication-002/core/INVENTORY.json',
    ROOT / 'canonical-work/publication-002/swt/INVENTORY.json',
    ROOT / 'canonical-work/publication-003/maven4/INVENTORY.json']
priors = [verify_inventory(path) for path in prior_inventories]
prior_new = {r['path'] for p in priors for r in p['files'] if r['before_git_blob'] == 'ABSENT'}
preimages = read(STAGE + '/PUBLICATION_PREIMAGES.json')
recipe_preimages = read(STAGE + '/PUBLICATION_RECIPE_PREIMAGES.json')
assert preimages['base_commit'] == recipe_preimages['base_commit'] == BASE
assert preimages['base_tree'] == recipe_preimages['base_tree'] == BASE_TREE
remote = {row['path']: row for row in preimages['paths']}
before = {}
for relative, name, expected in [
    (REGISTRY, 'CANONICAL_RECIPE_HOME.tsv', '72cde3e3aac244c0bbe4a7eb9e7f05c066936f20'),
    (LEDGER, 'M3_FILE_PROVENANCE.tsv', '90ef9a1e55d4c20126fed770aeb9286719f6c901')]:
    raw = (ROOT / 'canonical-work/publication-003/maven4' / name).read_bytes()
    assert raw == (CANON / relative).read_bytes() and raw.endswith(b'\n')
    assert blob(raw) == expected == remote[relative]['git_blob']
    before[relative] = raw

swt_handoff_path = ROOT / 'jobs-work/swt-owner-closure-20261021/FINAL_HANDOFF.json'
assert sha(swt_handoff_path.read_bytes()) == 'f0a4224a2b21a52297cc4ce518ea04d7957849684f0666eae7d2793476b576e5'
swt_handoff = json.loads(swt_handoff_path.read_text())
for row in swt_handoff['files']:
    raw = (CANON / row['repository_path']).read_bytes()
    assert sha(raw) == row['sha256'] and len(raw) == row['bytes'], row['repository_path']
assert len(swt_handoff['files']) == 228
source_inputs = []
for crate, expected_count in [(NLP, 21), (SWT, 18)]:
    rows = read(crate + '/proof/sdk-001-offline/SOURCE_INPUTS.json')
    assert len(rows) == expected_count
    for row in rows:
        raw = (CANON / row['path']).read_bytes()
        assert sha(raw) == row['sha256'] and len(raw) == row['bytes'], row['path']
    source_inputs.extend(rows)
    assert read(crate + '/memory/FINAL_RESULT.json')['status'] == 'PASS'
    assert read(crate + '/proof/sdk-001-offline/RESULT.json')['status'] == 'PASS'
assert read(NLP + '/proof/receiver-runtime-001/combined-receiver-001-receipts/RESULT.json')['state'] == 'PASS'
assert read(SWT + '/proof/receiver-runtime-001/RESULT.json')['status'] == 'PASS'

OUT.mkdir(parents=True, exist_ok=False)
for relative, raw in before.items():
    write_new(OUT / 'base' / Path(relative).name, raw)
write_new(CANON / STAGE / 'SWT_FROZEN_CURATION_HANDOFF.json', swt_handoff_path.read_bytes())
write_new(CANON / STAGE / 'seal_publication-002.py', Path(__file__).read_bytes())
put(CANON / STAGE / 'PUBLICATION_SCOPE.json', {
    'schema': 'M3_CURRENT_FORK_OWNER_PUBLICATION_SCOPE/1', 'status': 'DRAFT_QUALIFIED_ADDITIVE_STAGE',
    'parent_commit': BASE, 'parent_tree': BASE_TREE,
    'parent_pull_request': 'https://github.com/hsoliwal/com.synexia/pull/12657',
    'engine_source_pin': ENGINE, 'receiving_source_pin': RECEIVER, 'swt_receiving_pin': SWT_RECEIVER,
    'new_recipe_families': TASKS, 'actual_receiving_file_changes': {'M3jdk21': 2, 'eclipse.platform.swt': 1},
    'prior_packets': [{'inventory': str(p), 'sha256': sha(p.read_bytes()), 'files': len(i['files'])}
                      for p, i in zip(prior_inventories, priors)],
    'existing_catalogue_owners': 'The bounded current-owner decision index points to existing collection/resource/scheduler/Eclipse/Maven catalogues and exact new source contracts; no replacement implementation or algorithm catalogue.',
    'qualification_index': STAGE + '/QUALIFICATION_INDEX.json',
    'authorization': 'User-authorized implementation and PRs with Synexia component reuse; routine licensed source/array/source-lock/build-runtime maintenance. Exact notices preserved; no patent status or priority claim.',
    'hosted_admission': 'Actual broader pre-existing migration-engine pin failure is retained separately. This stage does not weaken or repair that owner.',
    'new_workflows': 0, 'shared_engine_edits': 0, 'deletions': 0,
    'status_boundary': 'Draft; local bounded actual proof does not establish full-product/upstream/platform/performance/hosted admission.'})
result_path = CANON / STAGE / 'PUBLICATION_PROVENANCE_CHECK-002.json'
put(result_path, {'status': 'PENDING_EXISTING_VALIDATOR'})

paths = []
for prefix in [STAGE, NLP, SWT]:
    paths.extend(p for p in (CANON / prefix).rglob('*') if eligible(p))
paths.append(CANON / CATALOGUE)
for task in TASKS:
    paths.append(CANON / MOD / 'src/main/resources/META-INF/rewrite' / (task + '.yml'))
for kind, phase in [('java', TASKS[0] + '-001'), ('text', TASKS[0] + '-002'), ('text', TASKS[1] + '-001')]:
    paths.extend(p for p in (CANON / MOD / 'src/main/resources/com/synexia/rewrite' / ('hash-pinned-' + kind) / phase).rglob('*') if eligible(p))
assert len(paths) == len(set(paths))
paths.sort()
assert not ({p.relative_to(CANON).as_posix() for p in paths} & prior_new)
absence = [row['path'] for row in preimages['paths'] + recipe_preimages['paths'] if row['status'] == 'ABSENT']
for path in paths:
    relative = path.relative_to(CANON).as_posix()
    assert any(relative == p or relative.startswith(p + '/') for p in absence), relative
    assert not path.is_symlink() and path.suffix not in ('.class', '.pyc', '.jar', '.jmod', '.so', '.dll', '.dylib')
    path.read_bytes().decode('utf-8', errors='strict')

spec = importlib.util.spec_from_file_location('current_owner_existing_provenance', CANON / 'scripts/legal/check_m3_provenance.py')
checker = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = checker
spec.loader.exec_module(checker)
policy = checker.load_policy(CANON / 'docs/M3-SCALE/provenance/m3-provenance-policy.json')
origins = {}


def origin(digest, repository, revision, source_path, license_id='Apache-2.0', notice=None,
           classification='PRESERVED_HISTORY', disposition='EXACT_PINNED_SOURCE_OR_EVIDENCE'):
    origins[digest] = dict(source_url='https://github.com/' + repository, source_revision=revision,
        source_path=source_path, license_id=license_id,
        notice_path=notice or CORE + '/licenses/Synexia-NOTICE', classification=classification,
        disposition=disposition)


for path in paths:
    rel = path.relative_to(CANON).as_posix()
    digest = sha(path.read_bytes())
    for fork, revision, notice in [('opennlp', '8b647d1d389736952e43fb46865c874747f7acde', 'NOTICE'),
                                   ('lucene', '873b6167603c69101d626ddffd8d733340300fea', 'NOTICE.txt')]:
        prefix = STAGE + '/nlp/forks/' + fork + '/'
        if rel.startswith(prefix):
            origin(digest, 'hsoliwal/' + fork, revision, rel[len(prefix):],
                   notice=STAGE + '/nlp/forks/' + fork + '/' + notice)
    for prefix in [STAGE + '/nlp/combined-receiver-source/', NLP + '/proof/owner-candidate-001/baseline2779/']:
        if rel.startswith(prefix):
            origin(digest, 'hsoliwal/M3jdk21', RECEIVER, rel[len(prefix):])
    prefix = SWT + '/proof/incumbent-inputs/'
    if rel.startswith(prefix):
        header = '\n'.join(path.read_text().splitlines()[:40])
        match = re.search(r'SPDX-License-Identifier:\s*([^\r\n*<]+)', header)
        license_id = match.group(1).strip() if match else 'EPL-2.0'
        if license_id.endswith('-->'): license_id = license_id[:-3].strip()
        origin(digest, 'hsoliwal/eclipse.platform.swt', SWT_RECEIVER, rel[len(prefix):], license_id,
               STAGE + '/swt/licenses/NOTICE')
for row in read(STAGE + '/swt/RIGHTS_INTAKE.json')['files']:
    license_id = 'EPL-2.0' if row['source_path'] in ('LICENSE', 'NOTICE') else 'Apache-2.0 OR EPL-2.0'
    origin(row['sha256'], 'hsoliwal/eclipse.platform.swt', SWT_RECEIVER, row['source_path'], license_id, STAGE + '/swt/licenses/NOTICE')
for crate in [NLP, SWT]:
    for row in read(crate + '/provenance/RECIPE_LINEAGE.json')['adaptations']:
        origin(row['source_sha256'], 'hsoliwal/com.synexia', row.get('source_revision', PUBLISHED_CORE), row['source_path'])
        origin(row['after_sha256'], 'hsoliwal/com.synexia', row.get('source_revision', PUBLISHED_CORE), row['source_path'],
               classification='UPSTREAM_MODIFIED', disposition='ADAPTED_UPSTREAM')
    for row in read(crate + '/memory/HELPER_LINEAGE.json')['sources']:
        origin(row['source_sha256'], 'hsoliwal/com.synexia', PUBLISHED_CORE, row['source_path'])
        origin(row['sha256'], 'hsoliwal/com.synexia', PUBLISHED_CORE, row['source_path'],
               classification='PRESERVED_HISTORY' if row['sha256'] == row['source_sha256'] else 'UPSTREAM_MODIFIED',
               disposition='EXACT_HELPER_OR_PROFILE_IDENTITY_ADAPTATION' if row['sha256'] == row['source_sha256'] else 'ADAPTED_UPSTREAM')
    for row in read(crate + '/SOURCE_SEALS.json')['files']:
        repository = 'hsoliwal/M3jdk21' if crate == NLP else 'hsoliwal/eclipse.platform.swt'
        revision = RECEIVER if crate == NLP else SWT_RECEIVER
        origin(row['before_sha256'], repository, revision, row['target_path'])
        origin(row['after_sha256'], repository, 'task-sha256:' + row['after_sha256'], row['target_path'],
               disposition='RETAINED_EXACT_QUALIFIED_FIRST_PARTY_POSTIMAGE')
    for name in ['Synexia-LICENSE', 'Synexia-NOTICE']:
        origin(sha((CANON / crate / 'licenses' / name).read_bytes()), 'hsoliwal/com.synexia', ENGINE,
               'LICENSE' if name.endswith('LICENSE') else 'NOTICE', disposition='EXACT_LICENSE_OR_NOTICE')
for name in ['M3HashPinnedJavaSnapshotRecipe.java', 'M3HashPinnedTextSnapshotRecipe.java', 'M3EditScope.java', 'M3ScopeInference.java']:
    path = MOD + '/src/main/java/com/synexia/rewrite/' + name
    origin(sha((CANON / path).read_bytes()), 'hsoliwal/com.synexia', ENGINE, path)
for row in read(NLP + '/proof/owner-candidate-001/LINEAGE_REVIEW.json')['canonical_observed_metadata']:
    tail = row['url'].split('/blob/', 1)[1]
    revision, source_path = tail.split('/', 1)
    origin(row['sha256'], 'hsoliwal/com.synexia', revision, source_path)
slf = read(SWT + '/provenance/SOURCE_AND_RIGHTS.json')['selected_dependency']
origin(slf['license_sha256'], 'qos-ch/slf4j', 'artifact-sha256:' + slf['jar_sha256'],
       'META-INF/LICENSE.txt', 'MIT', SWT + '/' + slf['license_path'], disposition='EXACT_CACHED_ARTIFACT_LICENSE')


def row_for(path):
    relative = path.relative_to(CANON).as_posix()
    data = path.read_bytes()
    found = origins.get(sha(data))
    if found:
        row = dict(found)
    else:
        head = '\n'.join(data.decode().splitlines()[:80])
        canonical_header = 'SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project' in head and 'SPDX-License-Identifier: Apache-2.0' in head
        if canonical_header and checker.is_commentable(relative, policy):
            classification, disposition = 'FIRST_PARTY', 'NEW_FIRST_PARTY_TASK_OR_EVIDENCE_TOOL'
        elif '/proof/' in relative or path.suffix in ('.log', '.diff'):
            classification, disposition = 'GENERATED', 'RETAINED_EXECUTION_OR_PREPARATION_EVIDENCE'
        elif path.suffix in ('.json', '.jsonl', '.tsv', '.txt'):
            classification, disposition = 'FIRST_PARTY_NO_HEADER', 'FIRST_PARTY_METADATA'
        else:
            classification, disposition = 'PRESERVED_HISTORY', 'RETAINED_TASK_PREPARATION_SOURCE'
        row = dict(source_url='https://github.com/hsoliwal/com.synexia', source_revision='task-stage:current-fork-owners-20261021',
            source_path=relative, license_id='Apache-2.0', notice_path=CORE + '/licenses/Synexia-NOTICE',
            classification=classification, disposition=disposition)
    return dict(path=relative, **row, evidence=STAGE + '/SOURCE_AND_RIGHTS.json')


rows = [row_for(path) for path in paths]
old_rows = list(csv.DictReader((line for line in before[LEDGER].decode().splitlines() if line and not line.startswith('#')), delimiter='\t'))
assert not ({row['path'] for row in rows} & {row['path'] for row in old_rows})
stream = io.StringIO()
csv.DictWriter(stream, fieldnames=FIELDS, delimiter='\t', lineterminator='\n').writerows(rows)
ledger = before[LEDGER] + stream.getvalue().encode()
registration = []
for task, named, harness in [
    (TASKS[0], 'com.synexia.m3.NlpCurrentReceiverLock20261021', 'M3NlpCurrentReceiverLockRecipeTest'),
    (TASKS[1], 'com.synexia.swt.CurrentOwnerRuntimeClosure20261021', 'M3SwtCurrentOwnerRuntimeClosureRecipeTest')]:
    crate = MOD + '/recipe-crates/' + task
    pom = ET.parse(CANON / crate / 'pom.xml').getroot()
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    coordinate = pom.find('m:groupId', ns).text + ':' + pom.find('m:artifactId', ns).text
    for kind, path, symbol in [
        ('RECIPE_DESCRIPTOR', MOD + '/src/main/resources/META-INF/rewrite/' + task + '.yml', named),
        ('MAVEN_MODULE', crate + '/pom.xml', coordinate),
        ('JAVA_TEST', crate + '/src/test/java/com/synexia/rewrite/' + harness + '.java', 'com.synexia.rewrite.' + harness)]:
        registration.append('\t'.join(['SYNEXIA_CANONICAL_RECIPE_HOME_V1', task, kind, path, symbol,
                                      blob((CANON / path).read_bytes()), 'Apache-2.0', 'PINNED_DRAFT_QUALIFICATION_ONLY']))
addition = ('\n'.join(registration) + '\n').encode()
registry = before[REGISTRY] + addition
write_new(OUT / 'CANONICAL_RECIPE_HOME.tsv', registry)
write_new(OUT / 'M3_FILE_PROVENANCE.tsv', ledger)
actual = checker.load_ledger(OUT / 'M3_FILE_PROVENANCE.tsv', set(policy['allowed_classifications']))
violations = []
for path in paths:
    violations.extend(checker.validate_change(checker.Change('A', path.relative_to(CANON).as_posix()), CANON, policy, actual))
for path in [REGISTRY, LEDGER]:
    violations.extend(checker.validate_change(checker.Change('M', path), CANON, policy, actual))
result = {'schema': 'M3_CURRENT_OWNER_FOCUSED_PROVENANCE_CHECK/1', 'status': 'FAIL' if violations else 'PASS',
    'existing_validator': 'scripts/legal/check_m3_provenance.py',
    'validator_sha256': sha((CANON / 'scripts/legal/check_m3_provenance.py').read_bytes()),
    'invocation': 'Imported existing load_policy/load_ledger/validate_change over the exact additive set; bounded hydration is not a full checkout CLI/reactor gate.',
    'new_files': len(paths), 'modified_append_owners': 2, 'new_provenance_rows': len(rows), 'registry_rows_added': len(registration),
    'classifications': dict(collections.Counter(row['classification'] for row in rows)),
    'prior_registry_prefix_exact': registry.startswith(before[REGISTRY]), 'prior_ledger_prefix_exact': ledger.startswith(before[LEDGER]),
    'violations': [vars(v) for v in violations],
    'limits': 'Exact changed-file metadata/header check only; no legal opinion, license regrant, runtime or hosted admission.'}
result_path.write_text(json.dumps(result, indent=2) + '\n')
assert not violations, violations
for relative, raw in [(REGISTRY, registry), (LEDGER, ledger)]:
    assert (CANON / relative).read_bytes() == before[relative]
    (CANON / relative).write_bytes(raw)
for inventory in prior_inventories:
    verify_inventory(inventory)
for row in source_inputs:
    assert sha((CANON / row['path']).read_bytes()) == row['sha256'], row['path']
records = []
for path in paths:
    raw = path.read_bytes()
    records.append({'path': path.relative_to(CANON).as_posix(), 'local_path': str(path),
        'before_git_blob': 'ABSENT', 'before_sha256': 'ABSENT', 'after_git_blob': blob(raw),
        'after_sha256': sha(raw), 'bytes': len(raw), 'mode': '100644'})
for relative, raw in [(REGISTRY, registry), (LEDGER, ledger)]:
    records.append({'path': relative, 'local_path': str(OUT / Path(relative).name),
        'before_git_blob': blob(before[relative]), 'before_sha256': sha(before[relative]),
        'after_git_blob': blob(raw), 'after_sha256': sha(raw), 'bytes': len(raw), 'mode': '100644'})
inventory = {'schema': 'M3_CANONICAL_ADDITIVE_PUBLICATION_INVENTORY/1', 'group': 'current-owners',
    'base_revision': BASE, 'base_tree': BASE_TREE, 'authoring_revision': ENGINE, 'files': records,
    'new_files': len(paths), 'modified_files': 2, 'deletions': 0, 'shared_engine_edits': 0,
    'registry_append_rows': len(registration), 'ledger_append_rows': len(rows), 'draft_required': True,
    'qualification': STAGE + '/QUALIFICATION_INDEX.json',
    'source_overrides': 'Use each row local_path; frozen owner overrides from all three previous stages remain unchanged.'}
put(OUT / 'INVENTORY.json', inventory)
put(OUT / 'APPEND_PROOF.json', {'status': 'PASS', 'base_revision': BASE, 'base_tree': BASE_TREE,
    'registry_before_git_blob': blob(before[REGISTRY]), 'registry_after_git_blob': blob(registry),
    'ledger_before_git_blob': blob(before[LEDGER]), 'ledger_after_git_blob': blob(ledger),
    'registry_prefix_exact': registry.startswith(before[REGISTRY]), 'ledger_prefix_exact': ledger.startswith(before[LEDGER]),
    'previous_inventory_files_verified': sum(len(i['files']) for i in priors),
    'current_sdk_source_inputs_verified': len(source_inputs), 'swt_frozen_files_verified': len(swt_handoff['files']),
    'no_duplicate_ledger_paths': True, 'new_files': len(paths), 'inventory_sha256': sha((OUT / 'INVENTORY.json').read_bytes())})
print(json.dumps({'status': 'PASS', 'files': len(records), 'new_files': len(paths),
    'inventory': str(OUT / 'INVENTORY.json'), 'inventory_sha256': sha((OUT / 'INVENTORY.json').read_bytes()),
    'provenance': result['status']}, indent=2))
