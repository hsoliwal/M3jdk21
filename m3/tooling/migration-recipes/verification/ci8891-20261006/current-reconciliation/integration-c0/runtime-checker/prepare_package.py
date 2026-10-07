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
parser.add_argument('--semantic-result',type=Path,required=True)
parser.add_argument('--semantic-inputs',type=Path,required=True)
parser.add_argument('--receiving-result',type=Path,required=True)
parser.add_argument('--receiving-inputs',type=Path,required=True)
args=parser.parse_args();P=args.proof.resolve()
# Exact actual /10 STOP and root-emitted evidence selection are bound below.
assert sha((P/'RESULT.json').read_bytes())=='c4cb9608ab895490158236e93334b83eea1996f454d8085c454676e3fea37e75','exact accepted Rf0 producer result required'
assert sha((P/'generated-candidate/OUTPUT.json').read_bytes())=='0c5889046c2e4e8962f02071b0ba6a9ae6a8b2fc82f2d62cd2cc1687b62f605c','exact accepted Rf0 output required'
result=load(P/'RESULT.json');assert result['schema']=='synexia.rf0.proof-result/1' and result['status']=='PASS_FOCUSED_PRODUCER' and result['failure'] is None
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
project=load(P/'staging.json');assert len(project)==16
for r in project:admit(P/'project'/r['path'],'rf0/project/'+r['path'],'qualified-project',r)
# Every local non-tool sealed input is retained, including exact reviewed authoring and failed trial metadata.
for r in load(P/'inputs.json'):original(Path(r['path']),'sealed-input',r)
# Select only the exact producer proof here. Separate semantic attempts enter through explicit regular-file cohorts; intentional negative symlinks are never traversed.
for trial in (P,):
 if not trial.is_dir():continue
 assert (trial/'RESULT.json').is_file(),'running trial cannot be selected'
 assert load(trial/'RESULT.json')['status'] in ('PASS_FOCUSED_PRODUCER','STOP')
 for p in sorted(trial.rglob('*')):
  if p.is_file():admit(p,'rf0/trials/'+trial.name+'/'+p.relative_to(trial).as_posix(),'actual-trial')
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
# Exact frozen Rfc handoff/source artifacts, with original authoring/refusal provenance already sealed in inputs.
handoff=C/'current-integration-repairs/runtime-checker-c0-authoring-v1/HANDOFF.json'
assert sha(handoff.read_bytes())=='7ad08b0111e149c2adb931f1ddd3181680d6730b2efa238f14195155f8b03ff6'
original(handoff,'rfc-reviewed-handoff')
assert len(load(handoff)['artifacts'])==33
for row in load(handoff)['artifacts']:original(Path(row['path']),'rfc-reviewed-source-artifact',row)
# The actual Sc0 source/proof is the bounded parent dependency. Historical archive parts are not traversed.
sc0=C/'recipe-recovery/current-string-c0/evidence/producer-v1'
assert sha((sc0/'RESULT.json').read_bytes())=='93a0a751404f26418165bcd9287b90287214d8c8f3213ce1baf1c975ccaa3fd6'
assert sha((sc0/'generated-candidate/OUTPUT.json').read_bytes())=='3e9df04fcc1e555dc3ea245cb4dd0a9a1fc6d59635412796c5cfa2e0afd704ac'
assert load(sc0/'RESULT.json')['status']=='PASS_FOCUSED_PRODUCER'
sc0_project=load(sc0/'staging.json');assert len(sc0_project)==20
for row in sc0_project:original(sc0/'project'/row['path'],'accepted-sc0-project-dependency',row)
for p in sorted(sc0.rglob('*')):
 if p.is_file():original(p,'accepted-sc0-proof-dependency')
