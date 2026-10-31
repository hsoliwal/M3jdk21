"""Independent read-only resource inspection; no candidate imports or execution."""
from pathlib import Path
import ast, collections, copy, csv, hashlib, io, json, re
B=Path('/workspace/scratch/1c68df1bae79/javac-convergence-20261005/work/m3jdk21-current/successor-e04');R=B/'overlay';O=B/'derivation-static-review/resources-v3';S=O/'inputs'
SOURCE='0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8';ROOT='0361e4a07b77a01df170523033fef01c4052ddc5';DEST='0994ecd65e86600f417f4d8702838d8c2663af61';DTREE='7e8f39ac999174f8d5b6028d638a68d8c317b070'
LAB='synexia.jcc-recipe-laboratory';JNI='synexia.jcc-java-jni-regression';IDS={LAB,JNI};PREFIX='m3/tooling/migration-recipes';TASK=PREFIX+'/tasks/jcc-source-recovery-handoff-20261005/source-recovery/';CRATE=R/PREFIX/'src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-recovery-handoff-20261005'
MAP='m3/docs/name-mapping.json';DOC='m3/docs/jcc-source-handoff.md';BIND='m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json';COV='m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv'
def load(p):return json.loads(Path(p).read_text())
def identity(p):
 p=Path(p);assert p.is_file() and not p.is_symlink(),p;d=p.read_bytes();return {'bytes':len(d),'sha256':hashlib.sha256(d).hexdigest(),'git_blob_sha1':hashlib.sha1(f'blob {len(d)}\0'.encode()+d).hexdigest(),'mode':'100755' if p.stat().st_mode&0o111 else '100644'}
def check(p,r):
 got=identity(p)
 for k in ('bytes','sha256','git_blob_sha1','mode'):
  if k in r:assert got[k]==r[k],(str(p),k)
 return got
snapshots=[]
def snapshot(p,name=None):
 p=Path(p);q=S/(name or p.name);d=p.read_bytes()
 if q.exists():assert q.read_bytes()==d,('Review input drift',str(p))
 else:q.write_bytes(d)
 snapshots.append({'source':str(p),'snapshot':str(q.relative_to(O)),**identity(p)})
 return load(p) if p.suffix=='.json' else d
packet=snapshot(B/'bound-inputs-v3/BOUND_RESOURCES_REVIEW_PACKET.json');assert identity(B/'bound-inputs-v3/BOUND_RESOURCES_REVIEW_PACKET.json')['sha256']=='9024a1180d4f5856130d9e3a7dd1e8f18786ca8825c9569397d37003019aac9a'
f=snapshot(B/'FINAL_SOURCE_INPUTS_V3.json');assert identity(B/'FINAL_SOURCE_INPUTS_V3.json')['sha256']=='df6713ad19ce80daad0383fbd5c3716412e2dca133e1db7e0150b88e141313b5';author=snapshot(B/'AUTHORING_RESULT_V3.json');plan=snapshot(packet['plan']['local_path']);snapshot(packet['manifest']['local_path']);check(packet['plan']['local_path'],packet['plan']);check(packet['manifest']['local_path'],packet['manifest']);check(packet['authoring']['local_path'],packet['authoring']);check(packet['bound_input']['local_path'],packet['bound_input'])
assert packet['receiving_execution'] is False and packet['operational_outputs_unchanged'] is True and packet['source_export_admitted'] is False
assert author['state']=='BOUND_RESOURCES_PENDING_EXECUTION' and author['recipe_execution']=='NOT_EXECUTED' and author['source_export_admitted'] is False
assert (author['source_commit'],author['source_root_tree'],author['destination_preimage_commit'])==(SOURCE,ROOT,DEST)
assert plan['schema']=='m3.sealed-install/1' and plan['recipe_id']=='jcc-source-recovery-handoff-20261005/1' and (plan['source_commit'],plan['target_commit'])==(SOURCE,DEST)
canonical=copy.deepcopy(plan);seal=canonical.pop('plan_sha256');assert hashlib.sha256((json.dumps(canonical,sort_keys=True,separators=(',',':'),ensure_ascii=True)+'\n').encode()).hexdigest()==seal==packet['plan_seal']==author['plan_sha256']=='0aa1f4a3e080c36d233066f0a6e1dc8e1047e6f0592dba60829b8cca3177df64'
outputs={r['path']:r for r in packet['outputs']};assert len(outputs)==4 and set(outputs)=={MAP,DOC,BIND,COV};assert [x['path'] for x in plan['outputs']]==sorted(outputs)
for p,x in outputs.items():
 for state in ('before','after'):check(x[state]['local_path'],x[state]);snapshot(x[state]['local_path'])
 assert x['operational_still_before'] is True and (R/p).read_bytes()==Path(x['before']['local_path']).read_bytes() and x['before']['sha256']!=x['after']['sha256']
 row=next(row for row in plan['outputs'] if row['path']==p)
 for state in ('before','after'):assert row[state]=={'resource':Path(x[state]['local_path']).name,'sha256':x[state]['sha256']}
