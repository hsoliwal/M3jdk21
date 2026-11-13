# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""gen_map_e1.py <RENAME_MAP_E_draft.tsv> <worktree> <out RENAME_MAP_E1.tsv>

CURATE_E pass E1 map: the jdk.internal.m3.collections rows of the draft (main types: family subpackage + no M3 prefix)
plus one row per test type of the internal test tree. A test follows its subject (the longest main type whose
de-prefixed name starts the test's de-prefixed name) into the subject's family; a test without a subject stays in the
root package. Every test drops the M3 prefix (unless the de-prefixed name is a java.* name: then it is kept).
Refuses on any duplicate target FQN."""
import os
import sys

draft, wt, out = sys.argv[1:4]
CORE = "jdk.internal.m3.collections"
rows = [l.rstrip("\r\n").split("\t") for l in list(open(draft, encoding="utf-8"))[1:] if l.strip()]
main = {r[0]: r[1] for r in rows if r[0].startswith(CORE + ".") and r[0].count(".") == CORE.count(".") + 1}
base = lambda n: n[2:] if n.startswith("M3") and n[2:3].isupper() else n
subj = {base(o.rsplit(".", 1)[1]): n for o, n in main.items()}
tdir = os.path.join(wt, "m3", "collections", "src", "test", "java", *CORE.split("."))
tests = sorted(f[:-5] for f in os.listdir(tdir) if f.endswith(".java")) if os.path.isdir(tdir) else []
JAVA = {"Test", "Override"}
out_rows, targets = [], {}
for o, n in sorted(main.items()):
    out_rows.append((o, n, "main"))
for t in tests:
    b = base(t)
    hit = max((s for s in subj if b.startswith(s) and len(b) > len(s) and b[len(s)].isupper()), key=len, default=None)
    pkg = subj[hit].rsplit(".", 1)[0] if hit else CORE
    out_rows.append((f"{CORE}.{t}", f"{pkg}.{b if b not in JAVA else t}", "test:" + (hit or "-")))
for o, n, _ in out_rows:
    targets.setdefault(n, []).append(o)
dups = {k: v for k, v in targets.items() if len(v) > 1}
if dups:
    for k, v in dups.items():
        print("DUP", k, v)
    raise SystemExit(2)
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("old_fqn\tnew_fqn\tkind\n")
    for o, n, k in out_rows:
        w.write(f"{o}\t{n}\t{k}\n")
print(f"E1 map: {sum(1 for r in out_rows if r[2] == 'main')} main, {len(tests)} tests "
      f"({sum(1 for r in out_rows if r[2].startswith('test:') and r[2] != 'test:-')} follow a subject)")
