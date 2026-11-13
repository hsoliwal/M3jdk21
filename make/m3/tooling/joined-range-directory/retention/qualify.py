# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Execute emitted test data with existing Maven/JUnit and exact component bytes."""
from pathlib import Path
import argparse,hashlib,json,os,re,shutil,subprocess,time,signal,xml.etree.ElementTree as ET,zipfile
def identity(b):return dict(bytes=len(b),sha256=hashlib.sha256(b).hexdigest(),gitBlob=hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest())
def verify_payloads(root,sources,snapshot):
 for name,raw in snapshot.items():
  assert (root/name).read_bytes()==raw,'emitted input changed: '+name
  if name.endswith('.java.txt'):
   assert (sources/name[:-4]).read_bytes()==raw,'copied compilation input changed: '+name
def terminate_owned(p):
 if os.name=='nt':
  if p.poll() is None:subprocess.run(['taskkill','/PID',str(p.pid),'/T','/F'],capture_output=True,timeout=15,check=False)
  p.wait(timeout=15);return
 for sig,seconds in [(signal.SIGTERM,2),(signal.SIGKILL,3)]:
  try:os.killpg(p.pid,sig)
  except ProcessLookupError:pass
  deadline=time.monotonic()+seconds
  while time.monotonic()<deadline:
   p.poll()
   try:os.killpg(p.pid,0)
   except ProcessLookupError:p.wait(timeout=5);return
   time.sleep(.05)
 p.wait(timeout=5)
 raise RuntimeError('owned process group terminal state unproved')
