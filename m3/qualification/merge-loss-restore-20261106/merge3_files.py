"""merge3_files.py <git-dir> <ours-rev> <base-rev> <theirs-rev> <paths.lst> <out-dir> : MERGE_LOSS_RESTORE v1.
Re-runs a lost merge per file: git merge-file ours=<ours-rev>:path base=<base-rev>:path theirs=<theirs-rev>:path
(--diff3). Typical use for a retain merge that silently kept one side: ours = current master, base = merge-base of
the retain merge's parents, theirs = the dropped parent. Writes <out-dir>/<path> and prints one verdict per path:
CLEAN (merged), CLEAN=ours (master already holds theirs' change), CLEAN=theirs, CONFLICT=<n>, ABSENT_<side>."""
import os
import subprocess
import sys
import tempfile
from pathlib import Path

gd, ours, base, theirs, lst, out = sys.argv[1:7]
env = dict(os.environ, GIT_DIR=gd)


def show(rev, path):
    r = subprocess.run(["git", "cat-file", "blob", f"{rev}:{path}"], capture_output=True, env=env)
    return r.stdout if r.returncode == 0 else None


for path in Path(lst).read_text(encoding="utf-8").split():
    o, b, t = show(ours, path), show(base, path), show(theirs, path)
    if o is None or t is None:
        print(f"ABSENT_{'OURS' if o is None else 'THEIRS'}\t{path}")
        continue
    with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as d:
        fo, fb, ft = (Path(d) / n for n in "obt")
        fo.write_bytes(o); fb.write_bytes(b or b""); ft.write_bytes(t)
        r = subprocess.run(["git", "merge-file", "-p", "--diff3", str(fo), str(fb), str(ft)], capture_output=True)
    dst = Path(out) / path
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(r.stdout)
    kind = "CLEAN" if r.returncode == 0 else f"CONFLICT={r.returncode}"
    if r.returncode == 0:
        kind += "=ours" if r.stdout == o else "=theirs" if r.stdout == t else ""
    print(f"{kind}\t{path}")
