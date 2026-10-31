# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""absorb_rename_generic.py (ABSORB_PKGS="old=new;..." in the environment)

absorb_rename.py <src_in> <TYPE_RENAME.tsv> <src_out> <report_dir> : leaf 1 of the Skija absorption, after
delombok + DELOMBOK_CLEAN. Deterministic, string- and comment-safe (SFA: edits happen only in code regions; offsets come
from a masked copy of the same length):

  STRIP_JB v1  remove the JetBrains documentation annotations (@ApiStatus.*, @NotNull, @Nullable, @Contract(...), also
               fully qualified) with their argument lists, and their imports
  RENAME v1    package prefixes io.github.humbleui.skija.impl -> jdk.m3.graphics.impl, io.github.humbleui.skija ->
               jdk.m3.graphics, io.github.humbleui.types -> jdk.m3.graphics; every type token of the map T -> M3T when it
               is unqualified or qualified by one of the new packages; files move to <pkg dirs>/M3T.java
Strings are reported, never edited (report_dir/STRING_MENTIONS.tsv). Counts per rule go to report_dir/RENAME_COUNTS.tsv."""
import os
import re
import sys
from collections import Counter

MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"""(?:.|\n)*?"""|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)
PKGS = tuple(tuple(x.split("=", 1)) for x in os.environ["ABSORB_PKGS"].split(";") if x)  # old=new;old=new, longest first
JB_ANN = re.compile(r'@(?:org\.jetbrains\.annotations\.)?(?:ApiStatus\.\w+|NotNull|Nullable|Contract)\b')
JB_IMPORT = re.compile(r'^import (?:static )?org\.jetbrains\.annotations\.[\w.*]+;[ \t]*\r?\n', re.M)
IDENT = re.compile(r'[A-Za-z_$][\w$]*')


def masked(s):
    return MASK.sub(lambda m: "".join(c if c == "\n" else " " for c in m.group(0)), s)


def strip_jb(s, counts):
    s, n = JB_IMPORT.subn("", s)
    counts["jb_import"] += n
    out, i, m = [], 0, masked(s)
    for a in JB_ANN.finditer(m):
        if a.start() < i:
            continue
        j = a.end()
        k = j
        while k < len(m) and m[k] in " \t":
            k += 1
        if k < len(m) and m[k] == "(":
            depth = 0
            while k < len(m):
                depth += (m[k] == "(") - (m[k] == ")")
                k += 1
                if depth == 0:
                    break
            j = k
        while j < len(m) and m[j] in " \t":
            j += 1
        out.append(s[i:a.start()])
        i = j
        counts["jb_annotation"] += 1
    out.append(s[i:])
    s = "".join(out)
    s2 = re.sub(r"^[ \t]+\r?\n", "", s, flags=re.M)
    return s2


def visible(s, by_pkg, fq):
    """Our simple names a file can name unqualified, as javac scopes them: own package, single-type imports of ours,
    on-demand imports of our packages (a JDK on-demand import never shadows these; it fills the rest)."""
    m = masked(s)
    pm = re.search(r"^package ([\w.]+);", m, re.M)
    names = set(by_pkg.get(pm.group(1), ())) if pm else set()
    for imp in re.finditer(r"^import ([\w.]+?)(\.\*)?;", m, re.M):
        if imp.group(2):
            names |= set(by_pkg.get(imp.group(1), ()))
        elif imp.group(1) in fq:
            names.add(imp.group(1).rsplit(".", 1)[1])
    return names


def rename(s, simple, counts, vis=None):
    m = masked(s)
    for old, new in PKGS:
        out, i = [], 0
        for hit in re.finditer(re.escape(old) + r"(?![\w])", m):
            out.append(s[i:hit.start()] + new)
            i = hit.end()
            counts["pkg"] += 1
        out.append(s[i:])
        s = "".join(out)
        m = masked(s)
    out, i = [], 0
    news = tuple(n for _, n in PKGS)
    for t in IDENT.finditer(m):
        name = t.group(0)
        if name not in simple:
            continue
        b = t.start()
        if b > 0 and m[b - 1] == ".":
            q = re.search(r"([\w.]+)\.$", m[:b])
            if not q or not any(q.group(1) == n or q.group(1).startswith(n + ".") for n in news):
                continue
        elif vis is not None and name not in vis:
            counts["type_token_shadowed"] += 1
            continue
        out.append(s[i:b] + simple[name])
        i = t.end()
        counts["type_token"] += 1
    out.append(s[i:])
    return "".join(out)


def main():
    src, ren, dst, rep = sys.argv[1:5]
    fq = {}
    with open(ren, encoding="utf-8") as f:
        next(f)
        for line in f:
            a, b = line.rstrip("\n").split("\t")
            fq[a] = b
    simple = {a.rsplit(".", 1)[1]: b.rsplit(".", 1)[1] for a, b in fq.items()}
    if len(simple) != len(fq):
        raise SystemExit("simple names are not unique")
    by_pkg = {}
    for a in fq:
        by_pkg.setdefault(a.rsplit(".", 1)[0], set()).add(a.rsplit(".", 1)[1])
    counts, strings = Counter(), []
    os.makedirs(rep, exist_ok=True)
    for dirpath, _, files in os.walk(src):
        for f in sorted(files):
            if not f.endswith(".java"):
                continue
            p = os.path.join(dirpath, f)
            s = open(p, encoding="utf-8", newline="").read()
            pm = re.search(r"^package ([\w.]+);", s, re.M)
            old_fqn = pm.group(1) + "." + f[:-5]
            if old_fqn not in fq:
                raise SystemExit(f"{old_fqn} is not in the rename map")
            vis = visible(s, by_pkg, fq)
            s = rename(strip_jb(s, counts), simple, counts, vis)
            for hit in re.finditer(r'"(?:\\.|[^"\\\n])*"', s):
                if "humbleui" in hit.group(0):
                    strings.append((fq[old_fqn], str(s.count("\n", 0, hit.start()) + 1), hit.group(0)))
            if any(re.search(r"\b" + re.escape(o) + r"\b", masked(s)) for o, _ in PKGS):
                raise SystemExit(f"old name left in code of {old_fqn}")
            new = fq[old_fqn]
            out = os.path.join(dst, *new.split(".")) + ".java"
            os.makedirs(os.path.dirname(out), exist_ok=True)
            open(out, "w", encoding="utf-8", newline="").write(s)
            counts["files"] += 1
    with open(os.path.join(rep, "RENAME_COUNTS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("rule\tcount\n" + "".join(f"{k}\t{v}\n" for k, v in sorted(counts.items())))
    with open(os.path.join(rep, "STRING_MENTIONS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("m3_type\tline\tstring\n" + "".join("\t".join(r) + "\n" for r in strings))
    print(" ".join(f"{k}={v}" for k, v in sorted(counts.items())), f"strings={len(strings)}")


main()
