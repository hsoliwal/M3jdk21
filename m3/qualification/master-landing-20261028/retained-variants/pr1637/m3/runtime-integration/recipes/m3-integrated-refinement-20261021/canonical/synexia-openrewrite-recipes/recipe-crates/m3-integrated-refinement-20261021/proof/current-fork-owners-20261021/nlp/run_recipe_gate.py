#!/usr/bin/env python3
"""Execute the existing pinned staging SDK gate without source edits."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import time
import xml.etree.ElementTree as ET

work=Path(__file__).resolve().parent
root=work/'receiver-source'
recipe=root/'m3/vendor/synexia/synexia-openrewrite-recipes'
crate=recipe/'crates/m3-nlp-search/current-forks-20261018'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source_rows=json.loads((work/'evidence/RECEIVER_SOURCE_FILES.json').read_text())
paths=[root/r['path'] for r in source_rows if r['path'].startswith('m3/vendor/synexia/synexia-openrewrite-recipes/')]
before={str(p.relative_to(root)):sha(p) for p in paths}
command=['/tmp/m3-build-tools/apache-maven-3.9.9/bin/mvn','-B','-ntp','-o',
    '-s',str(work.parent/'canonical-work/maven-proxy-settings.xml'),
    '-Dmaven.repo.local=/tmp/m3-build-m2','-f',str(crate/'pom.xml'),
    'test','dependency:build-classpath','-Dmdep.outputFile=target/sdk-classpath.txt']
log=work/'evidence/CURRENT_RECIPE_001.log'
env=dict(os.environ,JAVA_HOME='/tmp/m3-build-tools/jdk-21.0.12.1+1',
    MAVEN_OPTS='-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts')
start=time.monotonic()
with log.open('x') as stream:
    stream.write(json.dumps(command)+'\n');stream.flush()
    run=subprocess.run(command,cwd=root,stdout=stream,stderr=subprocess.STDOUT,env=env,timeout=300)
after={str(p.relative_to(root)):sha(p) for p in paths}
totals={k:0 for k in ['tests','failures','errors','skipped']}
reports=[]
for path in sorted((crate/'target/surefire-reports').glob('TEST-*.xml')):
    tree=ET.parse(path).getroot();row={k:int(tree.attrib.get(k,'0')) for k in totals}
    for key in totals:totals[key]+=row[key]
    reports.append({'path':str(path),'sha256':sha(path),'counts':row})
passed=run.returncode==0 and before==after and totals['tests']>0 and not any(totals[k] for k in ['failures','errors','skipped'])
receipt={'status':'PASS' if passed else 'FAIL','scope':'Existing current NLP staging recipe SDK; not combined receiver qualification',
    'command':command,'exit_code':run.returncode,'seconds':round(time.monotonic()-start,3),
    'tests':totals,'source_inputs_unchanged':before==after,'source_inputs':before,'reports':reports,
    'log_sha256':sha(log),'sdk_classpath_created':(crate/'target/sdk-classpath.txt').is_file()}
path=work/'evidence/CURRENT_RECIPE_001.json';path.write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps({k:receipt[k] for k in ['status','exit_code','seconds','tests','source_inputs_unchanged','sdk_classpath_created']},indent=2),flush=True)
raise SystemExit(0 if passed else 1)
