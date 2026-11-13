"""ixs_closure.py <src-dir> <seed>[,<seed>...] : INDEXSTRING_CLOSURE v1.
Transitive closure of same-package type references from the seed classes over a flat donor package directory
(one top-level type per file, file stem = type name). A reference is any identifier token equal to another file's stem
(comments and string literals stripped first). Prints one row per closure member (depth, bytes, lines, non-JDK
imports) and a footer with totals and the set of non-JDK imports, so a port leaf can be sized before any code moves."""
import re
import sys
from collections import deque
from pathlib import Path

src, seeds = Path(sys.argv[1]), sys.argv[2].split(",")
STRIP = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])\'', re.S)
TOKEN = re.compile(r"\b[A-Z][A-Za-z0-9_]*\b")
texts = {p.stem: p.read_text(encoding="utf-8", errors="replace") for p in src.glob("*.java")}
names = set(texts)
refs = {n: {t for t in TOKEN.findall(STRIP.sub(" ", s)) if t in names and t != n} for n, s in texts.items()}
imports = {n: sorted(set(re.findall(r"^import\s+(?:static\s+)?([\w.]+)", s, re.M))) for n, s in texts.items()}
depth = {}
queue = deque()
for s in seeds:
    if s not in names:
        sys.exit(f"seed {s} not in {src}")
    depth[s] = 0
    queue.append(s)
while queue:
    n = queue.popleft()
    for r in sorted(refs[n]):
        if r not in depth:
            depth[r] = depth[n] + 1
            queue.append(r)
external = set()
total_bytes = total_lines = 0
for n in sorted(depth, key=lambda k: (depth[k], k)):
    ext = [i for i in imports[n] if not i.startswith("java.")]
    external.update(ext)
    b, l = len(texts[n].encode("utf-8")), texts[n].count("\n")
    total_bytes += b
    total_lines += l
    print(f"{depth[n]}\t{b}\t{l}\t{n}\t{' '.join(ext)}")
print(f"CLOSURE seeds={','.join(seeds)} members={len(depth)} bytes={total_bytes} lines={total_lines} "
      f"max_depth={max(depth.values())}")
print("EXTERNAL " + " ".join(sorted(external)))
