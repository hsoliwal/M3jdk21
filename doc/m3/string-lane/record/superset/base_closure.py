# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""base_closure.py <donors-dir> <out-dir>

JAVA_BASE_CLOSURE v1 (2026-11-17; owner decision 36: the engine atoms go into java.base). java.base requires no other
module, so a donor file can enter java.base only when everything it imports is in java.base itself or in donor files
that can enter too. CPU only: reads the import declarations (and fully qualified uses of the non-base JDK packages
listed below) of every donor file under <donors-dir>/src, and computes the fixpoint:

  BASE      imports only java.base packages and other BASE donor files
  NEEDS     imports a JDK package outside java.base (module named) or an outside library (package named), directly or
            through a donor file that does
Writes BASE_CLOSURE.tsv (donor, file, status, direct blockers, first blocking chain) and BASE_SUMMARY.tsv (per donor:
files, BASE, NEEDS, blockers by module/library)."""
import collections
import os
import re
import sys

DONORS, OUT = sys.argv[1], sys.argv[2]
BASE = ("java.lang", "java.io", "java.math", "java.net", "java.nio", "java.security", "java.text", "java.time",
        "java.util", "javax.crypto", "javax.net", "javax.security.auth", "javax.security.cert")
NOT_BASE = {"java.util.logging": "java.logging", "java.util.prefs": "java.prefs", "java.beans": "java.desktop",
            "java.awt": "java.desktop", "javax.swing": "java.desktop", "javax.imageio": "java.desktop",
            "java.sql": "java.sql", "javax.sql": "java.sql", "javax.xml": "java.xml", "org.w3c.dom": "java.xml",
            "org.xml.sax": "java.xml", "javax.script": "java.scripting", "javax.naming": "java.naming",
            "java.lang.management": "java.management", "javax.management": "java.management",
            "java.lang.instrument": "java.instrument", "java.rmi": "java.rmi", "javax.annotation.processing":
            "java.compiler", "javax.lang.model": "java.compiler", "javax.tools": "java.compiler",
            "java.net.http": "java.net.http", "sun.misc": "jdk.unsupported"}
IMPORT = re.compile(r"^\s*import\s+(static\s+)?([\w.]+)(?:\.\*)?\s*;", re.M)
PACKAGE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)


def classify(name):
    """('base'|'jdk'|'lib', label) for an imported name."""
    for p, module in NOT_BASE.items():
        if name == p or name.startswith(p + "."):
            return "jdk", module
    if name.startswith(BASE) and any(name == b or name.startswith(b + ".") for b in BASE):
        return "base", ""
    if name.startswith(("java.", "javax.", "jdk.", "sun.", "com.sun.")):
        return "jdk", name.rsplit(".", 1)[0]
    return "lib", ".".join(name.split(".")[:3])


files = {}
by_package = collections.defaultdict(list)
for donor in sorted(os.listdir(os.path.join(DONORS, "src"))):
    for root, _, names in os.walk(os.path.join(DONORS, "src", donor)):
        for n in names:
            if not n.endswith(".java"):
                continue
            path = os.path.join(root, n)
            text = open(path, encoding="utf-8", errors="replace").read()
            pkg = (PACKAGE.search(text) or [None, ""])[1] if PACKAGE.search(text) else ""
            fqn = (pkg + "." if pkg else "") + n[:-5]
            imports = [m.group(2) for m in IMPORT.finditer(text)]
            for p, module in NOT_BASE.items():
                if re.search(r"\b" + re.escape(p) + r"\.[A-Za-z]", text) and p not in " ".join(imports):
                    imports.append(p + ".FullyQualifiedUse")
            rel = os.path.relpath(path, os.path.join(DONORS, "src", donor)).replace("\\", "/")
            files[fqn] = dict(donor=donor, rel=rel, pkg=pkg, imports=imports)
            by_package[pkg].append(fqn)

donor_packages = set(by_package)


def resolve(name):
    """Donor files an import refers to (a type, a static member, or a package on demand)."""
    if name in files:
        return [name]
    head = name.rsplit(".", 1)[0]
    if head in files:
        return [head]
    if name in donor_packages:
        return by_package[name]
    return []


direct = {}
edges = {}
for fqn, f in files.items():
    blockers, deps = set(), set()
    for imp in f["imports"]:
        targets = resolve(imp)
        if targets:
            deps.update(targets)
            continue
        if imp.startswith(tuple(p + "." for p in donor_packages if p)) or imp in donor_packages:
            continue
        kind, label = classify(imp)
        if kind != "base":
            blockers.add(("JDK:" if kind == "jdk" else "LIB:") + label)
    direct[fqn], edges[fqn] = blockers, deps
needs = {f: (sorted(direct[f])[0], [f]) for f in files if direct[f]}
changed = True
while changed:
    changed = False
    for f in files:
        if f in needs:
            continue
        for d in edges[f]:
            if d in needs:
                needs[f] = (needs[d][0], [f] + needs[d][1][:3])
                changed = True
                break
os.makedirs(OUT, exist_ok=True)
summary = collections.defaultdict(collections.Counter)
blockers_by = collections.defaultdict(collections.Counter)
with open(os.path.join(OUT, "BASE_CLOSURE.tsv"), "w", encoding="utf-8", newline="") as w:
    w.write("donor\tfile\tstatus\tdirect_blockers\tchain\n")
    for fqn in sorted(files, key=lambda x: (files[x]["donor"], files[x]["rel"])):
        f = files[fqn]
        status = "NEEDS" if fqn in needs else "BASE"
        summary[f["donor"]]["files"] += 1
        summary[f["donor"]][status] += 1
        for b in direct[fqn]:
            blockers_by[f["donor"]][b] += 1
        chain = " <- ".join(reversed(needs[fqn][1])) + " : " + needs[fqn][0] if fqn in needs else "-"
        w.write(f"{f['donor']}\t{f['rel']}\t{status}\t{','.join(sorted(direct[fqn])) or '-'}\t{chain}\n")
total = collections.Counter()
with open(os.path.join(OUT, "BASE_SUMMARY.tsv"), "w", encoding="utf-8", newline="") as w:
    w.write("donor\tfiles\tBASE\tNEEDS\ttop_direct_blockers\n")
    for d in sorted(summary):
        total.update(summary[d])
        top = ", ".join(f"{b} {n}" for b, n in blockers_by[d].most_common(6))
        w.write(f"{d}\t{summary[d]['files']}\t{summary[d]['BASE']}\t{summary[d]['NEEDS']}\t{top}\n")
    allb = collections.Counter()
    for c in blockers_by.values():
        allb.update(c)
    w.write(f"TOTAL\t{total['files']}\t{total['BASE']}\t{total['NEEDS']}\t"
            + ", ".join(f"{b} {n}" for b, n in allb.most_common(12)) + "\n")
print(open(os.path.join(OUT, "BASE_SUMMARY.tsv"), encoding="utf-8").read())
