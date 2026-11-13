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
# Reuse the verified immutable input budget instead of rewalking unrelated read-only history.
# No new output file is created by this qualification; only S can receive new task output.
import zlib
bw=cfg["readOnlyBudgetBaseline"]["receiptWrapper"];bd=zlib.decompress(bytes.fromhex(bw["payload"]))
assert len(bd)==bw["uncompressedBytes"] and hashlib.sha256(bd).hexdigest()==bw["uncompressedSha256"]
br=json.loads(bd);assert br["status"]=="PASS" and br["storage"]["fBefore"]==br["storage"]["fAfter"]==571469824 and br["storage"]["sAfter"]==0
f_before=cfg["readOnlyBudgetBaseline"]["allocatedBytes"];assert f_before==br["storage"]["fAfter"]
s_before=allocation(S);before=f_before+s_before
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
for routing_name in ["NODE_OPTIONS","NODE_COMPILE_CACHE","NODE_V8_COVERAGE","LLVM_PROFILE_FILE","CLANG_MODULE_CACHE_PATH"]:env.pop(routing_name,None)
env["TMPDIR"]=str(S/"temp")
env["JAVA_TOOL_OPTIONS"]="-XX:+PerfDisableSharedMem -XX:-UsePerfData -XX:-PerfDataSaveToFile"
cp=";".join(cfg["jars"]);env["M3_RUNTIME_CLASSPATH"]=cp
packets={};received={};recipeExpected={};errors=[];nonce="/"+uuid.uuid4().hex;reports=[];active=None
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
    lines=data.decode().splitlines();assert len(lines)==1
    path,sha,value=lines[0].split("\t");value=base64.b64decode(value,validate=True)
    assert path==cfg["target"] and value==recipeExpected[self.path] and ident(value)["sha256"]==sha
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
 started=time.monotonic();print("M3_MAPPING_OPTIONAL_STAGE_STARTED "+label,flush=True)
 active=subprocess.Popen(argv,cwd=S,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 try:log,_=active.communicate(timeout=180)
 except subprocess.TimeoutExpired:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);log,_=active.communicate(timeout=15);raise AssertionError(label+" timeout")
 item=dict(stage=label,pid=active.pid,argv=argv,packetIdentity=ident(p),sourcePins={n:ident(raw(v)) for n,v in sources.items()},exit=active.returncode,reaped=active.poll() is not None,elapsedSeconds=round(time.monotonic()-started,3),log=log.decode("utf-8","replace"),logIdentity=ident(log));reports.append(item);active=None
 assert item["exit"]==expected and not watchErrors and not errors,item["log"][-16000:]
 if expected==0:assert "M3_CPU_RAM_COMPILE_PASS" in item["log"],label
 else:assert "M3_ARRAY_PROVIDER_EXPECTED_ABI_REFUSAL" in item["log"],label
 budget();assert allocation(S)==(s_before if retainedOutputBytes is None else retainedOutputBytes),"retained task output";return item


# Compile the extracted donor C function verbatim; execute its valid Wasm object in existing V8.
# Both compiler output and the oracle transport remain in RAM.
assert cfg["donor"]["header"].count(cfg["donor"]["function"])==1
assert cfg["donor"]["function"] in cfg["donor"]["wrapper"]
clang=[r"C:\Program Files\LLVM\bin\clang.exe","--target=wasm32","-O2","-g0","-std=c11","-Wall","-Wextra","-Werror","-fno-ident","-nostdinc","-ffreestanding","-c","-x","c","-o","-","-"]
cproc=subprocess.run(clang,input=raw(cfg["donor"]["wrapper"]),stdout=subprocess.PIPE,stderr=subprocess.PIPE,cwd=S,env=env,timeout=30)
assert cproc.returncode==0,cproc.stderr.decode();assert cproc.stdout[:4]==b"\0asm" and len(cproc.stdout)<4096
import random
rng=random.Random(0x4d3352414e4745)
sources=[None,"","Aa","BB","numbers","source:de","source:hi-\u0939\u093f","source:ar-\u0639\u0631","source:zh-\u6c49","source:emoji-\U0001f600"]
records=[None,"","0","10000","AaBB","BBAa","\u212a","\u00e9","e\u0301","\U0001f600"]
bounds=[1,2,3,6,7,10,14,127,128,1000,20000,2147483646,2147483647]
def java_hash(value):
 if value is None:return 0
 units=value.encode("utf-16-be","surrogatepass");answer=0
 for i in range(0,len(units),2):answer=(answer*31+(units[i]<<8)+units[i+1])&0xffffffff
 return answer
vectors=[]
def vector(s,r,b):
 h=(31*java_hash(s)+java_hash(r))&0xffffffff;m=(h*0x9e3779b9)&0xffffffff;return [s,r,b,m^(m>>16)]
