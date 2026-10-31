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
parser.add_argument('--semantic-result',type=Path,required=True)
args=parser.parse_args();P=args.proof.resolve()
# This final prospective selection requires the exact actual /9 STOP and its frozen bounded cohort.
assert args.receiving_result is not None and args.receiving_inputs is not None and not args.receiving_pending,'exact actual primary terminal and bounded selection are mandatory'
receiving_terminal_pin={'path':str(C/'receiver-current-c0-string/primary-observation-v1/RESULT.json'),'sha256':'6f8d824c7b58f801a3563869d1a34605b36f160a1a04ea4dc6d9dc5455e5e6e8','bytes':33725}
receiving_selection_pin={'path':str(C/'receiver-current-c0-string-preparation/receiving-selection-v1/RECEIVING-SELECTION.json'),'sha256':'de8f5754b18391d8e3768d2a7a2ee0073884427eb09e45093cc1a44bf709aa8d','bytes':74585}
assert not args.receiving_result.is_symlink() and args.receiving_result.resolve()==Path(receiving_terminal_pin['path'])
assert not args.receiving_inputs.is_symlink() and args.receiving_inputs.resolve()==Path(receiving_selection_pin['path'])
assert pin(Path(receiving_terminal_pin['path']))==receiving_terminal_pin
assert pin(Path(receiving_selection_pin['path']))==receiving_selection_pin
receiving_selection=load(Path(receiving_selection_pin['path']))
assert set(receiving_selection)=={'schema','terminal','files'} and receiving_selection['schema']=='synexia.sc0.receiving-selection/1'
assert receiving_selection['terminal']==receiving_terminal_pin and isinstance(receiving_selection['files'],list) and len(receiving_selection['files'])==234
assert sum(row['bytes'] for row in receiving_selection['files'])==29379741
receiving_selection_paths=[row['path'] for row in receiving_selection['files']]
assert receiving_selection_paths==sorted(set(receiving_selection_paths)) and receiving_terminal_pin['path'] in receiving_selection_paths
for row in receiving_selection['files']:
 assert set(row)=={'path','sha256','bytes'} and Path(row['path']).is_absolute() and Path(row['path']).is_relative_to(C) and '..' not in Path(row['path']).parts
assert not args.receiving_inputs or args.receiving_result,'receiving inputs require an actual terminal result'
assert sha((P/'RESULT.json').read_bytes())=='93a0a751404f26418165bcd9287b90287214d8c8f3213ce1baf1c975ccaa3fd6','exact accepted Sc0 producer result required'
assert sha((P/'generated-candidate/OUTPUT.json').read_bytes())=='3e9df04fcc1e555dc3ea245cb4dd0a9a1fc6d59635412796c5cfa2e0afd704ac','exact accepted Sc0 output required'
result=load(P/'RESULT.json');assert result['schema']=='synexia.sc0.proof-result/1' and result['status']=='PASS_FOCUSED_PRODUCER' and result['failure'] is None
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
project=load(P/'staging.json');assert len(project)==20
for r in project:admit(P/'project'/r['path'],'sc0/project/'+r['path'],'qualified-project',r)
# Every local non-tool sealed input is retained, including exact reviewed authoring and failed trial metadata.
for r in load(P/'inputs.json'):original(Path(r['path']),'sealed-input',r)
# All actual current and preceding failed trial bodies. Compiled artifacts remain hash-only under the existing owner.
for trial in sorted((I/'evidence').iterdir()):
 if not trial.is_dir():continue
 assert (trial/'RESULT.json').is_file(),'running trial cannot be selected'
 assert load(trial/'RESULT.json')['status'] in ('PASS_FOCUSED_PRODUCER','STOP')
 for p in sorted(trial.rglob('*')):
  if p.is_file():admit(p,'sc0/trials/'+trial.name+'/'+p.relative_to(trial).as_posix(),'actual-trial')
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
# Retain exact reviewed Spc v1/v2 source artifacts without traversing unrelated authoring families.
for relative_handoff,digest in (
 ('current-integration-repairs/string-escape-c0-authoring-v1/HANDOFF.json','47f8d84237d4149a9876331270156506dc07ec3753e192d79285cdf60a016536'),
 ('current-integration-repairs/string-escape-c0-authoring-v2/HANDOFF.json','e2c0cfa3e5a760a279cd2844bf93df43d88af01b25b22829c42e91655b6d2131')):
 handoff=C/relative_handoff;assert sha(handoff.read_bytes())==digest
 original(handoff,'spc-reviewed-handoff')
 for row in load(handoff)['artifacts']:original(Path(row['path']),'spc-reviewed-source-artifact',row)
