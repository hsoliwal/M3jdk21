"""named_apply.py <m3jdk-git-dir> <master-sha> <rename-parent-sha> <naming-dir> <crate-target-path> <final-file> <out-dir>
HASH_PINNED_NAMED_APPLY v1 (one target, after crate_chain2.py chose its chain): start = <rename-parent>:<target>,
master path = jdk_naming.target_path(target). If master == named(start) writes named(final) (APPLY); otherwise
runs git merge-file master named(start) named(final) and writes the result (MERGED_CLEAN or CONFLICT=<n>, conflict
markers left in place for review). Output goes to <out-dir>/<master path>."""
import os
import subprocess
import sys
import tempfile
from pathlib import Path

gd, master, rparent, naming_dir, target, final_file, out = sys.argv[1:8]
sys.path.insert(0, naming_dir)
import jdk_naming  # noqa: E402


def blob(rev, path):
    r = subprocess.run(["git", "cat-file", "blob", f"{rev}:{path}"], capture_output=True, env=dict(os.environ, GIT_DIR=gd))
    return r.stdout if r.returncode == 0 else None


def named(data):
    return jdk_naming.encoded(jdk_naming.rewrite_text(jdk_naming.decoded(data))[0])


mpath = jdk_naming.target_path(target)
start, live = blob(rparent, target), blob(master, mpath)
fin = named(Path(final_file).read_bytes())
dst = Path(out) / mpath
dst.parent.mkdir(parents=True, exist_ok=True)
if live == named(start):
    dst.write_bytes(fin)
    print(f"APPLY\t{mpath}")
else:
    with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as d:
        o, b, t = (Path(d) / n for n in "obt")
        o.write_bytes(live); b.write_bytes(named(start)); t.write_bytes(fin)
        r = subprocess.run(["git", "merge-file", "-p", "--diff3", str(o), str(b), str(t)], capture_output=True)
    dst.write_bytes(r.stdout)
    print(f"{'MERGED_CLEAN' if r.returncode == 0 else 'CONFLICT=' + str(r.returncode)}\t{mpath}")
