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
# CPU-only full existing module compiler and JUnit qualification.
phase=json.loads((ROOT/'PHASE.json').read_text(encoding='utf-8'))
assert phase['capBytes']==CAP and phase['scope']=='caller-recipe-runtime-followup'
job=sys.argv[1]
assert job in ['suite-002']
log=ROOT/'logs'/('full-'+job+'.log');receipt=ROOT/'reports'/('full-'+job+'.json')
assert not log.exists() and not receipt.exists(),'closed or previous job label'
manifest=ROOT/'FULL_SUITE_INPUTS.json'
assert sha(manifest)=='bb88e30d2bf58ea0cb1fdf5ea19f79393f9e0f56985b56cd95e0cabfd18e7b00'
bound=json.loads(manifest.read_text(encoding='utf-8'))
assert bound['receiverHead']=='41dbfee857379dc7eaee961d37332dc89b83f37d' and len(bound['sourceFiles'])==458
source=ROOT/'full-suite-source'
for row in bound['sourceFiles']:
 p=source/pathlib.PurePosixPath(row['path']);assert p.stat().st_size==row['bytes'] and sha(p)==row['sha256'],'source drift '+row['path']
JUNIT=REPO/'org/junit/platform/junit-platform-console-standalone/1.10.1/junit-platform-console-standalone-1.10.1.jar'
assert sha(JUNIT)=='b42eaa53d13576d17db5fb8b280722a6ae9e36daf95f4262bc6e96d4cb20725f'
assert sha(JAVA/'bin/java.exe')=='d27c1a75edf380963c430232e93340eab4bd3694044976cff85421bf163b876c'
assert sha(JAVA/'bin/javac.exe')=='8a1d32f529ba4d01940194e3b06d14aff828e5d47f5f374ec88c81902fc3456f'
build=ROOT/'full-suite-output'
main=build/'main';tests=build/'test'
for label,expected in [('main-001','e9742cfcf4c59f4d958b54d42adaaea794af334e6041506b18e305c6246c07e9'),('tests-001','13503124718333c395b65491f1043f9eddb000a4f6647f9e851fe9f623dcf276')]:
 p=ROOT/(label+'.classes.json');assert sha(p)==expected,'compiled manifest drift'
 for row in json.loads(p.read_text()):assert sha(build/pathlib.PurePosixPath(row['path']))==row['sha256'],'compiled byte drift'
before=check()
baseJvm=['-Xmx512m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:ReservedCodeCacheSize=32m','-Djava.io.tmpdir='+str(ROOT/'tmp'),'-Duser.home='+str(ROOT/'home'),'-Djava.util.prefs.userRoot='+str(ROOT/'home/prefs'),'-Djava.util.prefs.systemRoot='+str(ROOT/'home/system-prefs')]
if job in ['main-001','tests-001']:
 out=main if job=='main-001' else tests
 assert not out.exists(),'fresh compiler destination';out.mkdir(parents=True)
 args=['--release','21','-proc:none','-implicit:none','-encoding','UTF-8','-sourcepath',str(ROOT/'qualify-001/selected-producer/empty-classpath'),'-d',str(out)]
 if job=='main-001':
  args+=['-classpath',str(ROOT/'qualify-001/selected-producer/empty-classpath'),'-Xlint:all,-serial,-this-escape','-Werror']
  selected=[r for r in bound['sourceFiles'] if r['path'].startswith('src/main/java/')]
  assert len(selected)==307
 else:
  assert (ROOT/'reports/full-main-001.json').is_file()
  assert json.loads((ROOT/'reports/full-main-001.json').read_text())['native']['exitcode']==0
  args+=['-classpath',os.pathsep.join([str(main),str(JUNIT)]),'-Xlint:all,-serial,-try,-this-escape']
  selected=[r for r in bound['sourceFiles'] if r['path'].startswith('src/test/java/')]
  assert len(selected)==151
 args+=[str(source/pathlib.PurePosixPath(row['path'])) for row in selected]
 argfile=ROOT/(job+'.javac.args')
 with argfile.open('x',encoding='utf-8',newline='\n') as f:
  for arg in args:f.write('"'+arg.replace('\\','/').replace('"','\\"')+'"\n')
 argv=[str(JAVA/'bin/javac.exe')]+['-J'+opt for opt in baseJvm]+['@'+str(argfile)]
