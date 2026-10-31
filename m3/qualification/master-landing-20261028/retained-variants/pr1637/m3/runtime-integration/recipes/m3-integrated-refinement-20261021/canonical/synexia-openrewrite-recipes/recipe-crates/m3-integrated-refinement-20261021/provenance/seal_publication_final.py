#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Seal two additive publication groups using the existing provenance validator."""
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
TASK = 'm3-integrated-refinement-20261021'
CORE = 'synexia-openrewrite-recipes/recipe-crates/' + TASK
SWT = 'synexia-openrewrite-recipes/recipes/widget-backing-20261014/scrollbar-fullrange-20261020'
MODULE = 'synexia-openrewrite-recipes'
REGISTRY = MODULE + '/CANONICAL_RECIPE_HOME.tsv'
LEDGER = 'docs/M3-SCALE/provenance/M3_FILE_PROVENANCE.tsv'
BASE = 'ad85831eda771cbd050124c806b45c050e545984'
BASE_TREE = 'ccc786139978c5d70d700850be0ac35835e53795'
SOURCE = '2779aba3f98f31c324be43f731be7914e1a05935'
CANONICAL = 'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07'
REGISTRY_SHA = 'b6ac38168e52bd316f19b255e778572a9e925d49fc2f1aa6a9e520fc88395cd2'
LEDGER_SHA = 'b106e0323fe7000ab53dbf0d5c839b6772399643858e7a9c976ec176d450a780'
FIELDS = ['path', 'classification', 'source_url', 'source_revision', 'source_path',
          'license_id', 'notice_path', 'disposition', 'evidence']


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


def collect(prefix):
    root = CANON / prefix
    return [path for path in sorted(root.rglob('*')) if path.is_file()
            and '__pycache__' not in path.parts and path.suffix not in ('.class', '.pyc')
            and path.relative_to(root).parts[0] != 'target']


def license_of(data, source_path=''):
    text = data.decode('utf-8', errors='strict')
    if 'ScheduledExecutorService.java' in source_path:
        return 'GPL-2.0-only WITH Classpath-exception-2.0'
    if 'SPDX-License-Identifier: GPL-2.0-only' in text[:6000]:
        return 'GPL-2.0-only'
    if 'Apache-2.0 OR EPL-2.0' in text[:6000]:
        return 'Apache-2.0 OR EPL-2.0'
    if 'GNU General Public License' in text[:6000]:
        return 'GPL-2.0-only WITH Classpath-exception-2.0' if 'Classpath' in text[:7000] else 'GPL-2.0-only'
    return 'Apache-2.0'


failed_registry = (ROOT / 'canonical-work/publication/core/CANONICAL_RECIPE_HOME.tsv').read_bytes()
failed_ledger = (ROOT / 'canonical-work/publication/core/M3_FILE_PROVENANCE.tsv').read_bytes()
assert (CANON / REGISTRY).read_bytes() == failed_registry
assert (CANON / LEDGER).read_bytes() == failed_ledger
registry_before = (ROOT / 'canonical-work/publication/base/CANONICAL_RECIPE_HOME.tsv').read_bytes()
ledger_before = (ROOT / 'canonical-work/publication/base/M3_FILE_PROVENANCE.tsv').read_bytes()
assert sha(registry_before) == REGISTRY_SHA and sha(ledger_before) == LEDGER_SHA
assert registry_before.endswith(b'\n') and ledger_before.endswith(b'\n')
out = ROOT / 'canonical-work/publication-002'
out.mkdir(exist_ok=False)
write_new(out / 'base/CANONICAL_RECIPE_HOME.tsv', registry_before)
write_new(out / 'base/M3_FILE_PROVENANCE.tsv', ledger_before)

# Exact source identities take precedence over derived artifact classifications.
origins = {}
readbacks = json.loads((CANON / CORE / 'provenance/RECOVERY_SOURCE_READBACKS.json').read_text())
for row in readbacks['sources']:
    origins[row['sha256']] = {'repository': row['source_repository'], 'revision': row['source_revision'],
                             'path': row['source_path'], 'classification': 'PRESERVED_HISTORY',
                             'disposition': 'EXACT_PINNED_SOURCE'}
