"""Select and copy finished fresh evidence using the unchanged archive owner's classifications.
No proof, build, package, verification or extraction command is executed here.
"""
from pathlib import Path
import argparse,hashlib,json,os,stat
HERE=Path(__file__).resolve().parent;I=HERE.parent;C=I.parents[1]
OWNER=C/'m3-checkout/m3/migration/intake-20261005/current-752-ci/cih-producer-portable/package_evidence.py'
OWNER_SHA='89d43693f508c723e431296cc06f611c8de4f94df8eaddea95a2c7742feb0574'
def sha(b):return hashlib.sha256(b).hexdigest()
def blob(b):return hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()
def load(p):return json.loads(p.read_bytes())
def pin(p):b=p.read_bytes();return {'path':str(p),'sha256':sha(b),'bytes':len(b)}
def write(p,v):
 assert not p.exists(),p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,sort_keys=True,indent=2)+'\n')
parser=argparse.ArgumentParser();parser.add_argument('--proof',type=Path,required=True);args=parser.parse_args();P=args.proof.resolve()
result=load(P/'RESULT.json');assert result['schema']=='synexia.ic0.proof-result/1' and result['status']=='PASS_FOCUSED_PRODUCER' and result['failure'] is None
assert result['runtime']['output']==pin(P/'generated-candidate/OUTPUT.json')
owner_bytes=OWNER.read_bytes();assert sha(owner_bytes)==OWNER_SHA
ns={'__name__':'_unchanged_package_owner','__file__':str(OWNER)};exec(compile(owner_bytes,str(OWNER),'exec'),ns)
# Imports the authenticated owner only for its existing classification; no owner CLI or archive function runs.
classify=ns['classify'];private=ns['PRIVATE_MATERIAL'];S=HERE/'source';assert not S.exists();S.mkdir()
selected={};mappings=[];excluded=[]
def admit(p,logical,role,expected=None):
 p=Path(p);assert p.is_file() and not p.is_symlink(),p
 if logical in selected:
  assert selected[logical]['inputPath']==str(p);return
 reason=classify(p,logical)
 if reason:
  read_now=expected is None
  if expected is None:expected=pin(p)
  excluded.append({'inputPath':str(p),'sha256':expected['sha256'],'bytes':expected['bytes'],'reason':reason,'currentBytesRead':read_now});return
 b=p.read_bytes()
 if expected is not None:assert sha(b)==expected['sha256'] and len(b)==expected['bytes'],p
 if b[:4]==b'\x7fELF' or b[:2]==b'MZ' or private.search(b):
  excluded.append({'inputPath':str(p),'sha256':sha(b),'bytes':len(b),'reason':'compiled-artifact-hash-only' if not private.search(b) else 'private-material-pattern-content-omitted','currentBytesRead':True});return
 mode=stat.S_IMODE(p.stat().st_mode)&0o777
 dest=S/logical;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(b);dest.chmod(mode)
 row={'inputPath':str(p),'logicalPath':logical,'bytes':len(b),'sha256':sha(b),'gitBlob':blob(b),'mode':format(mode,'04o'),'role':role};selected[logical]=row;mappings.append(row)
def original(p,role,expected=None):
 p=Path(p)
 if not p.is_relative_to(C):
  assert expected is not None;excluded.append({'inputPath':str(p),'sha256':expected['sha256'],'bytes':expected['bytes'],'reason':'external-host-input-manifest-only','currentBytesRead':False});return
 admit(p,'sources/'+p.relative_to(C).as_posix(),role,expected)