def main():
 ap=argparse.ArgumentParser()
 for n in ['task-root','out','jdk-home','maven','repo-head','repo-tail']:ap.add_argument('--'+n,type=Path,required=True)
 a=ap.parse_args();T=a.task_root.resolve();O=a.out.resolve();H=Path(__file__).resolve().parent
 snapshot={n:(H/n).read_bytes() for n in ['INPUTS.json','M3JoinedRetentionTest.java.txt','ForceGC.java.txt','retention-pom.xml','qualify.py']}
 inputs=json.loads(snapshot['INPUTS.json']);jar=T/inputs['jarRelative']
 assert identity(jar.read_bytes())==inputs['jar']
 assert O.is_relative_to(T) and not O.exists()
 samples=[]
 def used():return sum((p.stat().st_size+4095)//4096*4096 for p in T.rglob('*') if p.is_file() and not p.is_symlink())
 def gate(planned=16*1024):
  value=used();samples.append(value);assert value+planned<536870912 and shutil.disk_usage(T).free-planned>8*1024**3,'storage cap/reserve'
 gate(160*1024)
 release=(a.jdk_home/'release').read_text();assert re.search(r'^JAVA_VERSION="21[."]',release,re.M),'JDK21 required'
 with zipfile.ZipFile(jar) as z:
  for row in inputs['classes']:assert identity(z.read(row['path']))=={k:row[k] for k in ['bytes','sha256','gitBlob']}
 O.mkdir();src=O/'sources';src.mkdir()
 for name in ['M3JoinedRetentionTest','ForceGC']:
  raw=snapshot[name+'.java.txt'];(src/(name+'.java')).write_bytes(raw)
 verify_payloads(H,src,snapshot)
 for name in ['temp','home','build']:(O/name).mkdir()
 env=dict(os.environ)
 for name in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','CLASSPATH']:env.pop(name,None)
 env.update(JAVA_HOME=str(a.jdk_home),TEMP=str(O/'temp'),TMP=str(O/'temp'),TMPDIR=str(O/'temp'),MAVEN_USER_HOME=str(O/'home/.m2'),MAVEN_OPTS='-Xmx384m -XX:-UsePerfData -Djava.io.tmpdir='+str(O/'temp')+' -Duser.home='+str(O/'home'))
 env['PATH']=str(a.jdk_home/'bin')+os.pathsep+env['PATH']
 modes={'jit':[],'int':['-Xint'],'nocompact':['-XX:-CompactStrings'],'c2':['-Xbatch','-XX:-TieredCompilation']}
 expected={'discardedLeafBackingsCollectWhileTailViewAndArenaLive','liveSourceNegativeControlPreventsCollectionUntilSourceReleased','withinLeafBackingLivesUntilTheTinyViewIsReleased','emptyJoinedViewRetainsNoLeafBacking'}
 markers=['M3_JOINED_GC_DROPPED count='+str(n)+' cleared='+str(n-1)+' selectedBackingBytes=256 viewUnits=1' for n in [32,1024,8192]]
 markers+=['M3_JOINED_GC_NEGATIVE_CONTROL rooted=63 released=63','M3_JOINED_GC_LOCAL backingBytes=1048576 viewUnits=1 releaseCleared=true']
 markers+=['M3_JOINED_GC_POSITIVE_CONTROL scenario=source','M3_JOINED_GC_POSITIVE_CONTROL scenario=local']
 markers+=['M3_JOINED_GC_EMPTY count='+str(n)+' cleared='+str(n) for n in [2,1024]]
 records=[];classPins=None
 for mode,extra in modes.items():
  gate();verify_payloads(H,src,snapshot);reports=O/('reports-'+mode)
  argv=[str(a.maven),'-o','-B','-ntp','-f',str(H/'retention-pom.xml'),'-Dmaven.repo.local='+str(a.repo_head),'-Dmaven.repo.local.tail='+str(a.repo_tail),'-Dmaven.repo.local.tail.ignoreAvailability=false','-Dm3.retention.module.jar='+str(jar),'-Dm3.retention.build='+str(O/'build'),'-Dm3.retention.sources='+str(src),'-Dm3.retention.reports='+str(reports),'-Dm3.retention.temp='+str(O/'temp'),'-Dm3.retention.home='+str(O/'home'),'-Dm3.jvm.extra='+' '.join(extra),'test']
  start=time.monotonic();log=O/(mode+'.log')
  with log.open('xb') as stream:
   p=subprocess.Popen(argv,cwd=O,env=env,stdin=subprocess.DEVNULL,stdout=stream,stderr=subprocess.STDOUT,start_new_session=(os.name!='nt'))
   try:
    while p.poll() is None:
     gate()
     if time.monotonic()-start>180:raise TimeoutError(mode)
     time.sleep(1)
    code=p.wait()
   except BaseException:
    terminate_owned(p)
    raise
  verify_payloads(H,src,snapshot)
  assert log.stat().st_size<64*1024
  command=dict(mode=mode,argvSha256=hashlib.sha256(json.dumps(argv,separators=(',',':')).encode()).hexdigest(),exit=code,parentReaped=True,log=log.name,logIdentity=identity(log.read_bytes()),seconds=time.monotonic()-start)
  with (O/'commands.jsonl').open('a') as f:f.write(json.dumps(command)+'\n')
  assert code==0,(mode,log.read_text(errors='replace')[-1800:])
  files=list(reports.glob('TEST-*.xml'));assert len(files)==1
  raw=files[0].read_bytes();xml=ET.fromstring(raw)
  assert xml.attrib['tests']=='4' and xml.attrib['failures']==xml.attrib['errors']==xml.attrib['skipped']=='0'
  assert {r.attrib['name'] for r in xml.findall('testcase')}==expected
  output='\n'.join(r.text or '' for r in xml.findall('.//system-out'))
  assert all(output.splitlines().count(m)==1 for m in markers),(mode,output)
  compiled={p.relative_to(O/'build/test-classes').as_posix():identity(p.read_bytes()) for p in (O/'build/test-classes').rglob('*.class')}
  if classPins is None:classPins=compiled
  else:assert classPins==compiled
  assert identity(jar.read_bytes())==inputs['jar']
  records.append(dict(mode=mode,tests=4,xml=str(files[0].relative_to(O)),xmlIdentity=identity(raw),markers=markers,compiledClassesUnchanged=True))
  gate()
 result=dict(schema='M3_JOINED_REACHABILITY_QUALIFICATION_V1',status='PASS_EXISTING_JOINED_GC_WITNESSES_FOUR_MODES',junitTests=16,recipeTests='separate five SDK replay tests',modes=records,subject=inputs,compiledTestClasses=classPins,sourcePayloads={n:identity(raw) for n,raw in snapshot.items()},copiedSourceIdentities={p.name:identity(p.read_bytes()) for p in src.glob('*.java')},payloadsUnchangedThroughCompletion=True,selectedBackingIdentityObserved=True,positiveGcWitnessWhileRooted=True,posixCleanup='OWNED_GROUP_TERM_KILL_AND_PARENT_REAP; runtime not executed on POSIX in this Windows qualification',selectedWholeLeafRetentionObserved=True,discardedOwnedArrayCollectabilityObserved=True,rootedNegativeControlPassed=True,emptyViewCollectabilityObserved=True,totalRetainedHeapBytes='NOT_MEASURED',gcLivenessGuarantee='NOT_CLAIMED',context='Java classpath component, exact current class bodies; module encapsulation and JNI not qualified',storage=dict(cap=536870912,observedSampleMaximum=max(samples),samples=len(samples),continuousPeak='NOT_MEASURED'),authority=False)
 verify_payloads(H,src,snapshot)
 (O/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n');gate();print(json.dumps(dict(status=result['status'],junitTests=16,modes=4)))
if __name__=='__main__':main()