# Historical v1 predicate review and its explicit applicability correction are retained without gate-ready credit.
for relative_review,digest,size in (
 ('receiver-current-c0-string-preparation/semantic-static-review-v1/REVIEW.json','52fe4a4a286b13f80bc466991dd448e03d75efef1e9f8383332a3f56b9653c90',3044),
 ('receiver-current-c0-string-preparation/semantic-static-review-v1/APPLICABILITY-NOTE.json','0d572a09c11e86435eb53b6c664ba509874992f87f039ce1c4dea89546f99e96',1176)):
 original(C/relative_review,'historical-semantic-static-review',{'sha256':digest,'bytes':size})
# The accepted Fc0 producer is an explicit, bounded source/proof dependency. Its older bundles are not traversed.
fc0=C/'recipe-recovery/current-followup-c0/evidence/producer-v1'
assert sha((fc0/'RESULT.json').read_bytes())=='7f381ac0f810aab4dcc7d46d8ced7eb327452852f83468b31a2763af56db6213'
assert sha((fc0/'generated-candidate/OUTPUT.json').read_bytes())=='3405781f7b0364d20069cff62f68ab95a5333b61f26b927b53158e45a0aeeb2e'
assert load(fc0/'RESULT.json')['status']=='PASS_FOCUSED_PRODUCER'
fc0_project=load(fc0/'staging.json');assert len(fc0_project)==43
for row in fc0_project:original(fc0/'project'/row['path'],'accepted-fc0-project-dependency',row)
for p in sorted(fc0.rglob('*')):
 if p.is_file():original(p,'accepted-fc0-proof-dependency')
parent_producer={'result':pin(fc0/'RESULT.json'),'output':pin(fc0/'generated-candidate/OUTPUT.json'),'projectFiles':43,'outputs':15,'requalified':False}
# Both earlier primary observations remain literal history, without promotion or rewritten terminal status.
for relative_proof,digest,schema in (
 ('receiver-current-c0/primary-observation-v1','78195f460e0c2d58d4736660dab3e3244c16c3b42ac644e065833969ced8e466','m3.ci8891.receiver-capture-result/7-primary'),
 ('receiver-current-c0-followup/primary-observation-v1','fcbf7e041a1465babc525f4a7c9743a5d4f57cb43a1c4bd065d3e90fbdcd1a51','m3.ci8891.receiver-capture-result/8-primary')):
 prior=C/relative_proof;terminal=load(prior/'RESULT.json')
 assert sha((prior/'RESULT.json').read_bytes())==digest and terminal['schema']==schema and terminal['status']=='PRIMARY_ONLY_STOP'
 for p in sorted(prior.rglob('*')):
  if p.is_file():original(p,'prior-actual-receiving-stop')
# Preserve the two immutable published cohorts as separate dictionaries with a disjoint 435-path union.
parent_maps=[];parent_rows=[]
for relative_map,digest,count in (
 ('publication-design/integration-c0-final-v2/FINAL-PAIRED-FILES.json','bb815cfcf64ac5298fa857355b8caccb5ebda6c76e2803906d8c92ac9578a2a8',338),
 ('publication-design/followup-c0-final-v1/FINAL-PAIRED-FILES.json','5cde38612911bdaf74b1ec4dfb857f7f1f747eccf42f72aa55e5458c48df71f2',97)):
 parent_map=C/relative_map;assert sha(parent_map.read_bytes())==digest
 cohort=load(parent_map)['files'];assert len(cohort)==count
 for row in cohort:
  p=Path(row['file']);b=p.read_bytes();assert sha(b)==row['sha256'] and len(b)==row['bytes'] and blob(b)==row['gitBlob'],p
 parent_rows.extend(cohort);parent_maps.append(pin(parent_map));original(parent_map,'immutable-parent-paired-map')
