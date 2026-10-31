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
parser=argparse.ArgumentParser();parser.add_argument('--proof',type=Path,required=True)
receiving=parser.add_mutually_exclusive_group(required=True)
receiving.add_argument('--receiving-result',type=Path)
receiving.add_argument('--receiving-pending',action='store_true')
parser.add_argument('--receiving-inputs',type=Path)
args=parser.parse_args();P=args.proof.resolve()
assert not args.receiving_inputs or args.receiving_result,'receiving inputs require an actual terminal result'
assert sha((P/'RESULT.json').read_bytes())=='7f381ac0f810aab4dcc7d46d8ced7eb327452852f83468b31a2763af56db6213','exact accepted Fc0 producer result required'
assert sha((P/'generated-candidate/OUTPUT.json').read_bytes())=='3405781f7b0364d20069cff62f68ab95a5333b61f26b927b53158e45a0aeeb2e','exact accepted Fc0 output required'
result=load(P/'RESULT.json');assert result['schema']=='synexia.fc0.proof-result/1' and result['status']=='PASS_FOCUSED_PRODUCER' and result['failure'] is None
assert result['runtime']['output']==pin(P/'generated-candidate/OUTPUT.json')
owner_bytes=OWNER.read_bytes();assert sha(owner_bytes)==OWNER_SHA
ns={'__name__':'_unchanged_package_owner','__file__':str(OWNER)};exec(compile(owner_bytes,str(OWNER),'exec'),ns)
# Imports the authenticated owner only for its existing classification; no owner CLI or archive function runs.
classify=ns['classify'];private=ns['PRIVATE_MATERIAL'];S=HERE/'source';assert not S.exists();S.mkdir()
selected={};mappings=[];excluded=[]
def admit(p,logical,role,expected=None):
 p=Path(p);assert p.is_file() and not p.is_symlink(),p
 if logical in selected:
  prior=selected[logical];assert prior['inputPath']==str(p)
  if expected is not None:
   assert prior['sha256']==expected['sha256'] and prior['bytes']==expected['bytes'],p
   copied=(S/logical).read_bytes();assert sha(copied)==expected['sha256'] and len(copied)==expected['bytes'],p
  return
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
project=load(P/'staging.json');assert len(project)==43
for r in project:admit(P/'project'/r['path'],'fc0/project/'+r['path'],'qualified-project',r)
# Every local non-tool sealed input is retained, including exact reviewed authoring and failed trial metadata.
for r in load(P/'inputs.json'):original(Path(r['path']),'sealed-input',r)
# All actual current and preceding failed trial bodies. Compiled artifacts remain hash-only under the existing owner.
for trial in sorted((I/'evidence').iterdir()):
 if not trial.is_dir():continue
 assert (trial/'RESULT.json').is_file(),'running trial cannot be selected'
 assert load(trial/'RESULT.json')['status'] in ('PASS_FOCUSED_PRODUCER','STOP')
 for p in sorted(trial.rglob('*')):
  if p.is_file():admit(p,'fc0/trials/'+trial.name+'/'+p.relative_to(trial).as_posix(),'actual-trial')
# Reviewed local metadata only; bounded trees, no parent repository/cache walk.
roots=[I/'independent-review-v1',I/'independent-review-v2',I/'qualified-v1',I/'delivery-v1',I/'root-prepare',I/'root-run']
roots += [p for p in I.iterdir() if p.is_dir() and (p.name.startswith('root-run-') or p.name.startswith('root-prepare-'))]
for root in roots:
 if root.exists():
  for p in sorted(root.rglob('*')):
   if p.is_file():original(p,'review-or-root-capture')
for p in sorted(I.iterdir()):
 if p.is_file():original(p,'root-capture-or-readback')
for p in [I/'delivery-authoring-v1/emit_selection.py',C/'publication-design/integration-c0-final-v2/HISTORY-AVAILABILITY.json',HERE/'prepare_package.py',HERE/'finish_package.py',HERE/'reconstruct.py',HERE/'AUTHORING-PLAN.json']:
 if p.is_file():original(p,'delivery-provenance')
