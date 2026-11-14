# Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
# QA composition of existing Maven/RAM compiler, exact snapshot recipe and SharedLexiconImage.
import os,sys,json,hashlib,base64,ctypes,shutil,subprocess,http.server,threading,uuid,time,struct
from pathlib import Path
import tempfile
cfg=INPUT
T=Path(r"F:\codex-work\lexicon-migration-20261031");S=Path(r"K:\codex-work\agent-temp\lexicon-maven-r241")
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
assert S.resolve().drive.upper()=="K:" and S.resolve().is_relative_to(Path(r"K:\codex-work\agent-temp").resolve())
for child in [S/"temp",S/"home",S/"home/.m2",S/"repo-head"]:assert child.is_dir() and not child.is_symlink()
os.environ["TEMP"]=str(S/"temp");os.environ["TMP"]=str(S/"temp");tempfile.tempdir=str(S/"temp")
k=ctypes.WinDLL("kernel32",use_last_error=True);ps=ctypes.WinDLL("psapi",use_last_error=True)
b=ctypes.create_unicode_buffer(32768);assert k.GetTempPathW(len(b),b);assert Path(b.value).resolve()==(S/"temp").resolve()
assert ctypes.sizeof(ctypes.c_void_p)==8
spc=ctypes.c_ulong();bps=ctypes.c_ulong();freec=ctypes.c_ulong();allc=ctypes.c_ulong()
assert k.GetDiskFreeSpaceW("K:\\",ctypes.byref(spc),ctypes.byref(bps),ctypes.byref(freec),ctypes.byref(allc));assert spc.value*bps.value==4096
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
   if self.path in recipeExpected:
    lines=data.decode().splitlines();assert len(lines)==1
    path,sha,value=lines[0].split("\t");value=base64.b64decode(value,validate=True)
    expected_path,expected_value=recipeExpected[self.path]
    assert path==expected_path and value==expected_value and ident(value)["sha256"]==sha
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
 if label.startswith(("original-","digest-","after-")):argv=cfg["performanceJvmArgs"]+argv
 command=" ".join('"'+s.replace('"','\\"')+'"' for s in argv)
 argv=[r"C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin\mvn.cmd","-o","-B","-ntp","-f",str(pom),"-Dmaven.repo.local="+str(S/"repo-head"),"-Dmaven.repo.local.tail="+str(T/"range-current-qualification/proof/repo-head")+",C:/Users/hsoliwal/.m2/repository","-Dmaven.repo.local.tail.ignoreAvailability=false","-Dexec.args="+command,"org.codehaus.mojo:exec-maven-plugin:3.3.0:exec"]
 started=time.monotonic();print("M3_SHARED_TEST_STAGE_STARTED "+label,flush=True)
 active=subprocess.Popen(argv,cwd=S,env=env,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 try:log,_=active.communicate(timeout=180)
 except subprocess.TimeoutExpired:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);log,_=active.communicate(timeout=15);raise AssertionError(label+" timeout")
 item=dict(stage=label,pid=active.pid,argv=argv,packetIdentity=ident(p),sourcePins={n:ident(raw(v)) for n,v in sources.items()},exit=active.returncode,reaped=active.poll() is not None,elapsedSeconds=round(time.monotonic()-started,3),log=log.decode("utf-8","replace"),logIdentity=ident(log));reports.append(item);active=None
 assert item["exit"]==expected and not watchErrors and not errors,item["log"][-16000:]
 if expected==0:assert "M3_CPU_RAM_COMPILE_PASS" in item["log"],label
 else:assert "M3_ARRAY_PROVIDER_EXPECTED_ABI_REFUSAL" in item["log"],label
 budget();assert allocation(S)==(s_before if retainedOutputBytes is None else retainedOutputBytes),"retained task output";return item

