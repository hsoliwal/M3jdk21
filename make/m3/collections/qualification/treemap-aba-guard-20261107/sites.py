#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Enumerate every comparator-calling slot lookup of M3TreeMap and its users (SITES.tsv), and gate the guards.

Scope: M3SlotMap, M3OrderedSlotMap (the shared slot-map bodies a tree owner inherits), M3TreeMap, M3TreeSet and the
filtered views over tree owners (M3FilteredViews, M3MutableFilteredViews). A site is a call of a slot locator
(findSlot, entrySlot, firstSlot, lastSlot, nextSlot, previousIndexedSlot, nearIndexedSlot, near) or a key walk
(Tree.find, Tree.near, the put/defer descents). For each site: enclosing member (nearest preceding member header),
locator, whether it calls the user comparator, what the located slot feeds within the next lines (erase, write,
read, return) and the head guard that covers it. --check verifies that every guard of recipe TREEMAP_ABA_GUARD v1
is present in M3TreeMap (exit 1 otherwise) without writing.
usage: python -I sites.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
FILES = ("M3SlotMap.java", "M3OrderedSlotMap.java", "M3TreeMap.java", "M3TreeSet.java", "M3FilteredViews.java",
         "M3MutableFilteredViews.java")
LOCATORS = ("findSlot", "entrySlot", "firstSlot", "lastSlot", "nextSlot", "previousIndexedSlot", "nearIndexedSlot",
            "near", "find", "cursorStart", "firstCursorSlot")
CALL = re.compile(r"(?<![\w])(?:(\w+)\.)?(" + "|".join(LOCATORS) + r")\(")
DECL = re.compile(r"\bint\s+(" + "|".join(LOCATORS) + r")\s*\(")
MEMBER = re.compile(r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*"
                    r"(?:(?:public|protected|private|static|final|abstract|default|synchronized)\s+)*(?:<[^>]+>\s+)?"
                    r"(?P<type>[\w.]+(?:<[^()]*?>)?(?:\[\])*\s+)?(?P<name>\w+)\s*\(")
CONTROL = {"if", "for", "while", "switch", "catch", "return", "else", "new", "try", "throw", "super", "this"}
USES = (("erase", r"eraseAt\(|tree\.erase\(|\bpoll\("), ("write", r"writeAt\(|setValue\("),
        ("read", r"valueAt\(|keyAt\(|snapshot\(|entryFacade\(|requiredKey\(|nullableKey\(|LiveEntry\("),
        ("count", r"count\+\+"), ("return", r"\breturn\b"))
COMPARATOR = {"findSlot": "always", "entrySlot": "after a structural change (findSlot)", "find": "always",
              "near": "always", "nearIndexedSlot": "always", "firstSlot": "bounded views", "lastSlot": "bounded views",
              "nextSlot": "bounded views", "previousIndexedSlot": "bounded views", "cursorStart": "bounded views",
              "firstCursorSlot": "bounded views (tree owners)"}
GUARD = {"findSlot": "Tree.find walked() after every comparator call",
         "entrySlot": "hint path calls no comparator; fallback = findSlot (Tree.find walked())",
         "find": "walked() after every comparator call",
         "near": "Tree.near walked(); check after the bound probe; located() after slotWithin",
         "nearIndexedSlot": "= near (Tree.near walked(); bound-probe check; located())",
         "firstSlot": "Tree.near walked(); located() after slotWithin",
         "lastSlot": "Tree.near walked(); located() after slotWithin",
         "nextSlot": "located() after slotWithin", "previousIndexedSlot": "located() after slotWithin",
         "cursorStart": "creation never fails: a structural change yields -1, the cursor's epoch (read before) reports"
                        " it at the first call; the comparator's own exceptions propagate",
         "firstCursorSlot": "M3TreeMap: cursorStart(false); other owners: firstSlot (no revision change)"}
