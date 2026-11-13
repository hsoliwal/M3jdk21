"""conflict_class.py <m3-root> <out-dir> <merge.tsv>: classify each CONFLICT row of merge_unix4j.py.
EQUIVALENT = ours and relocated theirs differ only in comments, package/import lines and whitespace (keep ours);
otherwise print the token-level diff size and the first differing lines for review."""
import difflib
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from merge_unix4j import relocate, set_package, target  # noqa: E402


def norm(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    s = re.sub(r"//[^\n]*", "", s)
    return [l.strip() for l in s.splitlines() if l.strip() and not l.strip().startswith(("import ", "package "))]


m3_root, dev = Path(sys.argv[1]), Path(sys.argv[2])
for row in Path(sys.argv[3]).read_text(encoding="utf-8").splitlines():
    kind, rel, tpath = row.split("\t")
    if not kind.startswith("CONFLICT"):
        continue
    t = target(rel)
    pkg = t[1] if isinstance(t, tuple) else None
    theirs = set_package(relocate((dev / rel).read_text(encoding="utf-8")), pkg)
    ours = (m3_root / tpath).read_text(encoding="utf-8")
    a, b = norm(ours), norm(theirs)
    if a == b:
        print(f"EQUIVALENT\t{tpath}")
        continue
    ops = [o for o in difflib.SequenceMatcher(None, a, b, autojunk=False).get_opcodes() if o[0] != "equal"]
    print(f"DIFFERENT\t{tpath}\tours={len(a)} theirs={len(b)} hunks={len(ops)}")
    for tag, i1, i2, j1, j2 in ops[:6]:
        print(f"   {tag} ours[{i1}:{i2}] theirs[{j1}:{j2}]")
        for l in a[i1:min(i2, i1 + 2)]:
            print("     - " + l[:120])
        for l in b[j1:min(j2, j1 + 2)]:
            print("     + " + l[:120])
