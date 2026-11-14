# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""native_libmap.py <repo root> <NATIVE_CENSUS.tsv> <out NATIVE_LIBS.tsv>

NATIVE_LIBMAP v1 (2026-11-09) answers, for every jdk.m3.collections class with native methods: which library does it
load, and which M3 C files export its symbols? The library comes from System.loadLibrary("<name>"), or from the system
property passed to System.load. The C files are the files under src/jdk.m3.collections/share/native that define any
Java_<mangled class>_ symbol. The output has one row per class: class, library, C files, bound/total methods."""
import os
import re
import sys
from collections import defaultdict

root, census, out = sys.argv[1:4]
native_dir = os.path.join(root, "src", "jdk.m3.collections", "share", "native")
by_prefix = defaultdict(set)
for d, _, fs in os.walk(native_dir):
    for f in fs:
        if f.endswith((".c", ".cc", ".cpp")):
            p = os.path.join(d, f)
            for s in set(re.findall(r"\bJava_([A-Za-z0-9_]+?)_[a-z][A-Za-z0-9]*(?:__|\b)", open(p, encoding="utf-8", errors="replace").read())):
                by_prefix[s].add(os.path.relpath(p, native_dir).replace(os.sep, "/"))
rows = [l.rstrip("\n").split("\t") for l in list(open(census, encoding="utf-8"))[1:]]
classes = defaultdict(lambda: [0, 0])
for c, m, st, exp, _ in rows:
    classes[c][1] += 1
    classes[c][0] += st == "BOUND"
base = os.path.join(root, "src", "jdk.m3.collections", "share", "classes")
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("class\tlibrary\tc_files\tbound\ttotal\n")
    for c, (b, n) in sorted(classes.items()):
        src = open(os.path.join(base, *c.split("$")[0].split(".")) + ".java", encoding="utf-8", errors="replace").read()
        lib = re.findall(r'loadLibrary\("([^"]+)"\)', src)
        prop = re.findall(r'getProperty\("([^"]*native[^"]*)"', src)
        mangled = c.replace("_", "_1").replace("$", "_00024").replace(".", "_")
        cf = sorted(by_prefix.get(mangled, set()))
        w.write(f"{c}\t{','.join(sorted(set(lib))) or ('prop:' + ','.join(prop) if prop else '?')}\t{' '.join(cf) or '-'}\t{b}\t{n}\n")
print("NATIVE_LIBMAP classes", len(classes))