assert Path(outputs[DOC]['after']['local_path']).read_bytes()==Path(f['handoff_document']['local_path']).read_bytes()
manifest=list(csv.reader(Path(packet['manifest']['local_path']).read_text().splitlines(),delimiter='\t'));assert manifest==[[r['path'],r['before']['sha256'],r['after']['sha256'],r['after']['resource']] for r in plan['outputs']]
oldplan=load(CRATE.parent/'jcc-source-final-handoff-20261005/plan.json');assert plan['guards']==oldplan['guards'] and len(plan['guards'])==7
for r in packet['guards']:check(r['local_path'],r);assert {'path':r['path'],'sha256':r['sha256']} in plan['guards']
preimages=load(B.parent/'successor-e04-prep/baseline/PREIMAGES.json');assert preimages['destination_commit']==DEST and preimages['root_tree']==DTREE
for r in preimages['outputs']:check(outputs[r['path']]['before']['local_path'],r)
e3path=B.parent/'successor-e03/PUBLICATION_MANIFEST.json';e3=load(e3path);oldrefs={r['path']:r for r in e3['files']};assert len(oldrefs)==93
for p,r in oldrefs.items():check(R/p,r)
assert {r['path'] for r in packet['prior_e3_preserved_payload_files']}==set(oldrefs)-set(outputs) and len(packet['prior_e3_preserved_payload_files'])==89
for r in packet['prior_e3_preserved_payload_files']:check(r['local_path'],r);assert {k:r[k] for k in ('bytes','sha256','git_blob_sha1','mode')}=={k:oldrefs[r['path']][k] for k in ('bytes','sha256','git_blob_sha1','mode')}
inv=load(f['source_inventory']['local_path']);acct=load(f['root_accounting']['local_path']);context=load(B/'current-context/CONTEXT_REFERENCES.json')
before=load(outputs[MAP]['before']['local_path']);after=load(outputs[MAP]['after']['local_path']);a=copy.deepcopy(before);z=copy.deepcopy(after);oldrows=a['migration'].pop('records');newrows=z['migration'].pop('records');assert a==z and len(oldrows)==len(newrows)==46
assert [r['id'] for r in oldrows]==[r['id'] for r in newrows] and len({r['id'] for r in newrows})==46 and len(after['migration']['gates'])==20
keys=('repo','commit','module','path','symbol','signatures','sha256','git_blob_sha1','fingerprint','revision_role','tracking_ref')
def artifact(r):return {k:r[k] for k in keys}
oldsource_count=0;history_count=0;current_count=0;unchanged=0
for old,new in zip(oldrows,newrows):
 rid=old['id']
 if rid not in IDS:assert old==new;unchanged+=1;continue
 allowed={'observation','lineage','sources','reason','sync','recipe','materialization'}
 assert {k:old[k] for k in old if k not in allowed}=={k:new[k] for k in new if k not in allowed}
 assert new['status']=='blocked' and new['tests']==[] and new['targets']==old['targets']
 assert new['sources']==[artifact(r) for r in inv['sources'] if r['record']==rid];current_count+=len(new['sources'])
 assert new['lineage']['previous_sources']==old['lineage']['previous_sources']+old['sources'];history_count+=len(new['lineage']['previous_sources']);oldsource_count+=len(old['sources'])
 assert {k:v for k,v in new['lineage'].items() if k!='previous_sources'}=={k:v for k,v in old['lineage'].items() if k!='previous_sources'}
 for k,v in new['sync'].items():
  expected={'source_revision':SOURCE,'target_revision':DEST,'pending':f['pending_obligations']}.get(k,old['sync'].get(k));assert v==expected,(rid,'sync',k)
 assert new['observation']==f['record_observations'][rid] and new['reason']==f['record_reasons'][rid]
 for k,v in new['materialization'].items():assert v==f['record_materialization'][rid].get(k,old['materialization'].get(k))
 assert {k:v for k,v in new['recipe'].items() if k not in ('id','preconditions')}=={k:v for k,v in old['recipe'].items() if k not in ('id','preconditions')}
 assert new['recipe']['id']=='com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe/jcc-source-recovery-handoff-20261005'
 assert DEST in new['recipe']['preconditions'][0] and outputs[MAP]['before']['sha256'] in new['recipe']['preconditions'][0] and SOURCE in new['recipe']['preconditions'][2]
