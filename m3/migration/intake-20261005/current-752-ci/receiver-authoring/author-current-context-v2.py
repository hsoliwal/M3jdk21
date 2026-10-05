#!/usr/bin/env python3
"""Append the closed Cir repair to the first five actual qualified outputs."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,shutil
R=Path(__file__).resolve().parent;C=R.parent;TASK='m3/migration/intake-20261005/current-752-ci'
CORE=R/'author-current-context.py';assert hashlib.sha256(CORE.read_bytes()).hexdigest()=='14e1d2e133527ec80ea4e13c3c3abf2e409c73e95108753d83374b7d7041e032'
spec=importlib.util.spec_from_file_location('retained_context_author',CORE);old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
EXPECTED={
 'm3/tooling/a3/src/main/java/com/m3/a3/A3MemoryCompiler.java',
 'm3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java',
 'm3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/A3MemoryCompiler.java.after',
 'm3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/A3RegexMatrix.java.after',
 'm3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/manifest.tsv'}
BASE_MANIFEST='5ede1e52c03c1952a9d9393335a26ae780deb4eff3a3c7e5beb676a24a95c8a5'

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--producer',type=Path,required=True);ap.add_argument('--result-sha',required=True);ap.add_argument('--output',type=Path,required=True);ap.add_argument('--manifest',type=Path,required=True);a=ap.parse_args()
 req=old.req;sha=old.sha;load=old.load;write=old.write;copy=old.copy;producer=a.producer.resolve();result_path=producer/'RESULT.json'
 req(sha(result_path.read_bytes())==a.result_sha,'declared Cir producer result seal');result=load(result_path)
 req(result['schema']=='synexia.cir-752.proof-result/1' and result['status']=='PASS_FOCUSED_PRODUCER' and result['failure'] is None and result['sourceCommit']==old.SOURCE and result['targetCommit']==old.TARGET,'actual Cir PASS required')
 counts=result['junit']['counts'];req(counts['tests']==len(result['junit']['methods'])>0 and counts['errors']==counts['failures']==counts['skipped']==0,'complete Cir JUnit receipt')
 base_path=R/'consumer-manifest-v1.json';req(sha(base_path.read_bytes())==BASE_MANIFEST,'original materialized context identity');manifest=load(base_path);base=R/'candidate-v1';req(old.inventory(base)==manifest['files'],'original five-output context drift')
 ext={x['path']:x for x in manifest['externalInputs']}
 def bind(p,identity=None):
  p=Path(p);actual=old.row(p)
  if identity is not None:req(actual==identity,'Cir input/artifact drift '+str(p))
  if str(p) in ext:req(ext[str(p)]==actual,'conflicting old/new input')
  ext[str(p)]=actual
 for ident in list(ext.values()):bind(ident['path'],ident)
 req(sha((producer/'inputs.json').read_bytes())==result['inputManifestSHA256'] and sha((producer/'PATCH.json').read_bytes())==result['patchSHA256'],'Cir custody headers')
 for ident in load(producer/'inputs.json'):bind(ident['path'],ident)
 receipts=result['receipts'];req(len(receipts)==4,'four Cir gates')
 for i,ident in enumerate(receipts):
  bind(ident['path'],ident);receipt=load(Path(ident['path']));req(receipt['stage']==['static','compile','tests','runtime'][i] and receipt['exitCode']==0 and receipt['inputManifestSHA256']==result['inputManifestSHA256'] and receipt['priorReceipts']==receipts[:i],'ordered Cir receipts')
  for stream in ['stdout','stderr']:bind(receipt[stream]['path'],receipt[stream])
 for key in ['compiledArtifacts']:
  bind(result[key]['path'],result[key])
 for ident in load(Path(result['compiledArtifacts']['path'])):
  full=producer/'project/target'/ident['path'];bind(full,dict(ident,path=str(full)))
 bind(result['junit']['report']['path'],result['junit']['report'])
 for name in ['RESULT.json','PATCH.json','inputs.json','commands.json','spec.json','runtime.json','junit.json','tooling.json','tool-links.json']:bind(producer/name)
 runtime=result['runtime'];bind(runtime['output']['path'],runtime['output']);bind(runtime['patch']['path'],runtime['patch']);generated=producer/'generated-candidate';output=load(generated/'OUTPUT.json')
 req(output['schema']=='synexia.cir-752.output/1' and output['sourceCommit']==old.SOURCE and output['targetCommit']==old.TARGET,'Cir OUTPUT identity')
 req([e['path'] for e in output['outputs']]==sorted(EXPECTED) and {p.relative_to(generated).as_posix() for p in old.files(generated)}==EXPECTED|{'OUTPUT.json','candidate.patch'},'five exact Cir targets')
 seal=sha(('SYNEXIA-CIR-752-OUTPUT/1\n'+''.join(e['path']+'\t'+e['sha256']+'\t'+str(e['bytes'])+'\n' for e in output['outputs'])).encode());req(seal==output['outputSeal']==runtime['outputSeal'] and output['outputs']==runtime['files'],'actual Cir export seal')
 auth_path=C/'ci-repairs/CIR-AUTHORED.json';bind(auth_path);auth=load(auth_path);auth_by_path={e['path']:e for e in auth['targets']};req(set(auth_by_path)==EXPECTED and auth['sourceCommit']==old.SOURCE and auth['targetCommit']==old.TARGET,'closed admitted Cir target set')
 out=a.output.absolute();req(not out.exists() and not a.manifest.exists(),'fresh final context');shutil.copytree(base,out);task=out/TASK;plan=load(task/'crate/plan.json');mapping=load(task/'PATH_MAPPING.json');original_outputs=list(manifest['producerOutputs']);req(len(original_outputs)==5 and not ({e['path'] for e in original_outputs}&EXPECTED),'disjoint original and Cir outputs')
 for i,e in enumerate(output['outputs'],5):
  p=e['path'];b=(base/p).read_bytes();after=(generated/p).read_bytes();author=auth_by_path[p]
  req(sha(b)==author['beforeSHA256'] and len(b)==author['beforeBytes'],'Cir exact before bytes');req(sha(after)==e['sha256']==author['afterSHA256'] and len(after)==e['bytes']==author['afterBytes'],'Cir actual after matches admission')
  bind(generated/p);(out/p).write_bytes(after)
  for state,data in [('before',b),('after',after)]:q=task/'crate'/state/(str(i)+'.txt');q.write_bytes(data)
  plan['outputs'].append({'path':p,'before':{'resource':'before/'+str(i)+'.txt','sha256':sha(b)},'after':{'resource':'after/'+str(i)+'.txt','sha256':sha(after)}})
  manifest['producerOutputs'].append(dict(e,gitBlob=old.blob(after),artifact=str(generated/p)))
  mapping['outputs'].append({'path':p,'sourceCommit':old.SOURCE,'targetCommit':old.TARGET,'beforeSHA256':sha(b),'beforeGitBlob':old.blob(b),'afterSHA256':sha(after),'afterGitBlob':old.blob(after),'bytes':len(after),'producerOutputSeal':seal,'classification':'existing-A3-Java-lexical-repair' if p.endswith('.java') else 'existing-A3-recipe-resource-reconciliation','productAPIChanged':False})
 req(manifest['producerOutputs'][:5]==original_outputs,'original five output declarations changed')
 for e in original_outputs:req(sha((out/e['path']).read_bytes())==e['sha256'],'original five actual bytes changed')
 plan['recipe_id']='synexia-current-752-ci/2';plan['additional_producer_result_sha256']=a.result_sha;plan['additional_producer_output_seal']=seal;plan['declared_output_count']=10
 plan['guards'] += [{'path':p,'sha256':sha((out/p).read_bytes())} for p in ['m3/pom.xml','m3/tooling/a3/pom.xml']]
 plan.pop('plan_sha256');plan['plan_sha256']=sha((json.dumps(plan,sort_keys=True,separators=(',',':'),ensure_ascii=True)+'\n').encode());write(task/'crate/plan.json',plan)
 mapping['outputsCount']=10;mapping['producerResultSHA256s']=[manifest['provenance']['producerResultSHA256'],a.result_sha];write(task/'PATH_MAPPING.json',mapping)
 for name in ['RESULT.json','PATCH.json','inputs.json','commands.json','runtime.json','junit.json','build-artifacts.json','spec.json']:copy(producer/name,task/'a3-producer'/name)
 for p in old.files(producer/'chain'):copy(p,task/'a3-producer/chain'/p.name)
 for name in ['OUTPUT.json','candidate.patch']:copy(generated/name,task/'a3-producer'/name)
 copy(auth_path,task/'a3-producer/AUTHORED.json')
 rt=R/'proof-runner/runtime-v2.py';copy(rt,task/'verify-runtime.py');pom=task/'pom.xml';pom.write_text(pom.read_text().replace('Five-output current 752','Ten-output current 752').replace('sealed-five-output-recipe-check','sealed-ten-output-recipe-check'))
 readme=task/'README.md';readme.write_text(readme.read_text().replace('transports five actual outputs','transports ten actual outputs').replace('Full current migration-recipes and backports Maven verification','Full current migration-recipes, A3 and backports Maven verification')+'\nThe first five outputs retain their original qualified bytes. A second actual Cir producer repairs Java literal syntax in the two A3 source owners and their matching recipe resources, with the two corresponding manifest postimage cells reconciled. The failed initial full Java trial and successful independent Python receiving trial remain preserved; only the final complete receiving trial can qualify this ten-output context.\n')
 bind(base_path);bind(CORE);bind(Path(__file__).resolve());bind(rt);bind(R/'dependencies/acquisition-v3/RESULT.json');bind(R/'dependencies/acquire-v3.py')
 manifest['tools']['cache']=str(R/'dependencies/m2-v3');manifest['toolSets']['cache']=old.tool_inventory(Path(manifest['tools']['cache']))
 manifest['files']=old.inventory(out);manifest['producerOutputs']=sorted(manifest['producerOutputs'],key=lambda e:e['path']);manifest['externalInputs']=sorted(ext.values(),key=lambda e:e['path']);manifest['runtime']={'script':str(rt),'sha256':sha(rt.read_bytes()),'bytes':rt.stat().st_size}
 manifest['provenance'].update(declaredOutputCount=10,additionalProducerResultSHA256=a.result_sha,additionalProducerOutputSeal=seal,previousCandidateManifestSHA256=BASE_MANIFEST,allCurrentA3TestsRequired=True)
 write(a.manifest,manifest);print(json.dumps({'state':'MATERIALIZED_TEN_ACTUAL_OUTPUTS_NOT_CONSUMER_QUALIFIED','sourceFiles':len(manifest['files']),'manifestSHA256':sha(a.manifest.read_bytes()),'planSHA256':sha((task/'crate/plan.json').read_bytes()),'actualOutputs':10,'firstFivePreserved':True}))

if __name__=='__main__':main()
