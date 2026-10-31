"""Independent read-only inspection of frozen input data; no candidate imports/calls."""
from pathlib import Path, PurePosixPath
import base64, collections, hashlib, json, posixpath, re
B=Path('/workspace/scratch/1c68df1bae79/javac-convergence-20261005/work/m3jdk21-current/successor-e04')
O=B/'derivation-static-review/bound-v3'; S=O/'inputs'; SOURCE='0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8';ROOT='0361e4a07b77a01df170523033fef01c4052ddc5'
TASK='m3/tooling/migration-recipes/tasks/jcc-source-recovery-handoff-20261005/source-recovery/'
PREFIX='m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-recovery-handoff-20261005'
def load(p):return json.loads(Path(p).read_text())
def identity(p):
 p=Path(p);assert p.is_file() and not p.is_symlink(),p;d=p.read_bytes();return {'bytes':len(d),'sha256':hashlib.sha256(d).hexdigest(),'git_blob_sha1':hashlib.sha1(f'blob {len(d)}\0'.encode()+d).hexdigest(),'mode':'100755' if p.stat().st_mode&0o111 else '100644'}
def check(p,ref):
 p=Path(p);got=identity(p)
 for k in ('bytes','sha256','git_blob_sha1','mode'):
  if k in ref:assert got[k]==ref[k],(str(p),k)
 return got
snapshots=[]
def snapshot(p,name=None):
 p=Path(p);q=S/(name or p.name);data=p.read_bytes()
 if q.exists():assert q.read_bytes()==data,('Previously inspected input drift',p)
 else:q.write_bytes(data)
 snapshots.append({'source':str(p),'snapshot':str(q.relative_to(O)),**identity(p)})
 return load(p) if p.suffix=='.json' else data
f=snapshot(B/'FINAL_SOURCE_INPUTS_V3.json');packet=snapshot(B/'bound-inputs-v3/BOUND_INPUTS_REVIEW_PACKET.json');record=snapshot(B/'bound-inputs-v3/RECORD_CONTENT.json');doc=snapshot(B/'bound-inputs-v3/HANDOFF_DOCUMENT.md').decode();idx=snapshot(B/'custody-final-evidence/CUSTODY_EVIDENCE_INDEX.json');additional=snapshot(B/'custody-final-evidence/ADDITIONAL_RECEIPTS.json');prose=snapshot(B/'custody-final-evidence/CUSTODY_CARRY_PROSE.md').decode()
assert identity(B/'FINAL_SOURCE_INPUTS_V3.json')['sha256']=='df6713ad19ce80daad0383fbd5c3716412e2dca133e1db7e0150b88e141313b5'
assert identity(B/'bound-inputs-v3/HANDOFF_DOCUMENT.md')['sha256']=='d5e18258027f457a1fcdc45ff30b4e2e7a0723825ea95b651933af23f27fd28d'
assert identity(B/'bound-inputs-v3/BOUND_INPUTS_REVIEW_PACKET.json')['sha256']=='e0ae5375e313a8547f5cdb8f607d2fe1c04efe91d3d43c86d1a5878329beaf59'
for r in packet['files']:check(r['local_path'],r)
assert len(packet['files'])==14
for k in ('publication_receipt','source_inventory','root_accounting','handoff_document'):check(f[k]['local_path'],f[k])
pub=load(f['publication_receipt']['local_path']);inv=load(f['source_inventory']['local_path']);acct=load(f['root_accounting']['local_path'])
assert f['source_commit']==packet['source_commit']==idx['source_commit']==SOURCE
assert f['source_root_tree']==packet['source_root_tree']==idx['source_root']==ROOT
assert f['source_pr']==packet['source_pr']==idx['source_pr']==pub['pull_request']['url']
assert f['state']==packet['source_publication_status']=='PUBLISHED_CUSTODY_VERIFIED'
assert f['source_publication_custody']==inv['source_publication']['custody']
assert f['source_qualification_reviewed'] is True and f['promote_capability'] is False
assert inv['source_execution']['qualification_status']=='BLOCKED' and inv['source_execution']['source_export_admitted'] is False and inv['source_execution']['destination_gates_passed'] is False
for k in ('source_export_admitted','promote_capability','afterimages_generated','receiving_execution'):assert packet[k] is False
for k in ('pending_obligations','record_observations','record_reasons','record_materialization'):assert f[k]==record[k]
assert packet['designated_sources']==19 and packet['source_proof_files']==55
assert packet['destination_preimage_commit']=='0994ecd65e86600f417f4d8702838d8c2663af61'
assert len(f['additional_receipts'])==packet['additional_receipts']==131 and len(additional)==123
for name,r in additional.items():assert f['additional_receipts'][name]==r
expected_extra={'source-results/RECOVERY_RESULT.json','source-results/E4_EXECUTION_INPUT.json','source-results/RECOVERY_REPORT.md','source-results/DESIGNATED_SOURCE_OUTPUTS.json','source-results/FINAL_T03_REVIEW.md','source-results/ROOT_POM.xml','source-results/ROOT_POM_RESTORATION.json','source-results/ROOT_POM_RECOVERY_CALL.json'}
assert set(f['additional_receipts'])-set(additional)==expected_extra
receipts={name:f[key] for name,key in [('PUBLICATION_CUSTODY.json','publication_receipt'),('SOURCE_FIELDS_AND_PROOFS.json','source_inventory'),('ROOT_ACCOUNTING.json','root_accounting')]}
assert not set(receipts)&set(f['additional_receipts']);receipts.update(f['additional_receipts']);assert len(receipts)==packet['total_authored_receipts']==134
planned=[]
for name,r in receipts.items():
 assert isinstance(name,str) and '\\' not in name and not PurePosixPath(name).is_absolute() and str(PurePosixPath(name))==name
 assert all(p not in ('','.','..') for p in name.split('/'))
 check(r['local_path'],r);target=B/'overlay'/TASK/name
 assert not target.exists() and not target.is_symlink(),target
 for p in target.parents:assert not p.is_symlink() and (not p.exists() or p.is_dir()),p
 planned.append(TASK+name)