# Reconstruct the exact complete qualified Maven project under the documented stable logical path.
project=load(P/'staging.json');assert len(project)==292
for r in project:admit(P/'project'/r['path'],'ic0/project/'+r['path'],'qualified-project',r)
# Every local non-tool sealed input is retained, including exact reviewed authoring and failed trial metadata.
for r in load(P/'inputs.json'):original(Path(r['path']),'sealed-input',r)
# All actual current and preceding failed trial bodies. Compiled artifacts remain hash-only under the existing owner.
for trial in sorted((I/'evidence').iterdir()):
 if not trial.is_dir():continue
 assert (trial/'RESULT.json').is_file(),'running trial cannot be selected'
 assert load(trial/'RESULT.json')['status'] in ('PASS_FOCUSED_PRODUCER','STOP')
 for p in sorted(trial.rglob('*')):
  if p.is_file():admit(p,'ic0/trials/'+trial.name+'/'+p.relative_to(trial).as_posix(),'actual-trial')
# Reviewed local metadata only; bounded trees, no parent repository/cache walk.
roots=[I/'independent-review-v1',I/'independent-review-v2',I/'delivery-v1']
roots += [p for p in I.iterdir() if p.is_dir() and (p.name.startswith('root-run-') or p.name.startswith('root-prepare-'))]
for root in roots:
 if root.exists():
  for p in sorted(root.rglob('*')):
   if p.is_file():original(p,'review-or-root-capture')
for p in sorted(I.iterdir()):
 if p.is_file():original(p,'root-capture-or-readback')
for p in [I/'delivery-authoring-v1/emit_selection.py',C/'publication-design/integration-c0-checkpoint-v1/HISTORY-AVAILABILITY.json',HERE/'prepare_package.py']:
 if p.is_file():original(p,'delivery-provenance')
admit(OWNER,'package_evidence.py','unchanged-archive-owner')
# Bind every prepared input to an exact copied body or an explicit unchanged-owner exclusion.
alias={}
for r in mappings:alias.setdefault((r['sha256'],r['bytes']),[]).append(r['logicalPath'])
closure=[]
for r in load(P/'inputs.json'):
 hits=sorted(alias.get((r['sha256'],r['bytes']),[]));entry={'inputPath':r['path'],'sha256':r['sha256'],'bytes':r['bytes']}
 if hits:entry.update(disposition='EXACT_BODY_INCLUDED',logicalPaths=hits)
 else:
  reasons=[e['reason'] for e in excluded if e['inputPath']==r['path'] and e['sha256']==r['sha256'] and e['bytes']==r['bytes']];assert reasons,r['path'];entry.update(disposition='EXCLUDED_IDENTITY_ONLY',reasons=sorted(set(reasons)))
 closure.append(entry)
record={'schema':'synexia.ic0.package-source-closure/1','producer':{'result':pin(P/'RESULT.json'),'output':pin(P/'generated-candidate/OUTPUT.json')},'bodyMappings':sorted(mappings,key=lambda r:r['logicalPath']),'excludedIdentities':excluded,'sealedInputDispositions':closure,'compiledArtifactBodiesIncluded':False,'toolchainBodiesIncluded':False,'hermetic':False,'historicalMissingProofsRecreated':False,'consumerQualified':False,'archiveOwner':pin(OWNER)}
write(S/'SOURCE-CLOSURE.json',record)
# Empty LANES is deliberate: actual terminal evidence is explicitly selected and independently hash-closed above.
plan={'schema':'synexia.evidence.package-plan/1','status':'FINAL_REVIEWED_SELECTION','sourceCommit':result['sourceCommit'],'targetCommit':result['targetCommit'],'packageOwnerSHA256':OWNER_SHA,'resultSHA256':sha((P/'RESULT.json').read_bytes()),'laneSelection':{'required':[],'additional':[],'allowNotExecuted':[],'includeAllExisting':False},'includePaths':sorted([*selected,'SOURCE-CLOSURE.json']),'selectionMode':'EXPLICIT_FINISHED_BODIES_WITH_SOURCE_CLOSURE_AUDIT','noAutomaticLaneQualification':True}
write(HERE/'package-plan.json',plan)
write(HERE/'SOURCE-CLOSURE.json',record)
print(json.dumps({'plan':pin(HERE/'package-plan.json'),'closure':pin(HERE/'SOURCE-CLOSURE.json'),'bodyCount':len(selected)+1,'projectFiles':292,'actualResult':pin(P/'RESULT.json')}))