initial = CANON / CORE / 'proof/sdk-002-offline'
initial_seals = json.loads((initial / 'inputs' / CORE / 'SOURCE_SEALS.json').read_text())
target_rows = {}
for row in initial_seals['files']:
    target_rows[row['after_sha256']] = row
    if row['before_sha256'] != 'ABSENT':
        origins[row['before_sha256']] = {'repository': 'hsoliwal/M3jdk21', 'revision': SOURCE,
            'path': row['target_path'], 'classification': 'PRESERVED_HISTORY', 'disposition': 'EXACT_RECEIVING_PREIMAGE'}
    origins.setdefault(row['after_sha256'], {'repository': 'hsoliwal/M3jdk21',
        'revision': 'task-sha256:' + row['after_sha256'], 'path': row['target_path'],
        'classification': 'PRESERVED_HISTORY', 'disposition': 'RETAINED_UNCOMMITTED_RECIPE_POSTIMAGE'})
current_seals = json.loads((CANON / CORE / 'SOURCE_SEALS.json').read_text())
for row in current_seals['files']:
    target_rows[row['after_sha256']] = row
    origins.setdefault(row['after_sha256'], {'repository': 'hsoliwal/M3jdk21',
        'revision': 'task-sha256:' + row['after_sha256'], 'path': row['target_path'],
        'classification': 'PRESERVED_HISTORY', 'disposition': 'RETAINED_UNCOMMITTED_RECIPE_POSTIMAGE'})
for path in sorted((ROOT / 'collections-work/donors/pr1554').glob('*.java')):
    data = path.read_bytes()
    owner = 'src/main/java' if path.name in ('M3FilteredViews.java', 'M3FilteredNavigableSet.java') else 'src/test/java'
    origins[sha(data)] = {'repository': 'hsoliwal/M3jdk21', 'revision': '987f743658055252bdaf056610a3a903eba0bfee',
        'path': 'm3/collections/' + owner + '/com/m3/collections/' + path.name,
        'classification': 'PRESERVED_HISTORY', 'disposition': 'EXACT_PR1554_DONOR'}
launcher = ROOT / 'collections-work/donors/pr1618/launcher-verify_source.py'
origins[sha(launcher.read_bytes())] = {'repository': 'hsoliwal/M3jdk21',
    'revision': '6198bd110f16a09c572324d716f181c92ca84d38',
    'path': 'm3/vendor/third_party/junit-platform-launcher-1.10.2/verify_source.py',
    'classification': 'PRESERVED_HISTORY', 'disposition': 'EXACT_PR1618_DONOR'}
for path in (CANON / CORE / 'proof/target-observations-001/eclipse/eclipse-contract-donor-01').rglob('*.java'):
    if path.name in ('M3EclipseJobs.java', 'M3EclipseJobScheduler.java', 'M3EclipseJobsTest.java', 'M3EclipseJobSchedulerTest.java'):
        kind = 'main' if path.name in ('M3EclipseJobs.java', 'M3EclipseJobScheduler.java') else 'test'
        origins[sha(path.read_bytes())] = {'repository': 'hsoliwal/M3jdk21',
            'revision': '6b75e71559c09c9212ef051f2d3927202eaa5819',
            'path': 'm3/eclipse/core-runtime/src/' + kind + '/java/com/m3/eclipse/runtime/' + path.name,
            'classification': 'PRESERVED_HISTORY', 'disposition': 'EXACT_PR1576_DONOR'}
donor_root = MODULE + '/recipe-crates/m3-string-ast-index-views-recovery-20261020/'
adapted = {
    'pom.xml': 'pom.xml', 'apply_receiver.py': 'apply_receiver.py',
    'src/test/java/com/synexia/rewrite/M3IntegratedRefinementRecipeTest.java': 'src/test/java/com/synexia/rewrite/M3StringAstIndexViewsRecipeTest.java',
    'src/test/java/com/synexia/rewrite/memory/M3RecoveryMemory.java': 'memory/tooling/src/com/synexia/rewrite/memory/M3RecoveryMemory.java'}
for path, source in adapted.items():
    origins[sha((CANON / CORE / path).read_bytes())] = {'repository': 'hsoliwal/com.synexia',
        'revision': 'b6c35b9a1f528dd9e5a3d9f6f4c51ca360db43d7', 'path': donor_root + source,
        'classification': 'UPSTREAM_MODIFIED', 'disposition': 'ADAPTED_UPSTREAM'}

