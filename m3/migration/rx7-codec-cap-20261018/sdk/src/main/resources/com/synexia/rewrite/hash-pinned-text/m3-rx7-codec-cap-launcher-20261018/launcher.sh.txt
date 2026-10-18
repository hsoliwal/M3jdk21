#!/bin/sh
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Provider supervisor donor: f04b6bfc76e5a671968a478f44cf13268c04e5e9.
set -eu
python3 -I -S -B - <<'M3_RX7_CAP_PY'
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Adapted from the Apache-2.0 provider supervisor, Git blob f04b6bfc76e5a671968a478f44cf13268c04e5e9.
import hashlib,json,os,pathlib,re,resource,shutil,signal,stat,subprocess,time
import xml.etree.ElementTree as ET
MIB=1024**2
RESERVE,CAP,LOG_CAP,PROOF_CAP=8*1024**3,512*MIB,128*1024,2*MIB
SOURCE_PATH="m3/migration/rx7-codec-cap-20261018/sdk"
BINDING_PATH="m3/migration/rx7-codec-cap-20261018/source-binding.json"
WORKFLOW_PATH=".github/workflows/m3rx7-v7-image-codec-20261014.yml"
SCRIPT_PATH="m3/runtime-integration/check-m3rx7-v7-image-codec.sh"
CORE="src/java.base/share/classes/jdk/internal/mindex/"
TEST="test/jdk/java/util/regex/"
OLD={
 CORE+"M3RX7.java":"709ebea61c346c761662713e1cb7cdaf67f02bdfa49222ec23eef2353b44e0d2",
 CORE+"M3RX7Image.java":"5c9f80b832a722a9611a718d5c6228aa59721b5fae846267e05ac5c451353ba7",
 CORE+"M3RX7Codec.java":"fc6727011d2cc9027d43280ce692c4646c378438c0fb7be9c8f7ae7d0c1102c9",
 TEST+"M3RX7Test.java":"0c7d8ef062693a987d57c8fc36d7a69f015e08b12662d5a5774164276697d634",
 TEST+"M3RX7CodecTest.java":"bd2ed346fb4ba3945b0d7a14c04418f4996b2f11c4945e8fc14bdba24a99bd6a",
 "m3/docs/name-mapping.d/m3-regex-v7-image-codec.json":"32155be04c0d9635d7be7678609dde08a2b5dd797504a9a93752cf1c5050dfb6"}
EXPORTS={CORE+"M3RX7Codec.java":"8f224f3fb094f1b99a9a16a72c694bbb6914dfb629e1ad6421da2aa3364f7f5d",
 TEST+"M3RX7CodecBoundsTest.java":"4b7902424a6f6ad2640f38ffbdd5220a8cbbbfdecf7d0f1a0ca065e78eac1575"}
MARKER="M3_RX7_CODEC_CAP_SDK_PASS forwardStates=8 inverseStates=4 fixedPoints=12 compilerCases=3 originalOracles=2 boundedHeapProof=NOT_RUN outputs=2"
start=time.monotonic()
records=[]
root=pathlib.Path(os.environ["M3_PROOF_ROOT"])
source=pathlib.Path(os.environ["M3_SOURCE_ROOT"])
runner=pathlib.Path(os.environ["RUNNER_TEMP"])
sdk=source/SOURCE_PATH
build,overlay=root/"build",root/"emitted"
logs=root/"logs"
def need(ok,message):
 if not ok:raise RuntimeError(message)
def no_links(path):
 need(path.is_absolute() and path==pathlib.Path(os.path.normpath(path)),"absolute normalized path required")
 for item in (path,*path.parents):
  need(not item.is_symlink(),"symbolic-link path refused")
def read(path,maximum):
 no_links(path)
 need(path.is_file() and stat.S_ISREG(path.stat().st_mode),"bounded regular file required")
 need(path.stat().st_size<=maximum,"bounded source/log read")
 with path.open("rb") as stream:data=stream.read(maximum+1)
 need(len(data)<=maximum,"bounded source/log growth")
 return data
def digest(data):return hashlib.sha256(data).hexdigest()
def strict_json(data):
 def pairs(items):
  result={}
  for key,value in items:
   need(key not in result,"duplicate JSON field")
   result[key]=value
  return result
 return json.loads(data,object_pairs_hook=pairs,parse_constant=lambda _:(_ for _ in ()).throw(RuntimeError("nonfinite JSON")))