parent_producer={'result':pin(sc0/'RESULT.json'),'output':pin(sc0/'generated-candidate/OUTPUT.json'),'projectFiles':20,'outputs':5,'requalified':False}
# Prior primary observations remain literal history; they are not an Rf0 receiving result.
for relative_proof,digest,schema in (
 ('receiver-current-c0/primary-observation-v1','78195f460e0c2d58d4736660dab3e3244c16c3b42ac644e065833969ced8e466','m3.ci8891.receiver-capture-result/7-primary'),
 ('receiver-current-c0-followup/primary-observation-v1','fcbf7e041a1465babc525f4a7c9743a5d4f57cb43a1c4bd065d3e90fbdcd1a51','m3.ci8891.receiver-capture-result/8-primary'),
 ('receiver-current-c0-string/primary-observation-v1','6f8d824c7b58f801a3563869d1a34605b36f160a1a04ea4dc6d9dc5455e5e6e8','m3.ci8891.receiver-capture-result/9-primary')):
 prior=C/relative_proof;terminal=load(prior/'RESULT.json')
 assert sha((prior/'RESULT.json').read_bytes())==digest and terminal['schema']==schema and terminal['status']=='PRIMARY_ONLY_STOP'
 for p in sorted(prior.rglob('*')):
  if p.is_file():original(p,'prior-actual-receiving-stop')
# Preserve all three immutable published cohorts as separate dictionaries with a disjoint 507-path union.
parent_maps=[];parent_rows=[]
for relative_map,digest,count in (
 ('publication-design/integration-c0-final-v2/FINAL-PAIRED-FILES.json','bb815cfcf64ac5298fa857355b8caccb5ebda6c76e2803906d8c92ac9578a2a8',338),
 ('publication-design/followup-c0-final-v1/FINAL-PAIRED-FILES.json','5cde38612911bdaf74b1ec4dfb857f7f1f747eccf42f72aa55e5458c48df71f2',97),
 ('publication-design/string-c0-final-v1/FINAL-PAIRED-FILES.json','d7a416f075158f767d5fdba2aa634ae1a43737be25346b493e367c4104aa85e9',72)):
 parent_map=C/relative_map;assert sha(parent_map.read_bytes())==digest
 cohort=load(parent_map)['files'];assert len(cohort)==count
 for row in cohort:
  p=Path(row['file']);b=p.read_bytes();assert sha(b)==row['sha256'] and len(b)==row['bytes'] and blob(b)==row['gitBlob'],p
 parent_rows.extend(cohort);parent_maps.append(pin(parent_map));original(parent_map,'immutable-parent-paired-map')
