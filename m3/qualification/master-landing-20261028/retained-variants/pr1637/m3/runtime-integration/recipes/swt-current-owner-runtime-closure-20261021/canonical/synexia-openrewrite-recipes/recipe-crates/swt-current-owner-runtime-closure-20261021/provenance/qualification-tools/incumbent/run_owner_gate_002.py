#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Run the already-landed SWT proof owners in an unchanged isolated checkout."""
from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
SOURCE = HERE / 'source'
OUT = HERE / 'evidence-002'
OUT.mkdir(exist_ok=False)
PREFIX = 'docs/m3/scrollbar-full-range-20261020'
CRATE = SOURCE / PREFIX / 'recipes/swt-scrollbar-full-range-20261020'
PRODUCTION = 'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/ViewportScrollBarGeometry.java'
PIN = 'c0ffa6d76338ed1b06cfe0158b0e04d8930df546'
JDK = Path('/tmp/m3-build-tools/jdk-21.0.12.1+1')
MAVEN = '/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn'
records = []


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git(*args):
    return subprocess.check_output(['git', '-C', str(SOURCE), *args], text=True).strip()


assert git('rev-parse', 'HEAD') == PIN
assert git('status', '--porcelain=v1') == ''
assert git('hash-object', 'AGENTS.md') == 'e0ebc6f23d970a7c381eb514200924f99f276513'
assert sha(SOURCE / PRODUCTION) == 'df4f8498b1f633f2ffeae310f761ecaa0ea686e392557b8cfe2c684864a7f9b4'
tree = git('ls-files', '-z').split('\0')
additional = {'AGENTS.md', PRODUCTION,
    '.github/workflows/swt-scrollbar-full-range-20261020.yml',
    '.github/workflows/build.yml', '.github/workflows/maven.yml',
    'tests/org.eclipse.swt.tests/pom.xml',
    'tests/org.eclipse.swt.tests/build.properties',
    'tests/org.eclipse.swt.tests/JUnit Tests/org/eclipse/swt/tests/junit/AllTests.java',
    'tests/org.eclipse.swt.tests/JUnit Tests/org/eclipse/swt/tests/junit/AllNonBrowserTests.java',
    'tests/org.eclipse.swt.tests/JUnit Tests/org/eclipse/swt/tests/junit/AllWidgetTests.java',
    'tests/org.eclipse.swt.tests/JUnit Tests/org/eclipse/swt/tests/junit/Test_ViewportScrollBarGeometry.java',
    'tests/org.eclipse.swt.tests/JUnit Tests/org/eclipse/swt/tests/junit/Test_org_eclipse_swt_widgets_ScrollBar_GeometryDifferential.java',
    'tests/org.eclipse.swt.tests/JUnit Tests/org/eclipse/swt/tests/junit/ViewportDifferential.java'}
paths = sorted(path for path in tree if path.startswith(PREFIX + '/') or path in additional)
assert len(paths) == len(set(paths)) and additional.issubset(paths)
inputs = [{'path':path, 'sha256':sha(SOURCE / path), 'bytes':(SOURCE / path).stat().st_size,
           'git_blob':git('hash-object', path)} for path in paths]
