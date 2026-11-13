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

target=cfg["target"];resourceRoot="com/synexia/rewrite/hash-pinned-java/m3-version-one-java-hash-elision/"
manifest=target+"\t"+ident(raw(cfg["preimage"]))["sha256"]+"\t"+ident(raw(cfg["candidate"]))["sha256"]+"\tSharedLexiconImage.java.txt\n"
resources={resourceRoot+"manifest.tsv":raw(manifest),resourceRoot+"SharedLexiconImage.java.before":raw(cfg["preimage"]),resourceRoot+"SharedLexiconImage.java.txt":raw(cfg["candidate"])}
env=os.environ.copy()
for name in ["JAVA_TOOL_OPTIONS","_JAVA_OPTIONS","JDK_JAVA_OPTIONS","MAVEN_ARGS","CLASSPATH","GH_DEBUG"]:env.pop(name,None)
env.update(TEMP=str(S/"temp"),TMP=str(S/"temp"),JAVA_HOME=r"C:\Program Files\Java\jdk-21",MAVEN_USER_HOME=str(S/"home/.m2"),MAVEN_SKIP_RC="1",MAVEN_OPTS="-Xmx256m -XX:-UsePerfData -XX:-HeapDumpOnOutOfMemoryError -XX:ErrorFile="+str(S/"hs_err_pid%p.log")+" -XX:HeapDumpPath="+str(S/"heap.hprof")+" -Djava.io.tmpdir="+str(S/"temp")+" -Duser.home="+str(S/"home"))
env["JAVA_TOOL_OPTIONS"]="-XX:+PerfDisableSharedMem -XX:-UsePerfData -XX:-PerfDataSaveToFile"
cp=";".join(cfg["jars"]);env["M3_RUNTIME_CLASSPATH"]=cp
packets={};received={};errors=[];nonce="/"+uuid.uuid4().hex;reports=[];active=None;fixture=None;fixtureBytes=None
class H(http.server.BaseHTTPRequestHandler):
 def do_GET(self):
  data=packets.get(self.path)
  if data is None:self.send_error(404);return
  self.send_response(200);self.send_header("Content-Length",str(len(data)));self.end_headers();self.wfile.write(data)
 def do_POST(self):
  try:
   assert self.path==nonce+"/recipe-result" or self.path.startswith(nonce+"/result-")
   assert self.path not in received
   length=int(self.headers["Content-Length"]);assert 0<length<65536
   data=self.rfile.read(length)
   if self.path==nonce+"/recipe-result":
    rows=data.decode().splitlines();assert len(rows)==1
    path,sha,value=rows[0].split("\t");value=base64.b64decode(value,validate=True)
    assert path==target and value==raw(cfg["candidate"]) and ident(value)["sha256"]==sha
   else:value=json.loads(data)
   received[self.path]=value
   self.send_response(204);self.end_headers()
  except Exception as e:
   errors.append(type(e).__name__+":"+str(e));self.send_error(400)
 def log_message(self,*args):pass
server=http.server.ThreadingHTTPServer(("127.0.0.1",0),H)
serverThread=threading.Thread(target=server.serve_forever,daemon=True);serverThread.start()
base="http://127.0.0.1:"+str(server.server_port)
def packet(sources,res):
 return raw(str(len(sources))+"\n")+b"".join(raw(n+"\t"+ident(raw(v))["sha256"]+"\t")+base64.b64encode(raw(v))+b"\n" for n,v in sources.items())+b"".join(raw(n+"\t"+ident(v)["sha256"]+"\t")+base64.b64encode(v)+b"\n" for n,v in res.items())
observedPeaks=[];scanRetries=[]
def budget():
 for attempt in range(3):
  try:used=allocation(S);break
  except FileNotFoundError as failure:
   scanRetries.append(dict(type=type(failure).__name__,path=str(failure.filename),attempt=attempt))
   if attempt==2:raise
 total=f_before+used;free=shutil.disk_usage(S).free
 if not observedPeaks or total>max(observedPeaks):observedPeaks.append(total)
 assert total<=cap, "task_allocation:"+str(total)+">"+str(cap)
 assert free>=reserve, "reserve:"+str(free)+"<"+str(reserve)
watchErrors=[];stop=threading.Event()
def watch():
 while not stop.wait(.5):
  try:budget()
  except Exception as failure:
   watchErrors.append(dict(type=type(failure).__name__,message=str(failure),sFiles=[dict(path=str(p),bytes=p.stat().st_size) for p in S.rglob("*") if p.is_file()],sFree=shutil.disk_usage(S).free))
   if active is not None and active.poll() is None:subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15)
   return
