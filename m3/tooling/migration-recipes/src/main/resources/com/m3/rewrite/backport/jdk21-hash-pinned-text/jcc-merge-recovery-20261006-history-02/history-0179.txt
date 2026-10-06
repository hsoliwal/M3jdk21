"""Derive E4 source/root receipts from actual publication plus exact Git tree bodies.

Work-only adapter: requires concrete readback, complete tree objects and reviewed
execution summaries. It is not a source scanner, compiler or mapping authority.
No writes occur while DERIVATION_INPUTS is UNBOUND.
"""
from __future__ import annotations
import copy,hashlib,json,re,xml.etree.ElementTree as ET
from pathlib import Path
from publication_custody_v3 import admit_publication_custody
B=Path(__file__).resolve().parent
INPUT='be92c62ece9023b5c33676716a1076d00e26120a';INPUT_ROOT='3c4f32b66633a251ba2c117090830252a7e2da03'
OLD='d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906'

def data(p):return json.loads(p.read_text())
def encoded(v):return (json.dumps(v,indent=2)+'\n').encode()
def identity(p):
 assert p.is_file() and not p.is_symlink(),p
 b=p.read_bytes();return {'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'git_blob_sha1':hashlib.sha1(f'blob {len(b)}\0'.encode()+b).hexdigest()}
def receipt(ref):
 p=Path(ref['local_path']);a=identity(p);assert a['sha256']==ref['sha256'],p
 for k in ('bytes','git_blob_sha1'):
  if k in ref:assert a[k]==ref[k],(p,k)
 return p

def git_tree_id(tree):
 assert tree.get('truncated') is False,'Complete nonrecursive tree required'
 entries=tree['tree'];names=[e['path'] for e in entries]
 assert len(names)==len(set(names)) and all(n and '/' not in n and n not in ('.','..') for n in names)
 body=b''
 for e in sorted(entries,key=lambda e:(e['path']+('/' if e['type']=='tree' else '')).encode('utf-8')):
  assert re.fullmatch('[0-9a-f]{40}',e['sha'])
  mode=e['mode'];expected={'tree':'040000','commit':'160000','blob':None}[e['type']]
  if expected is not None:assert mode==expected
  else:assert mode in ('100644','100755','120000')
  body+=(('40000' if mode=='040000' else mode)+' '+e['path']).encode('utf-8')+b'\0'+bytes.fromhex(e['sha'])
 return hashlib.sha1(f'tree {len(body)}\0'.encode()+body).hexdigest()

def main():
 if not __debug__:raise RuntimeError('Optimized Python disables required handoff admission assertions')
 spec=data(B/'DERIVATION_INPUTS_V3.json')
 assert spec['state']=='PUBLISHED_CUSTODY_VERIFIED','No final recovery publication supplied'
 pub_path=receipt(spec['publication_receipt']);pub=data(pub_path)
 assert pub['schema']=='jcc-source-recovery-publication/1' and pub['status']=='PUBLISHED_CUSTODY_VERIFIED'
 custody=admit_publication_custody(pub)
 assert pub['repository']=='hsoliwal/com.synexia'
 source=pub['commit'];root=pub['tree'];ref='refs/heads/'+pub['branch']
 assert re.fullmatch('[0-9a-f]{40}',source) and source not in (INPUT,OLD,'0'*40)
 assert re.fullmatch('[0-9a-f]{40}',root) and ref=='refs/heads/aix/jcc-source-merge-recovery-20261005'
 assert pub['parent']==INPUT and pub['branch_commit_parent_tree_pr_verified'] is True
 assert pub['source_input_revision']==INPUT and pub['source_input_root']==INPUT_ROOT
 assert pub['ref_update_forced'] is False
 out=B/'source-binding-v3';assert not out.exists(),'Derive a new explicit epoch instead of overwriting'
 trees={};input_receipts=[]
 def admit(tree):
  tree_id=git_tree_id(tree);assert tree_id==tree['sha']
  normalized=sorted(({k:e[k] for k in ('path','mode','type','sha')} for e in tree['tree']),key=lambda e:e['path'])
  if tree_id in trees:
   prior=sorted(({k:e[k] for k in ('path','mode','type','sha')} for e in trees[tree_id]['tree']),key=lambda e:e['path'])
   assert prior==normalized
  trees[tree_id]=tree
 # Reuse the source publisher's actual existing base/readback models; verify
 # every reconstructed object and follow only paths from the real final root.
 model_path=receipt(spec['base_tree_model']);model=data(model_path)
 assert model['schema']=='jcc-merge-publication-base-tree-model/1'
 assert model['commit']==INPUT and model['root']==INPUT_ROOT
 for tree_id,entries in model['trees'].items():admit({'sha':tree_id,'tree':entries,'truncated':False})
 input_receipts.append({'local_path':str(model_path),**identity(model_path),'scope':'Retained content-addressed base tree bodies; exact objects rehashed.'})
 remote_path=receipt({'local_path':pub['readback_path'],'sha256':pub['readback_sha256']});remote=data(remote_path)
 assert remote['schema']=='jcc-source-recovery-remote-readback/1' and remote['repository']==pub['repository']
 assert remote['commit']['sha']==source and remote['commit']['tree']['sha']==root
 assert [x['sha'] for x in remote['commit']['parents']]==[INPUT]
 assert remote['ref']['ref']==ref and remote['ref']['object']['type']=='commit' and remote['ref']['object']['sha']==source
 assert remote['pull_request']['html_url']==pub['pull_request']['url'] and remote['pull_request']['head']['sha']==source
 for tree_id,tree in remote['trees'].items():
  assert tree['sha']==tree_id;admit(tree)
 input_receipts.append({'local_path':str(remote_path),**identity(remote_path),'scope':'Actual publication remote readback tree bodies; exact objects rehashed.'})
 assert root in trees
 def entry(path):
  assert path and not path.startswith('/') and all(x not in ('','..','.') for x in path.split('/'))
  at=root;trail=[];parts=path.split('/')
  for i,name in enumerate(parts):
   assert at in trees,('Missing exact ancestor tree body',path,at)
   choices=[x for x in trees[at]['tree'] if x['path']==name];assert len(choices)==1,(path,name)
   e=choices[0];trail.append({'tree':at,'name':name,'entry':copy.deepcopy(e)})
   if i<len(parts)-1:assert e['type']=='tree',(path,e);at=e['sha']
  return e,trail
 def bound_body(path,local):
  e,trail=entry(path);a=identity(local)
  assert e['type']=='blob' and e['mode'] in ('100644','100755')
  assert a['git_blob_sha1']==e['sha'],(path,'blob')
  if 'size' in e:assert a['bytes']==e['size'],(path,'size')
  return {'path':path,'commit':source,'local_path':str(local),**a,'mode':e['mode'],'final_tree_path_chain':trail}
 context_path=B/'current-context/CONTEXT_REFERENCES.json'
 assert identity(context_path)['sha256']=='fbc460874aeb04a688ad0ed9b47ad8e3f3b37f88af09d0f32ecfeedc2827fbed'
 context=data(context_path);assert context['source_input_commit']==INPUT and context['source_input_root']==INPUT_ROOT
 output_root=Path(spec['source_output_root']);assert output_root.is_dir()
 sources=[];changes=[]
 for current in context['designated_sources']:
  row=bound_body(current['path'],output_root/current['path'])
  prior=current['prior_E3_object'];row.update(repo='hsoliwal/com.synexia',module=current['module'],symbol=current['symbol'],signatures=copy.deepcopy(prior['signatures']) if prior else [],fingerprint=copy.deepcopy(prior['fingerprint']) if prior else None,revision_role='pinned',tracking_ref=ref,record=current['record'],input_commit=INPUT,input_sha256=current['sha256'],input_git_blob_sha1=current['git_blob_sha1'])
  sources.append(row)
  if row['sha256']!=current['sha256']:changes.append({'path':row['path'],'before_sha256':current['sha256'],'after_sha256':row['sha256'],'before_git_blob_sha1':current['git_blob_sha1'],'after_git_blob_sha1':row['git_blob_sha1'],'record':row['record'],'scope':'Designated source body difference from current be92; actual recipe/write proof is separately required.'})
 assert len(sources)==19
 proofs=[];seen=set()
 for obj in spec['proofs']:
  path=obj['path'];assert path not in seen;seen.add(path)
  local=receipt(obj);proofs.append(bound_body(path,local))
 assert proofs
 # Reviewed semantics are supplied separately; tree membership does not establish a pass.
 review_path=receipt(spec['execution_review']);review=data(review_path)
 assert review['source_input_commit']==INPUT and review['source_input_root']==INPUT_ROOT
 assert review['qualification_review_complete'] is True and review['source_export_admitted'] is False
 execution=copy.deepcopy(review['source_execution'])
 assert execution['input_remote_revision']==INPUT and execution['input_remote_root_tree']==INPUT_ROOT
 assert execution.get('published_output_revision') is None,'Do not predeclare an output identity in review'
 execution['published_output_revision']=source
 assert execution['source_export_admitted'] is False and execution['destination_gates_passed'] is False
 assert 'proofs' not in execution or execution['proofs']==[]
 execution['proofs']=[{k:r[k] for k in ('path','commit','bytes','sha256','git_blob_sha1')}|{'repo':'hsoliwal/com.synexia','url':f'https://github.com/hsoliwal/com.synexia/blob/{source}/{r["path"]}'} for r in proofs]
 # Recompute root and POM counts from this actual publication; no old count inference.
 pom_path=receipt(spec['root_pom']);pom=bound_body('pom.xml',pom_path);xml=ET.fromstring(pom_path.read_bytes());ns={'m':'http://maven.apache.org/POM/4.0.0'}
 assert xml.tag=='{'+ns['m']+'}project'
 decl=[]
 for node in xml.findall('./m:modules/m:module',ns):
  path=(node.text or '').strip();assert path;decl.append({'path':path,'context':'project'})
 for profile in xml.findall('./m:profiles/m:profile',ns):
  profile_id=(profile.findtext('m:id',default='',namespaces=ns) or '').strip();assert profile_id
  for node in profile.findall('./m:modules/m:module',ns):
   path=(node.text or '').strip();assert path;decl.append({'path':path,'context':'profile:'+profile_id})
 root_entries=[{k:e[k] for k in ('path','mode','type','sha')} for e in trees[root]['tree']]
 byroot={e['path']:e for e in root_entries}
 unresolved=[d for d in decl if d['path'].split('/')[0] not in byroot or byroot[d['path'].split('/')[0]]['type']!='tree']
 assert not unresolved,('Unresolved module-root declarations need explicit accounting policy before generation',unresolved)
 root_account={'schema':'jcc-final-root-accounting/1','repository':'hsoliwal/com.synexia','final_commit':source,'final_root_tree':root,'tracking_ref':ref,'root_entries':root_entries,'root_entry_count':len(root_entries),'root_entry_types':{kind:sum(e['type']==kind for e in root_entries) for kind in ('tree','blob','commit')},'root_pom':{'body_path':pom['local_path'],**{k:pom[k] for k in ('bytes','sha256','git_blob_sha1')},'final_tree_path_chain':pom['final_tree_path_chain']},'maven_declarations':decl,'maven_declaration_count':len(decl),'unresolved_module_root_declarations':unresolved,'qualification':'Actual final Git root and fresh root-POM declarations only; no reactor execution, descendant-file denominator or semantic closure.','publication_receipt':{'local_path':str(pub_path),**identity(pub_path)},'tree_receipts':input_receipts}
 source_pub={'commit':source,'root_tree':root,'parent':pub['parent'],'tracking_ref':ref,'pull_request':pub['pull_request'],'source_gate_or_export_authority_granted':False,'publication_status':pub['status'],'custody':copy.deepcopy(custody),'scope':'Publication Git custody with six mandatory direct body reads and two explicit invalid-UTF8 identity-only fixtures is separate from source execution qualification.'}
 inventory={'schema':'jcc-recovery-source-fields-and-proofs/1','source_commit':source,'source_root_tree':root,'tracking_ref':ref,'sources':sources,'proof_references':proofs,'source_publication':source_pub,'source_execution':execution,'current_designated_changes':changes,'designated_changes_are_full_repository_change_count':False,'source_input_commit':INPUT,'source_input_root':INPUT_ROOT,'publication_receipt':{'local_path':str(pub_path),**identity(pub_path)},'execution_review_receipt':{'local_path':str(review_path),**identity(review_path)},'tree_receipts':input_receipts,'source_current_context':{'local_path':str(B/'current-context/CONTEXT_REFERENCES.json'),**identity(B/'current-context/CONTEXT_REFERENCES.json')}}
 out.mkdir()
 for name,value in [('SOURCE_FIELDS_AND_PROOFS.json',inventory),('ROOT_ACCOUNTING.json',root_account)]: (out/name).write_bytes(encoded(value))
 result={'state':'DERIVED_FROM_ACTUAL_PUBLICATION_PENDING_INDEPENDENT_REVIEW','source_commit':source,'source_root_tree':root,'designated_sources':len(sources),'designated_changed_bodies':len(changes),'proofs':len(proofs),'root_entries':len(root_entries),'root_pom_declarations':len(decl),'files':[{ 'local_path':str(out/name),**identity(out/name)} for name in ('SOURCE_FIELDS_AND_PROOFS.json','ROOT_ACCOUNTING.json')],'source_export_admitted':False,'receiving_recipe_execution':False}
 (out/'DERIVATION_RESULT.json').write_bytes(encoded(result));print(json.dumps(result))
if __name__=='__main__':main()
