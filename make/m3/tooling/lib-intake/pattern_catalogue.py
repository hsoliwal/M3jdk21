#!/usr/bin/env python3
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Pattern CROSS_LIBRARY_PATTERN_CATALOGUE v1: atomization and patternization across every intake library at once.

Input: the M3Mop ATOMS.tsv of each relocated library (<work root>/<lib>/atoms/ATOMS.tsv) and of the M3 owners.
Atoms are grouped by three CPU keys, each a different claim:
  shapeKey       structural shape (names and literals normalized): a PATTERN when it recurs in 2+ libraries;
                 M3_KNOWN when an M3 owner already has the shape (reuse that owner's pattern)
  shapeKey+contractSha256 same shape AND contract: a CONSOLIDATION candidate across libraries or onto
                 an M3 owner; equal contract is a candidate, never proof (a differential test admits it)
  exactSha256    byte-identical atom: a DUPLICATE copied between libraries; one owner should hold it
Outputs in <out dir>: SHAPE_FAMILIES.tsv, CONSOLIDATION_CANDIDATES.tsv, EXACT_DUPLICATES.tsv, SUMMARY.tsv. METHOD atoms only, since
types and fields are not behaviour. Rows are sorted by reach (libraries, then atoms); nothing is rewritten.
Usage: pattern_catalogue.py <work root> <owner ATOMS.tsv> <out dir> [lib ...]"""
import collections, csv, os, sys


def atoms(path):
    with open(path, encoding="utf-8") as f:
        rows = [a for a in csv.DictReader(f, delimiter="\t") if a["kind"] == "METHOD"]
    # M3Mop's own pattern family per atom (PATTERNS.tsv beside ATOMS.tsv): the coarser grouping algorithms share
    patterns = os.path.join(os.path.dirname(path), "PATTERNS.tsv")
    ids = {}
    if os.path.isfile(patterns):
        with open(patterns, encoding="utf-8") as f:
            ids = {p["atomId"]: p["patternId"] + "|" + p["state"] for p in csv.DictReader(f, delimiter="\t")}
    for a in rows:
        a["patternId"] = ids.get(a["atomId"], "-|-")
    return rows


ROLE_ORDER = {"ALGORITHM": 0, "ITERATOR": 1, "CONSTRUCTOR": 2, "OBJECT_PROTOCOL": 3, "ACCESSOR": 4}


def role(a):
    """Coarse behaviour role from the atom's name and size: algorithm families are the ones worth minting as patterns."""
    name, lines = a["name"], int(a["endLine"]) - int(a["startLine"]) + 1
    if name == "<init>":
        return "CONSTRUCTOR"
    if name in ("equals", "hashCode", "toString", "compareTo", "clone"):
        return "OBJECT_PROTOCOL"
    if name in ("hasNext", "next", "remove", "iterator", "forEachRemaining", "spliterator"):
        return "ITERATOR"
    # atom spans include the Javadoc, so an accessor is a get/set/is/has name over a short span
    if lines <= 14 and name.startswith(("get", "set", "is", "has")) or lines <= 4:
        return "ACCESSOR"
    return "ALGORITHM"


def main():
    root, owners_path, out = sys.argv[1:4]
    libs = sys.argv[4:] or sorted(d for d in os.listdir(root) if os.path.isfile(os.path.join(root, d, "atoms", "ATOMS.tsv")))
    by_lib = {lib: atoms(os.path.join(root, lib, "atoms", "ATOMS.tsv")) for lib in libs}
    owners = atoms(owners_path)
    for a in owners:
        a["shape_contract"] = a["shapeKey"] + ":" + a["contractSha256"]
    for rows in by_lib.values():
        for a in rows:
            a["shape_contract"] = a["shapeKey"] + ":" + a["contractSha256"]
    owner_index = {k: collections.defaultdict(list) for k in ("shapeKey", "shape_contract", "patternId")}
    for a in owners:
        for key in owner_index:
            owner_index[key][a[key]].append(a["owner"].rsplit(".", 1)[-1] + "." + a["name"])
    os.makedirs(out, exist_ok=True)
    summary = collections.Counter(libraries=len(libs), method_atoms=sum(len(v) for v in by_lib.values()))
    # shape = pattern family; shape AND contract = consolidation candidate; exact bytes = copied duplicate
    for key, name in (("patternId", "PATTERN_FAMILIES"), ("shapeKey", "SHAPE_FAMILIES"), ("shape_contract", "CONSOLIDATION_CANDIDATES"), ("exactSha256", "EXACT_DUPLICATES")):
        groups = collections.defaultdict(list)
        for lib, rows in by_lib.items():
            for a in rows:
                groups[a[key]].append((lib, a))
        rows = []
        for k, members in groups.items():
            members_libs = sorted({lib for lib, _ in members})
            m3_members = owner_index.get(key, {}).get(k, [])
            if len(members_libs) < 2 and not m3_members:
                continue
            state = state_of(key, len(members_libs), bool(m3_members))
            roles = collections.Counter(role(a) for _, a in members)
            main_role = max(roles, key=lambda r: (roles[r], -ROLE_ORDER[r]))  # majority role; ties favour ALGORITHM
            sample = sorted({lib + ":" + a["owner"].rsplit(".", 1)[-1] + "." + a["name"] for lib, a in members})[:6]
            rows.append((main_role, len(members_libs), len(members), k, state, ",".join(members_libs), ";".join(sample),
                         ";".join(sorted(set(m3_members))[:3])))
            summary[name + "/" + state + "/" + main_role] += 1
            summary[name + "/" + state + "/" + main_role + "/atoms"] += len(members)
        rows.sort(key=lambda r: (ROLE_ORDER[r[0]], -r[1], -r[2], r[3]))
        with open(os.path.join(out, name + ".tsv"), "w", encoding="utf-8", newline="\n") as w:
            w.write("role\tlibraries\tatoms\tkey\tstate\tlibs\tsample\tm3_owner_sample\n")
            for r in rows:
                w.write("\t".join(str(x) for x in r) + "\n")
    with open(os.path.join(out, "SUMMARY.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("metric\tvalue\n")
        for k in sorted(summary):
            w.write(f"{k}\t{summary[k]}\n")
    for k in sorted(summary):
        print(f"{k}\t{summary[k]}")


def state_of(key, nlibs, m3):
    if key == "patternId":
        return "M3_PATTERN" if m3 else "CROSS_LIB_FAMILY"
    if key == "shapeKey":
        return "M3_KNOWN" if m3 else "CROSS_LIB_PATTERN"
    if key == "shape_contract":
        return "CONSOLIDATE_ONTO_M3" if m3 else "CONSOLIDATE_ACROSS_LIBS"
    return "DUPLICATE" if nlibs >= 2 else "SINGLE"


if __name__ == "__main__":
    main()
