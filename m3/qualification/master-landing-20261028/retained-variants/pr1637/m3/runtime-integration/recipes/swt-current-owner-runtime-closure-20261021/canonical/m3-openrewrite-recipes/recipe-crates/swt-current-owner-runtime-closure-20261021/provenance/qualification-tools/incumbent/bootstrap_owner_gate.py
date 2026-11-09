#!/usr/bin/env python3
"""Provision the unchanged current SWT owning POM; preserve the exact attempt."""
from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess
import time

here = Path(__file__).resolve().parent
source = here / 'source'
out = here / 'bootstrap-001'
out.mkdir(exist_ok=False)
crate = source / 'docs/m3/scrollbar-full-range-20261020/recipes/swt-scrollbar-full-range-20261020'
jdk = Path('/tmp/m3-build-tools/jdk-21.0.12.1+1')
inputs = json.loads((here / 'evidence-001/INPUTS.json').read_text())
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
assert all(sha(source / item['path']) == item['sha256'] for item in inputs)
command = ['/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn', '-B', '-ntp',
    '-s', '/workspace/scratch/206049dfb270/canonical-work/maven-proxy-settings.xml',
    '-Dmaven.repo.local=/tmp/m3-build-m2', '-f', str(crate / 'pom.xml'), 'clean', 'test']
env = dict(os.environ, JAVA_HOME=str(jdk), PATH=str(jdk/'bin') + ':' + os.environ.get('PATH',''),
    MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts')
(out/'COMMAND.json').write_text(json.dumps({'argv':command,'cwd':str(source),
    'environment':{k:env[k] for k in ('JAVA_HOME','MAVEN_OPTS')}},indent=2)+'\n')
start = time.monotonic()
with (out/'maven.log').open('w') as log:
    run = subprocess.run(command,cwd=source,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=300)
unchanged = all(sha(source / item['path']) == item['sha256'] for item in inputs)
status = subprocess.check_output(['git','-C',str(source),'status','--porcelain=v1'],text=True)
reports = crate/'target/surefire-reports'
if reports.exists():
    shutil.copytree(reports,out/'surefire-reports')
result = {'schema':'SWT_CURRENT_OWNER_DEPENDENCY_BOOTSTRAP/1',
    'status':'PASS' if run.returncode == 0 and unchanged and not status else 'FAIL',
    'exit_code':run.returncode,'duration_seconds':round(time.monotonic()-start,3),
    'command_sha256':sha(out/'COMMAND.json'),'log_sha256':sha(out/'maven.log'),
    'source_inputs_unchanged':unchanged,'tracked_and_untracked_status':status,
    'production_writes':0,'scope':'Provision unchanged owning POM using proxy and verified TLS; receiver runtime not run in this bootstrap.'}
(out/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result))
raise SystemExit(0 if result['status']=='PASS' else 1)