assert len(parent_rows)==507
for key in ('synexiaPath','m3Path'):assert len({row[key] for row in parent_rows})==507
parent_publication=C/'publication-design/string-c0-final-v1/PUBLISHED.json'
assert sha(parent_publication.read_bytes())=='4146e7b77cc83a7a10ada105975ac7130ddf098e3610b41b56c9b30a277222c0'
publication=load(parent_publication)
parent_heads={'hsoliwal/com.synexia':'b21badee0035a06f75714f16b1b7f02474e2973f','hsoliwal/M3jdk21':'c91ababf8e7f54125be7b58c7267f49233446e45'}
assert publication['schema']=='synexia.sc0.paired-publication/1' and publication['status']=='PUBLISHED_PAIRED_DRAFTS' and publication['totalCheckpointFilesPerRepository']==507
assert {row['repo']:row['head'] for row in publication['records']}==parent_heads
assert all(row['draft'] is True and row['state']=='open' and row['merged'] is False for row in publication['records'])
original(parent_publication,'immutable-parent-publication-receipt')
parent={'pairedMaps':parent_maps,'publicationReceipt':pin(parent_publication),'heads':parent_heads,'files':507,'localBodiesUnchanged':True,'remoteStateRevalidated':False}
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
# Actual focused semantic event, kept distinct from the eventual primary observation.
semantic_binding={'result': {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/evidence/semantic-v2/RESULT.json', 'sha256': 'b5a579eb42c8b306f7ef505e73b04d25a7fb91d5fd117157bc0d16fa47d7df90', 'bytes': 50611}, 'plan': {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/semantic-authoring-v1/SEMANTIC-PLAN-v4.json', 'sha256': '2a39e5803e2e7bfc6fab4bc467cf9f50b3005d79f2aa081e29e7f061b0f4e554', 'bytes': 126297}, 'controller': {'bytes': 25470, 'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/semantic-authoring-v1/rfc_semantic_capture_v4.py', 'sha256': 'bd30b628d4aac3e486fbfb9d646b50c0dd92b742bb2071b2156307fe495f178b'}, 'rootReadback': {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/evidence/semantic-v2/ROOT-READBACK-v1.json', 'sha256': '3226e1540c13cd47ff3bf8ddf386ce5a94d564084d065b183a1f51872bb2081d', 'bytes': 9057}, 'priorStop': {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/evidence/semantic-v1/RESULT.json', 'sha256': '0954ce5327412705d8a44e11feee952d4722aa358889c082016e3cd3a251fc22', 'bytes': 7602}, 'applicability': 'RF0_INTERMEDIATE_STAGE_BEFORE_PHC_FOUNDATION'}
semantic_selection_pin={'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/semantic-selection-v2/SEMANTIC-SELECTION.json', 'sha256': 'fdaf5f4cd76e855f09bacba1c4a5e1efb53b143936f28399211395396d534bff', 'bytes': 83818}
semantic_path=args.semantic_result.resolve();selection_path=args.semantic_inputs.resolve()
assert pin(semantic_path)==semantic_binding['result'] and pin(selection_path)==semantic_selection_pin
semantic_body=load(semantic_path);semantic_plan=load(Path(semantic_binding['plan']['path']))
assert semantic_body['schema']=='synexia.rfc.semantic-result/1' and semantic_body['status']=='PASS_RFC_SYNTAX_PATCH_CONTROLS_RECEIVING_PENDING'
assert semantic_body['failure'] is None and semantic_body['fatalFailure'] is None
semantic_observed={key:value for key,value in semantic_body.items() if isinstance(value,bool)}
assert semantic_observed=={'checkerSemanticallyExecuted': False, 'consumerQualified': False, 'fullM3RuntimeQualified': False, 'fullOpenJdkBuildQualified': False, 'fullReceiverQualified': False, 'jniQualified': False, 'priorReceivingGateQualificationInherited': False, 'productionPromotion': False, 'rfcSyntaxPatchControlsQualified': True, 'systemLoaderAndSharedLibraryClosureComplete': False}
assert semantic_body['plan']==semantic_binding['plan'] and semantic_plan['controller']==semantic_binding['controller']
assert semantic_plan['schema']=='synexia.rfc.semantic-plan/1'
assert semantic_body['producer']['result']==pin(P/'RESULT.json') and semantic_body['producer']['output']==pin(P/'generated-candidate/OUTPUT.json')
semantic_ids=['python-runtime-observation', 'git-loader-observation', 'rfc-syntax-patch-contract']
assert semantic_body['completedCommands']==semantic_ids and semantic_body['unexecutedCommands']==[]
assert len(semantic_body['receipts'])==len(semantic_plan['commands'])==3
assert [row['id'] for row in semantic_plan['commands']]==semantic_ids
semantic_selection=load(selection_path)
assert set(semantic_selection)=={'schema','terminal','files'} and semantic_selection['schema']=='synexia.rfc.semantic-selection/1'
assert semantic_selection['terminal']==semantic_binding['result'] and len(semantic_selection['files'])==251
semantic_paths=[row['path'] for row in semantic_selection['files']]
assert semantic_paths==sorted(set(semantic_paths)) and str(semantic_path) in semantic_paths
compiled_identities=[{'bytes': 42408, 'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/evidence/semantic-v2/observations/checker.pyc', 'sha256': '6cb823adb1624d48009629811560f21764a25983d3b092ad52e3068fadf9721b'}]
assert [row for row in semantic_selection['files'] if Path(row['path']).suffix in ('.class','.pyc')]==compiled_identities
# Exact regular files only. Deliberate negative symlinks remain literal entries in the sealed output/custody JSON.
for row in semantic_selection['files']:
 path=Path(row['path']);assert set(row)=={'path','sha256','bytes'} and path.is_absolute() and path.is_relative_to(C) and '..' not in path.parts
 assert path.is_file() and not path.is_symlink() and pin(path)==row
 original(path,'actual-rfc-semantic-selection-evidence',row)
