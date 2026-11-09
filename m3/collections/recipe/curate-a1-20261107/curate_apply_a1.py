# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_apply_a1.py <tree root> <a1.txt> [--check]

CURATE_M3_COLLECTIONS phase A1 v1 (user decision 2026-11-07: strict public surface; implementation detail moves to the
non-exported package jdk.internal.m3.collections). Moves the classes of a1.txt (module-local: no other module names
them) from jdk.m3.collections to jdk.internal.m3.collections, keeping simple names, inside <tree>/m3/collections:
  1. a moving main class: file path + package line;
  2. a test of package jdk.m3.collections whose module references are all moving classes moves with them;
  3. every Java file: explicit import, static import, nested import and fully qualified (dot or slash form, code and
     string literals) names of moving classes are retargeted;
  4. same-package references that the split turns into cross-package ones get explicit imports: a file left in
     jdk.m3.collections imports the moving classes it uses, a moved file imports the remaining ones it uses
     (only for names the file actually uses as identifiers, comments and literals stripped);
  5. C/C++ sources: JNI symbols Java_jdk_m3_collections_<X>_ and FindClass paths jdk/m3/collections/<X> of moving
     classes are retargeted.
Every rewrite is a fixed point (a second run changes nothing; --check exits 1 when a run would change a file). Prints
moved files, rewritten files and added imports."""
import os
import re
import sys

root, a1f = sys.argv[1:3]
check = "--check" in sys.argv
MOD = os.path.join(root, "m3", "collections")
OLD, NEW = "jdk.m3.collections", "jdk.internal.m3.collections"
moving = set(l.strip() for l in open(a1f, encoding="utf-8") if l.strip())
main_dir = os.path.join(MOD, "src", "main", "java", "jdk", "m3", "collections")
remaining = {f[:-5] for f in os.listdir(main_dir) if f.endswith(".java")} - moving if os.path.isdir(main_dir) else set()
new_main = os.path.join(MOD, "src", "main", "java", "jdk", "internal", "m3", "collections")
if os.path.isdir(new_main):
    remaining -= {f[:-5] for f in os.listdir(new_main) if f.endswith(".java")}
ident = re.compile(r"\b[A-Z][A-Za-z0-9_]*\b")
# v1.1: compiler-placed tests (tests are not API: each sits in the package where it compiles)
_rd = lambda v: set(l.strip() for l in open(os.environ[v], encoding="utf-8") if l.strip()) if os.environ.get(v) else set()
TEST_MOVE, TEST_STAY = _rd("TEST_MOVE"), _rd("TEST_STAY")
alt ="|".join(sorted(map(re.escape, moving), key=len, reverse=True))
FQN_DOT = re.compile(r"\bjdk\.m3\.collections\.(" + alt + r")(?![A-Za-z0-9_])")
# v1.3: a type descriptor prefix L (Ljdk/m3/collections/X;) is part of the slash form too
FQN_SLASH = re.compile(r"\b(L?)jdk/m3/collections/(" + alt + r")(?![A-Za-z0-9_])")
JNI = re.compile(r"\bJava_jdk_m3_collections_(" + alt + r")_")


def strip(t):
    t = re.sub(r"/\*.*?\*/", " ", t, flags=re.S)
    t = re.sub(r"//[^\n]*", " ", t)
    return re.sub(r'"(?:\\.|[^"\\\n])*"', '""', t)


def eol_of(t):
    return "\r\n" if "\r\n" in t else "\n"


def add_imports(t, fqns):
    have = set(re.findall(r"^\s*import\s+([\w.]+)\s*;", t, re.M))
    need = sorted(f for f in fqns if f not in have)
    if not need:
        return t, 0
    e = eol_of(t)
    block = "".join(f"import {f};{e}" for f in need)
    m = list(re.finditer(r"^\s*import\s[^;]+;[ \t]*\r?\n", t, re.M))
    if m:
        pos = m[-1].end()
    else:
        pk = re.search(r"^\s*package\s[^;]+;[ \t]*\r?\n", t, re.M)
        pos = pk.end() if pk else 0
        block = e + block
    return t[:pos] + block + t[pos:], len(need)


def java_files():
    for base, _, files in os.walk(MOD):
        if os.sep + "target" + os.sep in base + os.sep:
            continue
        for f in files:
            if f.endswith(".java"):
                yield os.path.join(base, f)


moves, rewrites, imports = [], 0, 0
plan = []
for p in java_files():
    t = open(p, encoding="utf-8", newline="").read()
    pk = re.search(r"^\s*package\s+([\w.]+)\s*;", t, re.M)
    pkg = pk.group(1) if pk else ""
    name = os.path.basename(p)[:-5]
    is_test = os.sep + "src" + os.sep + "test" + os.sep in p
    used = set(ident.findall(strip(t)))
    target_pkg = pkg
    if pkg == OLD:
        if not is_test and name in moving:
            target_pkg = NEW
        elif is_test:
            mods = used & (moving | remaining)
            if name in TEST_MOVE or (mods and mods <= moving and name not in TEST_STAY):
                target_pkg = NEW
    plan.append((p, t, pkg, target_pkg, used, is_test))
# v1.2: test classes are names too (helpers, contracts, proofs): imports follow where each test was placed
_tname = lambda p: os.path.basename(p)[:-5]
moved_tests = {_tname(p) for p, _, pkg, tp, _, it in plan if it and pkg == OLD and tp == NEW}
stay_tests = {_tname(p) for p, _, pkg, tp, _, it in plan if it and pkg == OLD and tp == OLD}
for p, t, pkg, target_pkg, used, is_test in plan:
    n = t
    n = re.sub(r"(?m)^(\s*import\s+(?:static\s+)?)jdk\.m3\.collections\.(" + alt + r")(?=[.;\s])", r"\1" + NEW + r".\2", n)
    n = FQN_DOT.sub(NEW + r".\1", n)
    n = FQN_SLASH.sub(r"\1jdk/internal/m3/collections/\2", n)
    if target_pkg != pkg:
        n = re.sub(r"(?m)^(\s*package\s+)" + re.escape(pkg) + r"(\s*;)", r"\1" + target_pkg + r"\2", n, count=1)
    added = 0
    if target_pkg == OLD:
        n, added = add_imports(n, {f"{NEW}.{x}" for x in used & (moving | moved_tests) if x != _tname(p)})
    elif target_pkg == NEW and pkg == OLD:
        n, added = add_imports(n, {f"{OLD}.{x}" for x in used & (remaining | stay_tests) if x != _tname(p)})
    elif pkg not in (OLD, NEW) and re.search(r"^\s*import\s+jdk\.m3\.collections\.\*\s*;", n, re.M) and used & moving:
        n, added = add_imports(n, {f"{NEW}.*"})
    imports += added
    dest = p
    if target_pkg != pkg:
        rel = os.path.relpath(p, MOD)
        dest = os.path.join(MOD, rel.replace(os.path.join("jdk", "m3", "collections") + os.sep,
                                             os.path.join("jdk", "internal", "m3", "collections") + os.sep, 1))
        moves.append((p, dest))
    if n != t or dest != p:
        rewrites += 1
        if not check:
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            open(dest, "w", encoding="utf-8", newline="").write(n)
            if dest != p:
                os.remove(p)
for base, _, files in os.walk(MOD):
    for f in files:
        if f.endswith((".c", ".cc", ".cpp", ".h")):
            p = os.path.join(base, f)
            t = open(p, encoding="utf-8", errors="surrogateescape", newline="").read()
            n = JNI.sub(r"Java_jdk_internal_m3_collections_\1_", t)
            n = FQN_SLASH.sub(r"\1jdk/internal/m3/collections/\2", n)
            if n != t:
                rewrites += 1
                if not check:
                    open(p, "w", encoding="utf-8", errors="surrogateescape", newline="").write(n)
print(f"CURATE_A1 moving={len(moving)} moved_files={len(moves)} rewritten_files={rewrites} added_imports={imports}"
      f"{' (check)' if check else ''}")
sys.exit(1 if check and rewrites else 0)
