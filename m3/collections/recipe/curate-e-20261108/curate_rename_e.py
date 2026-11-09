# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_rename_e.py <tree root> <RENAME_MAP.tsv> [--check]   (CURATE_E v1, derived by make_curate_e.py
from curate_rename_bc.py v1.5; env CURATE_CORE, TESTS_AS_OWNERS; internal packages are never exported)


CURATE_M3_COLLECTIONS phases B+C v1 (user-approved 2026-11-07): every public type of RENAME_MAP.tsv
(old FQN jdk.m3.collections.<S> -> new FQN <P>.<N>) gets its family subpackage P and JDK-style simple name N across the tree
(all Java and C files under <tree>/m3, plus recipe RELOCATION_MAP.tsv files). Rules:
  1. the owner file moves to <P>/<N>.java (package line, declaration via rule 3);
  2. imports (single, static, nested) and fully qualified names (dot, slash, descriptor L..;) of S in code, comments and
     string literals become P.N;
  3. the identifier S becomes N (code and comments, never string-literal text) in every file that sees S: its old package
     jdk.m3.collections, a single-type import of S, or the on-demand import jdk.m3.collections.*;
  4. a reference that the move turns cross-package gets an explicit import (a core file using a subpackaged type, a
     subpackaged file using any other jdk.m3.collections type);
  5. CLASH guard: when N would collide in a file with a simple name already visible there (another single-type import, a
     class of the file's own package, a nested type), that file uses the fully qualified P.N instead and no import;
  6. JNI symbols Java_jdk_m3_collections_<S>_ become the mangled P/N symbol; module-info gains exports P;
  7. RELOCATION_MAP.tsv rows of recipe-owned modules: FQN targets and annotation tokens @S follow (rerun the recipe).
Environment: STAY_CORE (file of S that keep the core package; the compiler fixed point fills it), TEST_MOVE (tests moved
into the subpackage of their class). Fixed point: --check exits 1 when a run would change a file."""
import os
import re
import sys

root, mapf = sys.argv[1:3]
check = "--check" in sys.argv
CORE = os.environ.get("CURATE_CORE", "jdk.m3.collections")  # CURATE_E: the package whose types move/rename
CORE_RE, CORE_SL, CORE_JNI = re.escape(CORE), CORE.replace(".", "/"), CORE.replace(".", "_")
TESTS_AS_OWNERS = os.environ.get("TESTS_AS_OWNERS") == "1"
_rd = lambda v: set(l.strip() for l in open(os.environ[v], encoding="utf-8") if l.strip()) if os.environ.get(v) else set()
STAY, TEST_MOVE = _rd("STAY_CORE"), _rd("TEST_MOVE")
ren = {}
for l in open(mapf, encoding="utf-8"):
    c = l.rstrip("\n").split("\t")
    if len(c) < 2 or not c[0].startswith(CORE + ".") or c[0] == c[1]:
        continue
    s = c[0][len(CORE) + 1:]
    p, n = c[1].rsplit(".", 1)
    if s in STAY:
        p = CORE
    if (p, n) != (CORE, s):
        ren[s] = (p, n)
M3 = os.path.join(root, "m3")
# v1.3: history is never rewritten (qualification records, retained variants, snapshots, history dirs)
SKIP_DIRS = {"target", "vendor", ".git", "qualification", "retained-variants", "snapshots", "history"}
alt = "|".join(sorted(map(re.escape, ren), key=len, reverse=True))
W = r"(?![A-Za-z0-9_$])"
FQN_DOT = re.compile(r"\b" + CORE_RE + r"\.(" + alt + r")" + W)
FQN_SL = re.compile(r"\b(L?)" + re.escape(CORE_SL) + r"/(" + alt + r")" + W)
IDENT = re.compile(r"(?<![A-Za-z0-9_$.])(" + alt + r")" + W)
JNI = re.compile(r"\bJava_" + CORE_JNI + r"_(" + alt + r")_")
LIT = re.compile(r'"(?:\\.|[^"\\\n])*"|"""[\s\S]*?"""')
EXACT = re.compile(r'^"(' + alt + r')"$')
SEGMENT = re.compile(r"(?<=[./$])(" + alt + r")(?=[.$/\"]|$)")


def new_fqn(s):
    p, n = ren[s]
    return f"{p}.{n}"


def code_spans(t):
    """split into (is_literal, text) runs; text blocks and string literals are literal, comments are code here"""
    out, pos = [], 0
    for m in LIT.finditer(t):
        out.append((False, t[pos:m.start()]))
        out.append((True, m.group(0)))
        pos = m.end()
    out.append((False, t[pos:]))
    return out


def java_files():
    for base, dirs, files in os.walk(M3):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for f in files:
            if f.endswith(".java") and os.sep + "resources" + os.sep not in base + os.sep:  # v1.3 resources = data; v1.2: frozen *.java.txt fixtures stay byte-identical (tests pin their digest)
                yield os.path.join(base, f)


# package index: simple class names per package (for the CLASH guard and rule 4)
pkg_classes = {}
files = list(java_files())
info = {}
for p in files:
    t = open(p, encoding="utf-8", errors="surrogateescape", newline="").read()
    pk = re.search(r"^\s*package\s+([\w.]+)\s*;", t, re.M)
    pkg = pk.group(1) if pk else ""
    name = os.path.basename(p)[:-5]
    info[p] = (t, pkg, name)
    pkg_classes.setdefault(pkg, set()).add(name)
core_names = set(pkg_classes.get(CORE, set()))  # v1.1: a snapshot of the OLD core names (the live set gains new names)
# CURATE_E guard: two types may never get one FQN (a STAY_CORE type pulled back into a package that already has its new
# simple name, or two renames onto one target); the run refuses instead of overwriting a file
_eff = {}
for _s, (_p, _n) in ren.items():
    _eff.setdefault((_p, _n), []).append(_s)
for _c in core_names - set(ren):
    _eff.setdefault((CORE, _c), []).append(_c)
_dup = {k: v for k, v in _eff.items() if len(v) > 1}
if _dup:
    raise SystemExit("CURATE_E refuses: duplicate target FQN " + "; ".join(f"{p}.{n} <- {v}" for (p, n), v in sorted(_dup.items())))
# CURATE_E v1.1: effective simple names per package AFTER the run ({pkg: {new name: {old names}}}); two packages may
# now hold one simple name (select.CollectionRequest, shape.CollectionRequest, root CollectionRequest)
eff_names = {}
for (_p, _n), _v in _eff.items():
    eff_names.setdefault(_p, {}).setdefault(_n, set()).update(_v)
moved_ct = {}
for s, (pp, n) in ren.items():
    pkg_classes.setdefault(pp, set()).add(n)
changed, moves, clashes, imports_added = 0, 0, 0, 0
for p in files:
    t, pkg, name = info[p]
    is_test = os.sep + "src" + os.sep + "test" + os.sep in p
    owner = pkg == CORE and (TESTS_AS_OWNERS or not is_test) and name in ren
    tpkg = ren[name][0] if owner else pkg
    if pkg == CORE and is_test and name in TEST_MOVE:
        tpkg = TEST_MOVE_PKG = None
    star = bool(re.search(r"^\s*import\s+" + CORE_RE + r"\.\*\s*;", t, re.M))
    single = set(re.findall(r"^\s*import\s+" + CORE_RE + r"\.(" + alt + r")\s*;", t, re.M)) if ren else set()
    sees = set(ren) if (pkg == CORE or star) else single
    other_imports = {i.rsplit(".", 1)[1] for i in re.findall(r"^\s*import\s+(?!static)([\w.]+)\s*;", t, re.M)
                     if not i.startswith(CORE + ".")}
    nested = set(re.findall(r"\b(?:class|interface|enum|record)\s+([A-Z]\w*)", t))
    visible = other_imports | (nested - {name})
    fq = {s for s in sees if ren[s][0] != tpkg and (ren[s][1] in visible or any(x != s for x in eff_names.get(tpkg, {}).get(ren[s][1], ())))}
    clashes += len(fq)
    # v1.2: lines marked curation-keep-names (declared TEST_EDITS that translate old names) are never rewritten
    keep = {}
    def _protect(m):
        k = f"\u0000KEEP{len(keep)}\u0000"
        keep[k] = m.group(0)
        return k
    n_ = re.sub(r"(?m)^.*curation-keep-names.*$", _protect, t)
    # rule 2: imports
    n_ = re.sub(r"(?m)^(\s*import\s+(?:static\s+)?)" + CORE_RE + r"\.(" + alt + r")(?=[.;\s])",
                lambda m: "" if m.group(2) in fq and "static" not in m.group(1) else m.group(1) + new_fqn(m.group(2)), n_)
    n_ = re.sub(r"(?m)^\s*;\s*\r?\n", "", n_) if n_ != t and re.search(r"(?m)^\s*;\s*$", n_) else n_
    parts = []
    for lit, seg in code_spans(n_):
        seg = FQN_DOT.sub(lambda m: new_fqn(m.group(1)), seg)
        seg = FQN_SL.sub(lambda m: m.group(1) + new_fqn(m.group(2)).replace(".", "/"), seg)
        if lit and sees:
            # v1.1 names as data: a literal that IS an old simple name, or an old name as a ./ $ path/name segment
            seg = EXACT.sub(lambda m: '"' + ren[m.group(1)][1] + '"' if m.group(1) in sees else m.group(0), seg)
            if not re.search(r"\.(txt|tsv|json|properties)\"?$", seg):  # v1.2: data-file names are not type names
                seg = SEGMENT.sub(lambda m: ren[m.group(1)][1] if m.group(1) in sees else m.group(0), seg)
        if not lit:
            seg = IDENT.sub(lambda m: (new_fqn(m.group(1)) if m.group(1) in fq else ren[m.group(1)][1])
                            if m.group(1) in sees else m.group(1), seg)
        parts.append(seg)
    n_ = "".join(parts)
    # rule 4: cross-package references created by the move
    used = set(re.findall(r"\b[A-Z][A-Za-z0-9_]*\b", re.sub(r'"(?:\\.|[^"\\\n])*"', '""', n_)))
    used_old = set(re.findall(r"\b[A-Z][A-Za-z0-9_]*\b", re.sub(r'"(?:\\.|[^"\\\n])*"', '""', t)))
    need = set()
    if pkg == CORE and tpkg == CORE:
        need = {new_fqn(s) for s in ren if ren[s][0] != CORE and s in used_old and s in sees and s not in fq}
    elif pkg == CORE and tpkg != CORE:
        for s, (pp, nn) in ren.items():
            if pp != tpkg and s in used_old and s not in fq:
                need.add(f"{pp}.{nn}")
        need |= {f"{CORE}.{c}" for c in core_names if c not in ren and c in used}
    if need:
        have = set(re.findall(r"^\s*import\s+([\w.]+)\s*;", n_, re.M))
        add = sorted(x for x in need if x not in have and x.rsplit(".", 1)[0] != tpkg)
        if add:
            e = "\r\n" if "\r\n" in n_ else "\n"
            ims = list(re.finditer(r"^\s*import\s[^;]+;[ \t]*\r?\n", n_, re.M))
            pos = ims[-1].end() if ims else re.search(r"^\s*package\s[^;]+;[ \t]*\r?\n", n_, re.M).end()
            n_ = n_[:pos] + ("" if ims else e) + "".join(f"import {x};{e}" for x in add) + n_[pos:]
            imports_added += len(add)
    for k, v in keep.items():
        n_ = n_.replace(k, v)
    dest = p
    if tpkg != pkg:
        n_ = re.sub(r"(?m)^(\s*package\s+)" + re.escape(pkg) + r"(\s*;)", r"\g<1>" + tpkg + r"\2", n_, count=1)
        rel = os.path.relpath(p, M3)
        new_name = (ren[name][1] if owner else name) + ".java"
        dest = os.path.join(M3, rel.replace(os.path.join(*CORE.split(".")) + os.sep + name + ".java",
                                            os.path.join(*tpkg.split(".")) + os.sep + new_name, 1))
        moves += 1
    elif owner:
        dest = os.path.join(os.path.dirname(p), ren[name][1] + ".java")
        moves += 1
    if n_ != t or dest != p:
        changed += 1
        if not check:
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            open(dest, "w", encoding="utf-8", errors="surrogateescape", newline="").write(n_)
            if dest != p:
                os.remove(p)
# v1.2 TEST_EDITS: declared exact edits of tests that pin data (sorted name lists, frozen fixtures); each
# must match exactly once, else the run refuses
if os.environ.get("TEST_EDITS"):
    import json
    for ed in json.load(open(os.environ["TEST_EDITS"], encoding="utf-8")):
        fp = os.path.join(M3, *ed["file"].split("/"))
        tt = open(fp, encoding="utf-8", newline="").read()
        if ed["new"] in tt and ed["old"] not in tt:
            continue
        if tt.count(ed["old"]) != 1:
            raise SystemExit(f"TEST_EDIT does not match exactly once: {ed['file']}: {ed['old'][:60]}")
        changed += 1
        if not check:
            open(fp, "w", encoding="utf-8", newline="").write(tt.replace(ed["old"], ed["new"]))
# C sources (JNI), recipe maps, module-info exports
for base, dirs, fs in os.walk(M3):
    dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
    for f in fs:
        p = os.path.join(base, f)
        if os.sep + "resources" + os.sep in base + os.sep:  # v1.5: resources (hash-pinned snapshots) are data, never rewritten
            continue
        if f.endswith((".c", ".cc", ".cpp", ".h")):
            t = open(p, encoding="utf-8", errors="surrogateescape", newline="").read()
            n_ = JNI.sub(lambda m: "Java_" + new_fqn(m.group(1)).replace(".", "_") + "_", t)
            n_ = FQN_SL.sub(lambda m: m.group(1) + new_fqn(m.group(2)).replace(".", "/"), n_)
        elif f == "pom.xml":
            t = open(p, encoding="utf-8", newline="").read()
            n_ = re.sub(r"(\*\*/)(" + alt + r")(\.java)", lambda m: m.group(1) + ren[m.group(2)][1] + m.group(3), t)
        elif f == "RELOCATION_MAP.tsv" and os.sep + os.path.join("m3", "compiler") + os.sep in p:
            # v1.4: only the compiler processors' maps follow (their LITERAL rows name annotations users write);
            # other recipes' maps stay the historical record of what they produced (chained: recipe, then CURATE_BC)
            t = open(p, encoding="utf-8", newline="").read()
            n_ = FQN_DOT.sub(lambda m: new_fqn(m.group(1)), t)
            n_ = re.sub(r"(?<=\t)@(" + alt + r")" + W, lambda m: "@" + ren[m.group(1)][1], n_)
        else:
            continue
        if n_ != t:
            changed += 1
            if not check:
                open(p, "w", encoding="utf-8", errors="surrogateescape", newline="").write(n_)
mi = os.path.join(M3, "collections", "src", "main", "java", "module-info.java")
if os.path.exists(mi):
    t = open(mi, encoding="utf-8", newline="").read()
    pkgs = sorted({pp for pp, _ in ren.values() if pp != CORE and not pp.startswith("jdk.internal.")})
    e = "\r\n" if "\r\n" in t else "\n"
    add = [x for x in pkgs if not re.search(r"^\s*exports\s+" + re.escape(x) + r"\s*;", t, re.M)]
    if add:
        m = list(re.finditer(r"^\s*exports\s+jdk\.m3\.collections\s*;[ \t]*\r?\n", t, re.M))
        pos = m[-1].end()
        t2 = t[:pos] + "".join(f"    exports {x};{e}" for x in add) + t[pos:]
        changed += 1
        if not check:
            open(mi, "w", encoding="utf-8", newline="").write(t2)
print(f"CURATE_E renamed={len(ren)} moved_or_renamed_files={moves} changed_files={changed} clash_fq={clashes} "
      f"added_imports={imports_added}{' (check)' if check else ''}")
sys.exit(1 if check and changed else 0)
