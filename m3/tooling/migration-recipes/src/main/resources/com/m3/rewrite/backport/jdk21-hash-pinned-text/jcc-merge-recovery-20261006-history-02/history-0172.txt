"""Work-only E4 resource generator, fail closed until real source publication.

Uses canonical map/recipe owners; never edits the four operational target files.
FINAL_SOURCE_INPUTS_V3.json is a reviewed local handoff input, not a registry.
"""
from __future__ import annotations
import copy,csv,hashlib,io,json,re
from pathlib import Path
from publication_custody_v3 import admit_publication_custody
B=Path(__file__).resolve().parent;R=B/'overlay';P=B.parent/'successor-e04-prep'
NAME='jcc-source-recovery-handoff-20261005';PREFIX=Path('m3/tooling/migration-recipes');TASK=PREFIX/'tasks'/NAME
C=R/PREFIX/'src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text'/NAME
OLD=C.parent/'jcc-source-final-handoff-20261005'
DEST='0994ecd65e86600f417f4d8702838d8c2663af61';DTREE='7e8f39ac999174f8d5b6028d638a68d8c317b070'
BASELINE='be92c62ece9023b5c33676716a1076d00e26120a';BTREE='3c4f32b66633a251ba2c117090830252a7e2da03';D1='d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906'
LAB='synexia.jcc-recipe-laboratory';JNI='synexia.jcc-java-jni-regression';IDS=(LAB,JNI)
MAP='m3/docs/name-mapping.json';DOC='m3/docs/jcc-source-handoff.md';BIND='m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json';COV='m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv'
def read(p):return json.loads(p.read_text())
def enc(x):return (json.dumps(x,indent=2,ensure_ascii=False)+'\n').encode()
def sha(d):return hashlib.sha256(d).hexdigest()
def ident(p):
 assert p.is_file() and not p.is_symlink(),p
 d=p.read_bytes();return {'bytes':len(d),'sha256':sha(d),'git_blob_sha1':hashlib.sha1(f'blob {len(d)}\0'.encode()+d).hexdigest()}
def checked_ref(ref):
 p=Path(ref['local_path']);a=ident(p)
 for k in ('bytes','sha256','git_blob_sha1'):
  if k in ref:assert a[k]==ref[k],(p,k)
 assert 'sha256' in ref,'Every input receipt must be hash-bound'
 return p

def artifact(row,source,ref):
 checked_ref(row)
 assert row['commit']==source and row['tracking_ref']==ref and row['repo']=='hsoliwal/com.synexia'
 return {k:copy.deepcopy(row[k]) for k in ('repo','commit','module','path','symbol','signatures','sha256','git_blob_sha1','fingerprint','revision_role','tracking_ref')}

def admit_receipt_targets(f,require_absent=True):
 reserved={'PUBLICATION_CUSTODY.json':f['publication_receipt'],'SOURCE_FIELDS_AND_PROOFS.json':f['source_inventory'],'ROOT_ACCOUNTING.json':f['root_accounting']}
 extras=f['additional_receipts'];assert isinstance(extras,dict)
 for name in extras:
  assert isinstance(name,str) and name not in ('','.','..') and '\\' not in name
  relative=Path(name)
  assert not relative.is_absolute() and relative.as_posix()==name
  assert all(part not in ('','.','..') for part in relative.parts)
 assert 'PUBLICATION_READBACK.json' not in extras,'V3 custody must not be labeled universal raw readback'
 assert not set(reserved)&set(extras),'Additional receipt replaces a reserved name'
 receipts={**reserved,**extras};destinations=[]
 for name in receipts:
  destination=R/TASK/'source-recovery'/name
  for parent in destination.parents:
   assert not parent.is_symlink(),('Symlink receipt parent',parent)
   assert not parent.exists() or parent.is_dir(),('Receipt parent is not a directory',parent)
  assert not destination.is_symlink(),destination
  if require_absent:assert not destination.exists(),destination
  else:
   assert destination.is_file(),destination
   assert ident(destination)==ident(checked_ref(receipts[name])),('Authored receipt drift',destination)
  destinations.append(destination)
 assert len(destinations)==len(set(destinations))
 for index,left in enumerate(destinations):
  for right in destinations[index+1:]:
   assert left not in right.parents and right not in left.parents,('Receipt file/ancestor conflict',left,right)
 return receipts

