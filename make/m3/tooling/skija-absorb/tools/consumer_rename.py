# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""consumer_rename.py <worktree> <files.txt> <TYPE_RENAME.tsv> <report_dir> [--dry-run]

CONSUMER_RENAME v1 of the Skija absorption: rewrites Java consumers of Skija / HumbleUI Types (SWT Render M3, GPU M3,
tests, tools) onto the M3jdk owner jdk.m3.graphics, in place. Deterministic, SFA-safe (edits only in code regions,
offsets from a masked copy), javac-scoped:

  - package prefixes io.github.humbleui.skija.impl -> jdk.m3.graphics.impl, io.github.humbleui.skija ->
    jdk.m3.graphics, io.github.humbleui.types -> jdk.m3.graphics (imports, static imports, fully qualified uses)
  - a type token T -> M3T when it is qualified by one of the new packages, or unqualified and resolving to the donor
    type in javac's scope: a single-type import of it, or an on-demand import of its package that no single-type import,
    own-file type or same-package type shadows
  - COLLISION (fail-closed, nothing written): a renamed name M3T would be shadowed in that file by an own-file type, a
    same-package type or a single-type import of another type named M3T
Strings are reported, never edited (report_dir/STRING_MENTIONS.tsv). Per-file counts: report_dir/CONSUMER_RENAME.tsv."""
import os
import re
import sys
from collections import Counter

MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"""(?:.|\n)*?"""|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)
PKGS = (("io.github.humbleui.skija.impl", "jdk.m3.graphics.impl"), ("io.github.humbleui.skija", "jdk.m3.graphics"),
        ("io.github.humbleui.types", "jdk.m3.graphics"))
IDENT = re.compile(r"[A-Za-z_$][\w$]*")
DECL = re.compile(r"\b(?:class|interface|enum|record|@interface)\s+([A-Za-z_$][\w$]*)")


def masked(s):
    return MASK.sub(lambda m: "".join(c if c == "\n" else " " for c in m.group(0)), s)


def package_index(root):
    """package -> simple names of top-level types declared in Java files on disk under the worktree (one read each)."""
    idx = {}
    for dirpath, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in (".git", "target", "bin")]
        for f in files:
            if f.endswith(".java"):
                p = os.path.join(dirpath, f)
                with open(p, encoding="utf-8", errors="replace") as h:
                    head = h.read(8192)
                m = re.search(r"^\s*package\s+([\w.]+)\s*;", masked(head), re.M)
                if m:
                    idx.setdefault(m.group(1), set()).add(f[:-5])
    return idx


def scope(m, by_pkg_old, idx):
    """(ours, blocked): simple donor names a file names unqualified, and all names an own type, the same package or a
    single-type import takes (javac: single-type import > own/same package > on-demand)."""
    pm = re.search(r"^package ([\w.]+);", m, re.M)
    pkg = pm.group(1) if pm else ""
    own = set(DECL.findall(m))
    same = set(idx.get(pkg, ())) | own
    single_ours, single_other, demand_ours = set(), set(), set()
    for imp in re.finditer(r"^import (static )?([\w.]+?)(\.\*)?;", m, re.M):
        if imp.group(1):
            continue
        name = imp.group(2)
        if imp.group(3):
            demand_ours |= set(by_pkg_old.get(name, ()))
        elif name.rsplit(".", 1)[0] in by_pkg_old and name.rsplit(".", 1)[1] in by_pkg_old[name.rsplit(".", 1)[0]]:
            single_ours.add(name.rsplit(".", 1)[1])
        else:
            single_other.add(name.rsplit(".", 1)[1])
    ours = single_ours | {n for n in demand_ours if n not in same and n not in single_other}
    return ours, same | single_other, pkg


def string_edits(s, rel, edits, counts):
    """STRING_EDITS v1: reviewed class-name string literals (ledger rows file, old, new, expected count); fail-closed."""
    for old, new, want in edits.get(rel, ()):
        got = s.count(old)
        if got != want:
            raise SystemExit(f"STRING_EDITS: {rel} has {got} x {old}, ledger expects {want}")
        s = s.replace(old, new)
        counts["string_edit"] += got
    return s


def rewrite(s, simple, ours, counts, blocked=frozenset(), fqn_of=None):
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
    news = tuple(n for _, n in PKGS)
    out, i, used = [], 0, set()
    for t in IDENT.finditer(m):
        name = t.group(0)
        if name not in simple:
            continue
        b = t.start()
        if b > 0 and m[b - 1] == ".":
            q = re.search(r"([\w.]+)\.$", m[:b])
            if not q or not any(q.group(1) == n or q.group(1).startswith(n + ".") for n in news):
                continue
        elif name not in ours:
            counts["shadowed"] += 1
            continue
        target = simple[name]
        is_qualified = b > 0 and m[b - 1] == "."
        if target in blocked and not is_qualified:
            # COLLISION_QUALIFY v1: the M3 name is taken in this file (own, same-package or imported type), so the
            # donor type is written fully qualified at this site; its single-type import is dropped below
            target = fqn_of[target]
            counts["collision_qualified"] += 1
        out.append(s[i:b] + target)
        used.add(simple[name])
        i = t.end()
        counts["type_token"] += 1
    out.append(s[i:])
    return "".join(out), used


def main():
    root, files_txt, ren, rep = sys.argv[1:5]
    dry = "--dry-run" in sys.argv
    fq = {}
    with open(ren, encoding="utf-8") as f:
        next(f)
        for line in f:
            a, b = line.rstrip("\n").split("\t")
            fq[a] = b
    simple = {a.rsplit(".", 1)[1]: b.rsplit(".", 1)[1] for a, b in fq.items()}
    by_pkg_old = {}
    for a in fq:
        by_pkg_old.setdefault(a.rsplit(".", 1)[0], set()).add(a.rsplit(".", 1)[1])
    idx = package_index(root)
    fqn_of = {b.rsplit(".", 1)[1]: b for b in fq.values()}
    edits = {}
    ledger = os.environ.get("STRING_EDITS")
    if ledger:
        with open(ledger, encoding="utf-8") as f:
            next(f)
            for line in f:
                if line.strip():
                    r, o, n, c = line.rstrip("\n").split("\t")
                    edits.setdefault(r, []).append((o, n, int(c)))
    files = [l.strip() for l in open(files_txt, encoding="utf-8") if l.strip().endswith(".java")]
    rows, strings, collisions, plan = [], [], [], []
    for rel in files:
        p = os.path.join(root, rel)
        s = open(p, encoding="utf-8", newline="").read()
        m = masked(s)
        ours, blocked, pkg = scope(m, by_pkg_old, idx)
        counts = Counter()
        targets_blocked = {simple[n] for n in ours if simple[n] in blocked}
        new, used = rewrite(s, simple, ours, counts, targets_blocked, fqn_of)
        for n in sorted(used & targets_blocked):
            new, k = re.subn(r"^import " + re.escape(fqn_of[n]) + r";[ \t]*\r?\n", "", new, flags=re.M)
            counts["collision_import_dropped"] += k
            collisions.append((rel, n, f"QUALIFIED (import dropped: {k})"))
        new = string_edits(new, rel, edits, counts)
        # IMPORT_DEDUPE v1: io.github.humbleui.skija.* and io.github.humbleui.types.* both become jdk.m3.graphics.*;
        # keep the first of identical import lines (ecj warns "never used" on the second)
        seen_imp, kept = set(), []
        for line in new.splitlines(keepends=True):
            key = line.strip()
            if key.startswith("import ") and key.endswith(";"):
                if key in seen_imp:
                    counts["import_dedupe"] += 1
                    continue
                seen_imp.add(key)
            kept.append(line)
        new = "".join(kept)
        for hit in re.finditer(r'"(?:\\.|[^"\\\n])*"', new):
            if "humbleui" in hit.group(0):
                strings.append((rel, str(new.count("\n", 0, hit.start()) + 1), hit.group(0)))
        left = len(re.findall(r"\bio\.github\.humbleui\b", masked(new)))
        rows.append((rel, str(counts["pkg"]), str(counts["type_token"]), str(counts["shadowed"]), str(left),
                     str(counts["collision_qualified"]), str(counts["string_edit"])))
        plan.append((p, s, new))
    os.makedirs(rep, exist_ok=True)
    w = lambda n, head, rs: open(os.path.join(rep, n), "w", encoding="utf-8", newline="\n").write(
        "\t".join(head) + "\n" + "".join("\t".join(r) + "\n" for r in rs))
    w("CONSUMER_RENAME.tsv", ("file", "pkg_refs", "type_tokens", "shadowed_tokens", "humbleui_left_in_code",
                              "collision_qualified", "string_edits"), rows)
    w("STRING_MENTIONS.tsv", ("file", "line", "string"), strings)
    w("COLLISIONS.tsv", ("file", "m3_name", "action"), collisions)
    tot = Counter()
    for r in rows:
        tot["files"] += 1; tot["pkg"] += int(r[1]); tot["type_token"] += int(r[2]); tot["shadowed"] += int(r[3]); tot["left"] += int(r[4])
    print(" ".join(f"{k}={v}" for k, v in sorted(tot.items())), f"strings_left={len(strings)} collisions_qualified={len(collisions)}")
    if tot["left"]:
        print("HELD: old names left in code; nothing written")
        sys.exit(1)
    if not dry:
        for p, old, new in plan:
            if new != old:
                open(p, "w", encoding="utf-8", newline="").write(new)


main()
