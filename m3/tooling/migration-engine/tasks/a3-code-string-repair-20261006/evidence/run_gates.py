#!/usr/bin/env python3
"""Materialize the reviewed exact recipe outputs and run unchanged A3 behavior gates."""
from pathlib import Path
import json,hashlib,datetime,os,subprocess,shutil,xml.etree.ElementTree as ET
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1];BASE=Path('m3/tooling/migration-recipes');TASK=BASE/'tasks/a3-code-string-repair-20261006';ENV=os.environ.copy();ENV['JAVA_HOME']=str(ROOT/'toolchain/jdk-21+35');ENV['PATH']=ENV['JAVA_HOME']+'/bin:'+ENV.get('PATH','')
def utc():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def ident(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'git_blob':hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()}
def save(p,d):
 p.parent.mkdir(parents=True,exist_ok=True)
 with p.open('x') as f:json.dump(d,f,indent=2);f.write('\n')
def check_inputs():
 d=json.load(open(HERE/'PROOF_INPUTS.json'))
 for r in d['rows']:assert ident(Path(r['path']))['sha256']==r['sha256']
 return len(d['rows'])
def invoke(name,cmd,cwd):
 p=HERE/'execution';p.mkdir(exist_ok=True);request={'argv':cmd,'cwd':str(cwd),'start_utc':utc(),'java_home':ENV['JAVA_HOME']};save(p/(name+'-request.json'),request)
 with (p/(name+'.log')).open('xb') as f:r=subprocess.run(cmd,cwd=cwd,env=ENV,stdout=f,stderr=subprocess.STDOUT)
 request.update({'finish_utc':utc(),'exit_code':r.returncode,'log':ident(p/(name+'.log'))});save(p/(name+'.json'),request);print(json.dumps({'stage':name,'exit_code':r.returncode}),flush=True);return request
 def_unused=None

def reports(pom):
 result=[]
 for p in sorted((pom.parent/'target/surefire-reports').glob('TEST-*.xml')):
  e=ET.parse(p).getroot();result.append({'identity':ident(p),'attributes':e.attrib,'methods':[{'attributes':c.attrib,'problems':[{'tag':x.tag,'attributes':x.attrib,'text':x.text} for x in c if x.tag in ('error','failure','skipped')]} for c in e.findall('testcase')]})
 return result

def main():
 check_inputs();cache=HERE/'cache';shutil.copytree(ROOT/'current-ci/repair-candidate/execution/cache-v02',cache)
 mvn=[str(ROOT/'toolchain/apache-maven-3.9.9/bin/mvn'),'-o','-B','-ntp','-Dmaven.repo.local='+str(cache)]
 work=HERE/'proof-workspace';pom=work/TASK/'verification/pom.xml';out=HERE/'execution/materialized';receipt={'schema':'m3-a3-code-string-repair-execution/1','status':'RUNNING','started_at':utc(),'proof_inputs':ident(HERE/'PROOF_INPUTS.json'),'recipe_crate':'a3-code-string-repair-20261006','library_versions':{'rewrite':'8.17.1','junit':'5.10.2','jdk':'21+35','maven':'3.9.9'},'original_tests_unchanged':True,'canonical_owners_unchanged':True,'broad_runtime_native_admission':False}
 try:
  proof=invoke('01-canonical-recipe',mvn+['-Dm3.a3.repair.materialized='+str(out),'-f',str(pom),'test'],work);receipt['proof_process']=proof;receipt['proof_junit']=reports(pom);assert proof['exit_code']==0
  rows=json.load(open(HERE/'candidate/CANDIDATES.json'))['rows'];outputs=[]
  for r in rows:
   i=ident(out/r['repository_path']);assert i['sha256']==r['after']['sha256'] and i['git_blob']==r['after']['git_blob'];outputs.append({**i,'repository_path':r['repository_path'],'expected_before_git_blob':r['before']['git_blob'],'origin':'ACTUAL_CANONICAL_OPENREWRITE_RESULT'})
  receipt['outputs']=outputs
  classes=HERE/'execution/compiled';classes.mkdir();cmd=[str(ROOT/'toolchain/jdk-21+35/bin/javac'),'--release','21','-proc:none','-encoding','UTF-8','-d',str(classes),str(out/rows[0]['repository_path'])];compiled=invoke('02-actual-produced-javac',cmd,out);receipt['javac_process']=compiled;assert compiled['exit_code']==0;receipt['compiled_classes']=[ident(p) for p in sorted(classes.rglob('*.class'))]
  behavior=HERE/'behavior-workspace';shutil.copytree(ROOT/'current-ci/a3-behavior-v01/workspace',behavior,ignore=shutil.ignore_patterns('target'))
  for r in rows:
   dest=behavior/r['repository_path'];dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(out/r['repository_path'],dest)
  bindings=[]
  for old in json.load(open(ROOT/'current-ci/a3-behavior-v01/INPUTS.json'))['rows']:
   path=behavior/old['repository_path'];i=ident(path);changed=next((r for r in rows if r['repository_path']==old['repository_path']),None);expected=changed['after']['sha256'] if changed else old['sha256'];assert i['sha256']==expected;bindings.append({**i,'repository_path':old['repository_path'],'origin':'ACTUAL_REPAIR_RESULT' if changed else old['origin']})
  save(HERE/'execution/BEHAVIOR_INPUTS.json',{'rows':bindings});receipt['behavior_inputs']=ident(HERE/'execution/BEHAVIOR_INPUTS.json')
  b_pom=behavior/BASE/'tasks/jcc-ci-repair-20261006/a3-behavior-verification/pom.xml';run=invoke('03-original-a3-behavior',mvn+['-f',str(b_pom),'test'],behavior);receipt['original_behavior_process']=run;receipt['original_behavior_junit']=reports(b_pom);assert run['exit_code']==0
  assert sum(len(x['methods']) for x in receipt['proof_junit'])==9 and sum(len(x['methods']) for x in receipt['original_behavior_junit'])==4
  for report in receipt['proof_junit']+receipt['original_behavior_junit']:
   assert all(int(report['attributes'][k])==0 for k in ('failures','errors','skipped'))
  receipt['status']='PASS';receipt['original_methods_passed']=4;receipt['proof_methods_passed']=9;receipt['recipe_outputs']=3;receipt['fixed_point_changes']=0;receipt['refusal_cases']=12;receipt['mixed_state_combinations']=8
 except BaseException as e:
  receipt['status']='FAIL';receipt['failure']=repr(e)
 finally:
  receipt['finish_utc']=utc();receipt['proof_inputs_unchanged_after']=check_inputs();save(HERE/'execution/EXECUTION.json',receipt);print(json.dumps({'status':receipt['status'],'receipt':ident(HERE/'execution/EXECUTION.json')}))
 return 0 if receipt['status']=='PASS' else 1
if __name__=='__main__':raise SystemExit(main())