TM = "M3TreeMap.java"
# (file, member header regex, required guard text inside that member) checked by --check
REQUIRED = ((TM, r"int find\(K key\)", "walked(expected)"),
            (TM, r"int near\(K key, int relation\) \{\n            int s", "walked(expected)"),
            (TM, r"@Override int firstSlot\(\)", "located("), (TM, r"@Override int lastSlot\(\)", "located("),
            (TM, r"@Override int nextSlot\(int slot\)", "located("),
            (TM, r"int previousIndexedSlot\(int slot\)", "located("),
            (TM, r"private int near\(K key, int relation\)", "located("),
            (TM, r"private int near\(K key, int relation\)", "high); check(expected);"),
            (TM, r"private int near\(K key, int relation\)", "low); check(expected);"),
            (TM, r"@Override public V put\(K key, V value\)", "keyAt(s)); check(expected);"),
            (TM, r"@Override public V put\(K key, V value\)", "int expected = revision(); // before the admission"),
            (TM, r"public boolean defer\(K key\)", "keyAt(s)); check(expected);"),
            (TM, r"int cursorStart\(boolean last\)", "if (revision() == expected) { throw changed; }"),
            (TM, r"@Override int firstCursorSlot\(\)", "cursorStart(false)"),
            ("M3SlotMap.java", r"final <T> Iterator<T> slots\(int kind\)",
             "private int expected = revision(); // before the first slot"),
            ("M3SlotMap.java", r"final <T> Iterator<T> slots\(int kind\)", "private int cursor = firstCursorSlot();"),
            ("M3MutableFilteredViews.java", r"SlotCursor\(M3TreeMap<\?, \?> slots, boolean fromLast\)",
             "slots.cursorStart(fromLast)"),
            (TM, r"@Override V eraseAt\(int slot\)", "tree.height[slot] == 0"))


def member_body(text: str, header: str) -> str:
    match = re.search(header, text)
    if not match:
        return ""
    start = text.index("{", match.start())
    depth, index = 0, start
    while index < len(text):
        depth += {"{": 1, "}": -1}.get(text[index], 0)
        if depth == 0:
            return text[start:index + 1]
        index += 1
    return text[start:]


def member_of(line: str):
    """The member a header line declares (method or constructor), else None."""
    header = MEMBER.match(line)
    if (header and header.group("name") not in CONTROL
            and (header.group("type") or header.group("name")[0].isupper())
            and ("{" in line or not line.rstrip().endswith(";"))
            and not line.strip().startswith(("return", "//", "*", "new", "throw"))):
        return header.group("name")
    return None


def sites(root: Path):
    rows = []
    for name in FILES:
        lines = (root / SRC / name).read_text(encoding="utf-8").split("\n")
        member = "-"
        for number, line in enumerate(lines, 1):
            member = member_of(line) or member
            for call in CALL.finditer(line):
                receiver, locator = call.group(1) or "this", call.group(2)
                if DECL.search(line) and DECL.search(line).group(1) == locator and call.start() > DECL.search(line).start():
                    continue  # the locator's own declaration
                if locator == "find" and receiver != "tree":
                    continue
                if locator == "near" and receiver not in ("tree", "this"):
                    continue
                window = line  # the call's line and its statement continuation, never the next member
                for following in lines[number:number + 2]:
                    if member_of(following) or following.strip().startswith("}"):
                        break
                    window += " " + following
                uses = ",".join(label for label, pattern in USES if re.search(pattern, window)) or "-"
                kind = "walk (Tree." + locator + ")" if receiver == "tree" else locator
                rows.append((name, str(number), member, kind, receiver, COMPARATOR[locator], uses, GUARD[locator]))
    tree = (root / SRC / "M3TreeMap.java").read_text(encoding="utf-8")
    for header, label in ((r"@Override public V put\(K key, V value\)", "put"),
                          (r"public boolean defer\(K key\)", "defer")):
        number = tree[:re.search(header, tree).start()].count("\n") + 1
        rows.append(("M3TreeMap.java", str(number), label, "walk (" + label + " descent)", "this", "always",
                     "write" if label == "put" else "-", "check(expected) after every descent comparator call"
                     + ("; revision read before admission (write half of compute/merge)" if label == "put" else "")))
    return rows


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(HERE.parents[4]))
    parser.add_argument("--check", action="store_true", help="only verify the guards; write nothing")
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    texts = {name: (root / SRC / name).read_text(encoding="utf-8").replace("\r\n", "\n") for name in FILES}
    missing = [name + " " + header + " :: " + guard for name, header, guard in REQUIRED
               if guard not in member_body(texts[name], header)]
    rows = sites(root)
    if not args.check:
        header = ("file", "line", "member", "locator", "receiver", "calls_comparator", "feeds", "head_guard")
        table = "\n".join("\t".join(row) for row in [header] + rows) + "\n"
        (HERE / "SITES.tsv").write_bytes(table.encode("utf-8"))
    by_file = {}
    for row in rows:
        by_file[row[0]] = by_file.get(row[0], 0) + 1
    print("SITES total=%d %s guards=%d/%d%s" % (len(rows), " ".join("%s=%d" % item for item in sorted(by_file.items())),
          len(REQUIRED) - len(missing), len(REQUIRED), "" if not missing else " MISSING: " + "; ".join(missing)))
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
