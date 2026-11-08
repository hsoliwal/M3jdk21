# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_e_bytecode.py (CURATE_E, derived by make_curate_e_bytecode.py) <base classes> <curated classes> <RENAME_MAP.tsv> <STAY_CORE.txt> <out-dir>

Bytecode proof of curation B+C: every class file of the base build (jdk/m3/collections and jdk/internal/m3/collections)
must be javap -v -p -c -constants identical to its curated counterpart once every renamed type is mapped back to its old
FQN (slash, dot and $-forms, longest names first). Line-number / local-variable tables are dropped (moved imports shift
lines); the constant pool is compared order-independently (javap_v_equal.py). Writes BYTECODE.tsv; exit 1 on any
difference or missing class."""
import os
import re
import subprocess
import sys

base, cur, mapf, stayf, out = sys.argv[1:6]
H = os.path.dirname(os.path.abspath(__file__))
JAVAP = r"C:\Program Files\Java\jdk-21\bin\javap.exe"
CORE = os.environ.get("CURATE_CORE", "jdk.m3.collections")  # CURATE_E
stay = set(l.strip() for l in open(stayf, encoding="utf-8") if l.strip())
ren = {}
for l in open(mapf, encoding="utf-8"):
    c = l.rstrip("\n").split("\t")
    if len(c) > 1 and c[0].startswith(CORE + ".") and c[0] != c[1]:
        s = c[0][len(CORE) + 1:]
        p, n = c[1].rsplit(".", 1)
        if s in stay:
            p = CORE
        if (p, n) != (CORE, s):
            ren[s] = (p, n)
pairs = sorted(((f"{p}.{n}", f"{CORE}.{s}") for s, (p, n) in ren.items()), key=lambda x: -len(x[0]))
files_map = {f"{n}.java": f"{s}.java" for s, (p, n) in ren.items()}
os.makedirs(out, exist_ok=True)
drop = re.compile(r"= Utf8\s+[A-Za-z0-9_$]+\.java\s*$|^(Classfile |  Last modified|  (MD5|SHA-256) checksum|  Compiled from)|SourceFile:|^\s+line \d+: \d+\s*$|"
                  r"LineNumberTable:|LocalVariableTable:|LocalVariableTypeTable:|^\s+\d+\s+\d+\s+\d+\s+\S+\s+\S+\s*$")


_FORMS = {}
for _new, _old in pairs:
    for _a, _b in ((_new.replace(".", "/"), _old.replace(".", "/")), (_new, _old), ("$" + _new.replace(".", "$"), "$" + _old.replace(".", "$"))):
        _FORMS.setdefault(_a, _b)
_RX = re.compile("(?:" + "|".join(re.escape(k) for k in sorted(_FORMS, key=len, reverse=True)) + r")(?![A-Za-z0-9_])") if _FORMS else None


def norm(t):
    if _RX:
        t = _RX.sub(lambda m: _FORMS[m.group(0)], t)
    # source-file names follow the rename (constant-pool Utf8 of the SourceFile attribute)
    t = re.sub(r"(?<![A-Za-z0-9_$])([A-Za-z0-9_]+\.java)(?![A-Za-z0-9_])", lambda m: files_map.get(m.group(1), m.group(1)), t)
    return "\n".join(l for l in t.splitlines() if not drop.search(l)) + "\n"


def dump(p):
    return norm(subprocess.run([JAVAP, "-v", "-p", "-c", "-constants", p], capture_output=True, text=True,
                               encoding="utf-8", errors="replace").stdout)


rows, bad = [], 0
for sub in (os.path.join("jdk", "m3", "collections"), os.path.join("jdk", "internal", "m3", "collections")):
    d = os.path.join(base, sub)
    if not os.path.isdir(d):
        continue
    for f in sorted(os.listdir(d)):
        if not f.endswith(".class"):
            continue
        top, rest = (f[:-6].split("$", 1) + [""])[:2]
        if sub == os.path.join(*CORE.split(".")) and top in ren:
            p, n = ren[top]
            g = os.path.join(cur, *p.split("."), n + ("$" + rest if rest else "") + ".class")
        else:
            g = os.path.join(cur, sub, f)
        if not os.path.exists(g):
            rows.append(f"{sub}/{f}\tMISSING_AFTER"); bad += 1; continue
        a, b = os.path.join(out, "a.txt"), os.path.join(out, "b.txt")
        open(a, "w", encoding="utf-8").write(dump(os.path.join(d, f)))
        open(b, "w", encoding="utf-8").write(dump(g))
        r = subprocess.run([sys.executable, "-I", os.path.join(H, "javap_v_equal.py"), a, b], capture_output=True, text=True)
        if r.returncode:
            bad += 1
            rows.append(f"{sub}/{f}\tDIFFERENT\t{(r.stdout.splitlines() + ['', ''])[1][:150]}")
        else:
            rows.append(f"{sub}/{f}\tEQUAL")
open(os.path.join(out, "BYTECODE.tsv"), "w", encoding="utf-8", newline="\n").write("class_file\tverdict\tnote\n" + "\n".join(rows) + "\n")
print(f"class files {len(rows)}: EQUAL {sum(1 for r in rows if r.endswith(chr(9) + 'EQUAL'))}, problems {bad}")
sys.exit(1 if bad else 0)