def relative(value):
 need(isinstance(value,str) and value and not value.startswith("/") and "\\" not in value
  and not any(ord(c)<32 or ord(c)==127 for c in value),"unsafe relative path")
 need(all(part not in ("",".","..",".git") for part in value.split("/")),"unsafe relative component")
 return value
def deadline():
 need(time.monotonic()-start<=720,"whole phase deadline")
def traversal_error(error):raise RuntimeError("storage census failed") from error
def gate(limit=480*MIB):
 used=0
 for base in (runner,source):
  no_links(base);need(base.is_dir(),"missing owned storage")
  for folder,names,files in os.walk(base,followlinks=False,onerror=traversal_error):
   for name in names+files:
    try:info=os.lstat(pathlib.Path(folder)/name)
    except FileNotFoundError:continue
    need(not stat.S_ISLNK(info.st_mode),"symlink in owned output/source tree")
    used+=max(info.st_size,info.st_blocks*512)
    need(used<=limit,"512MiB aggregate limit including artifact reserve")
 for base in (root,source):
  usage=os.statvfs(base)
  need(usage.f_bavail*usage.f_frsize>=RESERVE+CAP-used,"8GiB after planned output reserve")
 return used
def memory(required):
 values={}
 with open("/proc/meminfo","rt",encoding="ascii") as stream:
  for line in stream:
   key,_,value=line.partition(":")
   if key=="MemAvailable":values[key]=int(value.split()[0])*1024
 available=values.get("MemAvailable",0)
 current=pathlib.Path("/sys/fs/cgroup/memory.current")
 maximum=pathlib.Path("/sys/fs/cgroup/memory.max")
 if current.is_file() and maximum.is_file():
  bound=maximum.read_text(encoding="ascii").strip()
  if bound!="max":available=min(available,int(bound)-int(current.read_text(encoding="ascii").strip()))
 need(available>=required,"host/cgroup memory headroom")
 return available
for directory in (root,source,runner):no_links(directory);need(directory.is_dir(),"preflight roots missing")
need(root.parent==runner and source.parent==pathlib.Path(os.environ["GITHUB_WORKSPACE"]),"unexpected owned roots")
need(root!=source and root not in source.parents and source not in root.parents,"overlapping roots")
need(os.name=="posix" and pathlib.Path("/proc/self").is_dir(),"Linux execution lane required")
resource.setrlimit(resource.RLIMIT_CORE,(0,0))
for directory in (logs,build,overlay):
 no_links(directory);need(not directory.exists(),"fresh execution roots required")
 gate();directory.mkdir()
home,tmp,m2=root/"home",root/"tmp",root/"m2"
for directory in (home,tmp,m2,home/".m2"):
 no_links(directory);need(directory.is_dir(),"owned cache/temp preflight missing")
java_home=pathlib.Path(os.environ["M3_JAVA_HOME"])
maven=pathlib.Path(os.environ["M3_MAVEN"])
need(java_home.is_dir() and maven.is_file(),"preinstalled toolchains missing")
java=str(java_home/"bin/java");javac=str(java_home/"bin/javac")
env={key:os.environ[key] for key in ("PATH","LANG","LC_ALL","TZ") if key in os.environ}
env.update(HOME=str(home),USERPROFILE=str(home),MAVEN_USER_HOME=str(home/".m2"),
 TMPDIR=str(tmp),TEMP=str(tmp),TMP=str(tmp),JAVA_HOME=str(java_home),
 PATH=str(java_home/"bin")+os.pathsep+env.get("PATH","/usr/bin:/bin"),
 XDG_CACHE_HOME=str(root/"cache"),XDG_CONFIG_HOME=str(root/"config"),XDG_DATA_HOME=str(root/"data"),
 MAVEN_OPTS="-Xmx256m -Dfile.encoding=UTF-8 -Djava.io.tmpdir="+str(tmp)+" -Duser.home="+str(home)+" -Djansi.tmpdir="+str(tmp)+" -XX:+PerfDisableSharedMem",
 JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Djava.io.tmpdir="+str(tmp)+" -Duser.home="+str(home)+" -XX:+PerfDisableSharedMem -XX:-HeapDumpOnOutOfMemoryError -XX:ErrorFile="+str(logs/"hs_err_pid%p.log"),
 PYTHONDONTWRITEBYTECODE="1",GIT_OPTIONAL_LOCKS="0",GIT_NO_LAZY_FETCH="1",GIT_TERMINAL_PROMPT="0",
 GIT_CONFIG_NOSYSTEM="1",GIT_CONFIG_GLOBAL=os.devnull,GCM_INTERACTIVE="never")
