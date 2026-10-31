"""Read back already extracted archive bytes and emit review/readiness/paired mapping metadata."""
from pathlib import Path
import hashlib,json,stat
H=Path(__file__).resolve().parent;I=H.parent;S=H/'source';E=H/'extracted';B=H/'bundle'
SP='synexia-openrewrite-recipes/';MP='m3/tooling/migration-recipes/';TASK='tasks/m3-rc-10f/integration-c0/followup/';PORT='verification/ci8891-20261006/current-reconciliation/integration-c0/followup/'
def sha(b):return hashlib.sha256(b).hexdigest()
def blob(b):return hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()
def load(p):return json.loads(p.read_bytes())
def pin(p):b=p.read_bytes();return {'path':str(p),'sha256':sha(b),'bytes':len(b)}
def write(p,d):
 assert not p.exists(),p;p.write_text(json.dumps(d,sort_keys=True,indent=2)+'\n')
def mapped(p,suffix,role,**more):
 b=p.read_bytes();return {'file':str(p),'mode':'100644','bytes':len(b),'sha256':sha(b),'gitBlob':blob(b),'synexiaPath':SP+suffix,'m3Path':MP+suffix,'role':role,**more}
cl=load(H/'SOURCE-CLOSURE.json');plan=load(H/'package-plan.json');selected=plan['includePaths'];actual=sorted(p.relative_to(E).as_posix() for p in E.rglob('*') if p.is_file());assert actual==selected
readback=[]
for name in selected:
 a=(S/name).read_bytes();b=(E/name).read_bytes();assert a==b,name
 assert stat.S_IMODE((S/name).stat().st_mode)==stat.S_IMODE((E/name).stat().st_mode)
 readback.append({'logicalPath':name,'sha256':sha(b),'bytes':len(b),'mode':format(stat.S_IMODE((E/name).stat().st_mode),'04o'),'gitBlob':blob(b)})
project=E/'fc0/project';project_rows=[];flat=[];withheld=[]
for p in sorted(project.rglob('*')):
 if not p.is_file():continue
 b=p.read_bytes();name=p.relative_to(project).as_posix();row={'path':name,'mode':'100644','bytes':len(b),'sha256':sha(b),'gitBlob':blob(b)};project_rows.append(row)
 hazard=any(line.rstrip(b'\r\n').endswith((b' ',b'\t')) for line in b.splitlines(keepends=True)) or b.endswith(b'\n\n')
 (withheld if hazard else flat).append(row)
assert len(project_rows)==43 and len(withheld)==2 and len(flat)==41
assert {r['path']:r['sha256'] for r in withheld}=={
 'src/test/resources/before/m3/tooling/lane28/verify-plan.py':'5ca191f85c48be0ffec139b94d0ed91e0ac58be73686773241255f651799ec4b',
 'src/test/resources/before/m3/tooling/tq/verify-plan.py':'ff2c31f57aac81a7049612557a3757781927563bd2d7a7b598aa62a116e1d978'}
