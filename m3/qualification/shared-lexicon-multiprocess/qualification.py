# Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
# QA composition of existing Maven/RAM compiler, exact snapshot recipe and SharedLexiconImage.
import os,sys,json,hashlib,base64,ctypes,shutil,subprocess,http.server,threading,uuid,time,struct
from pathlib import Path
import tempfile
cfg=INPUT
T=Path(r"F:\codex-work\lexicon-migration-20261031");S=Path(r"S:\codex-work\agent-temp\lexicon-maven-r224")
cap=571473920;reserve=8589934592
def allocation(path):
 total=0;pending=[path]
 while pending:
  with os.scandir(pending.pop()) as entries:
   for e in entries:
    st=e.stat(follow_symlinks=False)
    if getattr(st,"st_file_attributes",0)&0x400:
     if path==T:continue
     raise AssertionError("reparse output lane")
    if e.is_dir(follow_symlinks=False):pending.append(e.path)
    else:total+=(st.st_size+4095)//4096*4096
 return total
def raw(s):return s.encode("utf-8")
def ident(d):return dict(bytes=len(d),sha256=hashlib.sha256(d).hexdigest(),gitBlob=hashlib.sha1(b"blob "+str(len(d)).encode()+b"\0"+d).hexdigest())
f_before=allocation(T);s_before=allocation(S);before=f_before+s_before
assert before+4096<=cap and shutil.disk_usage(S).free-65536>=reserve
assert S.resolve().drive.upper()=="S:" and S.resolve().is_relative_to(Path(r"S:\codex-work\agent-temp").resolve())
for child in [S/"temp",S/"home",S/"home/.m2",S/"repo-head"]:assert child.is_dir() and not child.is_symlink()
os.environ["TEMP"]=str(S/"temp");os.environ["TMP"]=str(S/"temp");tempfile.tempdir=str(S/"temp")
k=ctypes.WinDLL("kernel32",use_last_error=True);ps=ctypes.WinDLL("psapi",use_last_error=True)
b=ctypes.create_unicode_buffer(32768);assert k.GetTempPathW(len(b),b);assert Path(b.value).resolve()==(S/"temp").resolve()
assert ctypes.sizeof(ctypes.c_void_p)==8
spc=ctypes.c_ulong();bps=ctypes.c_ulong();freec=ctypes.c_ulong();allc=ctypes.c_ulong()
assert k.GetDiskFreeSpaceW("S:\\",ctypes.byref(spc),ctypes.byref(bps),ctypes.byref(freec),ctypes.byref(allc));assert spc.value*bps.value==4096
class Mem(ctypes.Structure):
 _fields_=[("dwLength",ctypes.c_ulong),("dwMemoryLoad",ctypes.c_ulong),("totalPhys",ctypes.c_ulonglong),("availPhys",ctypes.c_ulonglong),("totalCommit",ctypes.c_ulonglong),("availCommit",ctypes.c_ulonglong),("totalVirtual",ctypes.c_ulonglong),("availVirtual",ctypes.c_ulonglong),("extended",ctypes.c_ulonglong)]
mem=Mem();mem.dwLength=ctypes.sizeof(mem);assert k.GlobalMemoryStatusEx(ctypes.byref(mem));assert mem.availPhys>1073741824 and mem.availCommit>1073741824
driver=T/"shared-array-descriptors-20261112/jni-proof/project/M3LexiconCpuProof.java";pom=driver.parent/"pom.xml"
assert driver.read_bytes()==raw(cfg["driver"]) and pom.read_bytes()==raw(cfg["pom"])
snappy=T/"code-text-oracle-recipe/proof/temp/snappy-1.1.10-6e0cfc58-474b-47d4-bdb8-4cb270ee6e0e-snappyjava.dll"
assert hashlib.sha256(snappy.read_bytes()).hexdigest()=="3879fdfd746b77f29e6fade812669796c8dfcf4db2f5d5409b10c5e584ae5494"
for path,sha in cfg["jars"].items():assert hashlib.sha256(Path(path).read_bytes()).hexdigest()==sha
for name,item in cfg["sources"].items():assert ident(raw(item["content"]))["gitBlob"]==item["sha"],name
for name,item in cfg["recipeSources"].items():
 if item.get("sha"):assert ident(raw(item["content"]))["gitBlob"]==item["sha"],name