else:
 assert json.loads((ROOT/'reports/full-tests-001.json').read_text())['native']['exitcode']==0
 # Scan the complete test output using the repository's existing launcher.
 argv=[str(JAVA/'bin/java.exe')]+[a if a!='-Xmx512m' else '-Xmx256m' for a in baseJvm]+['-Djava.awt.headless=true','-cp',os.pathsep.join([str(main),str(tests),str(JUNIT)]),'jdk.m3.swing.testkit.M3JUnitLauncher','--scan',str(tests)]
data=SUP.read_bytes();assert hashlib.sha256(data).hexdigest()==SUP_HASH
ns={'__name__':'owned_job_supervisor'};exec(compile(data,str(SUP),'exec'),ns);ns['ROOT']=ROOT
env=dict(os.environ)
for n in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','MAVEN_OPTS','MAVEN_ARGS','CLASSPATH']:env.pop(n,None)
env.update(TEMP=r'K:\codex-work\agent-temp',TMP=r'K:\codex-work\agent-temp',JAVA_HOME=str(JAVA),USERPROFILE=str(ROOT/'home'),XDG_CACHE_HOME=str(ROOT/'cache'))
started=time.time();result=ns['run_owned'](argv,env,log,check,180,outcome={})
afterError=None
try:after=check(final=True)
except BaseException as error:after={'status':'HOLD'};afterError=type(error).__name__+': '+str(error)[:400]
text=log.read_text(encoding='utf-8',errors='replace')
counts={}
if job=='suite-002':
 import re
 for label in ['found','started','successful','failed','skipped','aborted']:
  values=re.findall(r'\[\s*(\d+) tests '+label+r'\s*\]',text)
  if len(values)==1:counts[label]=int(values[0])
classFiles=[]
if job!='suite-002':
 for p in sorted(out.rglob('*.class')):classFiles.append({'path':p.relative_to(build).as_posix(),'bytes':p.stat().st_size,'sha256':sha(p)})
 classManifest=ROOT/(job+'.classes.json')
 with classManifest.open('x',encoding='utf-8',newline='\n') as f:json.dump(classFiles,f,sort_keys=True,indent=2);f.write('\n')
for row in bound['sourceFiles']:assert sha(source/pathlib.PurePosixPath(row['path']))==row['sha256'],'post-run source drift'
record={'schema':'m3.owned-cpu-qualification/v1','job':'full-'+job,'command':argv,'startedUnix':started,'endedUnix':time.time(),'before':before,'after':after,'postGuardError':afterError,'native':result,'controllerSha256':sha(__file__),'sourceManifestSha256':sha(manifest),'sourceCount':len(selected) if job!='suite-002' else 458,'runner':'Repository M3JUnitLauncher with Console standalone1.10.1 libraries; distinct from hosted vendored1.10.2','runnerSha256':sha(JUNIT),'counts':counts,'classCount':len(classFiles),'classBytes':sum(p['bytes'] for p in classFiles),'classManifestSha256':sha(classManifest) if job!='suite-002' else None,'logBytes':log.stat().st_size,'logSha256':sha(log),'productionPromotion':False}
put('reports/full-'+job+'.json',record)
print(json.dumps(record,sort_keys=True),flush=True);print(text[-20000:],flush=True)
if afterError or not result.get('terminal') or not result.get('all_owned_native_handles_closed') or result.get('supervisor_error'):sys.exit(125)
sys.exit(result['exitcode'])