failure=None;results=[];owned_pom=None;owned_pom_created=False;replayMetrics={"status":"ASSEMBLED_BY_ENTRYPOINT"}
try:
 import types,random,math,ast,unittest,io
 if cfg.get("replayQualification"):
  rq=cfg["replayQualification"];scope={"__name__":"m3_replay_adapter_proof"};exec(compile(rq["source"],"<replay-adapter>","exec"),scope)
  assembled=scope["assemble"](lambda path:rq["files"][path]);assert assembled=={n:v for n,v in cfg.items() if n!="replayQualification"}
  bad=dict(rq["files"]);bad["run.py"]+="\n# unexpected drift\n"
  try:scope["assemble"](lambda path:bad[path]);raise AssertionError("replay admitted drift")
  except ValueError:pass
  replayMetrics={"status":"PASS","exactInputReconstruction":True,"driftRefusal":True,"readOnlyInputFiles":len(rq["files"])}
 module=types.ModuleType("m3_exporter_r243");sys.modules[module.__name__]=module
 owned_pom=S/"m3-precompute-r243-pom.xml";assert not owned_pom.exists();assert len(raw(cfg["taskPom"]))<4096
 with owned_pom.open("xb") as stream:owned_pom_created=True;stream.write(raw(cfg["taskPom"]))
 pom=owned_pom;budget()
 root="jdk/m3/rewrite/backport/jdk21-hash-pinned-text/precompute-canonical-r243/"
 resources={root+"manifest.tsv":raw(cfg["manifest"]),root+"export-m3-lexicon.py.txt":raw(cfg["after"]),"before.py":raw(cfg["exporter"]),"after.py":raw(cfg["after"])}
 recipe_sources={n:v["content"] for n,v in cfg["recipeSources"].items()};recipe_sources["jdk.m3.qualification.M3PrecomputeRecipeQualification"]=cfg["qa"]
 callback=nonce+"/result-precompute-recipe"
 run("precompute-canonical-recipe",recipe_sources,resources,"jdk.m3.qualification.M3PrecomputeRecipeQualification",[base+callback],retainedOutputBytes=s_before+4096)
 result=received[callback];assert result["found"]==result["passed"]==4 and result["failed"]==result["skipped"]==result["aborted"]==0,result
 generated=result["generated"];assert generated==cfg["after"];result["generatedIdentity"]=ident(raw(generated));del result["generated"];results.append(result)
 exec(compile(generated,"<recipe-produced-exporter>","exec"),module.__dict__)
 # Execute the unchanged test class body from the committed test source; only its filesystem import loader is replaced by the sealed RAM module.
 parsed=ast.parse(cfg["pythonTest"]);units=[node for node in parsed.body if isinstance(node,(ast.Import,ast.ImportFrom,ast.ClassDef))]
 test_scope={"EXPORT":module,"__name__":"m3_precompute_canonical_tests"}
 exec(compile(ast.Module(body=units,type_ignores=[]),"<sealed-python-tests>","exec"),test_scope)
 suite=unittest.defaultTestLoader.loadTestsFromTestCase(test_scope["PrecomputeCanonicalTest"])
 stream=io.StringIO();pyresult=unittest.TextTestRunner(stream=stream,verbosity=2).run(suite)
 assert pyresult.testsRun==7 and pyresult.wasSuccessful(),stream.getvalue()
 results.append(dict(stage="python-regression",found=pyresult.testsRun,passed=pyresult.testsRun,lenientJsonShapesPreserved=True,log=stream.getvalue(),pythonVersion=sys.version))
 bitset={0,1<<63}
 def add_bits(bits):
  if 0<=bits<0x7ff0000000000000:
   bitset.add(bits);bitset.add(bits|(1<<63))
 for bits in range(1,1025):add_bits(bits)
 for exponent in range(1,2047):
  for delta in (-1,0,1):add_bits((exponent<<52)+delta)
 for exponent in range(-323,309):
  value=float("1e"+str(exponent));bits=struct.unpack(">Q",struct.pack(">d",value))[0]
  for delta in (-1,0,1):add_bits(bits+delta)
 rng=random.Random(0x24320261114)
 for _ in range(5000):add_bits(rng.getrandbits(63))
 float_rows=[]
 for bits in sorted(bitset):
  value=struct.unpack(">d",struct.pack(">Q",bits))[0]
  float_rows.append(format(bits,"016x")+"\t"+module._java_double_text(value))
 floats="\n".join(float_rows)+"\n"
 cases=[]
 def add_case(label,value):
  text=json.dumps({"x":value},ensure_ascii=True,separators=(",",":"));produced=module.canonical_precompute_payload(text)
  assert module.canonical_precompute_payload(produced)==produced
  cases.append(dict(label=label,producer=produced))
 for i in range(256):add_case("char-"+str(i),chr(i))
 for i,value in enumerate([0.0,-0.0,1e-5,1e20,1e23,273.15,0.001,1e7,float.fromhex("0x0.0000000000001p-1022"),float.fromhex("0x1.fffffffffffffp1023"),r"\u007f","\\\x7f","a\"1e-5\\","\ud800","\udc00","\U0001f600"]):add_case("boundary-"+str(i),value)
 callback=nonce+"/result-precompute-wire"
 wire_sources={n:v["content"] for n,v in cfg["sources"].items()};wire_sources["jdk.m3.text.M3PrecomputeWireQualification"]=cfg["qa2"]
 run("precompute-canonical-java-oracle",wire_sources,{"floats.tsv":raw(floats),"cases.json":raw(json.dumps(cases,ensure_ascii=True))},"jdk.m3.text.M3PrecomputeWireQualification",[base+callback],retainedOutputBytes=s_before+4096)
 result=received[callback];assert result["found"]==result["passed"]==3 and result["failed"]==result["skipped"]==result["aborted"]==0,result
 assert result["metrics"]["finiteDoubleCases"]==len(bitset) and result["metrics"]["payloadCases"]==len(cases)
 result["corpusGeneratorSeed"]="0x24320261114";result["floatCorpusIdentity"]=ident(raw(floats));result["payloadCorpusIdentity"]=ident(raw(json.dumps(cases,ensure_ascii=True)));results.append(result)
