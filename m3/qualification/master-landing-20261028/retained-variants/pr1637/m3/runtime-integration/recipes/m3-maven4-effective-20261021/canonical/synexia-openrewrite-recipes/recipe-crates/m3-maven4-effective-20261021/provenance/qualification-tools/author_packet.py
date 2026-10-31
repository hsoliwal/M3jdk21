#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Adapt the qualified task harness without editing shared recipe engines."""
from pathlib import Path
import difflib
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
CANONICAL = ROOT / 'synexia'
MODULE = CANONICAL / 'synexia-openrewrite-recipes'
OLD = 'm3-integrated-refinement-20261021'
NEW = 'm3-maven4-effective-20261021'
ORIGIN = MODULE / 'recipe-crates' / OLD
ATTEMPT = ORIGIN / 'proof/sdk-003-workflow-successor-offline'
FROZEN = ATTEMPT / 'inputs/synexia-openrewrite-recipes/recipe-crates' / OLD
CRATE = MODULE / 'recipe-crates' / NEW
OLD_CLASS = 'M3IntegratedRefinementRecipeTest'
NEW_CLASS = 'M3Maven4EffectiveRecipeTest'
NAMES = [
    'pom.xml', 'prepare_packet.py', 'qualify.py', 'apply_receiver.py',
    'verify_sources.py', f'src/test/java/com/synexia/rewrite/{OLD_CLASS}.java',
]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def put(path, data):
    if path.exists():
        if path.read_bytes() != data:
            raise ValueError('authoring output already differs: ' + str(path))
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as stream:
        stream.write(data)


def replace_once(text, old, new):
    if text.count(old) != 1:
        raise ValueError('nonunique adaptation anchor: ' + old)
    return text.replace(old, new)


result = json.loads((ATTEMPT / 'RESULT.json').read_text())
assert result['status'] == 'PASS'
assert result['source_inputs_unchanged'] and result['compiled_parser_dependencies_unchanged']
assert result['counts'] == {'tests': 2, 'failures': 0, 'errors': 0, 'skipped': 0}
records = []
for name in NAMES:
    source = FROZEN / name
    original = source.read_bytes()
    assert original == (ORIGIN / name).read_bytes(), 'current donor drift: ' + name
    put(CRATE / 'provenance/donors/integrated-sdk-003' / name, original)
    content = original.decode('utf-8')
    content = content.replace(OLD, NEW).replace(OLD_CLASS, NEW_CLASS)
    content = content.replace('com.synexia.m3.IntegratedRefinement20261021',
                              'com.synexia.m3.Maven4Effective20261021')
    content = content.replace('M3 integrated refinement recovery 20261021',
                              'M3 Maven4 effective model projection 20261021')
    content = content.replace('M3_INTEGRATED_RECOVERY_SOURCE_SEALS/1',
                              'M3_MAVEN4_EFFECTIVE_SOURCE_SEALS/1')
    content = content.replace('M3_INTEGRATED_RECOVERY_SDK_ATTEMPT/1',
                              'M3_MAVEN4_EFFECTIVE_SDK_ATTEMPT/1')
    if name == 'pom.xml':
        content = replace_once(content, 'integrated-refinement-recovery-proof', 'maven4-effective-proof')
        content = replace_once(content,
            '<!-- Modified 2026-10-21 from pinned PR12605: new integrated task and separate pending-memory conformance gate. -->',
            '<!-- Modified 2026-10-21 from qualified integrated SDK003: separate Maven4 task; no inherited task-memory claims. -->')
    elif name == 'qualify.py':
        content = replace_once(content,
            "    expected_suites = {\n        'com.synexia.rewrite.M3Maven4EffectiveRecipeTest',\n        'com.synexia.rewrite.memory.M3CorrectionMemoryConformanceTest'}",
            "    expected_suites = {'com.synexia.rewrite.M3Maven4EffectiveRecipeTest'}")
        content = replace_once(content,
            "and counts == {'tests': 2, 'failures': 0, 'errors': 0, 'skipped': 0} and source_pass)",
            "and counts == {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0} and source_pass)")
        content = replace_once(content, 'This wrapper captures a fresh bounded attempt, exact input and',
            'Adapted from qualified integrated SDK003 for this distinct Maven4 packet.\nThis wrapper captures a fresh bounded attempt, exact input and')
    elif name.endswith('.java'):
        content = replace_once(content,
            ' * Dependency probes are source-sealed and limited to this packet; no old\n * 53-path or seven-event evidence is recovered. Product compiler, behavioral and native runtime gates\n * remain separate authorities.</p>',
            ' * Adapted again 2026-10-21 from the qualified integrated SDK003 task for\n * the separate Maven4 effective-model packet, retaining all scheduler gates.\n * Dependency probes bind this packet\'s actual parser/Maven/JDT owners. No\n * integrated-task runtime results or correction-memory events are inherited.\n * Product compiler and behavioral gates remain separate authorities.</p>')
    elif name in ('prepare_packet.py', 'apply_receiver.py', 'verify_sources.py'):
        content = content.replace('# SPDX-License-Identifier: Apache-2.0\n',
            '# SPDX-License-Identifier: Apache-2.0\n'
            '# Modified 2026-10-21 from qualified integrated SDK003 for the separate Maven4 packet.\n', 1)
    target_name = name.replace(OLD_CLASS, NEW_CLASS)
    target = CRATE / target_name
    encoded = content.encode('utf-8')
    put(target, encoded)
    diff = ''.join(difflib.unified_diff(original.decode().splitlines(keepends=True),
        content.splitlines(keepends=True), fromfile='qualified-sdk003/' + name,
        tofile='maven4-packet/' + target_name))
    put(CRATE / 'provenance/adaptations' / (Path(target_name).name + '.diff'), diff.encode())
    records.append({'source_path': str(source.relative_to(CANONICAL)),
        'source_sha256': sha(original), 'source_bytes': len(original),
        'snapshot': 'provenance/donors/integrated-sdk-003/' + name,
        'target_path': target_name, 'after_sha256': sha(encoded),
        'disposition': 'ADAPT_FIRST_PARTY_APACHE_2_0',
        'contract': 'New task identity, suite inventory and attribution fixtures; scheduler/refusal/replay algorithms unchanged'})