assert len(planned)==len(set(planned))
for a in planned:
 for b in planned:
  if a!=b:assert not b.startswith(a+'/'),(a,b)
assert not (B/'AUTHORING_RESULT_V3.json').exists() and not (B/'overlay'/PREFIX/'plan.json').exists()
assert sorted(p.name for p in (B/'overlay'/PREFIX).iterdir())==['before00-name-mapping.json.txt','before01-jcc-source-handoff.md.txt','before02-source-destination-bindings.json.txt','before03-root-coverage-obligations.tsv.txt']
counts=idx['counts'];assert counts['exact_original_files']==len(idx['items'])==121 and counts['exact_original_bytes']==sum(r['bytes'] for r in idx['items'])==15440298
assert len({r['planned_receiving_copy_name'] for r in idx['items']})==121
carried=[];drafts=0
for r in idx['items']:
 assert Path(r['source_local_path']).suffix!='.log'
 original=check(r['source_local_path'],r);staged=check(r['staged_local_path'],r);assert original==staged
 name=r['planned_receiving_copy_name'];assert r['planned_receiving_path']==TASK+name;assert additional[name]['local_path']==r['staged_local_path']
 assert r['carry_status']=='DIRECTLY_CARRIED_EXACT_ORIGINAL_FILE' and r['representation']=='EXACT_ORIGINAL_FILE_BYTES'
 if 'verified_prior_draft_copy' in r:assert check(r['verified_prior_draft_copy'],r)==original;drafts+=1
 carried.append({'name':name,'source_local_path':r['source_local_path'],**original})
assert drafts==counts['prior_draft_files_reverified']==35
assert set(additional)-{r['planned_receiving_copy_name'] for r in idx['items']}=={'custody-v3/CUSTODY_EVIDENCE_INDEX.json','custody-v3/CUSTODY_CARRY_PROSE.md'}
for k in ('receiving_published','original_absolute_references_rewritten','complete_original_raw_custody_envelope_reproduction_claim','complete_source_payload_checkout_claim','source_export_admitted','destination_gates_passed'):assert idx[k] is False
assert idx['structured_subobject_extractions']==[]
assert idx['publication_receipt_sha256']==f['publication_receipt']['sha256']
remote=load(additional['custody-v3/source-publisher/epoch-01/FINAL_READBACK.json']['local_path']);omitted=[];tree_objects={};omitted_counts=collections.Counter()
def pointer(obj,p):
 for part in p.split('/')[1:]:
  part=part.replace('~1','/').replace('~0','~');obj=obj[int(part)] if isinstance(obj,list) else obj[part]
 return obj
for r in idx['omitted_original_local_files']:
 assert r['staged_local_path'] is None and r['planned_receiving_copy_name'] is None
 check(r['source_local_path'],r);holder=load(additional[r['binding']['carried_copy_name']]['local_path']);entry=pointer(holder,r['binding']['json_pointer']);status=r['carry_status'];omitted_counts[status]+=1
 if isinstance(entry,dict):
  assert entry.get('local_path',entry.get('path'))==r['source_local_path'] and entry['sha256']==r['sha256']
  if 'bytes' in entry:assert entry['bytes']==r['bytes']
 else:assert entry==r['source_local_path'] and holder['readback_sha256']==r['sha256']
 if '/final-tree-calls/' in r['source_local_path']:
  bundle=load(r['source_local_path']);assert bundle['final_commit']==SOURCE
  for call in bundle['calls']:
   assert call['result']['status']=='fulfilled';obj=json.loads(call['result']['value']['structuredContent']['content']);assert call['url'].endswith('/'+obj['sha']);assert obj==remote['trees'][obj['sha']];assert obj['sha'] not in tree_objects;tree_objects[obj['sha']]=obj
 if r['source_local_path'].endswith('/BEFORE_BRANCH_READBACK.json'):
  prior=load(r['source_local_path'])
  for k in ('trees','blobs','raw_fixture_final_reads'):assert prior[k]==remote[k]
 omitted.append({'source_local_path':r['source_local_path'],'sha256':r['sha256'],'binding':r['binding'],'carry_status':status})
