#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Classify a master-vs-head pair of DoopFacadeFixesDifferential traces, episode by episode (stdlib only).

SAME: the episode's lines are byte-identical. FIX_A: the first differing line is an operation tagged dup=1 (its
factory or rebind callback admitted the same id, the leaf-A path), the head ledger has leaked=0 and double=0 and the
master ledger has leaked>0; later differing lines of that episode are CASCADE (disposal counts and stats of the same
owner). Anything else is UNCLASSIFIED. Also checked: every episode without a dup=1 operation is SAME; the head
traces have leaked=0 and double=0 in every episode.
usage: classify_diff.py MASTER_TRACE HEAD_TRACE OUT_TSV   (exit 1 when an episode is UNCLASSIFIED)"""
import re
import sys

LEDGER = re.compile(r" double=(\d+) leaked=(\d+) dupOps=(\d+)$")


def episodes(path):
    blocks, order = {}, []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            key = line.split(" ", 1)[0]
            if key not in blocks:
                blocks[key] = []
                order.append(key)
            blocks[key].append(line.rstrip("\n"))
    return order, blocks


def ledger(block):
    found = LEDGER.search(block[-1])
    if not block[-1].split(" ", 2)[1] == "END" or not found:
        raise SystemExit("episode without END ledger: " + block[-1][:120])
    return tuple(int(x) for x in found.groups())


def main(master_path, head_path, out_path):
    order, master = episodes(master_path)
    head_order, head = episodes(head_path)
    if order != head_order:
        raise SystemExit("episode lists differ")
    counts = {"SAME": 0, "FIX_A": 0, "UNCLASSIFIED": 0}
    cascade, rows, head_bad, nodup_diff = 0, [], 0, 0
    for key in order:
        m, h = master[key], head[key]
        hd, hl, hdup = ledger(h)
        md, ml, mdup = ledger(m)
        head_bad += 1 if hd or hl else 0
        if m == h:
            counts["SAME"] += 1
            continue
        if hdup == 0:
            nodup_diff += 1
        first = next(i for i, (a, b) in enumerate(zip(m, h)) if a != b) if len(m) == len(h) else -1
        if first >= 0 and " dup=1 " in h[first] and hl == 0 and hd == 0 and ml > 0:
            kind = "FIX_A"
            cascade += sum(1 for a, b in zip(m[first + 1:], h[first + 1:]) if a != b)
        else:
            kind = "UNCLASSIFIED"
        counts[kind] += 1
        rows.append((key, kind, str(first), str(mdup), "master_leaked=%d head_leaked=%d" % (ml, hl),
                     (h[first] if first >= 0 else "length differs")[:200]))
    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        out.write("episode\tclass\tfirst_diff_line\tdup_ops\tledger\thead_first_diff\n")
        for row in rows:
            out.write("\t".join(row) + "\n")
    print("CLASSIFY episodes=%d same=%d fix_a=%d unclassified=%d cascade_lines=%d head_leak_or_double=%d "
          "nodup_diff=%d" % (len(order), counts["SAME"], counts["FIX_A"], counts["UNCLASSIFIED"], cascade, head_bad,
                             nodup_diff))
    return 1 if counts["UNCLASSIFIED"] or head_bad or nodup_diff or not order else 0


if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:4]))
