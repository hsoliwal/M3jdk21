# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""ledger_remap.py <ledger in> <ledger out> <pre-split renamed M3XXH3.java> <pre-split CONSTANTS.tsv>
                   <split renamed M3XXH3.java> <split CONSTANTS.tsv>

XXH3_SPLIT v1 moves statements out of XXH3_64bits_internal and XXH3_128bits_internal into new private methods; the
LITERAL_CONSTANTS ledger names literals per member occurrence ('member#k'), so a moved literal would fall back to the
class-wide name of its value (a 16 that reads secret offset 16 would be named MIX_BYTES). This keeps every moved
literal's name and role: each literal occurrence of the split M3XXH3 is matched to its pre-split occurrence by the
statement it stands in (the source line without indentation and the literal's column in it; the merge lines that
XXH3_SPLIT rewrote are read as the pre-split fold lines they replace), then the rows of the split members are
rewritten: their pre-split rows are removed and one row per occurrence is written (the parents' remaining dispatch
code and the new atoms). Every other ledger row is copied unchanged. Stops when an occurrence has no pre-split match
or when two pre-split occurrences of one statement disagree. The CONSTANTS.tsv positions are those of the renamed
sources (before LITERAL_CONSTANTS), which is what both pairs of inputs are.
"""
import csv
import sys

NL = "\n"
TAB = "\t"
CLASS = "M3XXH3"
FILE = "M3XXH3.java"
PARENTS = ("XXH3_64bits_internal", "XXH3_128bits_internal")
NEW_MEMBERS = ("XXH3_hashLong", "XXH3_mergeAccs")
SECTION = ("# XXH3_SPLIT v1 members (tools/ledger_remap.py): every literal moved out of XXH3_64bits_internal and",
           "# XXH3_128bits_internal keeps its pre-split name and role, matched by statement")
# (marker of a statement XXH3_SPLIT rewrote, anchor in it, pre-split statement or None (the same text with the anchor
# replaced), anchor in the pre-split statement)
REWRITES = (("offSec + ", "offSec + ", None, "BYTE_BASE + 11 + "),
            ("secret, BYTE_BASE + 11, length * XXH_PRIME64_1));", "BYTE_BASE",
             "+ XXH3_mix2Accs(acc_0, acc_1, secret, BYTE_BASE + 11)", "BYTE_BASE"),
            ("secret, BYTE_BASE + 192 - 64 - 11, ~(length * XXH_PRIME64_2)));", "BYTE_BASE",
             "+ XXH3_mix2Accs(acc_0, acc_1, secret, BYTE_BASE + 192 - 64 - 11)", "BYTE_BASE"))


def occurrences(source, constants):
    lines = open(source, encoding="utf-8").read().split(NL)
    out = []
    with open(constants, encoding="utf-8") as f:
        for r in csv.DictReader(f, delimiter=TAB):
            if r["file"] == FILE:
                text = lines[int(r["line"]) - 1]
                stmt = text.lstrip()
                out.append(dict(r, stmt=stmt, rel=int(r["col"]) - 1 - (len(text) - len(stmt))))
    return out


def pre_split_index(occ):
    index = {}
    for o in occ:
        if o["member"].split("#", 1)[0] not in PARENTS:
            continue  # only the two parents' statements moved
        named = (o["name"], o["role"])
        if index.setdefault((o["stmt"], o["rel"], o["literal"]), named) != named:
            raise SystemExit(f"pre-split statement names {o['literal']} twice: {o['stmt']!r}")
    return index


def lookup(index, o):
    hit = index.get((o["stmt"], o["rel"], o["literal"]))
    if hit:
        return hit
    for marker, anchor, pre_stmt, pre_anchor in REWRITES:
        if marker in o["stmt"]:
            at = o["stmt"].index(anchor)
            if pre_stmt is None:
                pre = o["stmt"].replace(anchor, pre_anchor, 1)
                rel = o["rel"] + (len(pre_anchor) - len(anchor) if o["rel"] >= at + len(anchor) else 0)
            else:
                pre, rel = pre_stmt, pre_stmt.index(pre_anchor) + o["rel"] - at
            hit = index.get((pre, rel, o["literal"]))
            if hit:
                return hit
    return None


def split_member(member):
    return member in PARENTS or member in NEW_MEMBERS or member.startswith("XXH3_len_")


def main():
    ledger_in, ledger_out, pre_src, pre_const, split_src, split_const = sys.argv[1:7]
    index = pre_split_index(occurrences(pre_src, pre_const))
    rows, missing, members = {}, [], set()
    for o in occurrences(split_src, split_const):
        if not split_member(o["member"].split("#", 1)[0]):
            continue
        members.add(o["member"].split("#", 1)[0])
        named = lookup(index, o)
        if named is None:
            missing.append(f"{o['member']} {o['literal']} {o['stmt']!r}")
        else:
            rows[(o["member"], o["literal"])] = named
    if missing:
        raise SystemExit("no pre-split occurrence for:\n  " + "\n  ".join(missing))
    kept, dropped = [], 0
    for line in open(ledger_in, encoding="utf-8").read().split(NL):
        cells = line.split(TAB)
        if line in SECTION:
            continue
        if len(cells) >= 3 and cells[0] == CLASS and split_member(cells[1].split("#", 1)[0]):
            dropped += 1
            continue
        kept.append(line)
    while kept and not kept[-1]:
        kept.pop()

    def order(item):
        (member, literal), _ = item
        name, _, k = member.partition("#")
        return name, int(k or 0), literal

    body = "".join(TAB.join((CLASS, m, lit, name, role)) + NL for (m, lit), (name, role) in sorted(rows.items(), key=order))
    with open(ledger_out, "w", encoding="utf-8", newline=NL) as w:
        w.write(NL.join(kept) + NL + NL.join(SECTION) + NL + body)
    print(f"split members {len(members)}, rows written {len(rows)}, pre-split rows replaced {dropped}")


main()