for s in sources:
 for r in records:
  for bnd in bounds:vectors.append(vector(s,r,bnd))
while len(vectors)<12288:
 i=len(vectors);s=sources[rng.randrange(len(sources))];r=str(rng.getrandbits(32)) if i%3 else records[rng.randrange(len(records))];bnd=bounds[i%len(bounds)] if i%7 else rng.randrange(1,2147483648);vectors.append(vector(s,r,bnd))
node_script='const fs=require("fs");const x=JSON.parse(fs.readFileSync(0,"utf8"));const m=new WebAssembly.Module(Buffer.from(x.module,"base64"));const imports=WebAssembly.Module.imports(m);if(JSON.stringify(imports)!==JSON.stringify([{module:"env",name:"__linear_memory",kind:"memory"},{module:"env",name:"__indirect_function_table",kind:"table"}]))throw Error("imports");const inst=new WebAssembly.Instance(m,{env:{__linear_memory:new WebAssembly.Memory({initial:0}),__indirect_function_table:new WebAssembly.Table({initial:0,element:"anyfunc"})}});process.stdout.write(JSON.stringify(x.values.map(v=>inst.exports.reference(v[0],v[1])>>>0)));'
node_input=json.dumps(dict(module=base64.b64encode(cproc.stdout).decode(),values=[[x[3],x[2]] for x in vectors]),separators=(",",":")).encode()
nproc=subprocess.run([r"C:\Program Files\nodejs\node.exe","-e",node_script],input=node_input,stdout=subprocess.PIPE,stderr=subprocess.PIPE,cwd=S,env=env,timeout=30)
assert nproc.returncode==0,nproc.stderr.decode();answers=json.loads(nproc.stdout);assert len(answers)==len(vectors)
def encoded(v):return "-" if v is None else base64.b64encode(v.encode("utf-8")).decode()
oracle=raw("".join(encoded(v[0])+"\t"+encoded(v[1])+"\t"+str(v[2])+"\t"+str(v[3])+"\t"+str(a)+"\n" for v,a in zip(vectors,answers)))
assert all(0<=a<v[2] and a==(v[3]*v[2])>>32 for v,a in zip(vectors,answers))
oracle_proof=dict(donor=cfg["donor"]["commit"],source=ident(raw(cfg["donor"]["wrapper"])),function=ident(raw(cfg["donor"]["function"])),module=ident(cproc.stdout),compileArgv=clang,compileExit=0,nodeExit=0,nodeScript=ident(raw(node_script)),nodeInput=ident(node_input),nodeAnswers=ident(nproc.stdout),resource=ident(oracle),vectors=len(vectors),sFilesAfter=allocation(S),scope="Exact C donor function compiled to a327byte Wasm object and executed through existing V8 fromRAM. Portable arithmetic differential only; no host JNI/native-platform pass.")
assert allocation(S)==s_before

assert ident(raw(cfg["incumbentTest"]["content"]))["gitBlob"]==cfg["incumbentTest"]["sha"]
assert cfg["preimage"].count(cfg["atomBefore"])==1 and cfg["candidate"]==cfg["preimage"].replace(cfg["atomBefore"],cfg["atomAfter"])
image=Path(cfg["existingImage"]);image_bytes=image.read_bytes();assert len(image_bytes)<=4096 and image_bytes[:12]==b"M3LEX001\0\0\0\2"
recipe_sources={n:v["content"] for n,v in cfg["recipeSources"].items()}
qualified={}
results=[]
try:
 for context,pair in cfg["contexts"].items():
  assert pair["before"].count(cfg["atomBefore"])==1 and pair["after"]==pair["before"].replace(cfg["atomBefore"],cfg["atomAfter"])
  root="com/synexia/rewrite/hash-pinned-java/"+pair["crate"]+"/"
  manifest=cfg["target"]+"\t"+ident(raw(pair["before"]))["sha256"]+"\t"+ident(raw(pair["after"]))["sha256"]+"\tSharedLexiconCatalog.java.txt\n"
  resources={root+"manifest.tsv":raw(manifest),root+"SharedLexiconCatalog.java.before":raw(pair["before"]),root+"SharedLexiconCatalog.java.txt":raw(pair["after"]),root+"atom-before.txt":raw(cfg["atomBefore"]),root+"atom-after.txt":raw(cfg["atomAfter"])}
  endpoint=nonce+"/recipe-result-"+context;recipeExpected[endpoint]=raw(pair["after"])
  run("recipe-"+context,recipe_sources,resources,"com.synexia.rewrite.M3MappingOptionalRecipeQualification",[base+endpoint,pair["crate"]])
  assert received[endpoint]==raw(pair["after"]);qualified[context]=received[endpoint]
 prod={n:v["content"] for n,v in cfg["sources"].items()}
 prod["jdk.m3.text.SharedLexiconCatalogSiWarmTest"]=cfg["incumbentTest"]["content"]
 prod["jdk.m3.text.M3MappingOptionalQualification"]=cfg["qa"]
 plan=["base-before","range-after","base-after","range-before","range-before","base-after","range-after","base-before"]
 for i,mode in enumerate(plan):
  context,variant=mode.split("-");label=mode+"-"+str(i);sources=dict(prod)
  sources["jdk.m3.text.SharedLexiconCatalog"]=qualified[context].decode() if variant=="after" else cfg["contexts"][context]["before"]
  run(label,sources,{"range-oracle.tsv":oracle},"jdk.m3.text.M3MappingOptionalQualification",[mode,str(image),base+nonce+"/result-"+label])
  result=received[nonce+"/result-"+label];assert result["mode"]==mode and result["found"]==13 and result["passed"]==13 and result["failed"]==0 and result["aborted"]==0
  assert result["evidence"]["oracleVectors"]==12288 and (result["evidence"]["differentFromDonor"]==0)==(context=="range")
  assert result["evidence"]["floorModCalls"]==(0 if context=="range" else 1)
  assert result["evidence"]["factoryLock"]["threadsReaped"] and result["evidence"]["factoryLock"]["closeQueuedWhileFactoryPaused"]
  assert len(result["measurements"])==42;results.append(result)
 assert len({r["evidence"]["apiAndFieldFingerprint"] for r in results})==1
 for scenario in ["numeric-hits","numeric-misses","mixed-language"]:
  samples=[m for r in results for m in r["measurements"] if m["scenario"]==scenario]
  assert len({m["answer"] for m in samples})==1
 assert image.read_bytes()==image_bytes
