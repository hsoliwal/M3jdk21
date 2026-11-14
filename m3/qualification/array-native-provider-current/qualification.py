# Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
# QA composition of existing Maven/RAM compiler, exact snapshot recipe and SharedLexiconImage.
import os,sys,json,hashlib,base64,ctypes,shutil,subprocess,http.server,threading,uuid,time,struct
from pathlib import Path
import tempfile
cfg=INPUT
T=Path(r"F:\codex-work\lexicon-migration-20261031");S=Path(r"S:\codex-work\agent-temp\lexicon-maven-r224")
cap=571473920;reserve=8589934592
def allocation(path):
 # Native enumeration reuses WIN32_FIND_DATA sizes/attributes; no per-entry stat/open.
 # This is a bounded storage diagnostic, not a product filesystem implementation.
 from ctypes import wintypes as w
 k32=ctypes.WinDLL("kernel32",use_last_error=True)
 class D(ctypes.Structure):
  _fields_=[("attr",w.DWORD),("created",w.FILETIME),("access",w.FILETIME),("write",w.FILETIME),("high",w.DWORD),("low",w.DWORD),("r0",w.DWORD),("r1",w.DWORD),("name",w.WCHAR*260),("alt",w.WCHAR*14)]
 k32.FindFirstFileW.argtypes=[w.LPCWSTR,ctypes.POINTER(D)];k32.FindFirstFileW.restype=w.HANDLE
 k32.FindNextFileW.argtypes=[w.HANDLE,ctypes.POINTER(D)];k32.FindClose.argtypes=[w.HANDLE]
 pending=["\\\\?\\"+str(path)];total=0;dirs=0;started=time.monotonic()
 while pending:
  root=pending.pop();d=D();h=k32.FindFirstFileW(root+"\\*",ctypes.byref(d))
  assert h!=ctypes.c_void_p(-1).value,(root,ctypes.get_last_error())
  try:
   while True:
    if d.name not in [".",".."]:
     if d.attr&0x400:
      assert path==T,"reparse output lane"
     elif d.attr&0x10:pending.append(root+"\\"+d.name)
     else:total+=((((d.high<<32)|d.low)+4095)//4096)*4096
    if not k32.FindNextFileW(h,ctypes.byref(d)):
     assert ctypes.get_last_error()==18,ctypes.get_last_error()
     break
  finally:k32.FindClose(h)
  dirs+=1
  if path==T and dirs%5000==0:print("M3_STORAGE_NATIVE_SCAN dirs="+str(dirs)+" allocated="+str(total),flush=True)
  if time.monotonic()-started>90:raise TimeoutError("native allocation scan bounded")
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

env=os.environ.copy()
for name in ["JAVA_TOOL_OPTIONS","_JAVA_OPTIONS","JDK_JAVA_OPTIONS","MAVEN_ARGS","CLASSPATH","GH_DEBUG"]:env.pop(name,None)
env.update(TEMP=str(S/"temp"),TMP=str(S/"temp"),JAVA_HOME=r"C:\Program Files\Java\jdk-21",MAVEN_USER_HOME=str(S/"home/.m2"),MAVEN_SKIP_RC="1",MAVEN_OPTS="-Xmx256m -XX:-UsePerfData -XX:-HeapDumpOnOutOfMemoryError -XX:ErrorFile="+str(S/"hs_err_pid%p.log")+" -XX:HeapDumpPath="+str(S/"heap.hprof")+" -Djava.io.tmpdir="+str(S/"temp")+" -Duser.home="+str(S/"home"))
env["TMPDIR"]=str(S/"temp")
env["JAVA_TOOL_OPTIONS"]="-XX:+PerfDisableSharedMem -XX:-UsePerfData -XX:-PerfDataSaveToFile"
cp=";".join(cfg["jars"]);env["M3_RUNTIME_CLASSPATH"]=cp
packets={};received={};errors=[];nonce="/"+uuid.uuid4().hex;reports=[];active=None
seed=hashlib.sha256(b"M3_CORPUS_OPTIONAL_PERIOD_FIXTURE_V1").hexdigest()[:32]
class H(http.server.BaseHTTPRequestHandler):
 def do_GET(self):
  data=packets.get(self.path)
  if data is None:self.send_error(404);return
  self.send_response(200);self.send_header("Content-Length",str(len(data)));self.end_headers();self.wfile.write(data)
 def do_POST(self):
  try:
   assert self.path not in received
   size=int(self.headers["Content-Length"]);assert 0<size<1048576
   data=self.rfile.read(size)
   if self.path.startswith(nonce+"/recipe-result-"):
    stage=self.path.rsplit("-",1)[1];step=next(x for x in cfg["steps"] if x["id"]==stage)
    lines=data.decode().splitlines();assert len(lines)==1
    path,sha,value=lines[0].split("\t");value=base64.b64decode(value,validate=True)
    assert path==step["owner"] and value==raw(step["afterText"]) and ident(value)["sha256"]==sha
   else:
    assert self.path.startswith(nonce+"/result-");value=json.loads(data)
   received[self.path]=value;self.send_response(204);self.end_headers()
  except Exception as failure:errors.append(type(failure).__name__+":"+str(failure));self.send_error(400)
 def log_message(self,*args):pass
server=http.server.ThreadingHTTPServer(("127.0.0.1",0),H);serverThread=threading.Thread(target=server.serve_forever,daemon=True);serverThread.start()
base="http://127.0.0.1:"+str(server.server_port)
def packet(sources,res):
 return raw(str(len(sources))+"\n")+b"".join(raw(n+"\t"+ident(raw(v))["sha256"]+"\t")+base64.b64encode(raw(v))+b"\n" for n,v in sources.items())+b"".join(raw(n+"\t"+ident(v)["sha256"]+"\t")+base64.b64encode(v)+b"\n" for n,v in res.items())
watchErrors=[];scanRetries=[];observedPeaks=[];stop=threading.Event()
def budget():
 for attempt in range(3):
  try:used=allocation(S);break
  except FileNotFoundError as failure:
   scanRetries.append(dict(type=type(failure).__name__,path=str(failure.filename),attempt=attempt))
   if attempt==2:raise
 total=f_before+used;free=shutil.disk_usage(S).free
 if not observedPeaks or total>max(observedPeaks):observedPeaks.append(total)
 assert total<=cap,"task_allocation:"+str(total)+">"+str(cap)
 assert free>=reserve,"reserve:"+str(free)+"<"+str(reserve)
def watch():
 while not stop.wait(.5):
  try:budget()
  except Exception as failure:
   watchErrors.append(dict(type=type(failure).__name__,message=str(failure),sFree=shutil.disk_usage(S).free))
   if active is not None and active.poll() is None:subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15)
   return
