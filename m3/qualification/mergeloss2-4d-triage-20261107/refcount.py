"""refcount.py <CLEAN.tsv> <mlb-dir> <tree-root>... : MERGE_LOSS_REFCOUNT v1.
For each leaf of a mergeloss2 CLEAN table that mergeloss_batch.py wrote under <mlb-dir>/<leaf>/: the members the
origin has and master lacks (column members_missing_on_master) are counted as word tokens in the master source
trees (<tree-root>..., with each file of the leaf itself excluded; comments and string literals stripped). Prints one
row per leaf: n, batch verdict, missing-member count, members referenced elsewhere on master (name:count), and a
triage class:
  BODY_ONLY      no missing member: the loss is inside existing members (gate by differential)
  REFERENCED     a missing member's name occurs outside the leaf's files (check: same member or a homonym?)
  UNREFERENCED   every missing member is used only inside the leaf's files (capability or dead code)"""
import re
import sys
from pathlib import Path

table, mlb, roots = Path(sys.argv[1]), Path(sys.argv[2]), [Path(p) for p in sys.argv[3:]]
STRIP = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"', re.S)
rows = [l.split("\t") for l in table.read_text(encoding="utf-8").splitlines()]
head = rows[0]
col = {name: i for i, name in enumerate(head)}
corpus = {}
for root in roots:
    for p in root.rglob("*.java"):
        corpus[p.as_posix()] = STRIP.sub(" ", p.read_text(encoding="utf-8", errors="replace"))
verdicts = {}
batch = (Path(sys.argv[2]).parent / (Path(sys.argv[2]).name + ".txt"))
if batch.exists():
    for line in batch.read_text(encoding="utf-8").splitlines():
        if line.startswith("LEAF "):
            parts = line.split("\t")
            verdicts[parts[0][5:]] = parts[1]
for r in rows[1:]:
    n = r[col["n"]]
    miss = [m for m in r[col["members_missing_on_master"]].split(",") if m and m != "-"]
    own = [f for f in r[col["files"]].split(";") if f]
    own_tails = tuple("/" + f.split("/share/classes/")[-1].split("/src/main/java/")[-1] for f in own)
    hits = {}
    for m in miss:
        pat = re.compile(r"\b" + re.escape(m) + r"\b")
        c = sum(len(pat.findall(text)) for path, text in corpus.items() if not path.endswith(own_tails))
        if c:
            hits[m] = c
    cls = "BODY_ONLY" if not miss else "REFERENCED" if hits else "UNREFERENCED"
    shown = ",".join(f"{k}:{v}" for k, v in sorted(hits.items(), key=lambda kv: -kv[1])[:6])
    print(f"{n}\t{verdicts.get(n, '?')}\t{cls}\tmissing={len(miss)}\t{shown or '-'}\t{';'.join(f.split('/')[-1] for f in own)}")
