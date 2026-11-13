#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Self-tests of package_gate.py on synthetic trees: unchanged passes, removals pass, and each kind of sprawl fails."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import package_gate as g  # noqa: E402

BASE = [
    "m3/search/pom.xml", "m3/search/src/main/java/jdk/m3/search/Kmp.java",
    "m3/arrays/pom.xml", "m3/arrays/src/main/java/jdk/m3/arrays/Ints.java",
    "m3/collections/implementations/a/pom.xml", "m3/collections/implementations/a/src/main/java/jdk/m3/collections/apache/A.java",
    "m3/collections/implementations/b/pom.xml", "m3/collections/implementations/b/src/main/java/jdk/m3/collections/apache/B.java",
    "m3/search/qualification/x/recipe/src/main/java/jdk/m3/search/Copy.java",
    "m3/vendor/third_party/lib-1.0/src/main/java/org/lib/L.java",
    "src/java.base/share/classes/jdk/internal/m3/zip/M3Codec.java",
]
baseline = "\n".join(g.baseline_rows(BASE))
checks = 0


def expect(paths, kinds, label):
    global checks
    bad, _, _ = g.check(paths, baseline)
    got = sorted(row.split("\t")[0] for row in bad)
    assert got == sorted(kinds), f"{label}: {got}"
    checks += 1


expect(BASE, [], "unchanged tree")
expect([p for p in BASE if "/arrays/" not in p], [], "removed module")
expect(BASE + ["m3/search/src/main/java/jdk/m3/search/Bmh.java"], [], "new class in an existing package")
expect(BASE + ["make/m3/tooling/search/pom.xml", "make/m3/tooling/search/src/main/java/jdk/m3/tooling/search/S.java"],
       ["NEW_ROOT", "NEW_PACKAGE"], "new module with a new package")
expect(BASE + ["m3/search/src/main/java/jdk/m3/search/sorted/S.java"], ["NEW_PACKAGE"], "new package in an existing root")
expect(BASE + ["make/m3/tooling/x/pom.xml", "make/m3/tooling/x/src/main/java/jdk/m3/search/S.java"],
       ["NEW_ROOT", "SPLIT_GROWS"], "existing package split into a new root")
expect(BASE + ["m3/collections/implementations/c/pom.xml",
               "m3/collections/implementations/c/src/main/java/jdk/m3/collections/apache/C.java"],
       ["NEW_ROOT", "SPLIT_GROWS"], "an existing split grows")
expect(BASE + ["m3/vendor/third_party/lib-2.0/src/main/java/org/lib/L.java"], ["NEW_ROOT"], "new vendored version")
expect(BASE + ["m3/arrays/qualification/y/recipe/src/main/java/jdk/m3/zzz/Copy.java"], ["NEW_ROOT"],
       "new evidence copy root (a root, not a product package)")
expect(BASE + ["m3/search/src/main/java/jdk/m3/search/M3Bmh.java"], ["NEW_PREFIXED_TYPE"], "new M3-prefixed type")
expect(BASE + ["test/jdk/jdk/internal/m3/zip/M3CodecTest.java"], ["NEW_PREFIXED_TYPE"], "new prefixed test type")
expect([p for p in BASE if not p.endswith("M3Codec.java")]
       + ["src/java.base/share/classes/jdk/internal/m3/zip/Codec.java"], [], "legacy prefixed type renamed")
expect(BASE + ["src/java.base/share/classes/java/lang/M3StringFacts.java"], [], "M3 prefix outside M3 packages kept")
expect(BASE + ["m3/search/src/main/java/jdk/m3/search/recipe/M3Agent.java"], ["NEW_PACKAGE", "NEW_PREFIXED_TYPE"],
       "prefixed type in a package named like an evidence segment")
expect(BASE + ["m3/search/qualification/x/recipe/src/main/java/jdk/m3/search/M3Copy.java"], [],
       "prefixed type in an evidence copy root")
print(f"PACKAGE_LAYOUT_GATE_SELF_TEST checks={checks} PASS")