core_paths = collect(CORE)
core_paths += [CANON / MODULE / 'src/main/resources/META-INF/rewrite' / (TASK + '.yml')]
for kind in ('java', 'text'):
    owner = CANON / MODULE / 'src/main/resources/com/synexia/rewrite' / ('hash-pinned-' + kind)
    for phase in sorted(owner.glob(TASK + '-*')):
        core_paths += [p for p in phase.iterdir() if p.is_file()]
assert len(core_paths) == len(set(core_paths))
assert not (CANON / '.github/workflows/swt-scrollbar-fullrange-20261020.yml').exists()

# First-party proof records remain clearly distinguished from copied source fixtures.
source_pin = sha((CANON / CORE / 'SOURCE_SEALS.json').read_bytes())
def row_for(path, family):
    relative = path.relative_to(CANON).as_posix()
    data = path.read_bytes()
    digest = sha(data)
    origin = origins.get(digest)
    text = data.decode('utf-8', errors='strict')
    if origin:
        row = {'path': relative, 'classification': origin['classification'],
               'source_url': 'https://github.com/' + origin['repository'],
               'source_revision': origin['revision'], 'source_path': origin['path'],
               'license_id': license_of(data, origin['path']), 'notice_path': 'LICENSE',
               'disposition': origin['disposition']}
    else:
        canonical_header = ('SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project' in '\n'.join(text.splitlines()[:80])
                            and 'SPDX-License-Identifier: Apache-2.0' in '\n'.join(text.splitlines()[:80]))
        source_fixture = path.suffix == '.java' or ('postimage.' in path.name) or path.name.endswith('.java.txt')
        if source_fixture:
            classification, disposition = 'PRESERVED_HISTORY', 'RETAINED_TASK_SOURCE_FIXTURE'
        elif canonical_header and checker.is_commentable(relative, policy):
            classification, disposition = 'FIRST_PARTY', 'NEW_FIRST_PARTY_RECIPE_OR_DOCUMENT'
        elif '/proof/' in relative or path.suffix in ('.log', '.diff'):
            classification, disposition = 'GENERATED', 'RETAINED_EXECUTION_OR_PREPARATION_EVIDENCE'
        elif path.suffix in ('.json', '.jsonl', '.tsv', '.txt'):
            classification, disposition = 'FIRST_PARTY_NO_HEADER', 'FIRST_PARTY_METADATA'
        else:
            classification, disposition = 'PRESERVED_HISTORY', 'RETAINED_TASK_PREPARATION_SOURCE'
        row = {'path': relative, 'classification': classification,
               'source_url': 'https://github.com/hsoliwal/com.synexia',
               'source_revision': 'task-sha256:' + source_pin,
               'source_path': relative, 'license_id': license_of(data, relative),
               'notice_path': 'LICENSE', 'disposition': disposition}
    if row['license_id'].startswith('GPL-2.0'):
        row['notice_path'] = CORE + '/licenses/M3JDK21-LICENSE'
    row['evidence'] = CORE + '/QUALIFICATION.md' if family == 'core' else SWT + '/proof/20261021/QUALIFICATION.md'
    return row


def tsv(rows):
    stream = io.StringIO()
    writer = csv.DictWriter(stream, fieldnames=FIELDS, delimiter='\t', lineterminator='\n')
    writer.writerows(rows)
    return stream.getvalue().encode()


def registry_addition(family, descriptors):
    lines = []
    for kind, path, symbol in descriptors:
        data = (CANON / path).read_bytes()
        lines.append('\t'.join(['SYNEXIA_CANONICAL_RECIPE_HOME_V1', family, kind, path, symbol,
                                blob(data), 'Apache-2.0', 'PINNED_DRAFT_QUALIFICATION_ONLY']))
    return ('\n'.join(lines) + '\n').encode()


spec = importlib.util.spec_from_file_location('existing_provenance_owner', CANON / 'scripts/legal/check_m3_provenance.py')
checker = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = checker
spec.loader.exec_module(checker)
policy = checker.load_policy(CANON / 'docs/M3-SCALE/provenance/m3-provenance-policy.json')


