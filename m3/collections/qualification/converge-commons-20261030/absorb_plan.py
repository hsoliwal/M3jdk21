#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom (pattern ATOM_LEVEL_ABSORPTION_PLAN): converge a collection fork into M3JDK by atoms, not files. Inputs are
three MOP atomize outputs (ATOMS.tsv): the fork head (donor), M3JDK's relocated copy, and the first-party M3 owners.
Each donor atom (methods, constructors, types; kinds as the atomizer emits them) gets one state:
  IN_COPY        the copy has the identical atom (exactSha256): nothing to converge
  COPY_STALE     the copy has the same owner+name but other bytes: the fork changed this behaviour since the copy
  NEW_IN_FORK    the copy lacks the owner+name: behaviour the fork added
and one absorption verdict against the M3 owners:
  OWNER_CONTRACT the M3 owners already hold an atom with the same contractSha256 (absorbed by contract)
  OWNER_SHAPE    same shapeKey only (reuse the owner's pattern; prove by a differential test)
  GAP            no owner atom of that contract or shape: mint a pattern, then an M3 owner atom
Writes ABSORPTION_PLAN.tsv and SUMMARY.tsv. Usage: absorb_plan.py <fork ATOMS.tsv> <copy ATOMS.tsv> <owners ATOMS.tsv> <out dir>"""
import collections, csv, os, sys
fork_p, copy_p, own_p, out = sys.argv[1:5]


def load(p):
    with open(p, encoding="utf-8") as f:
        return list(csv.DictReader(f, delimiter="\t"))


fork, copy, own = load(fork_p), load(copy_p), load(own_p)
copy_exact = {a["exactSha256"] for a in copy}
copy_name = {(a["owner"].split(".")[-1], a["kind"], a["name"]) for a in copy}
own_contract = {a["contractSha256"] for a in own}
own_shape = {a["shapeKey"] for a in own}
summary = collections.Counter()
with open(os.path.join(out, "ABSORPTION_PLAN.tsv"), "w", encoding="utf-8", newline="\n") as w:
    w.write("path\towner\tkind\tname\tconvergence\tabsorption\texactSha256\tshapeKey\tcontractSha256\n")
    for a in fork:
        key = (a["owner"].split(".")[-1], a["kind"], a["name"])
        conv = "IN_COPY" if a["exactSha256"] in copy_exact else "COPY_STALE" if key in copy_name else "NEW_IN_FORK"
        absorb = "OWNER_CONTRACT" if a["contractSha256"] in own_contract else "OWNER_SHAPE" if a["shapeKey"] in own_shape else "GAP"
        summary[(conv, absorb)] += 1
        w.write("\t".join([a["path"], a["owner"], a["kind"], a["name"], conv, absorb, a["exactSha256"], a["shapeKey"], a["contractSha256"]]) + "\n")
with open(os.path.join(out, "SUMMARY.tsv"), "w", encoding="utf-8", newline="\n") as w:
    w.write("convergence\tabsorption\tatoms\n")
    for (c, b), n in sorted(summary.items()):
        w.write(f"{c}\t{b}\t{n}\n")
for (c, b), n in sorted(summary.items()):
    print(f"{c:12s} {b:15s} {n}")
print("total", sum(summary.values()))