owner=(E/'package_evidence.py').read_bytes();assert sha(owner)=='89d43693f508c723e431296cc06f611c8de4f94df8eaddea95a2c7742feb0574';(H/'package_evidence.py').write_bytes(owner)
write(H/'READBACK.json',{'schema':'synexia.fc0.package-readback/1','status':'PASS_EXACT_PACKAGE_RECONSTRUCTION','sourceClosure':pin(H/'SOURCE-CLOSURE.json'),'bundle':pin(B/'BUNDLE.json'),'files':readback,'projectFiles':43,'artifactExcludedCount':len(cl['excludedIdentities']),'consumerQualified':False,'hermetic':False,'proofGatesExecuted':False,'parentPublication':cl['parentPublication'],'receivingOutcome':cl['receivingOutcome']})
write(H/'PROJECT-READY.json',{'schema':'synexia.fc0.project-ready/1','producer':cl['producer'],'qualifiedProject':{'sourceRoot':str(project),'logicalPrefix':'fc0/project','files':project_rows},'flatFiles':flat,'archiveOnlyFiles':withheld,'flatMirrorMavenReady':False,'archive':{'bundle':pin(B/'BUNDLE.json'),'packageOwner':pin(H/'package_evidence.py')},'reconstruction':{'entrypoint':'reconstruct.py','argv':['python3','<portable>/reconstruct.py','--portable','<portable>','--output','<fresh-extraction>'],'verifyArgv':['python3','<portable>/package_evidence.py','verify','--bundle','<portable>/bundle'],'extractArgv':['python3','<portable>/package_evidence.py','extract','--bundle','<portable>/bundle','--output','<fresh-extraction>'],'mavenExampleArgv':['<admitted-maven>/bin/mvn','-o','-B','-ntp','-Dmaven.repo.local=<admitted-cache>','-f','<fresh-extraction>/fc0/project/pom.xml','test']},'originalReceiptPathsPreserved':True,'automaticRequalification':False})
(H/'README.md').write_text('''# Fresh Fc0 source and proof evidence

This package reconstructs the complete 43-file Maven task at `fc0/project` and retains the actual fresh producer trials, raw logs, reports, source resources and exact output bodies. READBACK.json compares every extracted body with its selected original. SOURCE-CLOSURE.json maps original absolute input paths to included bodies or explicit exclusions. The earlier 338 paired publication bodies remain unchanged at their recorded parent commits. SOURCE-CLOSURE.json records the actual receiving disposition when explicitly selected, or PENDING_NOT_INCLUDED; packaging does not infer receiving qualification. Old unavailable producer evidence has not been recreated or used as new admission.

The flat 41-file review mirror is **NOT MAVEN-READY**; two significant-trailing-whitespace before-image fixtures are archive-only. Run the checked reconstruction entry before Maven:

```sh
python3 /path/to/portable/reconstruct.py --portable /path/to/portable --output /path/to/fresh-extraction
/path/to/admitted-maven/bin/mvn -o -B -ntp -Dmaven.repo.local=/path/to/admitted-cache -f /path/to/fresh-extraction/fc0/project/pom.xml test
```

The entry reuses the unchanged archive owner, verifies every logical project body, and executes no proof gate. A subsequent Maven run is a new execution; the original absolute receipt paths remain unchanged. Use the exact admitted toolchain and dependency identities for comparison. Compiled classes, JDK/Maven/Python/cache bodies and settings are excluded under the unchanged owner and remain identity-only. This is source/evidence reconstruction, not a hermetic tool image or full receiving/JDK qualification.
''')
(H/'RECONSTRUCT.md').write_text('''This flat 41-file review mirror is **NOT MAVEN-READY**: it omits two exact significant-trailing-whitespace fixtures. The complete qualified 43-file project is in the companion archive. From this task directory, the portable folder is `../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup`.

```sh
python3 ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup/reconstruct.py --portable ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup --output /path/to/fresh-extraction
/path/to/admitted-maven/bin/mvn -o -B -ntp -Dmaven.repo.local=/path/to/admitted-cache -f /path/to/fresh-extraction/fc0/project/pom.xml test
```

PROJECT-READY.json pins all 43 logical files and the two archive-only fixtures. Their exact source hashes are `5ca191f85c48be0ffec139b94d0ed91e0ac58be73686773241255f651799ec4b` (lane28 verifier preimage) and `ff2c31f57aac81a7049612557a3757781927563bd2d7a7b598aa62a116e1d978` (TQ verifier preimage). This document is an unqualified publication envelope outside the project census. Reconstruction preserves source bytes and receipt paths; it does not turn later Maven execution into an original receipt.
''')
tr=[mapped(project/r['path'],TASK+r['path'],'qualified-project-flat',projectPath=r['path']) for r in flat]
write(H/'TASK-MAPPING.json',{'schema':'synexia.fc0.task-mapping/1','files':tr,'logicalProjectFiles':43,'flatProjectFiles':41,'archiveOnlyFiles':withheld,'producer':cl['producer'],'readback':pin(H/'READBACK.json')})
paths=sorted([p for p in B.iterdir() if p.is_file()]+[H/n for n in ('package_evidence.py','PROJECT-READY.json','READBACK.json','SOURCE-CLOSURE.json','package-plan.json','README.md','reconstruct.py','prepare_package.py','finish_package.py')],key=str)
pr=[mapped(p,PORT+p.relative_to(H).as_posix(),'portable-evidence',portablePath=p.relative_to(H).as_posix()) for p in paths]
write(H/'PORTABLE-MANIFEST.json',{'schema':'synexia.fc0.portable-manifest/1','files':pr,'producer':cl['producer'],'readback':pin(H/'READBACK.json'),'bundle':pin(B/'BUNDLE.json'),'artifactExcludedCount':len(cl['excludedIdentities'])})
# External paired envelope includes manifest files without a self-referential manifest digest.
extra=[mapped(H/'TASK-MAPPING.json',PORT+'TASK-MAPPING.json','mapping-envelope'),mapped(H/'PORTABLE-MANIFEST.json',PORT+'PORTABLE-MANIFEST.json','mapping-envelope'),mapped(H/'RECONSTRUCT.md',TASK+'RECONSTRUCT.md','unqualified-reconstruction-envelope')]
parent_paths=load(Path(cl['parentPublication']['pairedMap']['path']))['files']
assert len(parent_paths)==338
for key in ('synexiaPath','m3Path'):
 assert not ({row[key] for row in parent_paths}&{row[key] for row in tr+pr+extra}),'new mapping overlaps immutable parent publication'
write(H/'PAIRED-FILES.json',{'schema':'synexia.fc0.paired-files/1','files':sorted(tr+pr+extra,key=lambda r:r['synexiaPath']),'taskMapping':pin(H/'TASK-MAPPING.json'),'portableManifest':pin(H/'PORTABLE-MANIFEST.json'),'producer':cl['producer'],'readback':pin(H/'READBACK.json'),'consumerQualified':False,'parentPublication':cl['parentPublication'],'receivingOutcome':cl['receivingOutcome']})
print(json.dumps({'readback':pin(H/'READBACK.json'),'taskMapping':pin(H/'TASK-MAPPING.json'),'portableManifest':pin(H/'PORTABLE-MANIFEST.json'),'pairedFiles':pin(H/'PAIRED-FILES.json'),'logicalProject':43,'flatProject':41,'portableFiles':len(pr),'pairedCount':len(tr)+len(pr)+len(extra)}))