for row in [semantic_selection_pin,{'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/semantic-selection-v2/BOUNDARY.json', 'sha256': '94d646324e99622c72218b3698b7de495f5646875f8bcb73d409ffb42bc2ba89', 'bytes': 2149},{'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/semantic-selection-v2/SYMLINK-CUSTODY.json', 'sha256': '431cc3afae34ff8a2a48e6c807b88d38da810556e1ba3bc6f1ecf4eecd342e37', 'bytes': 1191}]:original(Path(row['path']),'rfc-semantic-selection-envelope',row)
for key in ('result','plan','controller','rootReadback','priorStop'):
 row=semantic_binding[key];assert row in semantic_selection['files'];original(Path(row['path']),'actual-rfc-semantic-binding',row)
assert load(Path(semantic_binding['priorStop']['path']))['status']=='STOP_RFC_SEMANTICS'
semantic_readback=load(Path(semantic_binding['rootReadback']['path']))
assert semantic_readback['resultSHA256']==semantic_binding['result']['sha256'] and semantic_readback['status']==semantic_body['status'] and semantic_readback['allThreeCommandsPassed'] is True
for row in [semantic_body['admission'],semantic_body['observedOutputs'],*semantic_body['evidence'],*semantic_body['receipts']]:
 assert row in semantic_selection['files'];original(Path(row['path']),'actual-rfc-semantic-evidence',row)
for index,(row,command) in enumerate(zip(semantic_body['receipts'],semantic_plan['commands'])):
 receipt=load(Path(row['path']))
 assert receipt['schema']=='synexia.rfc.semantic-command/1' and receipt['id']==semantic_ids[index]
 assert receipt['argv']==command['argv'] and receipt['cwd']==semantic_plan['cwd'] and receipt['environment']==semantic_plan['environment']
 assert receipt['timeoutSeconds']==command['timeoutSeconds'] and receipt['exitCode']==0 and receipt['executionError'] is None
 assert receipt['priorReceipts']==semantic_body['receipts'][:index] and receipt['planSHA256']==semantic_binding['plan']['sha256']
 for channel in ('stdout','stderr'):
  assert receipt[channel] in semantic_selection['files'];original(Path(receipt[channel]['path']),'actual-rfc-semantic-stream',receipt[channel])
semantic_dispositions=[]
for row in semantic_selection['files']:
 logical='sources/'+Path(row['path']).relative_to(C).as_posix()
 entry={'inputPath':row['path'],'sha256':row['sha256'],'bytes':row['bytes']}
 if row in compiled_identities:
  assert logical not in selected
  assert any(e['inputPath']==row['path'] and e['sha256']==row['sha256'] and e['bytes']==row['bytes'] and e['reason']=='compiled-artifact-hash-only' for e in excluded)
  entry.update(disposition='EXCLUDED_IDENTITY_ONLY',reasons=['compiled-artifact-hash-only'])
 else:
  assert logical in selected and selected[logical]['sha256']==row['sha256'] and selected[logical]['bytes']==row['bytes']
  entry.update(disposition='EXACT_BODY_INCLUDED',logicalPaths=[logical])
 semantic_dispositions.append(entry)
