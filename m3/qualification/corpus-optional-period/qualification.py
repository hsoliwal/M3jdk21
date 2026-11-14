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

env=os.environ.copy()
for name in ["JAVA_TOOL_OPTIONS","_JAVA_OPTIONS","JDK_JAVA_OPTIONS","MAVEN_ARGS","CLASSPATH","GH_DEBUG"]:env.pop(name,None)
env.update(TEMP=str(S/"temp"),TMP=str(S/"temp"),JAVA_HOME=r"C:\Program Files\Java\jdk-21",MAVEN_USER_HOME=str(S/"home/.m2"),MAVEN_SKIP_RC="1",MAVEN_OPTS="-Xmx256m -XX:-UsePerfData -XX:-HeapDumpOnOutOfMemoryError -XX:ErrorFile="+str(S/"hs_err_pid%p.log")+" -XX:HeapDumpPath="+str(S/"heap.hprof")+" -Djava.io.tmpdir="+str(S/"temp")+" -Duser.home="+str(S/"home"))
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
def run(label,sources,res,entry,args,expected=0,properties=None):
 global active
 budget();p=packet(sources,res);assert len(p)<=2097152;packets[nonce+"/"+label]=p
 argv=["--add-opens=java.base/java.lang=ALL-UNNAMED","-XX:-UsePerfData","-XX:-HeapDumpOnOutOfMemoryError","-Xmx512m","-XX:ErrorFile="+str(S/"hs_err_pid%p.log"),"-XX:HeapDumpPath="+str(S/"heap.hprof"),"-Djava.io.tmpdir="+str(S/"temp"),"-Duser.home="+str(S/"home"),"-Djunit.jupiter.execution.parallel.enabled=false","-Dorg.xerial.snappy.lib.path="+str(snappy.parent),"-Dorg.xerial.snappy.lib.name="+snappy.name]+["-D"+n+"="+v for n,v in (properties or {}).items()]+["-cp",cp,"--source","21",str(driver),base+nonce+"/"+label,ident(p)["sha256"],entry]+args
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
 else:assert "absentPeriodRoundTripsEveryPlane" in item["log"] and "mixedAbsentAndPresentPeriodsRoundTrip" in item["log"] and "M3_CORPUS_OPTIONAL_PERIOD_DISCRIMINATOR_RED" in item["log"],label
 budget();assert allocation(S)==s_before,"retained task output";return item
prod={n:v["content"] for n,v in cfg["sources"].items()}
for n,v in cfg["incumbentTests"].items():
 assert ident(raw(v["content"]))["gitBlob"]==v["sha"];prod[n]=v["content"]
prod["jdk.m3.text.M3CorpusOptionalPeriodQualification"]=cfg["qa"]
results=[];manifests={}
def product(label,mode,sources):
 run(label,sources,{},"jdk.m3.text.M3CorpusOptionalPeriodQualification",[mode,str(S/"temp"),base+nonce+"/result-"+label,seed],expected=1 if mode=="before" else 0)
 result=received[nonce+"/result-"+label]
 assert result["found"]==10 and result["aborted"]==0 and result["mode"]==mode
 assert result["passed"]==(8 if mode=="before" else 10) and result["failed"]==(2 if mode=="before" else 0),result
 assert result["absentPlanes"]==(0 if mode=="before" else 5) and result["incumbentRefusalChecks"]>50,result
 if mode=="before":assert set(result["failures"])=={"absentPeriodRoundTripsEveryPlane()","mixedAbsentAndPresentPeriodsRoundTrip()"},result
 for key,value in result["canonicalRoots"].items():
  if key in manifests:assert manifests[key]==value,"canonical bytes changed"
  manifests[key]=value
 results.append(result)
try:
 # Reproduce the accepted-constructor/public-reader failure before changing any owner.
 product("before-0","before",prod)
 recipeSources={n:v["content"] for n,v in cfg["recipeSources"].items()}
 for step in cfg["steps"]:
  stage=step["id"];crate="m3-corpus-optional-period-"+stage
  resourceRoot="com/synexia/rewrite/hash-pinned-java/"+crate+"/"
  manifest=step["owner"]+"\t"+ident(raw(step["beforeText"]))["sha256"]+"\t"+ident(raw(step["afterText"]))["sha256"]+"\tTarget.java.txt\n"
  res={resourceRoot+"manifest.tsv":raw(manifest),resourceRoot+"Target.java.before":raw(step["beforeText"]),resourceRoot+"Target.java.txt":raw(step["afterText"]),resourceRoot+"atom-before.txt":raw(step["atomBefore"]),resourceRoot+"atom-after.txt":raw(step["atomAfter"])}
  run("recipe-"+stage,recipeSources,res,"com.synexia.rewrite.M3CorpusOptionalPeriodRecipeQualification",[base+nonce+"/recipe-result-"+stage],properties={"m3.recipe.crate":crate,"m3.recipe.stage":stage})
  output=received[nonce+"/recipe-result-"+stage];assert output==raw(step["afterText"])
  if stage=="scope":recipeSources["com.synexia.rewrite.M3ScopeInference"]=output.decode()
  if stage=="java":recipeSources["com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe"]=output.decode()
  step["executedManifest"]=manifest
 after=dict(prod);after["jdk.m3.text.M3LanguageCorpusManifest"]=received[nonce+"/recipe-result-corpus"].decode()
 product("after-1","after",after)
 product("after-2","after",after)
 product("before-3","before",prod)
finally:
 if active is not None and active.poll() is None:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);active.wait(timeout=15)
 stop.set();watcher.join(timeout=10);server.shutdown();server.server_close();serverThread.join(timeout=5)
assert len(results)==4 and not errors and not watchErrors
f_after=allocation(T);s_after=allocation(S);assert f_after==f_before and s_after==s_before
out=dict(schema="M3_CORPUS_OPTIONAL_PERIOD_QUALIFICATION_V1",status="PASS",plan=cfg["plan"],reports=reports,results=results,recipeTests=24,productExecutions=40,positiveProductExecutions=36,expectedBaselineFailures=4,sourceChanges=[dict(stage=x["id"],path=x["owner"],before=ident(raw(x["beforeText"])),after=ident(received[nonce+"/recipe-result-"+x["id"]]),manifest=x["executedManifest"]) for x in cfg["steps"]],canonicalRoots=manifests,fixtureSeed=seed,storage=dict(fBefore=f_before,fAfter=f_after,sBefore=s_before,sAfter=s_after,peakObservedTaskAllocations=observedPeaks,capBytes=cap,reserveBytes=reserve,sFree=shutil.disk_usage(S).free,transientMaxPlannedAllocation=4096,allFixturesRemoved=True,scanRetries=scanRetries),generationCalls=0,scope="One source owner at a time: exact scope registry, exact Java recipe registry, existing manifest reader. Real public write/read; all5 corpus planes; full immutable incumbent main and5 selected write-free incumbent methods per product JVM. Existing header/schema/canonical roots and public empty-period refusals preserved. No corpus download/Unicode-domain decision/JNI/performance/platform/full-refusal-filesystem-main claim.")
EMIT(out)
