#!/usr/bin/env python3
"""Bounded owning-module Maven gate over actual SDK outputs on the current source."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,shutil,signal,subprocess,time
import xml.etree.ElementTree as E
p=argparse.ArgumentParser()
for name in ['repo','candidate','emitted','jdk','maven','out']:
    p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--timeout',type=int,default=600)
a=p.parse_args()
repo,candidate,emitted,jdk,maven,out=[x.resolve() for x in [a.repo,a.candidate,a.emitted,a.jdk,a.maven,a.out]]
out.mkdir(parents=True,exist_ok=False)
seals=json.loads((candidate/'SOURCE_SEALS.json').read_text())
for s in seals:
    path=s['target'];post=emitted/path;original=repo/path
    assert hashlib.sha256(post.read_bytes()).hexdigest()==s['after_sha256'],path+' emitted drift'
    if 'before_sha256' in s:
        assert hashlib.sha256(original.read_bytes()).hexdigest()==s['before_sha256'],path+' current drift'
    else:assert not original.exists(),path+' occupied current target'
module=repo/'m3/collections'
shutil.copytree(module/'src',out/'src')
for name in ['pom.xml','LICENSE','NOTICE','provenance.json','synexia-donation.json','name-mapping.json','SYNEXIA-DONATION-NOTICE.txt','NETBEANS-NOTICE.txt']:
    if (module/name).is_file():shutil.copy2(module/name,out/name)
for s in seals:
    relative=Path(s['target']).relative_to('m3/collections');dst=out/relative
    dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(emitted/s['target'],dst)
source_receipt={'source_kind':'ACTUAL_SDK_EMITTED_CURRENT_SEVEN','receiving_head':subprocess.check_output(['git','-C',str(repo),'rev-parse','HEAD'],text=True).strip(),'sdk_output':str(emitted),'sdk_execution_sha256':hashlib.sha256((emitted/'SDK_EXECUTION.tsv').read_bytes()).hexdigest(),'source_seals':seals,'source_files':[{'path':str(f.relative_to(out)),'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in sorted(out.rglob('*')) if f.is_file()]}
(out/'SOURCE_RECEIPT.json').write_text(json.dumps(source_receipt,indent=2)+'\n')
cmd=[str(maven/'bin/mvn'),'-B','-ntp','-o','-Dmaven.repo.local=/tmp/m3-collections-tools-20261019/m2','-DargLine=-Xmx512m','test']
env=os.environ.copy();env['JAVA_HOME']=str(jdk);env['PATH']=str(jdk/'bin')+':'+str(maven/'bin')+':/usr/local/bin:/usr/bin:/bin'
start=time.monotonic();started=datetime.datetime.now(datetime.timezone.utc).isoformat();timed_out=False
print('CURRENT_MODULE_STARTED main='+str(len(list((out/'src/main/java').rglob('*.java'))))+' test='+str(len(list((out/'src/test/java').rglob('*.java'))))+' timeout_seconds='+str(a.timeout),flush=True)
with (out/'MAVEN.log').open('w') as log:
    proc=subprocess.Popen(cmd,cwd=out,env=env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
    try:code=proc.wait(timeout=a.timeout)
    except subprocess.TimeoutExpired:
        timed_out=True;os.killpg(proc.pid,signal.SIGTERM)
        try:code=proc.wait(timeout=10)
        except subprocess.TimeoutExpired:os.killpg(proc.pid,signal.SIGKILL);code=proc.wait()
suites=[];failures=[];skips=[]
for report in sorted((out/'target/surefire-reports').glob('TEST-*.xml')):
    suite=E.parse(report).getroot();suites.append({k:suite.get(k) for k in ['name','tests','failures','errors','skipped','time']})
    for test in suite.findall('testcase'):
        for tag,target in [('failure',failures),('error',failures),('skipped',skips)]:
            for node in test.findall(tag):target.append({'suite':suite.get('name'),'test':test.get('name'),'kind':tag,'message':node.get('message'),'detail':node.text})
summary={'status':'TIMEOUT' if timed_out else ('PASS' if code==0 else 'FAIL'),'exit':code,'timeout_seconds':a.timeout,'seconds':round(time.monotonic()-start,3),'started_utc':started,'command':cmd,'source_kind':source_receipt['source_kind'],'suites':len(suites),'tests':sum(int(s['tests']) for s in suites),'failures':sum(int(s['failures']) for s in suites),'errors':sum(int(s['errors']) for s in suites),'skipped':sum(int(s['skipped']) for s in suites),'failure_details':failures,'skip_details':skips,'suite_details':suites}
(out/'JUNIT_SUMMARY.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps({k:v for k,v in summary.items() if k!='suite_details'},indent=2),flush=True)
raise SystemExit(0 if summary['status']=='PASS' else 1)
