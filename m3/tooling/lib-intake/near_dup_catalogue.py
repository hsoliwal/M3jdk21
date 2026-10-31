#!/usr/bin/env python3
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Pattern NEAR_DUP_PATTERN_FAMILIES v1: cross-library patternization of ALGORITHMS, which exact shapes miss.

Exact structural keys (M3Mop shapeKey/patternId) recur across libraries almost only for accessors and constructors.
Algorithms recur as near-duplicates: same operator multiset, control depth and operator frequency with different names,
literals and generics. This driver reuses the ts-lhash skill unchanged (tree-sitter Java leaves; L-Hash v1: H1 exact,
H2+H3+H4 near-duplicate signature) and adds identity: library, file, line and method name per leaf.
A near-duplicate class with leaves from 2+ libraries, or from a library and the M3 owners, is a pattern family:
CROSS_LIB_ALGORITHM (mint one M3 pattern) or M3_OWNED_ALGORITHM (an M3 owner already implements the family).
Only leaves of at least --min-tokens normalized tokens count (algorithm-sized). Candidates only: a differential test
admits any consolidation.
Usage: near_dup_catalogue.py --lhash-dir <ts-lhash skill dir> --owners <M3 owner source root> --out <dir>
       <lib>=<relocated source root> ..."""
import argparse, collections, os, sys


def leaves(ts, root, label, min_tokens):
    rows = []
    for dp, _, files in os.walk(root):
        for f in sorted(files):
            if not f.endswith(".java") or f == "module-info.java":
                continue
            path = os.path.join(dp, f)
            src = open(path, "rb").read()
            try:
                tree = ts.get_parser("java").parse(src)
            except Exception:
                continue
            for leaf in ts._iter_leaves(tree.root_node, ts.FUNC_LEAF["java"]):
                ids = ts.leaf_ids(leaf)
                if len(ids) < min_tokens:
                    continue
                h = ts.LH.hash_ids(ids)
                name_node = leaf.child_by_field_name("name")
                name = name_node.text.decode("utf-8", "replace") if name_node is not None else "?"
                rel = os.path.relpath(path, root).replace("\\", "/")
                rows.append((label, rel, leaf.start_point[0] + 1, name, h["h1"], (h["h2"], h["h3"], h["h4"]), len(ids)))
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lhash-dir", required=True)
    ap.add_argument("--owners", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--min-tokens", type=int, default=40)
    ap.add_argument("libs", nargs="+")
    a = ap.parse_args()
    sys.path.insert(0, a.lhash_dir)
    import ts_lhash as ts  # the skill module, unchanged
    all_rows = leaves(ts, a.owners, "M3_OWNERS", a.min_tokens)
    for spec in a.libs:
        lib, root = spec.split("=", 1)
        all_rows += leaves(ts, root, lib, a.min_tokens)
    classes = collections.defaultdict(list)
    for r in all_rows:
        classes[r[5]].append(r)
    os.makedirs(a.out, exist_ok=True)
    out, summary = [], collections.Counter()
    for sig, members in classes.items():
        libs = sorted({m[0] for m in members if m[0] != "M3_OWNERS"})
        owned = any(m[0] == "M3_OWNERS" for m in members)
        if len(libs) < 2 and not (owned and libs):
            continue
        state = "M3_OWNED_ALGORITHM" if owned else "CROSS_LIB_ALGORITHM"
        exact = len({m[4] for m in members}) == 1
        sample = ";".join(f"{m[0]}:{m[1].rsplit('/', 1)[-1]}:{m[2]}:{m[3]}" for m in sorted(members)[:8])
        out.append((len(libs), len(members), state, "EXACT" if exact else "NEAR", max(m[6] for m in members), ",".join(libs), sample))
        summary[state] += 1
        summary[state + "/leaves"] += len(members)
    out.sort(key=lambda r: (-r[0], -r[1], -r[4]))
    with open(os.path.join(a.out, "NEAR_DUP_FAMILIES.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("libraries\tleaves\tstate\tmatch\tmax_tokens\tlibs\tsample(lib:file:line:method)\n")
        for r in out:
            w.write("\t".join(str(x) for x in r) + "\n")
    summary["leaves_total"] = len(all_rows)
    summary["leaves_libraries"] = sum(1 for r in all_rows if r[0] != "M3_OWNERS")
    with open(os.path.join(a.out, "NEAR_DUP_SUMMARY.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("metric\tvalue\n")
        for k in sorted(summary):
            w.write(f"{k}\t{summary[k]}\n")
            print(f"{k}\t{summary[k]}")


if __name__ == "__main__":
    main()
