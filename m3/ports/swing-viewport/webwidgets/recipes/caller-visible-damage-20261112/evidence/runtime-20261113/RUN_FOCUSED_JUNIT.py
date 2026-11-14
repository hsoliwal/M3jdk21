# SPDX-License-Identifier: Apache-2.0
# CPU validation controller, reusing the existing pinned native supervisor.
import ctypes as c,ctypes.wintypes as w,hashlib,json,os,pathlib,sys,time
ROOT=pathlib.Path(__file__).resolve().parent
CAP=33554432
RESERVE=8589934592
CLOSEOUT=131072
SUP=pathlib.Path(r'S:\codex-work\storage-recovery-20261013\native-process-cwd-recipe-20261016\templates\OwnedJobSupervisor.py')
SUP_HASH='7ca2eb41ba44b69969fc59f53cb333354f42a932ed31eb257c0682e7813c3b9c'
JAVA=pathlib.Path(r'C:\Program Files\Java\jdk-21')
MAVEN=pathlib.Path(r'C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14')
REPO=pathlib.Path(r'C:\Users\hsoliwal\.m2\repository')
k=c.WinDLL('kernel32',use_last_error=True)
k.GetCompressedFileSizeW.argtypes=[w.LPCWSTR,c.POINTER(w.DWORD)]
k.GetCompressedFileSizeW.restype=w.DWORD
class Memory(c.Structure):
 _fields_=[('length',w.DWORD),('load',w.DWORD)]+[(n,c.c_ulonglong) for n in ['totalPhys','availPhys','totalPageFile','availPageFile','totalVirtual','availVirtual','availExtendedVirtual']]
def sha(p):return hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()
def free(drive):
 value=c.c_ulonglong()
 if not k.GetDiskFreeSpaceExW(drive,None,None,c.byref(value)):raise c.WinError(c.get_last_error())
 return value.value
peak={'charged':0,'ordinaryLogBytes':0}
def check(final=False):
 b=c.create_unicode_buffer(32768)
 assert k.GetTempPathW(32768,b) and b.value.rstrip('\\').lower()==r'K:\codex-work\agent-temp'.lower(),'native K temp'
 assert free('K:\\')>=RESERVE,'K reserve'
 charged=logical=logbytes=0
 for p in ROOT.rglob('*'):
  assert not p.is_symlink(),'symlink'
  if p.is_file():
   n=p.stat().st_size; high=w.DWORD();c.set_last_error(0)
   low=k.GetCompressedFileSizeW(str(p),c.byref(high))
   if low==0xffffffff and c.get_last_error():raise c.WinError(c.get_last_error())
   logical+=n;charged+=max(n,(high.value<<32)|low)
   if p.suffix=='.log':
    assert n<=65536,'per-log cap'
    logbytes+=max(n,(high.value<<32)|low)
 assert logbytes<=1048576,'total logs cap'
 assert charged<=CAP-(0 if final else CLOSEOUT),'charged task cap'
 assert free('S:\\')>=RESERVE+CAP-charged,'S after planned output'
 m=Memory();m.length=c.sizeof(m);assert k.GlobalMemoryStatusEx(c.byref(m)),'memory query'
 assert m.availPhys>=1610612736 and m.availPageFile>=8858370048,json.dumps({'memoryReserve':'FAIL','physicalAvailable':m.availPhys,'commitAvailable':m.availPageFile})
 peak['charged']=max(peak['charged'],charged);peak['ordinaryLogBytes']=max(peak['ordinaryLogBytes'],logbytes)
 return {'logical':logical,'charged':charged,'sFree':free('S:\\'),'kFree':free('K:\\'),'physicalAvailable':m.availPhys,'commitAvailable':m.availPageFile,'peak':dict(peak)}
def put(name,value):
 data=(json.dumps(value,sort_keys=True,indent=2)+'\n').encode('utf-8');assert len(data)<=32768
 with (ROOT/name).open('xb') as f:f.write(data);f.flush();os.fsync(f.fileno())
