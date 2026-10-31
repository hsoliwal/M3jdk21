# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""absorb_rename.py <src_in> <TYPE_RENAME.tsv> <src_out> <report_dir> [<CC_STRING_RENAME.tsv>] : leaf 1 of the Skija
absorption, after MR_SELECT + delombok + DELOMBOK_CLEAN. Deterministic, string- and comment-safe (SFA: edits happen
only in code regions; offsets come from a masked copy of the same length), except the exact literals of the string
ledger:

  STRIP_JB v1      remove the JetBrains documentation annotations (@ApiStatus.*, @NotNull, @Nullable, @Contract(...),
                   also fully qualified) with their argument lists, and their imports
  RENAME v1        package prefixes io.github.humbleui.skija.impl -> jdk.m3.graphics.impl, io.github.humbleui.skija ->
                   jdk.m3.graphics, io.github.humbleui.types -> jdk.m3.graphics; every type token of the map T -> M3T
                   when it is unqualified or qualified by one of the new packages; files move to <pkg dirs>/M3T.java
  STRING_RENAME v1 the Java rows of the string ledger (rows of CC_STRING_RENAME.tsv whose file is a .java source;
                   gen_rename.py makes them from skija_patterns.py's JAVA_CLASSREFS.tsv): in the file of the row, every
                   string literal equal to the old text becomes the new text. A row that is UNMAPPED, matches no
                   literal, or names a file that is not among the sources stops the run
  STRING_SCAN v1   every string literal left that names a donor type goes to report_dir/STRING_MENTIONS.tsv by kind:
                   RENAMED (by STRING_RENAME), PACKAGE (a humbleui package name without a type: the native loader's
                   module name, replaced in leaf 2), DISPLAY (a donor simple name inside text, such as messages and
                   toString: kept as the donor prints it). A class-file resource "T.class" of a donor type or a donor
                   binary name still in a literal is a missed run-time lookup and stops the run
Counts per rule go to report_dir/RENAME_COUNTS.tsv."""
import os
import re
import sys
from collections import Counter

MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"""(?:.|\n)*?"""|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)
PKGS = (("io.github.humbleui.skija.impl", "jdk.m3.graphics.impl"), ("io.github.humbleui.skija", "jdk.m3.graphics"),
        ("io.github.humbleui.types", "jdk.m3.graphics"))
JB_ANN = re.compile(r'@(?:org\.jetbrains\.annotations\.)?(?:ApiStatus\.\w+|NotNull|Nullable|Contract)\b')
JB_IMPORT = re.compile(r'^import (?:static )?org\.jetbrains\.annotations\.[\w.*]+;[ \t]*\r?\n', re.M)
IDENT = re.compile(r'[A-Za-z_$][\w$]*')
CLASS_FILE_LITERAL = re.compile(r'^"([A-Za-z_$][\w$]*)\.class"$')
BINARY_NAME = re.compile(r'io[./]github[./]humbleui(?:[./][a-z_$][\w$]*)*[./][A-Z][\w$]*')
SOURCE_SET = re.compile(r'^shared/java\d*/')
UNMAPPED = "UNMAPPED"
TEXT_BLOCK = '"""'


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


def string_ledger(path):
    """{package-relative source path: [(row line, old, new)]} of the Java rows of a CC_STRING_RENAME ledger."""
    rows = {}
    if not path:
        return rows
    with open(path, encoding="utf-8") as f:
        next(f)
        for line in f:
            file, row_line, old, new = line.rstrip("\n").split("\t")
            if not file.endswith(".java"):
                continue
            if new == UNMAPPED:
                raise SystemExit(f"string ledger row {file}:{row_line} {old!r} is {UNMAPPED}")
            rows.setdefault(SOURCE_SET.sub("", file), []).append((row_line, old, new))
    return rows


def string_literals(s):
    """(start, end) of every string literal in code; comments, char literals and text blocks are skipped."""
    return [(m.start(), m.end()) for m in MASK.finditer(s)
            if m.group(0).startswith('"') and not m.group(0).startswith(TEXT_BLOCK)]


def rename_strings(s, rows, counts, where):
    for row_line, old, new in rows:
        out, i, n = [], 0, 0
        for a, b in string_literals(s):
            if s[a:b] == '"' + old + '"':
                out.append(s[i:a] + '"' + new + '"')
                i = b
                n += 1
        out.append(s[i:])
        if not n:
            raise SystemExit(f"string ledger row {where}:{row_line} {old!r} matches no literal")
        s = "".join(out)
        counts["string_rename"] += n
    return s


def scan_strings(s, simple, renamed, m3_type, counts, mentions):
    for a, b in string_literals(s):
        lit, line = s[a:b], str(s.count("\n", 0, a) + 1)
        cf = CLASS_FILE_LITERAL.match(lit)
        if (cf and cf.group(1) in simple) or BINARY_NAME.search(lit):
            raise SystemExit(f"missed run-time class lookup in {m3_type}:{line}: {lit}")
        if lit in renamed:
            kind = "RENAMED"
        elif "humbleui" in lit:
            kind = "PACKAGE"
        elif any(t.group(0) in simple for t in IDENT.finditer(lit[1:-1])):
            kind = "DISPLAY"
        else:
            continue
        if kind != "RENAMED":
            counts["string_" + kind.lower()] += 1
        mentions.append((m3_type, line, kind, lit))


def main():
    src, ren, dst, rep = sys.argv[1:5]
    ledger = string_ledger(sys.argv[5] if len(sys.argv) > 5 else None)
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
    counts, mentions, used = Counter(), [], set()
    os.makedirs(rep, exist_ok=True)
    for dirpath, dirs, files in os.walk(src):
        dirs.sort()
        for f in sorted(files):
            if not f.endswith(".java"):
                continue
            p = os.path.join(dirpath, f)
            rel = os.path.relpath(p, src).replace(os.sep, "/")
            s = open(p, encoding="utf-8", newline="").read()
            pm = re.search(r"^package ([\w.]+);", s, re.M)
            old_fqn = pm.group(1) + "." + f[:-5]
            if old_fqn not in fq:
                raise SystemExit(f"{old_fqn} is not in the rename map")
            vis = visible(s, by_pkg, fq)
            s = rename(strip_jb(s, counts), simple, counts, vis)
            rows = ledger.get(rel, [])
            if rows:
                used.add(rel)
                s = rename_strings(s, rows, counts, rel)
            scan_strings(s, simple, {'"' + new + '"' for _, _, new in rows}, fq[old_fqn], counts, mentions)
            if re.search(r"\b(?:org\.jetbrains|io\.github\.humbleui)\b", masked(s)):
                raise SystemExit(f"old name left in code of {old_fqn}")
            new = fq[old_fqn]
            out = os.path.join(dst, *new.split(".")) + ".java"
            os.makedirs(os.path.dirname(out), exist_ok=True)
            open(out, "w", encoding="utf-8", newline="").write(s)
            counts["files"] += 1
    if set(ledger) - used:
        raise SystemExit("string ledger rows name files that are not sources: " + ", ".join(sorted(set(ledger) - used)))
    with open(os.path.join(rep, "RENAME_COUNTS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("rule\tcount\n" + "".join(f"{k}\t{v}\n" for k, v in sorted(counts.items())))
    with open(os.path.join(rep, "STRING_MENTIONS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("m3_type\tline\tkind\tstring\n" + "".join("\t".join(r) + "\n" for r in mentions))
    print(" ".join(f"{k}={v}" for k, v in sorted(counts.items())), f"mentions={len(mentions)}")


main()
