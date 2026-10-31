#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Seal this finite Maven4 addition with the existing provenance gate."""
import collections
import csv
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import shutil
import sys

ROOT = Path('/workspace/scratch/206049dfb270')
CANON = ROOT / 'synexia'
TASK = 'm3-maven4-effective-20261021'
MODULE = 'synexia-openrewrite-recipes'
CRATE = MODULE + '/recipe-crates/' + TASK
REGISTRY = MODULE + '/CANONICAL_RECIPE_HOME.tsv'
LEDGER = 'docs/M3-SCALE/provenance/M3_FILE_PROVENANCE.tsv'
BASE = 'a5a7eaa644138ee62f6756f0d8e9b3957302a2d9'
BASE_TREE = '949f5f459f6541eabe111833a8efc32b52fc67ff'
CORE_COMMIT = 'a2b50cfe926c5c5f400c7f659a947a5020fb8d4d'
SOURCE = '2779aba3f98f31c324be43f731be7914e1a05935'
ENGINE_SOURCE = 'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07'
FIELDS = ['path', 'classification', 'source_url', 'source_revision', 'source_path',
          'license_id', 'notice_path', 'disposition', 'evidence']
OUT = ROOT / 'canonical-work/publication-003/maven4'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def blob(data):
    return hashlib.sha1(('blob ' + str(len(data)) + '\0').encode() + data).hexdigest()


