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
phase=json.loads((ROOT/'PHASE.json').read_text(encoding='utf-8'))
assert phase['capBytes']==CAP and phase['reserveBytes']==RESERVE and phase['scope']=='caller-recipe-runtime-followup'
mode=sys.argv[1];assert mode in ['enrolled-qualify-001']
log=ROOT/'logs'/(mode+'.log');receipt=ROOT/'reports'/(mode+'.json')
assert not log.exists() and not receipt.exists(),'closed/previous job label'
before=check();data=SUP.read_bytes();assert hashlib.sha256(data).hexdigest()==SUP_HASH
ns={'__name__':'owned_job_supervisor'};exec(compile(data,str(SUP),'exec'),ns);ns['ROOT']=ROOT
argv=[str(JAVA/'bin/java.exe'),'-Xmx256m','-Duser.home='+str(ROOT/'home'),'-Djava.io.tmpdir='+str(ROOT/'tmp'),'-Djava.util.prefs.userRoot='+str(ROOT/'home/prefs'),'-Djava.util.prefs.systemRoot='+str(ROOT/'home/system-prefs'),'-Dmaven.home='+str(MAVEN),'-Dmaven.multiModuleProjectDirectory='+str(ROOT/'recipe-enrolled'),'-Dclassworlds.conf='+str(MAVEN/'bin/m2.conf'),'-cp',str(MAVEN/'boot/plexus-classworlds-2.9.0.jar'),'org.codehaus.plexus.classworlds.launcher.Launcher','-o','-B','-ntp','-nsu','-f',str(ROOT/'recipe-enrolled/pom.xml'),'-Dmaven.repo.local='+str(REPO),'-Dm3.caller.receiverRoot='+str(ROOT/'receiver'),'-Dm3.caller.recipeRoot='+str(ROOT/'recipe-enrolled'),'-Dm3.caller.outputRoot='+str(ROOT/mode),'-Dm3.caller.mode='+'qualify','verify']
env=dict(os.environ)
for n in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','MAVEN_OPTS','MAVEN_ARGS','CLASSPATH']:env.pop(n,None)
env.update(TEMP=r'K:\codex-work\agent-temp',TMP=r'K:\codex-work\agent-temp',JAVA_HOME=str(JAVA),USERPROFILE=str(ROOT/'home'),XDG_CACHE_HOME=str(ROOT/'cache'),MAVEN_SKIP_RC='1')
t0=time.time();outcome={}
result=ns['run_owned'](argv,env,log,check,300,outcome=outcome)
afterError=None
try:after=check(final=True)
except BaseException as failure:after={'status':'HOLD'};afterError=type(failure).__name__+': '+str(failure)[:400]
record={'schema':'m3.owned-cpu-qualification/v1','scope':phase['scope'],'job':mode,'command':argv,'startedUnix':t0,'endedUnix':time.time(),'before':before,'after':after,'supervisorSha256':SUP_HASH,'javaSha256':sha(JAVA/'bin/java.exe'),'controllerSha256':sha(__file__),'pomSha256':sha(ROOT/'recipe-enrolled/pom.xml'),'native':result,'logSha256':sha(log),'logBytes':log.stat().st_size,'productionPromotion':False,'postGuardError':afterError}
put('reports/'+mode+'.json',record)
print(json.dumps(record,sort_keys=True),flush=True)
print(log.read_text(encoding='utf-8',errors='replace')[-12000:],flush=True)
if afterError or not result.get('terminal') or not result.get('all_owned_native_handles_closed') or result.get('supervisor_error'):sys.exit(125)
sys.exit(result['exitcode'])