# These completed reviews and the preceding actual receiving STOP are outside the frozen producer inputs.
review_pins={
 'base-refresh-20261006-0443/native/fc0-producer-independent-review-v1/REVIEW.json':('98695b228ce921d22b2e67f8bf01928c0afceda00fb81ab5794c1b8672981624',5274),
 'base-refresh-20261006-0443/native/foundation-pattern-composition-review-v1/REVIEW-v2.json':('4021f36f982a12aaf63241ff93a3b1e28a9a65c339ef669dcf4e80cfc00c5f7c',24516)}
for name,(digest,size) in review_pins.items():original(C/name,'post-proof-independent-review',{'sha256':digest,'bytes':size})
prior_receiving=C/'receiver-current-c0/primary-observation-v1'
assert sha((prior_receiving/'RESULT.json').read_bytes())=='78195f460e0c2d58d4736660dab3e3244c16c3b42ac644e065833969ced8e466'
assert load(prior_receiving/'RESULT.json')['status']=='PRIMARY_ONLY_STOP'
for p in sorted(prior_receiving.rglob('*')):
 if p.is_file():original(p,'prior-actual-receiving-stop')
# The previously published 338 paired bodies are immutable dependencies, not new outputs.
parent_map=C/'publication-design/integration-c0-final-v2/FINAL-PAIRED-FILES.json'
assert sha(parent_map.read_bytes())=='bb815cfcf64ac5298fa857355b8caccb5ebda6c76e2803906d8c92ac9578a2a8'
parent_rows=load(parent_map)['files'];assert len(parent_rows)==338
for row in parent_rows:
 p=Path(row['file']);b=p.read_bytes();assert sha(b)==row['sha256'] and len(b)==row['bytes'] and blob(b)==row['gitBlob'],p
