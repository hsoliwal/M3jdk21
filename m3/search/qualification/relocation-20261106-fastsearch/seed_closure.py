"""seed_closure.py <seed simple names...> : transitive closure of synexia-fastsearch main classes reachable from the
seeds (imports + same-package simple-name references). Prints each member with its donor path and its non-java
imports, so the leaf takes exactly what the seeds need."""
import re
import sys
from pathlib import Path

D = Path(__file__).parent / "ffs"
files = {p: p.read_text(encoding="utf-8") for p in D.rglob("*.java")}
by_fq, by_simple = {}, {}
for p, s in files.items():
    pkg = re.search(r"^package ([\w.]+);", s, re.M).group(1)
    by_fq[pkg + "." + p.stem] = p
    by_simple.setdefault(p.stem, []).append(pkg + "." + p.stem)
todo = [fq for n in sys.argv[1:] for fq in by_simple.get(n, [])]
seen = set()
while todo:
    fq = todo.pop()
    if fq in seen:
        continue
    seen.add(fq)
    s = files[by_fq[fq]]
    pkg = fq.rsplit(".", 1)[0]
    body = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", s, flags=re.S)
    for imp in re.findall(r"^import (?:static )?([\w.]+?)(?:\.\*)?;", s, re.M):
        if imp in by_fq:
            todo.append(imp)
    for other, p in by_fq.items():
        if other.rsplit(".", 1)[0] == pkg and other != fq and re.search(rf"\b{other.rsplit('.', 1)[1]}\b", body):
            todo.append(other)
for fq in sorted(seen):
    ext = [i for i in re.findall(r"^import (?:static )?([\w.]+?)(?:\.\*)?;", files[by_fq[fq]], re.M)
           if not i.startswith("java.") and i not in by_fq]
    print(f"{fq}\t{by_fq[fq].relative_to(D).as_posix()}\t{','.join(ext)}")
