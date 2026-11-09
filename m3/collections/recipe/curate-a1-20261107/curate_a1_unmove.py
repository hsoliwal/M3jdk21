# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_a1_unmove.py <maven log> <a1.txt> <out a1.txt> <unmoved log.tsv> <test_move.txt> <test_stay.txt>

Compiler-driven fixed point for phase A1 (v2): the compiler is the authority on package coupling (bytecode misses
inlined constants and some package-private constructors). From a failed -Werror build log of the curated tree:
MAIN code
  "<type> X in module ... is not exported" at a file left in jdk.m3.collections -> X stays (un-move X);
  "jdk.internal.m3.collections.X is not public in jdk.internal.m3.collections" at a remaining file -> X stays;
  "<member> is not public in C ..." -> the moved side stays (the file if it moved, else C);
  any other error in a moved main file -> that file stays.
TESTS (not API: each test sits where it compiles)
  error in a moved test -> the test stays (test_stay);
  error in a remaining test naming jdk.internal.m3.collections (message, symbol or location line) -> the test moves
  (test_move); if it was already forced to stay, the moved class it names stays instead.
Updates the lists in place (test files) / writes the reduced moving list; exit 0 if anything changed."""
import re
import sys

log, a1f, outf, why, tmf, tsf = sys.argv[1:7]
moving = [l.strip() for l in open(a1f, encoding="utf-8") if l.strip()]
mv = set(moving)
rd = lambda f: set(l.strip() for l in open(f, encoding="utf-8") if l.strip()) if __import__("os").path.exists(f) else set()
tmove, tstay = rd(tmf), rd(tsf)
un, newmove, newstay = {}, set(), set()
loc = re.compile(r"\[(?:ERROR|WARNING)\] /\S*?/src/(main|test)/java/jdk/(internal/)?m3/collections/([\w$]+)\.java:\[\d+,\d+\] (.*)")
lines = open(log, encoding="utf-8", errors="replace").read().splitlines()
for i, line in enumerate(lines):
    m = loc.search(line)
    if not m:
        continue
    kind, moved_file, f, msg = m.group(1), bool(m.group(2)), m.group(3), m.group(4)
    ctx = msg + " " + " ".join(lines[i + 1:i + 4])
    if kind == "test":
        if moved_file:
            newstay.add(f)
        elif "jdk.internal.m3.collections" in ctx:
            if f in tstay:
                c = re.search(r"jdk\.internal\.m3\.collections\.(M3\w+)", ctx)
                if c and c.group(1) in mv:
                    un.setdefault(c.group(1), f"test {f} needs it in its package")
            else:
                newmove.add(f)
        continue
    e = re.search(r"jdk\.internal\.m3\.collections\.(M3\w+) in module \S+ is not exported", msg)
    if e and not moved_file:
        un.setdefault(e.group(1), f"exported {f} exposes it")
        continue
    e = re.search(r"jdk\.internal\.m3\.collections\.(M3\w+) is not public in jdk\.internal\.m3\.collections;", msg)
    if e and not moved_file:
        un.setdefault(e.group(1), f"remaining {f} uses the non-public class")
        continue
    e = re.search(r"is not public in (?:jdk\.(?:internal\.)?m3\.collections\.)?(M3\w+)", msg)
    if e:
        c = e.group(1)
        if moved_file and f in mv:
            un.setdefault(f, f"uses non-public member of remaining {c}")
        elif c in mv:
            un.setdefault(c, f"remaining {f} uses its non-public member")
        continue
    if moved_file and f in mv and "[ERROR]" in line:
        un.setdefault(f, "compile error after move: " + msg[:80])
un = {k: v for k, v in un.items() if k in mv}
newmove -= tmove
newstay -= tstay
tmove |= newmove
tstay |= newstay
tmove -= tstay
open(outf, "w", encoding="utf-8", newline="\n").write("\n".join(c for c in moving if c not in un) + "\n")
open(tmf, "w", encoding="utf-8", newline="\n").write("\n".join(sorted(tmove)) + "\n")
open(tsf, "w", encoding="utf-8", newline="\n").write("\n".join(sorted(tstay)) + "\n")
with open(why, "a", encoding="utf-8", newline="\n") as w:
    for k, v in sorted(un.items()):
        w.write(f"{k}\t{v}\n")
    for t in sorted(newmove):
        w.write(f"test:{t}\tMOVE\n")
    for t in sorted(newstay):
        w.write(f"test:{t}\tSTAY\n")
print(f"un-moved {len(un)}; moving now {len(mv) - len(un)}; tests +move {len(newmove)} +stay {len(newstay)}")
sys.exit(0 if (un or newmove or newstay) else 1)
