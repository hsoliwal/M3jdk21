"""memdiff.py <dir> <name>... : for each <dir>/<name>.{o,b,t} (master, PR base, PR head) report, by members.py, the
members the PR adds that master lacks (MISSING) and the members the PR changed where master still has the base
body (APPLY_CHANGE) or something else (MASTER_EVOLVED). A member is keyed by enclosing type, name and the last
part of its declaration head (overloads stay distinct)."""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from members import members  # noqa: E402


def key(m):
    return (m[0], m[2], re.sub(r"\s+", " ", m[4].split("{")[0])[-140:])


def load(path):
    text = path.read_text(encoding="utf-8")
    return {key(m): (m, text[m[6]:m[7]]) for m in members(text)}


d = Path(sys.argv[1])
for name in sys.argv[2:]:
    o, b, t = (load(d / f"{name}.{s}") for s in "obt")
    missing = [k for k in t if k not in b and k not in o]
    changed = [k for k in t if k in b and t[k][1] != b[k][1]]
    print(f"== {name}: PR adds {sum(1 for k in t if k not in b)}, MISSING on master {len(missing)}, PR changed {len(changed)}")
    for k in missing:
        print(f"  MISSING\t{k[0]}\t{k[1]}\t{t[k][0][3]}")
    for k in changed:
        state = "APPLY_CHANGE" if k in o and o[k][1] == b[k][1] else ("ALREADY" if k in o and o[k][1] == t[k][1]
                                                                       else "MASTER_EVOLVED")
        print(f"  {state}\t{k[0]}\t{k[1]}")
