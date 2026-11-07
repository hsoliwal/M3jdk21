"""mergeloss_batch.py <git-dir> <master> <LEAVES.tsv> <leaf-ids,> <out-dir> : MERGE_LOSS_BATCH v1.
For each merge-loss leaf (LEAVES.tsv columns n, origin_commit "sha date subject", files ';'-separated): fetch the origin
commit if missing, then per file 3-way ours = master, base = origin^, theirs = origin (git merge-file --diff3).
Writes <out-dir>/<leaf>/<path> and prints per leaf: the file verdicts (CLEAN, CLEAN=ours meaning master already holds
the change, CLEAN=theirs, CONFLICT=n, ABSENT_OURS/THEIRS) and a leaf verdict: ALREADY (all =ours), CLEAN (no conflict),
CONFLICT (any), NO_ORIGIN (leaf has no origin commit)."""
import os
import subprocess
import sys
import tempfile
from pathlib import Path

gd, master, leaves, ids, out = sys.argv[1:6]
want = set(ids.split(","))
env = dict(os.environ, GIT_DIR=gd)


def git(*a, binary=False):
    r = subprocess.run(["git", *a], capture_output=True, env=env)
    return (r.stdout if binary else r.stdout.decode().strip()) if r.returncode == 0 else None


def resolve(short):
    full = git("rev-parse", "--verify", "-q", short + "^{commit}")
    if full:
        return full
    r = subprocess.run(["gh", "api", f"repos/hsoliwal/M3jdk21/commits/{short}", "--jq", ".sha"], capture_output=True, text=True)
    sha = r.stdout.strip()
    if sha:
        subprocess.run(["git", "fetch", "-q", "--filter=blob:none", "origin", sha], env=env, capture_output=True)
        return git("rev-parse", "--verify", "-q", sha + "^{commit}")
    return None


rows = [l.split("\t") for l in Path(leaves).read_text(encoding="utf-8").splitlines()[1:]]
for r in rows:
    if r[0] not in want:
        continue
    origin = r[6].split(" ")[0] if r[6] else ""
    if not origin:
        print(f"LEAF {r[0]}\tNO_ORIGIN\tmerge={r[3]}")
        continue
    c = resolve(origin)
    if not c:
        print(f"LEAF {r[0]}\tNO_ORIGIN\t{origin} unresolved")
        continue
    verdicts = []
    for path in [p for p in r[7].split(";") if p]:
        o, b, t = git("cat-file", "blob", f"{master}:{path}", binary=True), git("cat-file", "blob", f"{c}^:{path}", binary=True), git("cat-file", "blob", f"{c}:{path}", binary=True)
        if o is None or t is None:
            verdicts.append(("ABSENT_OURS" if o is None else "ABSENT_THEIRS", path))
            if o is None and t is not None:
                dst = Path(out) / r[0] / path
                dst.parent.mkdir(parents=True, exist_ok=True)
                dst.write_bytes(t)
            continue
        with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as d:
            fo, fb, ft = (Path(d) / n for n in "obt")
            fo.write_bytes(o); fb.write_bytes(b or b""); ft.write_bytes(t)
            m = subprocess.run(["git", "merge-file", "-p", "--diff3", str(fo), str(fb), str(ft)], capture_output=True)
        dst = Path(out) / r[0] / path
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(m.stdout)
        k = "CLEAN" if m.returncode == 0 else f"CONFLICT={m.returncode}"
        if m.returncode == 0:
            k += "=ours" if m.stdout == o else "=theirs" if m.stdout == t else ""
        verdicts.append((k, path))
    kinds = {k for k, _ in verdicts}
    leaf = ("ALREADY" if kinds == {"CLEAN=ours"} else "CONFLICT" if any(k.startswith("CONFLICT") for k in kinds)
            else "CLEAN")
    print(f"LEAF {r[0]}\t{leaf}\t{origin}\t{r[6][:70]}")
    for k, p in verdicts:
        print(f"   {k}\t{p}")