# Focused, pre-existing JUnit contracts; no application source is generated here.
phase=json.loads((ROOT/'PHASE.json').read_text(encoding='utf-8'))
assert phase['scope']=='caller-recipe-runtime-followup' and phase['capBytes']==CAP
job=sys.argv[1]
assert job in ['compile-focused','compile-oracle','caller7','public4','approved18']
log=ROOT/'logs'/('junit-'+job+'.log')
assert not log.exists() and not (ROOT/'reports'/('junit-'+job+'.json')).exists(),'closed job label'
JUNIT=REPO/'org/junit/platform/junit-platform-console-standalone/1.10.1/junit-platform-console-standalone-1.10.1.jar'
assert sha(JUNIT)=='b42eaa53d13576d17db5fb8b280722a6ae9e36daf95f4262bc6e96d4cb20725f'
assert sha(JAVA/'bin/java.exe')=='d27c1a75edf380963c430232e93340eab4bd3694044976cff85421bf163b876c'
assert sha(JAVA/'bin/javac.exe')=='8a1d32f529ba4d01940194e3b06d14aff828e5d47f5f374ec88c81902fc3456f'
assert (ROOT/'qualify-001/selected-producer/PASS.sentinel').is_file()
classes=ROOT/'qualify-001/selected-producer/after-classes'
focused=ROOT/'focused-tests/classes'
oracle=ROOT/'oracle-tests/classes'
empty=ROOT/'qualify-001/selected-producer/empty-classpath'
cp=os.pathsep.join([str(classes),str(JUNIT)])
source=ROOT/'receiver/m3/ports/swing-viewport/src/test/java/jdk/m3/swing'
if job.startswith('compile-'):
 if job=='compile-focused':
  output=focused
  inputs=[source/'web/model/M3CollectionChecks.java',source/'web/model/M3CollectionModesTest.java',source/'viewport/M3ViewportCoreApiTest.java']
 else:
  output=oracle
  inputs=[ROOT/'oracle-src/M3ViewportCoreApiTest.java']
  assert sha(inputs[0])=='ba22b2bf42b059ab93d906383d52c16b96be6f2cdbd3e49bade4ed3d473e8522'
 assert not output.exists(),'fresh compiler output'
 output.mkdir(parents=True)
 argv=[str(JAVA/'bin/javac.exe'),'-J-Xmx128m','-J-XX:+UseSerialGC','-J-XX:ActiveProcessorCount=2','-J-Djava.io.tmpdir='+str(ROOT/'tmp'),'-J-Duser.home='+str(ROOT/'home'),'--release','21','-proc:none','-implicit:none','-encoding','UTF-8','-classpath',cp,'-sourcepath',str(empty),'-Xlint:all','-Werror','-d',str(output)]+[str(p) for p in inputs]
else:
 assert focused.is_dir()
 if job=='approved18':assert oracle.is_dir()
 outputs=[oracle,focused,classes,JUNIT] if job=='approved18' else [focused,classes,JUNIT]
 cp=os.pathsep.join(map(str,outputs))
 select='jdk.m3.swing.web.model.M3CollectionModesTest' if job=='caller7' else 'jdk.m3.swing.viewport.M3ViewportCoreApiTest'
 argv=[str(JAVA/'bin/java.exe'),'-Xmx128m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:ReservedCodeCacheSize=32m','-Djava.awt.headless=true','-Duser.home='+str(ROOT/'home'),'-Djava.io.tmpdir='+str(ROOT/'tmp'),'-cp',cp,'org.junit.platform.console.ConsoleLauncher','execute','--disable-banner','--disable-ansi-colors','--details=summary','--fail-if-no-tests','--select-class',select]
before=check()
data=SUP.read_bytes();assert hashlib.sha256(data).hexdigest()==SUP_HASH
ns={'__name__':'owned_job_supervisor'};exec(compile(data,str(SUP),'exec'),ns);ns['ROOT']=ROOT
env=dict(os.environ)
for n in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','MAVEN_OPTS','MAVEN_ARGS','CLASSPATH']:env.pop(n,None)
env.update(TEMP=r'K:\codex-work\agent-temp',TMP=r'K:\codex-work\agent-temp',JAVA_HOME=str(JAVA),USERPROFILE=str(ROOT/'home'),XDG_CACHE_HOME=str(ROOT/'cache'))
started=time.time()
result=ns['run_owned'](argv,env,log,check,90,outcome={})
afterError=None
try:after=check(final=True)
except BaseException as error:after={'status':'HOLD'};afterError=type(error).__name__+': '+str(error)[:400]
text=log.read_text(encoding='utf-8',errors='replace')
counts={}
if not job.startswith('compile-'):
 import re
 for label in ['found','started','successful','failed','skipped','aborted']:
  found=re.findall(r'\[\s*(\d+) tests '+label+r'\s*\]',text)
  if len(found)==1:counts[label]=int(found[0])
record={'schema':'m3.owned-cpu-qualification/v1','job':job,'command':argv,'startedUnix':started,'endedUnix':time.time(),'before':before,'after':after,'postGuardError':afterError,'native':result,'controllerSha256':sha(__file__),'runner':'JUnit Platform Console standalone1.10.1; distinct from hosted vendored1.10.2','runnerSha256':sha(JUNIT),'counts':counts,'logBytes':log.stat().st_size,'logSha256':sha(log),'productionPromotion':False}
put('reports/junit-'+job+'.json',record)
print(json.dumps(record,sort_keys=True),flush=True)
print(text[-18000:],flush=True)
if afterError or not result.get('terminal') or not result.get('all_owned_native_handles_closed') or result.get('supervisor_error'):sys.exit(125)
sys.exit(result['exitcode'])