target=cfg["target"];crate="m3-shared-image-process-probe";resourceRoot="com/synexia/rewrite/hash-pinned-java/"+crate+"/"
manifest=target+"\tABSENT\t"+ident(raw(cfg["probe"]))["sha256"]+"\tM3SharedImageProcessProbe.java.txt\n"
resources={resourceRoot+"manifest.tsv":raw(manifest),resourceRoot+"M3SharedImageProcessProbe.java.txt":raw(cfg["probe"])}
env=os.environ.copy()
for name in ["JAVA_TOOL_OPTIONS","_JAVA_OPTIONS","JDK_JAVA_OPTIONS","MAVEN_ARGS","CLASSPATH","GH_DEBUG"]:env.pop(name,None)
env.update(TEMP=str(S/"temp"),TMP=str(S/"temp"),JAVA_HOME=r"C:\Program Files\Java\jdk-21",MAVEN_USER_HOME=str(S/"home/.m2"),MAVEN_SKIP_RC="1",MAVEN_OPTS="-Xmx256m -XX:-UsePerfData -XX:-HeapDumpOnOutOfMemoryError -XX:ErrorFile="+str(S/"hs_err_pid%p.log")+" -XX:HeapDumpPath="+str(S/"heap.hprof")+" -Djava.io.tmpdir="+str(S/"temp")+" -Duser.home="+str(S/"home"))
cp=";".join(cfg["jars"]);env["M3_RUNTIME_CLASSPATH"]=cp
packets={};received={};errors=[];release=threading.Event();nonce="/"+uuid.uuid4().hex;reports=[];active=[];fixture=None;fileHandle=None;fixtureBytes=None;observations=[]
class H(http.server.BaseHTTPRequestHandler):
 def do_GET(self):
  data=packets.get(self.path)
  if data is None:self.send_error(404);return
  self.send_response(200);self.send_header("Content-Length",str(len(data)));self.end_headers();self.wfile.write(data)
 def do_POST(self):
  try:
   assert self.path in {nonce+"/recipe",nonce+"/reader-a",nonce+"/reader-b"} and self.path not in received
   length=int(self.headers["Content-Length"]);assert 0<length<65536
   data=self.rfile.read(length)
   if self.path.endswith("/recipe"):
    row=data.decode().splitlines();assert len(row)==1
    path,sha,encoded=row[0].split("\t");out=base64.b64decode(encoded,validate=True)
    assert path==target and out==raw(cfg["probe"]) and ident(out)["sha256"]==sha
    received[self.path]=out
   else:
    value=json.loads(data);assert set(value)=={"pid","address","mappedBytes","records","tests","identity","directorySha256","loadedHint"}
    assert value["tests"]==5 and value["address"]>0 and 0<value["mappedBytes"]<=4096
    received[self.path]=value
    assert release.wait(60),"parent did not release observation"
   self.send_response(204);self.end_headers()
  except Exception as e:
   errors.append(type(e).__name__+":"+str(e));self.send_error(400)
 def log_message(self,*args):pass
server=http.server.ThreadingHTTPServer(("127.0.0.1",0),H)
serverThread=threading.Thread(target=server.serve_forever,daemon=True);serverThread.start()
base="http://127.0.0.1:"+str(server.server_port)
def packet(sources,res):
 return raw(str(len(sources))+"\n")+b"".join(raw(n+"\t"+ident(raw(v))["sha256"]+"\t")+base64.b64encode(raw(v))+b"\n" for n,v in sources.items())+b"".join(raw(n+"\t"+ident(v)["sha256"]+"\t")+base64.b64encode(v)+b"\n" for n,v in res.items())
def budget():
 assert f_before+allocation(S)<=cap and shutil.disk_usage(S).free>=reserve