for name in ('LICENSE', 'NOTICE'):
    data = (CANONICAL / name).read_bytes()
    put(CRATE / 'licenses' / ('Synexia-' + name), data)

engine_records = []
for name in ('M3HashPinnedJavaSnapshotRecipe.java', 'M3HashPinnedTextSnapshotRecipe.java',
             'M3EditScope.java', 'M3ScopeInference.java'):
    path = MODULE / 'src/main/java/com/synexia/rewrite' / name
    frozen = ATTEMPT / 'inputs/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite' / name
    assert path.read_bytes() == frozen.read_bytes()
    engine_records.append({'path': str(path.relative_to(CANONICAL)),
        'sha256': sha(path.read_bytes()), 'disposition': 'UNCHANGED_SHARED_CANONICAL_OWNER'})

record = {'schema': 'M3_MAVEN4_EFFECTIVE_RECIPE_LINEAGE_V1',
    'canonical_repository': 'hsoliwal/com.synexia',
    'canonical_engine_revision': 'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07',
    'receiving_repository': 'hsoliwal/M3jdk21',
    'receiving_origin': '2779aba3f98f31c324be43f731be7914e1a05935',
    'task': NEW, 'recipe': 'com.synexia.m3.Maven4Effective20261021',
    'donor_qualification': {'path': str((ATTEMPT / 'RESULT.json').relative_to(CANONICAL)),
        'sha256': sha((ATTEMPT / 'RESULT.json').read_bytes()),
        'status': 'PASS', 'scope': 'Inherited harness lineage only; not this new packet proof'},
    'adaptations': records, 'unchanged_shared_engines': engine_records,
    'memory': 'Core correction events are not Maven4 events. Exact Maven4 failures remain separately retained; shared correction-memory intake is pending.',
    'publication': 'No global registry, provenance ledger, branch or target mutation by this authoring step'}
put(CRATE / 'provenance/RECIPE_LINEAGE.json', (json.dumps(record, indent=2) + '\n').encode())
print(json.dumps({'status': 'SCAFFOLD_AUTHORED', 'crate': str(CRATE), 'adapted_sources': len(records),
                  'shared_engines_unchanged': len(engine_records)}))