def validate_inputs(f,receipts_absent=False):
 if not __debug__:raise RuntimeError('Optimized Python disables required E4 input admission assertions')
 # No writes precede complete admission of real published identity and local proofs.
 assert f['state']=='PUBLISHED_CUSTODY_VERIFIED','Recovery publication has not been bound'
 source=f['source_commit'];root=f['source_root_tree'];ref=f['source_ref']
 assert isinstance(source,str) and re.fullmatch('[0-9a-f]{40}',source) and source not in (BASELINE,D1,'0'*40)
 assert isinstance(root,str) and re.fullmatch('[0-9a-f]{40}',root)
 assert isinstance(ref,str) and ref.startswith('refs/heads/') and ref!='refs/heads/develop'
 publication=read(checked_ref(f['publication_receipt']))
 assert publication['schema']=='jcc-source-recovery-publication/1' and publication['status']=='PUBLISHED_CUSTODY_VERIFIED'
 custody=admit_publication_custody(publication)
 assert f['source_publication_custody']==custody
 assert publication['repository']=='hsoliwal/com.synexia' and publication['parent']==BASELINE
 assert publication['branch']=='aix/jcc-source-merge-recovery-20261005' and ref=='refs/heads/'+publication['branch']
 assert publication['pull_request']['url']==f['source_pr']
 assert publication['commit']==source and publication.get('tree',publication.get('root_tree'))==root
 assert any(publication.get(k) is True for k in ('commit_parent_tree_and_ref_verified','branch_commit_parent_tree_pr_verified')),'Actual publication readback verification required'
 inventory=read(checked_ref(f['source_inventory']))
 assert inventory['source_commit']==source and inventory['source_root_tree']==root
 assert inventory['source_publication']['custody']==custody and inventory['source_publication']['publication_status']==publication['status']
 sources=inventory['sources'];assert len(sources)==19
 context_path=B/'current-context/CONTEXT_REFERENCES.json'
 assert ident(context_path)['sha256']=='fbc460874aeb04a688ad0ed9b47ad8e3f3b37f88af09d0f32ecfeedc2827fbed'
 baseline=read(context_path);expected={x['path']:x for x in baseline['designated_sources']}
 assert set(expected)=={x['path'] for x in sources} and len({x['path'] for x in sources})==19
 for row in sources:
  old=expected[row['path']];assert row['record']==old['record'] and row['symbol']==old['symbol'] and row['module']==old['module']
  assert row['revision_role']=='pinned';artifact(row,source,ref)
 proofs=inventory['proof_references'];assert proofs
 for row in proofs:
  assert row['commit']==source;checked_ref(row)
 execution=inventory['source_execution']
 assert execution['input_remote_revision']==BASELINE and execution['input_remote_root_tree']==BTREE and execution['published_output_revision']==source
 assert execution['source_export_admitted'] is False and execution['destination_gates_passed'] is False
 assert execution['proofs']==[{k:row[k] for k in ('path','commit','bytes','sha256','git_blob_sha1')}|{'repo':'hsoliwal/com.synexia','url':f'https://github.com/hsoliwal/com.synexia/blob/{source}/{row["path"]}'} for row in proofs]
 rootacct=read(checked_ref(f['root_accounting']))
 assert rootacct['final_commit']==source and rootacct['final_root_tree']==root
 assert len({x['path'] for x in rootacct['root_entries']})==len(rootacct['root_entries'])
 document=checked_ref(f['handoff_document']).read_text()
 assert not re.search(r'\{\{[^{}]+\}\}',document),'Final handoff prose still contains unbound template tokens'
 assert source in document and root in document and f['source_pr'] in document
 assert f['pending_obligations'] and all(isinstance(x,str) and x for x in f['pending_obligations'])
 assert f['source_qualification_reviewed'] is True and f['promote_capability'] is False
 admit_receipt_targets(f,require_absent=receipts_absent)
 for rid in IDS:
  assert isinstance(f['record_observations'][rid],str) and f['record_observations'][rid].strip()
  assert isinstance(f['record_reasons'][rid],str) and f['record_reasons'][rid].strip()
  for key in ('outputs','cached_facts'):
   assert isinstance(f['record_materialization'][rid][key],str) and f['record_materialization'][rid][key].strip()
 for obj in (f['publication_receipt'],f['source_inventory'],f['root_accounting'],*f['additional_receipts'].values()):checked_ref(obj)
 return source,root,ref,inventory,rootacct,baseline

