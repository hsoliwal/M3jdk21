from pathlib import Path
import base64, collections, hashlib, json, re, xml.etree.ElementTree as ET
B=Path('/workspace/scratch/1c68df1bae79/javac-convergence-20261005/work/m3jdk21-current/successor-e04');O=B/'derivation-static-review/bound-v3';S=O/'inputs'
SOURCE='0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8';ROOT='0361e4a07b77a01df170523033fef01c4052ddc5';BE92='be92c62ece9023b5c33676716a1076d00e26120a'
def load(p):return json.loads(Path(p).read_text())
def identity(p):
 p=Path(p);assert p.is_file() and not p.is_symlink(),p;r=p.read_bytes();return {'bytes':len(r),'sha256':hashlib.sha256(r).hexdigest(),'git_blob_sha1':hashlib.sha1(f'blob {len(r)}\0'.encode()+r).hexdigest(),'mode':'100755' if p.stat().st_mode&0o111 else '100644'}
def bound(ref):
 p=Path(ref.get('local_path',ref.get('body_path',ref.get('path'))));a=identity(p)
 for k in ('bytes','sha256','git_blob_sha1','mode'):
  if k in ref:assert a[k]==ref[k],(p,k)
 return p
inputs=[]
def snapshot(p,name=None):
 p=Path(p);raw=p.read_bytes();q=S/(name or p.name)
 if q.exists():assert q.read_bytes()==raw,('Changed review input',p)
 else:q.write_bytes(raw)
 inputs.append({'source':str(p),'snapshot':str(q.relative_to(O)),**identity(p)})
 return load(p) if p.suffix=='.json' else raw
spec=snapshot(B/'DERIVATION_INPUTS_V3.json');inv=snapshot(B/'source-binding-v3/SOURCE_FIELDS_AND_PROOFS.json');acct=snapshot(B/'source-binding-v3/ROOT_ACCOUNTING.json');pub=snapshot(bound(spec['publication_receipt']));model=load(bound(spec['base_tree_model']));remote_path=bound({'local_path':pub['readback_path'],'sha256':pub['readback_sha256']});remote=load(remote_path)
assert identity(bound(spec['publication_receipt']))['sha256']=='95f325ce57dadab60031f41514d9a9c5fa3ef6eb29a41e3c9a8c81e2dee996b8'
assert (pub['status'],pub['commit'],pub['tree'],pub['parent'])==('PUBLISHED_CUSTODY_VERIFIED',SOURCE,ROOT,BE92)
assert pub['pull_request']['url']=='https://github.com/hsoliwal/com.synexia/pull/9362' and pub['ref_update_forced'] is False
assert remote['commit']['sha']==SOURCE and remote['commit']['tree']['sha']==ROOT and [x['sha'] for x in remote['commit']['parents']]==[BE92]
assert remote['ref']['object']['sha']==SOURCE and remote['pull_request']['head']['sha']==SOURCE and remote['pull_request']['html_url']==pub['pull_request']['url']
trees={}
def tree_hash(entries):
 names=[e['path'] for e in entries];assert len(names)==len(set(names));raw=bytearray()
 for e in sorted(entries,key=lambda x:(x['path']+('/' if x['type']=='tree' else '')).encode()):
  assert e['path'] and '/' not in e['path'] and '\0' not in e['path'];m='40000' if e['mode']=='040000' else e['mode'];raw.extend((m+' '+e['path']).encode()+b'\0'+bytes.fromhex(e['sha']))
 return hashlib.sha1(f'tree {len(raw)}\0'.encode()+raw).hexdigest()
for sha,entries in model['trees'].items():assert tree_hash(entries)==sha;trees[sha]=entries
for sha,t in remote['trees'].items():
 assert t['sha']==sha and t['truncated'] is False and tree_hash(t['tree'])==sha
 if sha in trees:assert sorted((e['path'],e['mode'],e['type'],e['sha']) for e in trees[sha])==sorted((e['path'],e['mode'],e['type'],e['sha']) for e in t['tree'])
 trees[sha]=t['tree']
def resolve(path):
 at=ROOT;trail=[]
 for i,part in enumerate(path.split('/')):
  assert part not in ('','.','..');matches=[e for e in trees[at] if e['path']==part];assert len(matches)==1;e=matches[0];trail.append({'tree':at,'name':part,'entry':e})
  if i<len(path.split('/'))-1:assert e['type']=='tree';at=e['sha']
 return e,trail
assert inv['source_commit']==acct['final_commit']==SOURCE and inv['source_root_tree']==acct['final_root_tree']==ROOT
context=load(B/'current-context/CONTEXT_REFERENCES.json');current={r['path']:r for r in context['designated_sources']};changes=[];selected=[]
for group,count in [('sources',19),('proof_references',55)]:
 rows=inv[group];assert len(rows)==len({r['path'] for r in rows})==count
 for r in rows:
  assert r['commit']==SOURCE;assert Path(r['local_path']).suffix!='.log','Raw logs outside review scope';p=bound(r);entry,trail=resolve(r['path']);assert entry['type']=='blob' and (entry['sha'],entry['mode'])==(r['git_blob_sha1'],r['mode']);assert entry.get('size',r['bytes'])==r['bytes'];assert trail==r['final_tree_path_chain']
  selected.append({'group':group,'path':r['path'],'local_path':str(p),**identity(p)})
  if group=='sources':
   before=current[r['path']];assert r['repo']=='hsoliwal/com.synexia' and r['record']==before['record'] and r['symbol']==before['symbol'] and r['module']==before['module'];assert r['revision_role']=='pinned' and r['tracking_ref']=='refs/heads/'+pub['branch']
   if r['sha256']!=before['sha256']:changes.append(r['path'])