assert len(parent_rows)==435
for key in ('synexiaPath','m3Path'):assert len({row[key] for row in parent_rows})==435
parent_publication=C/'publication-design/followup-c0-final-v1/PUBLISHED.json'
assert sha(parent_publication.read_bytes())=='1da6eed2dada9ee85cea964e3097d869929b03975863c27a0412d55c44ba0d58'
publication=load(parent_publication)
parent_heads={'hsoliwal/com.synexia':'2b05a83fe790ae5b1926eed6bba88a6d5b3c0619','hsoliwal/M3jdk21':'18287e16c3d5dee6243752ccd653264c66bd2020'}
assert publication['schema']=='synexia.fc0.paired-publication/1'
assert {row['repo']:row['sha'] for row in publication['commits']}==parent_heads
original(parent_publication,'immutable-parent-publication-receipt')
parent={'pairedMaps':parent_maps,'publicationReceipt':pin(parent_publication),'heads':parent_heads,'files':435,'localBodiesUnchanged':True,'remoteStateRevalidated':False}
# Literal prior publication-check history. These immutable captures grant no new qualification.
publication_history=[]
for relative_history,digest,size in (
 ('publication-design/followup-c0-final-v1/source-check-v1/RESULT.json','0412191c668da818ec38883442d7fecbd5fa536de8749d099c787dd07753c31f',3726),
 ('publication-design/followup-c0-final-v1/m3-check-v2/RESULT.json','b40b1cb1851ddfacc407cbb5050f4017ff858a4a18bd50de655fe0cf10c732d6',27962),
 ('publication-design/followup-c0-final-v1/root-check-execution-v1/RESULT.json','ae3a68043410d7291ec66e431b7f0eb8a93ff7b72ad134f0bfd2d6b347387f9f',3039),
 ('publication-design/followup-c0-final-v1/root-check-execution-v1/m3.json','0a35965a8eadfeaea6351b40d9452ec4d90bb65937bff64cdb6d9fbbf37e5b7f',1363),
 ('publication-design/followup-c0-final-v1/root-check-execution-v1/m3.stderr.log','8ba3d31226d999cd44b72cdecf3d9314e9865de99d2aa4505ded8d12be3eb541',1319),
 ('publication-design/followup-c0-final-v1/root-check-execution-v1/m3.stdout.log','e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',0),
 ('publication-design/followup-c0-final-v1/root-check-execution-v1/source.json','c1fdbd6d0225bfd4ab3253798e0cd20a11118c84c916a913718543d61742d57c',1407),
 ('publication-design/followup-c0-final-v1/root-check-execution-v1/source.stderr.log','e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',0),
 ('publication-design/followup-c0-final-v1/root-check-execution-v1/source.stdout.log','7695060b430eb95b80b53501f858b367c0d7512025c9669a641bd0ade4be9f3f',282),
 ('publication-design/followup-c0-final-v1/root-check-execution-v2/m3.json','b0675d11054ae60cbeadf0f1fcc27a2ad5d3b2d7830b36b5bc18d7dd3fbdee57',1347),
 ('publication-design/followup-c0-final-v1/root-check-execution-v2/m3.stderr.log','e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',0),
 ('publication-design/followup-c0-final-v1/root-check-execution-v2/m3.stdout.log','90b3b7d9716cc5319ad0968d388dea5050dcec817fc9c00f61ad1abafd1783c4',400)):
 history_path=C/relative_history;history_pin={'path':str(history_path),'sha256':digest,'bytes':size}
 original(history_path,'prior-publication-check-history',history_pin);publication_history.append(history_pin)
publication_check_history={'files':publication_history,'literalHistoryOnly':True,'qualificationInherited':False}
# Bind the actual separate semantic event; this does not claim primary, runtime-fix or full receiving qualification.
semantic_path=args.semantic_result.resolve();assert semantic_path.is_relative_to(C)
assert sha(semantic_path.read_bytes())=='072c8c827e49f7822f780a17e9c1a2fcc80b1ce5744fbbd90f197901b61a41de'
semantic_body=load(semantic_path)
assert semantic_body['schema']=='synexia.spc.semantic-result/1' and semantic_body['status']=='PASS_SPC_SEMANTICS_PRIMARY_PENDING'
assert semantic_body['failure'] is None and semantic_body['fatalFailure'] is None
semantic_observed={key:value for key,value in semantic_body.items() if isinstance(value,bool)}
assert semantic_observed.get('spcSemanticQualified') is True
for key in ('primaryGroupQualified','consumerQualified','fullReceiverQualified','fullM3RuntimeQualified','runtimeFixQualified','productMainExecuted','productionPromotion','priorReceivingGateQualificationInherited','systemLoaderAndSharedLibraryClosureComplete'):
 assert semantic_observed.get(key) is False,key
