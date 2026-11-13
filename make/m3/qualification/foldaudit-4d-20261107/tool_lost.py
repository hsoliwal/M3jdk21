import importlib.util, os, subprocess, sys
spec = importlib.util.spec_from_file_location("jn", "S:/m3-scratch/converge-20261106/a3/jn/jdk_naming.py"); jn = importlib.util.module_from_spec(spec); spec.loader.exec_module(jn)
env = dict(os.environ, GIT_DIR="H:/m3-scratch/repos/M3jdk21-release.git")
M = subprocess.run(["git", "rev-parse", "refs/remotes/origin/master"], capture_output=True, text=True, env=env).stdout.strip()
def show(r, p):
    x = subprocess.run(["git", "show", f"{r}:{p}"], capture_output=True, env=env); return x.stdout.decode("utf-8", "replace") if x.returncode == 0 else None
def norm(t): return [" ".join(l.split()) for l in t.splitlines() if " ".join(l.split()) and not " ".join(l.split()).startswith(("//", "*", "/*"))]
for spec_ in sys.argv[1:]:
    leaf, head, opath, mpath = spec_.split("|")
    origin = show(head, opath); now = show(M, mpath)
    if now is None: print(f"{leaf}\tOWNER_ABSENT_ON_MASTER\t{mpath}"); continue
    renamed, _ = jn.rewrite_text(origin)
    have = set(norm(now)); lost = [s for s in norm(renamed) if s not in have]
    print(f"{leaf}\tlost_after_rename={len(lost)}\t{mpath.split('/')[-1]}\t" + " || ".join(x[:70] for x in lost[:3]))
