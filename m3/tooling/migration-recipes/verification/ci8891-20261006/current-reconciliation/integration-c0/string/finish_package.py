"""Read back already extracted archive bytes and emit review/readiness/paired mapping metadata."""
from pathlib import Path
import hashlib,json,stat
H=Path(__file__).resolve().parent;I=H.parent;S=H/'source';E=H/'extracted';B=H/'bundle'
SP='synexia-openrewrite-recipes/';MP='m3/tooling/migration-recipes/';TASK='tasks/m3-rc-10f/integration-c0/string/';PORT='verification/ci8891-20261006/current-reconciliation/integration-c0/string/'
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
project=E/'sc0/project';project_rows=[];flat=[];withheld=[]
for p in sorted(project.rglob('*')):
 if not p.is_file():continue
 b=p.read_bytes();name=p.relative_to(project).as_posix();row={'path':name,'mode':'100644','bytes':len(b),'sha256':sha(b),'gitBlob':blob(b)};project_rows.append(row)
 hazard=any(line.rstrip(b'\r\n').endswith((b' ',b'\t')) for line in b.splitlines(keepends=True)) or b.endswith(b'\n\n')
 (withheld if hazard else flat).append(row)
assert len(project_rows)==20 and len(withheld)==0 and len(flat)==20
owner=(E/'package_evidence.py').read_bytes();assert sha(owner)=='89d43693f508c723e431296cc06f611c8de4f94df8eaddea95a2c7742feb0574';(H/'package_evidence.py').write_bytes(owner)
write(H/'READBACK.json',{'schema':'synexia.sc0.package-readback/1','status':'PASS_EXACT_PACKAGE_RECONSTRUCTION','sourceClosure':pin(H/'SOURCE-CLOSURE.json'),'bundle':pin(B/'BUNDLE.json'),'files':readback,'projectFiles':20,'artifactExcludedCount':len(cl['excludedIdentities']),'consumerQualified':False,'hermetic':False,'proofGatesExecuted':False,'parentPublication':cl['parentPublication'],'parentProducer':cl['parentProducer'],'semanticOutcome':cl['semanticOutcome'],'receivingOutcome':cl['receivingOutcome']})
write(H/'PROJECT-READY.json',{'schema':'synexia.sc0.project-ready/1','producer':cl['producer'],'qualifiedProject':{'sourceRoot':str(project),'logicalPrefix':'sc0/project','files':project_rows},'flatFiles':flat,'archiveOnlyFiles':withheld,'flatMirrorMavenReady':True,'archive':{'bundle':pin(B/'BUNDLE.json'),'packageOwner':pin(H/'package_evidence.py')},'reconstruction':{'entrypoint':'reconstruct.py','argv':['python3','<portable>/reconstruct.py','--portable','<portable>','--output','<fresh-extraction>'],'verifyArgv':['python3','<portable>/package_evidence.py','verify','--bundle','<portable>/bundle'],'extractArgv':['python3','<portable>/package_evidence.py','extract','--bundle','<portable>/bundle','--output','<fresh-extraction>'],'mavenExampleArgv':['<admitted-maven>/bin/mvn','-o','-B','-ntp','-Dmaven.repo.local=<admitted-cache>','-f','<fresh-extraction>/sc0/project/pom.xml','test']},'originalReceiptPathsPreserved':True,'automaticRequalification':False})
(H/'README.md').write_text("""# Sc0 source and proof evidence

This package reconstructs the complete 20-file Maven task at sc0/project. The flat 20-file source mirror includes every qualified project body. READBACK.json compares selected source bytes with extracted bytes. SOURCE-CLOSURE.json retains the actual producer and separately selected semantic/primary outcomes with their original statuses; packaging infers no receiving or promotion qualification.

The original 338-file cohort and subsequent 97-file cohort remain separate immutable published dependencies, 435 paths in total. The accepted Fc0 producer source and proof are retained as bounded ancestry. Spc v1/v2 target and fixture provenance is retained; unrelated later authoring families are not implicitly selected.

Use the checked reconstruct.py entry with --portable PORTABLE --output FRESH_DIRECTORY to verify and extract the unchanged-owner archive, then compare all 20 project identities before any new Maven execution. The flat source tree is complete; Maven/JDK/Python/cache and compiled binaries remain excluded or identity-only. Reconstruction is not a hermetic tool image or a new proof receipt. Original qualified project POM and README bytes remain unchanged.
""")
(H/'RECONSTRUCT.md').write_text("""The flat 20-file review mirror contains the complete qualified Sc0 project. The same exact20 files are preserved in the companion archive. From this task directory, the portable folder is ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/string.

For checked archive reconstruction run python3 ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/string/reconstruct.py --portable ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/string --output /path/to/fresh-extraction. A subsequent Maven run against /path/to/fresh-extraction/sc0/project/pom.xml is a new execution and requires the admitted toolchain/cache. PROJECT-READY.json pins all 20 files; there are zero archive-only fixtures. This publication envelope is outside the qualified project census and does not upgrade any semantic or receiving outcome.
""")
tr=[mapped(project/r['path'],TASK+r['path'],'qualified-project-flat',projectPath=r['path']) for r in flat]
write(H/'TASK-MAPPING.json',{'schema':'synexia.sc0.task-mapping/1','files':tr,'logicalProjectFiles':20,'flatProjectFiles':20,'archiveOnlyFiles':withheld,'producer':cl['producer'],'readback':pin(H/'READBACK.json')})
paths=sorted([p for p in B.iterdir() if p.is_file()]+[H/n for n in ('package_evidence.py','PROJECT-READY.json','READBACK.json','SOURCE-CLOSURE.json','package-plan.json','README.md','reconstruct.py','prepare_package.py','finish_package.py')],key=str)
pr=[mapped(p,PORT+p.relative_to(H).as_posix(),'portable-evidence',portablePath=p.relative_to(H).as_posix()) for p in paths]
write(H/'PORTABLE-MANIFEST.json',{'schema':'synexia.sc0.portable-manifest/1','files':pr,'producer':cl['producer'],'readback':pin(H/'READBACK.json'),'bundle':pin(B/'BUNDLE.json'),'artifactExcludedCount':len(cl['excludedIdentities'])})
# External paired envelope includes manifest files without a self-referential manifest digest.
extra=[mapped(H/'TASK-MAPPING.json',PORT+'TASK-MAPPING.json','mapping-envelope'),mapped(H/'PORTABLE-MANIFEST.json',PORT+'PORTABLE-MANIFEST.json','mapping-envelope'),mapped(H/'RECONSTRUCT.md',TASK+'RECONSTRUCT.md','unqualified-reconstruction-envelope')]
parent_paths=[row for cohort in cl['parentPublication']['pairedMaps'] for row in load(Path(cohort['path']))['files']]
assert len(parent_paths)==435
for key in ('synexiaPath','m3Path'):
 assert not ({row[key] for row in parent_paths}&{row[key] for row in tr+pr+extra}),'new mapping overlaps immutable parent publication'
write(H/'PAIRED-FILES.json',{'schema':'synexia.sc0.paired-files/1','files':sorted(tr+pr+extra,key=lambda r:r['synexiaPath']),'taskMapping':pin(H/'TASK-MAPPING.json'),'portableManifest':pin(H/'PORTABLE-MANIFEST.json'),'producer':cl['producer'],'readback':pin(H/'READBACK.json'),'consumerQualified':False,'parentPublication':cl['parentPublication'],'parentProducer':cl['parentProducer'],'semanticOutcome':cl['semanticOutcome'],'receivingOutcome':cl['receivingOutcome']})
print(json.dumps({'readback':pin(H/'READBACK.json'),'taskMapping':pin(H/'TASK-MAPPING.json'),'portableManifest':pin(H/'PORTABLE-MANIFEST.json'),'pairedFiles':pin(H/'PAIRED-FILES.json'),'logicalProject':20,'flatProject':20,'portableFiles':len(pr),'pairedCount':len(tr)+len(pr)+len(extra)}))
