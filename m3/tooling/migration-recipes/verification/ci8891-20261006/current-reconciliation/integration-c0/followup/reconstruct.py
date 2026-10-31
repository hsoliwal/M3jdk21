#!/usr/bin/env python3
"""Reconstruct the complete immutable Maven project through the unchanged archive owner."""
from pathlib import Path
import argparse,hashlib,json,subprocess,sys
p=argparse.ArgumentParser();p.add_argument('--portable',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();root=a.portable.resolve();out=a.output.resolve();assert not out.exists(),'fresh output required'
def check(path,pin):
 b=path.read_bytes();assert len(b)==pin['bytes'] and hashlib.sha256(b).hexdigest()==pin['sha256'],path
manifest=json.loads((root/'PORTABLE-MANIFEST.json').read_bytes());ready_row=next(r for r in manifest['files'] if r['portablePath']=='PROJECT-READY.json');check(root/'PROJECT-READY.json',ready_row)
ready=json.loads((root/'PROJECT-READY.json').read_bytes());assert ready['schema']=='synexia.fc0.project-ready/1'
check(root/'package_evidence.py',ready['archive']['packageOwner']);check(root/'bundle/BUNDLE.json',ready['archive']['bundle'])
for argv in ([sys.executable,str(root/'package_evidence.py'),'verify','--bundle',str(root/'bundle')],[sys.executable,str(root/'package_evidence.py'),'extract','--bundle',str(root/'bundle'),'--output',str(out)]):subprocess.run(argv,check=True)
project=out/ready['qualifiedProject']['logicalPrefix'];rows=ready['qualifiedProject']['files'];assert len(rows)==43
actual=sorted(p.relative_to(project).as_posix() for p in project.rglob('*') if p.is_file());assert actual==[r['path'] for r in rows]
for row in rows:check(project/row['path'],row)
print(json.dumps({'status':'COMPLETE_PROJECT_RECONSTRUCTED','project':str(project),'projectFiles':len(rows),'proofGatesExecuted':False,'originalReceiptsRequalified':False,'mavenCommandExample':['/path/to/apache-maven-3.9.9/bin/mvn','-o','-B','-ntp','-Dmaven.repo.local=/path/to/admitted-m2','-f',str(project/'pom.xml'),'test']}))