def seal_group(name, paths, registry_base, ledger_base, addition):
    paths = sorted(paths)
    result_path = CANON / (CORE + '/proof/PUBLICATION_PROVENANCE_CHECK_002.json' if name == 'core'
                           else SWT + '/proof/20261021/PUBLICATION_PROVENANCE_CHECK.json')
    put_json(result_path, {'status': 'PENDING_EXISTING_VALIDATOR', 'scope': name})
    paths.append(result_path)
    paths.sort()
    rows = [row_for(path, name) for path in paths]
    prior = list(csv.DictReader((line for line in ledger_base.decode().splitlines() if line and not line.startswith('#')), delimiter='\t'))
    assert not ({row['path'] for row in rows} & {row['path'] for row in prior})
    registry = registry_base + addition
    ledger = ledger_base + tsv(rows)
    group = out / name
    write_new(group / 'CANONICAL_RECIPE_HOME.tsv', registry)
    write_new(group / 'M3_FILE_PROVENANCE.tsv', ledger)
    (CANON / REGISTRY).write_bytes(registry)
    (CANON / LEDGER).write_bytes(ledger)
    actual_ledger = checker.load_ledger(CANON / LEDGER, set(policy['allowed_classifications']))
    violations = []
    for path in paths:
        violations.extend(checker.validate_change(checker.Change('A', path.relative_to(CANON).as_posix()), CANON, policy, actual_ledger))
    for relative in (REGISTRY, LEDGER):
        violations.extend(checker.validate_change(checker.Change('M', relative), CANON, policy, actual_ledger))
    result = {'schema': 'M3_FOCUSED_PUBLICATION_PROVENANCE_CHECK/1',
        'status': 'PASS' if not violations else 'FAIL', 'scope': name,
        'existing_validator': 'scripts/legal/check_m3_provenance.py',
        'validator_sha256': sha((CANON / 'scripts/legal/check_m3_provenance.py').read_bytes()),
        'invocation': 'Imported existing load_policy/load_ledger/validate_change on this exact additive file set; bounded hydration has no Git checkout for the owner CLI.',
        'new_files': len(paths), 'modified_append_owners': 2,
        'new_provenance_rows': len(rows), 'classifications': dict(collections.Counter(row['classification'] for row in rows)),
        'prior_registry_bytes_retained': registry.startswith(registry_base),
        'prior_ledger_bytes_retained': ledger.startswith(ledger_base),
        'violations': [vars(v) for v in violations],
        'limits': 'Exact changed-file metadata/header gate only; this is not a legal opinion, license regrant, runtime test, whole-reactor or hosted CI result.'}
    result_path.write_text(json.dumps(result, indent=2) + '\n')
    assert not violations, violations
    records = []
    for path in paths:
        data = path.read_bytes()
        records.append({'path': path.relative_to(CANON).as_posix(), 'local_path': str(path),
                        'before_git_blob': 'ABSENT', 'before_sha256': 'ABSENT',
                        'after_git_blob': blob(data), 'after_sha256': sha(data),
                        'mode': '100644', 'bytes': len(data)})
    for relative, filename, before, after in ((REGISTRY, 'CANONICAL_RECIPE_HOME.tsv', registry_base, registry),
                                             (LEDGER, 'M3_FILE_PROVENANCE.tsv', ledger_base, ledger)):
        records.append({'path': relative, 'local_path': str(group / filename),
                        'before_git_blob': blob(before), 'before_sha256': sha(before),
                        'after_git_blob': blob(after), 'after_sha256': sha(after),
                        'mode': '100644', 'bytes': len(after)})
    inventory = {'schema': 'M3_CANONICAL_ADDITIVE_PUBLICATION_INVENTORY/1', 'group': name,
                 'base_revision': BASE if name == 'core' else 'PUBLISHED_CORE_COMMIT_REQUIRED',
                 'base_tree': BASE_TREE if name == 'core' else 'PUBLISHED_CORE_TREE_REQUIRED',
                 'publication_root_revision': BASE, 'authoring_revision': CANONICAL,
                 'files': records, 'new_files': len(paths), 'modified_files': 2,
                 'deletions': 0, 'shared_engine_edits': 0,
                 'registry_append_bytes': len(addition), 'ledger_append_rows': len(rows),
                 'draft_required': True,
                 'source_overrides': 'Use local_path for both append owners; the core override preserves the intermediate state before SWT additions.',
                 'qualification': result_path.relative_to(CANON).as_posix()}
    put_json(group / 'INVENTORY.json', inventory)
    put_json(group / 'APPEND_PROOF.json', {'status': 'PASS', 'base_revision': BASE,
        'registry_before_git_blob': blob(registry_base), 'registry_after_git_blob': blob(registry),
        'ledger_before_git_blob': blob(ledger_base), 'ledger_after_git_blob': blob(ledger),
        'registry_prefix_exact': registry[:len(registry_base)] == registry_base,
        'ledger_prefix_exact': ledger[:len(ledger_base)] == ledger_base,
        'new_rows': len(rows), 'no_duplicate_ledger_paths': True,
        'inventory_sha256': sha((group / 'INVENTORY.json').read_bytes())})
    print(json.dumps({'group': name, 'files': len(records), 'new_files': len(paths),
                      'provenance': result['status'], 'inventory': str(group / 'INVENTORY.json')}), flush=True)
    return registry, ledger


