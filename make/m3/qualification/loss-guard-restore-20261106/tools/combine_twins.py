"""combine_twins.py <git-dir> <ref> <rows.tsv> <out-dir> : RETAINED_3WAY_CHAIN v1. rows: tag<TAB>path<TAB>base-sha
(the CLEAN rows of three_way_restore.py). Rows of one path are applied in order on one result:
ours := <ref>:path; for each row: ours := merge-file(ours, base:path, retained-variants/<tag>/path). A conflict at
any step drops that path (printed CHAIN_CONFLICT); otherwise the result is written to <out-dir>/<path>."""
import os
import subprocess
import sys
import tempfile
from collections import OrderedDict
from pathlib import Path

PREFIX = "m3/qualification/master-landing-20261028/retained-variants/"
gd, ref, rows, out = sys.argv[1], sys.argv[2], sys.argv[3], Path(sys.argv[4])


def show(spec):
    r = subprocess.run(["git", "cat-file", "-p", spec], capture_output=True, env=dict(os.environ, GIT_DIR=gd))
    return r.stdout if r.returncode == 0 else b""


groups = OrderedDict()
for line in open(rows, encoding="utf-8").read().splitlines():
    tag, path, base = line.split("\t")[:3]
    groups.setdefault(path, []).append((tag, base))
for path, steps in groups.items():
    ours = show(f"{ref}:{path}")
    ok = True
    for tag, base in steps:
        with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as d:
            fo, fb, ft = (Path(d) / n for n in "obt")
            fo.write_bytes(ours); fb.write_bytes(show(f"{base}:{path}")); ft.write_bytes(show(f"{ref}:{PREFIX}{tag}/{path}"))
            r = subprocess.run(["git", "merge-file", "-p", str(fo), str(fb), str(ft)], capture_output=True)
        if r.returncode != 0:
            print(f"CHAIN_CONFLICT\t{path}\tat {tag}")
            ok = False
            break
        ours = r.stdout
    if ok:
        dst = out / path
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(ours)
        print(f"CHAINED\t{path}\t{'+'.join(t for t, _ in steps)}")