assert semantic_body['sc0Result']==pin(P/'RESULT.json') and semantic_body['sc0Output']==pin(P/'generated-candidate/OUTPUT.json')
semantic_plan_row=semantic_body['plan'];assert set(semantic_plan_row)=={'path','sha256','bytes'}
assert semantic_plan_row['sha256']=='946d2ce1d5d6b948c6ed8794c776904a07a886b980027092f2104f47dd18f85e'
semantic_plan_path=Path(semantic_plan_row['path']);assert semantic_plan_path.is_absolute() and semantic_plan_path.is_relative_to(C)
assert pin(semantic_plan_path)==semantic_plan_row
semantic_plan=load(semantic_plan_path);assert semantic_plan['schema']=='synexia.spc.semantic-plan/1'
semantic_controller=semantic_plan['controller'];assert set(semantic_controller)=={'path','sha256','bytes'}
assert semantic_controller['sha256']=='e5af8e5b84691328d05a4177881b18dae2c7596d19f6c743ebea0a7c804bc823'
assert semantic_plan['sc0Result']==semantic_body['sc0Result'] and semantic_plan['sc0Output']==semantic_body['sc0Output']
assert semantic_plan['selection']==semantic_body['selection'] and semantic_plan['staging']==semantic_body['staging']
semantic_ids=['compile-fixture','compiler-typed-literal-contract','metadata-preservation-controls']
assert semantic_body['completedCommands']==semantic_ids and semantic_body['unexecutedCommands']==[] and len(semantic_body['receipts'])==3
semantic_binding={'result':pin(semantic_path),'plan':semantic_plan_row,'controller':semantic_controller}
semantic_pins=[semantic_binding['result'],semantic_plan_row,semantic_controller,semantic_body['admission'],semantic_body['observedOutputs'],semantic_body['selection'],semantic_body['staging'],*semantic_body['evidence'],*semantic_body['receipts']]
for key in ('expectations','jdkAuthority','pythonAuthority','materializationCommandReceipt','materializationReceipt'):semantic_pins.append(semantic_plan[key])
semantic_pins.extend(semantic_plan['authorityInputs'])
for row in semantic_pins:
 assert set(row)>={'path','sha256','bytes'} and Path(row['path']).is_absolute() and Path(row['path']).is_relative_to(C)
 original(Path(row['path']),'actual-spc-semantic-evidence',row)
for index,(row,command) in enumerate(zip(semantic_body['receipts'],semantic_plan['commands'])):
 receipt=load(Path(row['path']))
 assert receipt['schema']=='synexia.spc.semantic-command/1' and receipt['id']==semantic_ids[index]
 assert receipt['argv']==command['argv'] and receipt['cwd']==semantic_plan['cwd'] and receipt['environment']==semantic_plan['environment']
 assert receipt['timeoutSeconds']==command['timeoutSeconds'] and receipt['exitCode']==0 and receipt['executionError'] is None
 assert receipt['priorReceipts']==semantic_body['receipts'][:index] and receipt['planSHA256']==semantic_plan_row['sha256']
 for channel in ('stdout','stderr'):original(Path(receipt[channel]['path']),'actual-spc-semantic-stream',receipt[channel])
# Exact observed Java source bodies are retained. The unchanged owner decides compiled-body exclusions; identities remain in the original output ledgers.
for group in semantic_body['sealedOutputs']:
 root=Path(group['root']);assert root.is_absolute() and root.parent==semantic_path.parent
 for row in group['files']:
  relative=Path(row['path']);assert not relative.is_absolute() and '..' not in relative.parts
  original(root/relative,'actual-spc-semantic-observation',row)
semantic_root_execution=C/'receiver-current-c0-string-preparation/semantic-authoring/root-execution-v1/RESULT.json'
semantic_root_readback=semantic_path.parent/'ROOT-READBACK.json'
assert sha(semantic_root_execution.read_bytes())=='afe1f60d72715f7bfcb143372e97e3b6e81c5d9d4f6b176fe41cfdfdb39e12d0'
assert sha(semantic_root_readback.read_bytes())=='35f53f08efd3a9f4661aa96ad6b10d1eca9b78b03a3847f040ad64c88905db17'
original(semantic_root_execution,'actual-spc-root-execution');original(semantic_root_readback,'actual-spc-root-readback')
# Retain the exact bounded semantic selection and its original bodies through the unchanged owner.
semantic_selection_path=C/'receiver-current-c0-string-preparation/semantic-selection-v1/SEMANTIC-SELECTION.json'
semantic_selection_pin={'path':str(semantic_selection_path),'sha256':'59e2b5a9598f343a04a528879b3928e420292ab678b2786c597158624c020821','bytes':32783}
assert not semantic_selection_path.is_symlink() and pin(semantic_selection_path)==semantic_selection_pin
semantic_selection=load(semantic_selection_path)
assert set(semantic_selection)=={'schema','terminal','files'} and semantic_selection['schema']=='synexia.sc0.semantic-selection/1'
assert semantic_selection['terminal']==pin(semantic_path) and isinstance(semantic_selection['files'],list) and len(semantic_selection['files'])==108
semantic_selection_paths=[row['path'] for row in semantic_selection['files']]
assert semantic_selection_paths==sorted(set(semantic_selection_paths)) and str(semantic_path) in semantic_selection_paths
assert all(row in receiving_selection['files'] for row in semantic_selection['files']),'exact semantic cohort must remain within final receiving selection'
for row in semantic_selection['files']:
 assert set(row)=={'path','sha256','bytes'} and Path(row['path']).is_absolute() and Path(row['path']).is_relative_to(C) and '..' not in Path(row['path']).parts
 original(Path(row['path']),'actual-spc-semantic-selection-evidence',row)