watcher=threading.Thread(target=watch,daemon=True);watcher.start()
def run(label,sources,res,entry,args,expected=0):
 global active
 budget();p=packet(sources,res);assert len(p)<=2097152;packets[nonce+"/"+label]=p
 argv=["--add-opens=java.base/java.nio=ALL-UNNAMED","--add-opens=java.base/java.lang=ALL-UNNAMED","-XX:-UsePerfData","-XX:-HeapDumpOnOutOfMemoryError","-XX:MaxDirectMemorySize=128m","-Xmx256m","-XX:ErrorFile="+str(S/"hs_err_pid%p.log"),"-XX:HeapDumpPath="+str(S/"heap.hprof"),"-Djava.io.tmpdir="+str(S/"temp"),"-Duser.home="+str(S/"home"),"-Djunit.jupiter.execution.parallel.enabled=false","-Dorg.xerial.snappy.lib.path="+str(snappy.parent),"-Dorg.xerial.snappy.lib.name="+snappy.name,"-cp",cp,"--source","21",str(driver),base+nonce+"/"+label,ident(p)["sha256"],entry]+args
 command=" ".join('"'+s.replace('"','\\"')+'"' for s in argv)
 argv=[r"C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin\mvn.cmd","-o","-B","-ntp","-f",str(pom),"-Dmaven.repo.local="+str(S/"repo-head"),"-Dmaven.repo.local.tail="+str(T/"range-current-qualification/proof/repo-head")+",C:/Users/hsoliwal/.m2/repository","-Dmaven.repo.local.tail.ignoreAvailability=false","-Dexec.args="+command,"org.codehaus.mojo:exec-maven-plugin:3.3.0:exec"]
 started=time.monotonic();print("M3_V1_HASH_STAGE_STARTED "+label,flush=True)
 active=subprocess.Popen(argv,cwd=S,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 try:log,_=active.communicate(timeout=180)
 except subprocess.TimeoutExpired:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);log,_=active.communicate(timeout=15);raise AssertionError(label+" timeout")
 item=dict(stage=label,pid=active.pid,argv=argv,packetIdentity=ident(p),sourcePins={n:ident(raw(v)) for n,v in sources.items()},exit=active.returncode,reaped=active.poll() is not None,elapsedSeconds=round(time.monotonic()-started,3),log=log.decode("utf-8","replace"),logIdentity=ident(log));reports.append(item);active=None
 assert item["exit"]==expected and not watchErrors and not errors,item["log"][-12000:]
 if expected==0:assert "M3_CPU_RAM_COMPILE_PASS" in item["log"],label
 else:assert "unusedVersionOneHashPassIsElided" in item["log"] and "M3_VERSION1_ELISION_DISCRIMINATOR_RED" in item["log"],label
 budget();return item
results=[]
try:
 recipeSources={n:v["content"] for n,v in cfg["recipeSources"].items()}
 run("recipe",recipeSources,resources,"com.synexia.rewrite.M3VersionOneHashRecipeQualification",[base+nonce+"/recipe-result"])
 assert received[nonce+"/recipe-result"]==raw(cfg["candidate"])
 prod={n:v["content"] for n,v in cfg["sources"].items()}
 prod["SharedLexiconImageContractTest"]=cfg["referenceTest"]
 prod["jdk.m3.text.M3VersionOneHashQualification"]=cfg["qa"]
 fixture=S/"temp"/("m3-v1-hash-"+nonce[1:]+".m3lex")
 assert not fixture.exists() and fixture.resolve().parent==(S/"temp").resolve()
 run("fixture",prod,{},"jdk.m3.text.M3VersionOneHashQualification",["create",str(fixture),base+nonce+"/unused"])
 fixtureBytes=fixture.read_bytes();assert 64<len(fixtureBytes)<=4096 and allocation(S)==s_before+4096
 for at,mode in enumerate(["before","after","after","before"]):
  label=mode+"-"+str(at);sources=dict(prod)
  if mode=="after":sources["jdk.m3.text.SharedLexiconImage"]=received[nonce+"/recipe-result"].decode()
  run(label,sources,{},"jdk.m3.text.M3VersionOneHashQualification",[mode,str(fixture),base+nonce+"/result-"+label],expected=1 if mode=="before" else 0)
  result=received[nonce+"/result-"+label]
  assert result["mode"]==mode and result["found"]==8 and result["aborted"]==0
  assert result["passed"]==(7 if mode=="before" else 8) and result["failed"]==(1 if mode=="before" else 0)
  assert result["v1JavaHashCalls"]==(139 if mode=="before" else 0) and result["v2JavaHashCalls"]==139
  results.append(result)
 assert fixture.read_bytes()==fixtureBytes
finally:
 if active is not None and active.poll() is None:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);active.wait(timeout=15)
 if fixture is not None and fixture.exists():
  assert fixtureBytes is not None and fixture.read_bytes()==fixtureBytes
  assert fixture.resolve().parent==(S/"temp").resolve() and fixture.name.startswith("m3-v1-hash-"+nonce[1:])
  fixture.unlink()
 stop.set();watcher.join(timeout=10);server.shutdown();server.server_close();serverThread.join(timeout=5)
assert not errors and not watchErrors and len(results)==4
f_after=allocation(T);s_after=allocation(S);assert f_after==f_before and s_after==s_before
out=dict(schema="M3_VERSION_ONE_JAVA_HASH_ELISION_QUALIFICATION_V1",status="PASS",workOrder=cfg["workOrder"],reports=reports,results=results,recipeTests=7,productExecutions=32,positiveProductExecutions=30,expectedBaselineFailures=2,sourceBefore=ident(raw(cfg["preimage"])),sourceAfter=ident(received[nonce+"/recipe-result"]),manifest=manifest,fixture=ident(fixtureBytes),storage=dict(fBefore=f_before,fAfter=f_after,sBefore=s_before,sAfter=s_after,peakTaskAllocation=before+4096,capBytes=cap,reserveBytes=reserve,sFree=shutil.disk_usage(S).free,transientAllocatedBytes=4096,fixtureRemoved=True),storageScanRetries=scanRetries,peakObservedTaskAllocations=observedPeaks,generationCalls=0,scope="One existing production owner. Real original reference fixture builder and exact6 production owners. All stages Maven/Java21 RAM. Isolated QA-only ASM counters prove skipped helper dispatch; uninstrumented current constructor behavior and timing/allocation measured separately. Single public-open fixture; no full original filesystem suite, JNI DLL, cross-platform or file-open/startup performance claim.")
EMIT(out)
