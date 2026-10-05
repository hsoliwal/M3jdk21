#!/usr/bin/env python3
"""Materialize the admitted receiver only from the qualified recipe export."""
from pathlib import Path
import argparse,hashlib,json,os,shutil
R=Path(__file__).resolve().parent;C=R.parent;N=C.parent
SOURCE='9963cc08ff13922b92a0e3db7c30f56fddacba7d';TARGET='752191c9291f6467110fb8a7badfbdc4c2d41af2';TASK='m3/migration/intake-20261005/current-752-ci'
PRODUCER_SHA='d6d265a959e378fce47d68b5aa4a3a3bbee14bebf21355a54bd8077cb51e8630'
NATIVE_SHA='633b27c05776d03397eef4950ca7ae56f8f1b4f793666887aa72ab3e4c00cfc2'

def req(x,m):
 if not x:raise ValueError(m)
def sha(b):return hashlib.sha256(b).hexdigest()
def load(p):return json.loads(p.read_text())
def write(p,obj):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,indent=2,sort_keys=True)+'\n')
def copy(a,b):b.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(a,b)
def row(p):
 req(p.is_file() and not p.is_symlink(),'regular input required '+str(p));b=p.read_bytes();return {'path':str(p),'sha256':sha(b),'bytes':len(b)}
def blob(b):return hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()
def files(root):
 out=[]
 for p in root.rglob('*'):
  req(not p.is_symlink(),'no context symlinks')
  if p.is_file():out.append(p)
  else:req(p.is_dir(),'no special files')
 return sorted(out,key=lambda p:p.relative_to(root).as_posix())
