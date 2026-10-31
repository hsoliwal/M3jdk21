#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run the incumbent SWT gates on exact applied recipe output, without edits."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--receiver',required=True,type=Path)
args=parser.parse_args()
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
RECEIVER=args.receiver.resolve(strict=True)
CRATE=ROOT/'synexia/synexia-openrewrite-recipes/recipe-crates/swt-current-owner-runtime-closure-20261021'
OUT=HERE/'receiver-runtime-001';OUT.mkdir(exist_ok=False)
BUILD=Path('/tmp/m3-swt-current-owner-receiver-001');BUILD.mkdir(exist_ok=False)
POM='docs/m3/scrollbar-full-range-20261020/recipes/swt-scrollbar-full-range-20261020/pom.xml'
OWNER=BUILD/Path(POM).parent
GEOMETRY='bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/ViewportScrollBarGeometry.java'
JDK=Path('/tmp/m3-build-tools/jdk-21.0.12.1+1')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=subprocess.check_output(['git','-C',str(RECEIVER),'rev-parse','HEAD'],text=True).strip()
assert pin=='c0ffa6d76338ed1b06cfe0158b0e04d8930df546'
seals=json.loads((CRATE/'SOURCE_SEALS.json').read_text())['files']
assert len(seals)==1 and seals[0]['target_path']==POM
emitted=CRATE/'target/receiver'/POM
assert sha(emitted)==seals[0]['after_sha256']
assert (RECEIVER/POM).read_bytes()==emitted.read_bytes(),'actual receiver must first receive the emitted POM'
original=json.loads((CRATE/'proof/incumbent-offline-001/INPUTS.json').read_text())
inputs=[]
for row in original:
    source=RECEIVER/row['path']
    expected=seals[0]['after_sha256'] if row['path']==POM else row['sha256']
    assert sha(source)==expected,'applied receiver input drift: '+row['path']
    destination=BUILD/row['path'];destination.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(source,destination)
    inputs.append({'path':row['path'],'sha256':sha(source),'bytes':source.stat().st_size})
(OUT/'INPUTS.json').write_text(json.dumps(inputs,indent=2)+'\n')
commands=[('sdk',['/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn','-B','-ntp','-o',
    '-Dmaven.repo.local=/tmp/m3-build-m2','-f',str(BUILD/POM),'clean','test']),
    ('receiver',['python3',str(OWNER/'verify.py'),'--receiver',str(BUILD)])]
env=dict(os.environ,JAVA_HOME=str(JDK),PATH=str(JDK/'bin')+':'+os.environ.get('PATH',''),
    MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData')
(OUT/'COMMANDS.json').write_text(json.dumps({'commands':commands,'cwd':str(BUILD),
    'environment':{key:env[key] for key in ('JAVA_HOME','MAVEN_OPTS')},
    'java_release_sha256':sha(JDK/'release')},indent=2)+'\n')
status='FAIL';error=None;counts=None;steps=[];started=time.monotonic()
try:
    for label,command in commands:
        clock=time.monotonic();before_time=time.time()
        with (OUT/(label+'.log')).open('w') as log:
            completed=subprocess.run(command,cwd=BUILD,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=300)
        steps.append({'name':label,'argv':command,'exit_code':completed.returncode,
            'duration_seconds':round(time.monotonic()-clock,3),'log_sha256':sha(OUT/(label+'.log'))})
        if label=='sdk':
            report=OWNER/'target/surefire-reports/TEST-com.synexia.rewrite.ScrollbarFullRangeRecipeTest.xml'
            if report.exists():
                assert report.stat().st_mtime>=before_time,'stale report refused'
                suite=ET.parse(report).getroot()
                counts={key:int(suite.get(key,'0')) for key in ('tests','failures','errors','skipped')}
        if completed.returncode:raise RuntimeError(label+' failed; source and logs retained')
        if label=='sdk':assert counts=={'tests':7,'failures':0,'errors':0,'skipped':0},counts
    receipt=json.loads((OWNER/'target/evidence/receipt.json').read_text())
    assert receipt['SDK_OUTPUT_CONSUMED'] and receipt['EXACT_RECEIVER']
    assert receipt['checksPerMode']==5723783 and receipt['packagePublicDescriptors']=='IDENTICAL'
    assert (OWNER/'target/generated-receiver'/GEOMETRY).read_bytes()==(RECEIVER/GEOMETRY).read_bytes()
    status='PASS'
except BaseException as failure:
    error=type(failure).__name__+': '+str(failure)
finally:
    for label,relative in [('surefire-reports','target/surefire-reports'),('owner-evidence','target/evidence')]:
        source=OWNER/relative
        if source.is_dir():
            destination=OUT/label;destination.mkdir()
            for path in sorted(source.iterdir()):
                if path.is_file() and not path.name.startswith('.'):
                    shutil.copyfile(path,destination/path.name)
    unchanged=all(sha(RECEIVER/row['path'])==row['sha256'] and sha(BUILD/row['path'])==row['sha256'] for row in inputs)
    if not unchanged:status='FAIL_SOURCE_DRIFT'
    result={'schema':'SWT_CURRENT_OWNER_EMITTED_POM_RECEIVER/1','status':status,'error':error,
        'receiver_repository':'hsoliwal/eclipse.platform.swt','receiver_base_revision':pin,
        'receiver_root':str(RECEIVER),'canonical_sdk_receipt_sha256':sha(CRATE/'proof/sdk-001-offline/RESULT.json'),
        'canonical_recipe_proof_sha256':sha(CRATE/'target/recipe-proof.tsv'),
        'source_seals_sha256':sha(CRATE/'SOURCE_SEALS.json'),'emitted_pom_sha256':sha(emitted),
        'actual_receiver_pom_sha256':sha(RECEIVER/POM),'sdk_emitted_pom_consumed':True,
        'actual_geometry_sha256':sha(RECEIVER/GEOMETRY),'production_geometry_changes':0,
        'original_test_sources_changed':0,'sdk_tests':counts,'steps':steps,
        'inputs_sha256':sha(OUT/'INPUTS.json'),'commands_sha256':sha(OUT/'COMMANDS.json'),
        'input_files':len(inputs),'all_input_readbacks_unchanged':unchanged,
        'duration_seconds':round(time.monotonic()-started,3),
        'scope':'Actual emitted proof-POM and exact current SWT owner; Java21 arithmetic/ABI only; native/platform/Tycho/fullM3/performance NOT_RUN'}
    (OUT/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result))
raise SystemExit(0 if status=='PASS' else 1)