assert unchanged==44 and oldsource_count==17 and history_count==21 and current_count==19
oldbind=load(outputs[BIND]['before']['local_path']);binding=load(outputs[BIND]['after']['local_path'])
changed={'destination_root_tree','source_acquisition_scope','source_root_tree','source_execution','source_current_context','source_commit','source_artifacts','source_publication','source_root_receipt_sha256','source_acquisition_receipt_sha256','prior_handoff','source_root_accounting','canonical_map_preimage_sha256','destination_commit'}
assert {k:v for k,v in binding.items() if k not in changed}=={k:v for k,v in oldbind.items() if k not in changed}
assert binding['source_artifacts']==[artifact(r) for r in inv['sources']] and binding['source_execution']==inv['source_execution']
assert binding['source_commit']==SOURCE and binding['source_root_tree']==ROOT and binding['destination_commit']==DEST and binding['destination_root_tree']==DTREE
assert binding['canonical_map_preimage_sha256']==outputs[MAP]['before']['sha256'] and binding['source_acquisition_receipt_sha256']==f['source_inventory']['sha256'] and binding['source_root_receipt_sha256']==f['root_accounting']['sha256']
assert binding['acceptance']=={'source_export_admitted':False,'destination_materialized':False,'destination_gates_passed':False,'remote_read_back_delivered':False}
pubcopy=copy.deepcopy(binding['source_publication']);copies=pubcopy.pop('receipts');assert pubcopy==inv['source_publication'];assert copies==author['evidence']
refs={name:f[key] for name,key in [('PUBLICATION_CUSTODY.json','publication_receipt'),('SOURCE_FIELDS_AND_PROOFS.json','source_inventory'),('ROOT_ACCOUNTING.json','root_accounting')]};refs.update(f['additional_receipts']);assert len(refs)==len(copies)==134 and set(copies)==set(refs)
for name,r in refs.items():
 copyref=copies[name];assert copyref['path']==TASK+name;check(R/copyref['path'],r);check(R/copyref['path'],copyref);assert (R/copyref['path']).read_bytes()==Path(r['local_path']).read_bytes()
ctx=binding['source_current_context'];assert ctx['input_commit']=='be92c62ece9023b5c33676716a1076d00e26120a' and ctx['input_root_tree']=='3c4f32b66633a251ba2c117090830252a7e2da03'
for key in ('receipts','explicit_context_artifacts','current_parent_test_classes'):assert ctx[key]==context[key]
for key,value in {'designated_binding_count':19,'original_source_packet_count':503,'current_context_count':527,'current_main_explicit_count':446,'current_parent_count':109,'current_parent_main':88,'current_parent_release8_vendor':19}.items():assert ctx[key]==value
for r in ctx['receipts'].values():check(R/r['path'],r)
prior=binding['prior_handoff'];assert prior['commit']==DEST and prior['root_tree']==DTREE and prior['old_source_commit']==oldbind['source_commit'] and prior['old_source_root']==oldbind['source_root_tree'];assert (R/prior['old_binding_resource']).read_bytes()==Path(outputs[BIND]['before']['local_path']).read_bytes();assert prior['old_binding_sha256']==outputs[BIND]['before']['sha256']
coverage=list(csv.DictReader(io.StringIO(Path(outputs[COV]['after']['local_path']).read_text()),delimiter='\t'));roots={e['path']:e for e in acct['root_entries']};assert len(coverage)==343 and [r['root_path'] for r in coverage]==[r['path'] for r in acct['root_entries']]
for r in coverage:
 e=roots[r['root_path']];assert (r['source_commit'],r['source_tree'],r['object_type'],r['mode'],r['object_id'])==(SOURCE,ROOT,e['type'],e['mode'],e['sha'])
 declarations=[d for d in acct['maven_declarations'] if d['path'].split('/')[0]==r['root_path']]
 assert r['root_pom_declarations']==';'.join(d['path'] for d in declarations) and r['declaration_contexts']==';'.join(d['context'] for d in declarations)
 for k in ('export_admitted','destination_materialized','read_back_delivered'):assert r[k]=='false'
 assert r['source_tested']=='NOT_ESTABLISHED_FOR_ROOT_UNIT' and r['destination_gates']=='NOT_RUN_FOR_ROOT_UNIT'