semantic_outcome={'status':'ACTUAL_SEMANTIC_TERMINAL_RETAINED','reportedSchema':semantic_body['schema'],'reportedStatus':semantic_body['status'],'terminal':semantic_binding['result'],'prerequisite':semantic_binding,'observedBooleans':semantic_observed,'receipts':semantic_body['receipts'],'evidence':semantic_body['evidence'],'rootReadback':semantic_binding['rootReadback'],'selectionManifest':semantic_selection_pin,'selectionFiles':251,'selectionDispositions':semantic_dispositions,'compiledIdentityOnlyFiles':len(compiled_identities),'bodyRequiredFiles':250,'qualificationNotInferredByPackaging':True,'consumerQualified':False}
# Bind the exact actual /10 primary STOP, without promoting focused semantic observations.
receiving_terminal_pin={'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-composed/primary-observation-v1/RESULT.json', 'sha256': '38b49d749921719b3773eeb8b10da1d0fc0461c3e483bdd7970c95797ab84ede', 'bytes': 83386}
receiving_selection_pin={'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-composed-preparation/receiving-selection-v1/RECEIVING-SELECTION.json', 'sha256': '10a9e36e98f23afcd25e1e4e6fd76593a7a3cf25782d6f8f26c5de36ac4fcf23', 'bytes': 262988}
terminal=args.receiving_result.resolve();receiving_selection_path=args.receiving_inputs.resolve()
assert pin(terminal)==receiving_terminal_pin and pin(receiving_selection_path)==receiving_selection_pin
terminal_body=load(terminal);receiving_selection=load(receiving_selection_path)
assert terminal_body['schema']=='m3.ci8891.receiver-capture-result/10-primary' and terminal_body['status']=='PRIMARY_ONLY_STOP'
assert terminal_body['observationScope']=='PRIMARY_ONLY' and terminal_body['fatalFailure'] is None
assert all(terminal_body[key]==result[key] for key in ('sourceCommit','targetCommit'))
assert terminal_body['attemptedCommands']==2 and terminal_body['completedCommands']==['dag-install-prerequisite']
assert terminal_body['groups']==[{'attemptedCommands': ['dag-install-prerequisite', 'full-reactor-clean-verify'], 'blockedObservation': None, 'failure': {'message': 'command failed: full-reactor-clean-verify', 'scope': 'COMMAND_OR_OBSERVATION', 'stage': 'full-reactor-clean-verify', 'type': 'RuntimeError'}, 'id': 'primary', 'status': 'STOP', 'unexecutedCommands': ['classver', 'public-javap', 'default-main', 'dgc-javac', 'dgc-java']}, {'id': 'bridge', 'reason': 'Separate primary-only observation epoch; full receiving remains pending', 'status': 'NOT_EXECUTED', 'unexecutedCommands': ['gatherer-current-fixture-javac', 'gatherer-current-docs', 'gatherer-current-guard-lowering', 'gatherer-current-manifest', 'gatherer-current-live-fixed-point', 'gatherer-original-materializer-tests', 'prepared-inputs-regressions', 'tq-sync-generation', 'tq-sync-custody', 'tq-sync-current-regressions', 'lane-generation', 'tq-generation', 'tq-custody', 'tq-current-regressions', 'lane-current-custody', 'lane-current-regressions', 'lane-migration-validation', 'foundation-current-regressions', 'foundation-ci-generation', 'foundation-image-generation', 'foundation-current-custody', 'jep493-workflow-structure', 'pattern-documentation-javac', 'pattern-documentation-contract', 'pattern-metadata-regressions', 'jep485-packet', 'jep485-project']}, {'id': 'legacy-example', 'reason': 'Separate primary-only observation epoch; full receiving remains pending', 'status': 'NOT_EXECUTED', 'unexecutedCommands': ['legacy-example-packet', 'legacy-example-project']}, {'id': 'collection-467', 'reason': 'Separate primary-only observation epoch; full receiving remains pending', 'status': 'NOT_EXECUTED', 'unexecutedCommands': ['jep-467-markdown-validate', 'jep-467-markdown-project']}, {'id': 'collection-493', 'reason': 'Separate primary-only observation epoch; full receiving remains pending', 'status': 'NOT_EXECUTED', 'unexecutedCommands': ['jep-493-runtime-image-validate', 'jep-493-runtime-image-project']}, {'id': 'legacy-jep458', 'reason': 'Separate primary-only observation epoch; full receiving remains pending', 'status': 'NOT_EXECUTED', 'unexecutedCommands': ['jep458-packet', 'jep458-project']}]
receiving_observed={key:value for key,value in terminal_body.items() if isinstance(value,bool)}
assert receiving_observed=={'allAdmittedGroupsQualified': False, 'bridgeGroupQualified': False, 'canonicalProductionApplied': False, 'canonicalPromotionAuthorized': False, 'consumerQualified': False, 'fullOpenJdkBuildQualified': False, 'fullReceiverQualified': False, 'hostedWorkflowExecuted': False, 'jep458BlockedSpecificationObserved': False, 'jep458HostedWorkflowQualified': False, 'jep458ProjectionQualified': False, 'materializedInputsQualified': False, 'positiveCliGroupsQualified': False, 'preparedInputFixedPointObserved': True, 'primaryGroupQualified': False, 'spcSemanticQualified': True, 'strictCanonicalAdmission': False, 'testsCountedOnlyFromActualReports': True}
assert terminal_body['focusedSemanticPrerequisites']['rfc']==semantic_binding
assert terminal_body['focusedSemanticPrerequisites']['completeReceivingQualified'] is False
assert set(receiving_selection)=={'schema','terminal','files'} and receiving_selection['schema']=='synexia.ph0.receiving-selection/1'
assert receiving_selection['terminal']==receiving_terminal_pin and len(receiving_selection['files'])==804
receiving_paths=[row['path'] for row in receiving_selection['files']]
assert receiving_paths==sorted(set(receiving_paths)) and str(terminal) in receiving_paths
assert all(row in receiving_selection['files'] for row in semantic_selection['files'])
receiving_compiled_identities=[{'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-string/spc-semantic-v1/fixture-classes/StringEscapeContract$Checked.class', 'sha256': '3732a0efd457afe4d1acee1e31b1d8ff5ea1edcd93f0d8946de08189cfe0b849', 'bytes': 283}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-string/spc-semantic-v1/fixture-classes/StringEscapeContract$ChooseEscape.class', 'sha256': '1083f20908329a49c5d628feb80bb0ddb83f78ce1de3787c55846b08c0733aad', 'bytes': 778}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-string/spc-semantic-v1/fixture-classes/StringEscapeContract.class', 'sha256': '5266e29fffb5f29b2531bad5a6bf3de6a25ed44f1f65cb1c01caa3d028ee2b31', 'bytes': 12621}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-string/spc-semantic-v1/java-observations/after-current/classes/M3StringPrecomputeSearchTest.class', 'sha256': '65e560f5b076031153e0b1a504ce593221f522a23324ab814f551de8f7a5c0ef', 'bytes': 31545}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-string/spc-semantic-v1/java-observations/after-template/classes/M3StringPrecomputeSearchTest.class', 'sha256': '65e560f5b076031153e0b1a504ce593221f522a23324ab814f551de8f7a5c0ef', 'bytes': 31545}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-string/spc-semantic-v1/java-observations/negative-wrong-atom/classes/M3StringPrecomputeSearchTest.class', 'sha256': 'c148f36f2e5efa177cd7c4ee144b90521beb1bd4b08771825507fb86b4df2733', 'bytes': 31537}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v1/fixture-classes/PatternHiddenCommentContract$1.class', 'sha256': '43efae5b03da47e857efbda21f8ff419a638aa5db8aed2edb8c6b690ae055e00', 'bytes': 845}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v1/fixture-classes/PatternHiddenCommentContract$2$1.class', 'sha256': 'd1ba121a72e49157666931692f142df6fa078fc51fb9a09093f375cf74df6d65', 'bytes': 1059}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v1/fixture-classes/PatternHiddenCommentContract$2.class', 'sha256': 'd11f34e138ac04c3ef3668cbf866b14f7f0183edbbf63475f5cf464570067c5d', 'bytes': 9319}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v1/fixture-classes/PatternHiddenCommentContract$Checked.class', 'sha256': 'bba59eb762ba1a4cc1416a1e544c03a0848a803d77d5ad6f8b20584dcdb9519f', 'bytes': 381}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v1/fixture-classes/PatternHiddenCommentContract$Edit.class', 'sha256': 'a37eebd7dba57b746546715df74f064acef0a17f9262fb6e7f3aae38ecd083fe', 'bytes': 1608}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v1/fixture-classes/PatternHiddenCommentContract$Parsed.class', 'sha256': '360c2c1b1a2aaed2569a1c9e8f30d46f64224b518e0f166386a47875fdb027df', 'bytes': 2884}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v1/fixture-classes/PatternHiddenCommentContract$Reference.class', 'sha256': '9c7c198ca318934cebe39797cbbcd2caf79482594002f60baadd186277653502', 'bytes': 1546}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v1/fixture-classes/PatternHiddenCommentContract.class', 'sha256': 'd7d840c90efc8eb810124e11246a001a90c34221ae973615539a9c4e61a7cefb', 'bytes': 18011}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v2/fixture-classes/PatternHiddenCommentContract$1.class', 'sha256': '43efae5b03da47e857efbda21f8ff419a638aa5db8aed2edb8c6b690ae055e00', 'bytes': 845}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v2/fixture-classes/PatternHiddenCommentContract$2$1.class', 'sha256': 'd1ba121a72e49157666931692f142df6fa078fc51fb9a09093f375cf74df6d65', 'bytes': 1059}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v2/fixture-classes/PatternHiddenCommentContract$2.class', 'sha256': 'd11f34e138ac04c3ef3668cbf866b14f7f0183edbbf63475f5cf464570067c5d', 'bytes': 9319}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v2/fixture-classes/PatternHiddenCommentContract$Checked.class', 'sha256': 'bba59eb762ba1a4cc1416a1e544c03a0848a803d77d5ad6f8b20584dcdb9519f', 'bytes': 381}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v2/fixture-classes/PatternHiddenCommentContract$Edit.class', 'sha256': 'a37eebd7dba57b746546715df74f064acef0a17f9262fb6e7f3aae38ecd083fe', 'bytes': 1608}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v2/fixture-classes/PatternHiddenCommentContract$Parsed.class', 'sha256': '360c2c1b1a2aaed2569a1c9e8f30d46f64224b518e0f166386a47875fdb027df', 'bytes': 2884}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v2/fixture-classes/PatternHiddenCommentContract$Reference.class', 'sha256': '9c7c198ca318934cebe39797cbbcd2caf79482594002f60baadd186277653502', 'bytes': 1546}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-pattern-hidden-c0/evidence/phc-semantic-v2/fixture-classes/PatternHiddenCommentContract.class', 'sha256': 'd7d840c90efc8eb810124e11246a001a90c34221ae973615539a9c4e61a7cefb', 'bytes': 18011}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/recipe-recovery/current-runtime-checker-c0/evidence/semantic-v2/observations/checker.pyc', 'sha256': '6cb823adb1624d48009629811560f21764a25983d3b092ad52e3068fadf9721b', 'bytes': 42408}]
assert [row for row in receiving_selection['files'] if Path(row['path']).suffix in ('.class','.pyc')]==receiving_compiled_identities
for row in receiving_selection['files']:
 path=Path(row['path']);assert set(row)=={'path','sha256','bytes'} and path.is_absolute() and path.is_relative_to(C) and '..' not in path.parts
 assert path.is_file() and not path.is_symlink() and pin(path)==row
 original(path,'actual-composed-receiving-evidence',row)
