# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""retire_profile.py <root> <profile_id> <out_dir> [--apply]

RETIRE_PROFILE v1 (M3jdk21 standalone): find every pom.xml under <root> holding <profile><id>PROFILE_ID</id>...,
patternize the blocks (whitespace-normalized SHA-256 per block -> variants), and with --apply remove each block
exactly (and a <profiles> element left empty), keeping every other byte. Fail-closed: a pom whose block is not one
balanced <profile>...</profile> element, or that no longer parses as XML after the edit, is not written.
Outputs: VARIANTS.tsv (variant sha, count, sample pom), POMS.tsv (pom, variant, action)."""
import hashlib
import os
import re
import sys
import xml.etree.ElementTree as ET

root, pid, out = sys.argv[1], sys.argv[2], sys.argv[3]
apply = "--apply" in sys.argv
BLOCK = re.compile(r"[ \t]*<profile>\s*<id>" + re.escape(pid) + r"</id>.*?</profile>[ \t]*\r?\n", re.S)
EMPTY = re.compile(r"[ \t]*<profiles>\s*</profiles>[ \t]*\r?\n")
variants, rows = {}, []
for dirpath, dirs, files in os.walk(root):
    dirs[:] = [d for d in dirs if d not in (".git", "target")]
    if "pom.xml" not in files:
        continue
    p = os.path.join(dirpath, "pom.xml")
    s = open(p, encoding="utf-8", newline="").read()
    if f"<id>{pid}</id>" not in s:
        continue
    rel = os.path.relpath(p, root).replace(os.sep, "/")
    m = list(BLOCK.finditer(s))
    if len(m) != 1 or m[0].group(0).count("<profile>") != 1:
        rows.append((rel, "-", f"HELD blocks={len(m)}"))
        continue
    norm = re.sub(r"\s+", " ", m[0].group(0)).strip()
    h = hashlib.sha256(norm.encode()).hexdigest()[:16]
    variants.setdefault(h, []).append(rel)
    new = s[:m[0].start()] + s[m[0].end():]
    new = EMPTY.sub("", new, count=1)
    try:
        ET.fromstring(new.encode("utf-8"))
    except ET.ParseError as e:
        rows.append((rel, h, f"HELD parse {e}"))
        continue
    if apply:
        open(p, "w", encoding="utf-8", newline="").write(new)
    rows.append((rel, h, "RETIRED" if apply else "PLANNED"))
os.makedirs(out, exist_ok=True)
with open(os.path.join(out, "VARIANTS.tsv"), "w", encoding="utf-8", newline="\n") as w:
    w.write("variant\tcount\tsample\n" + "".join(f"{h}\t{len(v)}\t{sorted(v)[0]}\n" for h, v in sorted(variants.items(), key=lambda kv: -len(kv[1]))))
with open(os.path.join(out, "POMS.tsv"), "w", encoding="utf-8", newline="\n") as w:
    w.write("pom\tvariant\taction\n" + "".join("\t".join(r) + "\n" for r in sorted(rows)))
held = sum(1 for r in rows if r[2].startswith("HELD"))
print(f"poms={len(rows)} variants={len(variants)} held={held} {'applied' if apply else 'dry-run'}")