def main():
 f=read(B/'FINAL_SOURCE_INPUTS_V3.json');source,root,ref,inventory,roots,context=validate_inputs(f,receipts_absent=True)
 assert not (C/'plan.json').exists() and not (B/'AUTHORING_RESULT_V3.json').exists(),'Use a new explicit epoch for any revised afterimages'
 oldplan=read(OLD/'plan.json');assert oldplan['plan_sha256']=='af65912789780e214f09644b38f542db5f7670b83ee1fada228bce08f8f9b30e'
 for guard in oldplan['guards']:assert ident(R/guard['path'])['sha256']==guard['sha256']
 oldmap=read(C/'before00-name-mapping.json.txt');mapping=copy.deepcopy(oldmap)
 oldrows={x['id']:x for x in oldmap['migration']['records']};rows={x['id']:x for x in mapping['migration']['records']}
 assert len(rows)==46 and len(mapping['migration']['gates'])==20
 for rid in IDS:
  row=rows[rid];row['lineage']['previous_sources']+=copy.deepcopy(row['sources']);row['sources']=[]
 for row in inventory['sources']:rows[row['record']]['sources'].append(artifact(row,source,ref))
 assert [len(rows[rid]['sources']) for rid in IDS]==[13,6]
 for rid in IDS:
  row=rows[rid];assert row['status']=='blocked' and row['tests']==[]
  row['sync']['source_revision']=source;row['sync']['target_revision']=DEST;row['sync']['pending']=copy.deepcopy(f['pending_obligations'])
  row['recipe']['id']='com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe/'+NAME
  row['recipe']['preconditions']=['All four existing E3 preimages at '+DEST+'; canonical map SHA256 '+ident(C/'before00-name-mapping.json.txt')['sha256']+'.','Preserve the complete E3 crate, all seven dependency guards, all previous source/target objects, unchanged receiver evidence and Descriptor reconciliation.','Actual recovery source publication '+source+' is bound. Current input be92 qualification and historical 0b/d1 evidence remain separately scoped; no capability or JDK promotion.']
  row['observation']=f['record_observations'][rid]
  row['reason']=f['record_reasons'][rid]
  for k in ('outputs','cached_facts'):
   if k in f['record_materialization'][rid]:row['materialization'][k]=f['record_materialization'][rid][k]
  for k in ('targets','tests'):assert row[k]==oldrows[rid][k]
  for k in ('previous_targets','supersedes'):assert row['lineage'][k]==oldrows[rid]['lineage'][k]
 oldbind=read(C/'before02-source-destination-bindings.json.txt');binding=copy.deepcopy(oldbind)
 binding.update(source_commit=source,source_root_tree=root,destination_commit=DEST,destination_root_tree=DTREE,canonical_map_preimage_sha256=ident(C/'before00-name-mapping.json.txt')['sha256'],source_acquisition_receipt_sha256=f['source_inventory']['sha256'],source_acquisition_scope='Actual recovery publication bindings plus separately retained complete be92 current-input context. Source execution, artifact publication and source-qualified export remain separate.',source_root_receipt_sha256=f['root_accounting']['sha256'])
 binding['source_artifacts']=[x for rid in IDS for x in rows[rid]['sources']]
 binding['prior_handoff']={'commit':DEST,'root_tree':DTREE,'pull_request':'https://github.com/hsoliwal/M3jdk21/pull/147','old_source_commit':D1,'old_source_root':oldbind['source_root_tree'],'preserved_crate':str(OLD.relative_to(R)),'old_binding_resource':str((OLD/'after02-source-destination-bindings.json.txt').relative_to(R)),'old_binding_sha256':ident(C/'before02-source-destination-bindings.json.txt')['sha256'],'scope':'The complete preserved E3 binding retains original0b/d1 proof scope and its earlier E2 handoff. This reference grants no qualification transfer.'}
 assert (OLD/'after02-source-destination-bindings.json.txt').read_bytes()==(C/'before02-source-destination-bindings.json.txt').read_bytes()
 binding['source_execution']=copy.deepcopy(inventory['source_execution'])
 binding['source_publication']=copy.deepcopy(inventory['source_publication'])
 assert binding['source_publication']['commit']==source and binding['source_publication']['root_tree']==root
 evidence_inputs=admit_receipt_targets(f)
 evidence={}
 for name,receipt in evidence_inputs.items():
  assert not Path(name).is_absolute() and '..' not in Path(name).parts
  origin=checked_ref(receipt);relative=TASK/'source-recovery'/name
  assert not (R/relative).exists(),relative
  evidence[name]={'path':str(relative),**ident(origin)}
 binding['source_publication']['receipts']=copy.deepcopy(evidence)
 binding['source_current_context']={'input_commit':BASELINE,'input_root_tree':BTREE,'designated_binding_count':19,'original_source_packet_count':503,'current_context_count':527,'current_main_explicit_count':446,'current_parent_count':109,'current_parent_main':88,'current_parent_release8_vendor':19,'current_parent_test_classes':context['current_parent_test_classes'],'receipts':copy.deepcopy(context['receipts']),'explicit_context_artifacts':copy.deepcopy(context['explicit_context_artifacts']),'scope':'Complete selected current context and declaration availability, not all-repository closure or execution authority;19 designated artifacts are not the complete compiler graph.'}
 # Generate root rows from actual final root/accounting; a root directory is not a Maven module.
 oldcoverage=list(csv.DictReader(io.StringIO((C/'before03-root-coverage-obligations.tsv.txt').read_text()),delimiter='\t'));columns=list(oldcoverage[0]);coverage=[]
 decls={}
 for d in roots['maven_declarations']:decls.setdefault(d['path'].split('/')[0],[]).append(d)
 partial={}
 for rid in IDS:
  for s in rows[rid]['sources']:partial.setdefault(s['path'].split('/')[0],set()).add(rid)
 for entry in roots['root_entries']:
  name=entry['path'];ds=decls.get(name,[])
  coverage.append(dict(source_commit=source,source_tree=root,root_path=name,object_type=entry['type'],mode=entry['mode'],object_id=entry['sha'],root_pom_declarations=';'.join(x['path'] for x in ds),declaration_contexts=';'.join(x['context'] for x in ds),source_known='ROOT_OBJECT_BOUND',source_tested='NOT_ESTABLISHED_FOR_ROOT_UNIT',mapping_reviewed='UNREVIEWED_ROOT_UNIT',coverage_obligation='family.entire-source-closure',partial_capability_records=';'.join(sorted(partial.get(name,set()))),export_admitted='false',destination_materialized='false',destination_gates='NOT_RUN_FOR_ROOT_UNIT',read_back_delivered='false'))
 stream=io.StringIO();writer=csv.DictWriter(stream,fieldnames=columns,delimiter='\t',lineterminator='\n');writer.writeheader();writer.writerows(coverage);coverage_bytes=stream.getvalue().encode()
 nested=[d for d in roots['maven_declarations'] if '/' in d['path']];direct=[d for d in roots['maven_declarations'] if '/' not in d['path']];nr={d['path'].split('/')[0] for d in nested};dr={d['path'] for d in direct}
 binding['source_root_accounting']={'root_entries':len(coverage),'root_trees':sum(e['type']=='tree' for e in roots['root_entries']),'root_blobs':sum(e['type']=='blob' for e in roots['root_entries']),'root_gitlinks':sum(e['type']=='commit' for e in roots['root_entries']),'root_pom_module_declarations':len(roots['maven_declarations']),'distinct_module_paths':len({d['path'] for d in roots['maven_declarations']}),'direct_root_modules':len(direct),'nested_module_paths':len(nested),'root_trees_with_nested_modules':len(nr),'nested_containers_also_direct_modules':len(nr&dr),'distinct_root_trees_referenced':len(nr|dr),'root_pom_sha256':roots['root_pom']['sha256'],'root_pom_git_blob':roots['root_pom']['git_blob_sha1'],'whole_repository_file_count':None,'semantic_dependency_closure_complete':False,'coverage_receipt_path':COV,'coverage_receipt_sha256':sha(coverage_bytes)}
 for k in ('descriptor_reconciliation','independent_receiving_fixture','existing_destination_reuse_artifacts','acceptance'):assert binding[k]==oldbind[k],k
 assert all(v is False for v in binding['acceptance'].values())
 for rid in set(rows)-set(IDS):assert rows[rid]==oldrows[rid]
 a=copy.deepcopy(oldmap);z=copy.deepcopy(mapping);del a['migration']['records'],z['migration']['records'];assert a==z
 outputs={MAP:('before00-name-mapping.json.txt','after00-name-mapping.json.txt',enc(mapping)),DOC:('before01-jcc-source-handoff.md.txt','after01-jcc-source-handoff.md.txt',checked_ref(f['handoff_document']).read_bytes()),BIND:('before02-source-destination-bindings.json.txt','after02-source-destination-bindings.json.txt',enc(binding)),COV:('before03-root-coverage-obligations.tsv.txt','after03-root-coverage-obligations.tsv.txt',coverage_bytes)}
 # Validate every output and Java binding sentinel before writing any generated resource.
 java=R/PREFIX/'src/test/java/com/m3/rewrite/backport/JccSourceRecoveryHandoffTest.java';text=java.read_text()
 for old in ('UNBOUND_RECOVERY_SOURCE_COMMIT','UNBOUND_RECOVERY_SOURCE_ROOT_TREE','UNBOUND_RECOVERY_SOURCE_REF','EXPECTED_RECOVERY_SOURCE_COUNT = -1'):assert text.count(old)==1
 for path,(bn,an,body) in outputs.items():assert (R/path).read_bytes()==(C/bn).read_bytes() and body!=(C/bn).read_bytes() and not (C/an).exists()
 plan={'schema':'m3.sealed-install/1','recipe_id':NAME+'/1','source_commit':source,'target_commit':DEST,'scope':'Additive four-existing-input receiving recovery metadata; source proof epochs separate and capability/JDK acceptance blocked.','outputs':[],'guards':copy.deepcopy(oldplan['guards'])};manifest=[]
 for path,(bn,an,body) in sorted(outputs.items()):
  (C/an).write_bytes(body);bh=ident(C/bn)['sha256'];ah=sha(body)
  plan['outputs'].append({'path':path,'before':{'resource':bn,'sha256':bh},'after':{'resource':an,'sha256':ah}});manifest.append('\t'.join((path,bh,ah,an)))
 plan['plan_sha256']=sha((json.dumps(plan,sort_keys=True,separators=(',',':'),ensure_ascii=True)+'\n').encode());(C/'plan.json').write_bytes(enc(plan));(C/'manifest.tsv').write_text('\n'.join(manifest)+'\n')
 for old,new in [('UNBOUND_RECOVERY_SOURCE_COMMIT',source),('UNBOUND_RECOVERY_SOURCE_ROOT_TREE',root),('UNBOUND_RECOVERY_SOURCE_REF',ref),('EXPECTED_RECOVERY_SOURCE_COUNT = -1','EXPECTED_RECOVERY_SOURCE_COUNT = 19')]:text=text.replace(old,new)
 java.write_text(text)
 for name,refobj in evidence_inputs.items():
  p=checked_ref(refobj);rel=TASK/'source-recovery'/name;dest=R/rel;dest.parent.mkdir(parents=True,exist_ok=True);assert not dest.exists();dest.write_bytes(p.read_bytes());evidence[name]={'path':str(rel),**ident(dest)}
 (B/'AUTHORING_RESULT_V3.json').write_bytes(enc({'state':'BOUND_RESOURCES_PENDING_EXECUTION','source_commit':source,'source_root_tree':root,'destination_preimage_commit':DEST,'plan_sha256':plan['plan_sha256'],'map_after_sha256':sha(outputs[MAP][2]),'source_artifacts':19,'current_context_count':527,'operational_outputs_unchanged':True,'source_inventory':f['source_inventory'],'evidence':evidence,'source_export_admitted':False,'recipe_execution':'NOT_EXECUTED'}))
 print(json.dumps({'state':'BOUND_RESOURCES_PENDING_EXECUTION','plan_sha256':plan['plan_sha256'],'source_commit':source,'operational_outputs_unchanged':True}))
if __name__=='__main__':main()
