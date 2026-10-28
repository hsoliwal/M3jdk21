#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run exact canonical sources in a fresh temporary build, retaining text evidence."""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[2]
TASK='swt-current-owner-runtime-closure-20261021'
CANONICAL=ROOT/'synexia'
REL=Path('synexia-openrewrite-recipes/recipe-crates')/TASK
CRATE=CANONICAL/REL
WORK=Path(__file__).resolve().parent
TEMP=Path('/tmp/m3-swt-current-owner-sdk-001/canonical')
TEMP.mkdir(parents=True,exist_ok=False)
sys.dont_write_bytecode=True
sys.path.insert(0,str(CRATE))
spec=importlib.util.spec_from_file_location('qualified_task_inputs',CRATE/'qualify.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
inputs=module.input_files(CRATE)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before={p.relative_to(CANONICAL).as_posix():sha(p) for p in inputs}
for source in inputs:
    destination=TEMP/source.relative_to(CANONICAL)
    destination.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(source,destination)
local=TEMP/REL
command=['python3','-B',str(local/'qualify.py'),
    '--java-home','/tmp/m3-build-tools/jdk-21.0.12.1+1',
    '--maven','/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn',
    '--maven-repo','/tmp/m3-build-m2','--parser-classpath',str(WORK/'parser-classpath.txt'),
    '--attempt','sdk-001-offline','--timeout-seconds','300']
(WORK/'SDK_001_COMMAND.json').write_text(json.dumps({'argv':command,'cwd':str(TEMP),
    'purpose':'Build exact frozen canonical inputs outside shared workspace build directories'},indent=2)+'\n')
with (WORK/'SDK_001.log').open('w') as log:
    result=subprocess.run(command,cwd=TEMP,stdout=log,stderr=subprocess.STDOUT,timeout=330)
attempt=local/'proof/sdk-001-offline'
if attempt.exists():
    destination=CRATE/'proof/sdk-001-offline'
    destination.parent.mkdir(parents=True,exist_ok=True)
    shutil.copytree(attempt,destination)
unchanged=before=={p.relative_to(CANONICAL).as_posix():sha(p) for p in inputs}
assert unchanged,'canonical authoring inputs changed during isolated execution'
assert before=={p.relative_to(TEMP).as_posix():sha(p) for p in module.input_files(local)},'isolated source drift'
if result.returncode==0:
    target=CRATE/'target';target.mkdir(exist_ok=False)
    for name in ('recipe-proof.tsv','proof-status.tsv','named-recipe.yml'):
        shutil.copyfile(local/'target'/name,target/name)
    for name in ('phase-receipts','receiver'):
        shutil.copytree(local/'target'/name,target/name)
receipt={'schema':'SWT_CANONICAL_ISOLATED_SDK_CUSTODY/1',
    'status':'PASS' if result.returncode==0 else 'FAIL','exit_code':result.returncode,
    'command':command,'source_inputs':before,'source_inputs_unchanged':unchanged,
    'task_receipt':str(CRATE/'proof/sdk-001-offline/RESULT.json'),
    'task_receipt_sha256':sha(CRATE/'proof/sdk-001-offline/RESULT.json'),
    'compiled_outputs_retained_in_temporary_directory_only':True}
(WORK/'SDK_001_CUSTODY.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps({key:receipt[key] for key in ('status','exit_code','source_inputs_unchanged','task_receipt','task_receipt_sha256')}))
raise SystemExit(result.returncode)