def worker(label,sources,res,entry,args,maven=False):
 budget();p=packet(sources,res);assert len(p)<=2097152;packets[nonce+"/"+label]=p
 argv=[r"C:\Program Files\Java\jdk-21\bin\java.exe","--add-opens=java.base/java.nio=ALL-UNNAMED","--add-opens=java.base/java.lang=ALL-UNNAMED","-XX:-UsePerfData","-XX:-HeapDumpOnOutOfMemoryError","-Xmx"+("256m" if maven else "128m"),"-XX:ErrorFile="+str(S/"hs_err_pid%p.log"),"-XX:HeapDumpPath="+str(S/"heap.hprof"),"-Djava.io.tmpdir="+str(S/"temp"),"-Duser.home="+str(S/"home"),"-Djunit.jupiter.execution.parallel.enabled=false","-Dorg.xerial.snappy.lib.path="+str(snappy.parent),"-Dorg.xerial.snappy.lib.name="+snappy.name,"-cp",cp,"--source","21",str(driver),base+nonce+"/"+label,ident(p)["sha256"],entry]+args
 if maven:
  command=" ".join('"'+s.replace('"','\\"')+'"' for s in argv[1:])
  argv=[r"C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin\mvn.cmd","-o","-B","-ntp","-f",str(pom),"-Dmaven.repo.local="+str(S/"repo-head"),"-Dmaven.repo.local.tail="+str(T/"range-current-qualification/proof/repo-head")+",C:/Users/hsoliwal/.m2/repository","-Dmaven.repo.local.tail.ignoreAvailability=false","-Dexec.args="+command,"org.codehaus.mojo:exec-maven-plugin:3.3.0:exec"]
 proc=subprocess.Popen(argv,cwd=S,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 item=dict(stage=label,pid=proc.pid,argv=argv,packetIdentity=ident(p),sourcePins={n:ident(raw(v)) for n,v in sources.items()},maven=maven);active.append((proc,item))
 print("M3_IMAGE_STAGE_STARTED "+label,flush=True)
 return proc,item
def collect(job,timeout=45):
 proc,item=job
 try:log,_=proc.communicate(timeout=timeout)
 except subprocess.TimeoutExpired:
  subprocess.run(["taskkill","/PID",str(proc.pid),"/T","/F"],env=env,capture_output=True,timeout=15);log,_=proc.communicate(timeout=15);raise AssertionError(item["stage"]+" timed out: "+log.decode("utf-8","replace")[-6000:])
 item.update(exit=proc.returncode,reaped=proc.poll() is not None,log=log.decode("utf-8","replace"),logIdentity=ident(log));reports.append(item)
 assert proc.returncode==0 and "M3_CPU_RAM_COMPILE_PASS" in item["log"],item["stage"]+":"+item["log"][-8000:]
 budget();return item["log"]
stop=threading.Event();watchErrors=[]
def watch():
 while not stop.wait(0.5):
  try:budget()
  except Exception:
   watchErrors.append("storage budget");release.set()
   for proc,_ in active:
    if proc.poll() is None:subprocess.run(["taskkill","/PID",str(proc.pid),"/T","/F"],env=env,capture_output=True,timeout=15)
   return
watcher=threading.Thread(target=watch,daemon=True);watcher.start()
k.CloseHandle.argtypes=[ctypes.c_void_p];k.CloseHandle.restype=ctypes.c_int
k.CreateFileW.argtypes=[ctypes.c_wchar_p,ctypes.c_ulong,ctypes.c_ulong,ctypes.c_void_p,ctypes.c_ulong,ctypes.c_ulong,ctypes.c_void_p];k.CreateFileW.restype=ctypes.c_void_p
k.GetFinalPathNameByHandleW.argtypes=[ctypes.c_void_p,ctypes.c_wchar_p,ctypes.c_ulong,ctypes.c_ulong];k.GetFinalPathNameByHandleW.restype=ctypes.c_ulong
k.OpenProcess.argtypes=[ctypes.c_ulong,ctypes.c_int,ctypes.c_ulong];k.OpenProcess.restype=ctypes.c_void_p
class FileInfo(ctypes.Structure):
 _fields_=[("attributes",ctypes.c_ulong),("creation",ctypes.c_ulonglong),("access",ctypes.c_ulonglong),("write",ctypes.c_ulonglong),("volume",ctypes.c_ulong),("sizeHigh",ctypes.c_ulong),("sizeLow",ctypes.c_ulong),("links",ctypes.c_ulong),("indexHigh",ctypes.c_ulong),("indexLow",ctypes.c_ulong)]
# FILETIME alignment is 4, unlike ULONGLONG: use the documented native structure.
class FT(ctypes.Structure):_fields_=[("low",ctypes.c_ulong),("high",ctypes.c_ulong)]
class FI(ctypes.Structure):
 _fields_=[("attributes",ctypes.c_ulong),("creation",FT),("access",FT),("write",FT),("volume",ctypes.c_ulong),("sizeHigh",ctypes.c_ulong),("sizeLow",ctypes.c_ulong),("links",ctypes.c_ulong),("indexHigh",ctypes.c_ulong),("indexLow",ctypes.c_ulong)]
k.GetFileInformationByHandle.argtypes=[ctypes.c_void_p,ctypes.POINTER(FI)];k.GetFileInformationByHandle.restype=ctypes.c_int
class WS(ctypes.Structure):_fields_=[("address",ctypes.c_void_p),("flags",ctypes.c_size_t)]
ps.QueryWorkingSetEx.argtypes=[ctypes.c_void_p,ctypes.c_void_p,ctypes.c_ulong];ps.QueryWorkingSetEx.restype=ctypes.c_int
ps.GetMappedFileNameW.argtypes=[ctypes.c_void_p,ctypes.c_void_p,ctypes.c_wchar_p,ctypes.c_ulong];ps.GetMappedFileNameW.restype=ctypes.c_ulong
assert ctypes.sizeof(WS)==16 and ctypes.sizeof(FI)==52
success=False
try:
 recipeSources={n:v["content"] for n,v in cfg["recipeSources"].items()}
 collect(worker("recipe",recipeSources,resources,"com.synexia.rewrite.M3SharedImageProcessRecipeQualification",[base+nonce+"/recipe"],True),150)
 assert received[nonce+"/recipe"]==raw(cfg["probe"])
 productSources={n:v["content"] for n,v in cfg["sources"].items()}
 productSources["jdk.m3.text.M3SharedImageProcessProbe"]=received[nonce+"/recipe"].decode()
 fixture=S/"temp"/("m3-image-process-"+nonce[1:]+".m3lex")
 assert not fixture.exists() and fixture.resolve().parent==(S/"temp").resolve()
 collect(worker("writer",productSources,{},"jdk.m3.text.M3SharedImageProcessProbe",["create",str(fixture),base+nonce+"/unused"],True),150)
 fixtureBytes=fixture.read_bytes();assert 64<len(fixtureBytes)<=4096 and allocation(S)==s_before+4096
 imageId=hashlib.sha256(fixtureBytes[:32]+fixtureBytes[64:]).hexdigest()
 version,count=struct.unpack_from(">II",fixtureBytes,8);payload,units=struct.unpack_from(">QQ",fixtureBytes,16)
 assert version==1 and payload==64+8*count and payload+2*units==len(fixtureBytes)
 directoryHash=hashlib.sha256(fixtureBytes[64:payload]).hexdigest()
 fileHandle=k.CreateFileW(str(fixture),0x80000000,1,None,3,0x80,None)
 assert fileHandle not in (None,ctypes.c_void_p(-1).value),ctypes.get_last_error()
 info=FI();assert k.GetFileInformationByHandle(fileHandle,ctypes.byref(info))
 final=ctypes.create_unicode_buffer(32768);assert k.GetFinalPathNameByHandleW(fileHandle,final,len(final),2)
 nativePath=final.value;assert nativePath.startswith("\\Device\\")
 a=worker("reader-a",productSources,{},"jdk.m3.text.M3SharedImageProcessProbe",["read",str(fixture),base+nonce+"/reader-a"])
 bjob=worker("reader-b",productSources,{},"jdk.m3.text.M3SharedImageProcessProbe",["read",str(fixture),base+nonce+"/reader-b"])
 deadline=time.monotonic()+90
 while nonce+"/reader-a" not in received or nonce+"/reader-b" not in received:
  budget();assert time.monotonic()<deadline and not errors,"reader readiness timeout: "+str(errors)
  for proc,item in [a,bjob]:
   if proc.poll() is not None:collect((proc,item));raise AssertionError("reader exited before observation")
  time.sleep(.1)
 values=[received[nonce+"/reader-a"],received[nonce+"/reader-b"]]
 assert values[0]["pid"]!=values[1]["pid"]
 for value,job in zip(values,[a,bjob]):
  assert value["pid"]==job[0].pid and job[0].poll() is None
  assert value["identity"]==imageId and value["mappedBytes"]==len(fixtureBytes) and value["records"]==count and value["directorySha256"]==directoryHash
  handle=k.OpenProcess(0x410,False,value["pid"]);assert handle,ctypes.get_last_error()
  try:
   path=ctypes.create_unicode_buffer(32768);assert ps.GetMappedFileNameW(handle,ctypes.c_void_p(value["address"]),path,len(path)),ctypes.get_last_error()
   assert path.value.casefold()==nativePath.casefold()
   # The image occupies one 4096-byte page; require full file coverage within that page.
   assert value["address"]%4096==0 and value["mappedBytes"]<=4096
   row=WS(value["address"],0);assert ps.QueryWorkingSetEx(handle,ctypes.byref(row),ctypes.sizeof(row)),ctypes.get_last_error()
   flags=row.flags;obs=dict(pid=value["pid"],mappingAddress=value["address"],mappedPath=path.value,fileByteOffset=0,coveredImageBytes=len(fixtureBytes),pageBytes=4096,queryFlags=flags,valid=bool(flags&1),shareCount=(flags>>1)&7,win32Protection=(flags>>4)&2047,shared=bool(flags&(1<<15)))
   observations.append(obs)
   assert obs["valid"] and obs["shared"] and obs["shareCount"]>=2 and obs["win32Protection"]==2,obs
  finally:assert k.CloseHandle(handle)
 assert fixture.read_bytes()==fixtureBytes and not errors
 release.set()
 collect(a);collect(bjob)
 success=True
finally:
 release.set()
 for proc,item in active:
  if proc.poll() is None:
   subprocess.run(["taskkill","/PID",str(proc.pid),"/T","/F"],env=env,capture_output=True,timeout=15)
   proc.wait(timeout=15)
 if fileHandle:assert k.CloseHandle(fileHandle)
 if fixture is not None and fixture.exists():
  assert fixtureBytes is not None and fixture.read_bytes()==fixtureBytes
  assert fixture.resolve().parent==(S/"temp").resolve() and fixture.name.startswith("m3-image-process-"+nonce[1:])
  fixture.unlink()
 stop.set();watcher.join(timeout=10);server.shutdown();server.server_close();serverThread.join(timeout=5)
assert success and not watchErrors and not errors
f_after=allocation(T);s_after=allocation(S);assert f_after==f_before and s_after==s_before
result=dict(schema="M3_SHARED_LEXICON_MULTIPROCESS_WINDOWS_PROOF_V1",status="PASS",reports=reports,recipeTests=5,readerJUnitExecutions=10,liveJvmObservations=observations,readerReports=values,imageFile=ident(fixtureBytes),imageIdentity=imageId,fixtureBase64=base64.b64encode(fixtureBytes).decode(),directorySha256=directoryHash,fileIdentity=dict(volume=info.volume,indexHigh=info.indexHigh,indexLow=info.indexLow,nativePath=nativePath),storage=dict(fBefore=f_before,fAfter=f_after,sBefore=s_before,sAfter=s_after,combined=before,peakTaskAllocation=before+4096,capBytes=cap,reserveBytes=reserve,sFree=shutil.disk_usage(S).free,nativeTemp=str(S/"temp"),transientFiles=1,transientFileBytes=len(fixtureBytes),transientAllocatedBytes=4096,fixtureRemoved=True),memory=dict(availablePhysicalBefore=mem.availPhys,availableCommitBefore=mem.availCommit,readerHeapCapEach=134217728),manifest=manifest,generationCalls=0,scope="Exact existing six production source owners, five recipe tests and five reader tests in each of two live Windows x64 JVMs. One local immutable CREATE_NEW fixture, one-page full-image coverage; OS-reported sharing only. No custom JNI DLL or direct Java array/String interning, PFN identity, stable virtual-address, startup/throughput or other-platform claim.")
EMIT(result)
