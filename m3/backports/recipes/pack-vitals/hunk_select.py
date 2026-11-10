# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""hunk_select.py <unified diff> <keep regex> <out patch> <audit tsv> [drop-block regex] [marker file]

HUNK_SELECT v1.2 (2026-11-10) is a declared donor-feature extractor. From a vendor's diff (base -> vendor) it takes
one feature family without the vendor's other changes.

- A hunk is KEPT when one of its changed lines matches the keep regex (case-insensitive), or contains a vendor date
  marker listed in the marker file (one marker per line, such as "SapMachine 2026-10-01"). The marker file holds the
  markers found in the feature's own commits.
- v1.1, mixed hunks: inside a kept hunk, a block of added lines that starts with a line matching the drop-block
  regex is removed. The block runs through the following added lines up to and including the first blank line at
  #if depth 0. v1.2: an #endif closes only an #if opened inside the block; an #endif that closes an outer #if ends
  the block and is kept. The hunk header's new-line count is recomputed. A kept hunk left with no change is
  dropped.
- The audit TSV lists every hunk with its decision and first changed line, plus every stripped block."""
import re
import sys

src, keep_rx, out, audit = sys.argv[1:5]
drop_rx = re.compile(sys.argv[5], re.I) if len(sys.argv) > 5 and sys.argv[5] else None
markers = [l.strip() for l in open(sys.argv[6], encoding="utf-8") if l.strip()] if len(sys.argv) > 6 else []
KEEP = re.compile(keep_rx, re.I)
text = open(src, encoding="utf-8", errors="surrogateescape").read().split("\n")
files, cur = [], None
for line in text:
    if line.startswith("diff --git "):
        cur = {"head": [line], "hunks": []}
        files.append(cur)
    elif cur is not None and not cur["hunks"] and not line.startswith("@@"):
        cur["head"].append(line)
    elif line.startswith("@@"):
        cur["hunks"].append([line])
    elif cur is not None and cur["hunks"]:
        cur["hunks"][-1].append(line)


def strip_blocks(h, name, rows):
    body, out_lines, i = h[1:], [], 0
    while i < len(body):
        l = body[i]
        if drop_rx and l.startswith("+") and drop_rx.search(l[1:]):
            j, depth = i, 0
            while j < len(body) and body[j].startswith("+"):
                s = body[j][1:].lstrip()
                if s.startswith("#if"):
                    depth += 1
                elif s.startswith("#endif"):
                    if depth == 0:
                        break  # v1.2: this #endif closes an #if opened before the block; keep it
                    depth -= 1
                    j += 1
                    if depth == 0:
                        break
                    continue
                j += 1
                if s.strip() == "" and depth == 0 and j - 1 > i:
                    break
            rows.append(f"{name}\t{h[0].split(' ')[1]}\tSTRIP_BLOCK\t{l[1:].strip()[:100]}")
            i = j
            continue
        out_lines.append(l)
        i += 1
    m = re.match(r"@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@(.*)", h[0])
    old_n = sum(1 for l in out_lines if l[:1] in (" ", "-") or l == "")
    new_n = sum(1 for l in out_lines if l[:1] in (" ", "+") or l == "")
    head = f"@@ -{m.group(1)},{old_n} +{m.group(3)},{new_n} @@{m.group(5)}"
    return [head] + out_lines


rows, patch = [], []
for f in files:
    name = f["head"][0].split(" b/", 1)[-1]
    kept = []
    for h in f["hunks"]:
        while h and h[-1] == "":
            h = h[:-1]
        changed = [l[1:] for l in h[1:] if l[:1] in "+-"]
        k = any(KEEP.search(l) for l in changed) or any(mk in l for mk in markers for l in changed)
        first = next((l.strip() for l in changed if l.strip()), "")[:100]
        rows.append(f"{name}\t{h[0].split(' ')[1]}\t{'KEEP' if k else 'DROP'}\t{first}")
        if k:
            h2 = strip_blocks(h, name, rows)
            if any(l[:1] in "+-" for l in h2[1:]):
                kept.append(h2)
    if kept:
        patch += f["head"] + [l for h in kept for l in h]
open(out, "w", encoding="utf-8", errors="surrogateescape", newline="\n").write("\n".join(patch) + "\n")
open(audit, "w", encoding="utf-8", newline="\n").write("file\told_start\tdecision\tfirst_changed_line\n" + "\n".join(rows) + "\n")
print("HUNK_SELECT kept", sum(r.split("\t")[2] == "KEEP" for r in rows), "dropped", sum(r.split("\t")[2] == "DROP" for r in rows),
      "stripped", sum(r.split("\t")[2] == "STRIP_BLOCK" for r in rows))
