#!/usr/bin/env python3
"""Run the unchanged original A3 tests with the exact current owner/resource closure."""
from pathlib import Path
import datetime,hashlib,json,os,subprocess,xml.etree.ElementTree as ET
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
def utc():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def ident(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'git_blob':hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()}
def write(p,obj):
 with p.open('x') as f:json.dump(obj,f,indent=2);f.write('\n')
def check():
 d=json.loads((HERE/'INPUTS.json').read_text())
 for row in d['rows']:
  assert ident(Path(row['path']))['sha256']==row['sha256'],row['path']
 return len(d['rows'])
def main():
 count=check();work=HERE/'workspace';pom=work/'m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006/a3-behavior-verification/pom.xml'
 env=os.environ.copy();env['JAVA_HOME']=str(ROOT/'toolchain/jdk-21+35');env['PATH']=env['JAVA_HOME']+'/bin:'+env.get('PATH','')
 cmd=[str(ROOT/'toolchain/apache-maven-3.9.9/bin/mvn'),'-o','-B','-ntp','-Dmaven.repo.local='+str(HERE/'cache'),'-f',str(pom),'test']
 request={'schema':'m3-a3-behavior-process/1','argv':cmd,'cwd':str(work),'java_home':env['JAVA_HOME'],'start_utc':utc(),'inputs':ident(HERE/'INPUTS.json'),'runner':ident(Path(__file__)),'staged_input_count':count,'scope':'Four unchanged original A3 recipe tests; no whole-module, foundation workflow, runtime/JNI or JDK build qualification.'}
 write(HERE/'MAVEN_REQUEST.json',request)
 with (HERE/'maven.log').open('xb') as log:p=subprocess.run(cmd,cwd=work,env=env,stdout=log,stderr=subprocess.STDOUT)
 request.update({'finish_utc':utc(),'exit_code':p.returncode,'log':ident(HERE/'maven.log'),'inputs_unchanged_after':check()})
 write(HERE/'MAVEN_PROCESS.json',request)
 reports=[]
 for path in sorted((pom.parent/'target/surefire-reports').glob('TEST-*.xml')):
  e=ET.parse(path).getroot();reports.append({'identity':ident(path),'attributes':e.attrib,'testcases':[{'attributes':c.attrib,'failures':[{'type':f.tag,'attributes':f.attrib,'text':f.text} for f in c if f.tag in ('failure','error','skipped')]} for c in e.findall('testcase')]})
 result={'schema':'m3-a3-behavior-execution/1','status':'PASS' if p.returncode==0 else 'FAIL','original_methods':sum(len(r['testcases']) for r in reports),'process':request,'junit_reports':reports,'library_versions':{'rewrite':'8.17.1','junit_jupiter':'5.10.2','maven':'3.9.9','jdk':'21+35'},'original_test_bodies_unchanged':True,'retained_production_owners_unchanged':True,'whole_module_or_jni_qualification':False}
 write(HERE/'EXECUTION.json',result)
 print(json.dumps({'status':result['status'],'methods':result['original_methods'],'receipt':ident(HERE/'EXECUTION.json')}))
 return p.returncode
if __name__=='__main__':raise SystemExit(main())