finally:
 if active is not None and active.poll() is None:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);active.wait(timeout=15)
 stop.set();watcher.join(timeout=10);server.shutdown();server.server_close();serverThread.join(timeout=5)
assert not errors and not watchErrors and len(results)==8
s_after=allocation(S);assert s_after==s_before
import statistics
comparison={}
for scenario in ["numeric-hits","numeric-misses","mixed-language"]:
 for consumer in ["non-escaping","escaping"]:
  key=scenario+"/"+consumer;comparison[key]={}
  for mode in ["base-before","base-after","range-before","range-after"]:
   samples=[m for r in results if r["mode"]==mode for m in r["measurements"] if m["scenario"]==scenario and m["consumer"]==consumer]
   per_process=[dict(nanos=statistics.median(m["nanos"] for m in r["measurements"] if m["scenario"]==scenario and m["consumer"]==consumer),bytes=statistics.median(m["bytes"] for m in r["measurements"] if m["scenario"]==scenario and m["consumer"]==consumer)) for r in results if r["mode"]==mode]
   comparison[key][mode]=dict(medianNanos=statistics.median(x["nanos"] for x in samples),medianBytes=statistics.median(x["bytes"] for x in samples),perProcess=per_process,samples=len(samples),callsPerSample=1000000,probes=next(r["evidence"][scenario+"Probes"] for r in results if r["mode"]==mode))
out=dict(schema="M3_MAPPING_OPTIONAL_QUALIFICATION_V1",status="PASS",reuseGate=cfg["reuseGate"],reports=reports,results=results,oracle=oracle_proof,recipeTests=14,productJUnitExecutions=104,positiveProductJUnitExecutions=104,contexts={n:dict(sourceBefore=ident(raw(p["before"])),sourceAfter=ident(qualified[n]),crate=p["crate"]) for n,p in cfg["contexts"].items()},existingImage=dict(path=str(image),identity=ident(image_bytes),unchanged=True),comparison=comparison,selection="PENDING_EVALUATION",storage=dict(readOnlyFInputBudgetBytes=f_before,readOnlyFBaselineReceipt=cfg["readOnlyBudgetBaseline"]["receiptBlob"],fFreshAllocationAudit="NOT_COMPLETED_TIMEOUT_90S_PRIOR_PHASE",sBefore=s_before,sAfter=s_after,capBytes=cap,reserveBytes=reserve,sFree=shutil.disk_usage(S).free,peakObservedTaskAllocations=observedPeaks,retainedBuildFiles=0),generationCalls=0,scope="One existing public Optional boundary plus private extraction under the same lifecycle read lock; exact fields/API/identities/cache/lifecycle unchanged. Recipe in master and pending range contexts. Unmodified SI post-admission semantics/warm/concurrency. Actual classbyte instrumentation validates factory-under-lock and blocked close; separate uninstrumented performance JVMs use escaping and non-escaping consumers. No hostJNI/fulladmission/otherplatform/universal speed guarantee.")
EMIT(out)
