#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Re-checks the ALREADY and PORT probes of census1-remainder-20261107a/ROUTE.tsv against a revision of this repository.

Each probe is a path that must exist in the tree (git ls-tree) and, where given, a text the blob must contain or a
blob sha the path must have. A FAIL means master moved under a decision (an owner renamed or removed), and that row
needs a new decision. Read-only; a blobless clone is enough (one cat-file per text probe).

usage: python -I verify.py [--root DIR] [--rev REV]      (defaults: the repository of this file, HEAD)
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

# leaf, path, kind (EXISTS | CONTAINS | BLOB), argument
PROBES = (
    ("L002", "test/jdk/java/util/ArrayList/M3ArrayListInlineMaskTest.java", "EXISTS", ""),
    ("L005", "m3/fs/src/main/java/jdk/m3/fs/M3FsPrecomputeBundle.java", "CONTAINS", "if (candidates == 0L) break;"),
    ("L014", "m3/algorithms/src/main/java/jdk/m3/algorithm/M3MinCostFlow.java", "EXISTS", ""),
    ("L014", "m3/algorithms/recipe/mincost-flow-20261106/PROVENANCE.tsv", "CONTAINS", "afa706282c5f040338bde3afd138f904e33ae393"),
    ("L014", "m3/algorithms/recipe/mincost-flow-20261106/PROVENANCE.tsv", "CONTAINS", "1f0dcde31649beaa350b5ade15566910092180c0"),
    ("L017", "m3/collections/src/main/java/jdk/m3/collections/M3MatRoaringIntSet.java", "EXISTS", ""),
    ("L017", "m3/collections/src/main/java/jdk/m3/collections/M3MatRankSelectBitVector.java", "EXISTS", ""),
    ("L037", "m3/collections/src/main/java/jdk/m3/collections/M3DoopMaskedWindowScan.java", "EXISTS", ""),
    ("L043", "m3/algorithms/recipe/catalogue-kernels-20261107/HANDOFF_4D.tsv", "CONTAINS", "M3ProblemShapeCatalogue"),
    ("L045", "m3/collections/src/main/java/jdk/m3/collections/M3MemoryPressure.java", "EXISTS", ""),
    ("L072", "m3/collections/src/test/java/jdk/m3/collections/M3FixedDecimal64Test.java", "EXISTS", ""),
    ("L073", "m3/collections/src/test/java/jdk/m3/collections/M3Unsigned128Test.java", "EXISTS", ""),
    ("L077", "m3/collections/src/main/java/jdk/m3/collections/M3TreeSet.java", "CONTAINS", "indexedOwner"),
    ("L040", "m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java", "EXISTS", ""),
    ("L041", "m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Utf16Glob.java", "EXISTS", ""),
    ("L048", "m3/compiler/nullness-facts/src/main/java/jdk/m3/compiler/nullness/M3JavaNullnessFlow.java", "EXISTS", ""),
    ("L008", "m3/optane/src/main/java/jdk/m3/optane/M3PmemRegion.java", "EXISTS", ""),
    ("L064", "m3/vendor/third_party/rewrite-java-21/src/main/java/org/openrewrite/java/isolated/ReloadableJava21JavadocVisitor.java", "EXISTS", ""),
)


def git(root: Path, *args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["git", "-C", str(root), *args], capture_output=True)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(Path(__file__).resolve().parents[3]))
    parser.add_argument("--rev", default="HEAD")
    args = parser.parse_args(argv)
    root = Path(args.root)
    failures = 0
    for leaf, path, kind, argument in PROBES:
        listed = git(root, "ls-tree", args.rev, "--", path).stdout.decode().split()
        if len(listed) < 3:
            print("FAIL %s missing %s" % (leaf, path))
            failures += 1
            continue
        if kind == "CONTAINS":
            blob = git(root, "cat-file", "blob", listed[2]).stdout
            if argument.encode() not in blob:
                print("FAIL %s %s does not contain %r" % (leaf, path, argument))
                failures += 1
                continue
        elif kind == "BLOB" and listed[2] != argument:
            print("FAIL %s %s is blob %s, pinned %s" % (leaf, path, listed[2], argument))
            failures += 1
            continue
        print("PASS %s %s %s" % (leaf, kind, path))
    print("VERIFY rev=%s probes=%d failures=%d" % (args.rev, len(PROBES), failures))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
