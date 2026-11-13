# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_a1_bytecode.py <base classes dir> <curated classes dir> <out-dir>

Bytecode proof of phase A1: every class file of the base build (package jdk.m3.collections) must be javap -v -p -c
-constants identical to its curated counterpart (jdk.m3.collections or jdk.internal.m3.collections) once
jdk/internal/m3/collections and jdk.internal.m3.collections are mapped back to the old package. Line-number and
local-variable tables are dropped (added import lines shift line numbers; locals are unchanged code). Constant pool is
compared as an order-independent multiset (javap_v_equal.py). Writes BYTECODE.tsv; exit 1 on any difference or a
class missing on either side."""
import os
import re
import subprocess
import sys

base, cur, out = sys.argv[1:4]
H = os.path.dirname(os.path.abspath(__file__))
JAVAP = r"C:\Program Files\Java\jdk-21\bin\javap.exe"
os.makedirs(out, exist_ok=True)
old_dir = os.path.join(base, "jdk", "m3", "collections")
cur_old = os.path.join(cur, "jdk", "m3", "collections")
cur_new = os.path.join(cur, "jdk", "internal", "m3", "collections")
base_files = sorted(f for f in os.listdir(old_dir) if f.endswith(".class"))
drop = re.compile(r"^(Classfile |  Last modified|  (MD5|SHA-256) checksum|  Compiled from)|SourceFile:|^\s+line \d+: \d+\s*$|"
                  r"LineNumberTable:|LocalVariableTable:|LocalVariableTypeTable:|^\s+\d+\s+\d+\s+\d+\s+\S+\s+\S+\s*$")


def dump(path):
    t = subprocess.run([JAVAP, "-v", "-p", "-c", "-constants", path], capture_output=True, text=True,
                       encoding="utf-8", errors="replace").stdout
    t = t.replace("jdk/internal/m3/collections/", "jdk/m3/collections/").replace("jdk.internal.m3.collections.", "jdk.m3.collections.")
    t = t.replace("jdk/internal/m3/collections", "jdk/m3/collections").replace("jdk.internal.m3.collections", "jdk.m3.collections")
    t = t.replace("$jdk$internal$m3$collections$", "$jdk$m3$collections$")  # synthetic $SwitchMap$<pkg>$ names
    return "\n".join(l for l in t.splitlines() if not drop.search(l)) + "\n"


rows, bad = [], 0
cur_names = set(os.listdir(cur_old)) | (set(os.listdir(cur_new)) if os.path.isdir(cur_new) else set())
for f in base_files:
    g = os.path.join(cur_old, f) if os.path.exists(os.path.join(cur_old, f)) else os.path.join(cur_new, f)
    if not os.path.exists(g):
        rows.append(f"{f}\tMISSING_AFTER"); bad += 1; continue
    a, b = os.path.join(out, "a.txt"), os.path.join(out, "b.txt")
    open(a, "w", encoding="utf-8").write(dump(os.path.join(old_dir, f)))
    open(b, "w", encoding="utf-8").write(dump(g))
    r = subprocess.run([sys.executable, "-I", os.path.join(H, "javap_v_equal.py"), a, b], capture_output=True, text=True)
    where = "moved" if g.startswith(cur_new) else "stayed"
    if r.returncode:
        bad += 1
        rows.append(f"{f}\tDIFFERENT\t{where}\t{r.stdout.splitlines()[1] if len(r.stdout.splitlines()) > 1 else ''}")
    else:
        rows.append(f"{f}\tEQUAL\t{where}")
extra = sorted(cur_names - set(base_files) - {"package-info.class"})
for f in extra:
    if f.endswith(".class"):
        rows.append(f"{f}\tNEW_AFTER"); bad += 1
open(os.path.join(out, "BYTECODE.tsv"), "w", encoding="utf-8", newline="\n").write("class_file\tverdict\twhere\tnote\n" + "\n".join(rows) + "\n")
eq = sum(1 for r in rows if "\tEQUAL\t" in r)
print(f"class files {len(base_files)}: EQUAL {eq}, problems {bad}")
sys.exit(1 if bad else 0)
