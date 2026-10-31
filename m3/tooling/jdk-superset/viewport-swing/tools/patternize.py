# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""patternize.py <ATOMS.tsv> <ATOM_SPEC.tsv> <templates_dir> <PERF.tsv> <TEST_PATTERNS.tsv> : PATTERNIZE stage (CPU).

One row per atom: its contract pattern (ATOMS.tsv), the dynamic test that checks it (ATOM_SPEC.tsv), the oracle of the
harness family, the route the harness takes for that atom (the right-hand side of its `case` in the template's
dispatch, extracted mechanically) and whether PERF.tsv measures it. Prints the per-pattern counts."""
import collections
import csv
import os
import re
import sys

ORACLE = {
    "ListLanes": "stock BasicListUI loops (getCellBounds int sum, convertLocationToRow walk) + M3RowOffsets Javadoc as code",
    "PieceScript": "java.util.ArrayList of the old row index per row (NEW = -1); private atoms by their inline contract",
    "MeasureScript": "stock BasicListUI.updateLayoutState of a second JList on the same DefaultListModel (+ Javadoc count)",
    "SorterScript": "stock javax.swing.table.TableRowSorter on the same DefaultTableModel, keys, filter and calls",
    "SizesScript": "javax.swing.SizeSequence; inViewOrder: stock SortManager.setViewRowHeightsFromModel over TableRowSorter",
    "TableScript": "java.util.Hashtable fed the same calls + stock JTree getExpandedDescendants/removeDescendantToggledPaths",
    "CacheScript.FIXED": "stock javax.swing.tree.FixedHeightLayoutCache on the same DefaultTreeModel and events",
    "CacheScript.VARIABLE": "stock javax.swing.tree.VariableHeightLayoutCache on the same DefaultTreeModel and events",
}
WHITE_BOX = {
    "mapSlot": "java.util.HashMap (open addressing, backward-shift deletion), then the stock cache",
    "mapGet": "java.util.HashMap (open addressing, backward-shift deletion), then the stock cache",
    "mapPut": "java.util.HashMap (open addressing, backward-shift deletion), then the stock cache",
    "mapRemove": "java.util.HashMap (open addressing, backward-shift deletion), then the stock cache",
    "add": "plain prefix sums (Fenwick add/prefix/build), then the stock cache",
    "prefix": "plain prefix sums (Fenwick add/prefix/build), then the stock cache",
    "build": "plain prefix sums (Fenwick add/prefix/build), then the stock cache",
    "mix": "bijection over 65536 inputs; List.lastIndexOf; lookup filing properties; then the stock cache",
    "scan": "List.lastIndexOf over the first n entries, then the stock cache",
    "buildLookup": "every entry filed once, reachable by probing, table of highestOneBit(n) * 4, then the stock cache",
    "checkChild": "TreePath.pathByAddingChild(null) message; null child through both caches",
    "sortLane": "Arrays.sort(Integer[], host order) on 11 sizes x 6 shapes",
    "runEnd": "run end and in-place reversal computed by a linear scan",
    "binaryInsertion": "Arrays.sort of the range, untouched outside",
    "merge": "Arrays.sort of the range, untouched outside",
    "binarySearch": "Arrays.binarySearch(Integer[], key, host order) for absent keys",
    "find": "Arrays.binarySearch over the custom row indices of SizeSequence",
    "lowerBound": "Arrays.binarySearch insertion point over the custom row indices",
    "nextCapacity": "Hashtable growth rule 2n + 1, capped at Integer.MAX_VALUE - 8; real table length via reflection",
    "isStableValue": "the Javadoc list of immutable simple types",
    "supportsIncrementalSizing": "the Javadoc list of delegates and borders",
}
CASE = re.compile(r'case\s+((?:"[^"]+"\s*,?\s*)+)->\s*', re.S)


def routes(text):
    out = {}
    for m in CASE.finditer(text):
        rest = text[m.end():]
        if rest.startswith("{"):
            depth, end = 0, 0
            for i, ch in enumerate(rest):
                depth += ch == "{"
                depth -= ch == "}"
                if depth == 0:
                    end = i + 1
                    break
            body = rest[1:end - 1]
        else:
            body = rest[:rest.index(";") + 1] if ";" in rest else rest[:80]
        body = " ".join(body.split())
        for name in re.findall(r'"([^"]+)"', m.group(1)):
            out.setdefault(name, body[:160])
    return out


def main():
    atoms_tsv, spec_tsv, templates, perf_tsv, out = sys.argv[1:6]
    with open(atoms_tsv, encoding="utf-8") as f:
        atoms = list(csv.DictReader([l for l in f if not l.startswith("#")], delimiter="\t"))
    spec = {r["atom_id"]: r for r in csv.DictReader(open(spec_tsv, encoding="utf-8"), delimiter="\t")}
    measured = set()
    if os.path.exists(perf_tsv):
        with open(perf_tsv, encoding="utf-8") as f:
            measured = {r["atom"] for r in csv.DictReader([l for l in f if not l.startswith("#")], delimiter="\t")}
    texts = {}
    for pkg in ("list", "table", "tree"):
        for name in os.listdir(os.path.join(templates, pkg)):
            texts[name[:-len(".java")]] = open(os.path.join(templates, pkg, name), encoding="utf-8").read()
    route_of = {h: routes(t) for h, t in texts.items()}
    rows, counts = [], collections.Counter()
    for a in atoms:
        s = spec[a["atom_id"]]
        harness = s["probe"].split(".")[0]
        family = harness
        if harness == "CacheScript":
            family += ".FIXED" if "Kind.FIXED" in s["probe"] else ".VARIABLE"
        key = re.search(r'"([^"]+)"\)$', s["probe"]).group(1)
        if harness == "ListLanes":
            key = a["file"] + "#" + a["name"]
        oracle = WHITE_BOX.get(a["name"], ORACLE[family])
        if a["owner"].endswith(".Host"):
            oracle = "recorded calls of the host + what the lanes do with the answer, checked against TableRowSorter"
        route = route_of.get(harness, {}).get(key) or route_of.get(harness, {}).get(a["name"], "-")
        rows.append((a["atom_id"], a["pattern"], f"{s['test']}::{s['display']}", oracle, route,
                     "measured" if a["atom_id"] in measured else "-"))
        counts[a["pattern"]] += 1
    with open(out, "w", encoding="utf-8", newline="\n") as w:
        w.write("atom\tpattern\ttest\toracle\troute\tperf\n")
        w.writelines("\t".join(r) + "\n" for r in rows)
    print(f"atoms={len(rows)} measured={sum(1 for r in rows if r[5] == 'measured')} "
          + " ".join(f"{k}={v}" for k, v in sorted(counts.items())))


main()
