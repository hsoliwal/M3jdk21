#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Adapt the qualified inventory harness to one current SWT proof-POM correction."""
from pathlib import Path
import difflib
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
CANONICAL = ROOT/'synexia'
MODULE = CANONICAL/'synexia-openrewrite-recipes'
OLD = 'm3-integrated-refinement-20261021'
NEW = 'swt-current-owner-runtime-closure-20261021'
OLD_CLASS = 'M3IntegratedRefinementRecipeTest'
NEW_CLASS = 'M3SwtCurrentOwnerRuntimeClosureRecipeTest'
ORIGIN = MODULE/'recipe-crates'/OLD
ATTEMPT = ORIGIN/'proof/sdk-003-workflow-successor-offline'
FROZEN = ATTEMPT/'inputs/synexia-openrewrite-recipes/recipe-crates'/OLD
CRATE = MODULE/'recipe-crates'/NEW
NAMES = ['pom.xml','prepare_packet.py','qualify.py','apply_receiver.py','verify_sources.py',
         f'src/test/java/com/synexia/rewrite/{OLD_CLASS}.java']
sha = lambda data: hashlib.sha256(data).hexdigest()

def put(path,data):
    if path.exists():
        if path.read_bytes()!=data: raise ValueError('frozen authoring output differs: '+str(path))
        return
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('xb') as stream: stream.write(data)

def once(text,old,new):
    if text.count(old)!=1: raise ValueError('nonunique adaptation anchor: '+old)
    return text.replace(old,new)

result=json.loads((ATTEMPT/'RESULT.json').read_text())
assert result['status']=='PASS' and result['source_inputs_unchanged']
assert result['compiled_parser_dependencies_unchanged']
assert result['counts']=={'tests':2,'failures':0,'errors':0,'skipped':0}
records=[]
for name in NAMES:
    original=(FROZEN/name).read_bytes()
    assert original==(ORIGIN/name).read_bytes(), 'qualified donor drift: '+name
    put(CRATE/'provenance/donors/integrated-sdk-003'/name,original)
    content=original.decode().replace(OLD,NEW).replace(OLD_CLASS,NEW_CLASS)
    content=content.replace('com.synexia.m3.IntegratedRefinement20261021',
                            'com.synexia.swt.CurrentOwnerRuntimeClosure20261021')
    content=content.replace('M3 integrated refinement recovery 20261021',
                            'SWT current owner proof runtime closure 20261021')
    content=content.replace('M3_INTEGRATED_RECOVERY_SOURCE_SEALS/1',
                            'SWT_CURRENT_OWNER_RUNTIME_CLOSURE_SOURCE_SEALS/1')
    content=content.replace('M3_INTEGRATED_RECOVERY_SDK_ATTEMPT/1',
                            'SWT_CURRENT_OWNER_RUNTIME_CLOSURE_SDK_ATTEMPT/1')
    if name=='pom.xml':
        content=once(content,'integrated-refinement-recovery-proof','swt-current-owner-runtime-closure-proof')
        content=once(content,
            '<!-- Modified 2026-10-21 from pinned PR12605: new integrated task and separate pending-memory conformance gate. -->',
            '<!-- Modified 2026-10-21 from qualified integrated SDK003: separate one-file SWT proof-POM correction. -->')
    elif name=='qualify.py':
        content=once(content,
            "    expected_suites = {\n        'com.synexia.rewrite.M3SwtCurrentOwnerRuntimeClosureRecipeTest',\n        'com.synexia.rewrite.memory.M3CorrectionMemoryConformanceTest'}",
            "    expected_suites = {'com.synexia.rewrite.M3SwtCurrentOwnerRuntimeClosureRecipeTest'}")
        content=once(content,"and counts == {'tests': 2, 'failures': 0, 'errors': 0, 'skipped': 0} and source_pass)",
            "and counts == {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0} and source_pass)")
        content=once(content,'refusal check. This wrapper captures a fresh bounded attempt, exact input and',
            'refusal check. Adapted from qualified integrated SDK003 for the one-file SWT POM correction.\nThis wrapper captures a fresh bounded attempt, exact input and')
    elif name.endswith('.java'):
        content=once(content,
            ' * Dependency probes are source-sealed and limited to this packet; no old\n * 53-path or seven-event evidence is recovered. Product compiler, behavioral and native runtime gates\n * remain separate authorities.</p>',
            ' * Adapted again 2026-10-21 from qualified integrated SDK003 for one SWT\n * proof-POM dependency correction. All scheduler/refusal/replay gates remain.\n * Attribution binds the actual unchanged current SWT geometry owner. No old\n * runtime proof or correction-memory event is inherited. SWT native/platform\n * and Tycho gates remain separate authorities.</p>')
    if name=='prepare_packet.py':
        content=once(content,"SOURCE = '2779aba3f98f31c324be43f731be7914e1a05935'",
            "SOURCE = 'c0ffa6d76338ed1b06cfe0158b0e04d8930df546'")
        content=once(content,"CANONICAL = 'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07'",
            "CANONICAL = 'a2b50cfe926c5c5f400c7f659a947a5020fb8d4d'")
        content=once(content,"'receiving_repository': 'hsoliwal/M3jdk21'",
            "'receiving_repository': 'hsoliwal/eclipse.platform.swt'")
    if name in ('prepare_packet.py','apply_receiver.py','verify_sources.py'):
        content=content.replace('# SPDX-License-Identifier: Apache-2.0\n',
            '# SPDX-License-Identifier: Apache-2.0\n# Modified 2026-10-21 from qualified integrated SDK003 for one SWT proof-POM correction.\n',1)
    if name=='verify_sources.py':
        content=content.replace('runtime and source-built JDK gates are separate',
            'SWT native/platform and Tycho gates are separate')
    target_name=name.replace(OLD_CLASS,NEW_CLASS)
    encoded=content.encode()
    put(CRATE/target_name,encoded)
    diff=''.join(difflib.unified_diff(original.decode().splitlines(keepends=True),content.splitlines(keepends=True),
        fromfile='qualified-sdk003/'+name,tofile='swt-pom-correction/'+target_name))
    put(CRATE/'provenance/adaptations'/(Path(target_name).name+'.diff'),diff.encode())
    records.append({'source_path':str((FROZEN/name).relative_to(CANONICAL)),
        'source_sha256':sha(original),'source_bytes':len(original),'target_path':target_name,
        'after_sha256':sha(encoded),'disposition':'ADAPT_FIRST_PARTY_APACHE_2_0',
        'contract':'Task identity, one-suite inventory and actual SWT attribution; scheduler/refusal/replay algorithms unchanged'})