def absent_group(pid):
 try:os.killpg(pid,0)
 except ProcessLookupError:return True
 return False
def cleanup(child):
 # This proves absence of the original process group, not escaped groups/sessions.
 if child.poll() is None:
  try:os.killpg(child.pid,signal.SIGTERM)
  except ProcessLookupError:pass
  try:child.wait(timeout=2)
  except subprocess.TimeoutExpired:
   try:os.killpg(child.pid,signal.SIGKILL)
   except ProcessLookupError:pass
   child.wait(timeout=5)
 else:child.wait(timeout=5)
 for sig,seconds in ((signal.SIGTERM,2),(signal.SIGKILL,3)):
  if absent_group(child.pid):return
  try:os.killpg(child.pid,sig)
  except ProcessLookupError:return
  end=time.monotonic()+seconds
  while time.monotonic()<end:
   if absent_group(child.pid):return
   time.sleep(.05)
 raise RuntimeError("original process group terminal state unproved")
def run(name,command,seconds=60,expected=0,heap=384*MIB):
 need(re.fullmatch("[a-z0-9-]{1,72}",name),"unsafe command label")
 deadline();gate();memory(heap+256*MIB)
 path=logs/(str(len(records)).zfill(2)+"-"+name+".log")
 child=None;code=None;failure=None;began=time.monotonic();clean=False
 try:
  with path.open("xb") as output:
   child=subprocess.Popen(command,cwd=source,env=env,stdin=subprocess.DEVNULL,
    stdout=output,stderr=subprocess.STDOUT,start_new_session=True)
   while child.poll() is None:
    deadline();gate();memory(256*MIB)
    need(path.stat().st_size<=LOG_CAP,"per-command log cap")
    need(time.monotonic()-began<=seconds,"per-command deadline")
    time.sleep(.25)
   code=child.wait(timeout=5)
   deadline();gate();need(path.stat().st_size<=LOG_CAP,"terminal log cap")
   need(time.monotonic()-began<=seconds,"terminal command deadline")
 except BaseException as error:
  failure=type(error).__name__
  raise
 finally:
  if child is not None:
   cleanup(child);clean=True
  records.append({"name":name,"returncode":code if code is not None else (child.returncode if child else None),
   "seconds":round(time.monotonic()-began,3),"log":str(path.relative_to(root)),
   "parentReaped":clean,"originalProcessGroupAbsent":clean,"termination":failure})
 gate()
 data=read(path,LOG_CAP)
 need(code==expected,name+" unexpected exit status "+str(code))
 return data.decode("utf-8",errors="replace")
def git(*arguments):
 text=run("git-"+str(len(records)),["git","--no-optional-locks",*arguments],15,heap=32*MIB).strip()
 need(len(text)<=4096,"bounded Git result")
 return text
def checkout_file(path):
 relative(path);actual=git("hash-object","--no-filters","--",path)
 need(re.fullmatch("[0-9a-f]{40}",actual) is not None and actual==git("rev-parse","HEAD:"+path),"working tree differs from exact HEAD")
def hash_file(path,expected,maximum=1024*1024):
 data=read(path,maximum);need(digest(data)==expected,"source/export hash drift: "+str(path.relative_to(source) if source in path.parents else path.name))
 return data
receipt={"schema":"M3_RX7_CODEC_CAP_HOSTED_V1","status":"FAILED","phaseAdvanced":False,
 "productionRuntimeApplied":False,"fullJdkQualified":False,"jniQualified":False,"performanceQualified":False,
 "containment":"owned original process group; escaped groups/sessions not contained",
 "storage":"cooperative allocated/logical census; not an OS quota","commands":records}
