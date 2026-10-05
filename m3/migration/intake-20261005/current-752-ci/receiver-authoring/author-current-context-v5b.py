#!/usr/bin/env python3
"""Append the closed Cih repair to the first nineteen actual qualified outputs."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,shutil
R=Path(__file__).resolve().parent;C=R.parent;TASK='m3/migration/intake-20261005/current-752-ci'
CORE=R/'author-current-context.py';assert hashlib.sha256(CORE.read_bytes()).hexdigest()=='14e1d2e133527ec80ea4e13c3c3abf2e409c73e95108753d83374b7d7041e032'
spec=importlib.util.spec_from_file_location('retained_context_author',CORE);old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
JCC_CRATE='m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-handoff-20261005'
ADDED={
 'm3/migration/test/test_jcc_handoff_current_context.py',
 JCC_CRATE+'/plan-intake-752-ci.json',
 JCC_CRATE+'/pom-before-752.xml'}
REPLACED={
 'm3/migration/test/test_jcc_handoff_context_profiles.py',
 'm3/migration/test/test_jcc_handoff_packet.py'}
EXPECTED=ADDED|REPLACED
BASE_MANIFEST='f25a39ea2c764a5a9f2a84b43915aa4480aa4302718bc098bd4bfaa39727f7b3'
RECEIVER_STOP='65ad6376f5e88ca2bd1e33e549c262a50bc58952ede7af8c17c9b0beed05ed73'
RECEIVING_HEAD='5c1ee9490e76943dfdb3ca27fade0d093d017226'

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--producer',type=Path,required=True);ap.add_argument('--authored',type=Path,required=True);ap.add_argument('--result-sha',required=True);ap.add_argument('--output',type=Path,required=True);ap.add_argument('--manifest',type=Path,required=True);a=ap.parse_args()
 req=old.req;sha=old.sha;load=old.load;write=old.write;copy=old.copy;producer=a.producer.resolve();result_path=producer/'RESULT.json'
 req(sha(result_path.read_bytes())==a.result_sha,'declared Cih producer result seal');result=load(result_path)
 req(result['schema']=='synexia.cih-752.proof-result/1' and result['status']=='PASS_FOCUSED_PRODUCER' and result['failure'] is None and result['sourceCommit']==old.SOURCE and result['targetCommit']==old.TARGET,'actual Cih PASS required')
 counts=result['junit']['counts'];req(counts['tests']==len(result['junit']['methods'])>0 and counts['errors']==counts['failures']==counts['skipped']==0,'complete Cih JUnit receipt')
 base_path=R/'consumer-manifest-v4.json';req(sha(base_path.read_bytes())==BASE_MANIFEST,'original materialized context identity');manifest=load(base_path);base=R/'candidate-v4';req(old.inventory(base)==manifest['files'],'original nineteen-output context drift')
 ext={x['path']:x for x in manifest['externalInputs']}
 def bind(p,identity=None):
  p=Path(p);actual=old.row(p)
  if identity is not None:req(actual==identity,'Cih input/artifact drift '+str(p))
  if str(p) in ext:req(ext[str(p)]==actual,'conflicting old/new input')
  ext[str(p)]=actual
 for ident in list(ext.values()):bind(ident['path'],ident)
 req(sha((producer/'inputs.json').read_bytes())==result['inputManifestSHA256'] and sha((producer/'PATCH.json').read_bytes())==result['patchSHA256'],'Cih custody headers')
 producer_inputs=load(producer/'inputs.json')
 for ident in producer_inputs:bind(ident['path'],ident)
 receipts=result['receipts'];req(len(receipts)==4,'four Cih gates')
 for i,ident in enumerate(receipts):
  bind(ident['path'],ident);receipt=load(Path(ident['path']));req(receipt['stage']==['static','compile','tests','runtime'][i] and receipt['exitCode']==0 and receipt['inputManifestSHA256']==result['inputManifestSHA256'] and receipt['priorReceipts']==receipts[:i],'ordered Cih receipts')
  for stream in ['stdout','stderr']:bind(receipt[stream]['path'],receipt[stream])
 for key in ['compiledArtifacts']:
  bind(result[key]['path'],result[key])
 for ident in load(Path(result['compiledArtifacts']['path'])):
  full=producer/'project/target'/ident['path'];bind(full,dict(ident,path=str(full)))
 bind(result['junit']['report']['path'],result['junit']['report'])
 for name in ['RESULT.json','PATCH.json','inputs.json','commands.json','spec.json','runtime.json','junit.json','tooling.json','tool-links.json']:bind(producer/name)
 runtime=result['runtime'];bind(runtime['output']['path'],runtime['output']);bind(runtime['patch']['path'],runtime['patch']);generated=producer/'generated-candidate';output=load(generated/'OUTPUT.json')
 req(output['schema']=='synexia.cih-752.output/1' and output['sourceCommit']==old.SOURCE and output['targetCommit']==old.TARGET,'Cih OUTPUT identity')
 req(output['receivingHead']==RECEIVING_HEAD and output['compositeContextManifestSHA256']==BASE_MANIFEST and output['replacements']==2 and output['additions']==3,'Cih composite lineage and operation totals')
 req([e['path'] for e in output['outputs']]==sorted(EXPECTED) and {p.relative_to(generated).as_posix() for p in old.files(generated)}==EXPECTED|{'OUTPUT.json','candidate.patch'},'five exact Cih targets')
 seal=sha(('SYNEXIA-CIH-752-OUTPUT/1\n'+''.join(e['path']+'\t'+e['sha256']+'\t'+str(e['bytes'])+'\n' for e in output['outputs'])).encode());req(seal==output['outputSeal']==runtime['outputSeal'] and output['outputs']==runtime['files'],'actual Cih export seal')
 auth_path=a.authored.resolve();req(old.row(auth_path) in producer_inputs,'Cih authoring declaration must be sealed in actual producer inputs');bind(auth_path);auth=load(auth_path);auth_by_path={e['path']:e for e in auth['targets']};req(set(auth_by_path)==EXPECTED and auth['sourceCommit']==old.SOURCE and auth['targetCommit']==old.TARGET,'closed admitted Cih target set')
 req(auth['receivingHead']==RECEIVING_HEAD and auth['compositeContextManifestSHA256']==BASE_MANIFEST and auth['receiverStopResultSHA256']==RECEIVER_STOP and auth['historical569ActiveExceptions']==sorted(REPLACED),'admitted composite preimages and exact prior-overlay exceptions')
 req(auth['replacements']==2 and auth['additions']==3 and auth['finalReceivingDistinctPaths']==24,'admitted finite scope')
 for e in auth['contextInputs']:
  actual=(base/e['path']).read_bytes();req(sha(actual)==e['sha256'] and len(actual)==e['bytes'],'exact Cih read-only context '+e['path'])
 bind(R/'evidence/consumer-v4/RESULT.json');req(sha((R/'evidence/consumer-v4/RESULT.json').read_bytes())==RECEIVER_STOP,'retained receiver STOP')
 out=a.output.absolute();req(not out.exists() and not a.manifest.exists(),'fresh final context');shutil.copytree(base,out);task=out/TASK;plan=load(task/'crate/plan.json');mapping=load(task/'PATH_MAPPING.json');original_outputs=list(manifest['producerOutputs']);req(len(original_outputs)==19 and not ({e['path'] for e in original_outputs}&EXPECTED),'disjoint original and Cih outputs')
 for i,e in enumerate(output['outputs'],19):
  p=e['path'];after=(generated/p).read_bytes();author=auth_by_path[p]
  operation='ADD' if p in ADDED else 'REPLACE'
  req(e['operation']==author['operation']==operation and e['beforeSHA256']==author['beforeSHA256'] and e['beforeBytes']==author['beforeBytes'],'exact operation/preimage declarations')
  if operation=='ADD':
   req(not (base/p).exists() and not (base/p).is_symlink() and author['beforeSHA256']=='ABSENT' and author['beforeBytes'] is None,'exact absent-before target')
   b=None;before_image=None;before_sha='ABSENT';before_blob=None
  else:
   b=(base/p).read_bytes();req(sha(b)==author['beforeSHA256'] and len(b)==author['beforeBytes'],'Cih exact composite before bytes')
   before_image={'resource':'before/'+str(i)+'.txt','sha256':sha(b)};before_sha=sha(b);before_blob=old.blob(b)
   (task/'crate/before'/(str(i)+'.txt')).write_bytes(b)
  req(sha(after)==e['sha256']==author['afterSHA256'] and len(after)==e['bytes']==author['afterBytes'],'Cih actual after matches admission')
  bind(generated/p);(out/p).parent.mkdir(parents=True,exist_ok=True);(out/p).write_bytes(after)
  (task/'crate/after'/(str(i)+'.txt')).write_bytes(after)
  plan['outputs'].append({'path':p,'before':before_image,'after':{'resource':'after/'+str(i)+'.txt','sha256':sha(after)}})
  manifest['producerOutputs'].append({'path':p,'sha256':e['sha256'],'bytes':e['bytes'],'gitBlob':old.blob(after),'artifact':str(generated/p)})
  mapping['outputs'].append({'path':p,'sourceCommit':old.SOURCE,'targetCommit':old.TARGET,'receivingHead':RECEIVING_HEAD,'compositeContextManifestSHA256':BASE_MANIFEST,'operation':operation,'beforeSHA256':before_sha,'beforeGitBlob':before_blob,'afterSHA256':sha(after),'afterGitBlob':old.blob(after),'bytes':len(after),'producerOutputSeal':seal,'classification':'exact-JCC-validator-POM-context-and-regression-evidence','productAPIChanged':False})
 req(manifest['producerOutputs'][:19]==original_outputs,'original nineteen output declarations changed')
 for e in original_outputs:req(sha((out/e['path']).read_bytes())==e['sha256'],'original nineteen actual bytes changed')
 plan['recipe_id']='synexia-current-752-ci/5';plan['cih_producer_result_sha256']=a.result_sha;plan['cih_producer_output_seal']=seal;plan['declared_output_count']=24
 plan['composite_preimage_context']={'receivingHead':RECEIVING_HEAD,'contextManifestSHA256':BASE_MANIFEST,'receiverStopResultSHA256':RECEIVER_STOP,'meaning':'current752 plus prior569 head5c and nineteen actual generated repairs; two JCC test preimages belong to that composite context'}
 req(not any(g['path'] in EXPECTED for g in plan['guards']),'Cih must not change any retained guard')
 req({e['path'] for e in plan['outputs'] if e['before'] is None}==ADDED,'exact installer additions')
 plan.pop('plan_sha256');plan['plan_sha256']=sha((json.dumps(plan,sort_keys=True,separators=(',',':'),ensure_ascii=True)+'\n').encode());write(task/'crate/plan.json',plan)
 mapping['outputsCount']=24;req(len(mapping['producerResultSHA256s'])==4,'four existing producer declarations');mapping['producerResultSHA256s'].append(a.result_sha);write(task/'PATH_MAPPING.json',mapping)
 for name in ['RESULT.json','PATCH.json','inputs.json','commands.json','runtime.json','junit.json','build-artifacts.json','spec.json']:copy(producer/name,task/'jcc-context-producer'/name)
 for p in old.files(producer/'chain'):copy(p,task/'jcc-context-producer/chain'/p.name)
 for name in ['OUTPUT.json','candidate.patch']:copy(generated/name,task/'jcc-context-producer'/name)
 copy(auth_path,task/'jcc-context-producer/AUTHORED.json')
 rt=R/'proof-runner/runtime-v5.py';copy(rt,task/'verify-runtime.py');pom=task/'pom.xml';pom.write_text(pom.read_text().replace('Nineteen-output current 752','Twenty-four-output current 752').replace('sealed-nineteen-output-recipe-check','sealed-twenty-four-output-recipe-check'))
 readme=task/'README.md';readme.write_text(readme.read_text().replace('transports nineteen actual outputs','transports twenty-four actual outputs')+'\nThe fifth actual Cih producer adds the exact current validator/POM context without changing either historical JCC plan or the four historical packet outputs. It replaces only the two active JCC test setup owners from the prior 569-file overlay, preserves all six packet and eight historical profile test bodies, and adds a current-context test, a separately sealed plan, and the exact old POM fixture. The original nineteen generated repair bytes stay exact. Three installer targets have explicit ABSENT preimages. Historical receipts retain their original context and are replayed without retargeting.\n')
 bind(base_path);bind(CORE);bind(Path(__file__).resolve());bind(rt);bind(R/'dependencies/acquisition-v3/RESULT.json');bind(R/'dependencies/acquire-v3.py')
 manifest['tools']['cache']=str(R/'dependencies/m2-v3');manifest['toolSets']['cache']=old.tool_inventory(Path(manifest['tools']['cache']))
 manifest['files']=old.inventory(out);manifest['producerOutputs']=sorted(manifest['producerOutputs'],key=lambda e:e['path']);manifest['externalInputs']=sorted(ext.values(),key=lambda e:e['path']);manifest['runtime']={'script':str(rt),'sha256':sha(rt.read_bytes()),'bytes':rt.stat().st_size}
 manifest['provenance'].update(declaredOutputCount=24,cihProducerResultSHA256=a.result_sha,cihProducerOutputSeal=seal,previousCandidateManifestSHA256=BASE_MANIFEST,allCurrentA3TestsRequired=True,addedTargetPaths=sorted(ADDED),outputOperations={e['path']:('ADD' if e['path'] in ADDED else 'REPLACE') for e in manifest['producerOutputs']},historical569ActiveExceptions=[{'path':p,'beforeSHA256':auth_by_path[p]['beforeSHA256'],'afterSHA256':auth_by_path[p]['afterSHA256']} for p in sorted(REPLACED)],compositeContextManifestSHA256=BASE_MANIFEST,receiverStopResultSHA256=RECEIVER_STOP)
 req(manifest['provenance']['sourceFilesBeforeTask']==2359,'retain original baseline count')
 req(len([e for e in manifest['files'] if not e['path'].startswith(TASK+'/')])==2359+len(ADDED),'exact baseline plus three added targets')
 write(a.manifest,manifest);print(json.dumps({'state':'MATERIALIZED_TWENTY_FOUR_ACTUAL_OUTPUTS_NOT_CONSUMER_QUALIFIED','sourceFiles':len(manifest['files']),'manifestSHA256':sha(a.manifest.read_bytes()),'planSHA256':sha((task/'crate/plan.json').read_bytes()),'actualOutputs':24,'firstNineteenPreserved':True,'additions':3,'prior569QualifiedExceptions':sorted(REPLACED)}))

if __name__=='__main__':main()