ra=binding['source_root_accounting'];assert (ra['root_entries'],ra['root_trees'],ra['root_blobs'],ra['root_gitlinks'],ra['root_pom_module_declarations'],ra['distinct_module_paths'])==(343,255,87,1,201,201)
assert (ra['direct_root_modules'],ra['nested_module_paths'],ra['root_trees_with_nested_modules'],ra['nested_containers_also_direct_modules'],ra['distinct_root_trees_referenced'])==(190,11,8,4,194)
assert ra['root_pom_sha256']==acct['root_pom']['sha256'] and ra['root_pom_git_blob']==acct['root_pom']['git_blob_sha1'] and ra['coverage_receipt_path']==COV and ra['coverage_receipt_sha256']==outputs[COV]['after']['sha256'] and ra['whole_repository_file_count'] is None and ra['semantic_dependency_closure_complete'] is False
for r in packet['entrypoint_test_and_proof_owners']:check(r['local_path'],r);snapshot(r['local_path'],Path(r['local_path']).name if Path(r['local_path']).name!='pom.xml' else 'verification-pom.xml')
java=Path(packet['entrypoint_test_and_proof_owners'][0]['local_path']).read_text();prepared=(B/'derivation-static-review/inputs/JccSourceRecoveryHandoffTest.java').read_text()
for a,z in [('UNBOUND_RECOVERY_SOURCE_COMMIT',SOURCE),('UNBOUND_RECOVERY_SOURCE_ROOT_TREE',ROOT),('UNBOUND_RECOVERY_SOURCE_REF',f['source_ref']),('EXPECTED_RECOVERY_SOURCE_COUNT = -1','EXPECTED_RECOVERY_SOURCE_COUNT = 19')]:assert prepared.count(a)==1;prepared=prepared.replace(a,z)
assert java==prepared and len(re.findall(r'^    @Test$',java,re.M))==12
python_path=R/'m3/migration/test/test_jcc_source_recovery_handoff_packet.py';assert python_path.read_bytes()==(B/'derivation-static-review/inputs/test_jcc_source_recovery_handoff_packet.py').read_bytes();py_ast=ast.parse(python_path.read_text());methods=[n.name for cls in py_ast.body if isinstance(cls,ast.ClassDef) for n in cls.body if isinstance(n,ast.FunctionDef) and n.name.startswith('test_')];assert len(methods)==6
assert identity(B/'run_verification_v3.py')['sha256']=='496e1877914e15f4342111c37020a2d58790038516b62872680427e585b21f50'
execution=snapshot(B/'authoring-invocation-v3/EXECUTION.json','AUTHORING_EXECUTION.json')
assert execution['exit_code']==0
for k in ('input','script','stdout','stderr'):check(execution[k]['local_path'],execution[k])
for k in ('stdout','stderr'):snapshot(execution[k]['local_path'],'authoring-'+k+'.log')
assert json.loads(Path(execution['stdout']['local_path']).read_text())['plan_sha256']==seal and execution['stderr']['bytes']==0
prep=load(B.parent/'successor-e04-prep/PREPARATION_MANIFEST.json');preprefs={r['path']:r for r in prep['files']}
for r in packet['entrypoint_test_and_proof_owners'][1:]:check(r['local_path'],preprefs['staging/'+Path(r['local_path']).relative_to(R).as_posix()])
result={'schema':'jcc-e4-authored-resource-independent-checks/1','source_commit':SOURCE,'source_root_tree':ROOT,'destination_preimage_commit':DEST,'plan_seal':seal,'four_operational_targets_still_before':True,'generated_outputs':{p:outputs[p]['after'] for p in sorted(outputs)},'seven_original_guards_exact':True,'e3_payload_files_preserved':93,'nonoperational_e3_payload_files_preserved':89,'ordered_mapping_records':46,'unaffected_whole_records':44,'destination_gates_unchanged':20,'historical_source_objects_exact':21,'previous_active_sources_retained':17,'current_designated_objects':19,'current_proofs_bound':55,'source_recovery_copies_exact':134,'complete_current_context_receipts_exact':len(ctx['receipts']),'root_coverage_rows':343,'root_module_declarations':201,'source_execution_custody_and_failed_flags_equal_admitted_input':True,'bound_java_only_four_reviewed_substitutions':True,'java_test_methods':12,'python_test_methods':6,'candidate_modules_imported_or_executed':False,'receiving_execution_performed':False,'remote_actions':False,'snapshots':snapshots}
(O/'RESOURCE_CHECKS.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('snapshots','generated_outputs')},indent=2))