def write_new(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as stream:
        stream.write(data)


def put_json(path, value):
    write_new(path, (json.dumps(value, indent=2) + '\n').encode())


def check_rows(rows, field):
    for row in rows:
        path = CANON / row[field]
        data = path.read_bytes()
        assert len(data) == row['bytes'] and sha(data) == row['sha256'], row[field]


handoff_path = ROOT / 'jobs-work/maven4-recipe/FINAL_HANDOFF.json'
handoff_bytes = handoff_path.read_bytes()
assert sha(handoff_bytes) == 'dc3cdb3e547a85c0f13de576479d19137c87de2b2bb002a1b763d0a62b371342'
handoff = json.loads(handoff_bytes)
check_rows(handoff['files'], 'repository_path')
assert len(handoff['files']) == 321
sdk_inputs = json.loads((CANON / CRATE / 'proof/sdk-002-offline/SOURCE_INPUTS.json').read_text())
check_rows(sdk_inputs, 'path')
assert len(sdk_inputs) == 36
preimages = json.loads((CANON / CRATE / 'provenance/PUBLICATION_PREIMAGES.json').read_text())
assert preimages['base_commit'] == BASE and preimages['base_tree'] == BASE_TREE
remote = {row['path']: row for row in preimages['paths']}

before = {}
for path, name, expected in (
    (REGISTRY, 'CANONICAL_RECIPE_HOME.tsv', '9ee657fecb080cf9588c31d362b84b195607f702'),
    (LEDGER, 'M3_FILE_PROVENANCE.tsv', 'dd045fa757cba8fe10cffb60bb6923aa44c5eb3e')):
    data = (ROOT / 'canonical-work/publication-002/swt' / name).read_bytes()
    assert data == (CANON / path).read_bytes() and data.endswith(b'\n')
    assert blob(data) == expected == remote[path]['git_blob']
    assert remote[path]['mode'] == '100644'
    before[path] = data
assert not OUT.exists()
OUT.mkdir(parents=True)
for path, data in before.items():
    write_new(OUT / 'base' / Path(path).name, data)

write_new(CANON / CRATE / 'provenance/PUBLICATION_CURATION_HANDOFF.json', handoff_bytes)
helper_target = CANON / CRATE / 'provenance/seal_maven4_publication.py'
write_new(helper_target, Path(__file__).read_bytes())
scope = {
    'schema': 'M3_MAVEN4_PUBLICATION_SCOPE/1',
    'status': 'DRAFT_QUALIFIED_SUCCESSOR',
    'parent_commit': BASE, 'parent_tree': BASE_TREE,
    'parent_pull_request': 'https://github.com/hsoliwal/com.synexia/pull/12654',
    'core_lineage_commit': CORE_COMMIT,
    'engine_authoring_pin': ENGINE_SOURCE,
    'receiving_authoring_pin': SOURCE,
    'lineage': 'Append only after the published core and SWT commits; neither packet is changed.',
    'historical_status_fields': 'Retained SOURCE_SEALS and initial lineage record their authoring-time not-run or pending status. QUALIFICATION_SUMMARY and dated actual SDK/receiver receipts provide the later result without overwriting historical records.',
    'authorization': 'The user authorized implementation and PRs for M3JDK21 and reuse of Synexia components. This is existing-owner Maven model ingress and recipe/test maintenance, with existing notices retained. No patent status or priority claim is made.',
    'qualification': 'QUALIFICATION_SUMMARY.json',
    'memory': 'memory/FINAL_RESULT-002.json',
    'publication_provenance': 'proof/PUBLICATION_PROVENANCE_CHECK.json',
    'unchanged_sdk_source_inputs': len(sdk_inputs),
    'frozen_curation_file_count': len(handoff['files']),
    'new_workflows': 0,
    'runtime_or_ci_admission': 'PENDING; local bounded qualification is not hosted CI or full product admission.'
}
put_json(CANON / CRATE / 'provenance/PUBLICATION_SCOPE.json', scope)
result_path = CANON / CRATE / 'proof/PUBLICATION_PROVENANCE_CHECK.json'
put_json(result_path, {'status': 'PENDING_EXISTING_VALIDATOR', 'scope': TASK})

spec = importlib.util.spec_from_file_location('existing_provenance_owner', CANON / 'scripts/legal/check_m3_provenance.py')
checker = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = checker
spec.loader.exec_module(checker)
policy = checker.load_policy(CANON / 'docs/M3-SCALE/provenance/m3-provenance-policy.json')

# Exact source identities take precedence over generated proof-file locations.
origins = {}


def origin(digest, repository, revision, path, classification, disposition):
    origins[digest] = dict(source_url='https://github.com/' + repository,
        source_revision=revision, source_path=path, classification=classification,
        disposition=disposition)


rights = json.loads((CANON / CRATE / 'provenance/SOURCE_AND_RIGHTS.json').read_text())
for row in rights['target_files']:
    if row['before_sha256'] != 'ABSENT':
        origin(row['before_sha256'], 'hsoliwal/M3jdk21', SOURCE, row['path'],
               'PRESERVED_HISTORY', 'EXACT_RECEIVING_PREIMAGE')
    origin(row['after_sha256'], 'hsoliwal/M3jdk21', 'task-sha256:' + row['after_sha256'],
           row['path'], 'PRESERVED_HISTORY', 'RETAINED_TASK_RECIPE_POSTIMAGE')
lineage = json.loads((CANON / CRATE / 'provenance/RECIPE_LINEAGE.json').read_text())
for row in lineage['unchanged_shared_engines']:
    origin(row['sha256'], 'hsoliwal/com.synexia', ENGINE_SOURCE, row['path'],
           'PRESERVED_HISTORY', 'EXACT_SHARED_ENGINE_SOURCE')
for row in lineage['adaptations']:
    origin(row['source_sha256'], 'hsoliwal/com.synexia', CORE_COMMIT, row['source_path'],
           'PRESERVED_HISTORY', 'EXACT_PUBLISHED_CORE_DONOR')
    origin(row['after_sha256'], 'hsoliwal/com.synexia', CORE_COMMIT, row['source_path'],
           'UPSTREAM_MODIFIED', 'ADAPTED_UPSTREAM')
successor = json.loads((CANON / CRATE / 'provenance/successor-002/LINEAGE.json').read_text())
for row in successor['adaptations']:
    original = next(p for p in lineage['adaptations'] if p['target_path'] == row['path'])
    origin(row['after_sha256'], 'hsoliwal/com.synexia', CORE_COMMIT, original['source_path'],
           'UPSTREAM_MODIFIED', 'ADAPTED_UPSTREAM')
helpers = json.loads((CANON / CRATE / 'memory/PUBLISHED_HELPER_LINEAGE.json').read_text())
for row in helpers['sources']:
    target = CANON / CRATE / 'memory/tooling/src' / row['path'].split('/src/test/java/', 1)[1]
    data = target.read_bytes()
    identical = sha(data) == row['sha256']
    origin(sha(data), 'hsoliwal/com.synexia', row['source_revision'], row['path'],
           'PRESERVED_HISTORY' if identical else 'UPSTREAM_MODIFIED',
           'EXACT_PUBLISHED_CORE_DONOR' if identical else 'ADAPTED_UPSTREAM')
origin('4fc14187d5dd9e49850dae6f26a3f19af4a77809edef350bc149a434217472b2',
       'hsoliwal/com.synexia', CORE_COMMIT,
       MODULE + '/recipe-crates/m3-integrated-refinement-20261021/proof/receiver-001/runner.py',
       'PRESERVED_HISTORY', 'EXACT_PUBLISHED_CORE_DONOR')
for row in rights['license_and_notice_files']:
    maven = Path(row['path']).name.startswith('Maven-Fork-')
    owner = 'hsoliwal/maven' if maven else 'hsoliwal/com.synexia'
    revision = '2993571eedbe96e06a737eaa8ae47cf112b255de' if maven else ENGINE_SOURCE
    original_path = Path(row['path']).name.rsplit('-', 1)[-1]
    origin(row['sha256'], owner, revision, original_path,
           'PRESERVED_HISTORY', 'EXACT_LICENSE_OR_NOTICE')

paths = [CANON / row['repository_path'] for row in handoff['files']]
for relative in ('memory/PUBLISHED_HELPER_LINEAGE.json', 'provenance/PUBLICATION_PREIMAGES.json',
                 'provenance/PUBLICATION_CURATION_HANDOFF.json', 'provenance/seal_maven4_publication.py',
                 'provenance/PUBLICATION_SCOPE.json', 'proof/PUBLICATION_PROVENANCE_CHECK.json'):
    paths.append(CANON / CRATE / relative)
assert len(paths) == len(set(paths))
paths.sort()
absence_prefixes = [r['path'] for r in preimages['paths'] if r['status'] == 'ABSENT']
for path in paths:
    relative = path.relative_to(CANON).as_posix()
    assert any(relative == p or relative.startswith(p + '/') for p in absence_prefixes), relative
    assert not path.is_symlink() and path.suffix not in ('.class', '.pyc', '.jar', '.so', '.dll', '.dylib')
    path.read_bytes().decode('utf-8', errors='strict')


def row_for(path):
    relative = path.relative_to(CANON).as_posix()
    data = path.read_bytes()
    digest = sha(data)
    found = origins.get(digest)
    if found:
        row = dict(found)
    else:
        head = '\n'.join(data.decode('utf-8').splitlines()[:80])
        header = ('SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project' in head
                  and 'SPDX-License-Identifier: Apache-2.0' in head)
        if header and checker.is_commentable(relative, policy):
            classification, disposition = 'FIRST_PARTY', 'NEW_FIRST_PARTY_RECIPE_OR_DOCUMENT'
        elif '/proof/' in relative or path.suffix in ('.log', '.diff'):
            classification, disposition = 'GENERATED', 'RETAINED_EXECUTION_OR_PREPARATION_EVIDENCE'
        elif path.suffix in ('.json', '.jsonl', '.tsv', '.txt'):
            classification, disposition = 'FIRST_PARTY_NO_HEADER', 'FIRST_PARTY_METADATA'
        else:
            classification, disposition = 'PRESERVED_HISTORY', 'RETAINED_TASK_PREPARATION_SOURCE'
        row = dict(source_url='https://github.com/hsoliwal/com.synexia',
                   source_revision='task-sha256:' + handoff['source_seals_sha256'],
                   source_path=relative, classification=classification, disposition=disposition)
    notice = 'Maven-Fork-NOTICE' if row['source_url'] == 'https://github.com/hsoliwal/maven' else 'Synexia-NOTICE'
    return dict(path=relative, **row, license_id='Apache-2.0',
                notice_path=CRATE + '/licenses/' + notice,
                evidence=CRATE + '/provenance/SOURCE_AND_RIGHTS.json')


rows = [row_for(path) for path in paths]
prior = list(csv.DictReader((line for line in before[LEDGER].decode().splitlines()
                            if line and not line.startswith('#')), delimiter='\t'))
assert not ({row['path'] for row in rows} & {row['path'] for row in prior})
stream = io.StringIO()
writer = csv.DictWriter(stream, fieldnames=FIELDS, delimiter='\t', lineterminator='\n')
writer.writerows(rows)
ledger = before[LEDGER] + stream.getvalue().encode()
registration = []
for kind, path, symbol in (
    ('RECIPE_DESCRIPTOR', MODULE + '/src/main/resources/META-INF/rewrite/' + TASK + '.yml', 'com.synexia.m3.Maven4Effective20261021'),
    ('MAVEN_MODULE', CRATE + '/pom.xml', 'com.synexia.m3:maven4-effective-proof'),
    ('JAVA_TEST', CRATE + '/src/test/java/com/synexia/rewrite/M3Maven4EffectiveRecipeTest.java', 'com.synexia.rewrite.M3Maven4EffectiveRecipeTest')):
    registration.append('\t'.join(['SYNEXIA_CANONICAL_RECIPE_HOME_V1', TASK, kind, path, symbol,
                                  blob((CANON / path).read_bytes()), 'Apache-2.0',
                                  'PINNED_DRAFT_QUALIFICATION_ONLY']))
addition = ('\n'.join(registration) + '\n').encode()
registry = before[REGISTRY] + addition
for relative, data in ((REGISTRY, registry), (LEDGER, ledger)):
    write_new(OUT / Path(relative).name, data)
    (CANON / relative).write_bytes(data)

actual = checker.load_ledger(CANON / LEDGER, set(policy['allowed_classifications']))
violations = []
for path in paths:
    violations.extend(checker.validate_change(checker.Change('A', path.relative_to(CANON).as_posix()), CANON, policy, actual))
for path in (REGISTRY, LEDGER):
    violations.extend(checker.validate_change(checker.Change('M', path), CANON, policy, actual))
result = dict(schema='M3_FOCUSED_PUBLICATION_PROVENANCE_CHECK/1', status='FAIL' if violations else 'PASS',
    scope=TASK, existing_validator='scripts/legal/check_m3_provenance.py',
    validator_sha256=sha((CANON / 'scripts/legal/check_m3_provenance.py').read_bytes()),
    invocation='Imported existing load_policy/load_ledger/validate_change on this exact additive file set; bounded hydration is not a Git checkout for the owner CLI.',
    new_files=len(paths), modified_append_owners=2, new_provenance_rows=len(rows),
    classifications=dict(collections.Counter(r['classification'] for r in rows)),
    prior_registry_bytes_retained=registry.startswith(before[REGISTRY]),
    prior_ledger_bytes_retained=ledger.startswith(before[LEDGER]),
    violations=[vars(v) for v in violations],
    limits='Exact changed-file metadata/header gate only; not a legal opinion, license regrant, runtime gate, whole-reactor or hosted CI result.')
result_path.write_text(json.dumps(result, indent=2) + '\n')
assert not violations, violations
check_rows(handoff['files'], 'repository_path')
check_rows(sdk_inputs, 'path')

records = []
for path in paths:
    data = path.read_bytes()
    records.append(dict(path=path.relative_to(CANON).as_posix(), local_path=str(path),
                        before_git_blob='ABSENT', before_sha256='ABSENT',
                        after_git_blob=blob(data), after_sha256=sha(data), mode='100644', bytes=len(data)))
for relative, data in ((REGISTRY, registry), (LEDGER, ledger)):
    records.append(dict(path=relative, local_path=str(OUT / Path(relative).name),
                        before_git_blob=blob(before[relative]), before_sha256=sha(before[relative]),
                        after_git_blob=blob(data), after_sha256=sha(data), mode='100644', bytes=len(data)))
inventory = dict(schema='M3_CANONICAL_ADDITIVE_PUBLICATION_INVENTORY/1', group='maven4',
    base_revision=BASE, base_tree=BASE_TREE,
    publication_root_revision='ad85831eda771cbd050124c806b45c050e545984', authoring_revision=ENGINE_SOURCE,
    files=records, new_files=len(paths), modified_files=2, deletions=0, shared_engine_edits=0,
    registry_append_bytes=len(addition), ledger_append_rows=len(rows), draft_required=True,
    source_overrides='Use local_path for both append owners; prior core and SWT intermediate inventories remain frozen.',
    qualification=result_path.relative_to(CANON).as_posix())
put_json(OUT / 'INVENTORY.json', inventory)
put_json(OUT / 'APPEND_PROOF.json', dict(status='PASS', base_revision=BASE, base_tree=BASE_TREE,
    registry_before_git_blob=blob(before[REGISTRY]), registry_after_git_blob=blob(registry),
    ledger_before_git_blob=blob(before[LEDGER]), ledger_after_git_blob=blob(ledger),
    registry_prefix_exact=registry.startswith(before[REGISTRY]), ledger_prefix_exact=ledger.startswith(before[LEDGER]),
    new_rows=len(rows), no_duplicate_ledger_paths=True,
    frozen_curation_files_verified=len(handoff['files']), actual_sdk_inputs_verified=len(sdk_inputs),
    inventory_sha256=sha((OUT / 'INVENTORY.json').read_bytes())))
print(json.dumps(dict(status='PASS', files=len(records), new_files=len(paths),
    provenance=result['status'], inventory=str(OUT / 'INVENTORY.json'),
    inventory_sha256=sha((OUT / 'INVENTORY.json').read_bytes())), indent=2))