(OUT / 'INPUTS.json').write_text(json.dumps(inputs, indent=2) + '\n')
env = dict(os.environ, JAVA_HOME=str(JDK),
           PATH=str(JDK / 'bin') + ':' + os.environ.get('PATH', ''),
           MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData')
commands = [
    ('sdk', [MAVEN, '-B', '-ntp', '-o', '-Dmaven.repo.local=/tmp/m3-build-m2',
             '-f', str(CRATE / 'pom.xml'), 'clean', 'test']),
    ('receiver', ['python3', str(CRATE / 'verify.py'), '--receiver', str(SOURCE)])]
(OUT / 'COMMANDS.json').write_text(json.dumps({'commands':commands,
    'cwd':str(SOURCE), 'environment':{key:env[key] for key in ('JAVA_HOME','MAVEN_OPTS')},
    'java_release_sha256':sha(JDK / 'release')}, indent=2) + '\n')
status = 'FAIL'
failure = None
counts = None
start = time.monotonic()
try:
    for label, command in commands:
        before = time.monotonic()
        with (OUT / (label + '.log')).open('w') as log:
            result = subprocess.run(command, cwd=SOURCE, env=env, stdout=log,
                                    stderr=subprocess.STDOUT, timeout=300)
        records.append({'name':label, 'argv':command, 'exit_code':result.returncode,
                        'seconds':round(time.monotonic()-before,3),
                        'log_sha256':sha(OUT / (label + '.log'))})
        if result.returncode:
            raise RuntimeError(label + ' failed; unchanged owner and log retained')
        if label == 'sdk':
            report = CRATE / 'target/surefire-reports/TEST-com.synexia.rewrite.ScrollbarFullRangeRecipeTest.xml'
            suite = ET.parse(report).getroot()
            counts = {key:int(suite.get(key,'0')) for key in ('tests','failures','errors','skipped')}
            assert counts == {'tests':7,'failures':0,'errors':0,'skipped':0}, counts
    receipt = json.loads((CRATE / 'target/evidence/receipt.json').read_text())
    assert receipt['EXACT_RECEIVER'] is True and receipt['SDK_OUTPUT_CONSUMED'] is True
    assert receipt['checksPerMode'] == 5723783 and receipt['packagePublicDescriptors'] == 'IDENTICAL'
    status = 'PASS_EXISTING_CURRENT_SWT_OWNER'
except BaseException as error:
    failure = type(error).__name__ + ': ' + str(error)
finally:
    for label, directory in (('surefire-reports','target/surefire-reports'),
                             ('owner-evidence','target/evidence')):
        source = CRATE / directory
        if source.exists():
            shutil.copytree(source, OUT / label)
    after = [{'path':row['path'], 'sha256':sha(SOURCE / row['path']),
              'bytes':(SOURCE / row['path']).stat().st_size,
              'git_blob':git('hash-object', row['path'])} for row in inputs]
    (OUT / 'INPUTS_AFTER.json').write_text(json.dumps(after, indent=2) + '\n')
    unchanged = inputs == after and git('status', '--porcelain=v1') == ''
    if not unchanged:
        status = 'FAIL_SOURCE_CHANGED'
    result = {'schema':'SWT_EXISTING_CURRENT_OWNER_QUALIFICATION/1', 'status':status,
        'repository':'hsoliwal/eclipse.platform.swt', 'revision':PIN,
        'tree':git('rev-parse','HEAD^{tree}'), 'production_sha256':sha(SOURCE / PRODUCTION),
        'production_git_blob':git('hash-object', PRODUCTION),
        'canonical_receiving_source':'hsoliwal/com.synexia@9372a2d57f1a05394172a7bd85dda3f0d6d83983',
        'canonical_pr':'https://github.com/hsoliwal/com.synexia/pull/12544',
        'received_through':['https://github.com/hsoliwal/eclipse.platform.swt/pull/236',
                            'https://github.com/hsoliwal/eclipse.platform.swt/pull/237'],
        'sdk_tests':counts, 'steps':records, 'failure':failure,
        'input_files':len(inputs), 'inputs_sha256':sha(OUT/'INPUTS.json'),
        'commands_sha256':sha(OUT/'COMMANDS.json'), 'source_inputs_unchanged':unchanged,
        'production_writes':0, 'duration_seconds':round(time.monotonic()-start,3),
        'scope':'Actual existing7-test OpenRewrite owner and generated/current receiver arithmetic; no SWT/Tycho/native/display/image/performance promotion'}
    (OUT / 'RESULT.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'status':status,'counts':counts,'source_inputs_unchanged':unchanged,
                      'failure':failure,'receipt':str(OUT/'RESULT.json')}))
if status != 'PASS_EXISTING_CURRENT_SWT_OWNER':
    raise SystemExit(1)
