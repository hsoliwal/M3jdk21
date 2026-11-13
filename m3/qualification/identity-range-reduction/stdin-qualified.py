import os,sys,ctypes,shutil,json,hashlib,traceback,zlib
b=ctypes.create_unicode_buffer(32768);assert ctypes.windll.kernel32.GetTempPathW(len(b),b);assert b.value.rstrip("\\").lower()==r"F:\codex-work\agent-temp".lower()
assert shutil.disk_usage(r"S:\codex-work\agent-temp\lexicon-maven-r224").free-65536>=8589934592
k=ctypes.windll.kernel32;k.GetStdHandle.restype=ctypes.c_void_p;h=k.GetStdHandle(-10);m=ctypes.c_ulong();assert k.GetConsoleMode(ctypes.c_void_p(h),ctypes.byref(m));assert k.SetConsoleMode(ctypes.c_void_p(h),m.value&~4)
print("M3_STDIN_READY",flush=True)
raw=bytes.fromhex("".join(sys.stdin.readline().strip() for _ in range(871)));assert len(raw)<=2097152 and hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()=="eaf05e6c6405784a2ea4c1adb1cd1a06da95a627"
def emit(v):
 d=json.dumps(v,ensure_ascii=True,separators=(",",":")).encode();s=json.dumps(dict(schema=v.get("schema"),status=v.get("status","FAIL"),sha256=hashlib.sha256(d).hexdigest(),bytes=len(d),recipeTests=v.get("recipeTests"),readerJUnitExecutions=v.get("readerJUnitExecutions"),storage=v.get("storage"),error=v.get("error")),separators=(",",":"));z=zlib.compress(d,9).hex()
 print("M3_FRAME_START",flush=True)
 for i in range(0,len(s),60):print("m"+str(i//60).zfill(5)+"="+s[i:i+60],flush=True)
 for i in range(0,len(z),60):print("z"+str(i//60).zfill(5)+"="+z[i:i+60],flush=True)
 print("M3_FRAME_END",flush=True)
ctx=dict(INPUT=json.loads(raw),EMIT=emit,__name__="__main__")
try:exec(compile(ctx["INPUT"]["code"],"sealed-identity-range.py","exec"),ctx)
except Exception as e:
 emit(dict(schema="M3_IDENTITY_RANGE_FAILURE_V1",error=str(e),traceback=traceback.format_exc(),reports=ctx.get("reports",[]),observations=ctx.get("observations",[]),watchErrors=ctx.get("watchErrors",[]),storageErrors=ctx.get("errors",[])));sys.exit(1)
