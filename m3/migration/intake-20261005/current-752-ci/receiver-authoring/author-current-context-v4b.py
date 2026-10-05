#!/usr/bin/env python3
"""Append the closed Cic repair to the first sixteen actual qualified outputs."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,shutil
R=Path(__file__).resolve().parent;C=R.parent;TASK='m3/migration/intake-20261005/current-752-ci'
CORE=R/'author-current-context.py';assert hashlib.sha256(CORE.read_bytes()).hexdigest()=='14e1d2e133527ec80ea4e13c3c3abf2e409c73e95108753d83374b7d7041e032'
spec=importlib.util.spec_from_file_location('retained_context_author',CORE);old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
EXPECTED={
 'm3/tooling/a3/src/main/java/com/m3/a3/A3Cases.java',
 'm3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-mastery-lab/A3Cases.java.after',
 'm3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-mastery-lab/manifest.tsv'}
BASE_MANIFEST='cffd588bc44cdaa139775922330ecc36a539716e64c938d3e9cf2b0281721364'

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--producer',type=Path,required=True);ap.add_argument('--result-sha',required=True);ap.add_argument('--output',type=Path,required=True);ap.add_argument('--manifest',type=Path,required=True);a=ap.parse_args()
 req=old.req;sha=old.sha;load=old.load;write=old.write;copy=old.copy;producer=a.producer.resolve();result_path=producer/'RESULT.json'
 req(sha(result_path.read_bytes())==a.result_sha,'declared Cic producer result seal');result=load(result_path)
 req(result['schema']=='synexia.cic-752.proof-result/1' and result['status']=='PASS_FOCUSED_PRODUCER' and result['failure'] is None and result['sourceCommit']==old.SOURCE and result['targetCommit']==old.TARGET,'actual Cic PASS required')
 counts=result['junit']['counts'];req(counts['tests']==len(result['junit']['methods'])>0 and counts['errors']==counts['failures']==counts['skipped']==0,'complete Cic JUnit receipt')
 base_path=R/'consumer-manifest-v3.json';req(sha(base_path.read_bytes())==BASE_MANIFEST,'original materialized context identity');manifest=load(base_path);base=R/'candidate-v3';req(old.inventory(base)==manifest['files'],'original sixteen-output context drift')
 ext={x['path']:x for x in manifest['externalInputs']}
 def bind(p,identity=None):
  p=Path(p);actual=old.row(p)
  if identity is not None:req(actual==identity,'Cic input/artifact drift '+str(p))
  if str(p) in ext:req(ext[str(p)]==actual,'conflicting old/new input')
  ext[str(p)]=actual
 for ident in list(ext.values()):bind(ident['path'],ident)
 req(sha((producer/'inputs.json').read_bytes())==result['inputManifestSHA256'] and sha((producer/'PATCH.json').read_bytes())==result['patchSHA256'],'Cic custody headers')
 for ident in load(producer/'inputs.json'):bind(ident['path'],ident)
 receipts=result['receipts'];req(len(receipts)==4,'four Cic gates')
 for i,ident in enumerate(receipts):
  bind(ident['path'],ident);receipt=load(Path(ident['path']));req(receipt['stage']==['static','compile','tests','runtime'][i] and receipt['exitCode']==0 and receipt['inputManifestSHA256']==result['inputManifestSHA256'] and receipt['priorReceipts']==receipts[:i],'ordered Cic receipts')
  for stream in ['stdout','stderr']:bind(receipt[stream]['path'],receipt[stream])
 for key in ['compiledArtifacts']:
  bind(result[key]['path'],result[key])
 for ident in load(Path(result['compiledArtifacts']['path'])):
  full=producer/'project/target'/ident['path'];bind(full,dict(ident,path=str(full)))
 bind(result['junit']['report']['path'],result['junit']['report'])
 for name in ['RESULT.json','PATCH.json','inputs.json','commands.json','spec.json','runtime.json','junit.json','tooling.json','tool-links.json']:bind(producer/name)
 runtime=result['runtime'];bind(runtime['output']['path'],runtime['output']);bind(runtime['patch']['path'],runtime['patch']);generated=producer/'generated-candidate';output=load(generated/'OUTPUT.json')
 req(output['schema']=='synexia.cic-752.output/1' and output['sourceCommit']==old.SOURCE and output['targetCommit']==old.TARGET,'Cic OUTPUT identity')
 req([e['path'] for e in output['outputs']]==sorted(EXPECTED) and {p.relative_to(generated).as_posix() for p in old.files(generated)}==EXPECTED|{'OUTPUT.json','candidate.patch'},'three exact Cic targets')
 seal=sha(('SYNEXIA-CIC-752-OUTPUT/1\n'+''.join(e['path']+'\t'+e['sha256']+'\t'+str(e['bytes'])+'\n' for e in output['outputs'])).encode());req(seal==output['outputSeal']==runtime['outputSeal'] and output['outputs']==runtime['files'],'actual Cic export seal')
 auth_path=C/'ci-repairs/CIC-AUTHORED.json';bind(auth_path);auth=load(auth_path);auth_by_path={e['path']:e for e in auth['targets']};req(set(auth_by_path)==EXPECTED and auth['sourceCommit']==old.SOURCE and auth['targetCommit']==old.TARGET,'closed admitted Cic target set')
 out=a.output.absolute();req(not out.exists() and not a.manifest.exists(),'fresh final context');shutil.copytree(base,out);task=out/TASK;plan=load(task/'crate/plan.json');mapping=load(task/'PATH_MAPPING.json');original_outputs=list(manifest['producerOutputs']);req(len(original_outputs)==16 and not ({e['path'] for e in original_outputs}&EXPECTED),'disjoint original and Cic outputs')
 for i,e in enumerate(output['outputs'],16):
  p=e['path'];b=(base/p).read_bytes();after=(generated/p).read_bytes();author=auth_by_path[p]
  req(sha(b)==author['beforeSHA256'] and len(b)==author['beforeBytes'],'Cic exact before bytes');req(sha(after)==e['sha256']==author['afterSHA256'] and len(after)==e['bytes']==author['afterBytes'],'Cic actual after matches admission')
  bind(generated/p);(out/p).write_bytes(after)
  for state,data in [('before',b),('after',after)]:q=task/'crate'/state/(str(i)+'.txt');q.write_bytes(data)
  plan['outputs'].append({'path':p,'before':{'resource':'before/'+str(i)+'.txt','sha256':sha(b)},'after':{'resource':'after/'+str(i)+'.txt','sha256':sha(after)}})
  manifest['producerOutputs'].append(dict(e,gitBlob=old.blob(after),artifact=str(generated/p)))
  mapping['outputs'].append({'path':p,'sourceCommit':old.SOURCE,'targetCommit':old.TARGET,'beforeSHA256':sha(b),'beforeGitBlob':old.blob(b),'afterSHA256':sha(after),'afterGitBlob':old.blob(after),'bytes':len(after),'producerOutputSeal':seal,'classification':'existing-hostile-regex-fixture-or-owned-resource-reconciliation','productAPIChanged':False})
 req(manifest['producerOutputs'][:16]==original_outputs,'original sixteen output declarations changed')
 for e in original_outputs:req(sha((out/e['path']).read_bytes())==e['sha256'],'original sixteen actual bytes changed')
 plan['recipe_id']='synexia-current-752-ci/4';plan['cic_producer_result_sha256']=a.result_sha;plan['cic_producer_output_seal']=seal;plan['declared_output_count']=19
 req(not any(g['path'] in EXPECTED for g in plan['guards']),'Cic must not change any retained guard')
 plan.pop('plan_sha256');plan['plan_sha256']=sha((json.dumps(plan,sort_keys=True,separators=(',',':'),ensure_ascii=True)+'\n').encode());write(task/'crate/plan.json',plan)
 mapping['outputsCount']=19;req(len(mapping['producerResultSHA256s'])==3,'three existing producer declarations');mapping['producerResultSHA256s'].append(a.result_sha);write(task/'PATH_MAPPING.json',mapping)
 for name in ['RESULT.json','PATCH.json','inputs.json','commands.json','runtime.json','junit.json','build-artifacts.json','spec.json']:copy(producer/name,task/'corpus-producer'/name)
 for p in old.files(producer/'chain'):copy(p,task/'corpus-producer/chain'/p.name)
 for name in ['OUTPUT.json','candidate.patch']:copy(generated/name,task/'corpus-producer'/name)
 copy(auth_path,task/'corpus-producer/AUTHORED.json')
 rt=R/'proof-runner/runtime-v4.py';copy(rt,task/'verify-runtime.py');pom=task/'pom.xml';pom.write_text(pom.read_text().replace('Sixteen-output current 752','Nineteen-output current 752').replace('sealed-sixteen-output-recipe-check','sealed-nineteen-output-recipe-check'))
 readme=task/'README.md';readme.write_text(readme.read_text().replace('transports sixteen actual outputs','transports nineteen actual outputs')+'\nThe fourth actual Cic producer repairs only the executable hostile REGEX escaping in A3Cases and its matching after resource, with the exact one-row manifest reconciliation. The separate hostile TEXT decoy is unchanged. All initial sixteen generated output bytes, public interfaces, test assertions, fixtures and schedules remain unchanged.\n')
 bind(base_path);bind(CORE);bind(Path(__file__).resolve());bind(rt);bind(R/'dependencies/acquisition-v3/RESULT.json');bind(R/'dependencies/acquire-v3.py')
 manifest['tools']['cache']=str(R/'dependencies/m2-v3');manifest['toolSets']['cache']=old.tool_inventory(Path(manifest['tools']['cache']))
 manifest['files']=old.inventory(out);manifest['producerOutputs']=sorted(manifest['producerOutputs'],key=lambda e:e['path']);manifest['externalInputs']=sorted(ext.values(),key=lambda e:e['path']);manifest['runtime']={'script':str(rt),'sha256':sha(rt.read_bytes()),'bytes':rt.stat().st_size}
 manifest['provenance'].update(declaredOutputCount=19,cicProducerResultSHA256=a.result_sha,cicProducerOutputSeal=seal,previousCandidateManifestSHA256=BASE_MANIFEST,allCurrentA3TestsRequired=True)
 write(a.manifest,manifest);print(json.dumps({'state':'MATERIALIZED_NINETEEN_ACTUAL_OUTPUTS_NOT_CONSUMER_QUALIFIED','sourceFiles':len(manifest['files']),'manifestSHA256':sha(a.manifest.read_bytes()),'planSHA256':sha((task/'crate/plan.json').read_bytes()),'actualOutputs':19,'firstSixteenPreserved':True}))

if __name__=='__main__':main()