core_add = registry_addition(TASK, [
    ('RECIPE_DESCRIPTOR', MODULE + '/src/main/resources/META-INF/rewrite/' + TASK + '.yml', 'com.synexia.m3.IntegratedRefinement20261021'),
    ('MAVEN_MODULE', CORE + '/pom.xml', 'com.synexia.m3:integrated-refinement-recovery-proof'),
    ('JAVA_TEST', CORE + '/src/test/java/com/synexia/rewrite/M3IntegratedRefinementRecipeTest.java', 'com.synexia.rewrite.M3IntegratedRefinementRecipeTest')])
core_registry, core_ledger = seal_group('core', core_paths, registry_before, ledger_before, core_add)

# Original SWT proposal bytes have their own finite source custody and proof lane.
swt_paths = collect(SWT)
swt_paths += [CANON / MODULE / 'src/main/resources/META-INF/rewrite/swt-scrollbar-fullrange-20261020.yml']
swt_resources = CANON / MODULE / 'src/main/resources/com/synexia/rewrite/hash-pinned-java/swt-scrollbar-fullrange-20261020'
swt_paths += [path for path in swt_resources.iterdir() if path.is_file()]
copy_record = json.loads((CANON / SWT / 'proof/20261021/SOURCE_COPY.json').read_text())
for row in copy_record['files']:
    source_path = row['path']
    origins[row['sha256']] = {'repository': 'hsoliwal/com.synexia',
        'revision': 'task-sha256:' + row['sha256'], 'path': 'retained-proposal/' + source_path,
        'classification': 'PRESERVED_HISTORY', 'disposition': 'EXACT_ORIGINAL_TASK_PROPOSAL'}
pins = json.loads((CANON / SWT / 'SOURCE_PINS.json').read_text())
origins[pins['beforeSha256']] = {'repository': 'hsoliwal/eclipse.platform.swt',
    'revision': pins['targetBase'], 'path': pins['productionPath'],
    'classification': 'PRESERVED_HISTORY', 'disposition': 'EXACT_SWT_RECEIVING_PREIMAGE'}
origins[pins['candidateSha256']] = {'repository': 'hsoliwal/eclipse.platform.swt',
    'revision': pins['targetBase'], 'path': pins['productionPath'],
    'classification': 'UPSTREAM_MODIFIED', 'disposition': 'MODIFIED_UPSTREAM'}
swt_add = registry_addition('swt-scrollbar-fullrange-20261020', [
    ('RECIPE_DESCRIPTOR', MODULE + '/src/main/resources/META-INF/rewrite/swt-scrollbar-fullrange-20261020.yml', 'com.synexia.swt.ScrollbarFullRange20261020'),
    ('MAVEN_MODULE', SWT + '/pom.xml', 'com.synexia.verification:swt-scrollbar-fullrange-20261020'),
    ('JAVA_TEST', SWT + '/src/test/java/com/synexia/rewrite/SwtScrollbarFullRangeRecipeTest.java', 'com.synexia.rewrite.SwtScrollbarFullRangeRecipeTest')])
seal_group('swt', swt_paths, core_registry, core_ledger, swt_add)
