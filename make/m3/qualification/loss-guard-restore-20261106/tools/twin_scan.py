# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""twin_scan.py <TWINS.tsv> <out.tsv>: content-level check of every retained-variants source twin whose live path
exists on master. Declarations (Java/C/C++ method and function heads, Python def/class, shell functions, workflow job
names) of the retained copy that the live file does not declare are listed: rel, label, missing_count, missing_names.
Both blobs are read with git cat-file --batch (blobs prefetched by OID). Mechanical LSEM comparison; no verdict."""
import re
import subprocess
import sys
from pathlib import Path

REPO = "C:/m3-superset/conv/m3jdk21"
JAVA = re.compile(r"^\s*(?:public|protected|private|static|final|native|synchronized|abstract|default|\s)*"
                  r"[\w<>\[\],.? ]+\s+(\w+)\s*\([^;]*$", re.M)
C = re.compile(r"^(?!\s)(?:[\w*]+\s+)+\**(\w+)\s*\([^;]*\)\s*\{?\s*$", re.M)
PY = re.compile(r"^\s*(?:def|class)\s+(\w+)", re.M)
SH = re.compile(r"^\s*(?:function\s+)?(\w+)\s*\(\)\s*\{", re.M)
YML = re.compile(r"^  (\w[\w-]*):\s*$", re.M)
KEYWORDS = {"if", "for", "while", "switch", "return", "new", "else", "catch", "synchronized", "sizeof", "throw"}


def decls(path, text):
    ext = path.rsplit(".", 1)[-1]
    rx = {"java": JAVA, "c": C, "h": C, "cpp": C, "py": PY, "sh": SH, "yml": YML}.get(ext)
    if rx is None:
        return set()
    return {m.group(1) for m in rx.finditer(text)} - KEYWORDS


rows = [l.rstrip("\n").split("\t") for l in list(open(sys.argv[1], encoding="utf-8"))[1:]]
blobs = sorted({r[3] for r in rows} | {r[4] for r in rows})
listing = Path("logs/twin_blob_list.txt")
listing.write_text("".join(b + "\n" for b in blobs), encoding="utf-8", newline="\n")  # stdin from a file: no pipe deadlock
p = subprocess.Popen(["git", "-C", REPO, "cat-file", "--batch"], stdin=open(listing, "rb"), stdout=subprocess.PIPE)
content = {}
for b in blobs:
    hdr = p.stdout.readline().decode().split()
    if hdr[1] == "missing":
        content[b] = None
        continue
    size = int(hdr[2])
    content[b] = p.stdout.read(size).decode("utf-8", "replace")
    p.stdout.read(1)
out = []
for rel, label, rp, live_b, ret_b in rows:
    if content.get(live_b) is None or content.get(ret_b) is None:
        out.append((rel, label, "?", "blob missing"))
        continue
    missing = sorted(decls(rel, content[ret_b]) - decls(rel, content[live_b]))
    if missing:
        out.append((rel, label, str(len(missing)), ",".join(missing)[:300]))
with open(sys.argv[2], "w", encoding="utf-8", newline="\n") as o:
    o.write("rel\tlabel\tmissing_count\tmissing_declarations\n")
    for r in out:
        o.write("\t".join(r) + "\n")
print("twins", len(rows), "with declarations missing in live", len(out))
