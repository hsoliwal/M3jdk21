#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom BLOB_NEED: the blob ids a standalone apply check of each candidate commit needs: upstream pre/post blobs of every
touched path (from `git log --raw`) and the M3jdk21 master blob at the same path (from `git ls-tree -r`).
Writes <out>/commits.<areas>.txt (oldest first), <out>/upstream_oids.txt and <out>/master_oids.txt.
Usage: need_blobs.py <data dir> <BACKLOG.tsv> <out dir> <area,area,...>"""
import csv, os, sys
csv.field_size_limit(1 << 24)
D, BL, OUT, AREAS = sys.argv[1], sys.argv[2], sys.argv[3], set(sys.argv[4].split(","))
os.makedirs(OUT, exist_ok=True)
Z = "0" * 40
want = set()
for r in csv.DictReader(open(BL, encoding="utf-8", newline=""), delimiter="\t"):
    if r["state"] == "CANDIDATE" and r["area"] in AREAS: want.update(r["upstream_commit"].split(","))
date = {}
for line in open(os.path.join(D, "upstream_commits_raw.txt"), encoding="utf-8"):
    sha, d, _ = line[2:].split("\t", 2); date[sha] = d
master = {}
for line in open(os.path.join(D, "master_ls_tree.txt"), encoding="utf-8"):
    meta, path = line.rstrip("\n").split("\t", 1); mode, typ, oid = meta.split()
    if typ == "blob": master[path] = oid
up, mo, cur = set(), set(), None
for line in open(os.path.join(D, "upstream_commit_raw.txt"), encoding="utf-8"):
    if line.startswith("@@"): cur = line[2:42]; continue
    if cur not in want or not line.startswith(":"): continue
    meta, path = line.rstrip("\n").split("\t", 1); f = meta.split()
    for oid, mode in ((f[2], f[0][1:]), (f[3], f[1])):
        if oid != Z and mode != "160000": up.add(oid)
    if path in master: mo.add(master[path])
tag = "_".join(sorted(AREAS))
open(os.path.join(OUT, f"commits.{tag}.txt"), "w", newline="\n").write("".join(c + "\n" for c in sorted(want, key=lambda c: date[c])))
open(os.path.join(OUT, f"upstream_oids.{tag}.txt"), "w", newline="\n").write("".join(o + "\n" for o in sorted(up)))
open(os.path.join(OUT, f"master_oids.{tag}.txt"), "w", newline="\n").write("".join(o + "\n" for o in sorted(mo - up)))
print("commits", len(want), "upstream_oids", len(up), "master_oids", len(mo - up))
