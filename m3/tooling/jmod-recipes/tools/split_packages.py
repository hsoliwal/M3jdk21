#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""JPMS split-package audit over the vendored jars (JMOD_IMAGE.tsv rows that are JMOD/MODULAR_JAR/NEEDS_DESCRIPTOR):
packages per jar from the release-21 view (META-INF/versions/<=21 overlaid), packages present in more than one jar,
and the merge groups (connected components of jars sharing a package) that must become one named module for jlink.
Writes SPLIT_PACKAGES.tsv (package, jars) and MERGE_GROUPS.tsv (group, jars, shared packages)."""
import argparse, csv, io, sys, zipfile
from collections import defaultdict
from pathlib import Path


def packages(jar):
    out = set()
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if not n.endswith(".class") or n.endswith("module-info.class"):
                continue
            if n.startswith("META-INF/versions/"):
                parts = n.split("/")
                if not parts[2].isdigit() or int(parts[2]) > 21:
                    continue
                n = "/".join(parts[3:])
            elif n.startswith("META-INF/"):
                continue
            if "/" in n:
                out.add(n.rsplit("/", 1)[0].replace("/", "."))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--image-tsv", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    a = ap.parse_args()
    rows = [r for r in csv.DictReader(io.open(a.image_tsv, encoding="utf-8", newline=""), delimiter="\t")
            if r["disposition"].startswith(("JMOD", "MODULAR_JAR", "NEEDS_DESCRIPTOR"))]
    owners = defaultdict(set)
    for r in rows:
        for p in packages(r["jar"]):
            owners[p].add(r["artifact"] + ":" + r["version"])
    split = {p: j for p, j in owners.items() if len(j) > 1}
    parent = {}

    def find(x):
        while parent.setdefault(x, x) != x:
            parent[x] = parent[parent[x]]; x = parent[x]
        return x
    for jars in split.values():
        js = sorted(jars)
        for j in js[1:]:
            parent[find(j)] = find(js[0])
    groups = defaultdict(set)
    for jars in split.values():
        for j in jars:
            groups[find(j)].add(j)
    a.out.mkdir(parents=True, exist_ok=True)
    with io.open(a.out / "SPLIT_PACKAGES.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("package\tjars\n")
        for p in sorted(split):
            f.write("%s\t%s\n" % (p, ",".join(sorted(split[p]))))
    with io.open(a.out / "MERGE_GROUPS.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("group\tjars\tshared_packages\n")
        for i, (root, jars) in enumerate(sorted(groups.items(), key=lambda kv: sorted(kv[1])[0])):
            shared = sorted(p for p, j in split.items() if j <= jars)
            f.write("G%02d\t%s\t%s\n" % (i, ",".join(sorted(jars)), ",".join(shared)))
    print("jars", len(rows), "packages", len(owners), "split packages", len(split), "merge groups", len(groups))
    return 0


if __name__ == "__main__":
    sys.exit(main())
