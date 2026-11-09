#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Closure of donor Java types at a donor revision (read-only, git show): same-package identifiers, imports and qualified
references, comments excluded; types outside the source roots print MISSING.

usage: python -I closure.py <donor-git> <rev> <root>[,<root>...] <fqn>[,<fqn>...]   (the inventory of RELOCATION_MAP.tsv)
"""
import re
import subprocess
import sys

GIT, REV, ROOTS = sys.argv[1], sys.argv[2], sys.argv[3].split(",")   # source roots, e.g. .../src/main/java
SEEDS = sys.argv[4].split(",")                            # fully qualified type names
IDENT = re.compile(r"\b[A-Z][A-Za-z0-9_]*\b")
IMPORT = re.compile(r"^import\s+(?:static\s+)?([\w.]+)\s*;", re.M)
QUALIFIED = re.compile(r"\b((?:[a-z]\w*\.)+[A-Z]\w*)")
COMMENT = re.compile(r"/\*.*?\*/|//[^\n]*", re.S)


def git(*args):
    return subprocess.run(["git", "-C", GIT, *args], check=True, capture_output=True).stdout.decode("utf-8")


listing = {}


def package_types(pkg):
    if pkg not in listing:
        listing[pkg] = {}
        for root in ROOTS:
            out = git("ls-tree", "--name-only", REV, root + "/" + pkg.replace(".", "/") + "/")
            for p in out.splitlines():
                if p.endswith(".java"):
                    listing[pkg].setdefault(p.rsplit("/", 1)[1][:-5], p)
    return listing[pkg]


seen, todo, docs_only = {}, list(SEEDS), {}
while todo:
    fqn = todo.pop()
    if fqn in seen:
        continue
    pkg, name = fqn.rsplit(".", 1)
    path = package_types(pkg).get(name)
    if path is None:
        print("MISSING	%s" % fqn)
        seen[fqn] = ("-", "-", 0)
        continue
    text = git("show", REV + ":" + path)
    seen[fqn] = (path, git("rev-parse", REV + ":" + path).strip(), text.count("\n"))
    code = COMMENT.sub(" ", text)
    for imported in IMPORT.findall(code) + [q for q in QUALIFIED.findall(code) if q.startswith("com.")]:
        if imported.startswith(("java.", "javax.", "jdk.")):
            continue
        parts = imported.split(".")
        for cut in range(len(parts) - 1, 0, -1):
            if parts[cut - 1][:1].islower() and parts[cut][:1].isupper():
                todo.append(".".join(parts[:cut + 1]))
                break
    local = package_types(pkg)
    for ident in set(IDENT.findall(code)):
        if ident in local and ident != name:
            todo.append(pkg + "." + ident)
    for ident in set(IDENT.findall(text)) - set(IDENT.findall(code)):
        if ident in local and pkg + "." + ident not in SEEDS:
            docs_only.setdefault(pkg + "." + ident, set()).add(name)
for fqn in sorted(seen):
    path, blob, lines = seen[fqn]
    print("CLOSURE\t%s\t%s\t%d\t%s" % (fqn, blob[:12], lines, path))
for fqn in sorted(set(docs_only) - set(seen)):
    print("DOC_ONLY\t%s\tnamed in comments of %s" % (fqn, ",".join(sorted(docs_only[fqn]))))
print("TOTAL types=%d lines=%d" % (len(seen), sum(v[2] for v in seen.values())))
