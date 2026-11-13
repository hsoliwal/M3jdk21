#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""PRODUCT_NAME_UNIFY v1: the product name is one token (user, 2026-11-09: "M3JDK is one unit, not m3 + jdk").

usage: product_name.py --root DIR [--check] [--exclude PREFIX ...]
In live text (Markdown, workflows, Python, shell, Java; not evidence segments, not TSV/JSON ledgers, not this recipe's
directory, not NAMING_KEEP.tsv files) the split spellings become the per-kind product name of NAMING_STANDARD
(prose M3JDK, kebab m3jdk-, snake m3jdk_):
  M3 JDK              -> M3JDK        prose, workflow and step names
  M3_JDK  (the name)  -> M3JDK        the environment variable every script reads as M3JDK (docs still said M3_JDK)
  --m3-jdk[-x]        -> --m3jdk[-x]  a command-line flag ...
  m3_jdk[_x]          -> m3jdk[_x]    ... and its argparse destination, renamed together
Not touched: invariant ids pinned by ledgers (M3-JDK-PORT-1), donor paths of the com.synexia repository (m3-jdk-*),
M3_JDK21-style constants (jdk_naming.py PRODUCT_CONST owns them). Prints one EDIT row per file; --check writes
nothing and exits 1 when anything would change (a second run: 0)."""
import argparse
import re
import sys
from pathlib import Path

EVIDENCE = re.compile(r"/(recipe-custody|custody|evidence|qualification|recipes?|retained-variants|proofs?|snapshots?|"
                      r"before|after|postimages?|preimages?|donors?|intakes?|fixtures?|golden|expected|samples?|tasks)/")
SOURCE_ROOT = re.compile(r"^(.*?/src/(?:main|test)/(?:java|resources)/|src/[^/]+/share/classes/|test/jdk/)")
SUFFIXES = {".md", ".yml", ".yaml", ".py", ".sh", ".java"}
RULES = (
    (re.compile(r"(?<![\w-])M3 JDK(?![\w-])"), "M3JDK"),
    (re.compile(r"(?<![\w$])M3_JDK(?![\w$])"), "M3JDK"),
    (re.compile(r"(?<![\w-])--m3-jdk(?=[-\s\"'=,)\]]|$)", re.M), "--m3jdk"),
    (re.compile(r"(?<![\w$])m3_jdk(?=_|(?![\w$]))"), "m3jdk"),
)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", required=True)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--exclude", action="append", default=["make/m3/evidence/", "make/m3/qualification/", "m3/vendor/",
                                                         "make/m3/tooling/jdk-naming/"])
    a = ap.parse_args()
    root = Path(a.root)
    keep_file = root / "make/m3/tooling/jdk-naming/NAMING_KEEP.tsv"
    keep = {line.split("\t")[0] for line in keep_file.read_text(encoding="utf-8").splitlines()[1:]} if keep_file.is_file() else set()
    changed = {}
    for p in sorted(root.rglob("*")):
        if not p.is_file() or p.suffix not in SUFFIXES or ".git" in p.parts:
            continue
        rel = p.relative_to(root).as_posix()
        pre = SOURCE_ROOT.match(rel)
        if rel.startswith(tuple(a.exclude)) or rel in keep or EVIDENCE.search("/" + (pre.group(1) if pre else rel)):
            continue
        try:
            text = p.read_bytes().decode("utf-8")
        except UnicodeDecodeError:
            continue
        new = text
        for rx, rep in RULES:
            new = rx.sub(rep, new)
        if new != text:
            changed[p] = new
            print(f"EDIT\t{rel}")
    print(f"PRODUCT_NAME_UNIFY v1 edits={len(changed)} check={a.check}", file=sys.stderr)
    if a.check:
        return 1 if changed else 0
    for p, text in changed.items():
        p.write_bytes(text.encode("utf-8"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
