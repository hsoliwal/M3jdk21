#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Second pass of pr_history_loss.py (pattern PR_HISTORY_LOSS, classification): every LOSS.tsv row (pr, path with
missing added lines) becomes
  RETAINED  the tip keeps the PR's file under a retained-variants/pr<N>/ directory (preserved, not live),
  MOVED     at least 80% of the missing lines are in one tip file with the same basename (renamed or moved),
  LOST      otherwise (the recovery candidates),
and a closed PR's rows are POSSIBILITY instead of LOST (work that never landed). Writes CLASSIFIED.tsv and
RECOVERY_QUEUE.tsv (LOST and POSSIBILITY rows ranked by missing lines, with the PR head blob to restore from).
Missing blobs are fetched in one batch.
Usage: loss_classify.py <repo> <tip> <out dir of pr_history_loss.py> <prs.tsv>"""
import collections, os, re, subprocess, sys
repo, tip, out, prs_path = sys.argv[1:5]
env = dict(os.environ, GIT_NO_LAZY_FETCH="1")
run = lambda *a, inp=None: subprocess.run(["git", "-C", repo, *a], input=inp, capture_output=True, env=env).stdout
norm = lambda s: re.sub(r"\s+", " ", s).strip()
heads = {l.split("\t")[0]: l.split("\t")[2] for l in open(prs_path, encoding="utf-8") if l.strip()}
tipmap, byname, retained = {}, collections.defaultdict(list), collections.defaultdict(set)
for l in run("ls-tree", "-r", tip).decode("utf-8", "replace").splitlines():
    meta, path = l.split("\t", 1); oid = meta.split()[2]; tipmap[path] = oid
    byname[path.rsplit("/", 1)[-1]].append(path)
    m = re.search(r"retained-variants/pr(\d+)/(.+)$", path)
    if m:
        retained[m.group(1)].add(m.group(2))
rows = [l.rstrip("\n").split("\t") for l in open(os.path.join(out, "LOSS.tsv"), encoding="utf-8")][1:]


def pr_blob(pr, path):
    o = run("ls-tree", heads.get(pr, ""), "--", path).decode().split()
    return o[2] if len(o) >= 3 else ""


need = set()
plan = []
for pr, state, path, added, miss, verdict in rows:
    keep = any(path.endswith(r) or r.endswith(path.split("/", 1)[-1]) for r in retained.get(pr, ()))
    cands = [p for p in byname.get(path.rsplit("/", 1)[-1], []) if p != path and "retained-variants" not in p][:6]
    b = pr_blob(pr, path)
    need.update([b] + [tipmap[c] for c in cands] + ([tipmap[path]] if path in tipmap else []))
    plan.append((pr, state, path, int(added), int(miss), verdict, keep, cands, b))
need.discard("")
miss_oids = [l.split()[0] for l in run("cat-file", "--batch-check", inp=("\n".join(sorted(need)) + "\n").encode()).decode().splitlines() if l.endswith(" missing")]
for i in range(0, len(miss_oids), 2000):
    subprocess.run(["git", "-C", repo, "-c", "fetch.negotiationAlgorithm=noop", "fetch", "-q", "origin", "--no-tags", "--no-write-fetch-head",
                    "--filter=blob:none", "--stdin"], input=("\n".join(miss_oids[i:i + 2000]) + "\n").encode(), capture_output=True)
cache = {}


def lines(oid):
    if not oid:
        return collections.Counter()
    if oid not in cache:
        r = subprocess.run(["git", "-C", repo, "cat-file", "blob", oid], capture_output=True, env=env)
        cache[oid] = collections.Counter(n for n in (norm(x) for x in r.stdout.decode("utf-8", "replace").splitlines()) if len(n) > 3)
    return cache[oid]


cls = open(os.path.join(out, "CLASSIFIED.tsv"), "w", encoding="utf-8", newline="\n")
cls.write("pr\tstate\tpath\tadded\tmissing\tclass\tmoved_to\tpr_blob\n")
queue = []
for pr, state, path, added, miss, verdict, keep, cands, b in plan:
    if keep:
        c, to = "RETAINED", "-"
    else:
        prl = lines(b); tipl = lines(tipmap.get(path, ""))
        lost = prl - tipl
        best, bestc = 0.0, "-"
        for cpath in cands:
            hit = sum((lost & lines(tipmap[cpath])).values()) / max(1, sum(lost.values()))
            if hit > best:
                best, bestc = hit, cpath
        c, to = ("MOVED", bestc) if best >= 0.8 else ("POSSIBILITY" if state == "CLOSED" else "LOST", bestc if best > 0 else "-")
    cls.write(f"{pr}\t{state}\t{path}\t{added}\t{miss}\t{c}\t{to}\t{b}\n")
    if c in ("LOST", "POSSIBILITY"):
        queue.append((miss, pr, state, path, c, b))
cls.close()
with open(os.path.join(out, "RECOVERY_QUEUE.tsv"), "w", encoding="utf-8", newline="\n") as q:
    q.write("missing\tpr\tstate\tpath\tclass\tpr_blob\n")
    for r in sorted(queue, reverse=True):
        q.write("\t".join(map(str, r)) + "\n")
tally = collections.Counter(); lines_by = collections.Counter()
for pr, state, path, added, miss, verdict, keep, cands, b in plan:
    pass
for l in open(os.path.join(out, "CLASSIFIED.tsv"), encoding="utf-8").read().splitlines()[1:]:
    f = l.split("\t"); tally[f[5]] += 1; lines_by[f[5]] += int(f[4])
print({k: (tally[k], lines_by[k]) for k in tally})