original(semantic_selection_path,'spc-semantic-selection',semantic_selection_pin)
# The census is metadata only; every one of its 20 required bodies must already be retained by the exact selection.
semantic_census_path=C/'publication-design/string-c0-check-authoring-v1/m3-check/SEMANTIC-CUSTODY-INPUTS.json'
semantic_census_pin={'path':str(semantic_census_path),'sha256':'c7f64c36e70556f07947844e38005bd2ed9bb4bd2612b8ef0d4be04d4b0d098b','bytes':5983}
assert not semantic_census_path.is_symlink() and pin(semantic_census_path)==semantic_census_pin
semantic_census=load(semantic_census_path)
assert semantic_census['schema']=='synexia.sc0.publication-semantic-custody-requirements/1' and semantic_census['status']=='READ_ONLY_ACTUAL_PIN_CENSUS'
assert semantic_census['semanticResult']==pin(semantic_path) and len(semantic_census['files'])==20
assert semantic_census['allBodiesRehashed'] is True and semantic_census['gatesExecuted'] is False and semantic_census['compiledOutputsRequiredLoose'] is False
for row in semantic_census['files']:
 assert row in semantic_selection['files'],'census body must belong to exact semantic selection'
 logical='sources/'+Path(row['path']).relative_to(C).as_posix()
 assert logical in selected and selected[logical]['sha256']==row['sha256'] and selected[logical]['bytes']==row['bytes'],'required census body must be included'