original(receiving_selection_path,'composed-receiving-selection',receiving_selection_pin)
for row in [{'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-composed-preparation/root-receiving-selection-v1/COMMAND.json', 'sha256': 'ce830fd7d46e5d391afed8668f96d26fa0dbb4c9865d019c9c03e711b6532374', 'bytes': 1816}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-composed-preparation/root-receiving-selection-v1/RESULT.json', 'sha256': 'b75f3963d269b6baa3c7ef6e516efc57adefbc6ada4fe5638227809c6112a2a6', 'bytes': 874}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-composed-preparation/root-receiving-selection-v1/stderr.log', 'sha256': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'bytes': 0}, {'path': '/workspace/scratch/5809f5dce3dd/continuation-20261006/receiver-current-c0-composed-preparation/root-receiving-selection-v1/stdout.log', 'sha256': '9ca8060e6d9bcbe70663c6aa3e2e734a51384e6f44816d164c0eb3804f486aed', 'bytes': 288}]:original(Path(row['path']),'actual-root-receiving-selection-execution',row)
materialization=[]
for key in ('materializationCommandReceipt','materializationReceipt'):
 row=terminal_body[key];assert row in receiving_selection['files'];materialization.append({'field':key,**row})
for row in terminal_body['receipts']+terminal_body['evidence']:assert row in receiving_selection['files']
receiving_dispositions=[]
for row in receiving_selection['files']:
 logical='sources/'+Path(row['path']).relative_to(C).as_posix()
 entry={'inputPath':row['path'],'sha256':row['sha256'],'bytes':row['bytes']}
 if row in receiving_compiled_identities:
  assert logical not in selected
  assert any(e['inputPath']==row['path'] and e['sha256']==row['sha256'] and e['bytes']==row['bytes'] and e['reason']=='compiled-artifact-hash-only' for e in excluded)
  entry.update(disposition='EXCLUDED_IDENTITY_ONLY',reasons=['compiled-artifact-hash-only'])
 else:
  assert logical in selected and selected[logical]['sha256']==row['sha256'] and selected[logical]['bytes']==row['bytes']
  entry.update(disposition='EXACT_BODY_INCLUDED',logicalPaths=[logical])
 receiving_dispositions.append(entry)