def inventory(root):return [dict(path=p.relative_to(root).as_posix(),sha256=sha(p.read_bytes()),bytes=p.stat().st_size,gitBlob=blob(p.read_bytes())) for p in files(root)]
def tool_inventory(root):
 out=[]
 for p in sorted(root.rglob('*'),key=lambda x:x.relative_to(root).as_posix()):
  name=p.relative_to(root).as_posix()
  if p.is_symlink():out.append({'path':name,'symlink':os.readlink(p)})
  elif p.is_file():out.append({'path':name,'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size})
  else:req(p.is_dir(),'special tool file')
 return out

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path,required=True);ap.add_argument('--manifest',type=Path,required=True);a=ap.parse_args();out=a.output.absolute();req(not out.exists() and not a.manifest.exists(),'fresh materialization paths required')
 producer=C/'ci-repairs/evidence/producer-v2';result_path=producer/'RESULT.json';req(sha(result_path.read_bytes())==PRODUCER_SHA,'exact qualified producer result')
 result=load(result_path);req(result['status']=='PASS_FOCUSED_PRODUCER' and result['failure'] is None and result['sourceCommit']==SOURCE and result['targetCommit']==TARGET,'producer must pass')
 req(result['junit']['counts']=={'tests':9,'failures':0,'errors':0,'skipped':0},'exact9 producer methods')
 req(sha((producer/'inputs.json').read_bytes())==result['inputManifestSHA256'] and sha((producer/'PATCH.json').read_bytes())==result['patchSHA256'],'producer custody headers')
 ext={}
 def bind(p,identity=None):
  p=Path(p);actual=row(p)
  if identity is not None:req(actual==identity,'producer artifact/input drift '+str(p))
  if str(p) in ext:req(ext[str(p)]==actual,'ambiguous input identity')
  ext[str(p)]=actual
 for ident in load(producer/'inputs.json'):bind(ident['path'],ident)
 receipts=result['receipts'];req(len(receipts)==4,'four producer gates')
 for i,ident in enumerate(receipts):
  bind(ident['path'],ident);receipt=load(Path(ident['path']));req(receipt['stage']==['static','compile','tests','runtime'][i] and receipt['exitCode']==0 and receipt['inputManifestSHA256']==result['inputManifestSHA256'] and receipt['priorReceipts']==receipts[:i],'ordered producer receipts')
  for stream in ['stdout','stderr']:bind(receipt[stream]['path'],receipt[stream])
 bind(result['junit']['report']['path'],result['junit']['report']);bind(result['compiledArtifacts']['path'],result['compiledArtifacts'])
 for ident in load(Path(result['compiledArtifacts']['path'])):
  full=producer/'project/target'/ident['path'];bind(full,dict(ident,path=str(full)))
 for name in ['RESULT.json','PATCH.json','inputs.json','commands.json','spec.json','runtime.json','junit.json','tooling.json','tool-links.json']:bind(producer/name)
 runtime=result['runtime'];bind(runtime['output']['path'],runtime['output']);bind(runtime['patch']['path'],runtime['patch']);outputs=load(Path(runtime['output']['path']));generated=producer/'generated-candidate'
 expected={}
 for name in ['java','text']:
  p=C/('ci-repairs/'+name+'-authoring-v1/AUTHORED.json');bind(p);auth=load(p)
  req(auth['sourceCommit']==SOURCE and auth['targetCommit']==TARGET,'authoring source declaration')
  for e in auth['outputs']:req(e['path'] not in expected,'disjoint authoring families');expected[e['path']]=e
 req(len(expected)==5 and outputs['schema']=='synexia.ci-752.output/1' and outputs['sourceCommit']==SOURCE and outputs['targetCommit']==TARGET,'exact output protocol')
 req([e['path'] for e in outputs['outputs']]==sorted(expected),'sorted complete5 output paths')
 req({p.relative_to(generated).as_posix() for p in files(generated)}==set(expected)|{'OUTPUT.json','candidate.patch'},'exact output directory')
 seal=sha(('SYNEXIA-CI-752-OUTPUT/1\n'+''.join(e['path']+'\t'+e['sha256']+'\t'+str(e['bytes'])+'\n' for e in outputs['outputs'])).encode())
 req(seal==outputs['outputSeal']==runtime['outputSeal'] and outputs['outputs']==runtime['files'],'actual complete export seal')
 baseline=R/'receiving-unrepaired-v3';baseline_manifest=R/'acquisition/receiving-unrepaired-manifest-v3.json';bind(baseline_manifest);req(inventory(baseline)==load(baseline_manifest)['files'],'complete acquired receiving context')
 shutil.copytree(baseline,out);task=out/TASK;req(not task.exists(),'append-only task must be absent');task.mkdir(parents=True);planrows=[];mapping=[];outputrows=[]
 for i,e in enumerate(outputs['outputs']):
  p=e['path'];b=(baseline/p).read_bytes();g=generated/p;after=g.read_bytes();auth=expected[p]
  req(sha(b)==auth['beforeSha256'] and len(b)==auth['beforeBytes'],'current beforeimage bound');req(sha(after)==e['sha256']==auth['afterSha256'] and len(after)==e['bytes']==auth['afterBytes'],'actual output vs authored expectation')
  bind(g);(out/p).write_bytes(after)
  for state,data in [('before',b),('after',after)]:q=task/'crate'/state/(str(i)+'.txt');q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(data)
  planrows.append({'path':p,'before':{'resource':'before/'+str(i)+'.txt','sha256':sha(b)},'after':{'resource':'after/'+str(i)+'.txt','sha256':sha(after)}})
  outputrows.append(dict(e,gitBlob=blob(after),artifact=str(g)))
  mapping.append({'path':p,'sourceCommit':SOURCE,'targetCommit':TARGET,'beforeSHA256':sha(b),'beforeGitBlob':blob(b),'afterSHA256':sha(after),'afterGitBlob':blob(after),'bytes':len(after),'producerOutputSeal':seal,'classification':'existing-proof-fixture-repair' if p.endswith('.java') else ('host-CI-toolchain-selection' if p.startswith('.github/') else 'host-backports-validation-repair'),'productAPIChanged':False})
 guard_paths=['m3/migration/recipe.py','m3/migration/migration.py','m3/docs/name-mapping.json','m3/docs/name-mapping.schema.json','m3/tooling/migration-recipes/pom.xml','m3/backports/pom.xml','SYNEXIA_CONVERGENCE_MODEL.md']
 plan={'schema':'m3.sealed-install/1','recipe_id':'synexia-current-752-ci/1','source_commit':SOURCE,'target_commit':TARGET,'target_tree':'44feeb32f77a3442606ef3b63b923fd43732b20d','publication_head':'5c1ee9490e76943dfdb3ca27fade0d093d017226','preimage_context':'Exact 752 source with unchanged 569-path PR148 intake overlay, plus closed current Java/backports source-test closure. All five replacement preimages equal the exact 752 target bytes. Source commit records Synexia recipe provenance.','producer_result_sha256':PRODUCER_SHA,'producer_output_seal':seal,'guards':[{'path':p,'sha256':sha((out/p).read_bytes())} for p in guard_paths],'outputs':planrows}
 plan['plan_sha256']=sha((json.dumps(plan,sort_keys=True,separators=(',',':'),ensure_ascii=True)+'\n').encode());write(task/'crate/plan.json',plan)
 write(task/'PATH_MAPPING.json',{'schema':'m3.current-752-path-mapping/1','outputs':mapping,'outputsCount':5,'existingCanonicalRegistrySHA256':sha((out/'m3/docs/name-mapping.json').read_bytes()),'existingCanonicalRegistryModified':False,'old35OutputsRepinned':False,'canonicalProductionApplied':False,'productRuntimeAccepted':False})
 for name in ['RESULT.json','PATCH.json','inputs.json','commands.json','runtime.json','junit.json','build-artifacts.json','spec.json']:
  copy(producer/name,task/'producer'/name)
 for p in files(producer/'chain'):copy(p,task/'producer/chain'/p.name)
 for name in ['OUTPUT.json','candidate.patch']:copy(generated/name,task/'producer'/name)
 for p in files(C/'ci-repairs/task-packet'):bind(p);copy(p,task/'source-task-packet'/p.name)
 native_path=C/'native/publication-manifest.json';bind(native_path);req(sha(native_path.read_bytes())==NATIVE_SHA,'native delivery manifest')
 native=load(native_path);req(native['fileCount']==len(native['files'])==24,'closed native delivery')
 native_rows=[]
 for e in native['files']:
  f=Path(e['file']);bind(f);req(sha(f.read_bytes())==e['sha256'] and len(f.read_bytes())==e['bytes'] and blob(f.read_bytes())==e['sha'],'native source bytes')
  relative=e['path'].split('/native-current-v1/',1)[1];copy(f,task/'native-current-v1'/relative);native_rows.append({'sourcePath':e['path'],'receivingPath':TASK+'/native-current-v1/'+relative,'sha256':e['sha256'],'bytes':e['bytes'],'gitBlob':e['sha']})
 write(task/'native-delivery-mapping.json',{'sourceManifestSHA256':NATIVE_SHA,'files':native_rows,'byteIdentical':True,'nativeReexecutedByReceivingProof':False,'productRuntimeAccepted':False})
 copy(native_path,task/'native-publication-manifest.json')
 copy(R/'proof-runner/runtime.py',task/'verify-runtime.py')
 (task/'pom.xml').write_text('''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
<modelVersion>4.0.0</modelVersion><groupId>com.m3</groupId><artifactId>synexia-current-752-ci-task</artifactId><version>1</version><packaging>pom</packaging>
<name>Five-output current 752 CI installation task</name><properties><m3.python>python3</m3.python></properties>
<build><plugins><plugin><groupId>org.codehaus.mojo</groupId><artifactId>exec-maven-plugin</artifactId><version>3.6.4</version><executions><execution><id>sealed-five-output-recipe-check</id><phase>verify</phase><goals><goal>exec</goal></goals><configuration><executable>${m3.python}</executable><workingDirectory>${project.basedir}/../../../..</workingDirectory><arguments><argument>m3/migration/recipe.py</argument><argument>check</argument><argument>--plan</argument><argument>m3/migration/intake-20261005/current-752-ci/crate/plan.json</argument><argument>--root</argument><argument>.</argument></arguments></configuration></execution></executions></plugin></plugins></build></project>
''')
 (task/'README.md').write_text('''# Current 752 receiving task

This append-only task transports five actual outputs from the qualified Synexia producer at 9963cc08ff13922b92a0e3db7c30f56fddacba7d into the exact M3 752191c9291f6467110fb8a7badfbdc4c2d41af2 receiving context with the unchanged PR148 intake overlay. PATH_MAPPING.json records every path. The existing sealed installer owns apply, check, rollback and refusal behavior; the task POM checks its complete current state. No new migration engine is introduced.

The producer receipt covers real recipe execution, typed Java output parsing and bounded permutation/refusal controls. Full current migration-recipes and backports Maven verification and the retained intake tests are a separate receiving proof; its result is published separately after execution. Preserved producer metadata contains original absolute custody paths and is historical evidence, not a promise that its original toolchain is bundled.

The native-current-v1 directory contains the exact separately qualified native delivery bytes. Its mapping preserves the original evidence hashes; this receiver does not rerun those native commands. The existing 81-record registry and original 35-output receipts are unchanged and retain their original source/target pins. This task neither declares a JDK/HotSpot port nor grants runtime acceptance.
''')
 bind(R/'proof-runner/runtime.py');bind(Path(__file__).resolve());bind(C/'synexia/synexia-openrewrite-recipes/docs/20261005-m3-ci-admission.md')
 for p in [R/'dependencies/acquisition-v2/RESULT.json',R/'dependencies/READY.md',R/'acquisition/java-corpus-acquired.json',R/'acquisition/backports-external-acquired.json']:bind(p)
 tools={'javaHome':str(R/'dependencies/toolchain-v1/jdk-21+35'),'mavenHome':str(N.parent/'provider-superset-20261005/toolchain/apache-maven-3.9.9'),'cache':str(R/'dependencies/m2-v2'),'pythonEnv':str(N/'m3-python'),'python':str(N/'m3-python/bin/python'),'git':shutil.which('git')}
 bind(Path(tools['git']).resolve())
 sets={key:tool_inventory(Path(tools[root])) for key,root in [('java','javaHome'),('maven','mavenHome'),('cache','cache'),('python','pythonEnv')]}
 manifest={'schema':'m3.current-consumer-proof/1','source':{'commit':SOURCE,'tree':'6c055c584ebb5fa177420d1668db73bcb7e7fef4'},'receiving':{'commit':TARGET,'tree':'44feeb32f77a3442606ef3b63b923fd43732b20d'},'publicationHead':{'commit':'5c1ee9490e76943dfdb3ca27fade0d093d017226','tree':'f75369e0d15eb81ee0c8e1ae6e4fa62f767c42dc'},'files':inventory(out),'producerOutputs':outputrows,'externalInputs':sorted(ext.values(),key=lambda e:e['path']),'tools':tools,'toolSets':sets,'runtime':dict(script=str(R/'proof-runner/runtime.py'),sha256=sha((R/'proof-runner/runtime.py').read_bytes()),bytes=(R/'proof-runner/runtime.py').stat().st_size),'task':TASK,'provenance':{'producerResultSHA256':PRODUCER_SHA,'producerOutputSeal':seal,'nativePublicationManifestSHA256':NATIVE_SHA,'receivingAcquisitionManifestSHA256':sha(baseline_manifest.read_bytes()),'sourceFilesBeforeTask':len(inventory(baseline)),'oldQualifiedCommit':'8131f535300c005afd27443d0281ede11198522f','allCurrentRepositoryFilesCovered':False,'allCurrentMigrationRecipesAndBackportsTestsRequired':True}}
 write(a.manifest,manifest)
 print(json.dumps({'state':'MATERIALIZED_ACTUAL_EXPORT_NOT_CONSUMER_QUALIFIED','sourceFiles':len(manifest['files']),'taskFiles':len(files(task)),'manifestSHA256':sha(a.manifest.read_bytes()),'planSHA256':sha((task/'crate/plan.json').read_bytes()),'producerResultSHA256':PRODUCER_SHA,'externalInputs':len(ext)}))

if __name__=='__main__':main()
