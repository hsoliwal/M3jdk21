"""hunk_kinds.py <file>... : classifies the diff3 conflict hunks of merged files (MERGE_HUNK_KINDS v1).
ADD_ADD: empty base section. THEIRS_DUP: theirs text already contained in ours. OURS_DUP: ours text contained in theirs
(theirs is a superset of ours at that spot). MODIFY: both sides changed base lines differently. Prints per file the
counts and, with -v, each MODIFY hunk's first lines."""
import re
import sys

verbose = "-v" in sys.argv
pat = re.compile(r"<<<<<<< [^\n]*\n(.*?)\|\|\|\|\|\|\| [^\n]*\n(.*?)=======\n(.*?)>>>>>>> [^\n]*\n", re.S)
for path in [a for a in sys.argv[1:] if a != "-v"]:
    text = open(path, encoding="utf-8", errors="replace").read()
    counts = {"ADD_ADD": 0, "THEIRS_DUP": 0, "OURS_DUP": 0, "MODIFY": 0}
    shown = []
    for m in pat.finditer(text):
        o, b, t = m.group(1), m.group(2), m.group(3)
        if t.strip() and t in o:
            counts["THEIRS_DUP"] += 1
        elif o.strip() and o in t:
            counts["OURS_DUP"] += 1
        elif not b:
            counts["ADD_ADD"] += 1
        else:
            counts["MODIFY"] += 1
            shown.append((o.strip().splitlines()[:2], t.strip().splitlines()[:2]))
    print(f"{path}\t" + " ".join(f"{k}={v}" for k, v in counts.items()))
    if verbose:
        for o, t in shown[:3]:
            print("    ours:  ", " | ".join(x[:70] for x in o))
            print("    theirs:", " | ".join(x[:70] for x in t))
