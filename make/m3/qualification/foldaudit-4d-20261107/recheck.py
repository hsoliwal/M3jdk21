"""recheck.py <git-dir> <master> <LEAVES.tsv> <lane> <priority> : FOLD_AUDIT_RECHECK v1.
For each fold-audit leaf of a lane and priority: the origin file (retained copy when listed, else the owner file at the
first origin head) is compared with the owner file on the given master. A lost line is a non-blank, non-comment line
of the origin (whitespace-normalised) absent from master. Prints leaf, owner, origin lines lost now vs the audit's
lost_lines, and the first lost line, so leaves already fixed on master drop out before any work starts."""
import os
import subprocess
import sys

gd, master, table, lane, priority = sys.argv[1:6]
env = dict(os.environ, GIT_DIR=gd)


def show(rev, path):
    r = subprocess.run(["git", "show", f"{rev}:{path}"], capture_output=True, env=env)
    return r.stdout.decode("utf-8", "replace") if r.returncode == 0 else None


def norm(text):
    out = []
    for line in text.splitlines():
        s = " ".join(line.split())
        if s and not s.startswith(("//", "*", "/*")):
            out.append(s)
    return out


rows = [l.rstrip("\n").split("\t") for l in open(table, encoding="utf-8")]
col = {n: i for i, n in enumerate(rows[0])}
for r in rows[1:]:
    if lane not in r[col["lane"]] or r[col["priority"]] != priority:
        continue
    owner = r[col["owner_file"]]
    retained = r[col["retained_copy"]]
    heads = [h for h in r[col["origin_heads"]].replace(";", ",").split(",") if h and h != "-"]
    origin = show(master, retained) if retained and retained != "-" else None
    if origin is None and heads:
        origin = show(heads[0], owner)
    now = show(master, owner)
    if origin is None:
        print(f"{r[0]}\tNO_ORIGIN\t{owner}")
        continue
    if now is None:
        print(f"{r[0]}\tOWNER_ABSENT\t{owner}\taudit_lost={r[col['lost_lines']]}")
        continue
    have = set(norm(now))
    lost = [s for s in norm(origin) if s not in have]
    print(f"{r[0]}\tlost_now={len(lost)}\taudit_lost={r[col['lost_lines']]}\t{owner}\t{(lost[0][:90] if lost else '-')}")
