# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""atomize_viewport.py <census.tsv> <src_root> <out ATOMS.tsv> : ATOMIZE stage of the viewport-swing atom tests (CPU).

Reads the PER_ATOM_TEST_CENSUS rows of sun.swing.list, sun.swing.table and sun.swing.tree (method and constructor
atoms of M3JDK master), finds each atom's declaration in its source (modifiers, return type, parameter types; the
k-th census row of a name maps to the k-th declaration of that name in the file, census rows being in source order)
and groups the atoms by contract pattern with the ordered rules of VIEWPORT_ATOM_PATTERNS (first match wins).
The pattern names the oracle family the generator gen_atom_tests.py instantiates. Prints one summary line."""
import collections
import csv
import os
import re
import sys

PATTERNS_VERSION = "VIEWPORT_ATOM_PATTERNS v1"
PACKAGES = re.compile(r"^sun\.swing\.(list|table|tree)$")
MASK = re.compile(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"|'(?:\\.|[^'\\])'", re.S)
KEYWORDS = {"return", "new", "throw", "else", "case", "do", "if", "while", "for", "switch", "catch", "synchronized",
            "assert", "yield"}
MODIFIERS = ("public", "protected", "private", "static", "final", "synchronized", "abstract", "default", "native")

# Ordered rules: (owner regex, name regex, pattern). Patterns follow the task's contract groups: getter/setter,
# add/remove/get invariants, index mapping, range/window math, equals/hashCode, iteration order, exceptional
# inputs; plus sort order, prefix aggregates, capability gates, lifecycle and model events for the layout state.
# A constructor (name == owner) is INITIAL_STATE: the state a new instance answers, and its argument checks.
RULES = [
    (r"\.Visible$", r".*", "ITERATION_ORDER"),
    (r"\.Host$", r".*", "INTERFACE_CONTRACT"),
    (r"^M3RowOffsets$", r".*", "INTERFACE_CONTRACT"),
    (r".*", r"^(checkIndex|checkChild)$", "EXCEPTIONAL_INPUT"),
    (r".*", r"^(mix|mapSlot|scan|buildLookup|hashed|nextCapacity)$", "EQUALS_HASHCODE"),
    (r".*", r"^(expandedKeysBelow|getVisiblePathsFrom|snapshot)$", "ITERATION_ORDER"),
    (r".*", r"^(accepts|computeValue|stockToggledPaths|indexes|isStableValue|supportsIncrementalSizing|sameInputs"
            r"|track|indexed|monotone|bound)$", "GATE"),
    (r".*", r"^(sortLane|runEnd|binaryInsertion|merge|binarySearch|insertInOrder|sort|sortExistingData"
            r"|sortKeysChanged|initializeFilteredMapping|createModelToView|createViewToModel"
            r"|setModelToViewFromViewToModel)$", "SORT_ORDER"),
    (r".*", r"^(fenwick|fenwickAdd|fenwickPrefix|add|prefix|build|rebuildFenwick|bubble|propagate|changeTot"
            r"|addInvalid|markDirty|subtreeMax|maxNodeWidth|total|start)$", "AGGREGATE_PREFIX"),
    (r".*", r"^(startOf|firstEndingAfter|rowAt|modelIndex|viewIndex|getIndex|getPosition|getPathForRow"
            r"|getRowForPath|locate|find|rowOf|rowOfNode|rowOfChild|descend|startOffset|slotForModelIndex"
            r"|lowerBound|childNode|childIndex|childIndex0|childValue|pathAt|stateAt|resolve|resolveParent|walk"
            r"|mapGet|inViewOrder)$", "INDEX_MAPPING"),
    (r".*", r"^(rowContainingY|getPathClosestTo|getVisibleChildCount|yOrigin|getBounds|boundsAt|boundsOfNode"
            r"|boundsOfChild|prefHeight|validX|validW|getPreferredHeight|getPreferredWidth|visible)$", "RANGE_WINDOW"),
    (r".*", r"^(treeNodesChanged|treeNodesInserted|treeNodesRemoved|treeStructureChanged|added|removed|changed"
            r"|rowsInserted|rowsDeleted|rowsUpdated|rowsInserted0|rowsDeleted0|rowsUpdated0|shouldOptimizeChange"
            r"|childInserted|childRemoved)$", "MODEL_EVENT"),
    (r".*", r"^(expand|collapse|makeVisible|ensurePathIsExpanded|setExpandedState|isExpanded|getExpandedState"
            r"|refreshVisible|validateDescendants|load|measure|measureAll|remeasure|same|size|keep|updateSize"
            r"|setSize|invalidateSizes|invalidatePathBounds|deepInvalidate|updateNodeSizes|walkUpdate"
            r"|validateReverse|rebuild|setModel)$", "LIFECYCLE"),
    (r".*", r"^(insert|remove|change|split|open|reset|clear|put|link|attach|unlink|newNode|newState|addState"
            r"|removeState|insertSlot|removeSlot|insertLane|removeLane|laneCountChanged|grow|growNodes|lanes"
            r"|reindex|unmap|repath|mapPut|mapRemove|dropLookup|toDense|setSizes|insertEntries|removeEntries"
            r"|getSizes|removeDescendants)$", "ADD_REMOVE_GET"),
    (r".*", r"^(fullMeasuresOnDeltas|fullSortsOnDeltas|rebuilds|remeasured|cellWidth|cellHeights|count|length"
            r"|defaultSize|bucketCount|transformed|viewRowCount|rowCount|getRowCount|expanded|is|set|bind"
            r"|getSize|setRootVisible|setRowHeight|setNodeDimensions)$", "GETTER_SETTER"),
]


def masked(text):
    return MASK.sub(lambda m: re.sub(r"[^\n]", " ", m.group(0)), text)


def split_params(params):
    out, depth, cur = [], 0, []
    for ch in params:
        if ch in "<[":
            depth += 1
        elif ch in ">]":
            depth -= 1
        if ch == "," and depth == 0:
            out.append("".join(cur))
            cur = []
        else:
            cur.append(ch)
    if "".join(cur).strip():
        out.append("".join(cur))
    types = []
    for p in out:
        p = re.sub(r"@\w+(\([^)]*\))?", " ", p).replace("final ", " ").strip()
        types.append(" ".join(p.split()[:-1]).replace(" ", ""))
    return types


def declarations(text, name):
    pat = re.compile(r"^[ \t]*((?:@\w+(?:\([^)]*\))?\s+)*)((?:(?:" + "|".join(MODIFIERS) + r")\s+)*)"
                     r"(?:<[^>]*>\s+)?(?:([\w.$<>\[\],?]+)\s+)?(" + re.escape(name) + r")\s*\(([^)]*)\)"
                     r"\s*(?:throws\s+[\w., ]+)?\s*[{;]", re.M)
    found = []
    for m in pat.finditer(text):
        ret = (m.group(3) or "").strip()
        if ret in KEYWORDS or ret.split(" ")[-1] in KEYWORDS:
            continue
        found.append((m.start(), m.group(2).split(), ret, split_params(m.group(5))))
    return found


def pattern_of(owner, name):
    if name == owner.split(".")[-1]:
        return "INITIAL_STATE"
    for o, n, p in RULES:
        if re.search(o, owner) and re.search(n, name):
            return p
    return "UNCLASSIFIED"


def main():
    census, src_root, out = sys.argv[1:4]
    rows = [r for r in csv.DictReader(open(census, encoding="utf-8"), delimiter="\t") if PACKAGES.match(r["package"])]
    texts, seen, result = {}, collections.Counter(), []
    for r in rows:
        path = os.path.join(src_root, "java.desktop", r["path"])
        if path not in texts:
            texts[path] = masked(open(path, encoding="utf-8").read())
        top = os.path.basename(r["path"])[:-len(".java")]
        simple = r["owner"].split(".")[-1]
        decls = declarations(texts[path], r["name"])
        if r["name"] == simple:
            decls = [d for d in decls if not d[2]]
        else:
            decls = [d for d in decls if d[2]]
        k = seen[(r["path"], r["name"], r["name"] == simple)]
        seen[(r["path"], r["name"], r["name"] == simple)] += 1
        if k >= len(decls):
            raise SystemExit(f"no declaration {k} of {r['owner']}.{r['name']} in {path}")
        _, mods, ret, params = decls[k]
        kind = "constructor" if not ret else "method"
        interface = r["owner"].endswith(".Host") or top == "M3RowOffsets"
        vis = next((m for m in mods if m in ("public", "protected", "private")), "public" if interface else "package")
        atom_id = f"{r['owner']}#{r['name']}({','.join(params)})"
        result.append({"atom_id": atom_id, "package": r["package"], "file": top, "owner": r["owner"], "name": r["name"],
                       "kind": kind, "returns": ret or "-", "params": ",".join(params) or "-", "visibility": vis,
                       "static": "static" if "static" in mods else "-", "n_tokens": r["n_tokens"],
                       "complexity": r["complexity"], "census_status": r["test_status"],
                       "pattern": pattern_of(r["owner"], r["name"])})
    cols = list(result[0].keys())
    with open(out, "w", encoding="utf-8", newline="\n") as w:
        w.write("# " + PATTERNS_VERSION + "\n" + "\t".join(cols) + "\n")
        w.writelines("\t".join(a[c] for c in cols) + "\n" for a in result)
    by = collections.Counter(a["pattern"] for a in result)
    print(f"atoms={len(result)} files={len(texts)} " + " ".join(f"{k}={v}" for k, v in sorted(by.items())))
    if by.get("UNCLASSIFIED"):
        raise SystemExit("unclassified atoms: " + ", ".join(a["atom_id"] for a in result
                                                            if a["pattern"] == "UNCLASSIFIED"))


main()
