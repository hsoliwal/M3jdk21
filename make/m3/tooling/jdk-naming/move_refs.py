#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""MOVE_REFS v1.1: the live references of an area moved by move_module_area.py follow the move.

usage: move_refs.py <repo root> <rules.tsv> [--check]
Rules (tab separated, '#' comments):
  FILES   <glob>                        files the rules apply to (workflows, scripts the workflows run); repeatable
  REPLACE <old literal>  <new literal>  every occurrence in the FILES (a new literal that already contains the old one is
                                        applied once: a second run changes nothing)
  SPARSE  <entry>  <added entry>...     in every sparse-checkout block of a workflow that lists <entry> exactly, the
                                        added entries follow it (same indentation), unless already listed
  RESIDUE <old prefix>                  after the run, a live reference to <old prefix> in the FILES is a finding
  KEEP    <literal>                     a residue line containing this literal is allowed (paths that still exist)
  PROTECT <literal>                     a line containing this literal is never rewritten and never residue (v1.1:
                                        paths inside a checkout of an older commit, e.g. baseline/m3/...)
Idempotent; --check writes nothing and exits 1 when a run would change a file; exit 2 when RESIDUE findings remain.
Written after the m3/arrays move (#2825) left 17 workflows and 4 scripts reading m3/arrays/src/main/java and
m3/arrays/src/main/native, and sparse checkouts without the module paths (RECIPE: a move is not done until its live
references follow)."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1]).resolve()
RULES = Path(sys.argv[2]).resolve()
CHECK = "--check" in sys.argv
SPARSE_HEAD = re.compile(r"^(\s*)sparse-checkout:\s*\|\s*$")


def read_rules():
    rules = {"FILES": [], "REPLACE": [], "SPARSE": [], "RESIDUE": [], "KEEP": [], "PROTECT": []}
    for raw in RULES.read_text(encoding="utf-8").splitlines():
        if not raw.strip() or raw.startswith("#"):
            continue
        kind, *rest = raw.split("\t")
        rules[kind].append(rest[0] if kind in ("FILES", "RESIDUE", "KEEP", "PROTECT") else tuple(rest))
    return rules


def replace_all(text: str, old: str, new: str) -> str:
    if old in new:
        # protect occurrences already rewritten, then rewrite the rest once
        marker = "\0MOVE_REFS\0"
        text = text.replace(new, marker)
        text = text.replace(old, new)
        return text.replace(marker, new)
    return text.replace(old, new)


def sparse_add(text: str, rows) -> str:
    lines = text.split("\n")
    out, i = [], 0
    while i < len(lines):
        out.append(lines[i])
        head = SPARSE_HEAD.match(lines[i])
        i += 1
        if not head:
            continue
        indent = len(head.group(1))
        block = []
        while i < len(lines) and (lines[i].strip() == "" or len(lines[i]) - len(lines[i].lstrip()) > indent):
            block.append(lines[i])
            i += 1
        listed = {b.strip() for b in block}
        grown = []
        for line in block:
            grown.append(line)
            for entry, *added in rows:
                if line.strip() == entry:
                    pad = line[:len(line) - len(line.lstrip())]
                    for a in added:
                        if a not in listed:
                            grown.append(pad + a)
                            listed.add(a)
        out.extend(grown)
    return "\n".join(out)


def main() -> int:
    rules = read_rules()
    files = sorted({p for g in rules["FILES"] for p in ROOT.glob(g) if p.is_file()})
    changed, findings = [], []
    for path in files:
        text = path.read_text(encoding="utf-8")
        new = text
        if rules["PROTECT"]:
            # v1.1: a line naming a PROTECT literal (e.g. a checkout of an older commit under baseline/) keeps its bytes
            kept = new.split("\n")
            for old, rep in rules["REPLACE"]:
                kept = [line if any(p in line for p in rules["PROTECT"]) else replace_all(line, old, rep) for line in kept]
            new = "\n".join(kept)
        else:
            for old, rep in rules["REPLACE"]:
                new = replace_all(new, old, rep)
        if path.suffix in (".yml", ".yaml"):
            new = sparse_add(new, rules["SPARSE"])
        if new != text:
            changed.append(path.relative_to(ROOT).as_posix())
            if not CHECK:
                path.write_text(new, encoding="utf-8", newline="\n")
        for number, line in enumerate(new.split("\n"), 1):
            if any(r in line for r in rules["RESIDUE"]) and not any(k in line for k in rules["KEEP"] + rules["PROTECT"]):
                findings.append(f"{path.relative_to(ROOT).as_posix()}:{number}: {line.strip()[:160]}")
    for c in changed:
        print("CHANGED\t" + c)
    for f in findings:
        print("RESIDUE\t" + f)
    print(f"MOVE_REFS v1.1 files={len(files)} changed={len(changed)} residue={len(findings)} check={CHECK}")
    if findings:
        return 2
    return 1 if CHECK and changed else 0


if __name__ == "__main__":
    sys.exit(main())
