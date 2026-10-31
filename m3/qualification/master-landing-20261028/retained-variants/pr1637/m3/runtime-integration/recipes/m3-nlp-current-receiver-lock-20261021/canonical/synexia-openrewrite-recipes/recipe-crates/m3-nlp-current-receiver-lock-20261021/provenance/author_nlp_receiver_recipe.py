#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prepare a separate NLP lock task from the published, qualified core harness."""
from pathlib import Path
import difflib
import hashlib
import json

ROOT = Path('/workspace/scratch/206049dfb270')
CANON = ROOT / 'synexia'
MODULE = 'synexia-openrewrite-recipes'
OLD = 'm3-integrated-refinement-20261021'
TASK = 'm3-nlp-current-receiver-lock-20261021'
BASE = CANON / MODULE / 'recipe-crates' / OLD
DEST = CANON / MODULE / 'recipe-crates' / TASK
SOURCE_COMMIT = 'a2b50cfe926c5c5f400c7f659a947a5020fb8d4d'
sha = lambda data: hashlib.sha256(data).hexdigest()
inventory = json.loads((ROOT / 'canonical-work/publication-002/core/INVENTORY.json').read_text())
published = {row['path']: row for row in inventory['files']}


def write_new(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as stream:
        stream.write(data)


assert not DEST.exists()
adaptations = []
source_files = ['pom.xml', 'prepare_packet.py', 'qualify.py', 'apply_receiver.py', 'verify_sources.py',
                'src/test/java/com/synexia/rewrite/M3IntegratedRefinementRecipeTest.java']
for name in source_files:
    path = BASE / name
    before = path.read_bytes()
    key = path.relative_to(CANON).as_posix()
    assert sha(before) == published[key]['after_sha256'], key
    after = before.decode().replace(OLD, TASK).replace(
        'M3IntegratedRefinementRecipeTest', 'M3NlpCurrentReceiverLockRecipeTest').replace(
        'com.synexia.m3.IntegratedRefinement20261021', 'com.synexia.m3.NlpCurrentReceiverLock20261021').replace(
        'integrated-refinement-recovery-proof', 'nlp-current-receiver-lock-proof').replace(
        'M3 integrated refinement recovery 20261021', 'M3 current NLP receiver source lock 20261021').replace(
        'M3_INTEGRATED_RECOVERY_', 'M3_NLP_CURRENT_RECEIVER_LOCK_')
    if name == 'pom.xml':
        after = after.replace(
            '<!-- Modified 2026-10-21 from pinned PR12605: new integrated task and separate pending-memory conformance gate. -->',
            '<!-- Modified 2026-10-21 from published core #12648: distinct NLP lock packet; unchanged engines and scheduler harness; memory remains a separate gate. -->')
    elif name == 'qualify.py':
        old_suites = "    expected_suites = {\n        'com.synexia.rewrite.M3NlpCurrentReceiverLockRecipeTest',\n        'com.synexia.rewrite.memory.M3CorrectionMemoryConformanceTest'}"
        assert old_suites in after
        after = after.replace(old_suites,
            "    expected_suites = {'com.synexia.rewrite.M3NlpCurrentReceiverLockRecipeTest'}")
        assert "counts == {'tests': 2," in after
        after = after.replace("counts == {'tests': 2,", "counts == {'tests': 1,")
    if name.endswith('.py'):
        after = after.replace('# SPDX-License-Identifier: Apache-2.0\n',
            '# SPDX-License-Identifier: Apache-2.0\n# Adapted 2026-10-21 from published core #12648 for this distinct NLP lock task.\n', 1)
    if name.endswith('.java'):
        marker = ' * remain separate authorities.</p>'
        assert marker in after
        after = after.replace(marker,
            ' * remain separate authorities. Adapted 2026-10-21 from published core #12648\n'
            ' * for the distinct one-file NLP source-lock correction; scheduler gates and\n'
            ' * actual-owner attribution are retained. No core runtime or memory result\n'
            ' * is inherited by this packet.</p>')
    destination_name = name.replace('M3IntegratedRefinementRecipeTest', 'M3NlpCurrentReceiverLockRecipeTest')
    payload = after.encode()
    snapshot = 'provenance/donors/published-core/' + name
    diff = 'provenance/adaptations/' + Path(destination_name).name + '.diff'
    write_new(DEST / snapshot, before)
    write_new(DEST / destination_name, payload)
    difference = ''.join(difflib.unified_diff(before.decode().splitlines(True),
                         after.splitlines(True), fromfile=key,
                         tofile=MODULE + '/recipe-crates/' + TASK + '/' + destination_name))
    write_new(DEST / diff, difference.encode())
    adaptations.append(dict(source_path=key, source_revision=SOURCE_COMMIT,
        source_git_blob=published[key]['after_git_blob'], source_sha256=sha(before),
        snapshot=snapshot, target_path=destination_name, after_sha256=sha(payload), diff=diff,
        disposition='ADAPTED_UPSTREAM', license='Apache-2.0'))
lineage = dict(schema='M3_NLP_CURRENT_RECEIVER_LOCK_RECIPE_LINEAGE/1',
    task=TASK, status='FOUNDATION_ONLY_NOT_QUALIFIED',
    source_repository='hsoliwal/com.synexia', source_revision=SOURCE_COMMIT,
    source_pull_request='https://github.com/hsoliwal/com.synexia/pull/12648',
    canonical_engine_revision='e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07',
    shared_engine_changes=False, inherited_runtime_or_memory_results=False,
    adaptations=adaptations,
    scope='Task IDs, suite inventory and declared actual-owner attribution change; generic scheduler/refusal/fixed-point/replay/copy guards remain unchanged. Reviewed one-file candidate and owning runtime proof are separate prerequisites.')
write_new(DEST / 'provenance/RECIPE_LINEAGE.json', (json.dumps(lineage, indent=2) + '\n').encode())
write_new(DEST / 'provenance/author_nlp_receiver_recipe.py', Path(__file__).read_bytes())
print(json.dumps(dict(status='FOUNDATION_ONLY_NOT_QUALIFIED', crate=str(DEST),
                      source_files=len(adaptations), shared_engine_changes=False), indent=2))
