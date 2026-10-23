#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Append actual emitted-POM receiver evidence without altering prior receipts."""
from pathlib import Path
import hashlib
import json
import subprocess

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
CRATE=ROOT/'synexia/synexia-openrewrite-recipes/recipe-crates/swt-current-owner-runtime-closure-20261021'
RECEIVER=ROOT/'jobs-work/swt-receiver-current-001/source'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
records=[]

def put(path,data):
    if path.exists():
        if path.read_bytes()!=data:raise ValueError('frozen receiver evidence drift: '+str(path))
        return
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('xb') as stream:stream.write(data)

def copy(source,relative):
    target=CRATE/relative
    put(target,source.read_bytes())
    assert sha(target)==sha(source)
    records.append({'source_path':str(source),'repository_path':target.relative_to(ROOT/'synexia').as_posix(),
                    'sha256':sha(source),'bytes':source.stat().st_size})

runtime=HERE/'receiver-runtime-001'
receipt=json.loads((runtime/'RESULT.json').read_text())
assert receipt['status']=='PASS' and receipt['sdk_emitted_pom_consumed']
assert receipt['all_input_readbacks_unchanged'] and receipt['production_geometry_changes']==0
assert receipt['sdk_tests']=={'tests':7,'failures':0,'errors':0,'skipped':0}
application=ROOT/'integration-work/swt-receiver-001'
applied=json.loads((application/'RESULT.json').read_text())
assert applied['status']=='GENERATED_SWT_POM_APPLIED'
assert [row['writes'] for row in applied['steps']]==[1,1,0]
changed=subprocess.check_output(['git','-C',str(RECEIVER),'diff','--name-only','HEAD'],text=True).splitlines()
assert changed==['docs/m3/scrollbar-full-range-20261020/recipes/swt-scrollbar-full-range-20261020/pom.xml'],changed
for folder,prefix in [(runtime,'proof/receiver-runtime-001'),(application,'proof/receiver-application-001')]:
    for source in sorted(folder.rglob('*')):
        if source.is_file() and not any(part.startswith('.') for part in source.relative_to(folder).parts):
            assert source.suffix in ('.json','.log','.xml','.txt'),source
            copy(source,prefix+'/'+source.relative_to(folder).as_posix())
for name in ('verify_applied_receiver.py','append_receiver_evidence.py'):
    copy(HERE/name,'provenance/qualification-tools/'+name)
record={'schema':'SWT_CURRENT_OWNER_RECEIVER_EVIDENCE_INTAKE/1','readback':'PASS',
    'copied_files':len(records),'total_bytes':sum(row['bytes'] for row in records),
    'actual_git_diff_paths':changed,'files':records,'production_geometry_unchanged':True,
    'sdk_source_inputs_unchanged':True}
source_inputs=json.loads((CRATE/'proof/sdk-001-offline/SOURCE_INPUTS.json').read_text())
for row in source_inputs:
    assert sha(ROOT/'synexia'/row['path'])==row['sha256'],'qualified canonical input changed'
put(CRATE/'provenance/RECEIVER_EVIDENCE_INTAKE.json',(json.dumps(record,indent=2)+'\n').encode())
print(json.dumps({key:record[key] for key in ('readback','copied_files','total_bytes','production_geometry_unchanged')}))
