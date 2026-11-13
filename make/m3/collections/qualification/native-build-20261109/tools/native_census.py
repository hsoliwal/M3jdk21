# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""native_census.py <repo root> <out NATIVE_CENSUS.tsv>

NATIVE_CENSUS v1.2 (2026-11-09) is a method-level JNI binding census of src/jdk.m3.collections. For every declared
`native` method of every class it records the expected short JNI symbol, Java_<mangled FQN>_<mangled method>. It then
checks the C sources under src/jdk.m3.collections/share/native and m3/ (excluding evidence, qualification, vendor and
docs trees) and assigns one status per method:

  BOUND     the exact symbol, or its overload-mangled long form, is exported;
  STALE     a symbol with the same method name exists under another class prefix that ends with the same simple name,
            or with the simple name minus an M3 prefix (a class renamed by curation whose C was not updated);
  ABSENT    no symbol for the method exists.

The scan reads declarations with a regex (`native` modifier through the method name), and reads C symbols as
`Java_<...>`. RegisterNatives tables are reported separately."""
import os
import re
import subprocess
import sys

root, out = sys.argv[1:3]


def mangle(s):
    return s.replace("_", "_1").replace("$", "_00024")


C_SKIP = re.compile(r"/(evidence|qualification|vendor|docs|recipe|snapshots)/")
files = subprocess.run(["git", "-C", root, "ls-files", "src/jdk.m3.collections/share/native", "m3"], capture_output=True,
                       text=True).stdout.split()
syms, register, templates = set(), set(), []
for f in files:
    if not f.endswith((".c", ".cc", ".cpp", ".h")) or C_SKIP.search("/" + f):
        continue
    t = open(os.path.join(root, f), encoding="utf-8", errors="replace").read()
    syms |= set(re.findall(r"\bJava_[A-Za-z0-9_]+", t))
    # v1.2: token-pasting macros (Java_<owner>_bounds##NAME##0) export one symbol per instantiation.
    templates += [re.compile(re.escape(head) + r"[A-Z][A-Za-z]*" + re.escape(tail) + "$")
                  for head, tail in re.findall(r"\b(Java_[A-Za-z0-9_]+?)##\w+##([A-Za-z0-9_]*)", t)]
    if "RegisterNatives" in t:
        register.add(f)
NAT = re.compile(r"\bnative\s+[\w<>\[\], .?]+?\s+(\w+)\s*\(")
CLS = re.compile(r"\b(?:class|interface|enum|record)\s+(\w+)[^{;]*\{")


def class_spans(text):
    """(name, start, end) of every class body; text has comments and literals blanked, so braces balance."""
    spans = []
    for m in CLS.finditer(text):
        depth, i = 0, m.end() - 1
        while i < len(text):
            depth += {"{": 1, "}": -1}.get(text[i], 0)
            if depth == 0:
                break
            i += 1
        spans.append((m.group(1), m.end(), i))
    return spans


def binary_name(package, spans, pos):
    """v1.1: natives of nested classes belong to Outer$Inner (the JNI symbol mangles $ as _00024)."""
    chain = [name for name, start, end in sorted(spans, key=lambda s: s[1]) if start <= pos <= end]
    return package + "." + "$".join(chain)


rows = []
base = os.path.join(root, "src", "jdk.m3.collections", "share", "classes")
for d, _, fs in os.walk(base):
    for f in fs:
        if not f.endswith(".java"):
            continue
        p = os.path.join(d, f)
        t = open(p, encoding="utf-8", errors="replace").read()
        if not re.search(r"\bnative\b", t):
            continue
        top = os.path.relpath(p, base)[:-5].replace(os.sep, ".")
        package = top.rsplit(".", 1)[0]
        code = re.sub(r"/\*.*?\*/|//[^\n]*", " ", t, flags=re.S)
        code = re.sub(r'"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])\'', '""', code)
        spans = class_spans(code)
        for m in NAT.finditer(code):
            name = m.group(1)
            fqn = binary_name(package, spans, m.start())
            simple = fqn.rsplit(".", 1)[1].rsplit("$", 1)[-1]
            short = "Java_" + mangle(fqn).replace(".", "_") + "_" + mangle(name)
            if short in syms or any(s.startswith(short + "__") for s in syms) or any(x.match(short) for x in templates):
                status, other = "BOUND", ""
            else:
                cands = sorted(s for s in syms if s.endswith("_" + mangle(name)) or ("_" + mangle(name) + "__") in s)
                stale = [s for s in cands if re.search(r"_(M3)?" + re.escape(simple) + "_" + re.escape(mangle(name)) + r"(__|$)", s)
                         or re.search(r"_" + re.escape(simple[2:] if simple.startswith("M3") else simple) + "_", s)]
                status, other = ("STALE", stale[0]) if stale else ("ABSENT", "")
            rows.append((fqn, name, status, short, other or "-"))
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("class\tmethod\tstatus\texpected_symbol\tstale_symbol\n")
    for r in sorted(rows):
        w.write("\t".join(r) + "\n")
from collections import Counter
print("NATIVE_CENSUS methods %d: %s; RegisterNatives files %d" % (
    len(rows), ", ".join(f"{k} {v}" for k, v in sorted(Counter(r[2] for r in rows).items())), len(register)))
