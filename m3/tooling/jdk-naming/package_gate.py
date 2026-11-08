#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""PACKAGE_LAYOUT_GATE v1: no new package or module-root sprawl in M3jdk21.

The rule (user decision 2026-11-08): new code joins one of the existing packages and module roots; a new package, a
new source root or a package split across more source roots needs the user's approval, given as a reviewed change
to PACKAGE_BASELINE.tsv next to this script.

Source root = the directory before /src/main/java/ of a Java main source. Kinds:
  VENDOR  under m3/vendor/;
  COPY    a path segment names recipe/qualification custody, fixtures, intakes, evidence, tasks, before/after/out
          images, receiving, retained variants, resources, proofs or snapshots (evidence copies, not product);
  REAL    carries its own pom.xml or module-info.java;
  LOOSE   anything else.
Product packages are the packages of REAL and LOOSE roots.

  package_gate.py baseline [--tree REV | --paths FILE]   print the baseline of a tree
  package_gate.py check    [--tree REV | --paths FILE] [--baseline FILE]
check fails (exit 1) on: a source root (any kind) absent from the baseline; a product package absent from the
baseline; a product package declared by more source roots than the baseline allows (SPLIT rows; an unlisted package
allows one). Removals always pass. Prints one row per violation and a summary line."""
import argparse
import collections
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
COPY = re.compile(r"/(recipe-custody|custody|fixtures?|test-fixtures|intakes?|evidence|tasks|before|after|out|inverse|"
                  r"receiving|retained-variants|candidate|qualified|resources|proof|baseline|postimages?|preimages?|"
                  r"donor|donors-src|snapshots?|golden|expected|samples?|qualification|recipes?)/")
SOURCE = re.compile(r"^(.*)/src/main/java/(.+)/([^/]+)\.java$")


def layout(paths):
    """{root: kind}, {product package: set(roots)} of one tree."""
    present = set(paths)
    pkgs = collections.defaultdict(set)
    for p in paths:
        m = SOURCE.match(p)
        if m and m.group(3) not in ("module-info", "package-info"):
            pkgs[m.group(1)].add(m.group(2).replace("/", "."))
    kinds = {}
    for root in pkgs:
        if root.startswith("m3/vendor/"):
            kinds[root] = "VENDOR"
        elif COPY.search("/" + root + "/"):
            kinds[root] = "COPY"
        elif f"{root}/pom.xml" in present or f"{root}/src/main/java/module-info.java" in present:
            kinds[root] = "REAL"
        else:
            kinds[root] = "LOOSE"
    product = collections.defaultdict(set)
    for root, ps in pkgs.items():
        if kinds[root] in ("REAL", "LOOSE"):
            for p in ps:
                product[p].add(root)
    return kinds, product


def read_paths(args):
    if args.paths:
        lines = Path(args.paths).read_text(encoding="utf-8").splitlines()
        return [line.split(" ", 1)[-1] for line in lines if line.strip()]
    out = subprocess.run(["git", "ls-tree", "-r", "--name-only", args.tree], capture_output=True, check=True)
    return out.stdout.decode("utf-8").splitlines()


def baseline_rows(paths):
    kinds, product = layout(paths)
    rows = [f"ROOT\t{r}\t{kinds[r]}" for r in sorted(kinds)]
    rows += [f"PACKAGE\t{p}" for p in sorted(product)]
    rows += [f"SPLIT\t{p}\t{len(rs)}" for p, rs in sorted(product.items()) if len(rs) > 1]
    return rows


def check(paths, baseline):
    roots, packages, split = set(), set(), {}
    for line in baseline.splitlines():
        cols = line.split("\t")
        if cols[0] == "ROOT":
            roots.add(cols[1])
        elif cols[0] == "PACKAGE":
            packages.add(cols[1])
        elif cols[0] == "SPLIT":
            split[cols[1]] = int(cols[2])
    kinds, product = layout(paths)
    bad = [f"NEW_ROOT\t{r}\t{kinds[r]}" for r in sorted(kinds) if r not in roots]
    bad += [f"NEW_PACKAGE\t{p}\t{' '.join(sorted(product[p]))}" for p in sorted(product) if p not in packages]
    bad += [f"SPLIT_GROWS\t{p}\t{len(rs)}>{split.get(p, 1)}\t{' '.join(sorted(rs))}"
            for p, rs in sorted(product.items()) if len(rs) > split.get(p, 1)]
    return bad, kinds, product


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("mode", choices=("baseline", "check"))
    ap.add_argument("--tree", default="HEAD")
    ap.add_argument("--paths")
    ap.add_argument("--baseline", default=str(HERE / "PACKAGE_BASELINE.tsv"))
    args = ap.parse_args()
    paths = read_paths(args)
    if args.mode == "baseline":
        print("\n".join(baseline_rows(paths)))
        return 0
    bad, kinds, product = check(paths, Path(args.baseline).read_text(encoding="utf-8"))
    for row in bad:
        print(row)
    counts = collections.Counter(kinds.values())
    print(f"PACKAGE_LAYOUT_GATE violations={len(bad)} roots={len(kinds)} real={counts['REAL']} loose={counts['LOOSE']} "
          f"copy={counts['COPY']} vendor={counts['VENDOR']} product_packages={len(product)} "
          f"split_packages={sum(1 for rs in product.values() if len(rs) > 1)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