watcher=threading.Thread(target=watch,daemon=True);watcher.start()
def run(label,sources,res,entry,args,expected=0,properties=None,retainedOutputBytes=None):
 global active
 budget();p=packet(sources,res);assert len(p)<=2097152;packets[nonce+"/"+label]=p
 argv=["-Xcheck:jni","--add-opens=java.base/java.lang=ALL-UNNAMED","-XX:-UsePerfData","-XX:-HeapDumpOnOutOfMemoryError","-Xmx512m","-XX:ErrorFile="+str(S/"hs_err_pid%p.log"),"-XX:HeapDumpPath="+str(S/"heap.hprof"),"-Djava.io.tmpdir="+str(S/"temp"),"-Duser.home="+str(S/"home"),"-Djunit.jupiter.execution.parallel.enabled=false","-Dorg.xerial.snappy.lib.path="+str(snappy.parent),"-Dorg.xerial.snappy.lib.name="+snappy.name]+["-D"+n+"="+v for n,v in (properties or {}).items()]+["-cp",cp,"--source","21",str(driver),base+nonce+"/"+label,ident(p)["sha256"],entry]+args
 command=" ".join('"'+s.replace('"','\\"')+'"' for s in argv)
 argv=[r"C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin\mvn.cmd","-o","-B","-ntp","-f",str(pom),"-Dmaven.repo.local="+str(S/"repo-head"),"-Dmaven.repo.local.tail="+str(T/"range-current-qualification/proof/repo-head")+",C:/Users/hsoliwal/.m2/repository","-Dmaven.repo.local.tail.ignoreAvailability=false","-Dexec.args="+command,"org.codehaus.mojo:exec-maven-plugin:3.3.0:exec"]
 started=time.monotonic();print("M3_CORPUS_PERIOD_STAGE_STARTED "+label,flush=True)
 active=subprocess.Popen(argv,cwd=S,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 try:log,_=active.communicate(timeout=180)
 except subprocess.TimeoutExpired:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);log,_=active.communicate(timeout=15);raise AssertionError(label+" timeout")
 item=dict(stage=label,pid=active.pid,argv=argv,packetIdentity=ident(p),sourcePins={n:ident(raw(v)) for n,v in sources.items()},exit=active.returncode,reaped=active.poll() is not None,elapsedSeconds=round(time.monotonic()-started,3),log=log.decode("utf-8","replace"),logIdentity=ident(log));reports.append(item);active=None
 assert item["exit"]==expected and not watchErrors and not errors,item["log"][-16000:]
 if expected==0:assert "M3_CPU_RAM_COMPILE_PASS" in item["log"],label
 else:assert "M3_ARRAY_PROVIDER_EXPECTED_ABI_REFUSAL" in item["log"],label
 budget();assert allocation(S)==(s_before if retainedOutputBytes is None else retainedOutputBytes),"retained task output";return item