assert len(omitted)==132 and dict(omitted_counts)=={'RECEIPT_BOUND_ORIGINAL_LOCAL_ENVELOPE_NOT_CARRIED':42,'RECEIPT_BOUND_REDUNDANT_LOCAL_READBACK_NOT_CARRIED':1,'INDEX_BOUND_TRANSPORT_BATCH_NOT_CARRIED':89}
assert len(tree_objects)==counts['normalized_final_tree_bodies']==830 and tree_objects==remote['trees']
assert counts['omitted_final_tree_call_envelopes']==42 and counts['omitted_before_branch_readbacks']==1 and counts['omitted_transport_batches']==89
assert counts['final_file_call_envelopes']==sum('/final-file-calls/' in r['source_local_path'] for r in idx['items'])==23
assert counts['final_publication_call_envelopes']==sum('/final-publication-calls/' in r['source_local_path'] for r in idx['items'])==3
assert counts['exact_raw_create_captures']==sum('/raw-custody-v3/create-call-' in r['source_local_path'] for r in idx['items'])==4
restoration_ref=f['additional_receipts']['source-results/ROOT_POM_RESTORATION.json'];restoration=snapshot(restoration_ref['local_path']);call_ref=f['additional_receipts']['source-results/ROOT_POM_RECOVERY_CALL.json'];call=snapshot(call_ref['local_path']);check(call_ref['local_path'],{'sha256':restoration['actual_call_sha256']})
assert call['request']=={'repository_full_name':'hsoliwal/com.synexia','path':'pom.xml','ref':SOURCE,'encoding':'base64'}
response=call['response']['structuredContent'];raw=base64.b64decode(''.join(response['content'].split()),validate=True);pom_path=Path(f['additional_receipts']['source-results/ROOT_POM.xml']['local_path']);assert raw==pom_path.read_bytes();assert response['sha']==restoration['git_blob']=='98889a040aad5cc3c82f782bb4b173d118207988'
for p in restoration['paths']:check(p,restoration)
assert restoration['source_commit']==SOURCE and hashlib.sha256(raw).hexdigest()==acct['root_pom']['sha256']=='f01cca4d90bd8fa1b1125474b5178cc80762d934bcb7bf5817f08f3f0dcf6aab'
assert not re.search(r'\{\{[^{}]+\}\}|UNBOUND_RECOVERY',doc) and SOURCE in doc and ROOT in doc and f['source_pr'] in doc
assert prose.strip() in doc
links=[]
for link in re.findall(r'\]\(([^)]+)\)',doc):
 if link.startswith('https://'):
  links.append({'link':link,'kind':'external-provenance'});continue
 target=posixpath.normpath(posixpath.join('m3/docs',link.split('#')[0]));assert target in planned or (B/'overlay'/target).is_file(),(link,target)
 links.append({'link':link,'target':target,'kind':'planned-receipt' if target in planned else 'existing-preserved-file'})
result={'schema':'jcc-e04-bound-package-review/1','scope':'Independent standard-library byte/hash/JSON/XML-relationship and path inspection; no candidate module imports/functions, generator, gates, or remote calls.','source_commit':SOURCE,'source_root_tree':ROOT,'packet_references_checked':14,'planned_receipt_copies':134,'additional_receipts':131,'exact_custody_additional_map_entries':123,'carried_original_files':121,'carried_original_bytes':15440298,'prior_draft_copies_checked':35,'omitted_envelopes_independently_hashed_and_bound':132,'omitted_by_status':dict(omitted_counts),'actual_tree_envelopes_matched_to_final_normalized_objects':830,'restored_root_pom_actual_capture_body_matched':True,'final_prose_has_no_unbound_tokens':True,'handoff_links':links,'all_planned_receipt_paths_absent_canonical_nonconflicting':True,'source_qualification_status':'BLOCKED','source_export_admitted':False,'promote_capability':False,'afterimages_generated':False,'receiving_execution':False,'snapshots':snapshots,'planned_paths':planned,'carried_files':carried,'omitted_files':omitted}
(O/'PACKAGE_CHECKS.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('snapshots','planned_paths','carried_files','omitted_files')},indent=2))
