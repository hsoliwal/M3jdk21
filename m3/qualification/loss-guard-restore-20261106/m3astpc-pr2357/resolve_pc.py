"""resolve_pc.py <merged-diff3.java> <out.java> : RETAINED_3WAY_RESOLVE M3ASTPC/pr2357 v1.
Hunk 1 (option fields, empty base): union, master's hint fields then pr2357's relation fields.
Hunk 2 (validation before open): master's hint checks, then pr2357's relation checks, then pr2357's
`open(task, retainSyntax || relations != null)`, which supersedes the base open line that master kept.
Refuses unless there are exactly these two hunks with the expected shapes."""
import re
import sys

text = open(sys.argv[1], encoding="utf-8", newline="").read()
pat = re.compile(r"<<<<<<< [^\n]*\n(.*?)\|\|\|\|\|\|\| [^\n]*\n(.*?)=======\n(.*?)>>>>>>> [^\n]*\n", re.S)
hunks = list(pat.finditer(text))
if len(hunks) != 2:
    sys.exit(f"REFUSE hunks={len(hunks)}")
OPEN_BASE = "        Session session = (Session) open(task, retainSyntax);\n"
OPEN_NEW = "        Session session = (Session) open(task, retainSyntax || relations != null);\n"


def fix(m):
    ours, base, theirs = m.group(1), m.group(2), m.group(3)
    if base == "":
        return ours + theirs
    if base == OPEN_BASE and ours.endswith(OPEN_BASE) and theirs.endswith(OPEN_NEW):
        return ours[: -len(OPEN_BASE)] + theirs
    sys.exit("REFUSE unexpected hunk shape")


out = pat.sub(fix, text)
if "<<<<<<<" in out or ">>>>>>>" in out:
    sys.exit("REFUSE markers left")
open(sys.argv[2], "w", encoding="utf-8", newline="").write(out)
print("RESOLVED hunks=2")