# Exact current translation unit, with quoted project headers expanded from pinned inputs only.
# Standard/JDK headers retain their installed owners; no replacement JNI implementation.
assert cfg["coreHeader"].replace("M3JDK 21:","M3JDK21:")==cfg["cacheDeps"][0]["content"]
for item in cfg["cacheDeps"]:
 assert Path(item["path"]).read_text(encoding="utf-8")==item["content"]
source=cfg["nativeC"].replace('#include "m3_arrays.h"',cfg["header"]).replace('#include "m3_jni_core.h"',cfg["coreHeader"])+"\n"+cfg["compareC"].replace('#include "m3_arrays.h"',cfg["header"])
gcc=Path(r"C:\ProgramData\mingw64\mingw64\bin\gcc.exe")
flags=["-std=c11","-Wall","-Wextra","-Wpedantic","-Werror","-fsyntax-only","-I",r"C:\Program Files\Java\jdk-21\include","-I",r"C:\Program Files\Java\jdk-21\include\win32","-x","c","-"]
compileLog=subprocess.run([str(gcc)]+flags,input=raw(source),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,env=env,cwd=S,timeout=45)
assert compileLog.returncode==0,compileLog.stdout.decode("utf-8","replace")
assert allocation(S)==s_before
prod={n:v["content"] for n,v in cfg["sources"].items()}
prod["jdk.m3.arrays.M3ArrayProviderQualification"]=cfg["qa"]
try:
 run("java-fallback",prod,{},"jdk.m3.arrays.M3ArrayProviderQualification",["java",base+nonce+"/result-java"])
 run("old-namespace",prod,{},"jdk.m3.arrays.M3ArrayProviderQualification",["legacy",base+nonce+"/result-legacy",str(T/"array-jni-master-qualification-20261105/bin/m3_arrays.dll")],expected=1)
finally:
 if active is not None and active.poll() is None:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);active.wait(timeout=15)
 stop.set();watcher.join(timeout=10);server.shutdown();server.server_close();serverThread.join(timeout=5)
assert not errors and not watchErrors
f_after=allocation(T);s_after=allocation(S)
assert f_after==f_before and s_after==s_before
EMIT(dict(schema="M3_ARRAY_PROVIDER_PREFLIGHT_V1",status="PASS",reports=reports,results=received,syntax=dict(argv=[str(gcc)]+flags,source=ident(raw(source)),exit=compileLog.returncode,log=compileLog.stdout.decode()),nativePins={n:ident(raw(cfg[n])) for n in ["nativeC","compareC","header","coreHeader"]},storage=dict(fBefore=f_before,sBefore=s_before,fAfter=f_after,sAfter=s_after,capBytes=cap,reserveBytes=reserve,sFree=shutil.disk_usage(S).free,peakObservedTaskAllocations=observedPeaks),scope="5 unmodified Java fallback tests; 7 intentional current-class/legacy-DLL ABI failures; strict C syntax check only, no current native binary build or native behavior pass. No product source changes; no output files.",generationCalls=0))
