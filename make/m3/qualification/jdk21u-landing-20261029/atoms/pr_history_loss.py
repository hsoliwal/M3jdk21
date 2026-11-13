#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom (pattern PR_HISTORY_LOSS): connect the commit history with the PRs to find lost code and unused work
(user 2026-10-30: "look at the commit history and connect with the PRs ... figure out lost code, also see new
possibilities"). For every PR of <prs.tsv> (number, state MERGED|CLOSED, head oid, base oid, closed date, title):
the PR's change = diff(merge-base(head, base oid), head). Its added lines (blank lines and lines of three
characters or fewer ignored, whitespace-normalised) are checked against the file at <tip> as a multiset:
  SURVIVES (every added line is in the tip file), PARTIAL (some missing), FILE_GONE (the tip lacks the file).
Rows go to LOSS.tsv (pr, state, path, added, missing, verdict) and a summary per PR to PRS.tsv (pr, state, files,
added, missing, missing share, head contained in tip). A CLOSED PR whose lines are missing is a "possibility"
(work never landed); a MERGED PR with missing lines is "loss" (landed, later dropped or overwritten: read it).
Missing blobs are fetched in batches first (never one lazy fetch per blob). PRs touching more than <max> files are
skipped (merges of whole update trains).
Usage: pr_history_loss.py <repo> <tip> <prs.tsv> <out dir> [max files=400]"""
import collections, os, re, subprocess, sys
repo, tip, prs_path, out = sys.argv[1:5]; MAX = int(sys.argv[5]) if len(sys.argv) > 5 else 400
os.makedirs(out, exist_ok=True)
env = dict(os.environ, GIT_NO_LAZY_FETCH="1")


def git(*a, inp=None, check=True):
    r = subprocess.run(["git", "-C", repo, *a], input=inp, capture_output=True, env=env)
    if check and r.returncode not in (0, 1):
        raise RuntimeError(r.stderr.decode("utf-8", "replace")[:300])
    return r.stdout


def missing(oids):
    if not oids:
        return []
    o = git("cat-file", "--batch-check", inp=("\n".join(oids) + "\n").encode()).decode()
    return [l.split()[0] for l in o.splitlines() if l.endswith(" missing")]


def fetch(oids):
    for i in range(0, len(oids), 2000):
        subprocess.run(["git", "-C", repo, "-c", "fetch.negotiationAlgorithm=noop", "fetch", "-q", "origin", "--no-tags",
                        "--no-write-fetch-head", "--recurse-submodules=no", "--filter=blob:none", "--stdin"],
                       input=("\n".join(oids[i:i + 2000]) + "\n").encode(), capture_output=True)


norm = lambda s: re.sub(r"\s+", " ", s).strip()
rows = [l.rstrip("\n").split("\t") for l in open(prs_path, encoding="utf-8") if l.strip()]
plan = []
for num, state, head, base, closed, title in rows:
    fork = git("merge-base", head, base, check=False).decode().strip()
    if not fork:
        continue
    raw = git("diff-tree", "-r", "--no-renames", "--raw", "--no-abbrev", fork, head).decode().splitlines()
    files = [(l.split("\t")[1], l.split()[2], l.split()[3]) for l in raw if "\t" in l]
    files = [(p, a, b) for p, a, b in files if not set(b) <= {"0"} and re.search(r"\.(java|c|cpp|h|hpp|ad|py|sh|md|tsv|yml|xml|properties|gmk|m4)$", p)]
    if 0 < len(files) <= MAX:
        plan.append((num, state, head, fork, closed, title, files))
tipmap = {}
for l in git("ls-tree", "-r", tip).decode("utf-8", "replace").splitlines():
    meta, path = l.split("\t", 1); tipmap[path] = meta.split()[2]
need = sorted({x for _, _, _, _, _, _, fs in plan for p, a, b in fs for x in (a, b, tipmap.get(p, "0" * 40)) if not set(x) <= {"0"}})
miss = missing(need); print(f"{len(plan)} PRs, {len(need)} blobs, {len(miss)} to fetch", flush=True); fetch(miss)
cache = {}


def lines(oid):
    if set(oid) <= {"0"}:
        return []
    if oid not in cache:
        r = subprocess.run(["git", "-C", repo, "cat-file", "blob", oid], capture_output=True, env=env)
        cache[oid] = [norm(x) for x in r.stdout.decode("utf-8", "replace").splitlines()] if r.returncode == 0 else None
    return cache[oid]


loss = open(os.path.join(out, "LOSS.tsv"), "w", encoding="utf-8", newline="\n"); loss.write("pr\tstate\tpath\tadded\tmissing\tverdict\n")
summ = open(os.path.join(out, "PRS.tsv"), "w", encoding="utf-8", newline="\n"); summ.write("pr\tstate\tclosed\tfiles\tadded\tmissing\tmissing_share\tcontained\ttitle\n")
for num, state, head, fork, closed, title, files in plan:
    ta = tm = 0
    contained = subprocess.run(["git", "-C", repo, "merge-base", "--is-ancestor", head, tip], env=env).returncode == 0
    for p, a, b in files:
        old, new = lines(a), lines(b)
        if new is None or old is None:
            continue
        added = collections.Counter(new) - collections.Counter(old)
        added = collections.Counter({k: v for k, v in added.items() if len(k) > 3})
        n = sum(added.values())
        if not n:
            continue
        t = lines(tipmap[p]) if p in tipmap else []
        if t is None:
            continue
        gone = p not in tipmap
        m = sum((added - collections.Counter(t)).values())
        ta += n; tm += m
        if m:
            loss.write(f"{num}\t{state}\t{p}\t{n}\t{m}\t{'FILE_GONE' if gone else 'PARTIAL'}\n")
    if ta:
        summ.write(f"{num}\t{state}\t{closed}\t{len(files)}\t{ta}\t{tm}\t{tm / ta:.2f}\t{'yes' if contained else 'no'}\t{title[:90]}\n")
loss.close(); summ.close(); print("done", flush=True)