report=None
try:
 head=git("rev-parse","HEAD")
 need(re.fullmatch("[0-9a-f]{40}",head) is not None and head==os.environ["EXPECTED_SOURCE_SHA"],"checkout must be exact trusted candidate HEAD")
 binding_bytes=read(source/BINDING_PATH,64*1024);binding=strict_json(binding_bytes)
 need(binding.get("schema")=="SYNEXIA_RX7_CODEC_CAP_BINDING_V1"
  and binding.get("repository")=="hsoliwal/com.synexia" and binding.get("receiverCorePath")==SOURCE_PATH,"binding owner/schema drift")
 need(re.fullmatch("[0-9a-f]{40}",binding.get("sourceCommit","")) is not None,"immutable canonical source commit required")
 need(binding.get("runtimeApplied") is False and binding.get("phaseAdvanced") is False,"binding may not claim production promotion")
 tree=git("rev-parse","HEAD:"+SOURCE_PATH)
 pins_bytes=read(sdk/"pins.json",256*1024);pins=strict_json(pins_bytes)
 need(tree==binding.get("moduleGitTree") and digest(pins_bytes)==binding.get("pinsSha256"),"exact SDK tree/pins binding drift")
 need(pins.get("schema")=="M3_RX7_CODEC_CAP_SDK_PINS_V1","SDK pins schema drift")
 files=pins.get("files");need(isinstance(files,dict) and 1<=len(files)<=128,"closed SDK pins map required")
 expected_names={"activatesNamedForwardAndInverseDescriptors","qualifiesActualRecipeAndProtectedOraclesBeforeExport","qualifiesLauncherRecipeAndFixedPoints"}
 execution=pins.get("expectedExecution",{})
 need(execution.get("junitClass")=="com.synexia.rewrite.M3RX7CodecCapRecipeTest"
  and isinstance(execution.get("namedMethods"),list)
  and len(execution["namedMethods"])==3 and set(execution["namedMethods"])==expected_names,"SDK declared execution inventory drift")
 observed=set()
 for folder,names,entries in os.walk(sdk,followlinks=False,onerror=traversal_error):
  for name in names+entries:no_links(pathlib.Path(folder)/name)
  for name in entries:
   path=pathlib.Path(folder)/name;relative_path=path.relative_to(sdk).as_posix()
   need(path.is_file(),"nonregular SDK entry")
   if relative_path!="pins.json":observed.add(relative_path)
 need(observed==set(files),"SDK pins must cover every nonself file exactly")
 for path,pin in files.items():
  relative(path)
  need(isinstance(pin,dict) and set(pin)=={"sha256","bytes","license"},"invalid SDK file pin shape")
  need(isinstance(pin["sha256"],str) and re.fullmatch("[0-9a-f]{64}",pin["sha256"]) is not None
   and type(pin["bytes"]) is int and 0<pin["bytes"]<=1024*1024
   and isinstance(pin["license"],str) and bool(pin["license"]),"invalid SDK file pin")
  data=hash_file(sdk/path,pin["sha256"],1024*1024)
  need(len(data)==pin["bytes"],"SDK pinned byte count drift")
 for path in (BINDING_PATH,SCRIPT_PATH,WORKFLOW_PATH):checkout_file(path)
 for path,wanted in OLD.items():hash_file(source/path,wanted)
 need(not (source/(TEST+"M3RX7CodecBoundsTest.java")).exists(),"bounds test must remain an unapplied candidate")
 need((sdk/"pom.xml").is_file(),"closed Maven recipe POM missing")
 template_home=sdk/"src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-rx7-codec-cap-launcher-20261018"
 need(read(source/SCRIPT_PATH,64*1024)==read(template_home/"launcher.sh.txt",64*1024),"active launcher is not exact SDK postimage")
 need(read(source/WORKFLOW_PATH,64*1024)==read(template_home/"workflow.yml.txt",64*1024),"active workflow is not exact SDK postimage")
 need(digest(read(source/SCRIPT_PATH,64*1024))==binding.get("launcherSha256")
  and digest(read(source/WORKFLOW_PATH,64*1024))==binding.get("workflowSha256"),"launcher/workflow binding drift")
 java_version=run("java-version",[java,"-version"],15,heap=64*MIB)
 javac_version=run("javac-version",[javac,"-version"],15,heap=128*MIB)
 need(re.search(r'version "21[.\"]',java_version) is not None and re.search(r"\bjavac 21(?:\.|\s)",javac_version),"actual Java21 toolchain required")
 settings=home/".m2"
 for filename in ("settings.xml","global-settings.xml"):
  need(read(settings/filename,1024)==b'<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"/>\n',"explicit empty Maven settings required")
 maven_log=run("actual-maven-openrewrite-sdk",[str(maven),"-B","-ntp","-s",str(settings/"settings.xml"),
  "-gs",str(settings/"global-settings.xml"),"-Dmaven.repo.local="+str(m2),"-f",str(sdk/"pom.xml"),
  "-Dm3.build.root="+str(build),"-Dm3.output.root="+str(overlay),"-Duser.home="+str(home),"verify"],360,heap=768*MIB)
 report=build/"surefire-reports/TEST-com.synexia.rewrite.M3RX7CodecCapRecipeTest.xml"
 suite=ET.fromstring(read(report,256*1024))
 cases=suite.findall("testcase")
 names={case.get("name") for case in cases}
 need(int(suite.get("tests","0"))==len(cases)==3 and len(names)==len(cases),"actual named SDK tests required")
 need(all(int(suite.get(key,"0"))==0 for key in ("failures","errors","skipped")),"SDK tests failed or skipped")
 need(names==expected_names,"exact three SDK methods missing")
 need(all(case.get("classname")=="com.synexia.rewrite.M3RX7CodecCapRecipeTest"
  and not any(case.find(key) is not None for key in ("failure","error","skipped")) for case in cases),"unexecuted/failing SDK case")
 need(MARKER in maven_log or MARKER in "".join(suite.itertext()),"actual SDK export marker absent")
 emitted={}
 for folder,names,entries in os.walk(overlay,followlinks=False,onerror=traversal_error):
  for name in names+entries:no_links(pathlib.Path(folder)/name)
  for name in entries:
   path=pathlib.Path(folder)/name;relative_path=path.relative_to(overlay).as_posix()
   need(path.is_file() and relative_path in EXPORTS,"undeclared SDK output")
   emitted[relative_path]=hash_file(path,EXPORTS[relative_path],64*1024)
 need(set(emitted)==set(EXPORTS),"actual recipe must emit exactly codec and bounds main")
 for path,wanted in OLD.items():hash_file(source/path,wanted)
 baseline_resource=sdk/"src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-rx7-codec-cap-20261018-inverse/M3RX7Codec.java.txt"
 need(read(baseline_resource,64*1024)==read(source/(CORE+"M3RX7Codec.java"),64*1024),"baseline is not exact original codec")
 def compile_case(label,candidate):
  gate();directory=root/label;directory.mkdir()
  src=directory/"sources";classes=directory/"classes";tests=directory/"testclasses";empty=directory/"empty"
  for path in (src,classes,tests,empty):path.mkdir()
  production=[]
  for name in ("M3RX7.java","M3RX7Image.java","M3RX7Codec.java"):
   relative_path=CORE+name
   content=emitted[relative_path] if candidate and relative_path in emitted else read(source/relative_path,64*1024)
   destination=src/name;gate();destination.write_bytes(content);production.append(str(destination))
  common=[javac,"-J-Xmx256m","-encoding","UTF-8","-proc:none","-implicit:none","-Xlint:all","-Werror",
   "-classpath",str(empty),"-sourcepath",str(empty)]
  run(label+"-compile-owners",common+["--patch-module","java.base="+str(src),"-d",str(classes),*production],60)
  inputs=[]
  for name in ("M3RX7Test.java","M3RX7CodecTest.java","M3RX7CodecBoundsTest.java"):
   relative_path=TEST+name
   content=emitted[relative_path] if relative_path in emitted else read(source/relative_path,64*1024)
   destination=src/name;gate();destination.write_bytes(content);inputs.append(str(destination))
  run(label+"-compile-oracles",common+["--patch-module","java.base="+str(classes),
   "--add-exports","java.base/jdk.internal.mindex=ALL-UNNAMED","-d",str(tests),*inputs],60)
  return classes,tests
 baseline_classes,baseline_tests=compile_case("baseline",False)
 def java_command(classes,tests,main,interpreted):
  return [java,"-Xmx96m",*(["-Xint"] if interpreted else []),"-Xcheck:jni",
   "--patch-module","java.base="+str(classes),"--add-exports","java.base/jdk.internal.mindex=ALL-UNNAMED",
   "-cp",str(tests),main]
 baseline=run("baseline-clone-counterexample",java_command(baseline_classes,baseline_tests,"M3RX7CodecBoundsTest",True),45,expected=1,heap=256*MIB)
 need(re.search(r"(?m)^M3RX7CodecBoundsTest INPUT_READY bytes=67108865$",baseline) is not None,"counterexample never allocated intended input")
 need("java.lang.OutOfMemoryError: Java heap space" in baseline
  and re.search(r"java\.lang\.Object\.clone\(Native Method\)",baseline) is not None
  and "jdk.internal.mindex.M3RX7Codec.decodePrecomputed(" in baseline,"baseline failure is not decoder clone OOM")
 need(" PASS" not in baseline,"baseline cannot pass bounded allocation regression")
 candidate_classes,candidate_tests=compile_case("candidate",True)
 markers={"M3RX7Test":r"(?m)^M3RX7Test candidate_oracle_checks=[1-9][0-9]* PASS$",
  "M3RX7CodecTest":r"(?m)^M3RX7CodecTest checks=[1-9][0-9]* PASS$",
  "M3RX7CodecBoundsTest":r"(?m)^M3RX7CodecBoundsTest PASS$"}
 for interpreted in (False,True):
  for main,pattern in markers.items():
   output=run("candidate-"+("int-" if interpreted else "mixed-")+main.lower(),java_command(candidate_classes,candidate_tests,main,interpreted),45,heap=256*MIB)
   need(re.search(pattern,output) is not None and "OutOfMemoryError" not in output,"candidate missing protected oracle or bounds success")
 for path,wanted in OLD.items():hash_file(source/path,wanted)
 for path,wanted in EXPORTS.items():hash_file(overlay/path,wanted,64*1024)
 receipt.update(status="PASSED",checkoutCommit=head,canonicalSourceCommit=binding["sourceCommit"],
  sdkTree=tree,pinsSha256=digest(pins_bytes),sdkTests=len(cases),exports=EXPORTS,
  boundedHeapCounterexample="EXACT_OLD_CODEC_CLONE_OOM_AFTER_INPUT_READY",
  boundedHeapCandidate="BOTH_ORIGINAL_ORACLES_AND_BOUNDS_NORMAL_AND_XINT_PASSED",
  heapBytes=96*MIB)