original(semantic_census_path,'semantic-custody-requirements-metadata',semantic_census_pin)
semantic_outcome={'status':'ACTUAL_SEMANTIC_TERMINAL_RETAINED','reportedSchema':semantic_body['schema'],'reportedStatus':semantic_body['status'],'terminal':pin(semantic_path),'prerequisite':semantic_binding,'observedBooleans':semantic_observed,'receipts':semantic_body['receipts'],'evidence':semantic_body['evidence'],'rootExecution':pin(semantic_root_execution),'rootReadback':pin(semantic_root_readback),'selectionManifest':semantic_selection_pin,'selectionFiles':108,'custodyCensus':semantic_census_pin,'custodyBodyFiles':20,'qualificationNotInferredByPackaging':True,'consumerQualified':False}
# One bounded receiving selection is made before packaging; no sealed archive is appended or rewritten.
receiving_outcome={'status':'PENDING_NOT_INCLUDED','consumerQualified':False,'files':[]}
if args.receiving_result:
 terminal=args.receiving_result.resolve();assert terminal.is_relative_to(C)
 assert pin(terminal)==receiving_terminal_pin
 terminal_body=load(terminal);status=terminal_body.get('status')
 assert terminal_body.get('schema')=='m3.ci8891.receiver-capture-result/9-primary' and status in ('PRIMARY_ONLY_PASS_RECEIVING_PENDING','PRIMARY_ONLY_STOP'),'declared current-string primary terminal required'
 assert status=='PRIMARY_ONLY_STOP','retain the actual observed terminal status'
 assert terminal_body.get('observationScope')=='PRIMARY_ONLY'
 assert all(terminal_body.get(key)==result[key] for key in ('sourceCommit','targetCommit'))
 observed={key:value for key,value in terminal_body.items() if isinstance(value,bool)}
 assert observed.get('primaryGroupQualified') is (status=='PRIMARY_ONLY_PASS_RECEIVING_PENDING')
 for key in ('allAdmittedGroupsQualified','positiveCliGroupsQualified','bridgeGroupQualified','materializedInputsQualified','consumerQualified','fullReceiverQualified','strictCanonicalAdmission','canonicalProductionApplied','canonicalPromotionAuthorized','hostedWorkflowExecuted','fullOpenJdkBuildQualified','jep458BlockedSpecificationObserved','jep458ProjectionQualified','jep458HostedWorkflowQualified'):
  assert observed.get(key) is False,key
 assert observed.get('preparedInputFixedPointObserved') is True and observed.get('testsCountedOnlyFromActualReports') is True
 assert observed.get('spcSemanticQualified') is True
 assert terminal_body.get('spcSemanticPrerequisite')==semantic_binding,'primary must bind the exact separate semantic event'
 materialization=[]
 for key in ('materializationCommandReceipt','materializationReceipt'):
  row=terminal_body[key];assert set(row)=={'path','sha256','bytes'} and Path(row['path']).is_absolute() and Path(row['path']).is_relative_to(C)
  original(Path(row['path']),'actual-receiving-materialization',row);materialization.append({'field':key,**row})
 original(terminal,'actual-receiving-terminal',receiving_terminal_pin)
 receiving_files=[pin(terminal)]
 if args.receiving_inputs:
  selection=args.receiving_inputs.resolve();assert selection.is_relative_to(C)
  assert pin(selection)==receiving_selection_pin
  value=load(selection);assert set(value)=={'schema','terminal','files'} and value['schema']=='synexia.sc0.receiving-selection/1'
  assert value==receiving_selection and len(value['files'])==234
  assert value['terminal']==pin(terminal) and isinstance(value['files'],list)
  paths=[row['path'] for row in value['files']];assert paths==sorted(set(paths)) and str(terminal) in paths
  for row in value['files']:
   assert set(row)=={'path','sha256','bytes'} and Path(row['path']).is_absolute() and Path(row['path']).is_relative_to(C) and '..' not in Path(row['path']).parts
   original(Path(row['path']),'actual-receiving-evidence',row)
  original(selection,'receiving-selection',receiving_selection_pin);receiving_files=value['files']
 receiving_outcome={'status':'ACTUAL_TERMINAL_RETAINED','reportedSchema':terminal_body['schema'],'reportedStatus':status,'observationScope':terminal_body['observationScope'],'observedBooleans':observed,'terminal':pin(terminal),'selectionManifest':receiving_selection_pin,'files':receiving_files,'materializationEvidence':materialization,'spcSemanticPrerequisite':terminal_body['spcSemanticPrerequisite'],'consumerQualified':False,'qualificationNotInferredByPackaging':True}
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
record={'schema':'synexia.sc0.package-source-closure/1','producer':{'result':pin(P/'RESULT.json'),'output':pin(P/'generated-candidate/OUTPUT.json')},'bodyMappings':sorted(mappings,key=lambda r:r['logicalPath']),'excludedIdentities':excluded,'sealedInputDispositions':closure,'compiledArtifactBodiesIncluded':False,'toolchainBodiesIncluded':False,'hermetic':False,'historicalMissingProofsRecreated':False,'consumerQualified':False,'archiveOwner':pin(OWNER),'parentPublication':parent,'parentProducer':parent_producer,'publicationCheckHistory':publication_check_history,'semanticOutcome':semantic_outcome,'receivingOutcome':receiving_outcome}
write(S/'SOURCE-CLOSURE.json',record)
# Empty LANES is deliberate: actual terminal evidence is explicitly selected and independently hash-closed above.
plan={'schema':'synexia.evidence.package-plan/1','status':'FINAL_REVIEWED_SELECTION','sourceCommit':result['sourceCommit'],'targetCommit':result['targetCommit'],'packageOwnerSHA256':OWNER_SHA,'resultSHA256':sha((P/'RESULT.json').read_bytes()),'laneSelection':{'required':[],'additional':[],'allowNotExecuted':[],'includeAllExisting':False},'includePaths':sorted([*selected,'SOURCE-CLOSURE.json']),'selectionMode':'EXPLICIT_FINISHED_BODIES_WITH_SOURCE_CLOSURE_AUDIT','noAutomaticLaneQualification':True}
write(HERE/'package-plan.json',plan)
write(HERE/'SOURCE-CLOSURE.json',record)
print(json.dumps({'plan':pin(HERE/'package-plan.json'),'closure':pin(HERE/'SOURCE-CLOSURE.json'),'bodyCount':len(selected)+1,'projectFiles':20,'actualResult':pin(P/'RESULT.json')}))
