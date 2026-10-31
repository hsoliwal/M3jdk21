#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Qualify one candidate POM with exact incumbent SWT sources and tests."""
from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
BASE = ROOT.parent / 'swt-receiver-current-001'
OUT = ROOT / 'candidate-001'
OUT.mkdir(parents=True, exist_ok=False)
SOURCE = Path('/tmp/m3-swt-current-owner-candidate-001')
SOURCE.mkdir(exist_ok=False)
POM = 'docs/m3/scrollbar-full-range-20261020/recipes/swt-scrollbar-full-range-20261020/pom.xml'
CRATE = SOURCE / Path(POM).parent
JDK = Path('/tmp/m3-build-tools/jdk-21.0.12.1+1')
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
inputs = json.loads((BASE/'evidence-001/INPUTS.json').read_text())
for row in inputs:
    original = BASE / 'source' / row['path']
    assert sha(original) == row['sha256']
    target = SOURCE / row['path']
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(original.read_bytes())
before = (SOURCE/POM).read_bytes()
anchor = b'  <dependencies>\n'
assert before.count(anchor) == 1
addition = b'    <dependency><groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId><version>2.0.13</version></dependency>\n'
candidate = before.replace(anchor,anchor+addition)
template = ROOT / 'candidates' / POM
template.parent.mkdir(parents=True,exist_ok=True)
with template.open('xb') as stream: stream.write(candidate)
(SOURCE/POM).write_bytes(candidate)
expected = {row['path']:sha(SOURCE/row['path']) for row in inputs}
(OUT/'INPUTS.json').write_text(json.dumps({'baseline':inputs,'candidate':expected,
    'candidate_pom':{'path':POM,'before_sha256':hashlib.sha256(before).hexdigest(),
        'after_sha256':sha(template),'template':str(template)},
    'compiled_directory_is_fresh':True,'unchanged_original_test_count':7},indent=2)+'\n')
env = dict(os.environ,JAVA_HOME=str(JDK),PATH=str(JDK/'bin')+':'+os.environ.get('PATH',''),
    MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData')
commands = [('sdk',['/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn','-B','-ntp','-o',
    '-Dmaven.repo.local=/tmp/m3-build-m2','-f',str(SOURCE/POM),'clean','test']),
    ('receiver',['python3',str(CRATE/'verify.py'),'--receiver',str(SOURCE)])]
(OUT/'COMMANDS.json').write_text(json.dumps({'cwd':str(SOURCE),'commands':commands,
    'environment':{key:env[key] for key in ('JAVA_HOME','MAVEN_OPTS')},
    'java_release_sha256':sha(JDK/'release')},indent=2)+'\n')
status = 'FAIL'; error = None; counts = None; steps = []
start = time.monotonic()
try:
    for label,command in commands:
        before_time = time.time(); clock = time.monotonic()
        with (OUT/(label+'.log')).open('w') as log:
            completed = subprocess.run(command,cwd=SOURCE,env=env,stdout=log,
                stderr=subprocess.STDOUT,timeout=300)
        steps.append({'name':label,'argv':command,'exit_code':completed.returncode,
            'duration_seconds':round(time.monotonic()-clock,3),'log_sha256':sha(OUT/(label+'.log'))})
        if label == 'sdk':
            report = CRATE/'target/surefire-reports/TEST-com.synexia.rewrite.ScrollbarFullRangeRecipeTest.xml'
            if report.exists():
                assert report.stat().st_mtime >= before_time
                suite = ET.parse(report).getroot()
                counts = {key:int(suite.get(key,'0')) for key in ('tests','failures','errors','skipped')}
        if completed.returncode: raise RuntimeError(label+' failed; exact evidence retained')
        if label == 'sdk': assert counts == {'tests':7,'failures':0,'errors':0,'skipped':0},counts
    receipt = json.loads((CRATE/'target/evidence/receipt.json').read_text())
    assert receipt['EXACT_RECEIVER'] and receipt['SDK_OUTPUT_CONSUMED']
    assert receipt['checksPerMode'] == 5723783 and receipt['packagePublicDescriptors'] == 'IDENTICAL'
    status = 'PASS'
except BaseException as failure:
    error = type(failure).__name__+': '+str(failure)
finally:
    for label,relative in [('surefire-reports','target/surefire-reports'),('owner-evidence','target/evidence')]:
        directory = CRATE/relative
        if directory.is_dir():
            destination = OUT/label; destination.mkdir()
            for path in sorted(directory.iterdir()):
                if path.is_file() and not path.name.startswith('.'):
                    shutil.copyfile(path,destination/path.name)
    after = {row['path']:sha(SOURCE/row['path']) for row in inputs}
    unchanged = after == expected
    assert all(sha(BASE/'source'/row['path']) == row['sha256'] for row in inputs)
    if not unchanged: status = 'FAIL_SOURCE_DRIFT'
    result = {'schema':'SWT_CURRENT_OWNER_POM_CANDIDATE/1','status':status,'error':error,
        'receiver_revision':'c0ffa6d76338ed1b06cfe0158b0e04d8930df546',
        'candidate_pom_sha256':sha(template),'sdk_tests':counts,'steps':steps,
        'inputs_sha256':sha(OUT/'INPUTS.json'),'commands_sha256':sha(OUT/'COMMANDS.json'),
        'candidate_inputs_unchanged':unchanged,'baseline_inputs_unchanged':True,
        'production_source_writes':0,'original_test_sources_changed':0,
        'duration_seconds':round(time.monotonic()-start,3),
        'scope':'POM candidate only; existing seven-test SDK plus exact generated/current arithmetic receiver; native/Tycho/display/image/performance NOT_RUN'}
    (OUT/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result))
raise SystemExit(0 if status == 'PASS' else 1)
