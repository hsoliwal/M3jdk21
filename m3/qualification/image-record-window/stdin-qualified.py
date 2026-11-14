import os,sys,ctypes,shutil,json,hashlib,traceback,zlib
b=ctypes.create_unicode_buffer(32768);assert ctypes.windll.kernel32.GetTempPathW(len(b),b);assert b.value.rstrip("\\").lower()==r"F:\codex-work\agent-temp".lower()
assert shutil.disk_usage(r"S:\codex-work\agent-temp\lexicon-maven-r224").free-65536>=8589934592
for n in ("TEMP","TMP","TMPDIR"):os.environ[n]=r"S:\codex-work\agent-temp\lexicon-maven-r224\temp"
b=ctypes.create_unicode_buffer(32768);assert ctypes.windll.kernel32.GetTempPathW(len(b),b);assert b.value.rstrip("\\").lower()==r"S:\codex-work\agent-temp\lexicon-maven-r224\temp".lower()
k=ctypes.windll.kernel32;k.GetStdHandle.restype=ctypes.c_void_p;h=k.GetStdHandle(-10);m=ctypes.c_ulong();assert k.GetConsoleMode(ctypes.c_void_p(h),ctypes.byref(m));assert k.SetConsoleMode(ctypes.c_void_p(h),m.value&~4)
print("M3_STDIN_READY",flush=True)
raw=bytes.fromhex("".join(sys.stdin.readline().strip() for _ in range(788)));assert len(raw)<=2097152 and hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()=="53faa54b2f2f73da1bf29afd973fad27390521c7"
def emit(v):
 import subprocess
 d=json.dumps(v,ensure_ascii=True,separators=(",",":")).encode()
 w=dict(schema="M3_ZLIB_HEX_PROOF_V1",uncompressedBytes=len(d),uncompressedSha256=hashlib.sha256(d).hexdigest(),payload=zlib.compress(d,9).hex())
 value=json.dumps(w,separators=(",",":"))
 sha=hashlib.sha1(b"blob "+str(len(value.encode())).encode()+b"\0"+value.encode()).hexdigest()
 env=os.environ.copy();env.pop("GH_DEBUG",None);env.update(GH_NO_UPDATE_NOTIFIER="1",GH_NO_EXTENSION_UPDATE_NOTIFIER="1",GH_PROMPT_DISABLED="1",GH_PAGER="cat")
 req=json.dumps(dict(content=value,encoding="utf-8"),separators=(",",":")).encode()
 r=subprocess.run([shutil.which("gh"),"api","--hostname","github.com","--method","POST","repos/hsoliwal/com.synexia/git/blobs","--input","-"],input=req,env=env,cwd=r"S:\codex-work\agent-temp\lexicon-maven-r224",capture_output=True,timeout=45)
 if r.returncode==0:
  saved=json.loads(r.stdout);assert saved["sha"]==sha
  out=dict(schema="M3_PERSISTED_RECEIPT_V1",status=v.get("status","FAIL"),receiptBlob=sha,bytes=len(d),sha256=w["uncompressedSha256"],recipeTests=v.get("recipeTests"),productJUnitExecutions=v.get("productJUnitExecutions"),storage=v.get("storage"),inputBlob="53faa54b2f2f73da1bf29afd973fad27390521c7")
  s=json.dumps(out,ensure_ascii=True,separators=(",",":"))
  for i in range(0,len(s),60):print("p"+str(i//60).zfill(5)+"="+s[i:i+60],flush=True)
  print("M3_PERSIST_END",flush=True);return
 print("M3_PERSIST_UNCONFIRMED="+sha,flush=True)
 s=json.dumps(dict(schema=v.get("schema"),status=v.get("status","FAIL"),sha256=w["uncompressedSha256"],bytes=len(d),error=v.get("error")),separators=(",",":"));z=w["payload"]
 print("M3_FRAME_START",flush=True)
 for i in range(0,len(s),60):print("m"+str(i//60).zfill(5)+"="+s[i:i+60],flush=True)
 for i in range(0,len(z),60):print("z"+str(i//60).zfill(5)+"="+z[i:i+60],flush=True)
 print("M3_FRAME_END",flush=True)
ctx=dict(INPUT=json.loads(raw),EMIT=emit,__name__="__main__")
try:exec(compile(ctx["INPUT"]["code"],"sealed-image-record-window.py","exec"),ctx)
except Exception as e:
 emit(dict(schema="M3_IMAGE_WINDOW_FAILURE_V1",error=str(e),traceback=traceback.format_exc(),reports=ctx.get("reports",[]),observations=ctx.get("observations",[]),watchErrors=ctx.get("watchErrors",[]),storageErrors=ctx.get("errors",[])));sys.exit(1)