except BaseException as error:
 receipt["failure"]=str(error)[:1024]
 raise
finally:
 # Never create failure/success artifacts without fresh reserve and planned-copy gates.
 before=gate(496*MIB)
 selected=[source/BINDING_PATH] if (source/BINDING_PATH).is_file() else []
 selected.extend(sorted(logs.glob("*.log")))
 if report is not None and report.is_file():selected.append(report)
 for path in EXPORTS:
  if (overlay/path).is_file():selected.append(overlay/path)
 need(len(selected)<=64,"closed proof file-count cap")
 payloads=[]
 for index,path in enumerate(selected):
  data=read(path,256*1024)
  payloads.append((str(index).zfill(2)+"-"+path.name,data))
 receipt["commands"]=records
 payloads.append(("hosted-receipt.json",(json.dumps(receipt,indent=2,sort_keys=True)+"\n").encode("utf-8")))
 inventory={name:{"bytes":len(data),"sha256":digest(data)} for name,data in payloads}
 inventory_bytes=(json.dumps({"selfExcluded":True,"files":inventory},indent=2,sort_keys=True)+"\n").encode("utf-8")
 planned=sum(len(data) for _,data in payloads)+len(inventory_bytes)
 need(planned<=PROOF_CAP and before+planned+MIB<=496*MIB,"closed proof exceeds2MiB or reserved storage")
 bundle=root/"proof";need(not bundle.exists(),"fresh closed proof root required");bundle.mkdir()
 for name,data in payloads:
  gate(496*MIB);(bundle/name).write_bytes(data)
 gate(496*MIB);(bundle/"inventory.json").write_bytes(inventory_bytes)
 gate(496*MIB)
 print("RX7_CODEC_CAP_PROOF status="+receipt["status"]+" files="+str(len(payloads)+1)+" bytes="+str(planned)+" productionApplied=false fullJdkQualified=false jniQualified=false")
M3_RX7_CAP_PY
