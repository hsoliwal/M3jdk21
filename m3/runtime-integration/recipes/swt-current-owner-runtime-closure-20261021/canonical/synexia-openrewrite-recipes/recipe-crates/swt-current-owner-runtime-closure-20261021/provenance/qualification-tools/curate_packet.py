#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Preserve finite SWT proof inputs and receipts; no binary build outputs."""
from pathlib import Path
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
WORK=Path(__file__).resolve().parent
BASE=ROOT/'jobs-work/swt-receiver-current-001'
CRATE=ROOT/'synexia/synexia-openrewrite-recipes/recipe-crates/swt-current-owner-runtime-closure-20261021'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
records=[]

def put(path,data):
    if path.exists():
        if path.read_bytes()!=data:raise ValueError('frozen evidence differs: '+str(path))
        return
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('xb') as stream:stream.write(data)

def intake(source,relative,scope):
    destination=CRATE/relative
    put(destination,source.read_bytes())
    assert sha(destination)==sha(source)
    records.append({'source_path':str(source),'repository_path':destination.relative_to(ROOT/'synexia').as_posix(),
        'bytes':source.stat().st_size,'sha256':sha(source),'scope':scope})

inputs=json.loads((BASE/'evidence-001/INPUTS.json').read_text())
for row in inputs:
    source=BASE/'source'/row['path']
    if sha(source)!=row['sha256']:
        source=CRATE/'preimages'/row['path']
    assert sha(source)==row['sha256']
    intake(source,'proof/incumbent-inputs/'+row['path'],'Pinned current SWT source and existing proof/test/workflow input; not a new edit')

for name in ('RESULT.json','INPUTS.json','INPUTS_AFTER.json','COMMANDS.json','sdk.log'):
    intake(BASE/'evidence-001'/name,'proof/incumbent-offline-001/'+name,'Zero tests; missing offline clean-plugin3.2.0')
for name in ('RESULT.json','COMMAND.json','maven.log'):
    intake(BASE/'bootstrap-001'/name,'proof/incumbent-bootstrap-001/'+name,'Actual7test failure from missing SLF4J API')
for path in sorted((BASE/'bootstrap-001/surefire-reports').iterdir()):
    if path.is_file() and not path.name.startswith('.'):
        intake(path,'proof/incumbent-bootstrap-001/surefire-reports/'+path.name,'Actual7test failure reports')
for name in ('RESULT.json','INPUTS.json','INPUTS_AFTER.json','COMMANDS.json','sdk.log'):
    intake(BASE/'evidence-002'/name,'proof/incumbent-offline-002/'+name,'Zero tests; transient compiled-class discovery infrastructure refusal')
for path in sorted((BASE/'evidence-002/surefire-reports').iterdir()):
    if path.is_file() and path.suffix in ('.dump','.dumpstream'):
        intake(path,'proof/incumbent-offline-002/surefire-reports/'+path.name+'.txt','Discovery diagnostic, not test execution')

bootstrap_report=BASE/'bootstrap-001/surefire-reports/TEST-com.synexia.rewrite.ScrollbarFullRangeRecipeTest.xml'
suite=ET.parse(bootstrap_report).getroot()
counts={key:int(suite.get(key,'0')) for key in ('tests','failures','errors','skipped')}
assert counts=={'tests':7,'failures':4,'errors':3,'skipped':0}
assert 'Tests run: 0, Failures: 0, Errors: 0, Skipped: 0' in (BASE/'evidence-002/sdk.log').read_text()
stale=[]
for report in sorted((BASE/'evidence-002/surefire-reports').rglob('TEST-*.xml')):
    assert sha(report)==sha(bootstrap_report),'unexpected fresh report in zero-test attempt'
    stale.append({'path':str(report),'sha256':sha(report),'classification':'STALE_COPY_OF_BOOTSTRAP_001_NOT_EXECUTED_BY_ATTEMPT_002'})
classification={'schema':'SWT_CURRENT_OWNER_FAILURE_CLASSIFICATION/1',
    'attempts':[{'path':'proof/incumbent-offline-001','kind':'OFFLINE_DEPENDENCY_UNAVAILABLE','executed_tests':0,
        'signature':'maven-clean-plugin:3.2.0 missing from offline cache','product_source_changed':False},
        {'path':'proof/incumbent-bootstrap-001','kind':'OWNING_POM_RUNTIME_CLOSURE','executed_tests':7,
         'counts':counts,'signature':'java.lang.NoClassDefFoundError: org/slf4j/LoggerFactory',
         'correction':'Add existing qualified org.slf4j:slf4j-api:2.0.13 to the owning proof POM'},
        {'path':'proof/incumbent-offline-002','kind':'LOCAL_COMPILED_CLASS_DISCOVERY','executed_tests':0,
         'signature':"Unable to create test class 'com.synexia.rewrite..rsync-tmp.ScrollbarFullRangeRecipeTest'",
         'correction':'Fresh isolated temporary build directory; no selector, test, engine or production change'}],
    'stale_reports':stale,'stale_report_payloads':'Already retained once under incumbent-bootstrap-001; not counted again',
    'candidate_receipt':'proof/pom-candidate-001/RESULT.json',
    'runtime_scope':'Arithmetic only; native widget/Tycho/platform/display/performance gates not run'}
put(CRATE/'provenance/BASELINE_FAILURE_CLASSIFICATION.json',(json.dumps(classification,indent=2)+'\n').encode())

for path in sorted((WORK/'candidate-001').rglob('*')):
    if path.is_file() and not any(part.startswith('.') for part in path.relative_to(WORK/'candidate-001').parts):
        intake(path,'proof/pom-candidate-001/'+path.relative_to(WORK/'candidate-001').as_posix(),'Qualified POM authoring candidate; actual final emitted-POM receiver proof remains separate')
for path in sorted((WORK/'receiver-qualification-001').iterdir()):
    if path.is_file() and path.suffix in ('.json','.log'):
        intake(path,'proof/copier-qualification-001/'+path.name,'Actual qualified-output copier:1write,0replay,4guardrefusals')
for name in ('REMOTE_LINEAGE.json','HANDOFF.json','ATTRIBUTION.json','attribution-compile.log','parser-classpath.txt',
             'SDK_001_COMMAND.json','SDK_001.log','SDK_001_CUSTODY.json'):
    intake(WORK/name,'provenance/intake/'+name,'Exact task pin, byte custody or command receipt')
for name in ('author_packet.py','run_candidate_001.py','run_canonical_sdk_001.py','curate_packet.py'):
    intake(WORK/name,'provenance/qualification-tools/'+name,'Reproducible authoring/qualification/intake tool; not transformation authority')
for name in ('run_owner_gate.py','run_owner_gate_002.py','bootstrap_owner_gate.py'):
    intake(BASE/name,'provenance/qualification-tools/incumbent/'+name,'Original failed-attempt command orchestrator retained unchanged')

manifest={'schema':'SWT_CURRENT_OWNER_EVIDENCE_INTAKE/1','files':records,
    'copied_files':len(records),'total_bytes':sum(row['bytes'] for row in records),
    'excluded':'Compiled classes, JAR payloads, caches, temporary receiver trees and duplicate stale XML bytes',
    'readback':'PASS'}
put(CRATE/'provenance/EVIDENCE_INTAKE.json',(json.dumps(manifest,indent=2)+'\n').encode())
print(json.dumps({key:manifest[key] for key in ('copied_files','total_bytes','readback')}))