for name in ('LICENSE','NOTICE'):
    put(CRATE/'licenses'/('Synexia-'+name),(CANONICAL/name).read_bytes())
engines=[]
for name in ('M3HashPinnedJavaSnapshotRecipe.java','M3HashPinnedTextSnapshotRecipe.java','M3EditScope.java','M3ScopeInference.java'):
    path=MODULE/'src/main/java/com/synexia/rewrite'/name
    assert path.read_bytes()==(ATTEMPT/'inputs/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite'/name).read_bytes()
    engines.append({'path':path.relative_to(CANONICAL).as_posix(),'sha256':sha(path.read_bytes()),
                    'disposition':'UNCHANGED_SHARED_CANONICAL_OWNER'})
record={'schema':'SWT_CURRENT_OWNER_RUNTIME_CLOSURE_RECIPE_LINEAGE/1','task':NEW,
    'canonical_repository':'hsoliwal/com.synexia','qualified_harness_revision':'a2b50cfe926c5c5f400c7f659a947a5020fb8d4d',
    'canonical_engine_revision':'e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07',
    'receiving_repository':'hsoliwal/eclipse.platform.swt','receiving_revision':'c0ffa6d76338ed1b06cfe0158b0e04d8930df546',
    'donor_qualification':{'path':str((ATTEMPT/'RESULT.json').relative_to(CANONICAL)),
        'sha256':sha((ATTEMPT/'RESULT.json').read_bytes()),'status':'PASS','scope':'Harness lineage only, not this task proof'},
    'adaptations':records,'unchanged_engines':engines,'memory':'Fresh SWT observations only, no inherited task events',
    'publication':'No target, registry, ledger or branch changes by this authoring step'}
put(CRATE/'provenance/RECIPE_LINEAGE.json',(json.dumps(record,indent=2)+'\n').encode())
print(json.dumps({'status':'SCAFFOLD_AUTHORED','crate':str(CRATE),'adapted_sources':len(records),'unchanged_engines':len(engines)}))
