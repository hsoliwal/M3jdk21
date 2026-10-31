#!/usr/bin/env python3
"""Provision exact dependencies requested by the unchanged donor source gate."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import time

work = Path(__file__).resolve().parent
name = sys.argv[1]
directory, plugin = {'opennlp': ('dev/m3-jmod','3.11.0'),
                     'lucene': ('.m3/current-jmods','3.8.1')}[name]
owner = work / 'forks' / name / directory
out = work / 'dependencies' / name
out.mkdir(parents=True,exist_ok=False)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
lock = json.loads((owner / 'dependency-lock.json').read_text())
command = ['/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn','-B','-ntp',
    '-s',str(work.parent/'canonical-work/maven-proxy-settings.xml'),
    '-Dmaven.repo.local=/tmp/m3-build-m2','-f',str(owner/'dependencies.xml'),
    'org.apache.maven.plugins:maven-dependency-plugin:'+plugin+':copy-dependencies',
    '-DincludeScope=runtime','-DoutputDirectory='+str(out)]
env = dict(os.environ,JAVA_HOME='/tmp/m3-build-tools/jdk-21.0.12.1+1',
    MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts')
log = work/'evidence'/(name+'-dependencies-001.log')
start = time.monotonic()
with log.open('x') as stream:
    stream.write(json.dumps(command)+'\n');stream.flush()
    result = subprocess.run(command,cwd=owner,stdout=stream,stderr=subprocess.STDOUT,env=env,timeout=300)
rows=[]
for filename,row in lock['artifacts'].items():
    path=out/filename
    actual=sha(path) if path.is_file() else 'ABSENT'
    rows.append({'filename':filename,'coordinate':row['coordinate'],'expected_sha256':row['sha256'],
        'actual_sha256':actual,'bytes':path.stat().st_size if path.is_file() else 0,
        'match':actual==row['sha256']})
receipt={'status':'PASS' if result.returncode==0 and all(r['match'] for r in rows) else 'FAIL',
    'scope':'locked external artifact provisioning, not source or runtime qualification',
    'command':command,'exit_code':result.returncode,'seconds':round(time.monotonic()-start,3),
    'dependency_pom_sha256':sha(owner/'dependencies.xml'),
    'dependency_lock_sha256':sha(owner/'dependency-lock.json'),'log_sha256':sha(log),'artifacts':rows}
path=work/'evidence'/(name.upper()+'_DEPENDENCIES_001.json')
path.write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps({'status':receipt['status'],'artifacts':len(rows),'seconds':receipt['seconds'],
    'receipt':str(path),'sha256':sha(path)},indent=2),flush=True)
raise SystemExit(0 if receipt['status']=='PASS' else 1)
