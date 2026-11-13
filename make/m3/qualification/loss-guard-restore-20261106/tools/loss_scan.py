# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""loss_scan.py <repo> <master-ref> <MERGES.txt> <HEADS_MSG.txt> <MASTER_PATHS.txt> <out.tsv>

Local LOSS_GUARD (same tree comparison as api_fold_branch.py --loss-guard, through git plumbing on a blobless clone,
no blob fetch): for every branch head merged into master (every non-first parent of a merge commit, plus every head
named in a landing message) compute base = merge-base(first parent, head) and list the paths the head ADDED or
MODIFIED relative to that base (git diff-tree --no-renames, status A/M/T) that are absent from the master tree.
Rows: path, pr, head, head_date, merge_commit, base, status, head_blob."""
import re
import subprocess
import sys
from collections import defaultdict

repo, master, merges_f, heads_f, mpaths_f, out_f = sys.argv[1:7]
LANDING = re.compile(r"#(\d+)")


def git(*a):
    return subprocess.run(["git", "-C", repo, *a], capture_output=True, text=True, encoding="utf-8",
                          errors="replace").stdout


master_paths = set(open(mpaths_f, encoding="utf-8").read().split("\n"))
heads = {}  # head -> (pr, p1, merge_commit)
for line in open(merges_f, encoding="utf-8"):
    line = line.rstrip("\n")
    if not line:
        continue
    shas, _, subject = line.partition("|")
    parts = shas.split()
    m, p1, rest = parts[0], parts[1], parts[2:]
    prs = ",".join(sorted(set(LANDING.findall(subject)), key=int)) or "-"
    for p in rest:
        heads.setdefault(p, (prs, p1, m))
for line in open(heads_f, encoding="utf-8"):
    pr, sha = line.split()
    full = git("rev-parse", "--verify", sha + "^{commit}").strip()
    if full and full not in heads:
        heads[full] = (pr, None, "-")
print("heads", len(heads), file=sys.stderr)
rows = []
seen_pairs = set()
for i, (head, (pr, p1, m)) in enumerate(sorted(heads.items(), key=lambda kv: kv[1][2])):
    if i % 100 == 0:
        print("heartbeat scan", i, "/", len(heads), file=sys.stderr)
    base = git("merge-base", p1 or master, head).strip()
    if not base or base == head:
        continue
    if (base, head) in seen_pairs:
        continue
    seen_pairs.add((base, head))
    hdate = git("show", "-s", "--format=%cs", head).strip()
    raw = git("diff-tree", "-r", "--no-renames", "--raw", base, head)
    for r in raw.split("\n"):
        if not r:
            continue
        meta, _, path = r.partition("\t")
        f = meta.split()
        status, blob = f[4][0], f[3]
        if status in "AMT" and path not in master_paths:
            rows.append((path, pr, head[:12], hdate, m[:12], base[:12], status, blob))
with open(out_f, "w", encoding="utf-8", newline="\n") as o:
    o.write("path\tpr\thead\thead_date\tmerge_commit\tbase\tstatus\thead_blob\n")
    for r in sorted(rows):
        o.write("\t".join(r) + "\n")
by_path = defaultdict(set)
for r in rows:
    by_path[r[0]].add(r[1])
print("rows", len(rows), "unique paths", len(by_path), file=sys.stderr)
