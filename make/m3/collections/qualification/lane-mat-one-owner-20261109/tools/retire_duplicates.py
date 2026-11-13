# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""retire_duplicates.py <RETIRE.tsv> <repo root>

RETIRE_DUPLICATE_OWNER v1.1 (2026-11-09). Each TSV row is: duplicate FQN, surviving owner FQN, evidence. A duplicate
must be a class whose bytecode equals the survivor's modulo names; that is checked beforehand (EVIDENCE).

The duplicate's source file is deleted. Every Java file under src/jdk.m3.collections/share/classes and test/jdk/jdk
that can name a duplicate is rewritten. A file can name it when it is in the duplicate's package, imports it, or
spells its FQN. In such a file:
- an import of the duplicate becomes an import of the survivor;
- a file in the duplicate's package gets an import of the survivor;
- the FQN and the simple name become the survivor's, also inside comments, so {@link} references stay valid.
Lines that carry provenance (lineage, Synexia, absorbed, blob, RELOCATE, receipt) are left as they were, because they
name the donor class as it was. Prints one line per touched file and a total."""
import os
import re
import sys

tsv, root = sys.argv[1:3]
PROVENANCE = re.compile(r"Lineage|lineage|Synexia|absorbed|blob [0-9a-f]|RELOCATE|receipt")
rows = [line.rstrip("\n").split("\t") for line in open(tsv, encoding="utf-8")
        if line.strip() and not line.startswith("duplicate")]
pairs = [(row[0], row[1]) for row in rows]
src = os.path.join(root, "src", "jdk.m3.collections", "share", "classes")
tests = os.path.join(root, "test", "jdk", "jdk")
for dup, _ in pairs:
    os.remove(os.path.join(src, *dup.split(".")) + ".java")
touched = 0
for base in (src, tests):
    for d, _, fs in os.walk(base):
        for f in fs:
            if not f.endswith(".java"):
                continue
            p = os.path.join(d, f)
            text = open(p, encoding="utf-8").read()
            new = text
            found = re.search(r"^package\s+([\w.]+);", new, re.M)
            package = found.group(1) if found else ""
            imports = []
            for dup, surv in pairs:
                dpkg, dsim = dup.rsplit(".", 1)
                spkg, ssim = surv.rsplit(".", 1)
                import_line = re.compile(r"^import\s+" + re.escape(dup) + r";", re.M)
                imported = import_line.search(new)
                if not (package == dpkg or imported or dup in new):
                    continue
                if not re.search(r"\b" + dsim + r"\b", new):
                    continue
                if imported:
                    new = import_line.sub("import " + surv + ";", new)
                elif package == dpkg and package != spkg:
                    imports.append("import " + surv + ";")
                lines = new.split("\n")
                for i, line in enumerate(lines):
                    if PROVENANCE.search(line) or line.startswith("import "):
                        continue
                    line = re.sub(r"\b" + re.escape(dup) + r"\b", surv, line)
                    lines[i] = re.sub(r"(?<![\w.])" + dsim + r"\b", ssim, line)
                new = "\n".join(lines)
            block = "\n".join(sorted(set(i for i in imports if i not in new)))
            if block:
                m = re.search(r"^package\s+[\w.]+;\n", new, re.M)
                new = new[:m.end()] + "\n" + block + "\n" + new[m.end():]
            if new != text:
                with open(p, "w", encoding="utf-8", newline="\n") as w:
                    w.write(new)
                touched += 1
                print("RETIRE_DUPLICATE_OWNER", os.path.relpath(p, root).replace(os.sep, "/"))
print("RETIRE_DUPLICATE_OWNER deleted", len(pairs), "touched", touched)