original(parent_map,'immutable-parent-paired-map')
parent_publication=C/'publication-design/integration-c0-final-v2/PUBLISHED.json'
assert sha(parent_publication.read_bytes())=='3cfd177c701dcbf0723dfd6b9b9b0ba7986130bc83c266b74f774de5618e9735'
publication=load(parent_publication)
parent_heads={'hsoliwal/com.synexia':'f7de6eebed10a4996dfcecc49172d45ac28244c3','hsoliwal/M3jdk21':'7fe5cb0d19bdb3a332f709dfcfbb1f3800919eb4'}
assert {row['repository']:row['head'] for row in publication['prs']}==parent_heads
original(parent_publication,'immutable-parent-publication-receipt')
parent={'pairedMap':pin(parent_map),'publicationReceipt':pin(parent_publication),'heads':parent_heads,'files':338,'localBodiesUnchanged':True,'remoteStateRevalidated':False}
# One bounded receiving selection is made before packaging; no sealed archive is appended or rewritten.
receiving_outcome={'status':'PENDING_NOT_INCLUDED','consumerQualified':False,'files':[]}
if args.receiving_result:
 terminal=args.receiving_result.resolve();assert terminal.is_relative_to(C)
 terminal_body=load(terminal);status=terminal_body.get('status')
 assert terminal_body.get('schema')=='m3.ci8891.receiver-capture-result/8-primary' and status in ('PRIMARY_ONLY_PASS_RECEIVING_PENDING','PRIMARY_ONLY_STOP'),'declared current-followup primary terminal required'
 assert terminal_body.get('observationScope')=='PRIMARY_ONLY'
 assert all(terminal_body.get(key)==result[key] for key in ('sourceCommit','targetCommit'))
 observed={key:value for key,value in terminal_body.items() if isinstance(value,bool)}
 assert observed.get('primaryGroupQualified') is (status=='PRIMARY_ONLY_PASS_RECEIVING_PENDING')
 for key in ('allAdmittedGroupsQualified','positiveCliGroupsQualified','bridgeGroupQualified','materializedInputsQualified','consumerQualified','fullReceiverQualified','strictCanonicalAdmission','canonicalProductionApplied','canonicalPromotionAuthorized','hostedWorkflowExecuted','fullOpenJdkBuildQualified','jep458BlockedSpecificationObserved','jep458ProjectionQualified','jep458HostedWorkflowQualified'):
  assert observed.get(key) is False,key
 assert observed.get('preparedInputFixedPointObserved') is True and observed.get('testsCountedOnlyFromActualReports') is True
 materialization=[]
 for key in ('materializationCommandReceipt','materializationReceipt'):
  row=terminal_body[key];assert set(row)=={'path','sha256','bytes'} and Path(row['path']).is_absolute() and Path(row['path']).is_relative_to(C)
  original(Path(row['path']),'actual-receiving-materialization',row);materialization.append({'field':key,**row})
 original(terminal,'actual-receiving-terminal')
 receiving_files=[pin(terminal)]
 if args.receiving_inputs:
  selection=args.receiving_inputs.resolve();assert selection.is_relative_to(C)
  value=load(selection);assert set(value)=={'schema','terminal','files'} and value['schema']=='synexia.fc0.receiving-selection/1'
  assert value['terminal']==pin(terminal) and isinstance(value['files'],list)
  paths=[row['path'] for row in value['files']];assert paths==sorted(set(paths)) and str(terminal) in paths
  for row in value['files']:
   assert set(row)=={'path','sha256','bytes'} and Path(row['path']).is_absolute() and Path(row['path']).is_relative_to(C)
   original(Path(row['path']),'actual-receiving-evidence',row)
  original(selection,'receiving-selection');receiving_files=value['files']
 receiving_outcome={'status':'ACTUAL_TERMINAL_RETAINED','reportedSchema':terminal_body['schema'],'reportedStatus':status,'observationScope':terminal_body['observationScope'],'observedBooleans':observed,'terminal':pin(terminal),'files':receiving_files,'materializationEvidence':materialization,'consumerQualified':False,'qualificationNotInferredByPackaging':True}
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
record={'schema':'synexia.fc0.package-source-closure/1','producer':{'result':pin(P/'RESULT.json'),'output':pin(P/'generated-candidate/OUTPUT.json')},'bodyMappings':sorted(mappings,key=lambda r:r['logicalPath']),'excludedIdentities':excluded,'sealedInputDispositions':closure,'compiledArtifactBodiesIncluded':False,'toolchainBodiesIncluded':False,'hermetic':False,'historicalMissingProofsRecreated':False,'consumerQualified':False,'archiveOwner':pin(OWNER),'parentPublication':parent,'receivingOutcome':receiving_outcome}
write(S/'SOURCE-CLOSURE.json',record)
# Empty LANES is deliberate: actual terminal evidence is explicitly selected and independently hash-closed above.
plan={'schema':'synexia.evidence.package-plan/1','status':'FINAL_REVIEWED_SELECTION','sourceCommit':result['sourceCommit'],'targetCommit':result['targetCommit'],'packageOwnerSHA256':OWNER_SHA,'resultSHA256':sha((P/'RESULT.json').read_bytes()),'laneSelection':{'required':[],'additional':[],'allowNotExecuted':[],'includeAllExisting':False},'includePaths':sorted([*selected,'SOURCE-CLOSURE.json']),'selectionMode':'EXPLICIT_FINISHED_BODIES_WITH_SOURCE_CLOSURE_AUDIT','noAutomaticLaneQualification':True}
write(HERE/'package-plan.json',plan)
write(HERE/'SOURCE-CLOSURE.json',record)
print(json.dumps({'plan':pin(HERE/'package-plan.json'),'closure':pin(HERE/'SOURCE-CLOSURE.json'),'bodyCount':len(selected)+1,'projectFiles':43,'actualResult':pin(P/'RESULT.json')}))
