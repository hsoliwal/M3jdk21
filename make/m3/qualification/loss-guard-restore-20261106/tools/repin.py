# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""repin.py <worktree> <workflow> <REPIN.tsv>: recipe WORKFLOW_BLOB_REPIN v1.
A workflow that guards its inputs with  test "$(git hash-object <path>)" = '<sha>'  lines is re-pinned to the blob ids
the same paths have in the worktree (git hash-object of the checked-out file). Only the 40-hex literal of a matching
line changes; a path absent from the worktree refuses. Rows: path, old_blob, new_blob, changed."""
import re
import subprocess
import sys
from pathlib import Path

wt, wf, out = sys.argv[1:4]
PIN = re.compile(r'''(test "\$\(git hash-object (?P<path>[^)"]+)\)" = ')(?P<sha>[0-9a-f]{40})(')''')
text = Path(wt, wf).read_text(encoding="utf-8")
rows = []


def repin(m):
    path = m.group("path")
    if not Path(wt, path).is_file():
        raise SystemExit(f"REFUSED: pinned path not in the worktree: {path}")
    new = subprocess.run(["git", "-C", wt, "hash-object", path], capture_output=True, text=True, check=True).stdout.strip()
    rows.append((path, m.group("sha"), new, "yes" if new != m.group("sha") else "no"))
    return m.group(1) + new + m.group(4)


new_text = PIN.sub(repin, text)
if not rows:
    raise SystemExit("REFUSED: no pin lines found")
Path(wt, wf).write_text(new_text, encoding="utf-8", newline="\n")
with open(out, "w", encoding="utf-8", newline="\n") as o:
    o.write("path\told_blob\tnew_blob\tchanged\n")
    for r in rows:
        o.write("\t".join(r) + "\n")
print("repinned", sum(r[3] == "yes" for r in rows), "of", len(rows), "pins in", wf)