except BaseException as ex:failure=dict(type=type(ex).__name__,message=str(ex)[:20000])
finally:
 if active is not None and active.poll() is None:
  subprocess.run(["taskkill","/PID",str(active.pid),"/T","/F"],env=env,capture_output=True,timeout=15);active.wait(timeout=15)
 if owned_pom_created and owned_pom is not None and owned_pom.exists():
  try:
   assert owned_pom.resolve().parent==S.resolve() and owned_pom.name=="m3-precompute-r243-pom.xml" and owned_pom.read_bytes()==raw(cfg["taskPom"])
   owned_pom.unlink()
  except Exception as ex:failure=dict(type="TemporaryPomCleanupRefused",message=str(ex))
 stop.set();watcher.join(timeout=10);server.shutdown();server.server_close();serverThread.join(timeout=5)
out=dict(schema="M3_PRECOMPUTE_CANONICAL_QUALIFICATION_R243",status="PASS" if failure is None and len(results)==3 and not errors and not watchErrors and allocation(S)==s_before else "FAIL",failure=failure,reports=reports,results=results,storage=dict(before=s_before,after=allocation(S),readOnlyFBytes=f_before,cap=cap,reserve=reserve,free=shutil.disk_usage(S).free,peaks=observedPeaks),generationCalls=0,replayAssembly=replayMetrics,inputPins=dict(exporter=ident(raw(cfg["exporter"])),afterExporter=ident(raw(cfg["after"])),pythonTest=ident(raw(cfg["pythonTest"])),recipe=ident(raw(next(iter(cfg["recipeSources"].values()))["content"]))))
EMIT(out)
if out["status"]!="PASS":raise SystemExit(2)