receiving_outcome={'status':'ACTUAL_TERMINAL_RETAINED','reportedSchema':terminal_body['schema'],'reportedStatus':terminal_body['status'],'observationScope':terminal_body['observationScope'],'observedBooleans':receiving_observed,'terminal':receiving_terminal_pin,'selectionManifest':receiving_selection_pin,'files':receiving_selection['files'],'selectionDispositions':receiving_dispositions,'compiledIdentityOnlyFiles':23,'bodyRequiredFiles':781,'materializationEvidence':materialization,'spcSemanticPrerequisite':terminal_body['spcSemanticPrerequisite'],'spcHistoricalContext':terminal_body['spcHistoricalContext'],'focusedSemanticPrerequisites':terminal_body['focusedSemanticPrerequisites'],'groups':terminal_body['groups'],'attemptedCommands':2,'completedCommands':['dag-install-prerequisite'],'consumerQualified':False,'qualificationNotInferredByPackaging':True}
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
record={'schema':'synexia.rf0.package-source-closure/1','producer':{'result':pin(P/'RESULT.json'),'output':pin(P/'generated-candidate/OUTPUT.json')},'bodyMappings':sorted(mappings,key=lambda r:r['logicalPath']),'excludedIdentities':excluded,'sealedInputDispositions':closure,'compiledArtifactBodiesIncluded':False,'toolchainBodiesIncluded':False,'hermetic':False,'historicalMissingProofsRecreated':False,'consumerQualified':False,'archiveOwner':pin(OWNER),'parentPublication':parent,'parentProducer':parent_producer,'publicationCheckHistory':publication_check_history,'semanticOutcome':semantic_outcome,'receivingOutcome':receiving_outcome}
write(S/'SOURCE-CLOSURE.json',record)
# Empty LANES is deliberate: actual terminal evidence is explicitly selected and independently hash-closed above.
plan={'schema':'synexia.evidence.package-plan/1','status':'FINAL_REVIEWED_SELECTION','sourceCommit':result['sourceCommit'],'targetCommit':result['targetCommit'],'packageOwnerSHA256':OWNER_SHA,'resultSHA256':sha((P/'RESULT.json').read_bytes()),'laneSelection':{'required':[],'additional':[],'allowNotExecuted':[],'includeAllExisting':False},'includePaths':sorted([*selected,'SOURCE-CLOSURE.json']),'selectionMode':'EXPLICIT_FINISHED_BODIES_WITH_SOURCE_CLOSURE_AUDIT','noAutomaticLaneQualification':True}
write(HERE/'package-plan.json',plan)
write(HERE/'SOURCE-CLOSURE.json',record)
print(json.dumps({'plan':pin(HERE/'package-plan.json'),'closure':pin(HERE/'SOURCE-CLOSURE.json'),'bodyCount':len(selected)+1,'projectFiles':16,'actualResult':pin(P/'RESULT.json')}))
