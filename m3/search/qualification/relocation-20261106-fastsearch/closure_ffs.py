"""closure_ffs.py : LEAF_CLOSURE over synexia-fastsearch main sources (ffs/). A class joins when every import is java.*
or another joined class of the module, it has no lombok annotation and no reference to a non-joined module class.
Prints JOIN rows (size, package, class) and the reason for the first blocker of each excluded class."""
import re
from pathlib import Path

D = Path(__file__).parent / "ffs"
files = {p: p.read_text(encoding="utf-8") for p in D.rglob("*.java")}
fq = {}
for p, s in files.items():
    m = re.search(r"^package ([\w.]+);", s, re.M)
    fq[m.group(1) + "." + p.stem] = p
keep = set(fq)
why = {}
changed = True
while changed:
    changed = False
    for name in sorted(keep):
        s = files[fq[name]]
        pkg = name.rsplit(".", 1)[0]
        imps = re.findall(r"^import (?:static )?([\w.]+?)(?:\.\*)?;", s, re.M)
        bad = [i for i in imps if not i.startswith("java.") and i != "com.synexia.common.progress.ProgressMonitor" and not any(i == k or i.startswith(k + ".") for k in keep)]
        if "@Slf4j" in s or "lombok" in s:
            bad.append("lombok")
        body = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", s, flags=re.S)
        same = [n for n in fq if n.rsplit(".", 1)[0] == pkg and n not in keep and n != name
                and re.search(rf"\b{n.rsplit('.', 1)[1]}\b", body)]
        if bad or same:
            keep.discard(name)
            why[name] = (bad + same)[0]
            changed = True
for n in sorted(keep):
    print(f"JOIN\t{len(files[fq[n]].splitlines())}\t{n}")
for n, w in sorted(why.items()):
    print(f"OUT\t{n}\t{w}")