assert set(changes)=={r['path'] for r in inv['current_designated_changes']} and len(changes)==4
assert {k:v for k,v in collections.Counter(r['record'] for r in inv['sources']).items()}=={'synexia.jcc-recipe-laboratory':13,'synexia.jcc-java-jni-regression':6}
assert acct['root_entries']==[{k:e[k] for k in ('path','mode','type','sha')} for e in trees[ROOT]];assert acct['root_entry_count']==len(acct['root_entries'])==343;assert dict(collections.Counter(e['type'] for e in trees[ROOT]))==acct['root_entry_types']=={'tree':255,'blob':87,'commit':1}
pom=Path(acct['root_pom']['body_path']);pom_state='PENDING_MISSING_BODY'
if pom.exists():
 bound(acct['root_pom']);entry,trail=resolve('pom.xml');assert entry['sha']==acct['root_pom']['git_blob_sha1'] and trail==acct['root_pom']['final_tree_path_chain'];xml=ET.fromstring(pom.read_bytes());ns={'m':'http://maven.apache.org/POM/4.0.0'};decl=[]
 for node in xml.findall('./m:modules/m:module',ns):decl.append({'path':node.text.strip(),'context':'project'})
 for profile in xml.findall('./m:profiles/m:profile',ns):
  for node in profile.findall('./m:modules/m:module',ns):decl.append({'path':node.text.strip(),'context':'profile:'+profile.findtext('m:id',namespaces=ns).strip()})
 assert decl==acct['maven_declarations'] and len(decl)==acct['maven_declaration_count']==201;pom_state='BODY_HASH_TREE_MEMBERSHIP_AND_FRESH_XML_DECLARATIONS_MATCH';snapshot(pom,'root-pom.xml')
review=snapshot(bound(spec['execution_review']),'SOURCE_EXECUTION_INPUT.json');execution=inv['source_execution'];old=dict(review['source_execution']);actual=dict(execution);assert old.pop('published_output_revision') is None;assert actual.pop('published_output_revision')==SOURCE;assert old.pop('proofs')==[];proofs=actual.pop('proofs');assert old==actual;assert execution['qualification_status']=='BLOCKED' and execution['source_export_admitted'] is False and execution['destination_gates_passed'] is False
assert proofs==[{k:r[k] for k in ('path','commit','bytes','sha256','git_blob_sha1')}|{'repo':'hsoliwal/com.synexia','url':f'https://github.com/hsoliwal/com.synexia/blob/{SOURCE}/{r["path"]}'} for r in inv['proof_references']]
custody=inv['source_publication']['custody'];assert custody['raw_fixture_custody']==pub['raw_fixture_custody'];assert custody['blob_body_readbacks']==pub['blob_body_readbacks'];assert custody['status']=='PUBLISHED_CUSTODY_VERIFIED';assert custody['all_required_blob_bodies_read_back'] is False and custody['malformed_utf8_body_readback'] is False;assert custody['observed_direct_body_readbacks']==len(pub['blob_body_readbacks'])==21
readback_bodies=[]
for obj in remote['blobs']:
 data=base64.b64decode(''.join(obj['content'].split()),validate=True);blob=hashlib.sha1(f'blob {len(data)}\0'.encode()+data).hexdigest();row={'git_blob':blob,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()};assert obj['sha']==blob;assert row in pub['blob_body_readbacks'];readback_bodies.append(row)
assert len(readback_bodies)==len({r['git_blob'] for r in readback_bodies})==21
for r in pub['raw_fixture_custody']['blob_identity_custody']:
 assert r['raw_body_readback'] is False and r['remote_sha256_observed'] is False and r['method']=='BASE64_CREATE_AND_REMOTE_GIT_TREE_IDENTITY';assert r['git_blob'] not in {r['git_blob'] for r in readback_bodies}
manifest=load(pub['manifest_path']);assert identity(Path(pub['manifest_path']))['sha256']==pub['manifest_sha256'];assert len(manifest['rows'])==pub['payload_files']==2796;assert sum(r['bytes'] for r in manifest['rows'])==pub['payload_bytes']==98141352
result={'schema':'jcc-e04-actual-bound-derived-review/1','scope':'Independent file/JSON/tree/hash/XML inspection only; no candidate imports/functions, source or receiving gates, or remote calls.','source_commit':SOURCE,'source_root':ROOT,'publication_status':pub['status'],'source_files':19,'proof_files':55,'designated_changes':changes,'remote_tree_objects_rehashed':len(remote['trees']),'base_tree_objects_rehashed':len(model['trees']),'root_entries':343,'root_types':acct['root_entry_types'],'root_pom_review':pom_state,'declared_root_module_declarations':acct['maven_declaration_count'],'source_execution_matches_reviewed_input_except_actual_output_and_proofs':True,'source_qualification_status':execution['qualification_status'],'direct_body_readbacks_independently_decoded':21,'identity_only_fixture_readback_flags_false':2,'payload_files':2796,'payload_raw_bytes':98141352,'inputs':inputs,'selected_bodies_checked':selected}
(O/'DERIVED_INPUT_CHECKS.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('inputs','selected_bodies_checked')},indent=2))
