# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""doclint_repair.py <javac -Xdoclint log> [mirror root ...]

DOCLINT_REPAIR v1 (2026-11-09) is a declared, comment-only repair of donor javadoc for M3's doclint build (make
images, -Xdoclint with -Werror). It reads `<file>:<line>: error: <message>` rows and fixes only that line:

- `malformed HTML`: a `<` that does not open a tag becomes `&lt;`;
- `unexpected heading used: <Hn>, compared to implicit preceding heading: <Hm>`: <hn>...</hn> becomes the expected
  level, one below the implicit heading (member docs follow H3, so use H4);
- `heading used out of sequence: <Hn>, compared to implicit preceding heading: <H1>`: <hn> becomes <h2>;
- `reference not found` on an `@see` line: the line is removed (the target is not in M3).

Bytecode never changes. Each mirror root receives the same edit to the file at the same path relative to
src/jdk.m3.collections/share/classes (for example the relocation output). Any other message stops the tool."""
import re
import sys
from collections import defaultdict

log = sys.argv[1]
mirrors = sys.argv[2:]
edits = defaultdict(list)
for line in open(log, encoding="utf-8", errors="replace"):
    m = re.match(r"(.+?\.java):(\d+): error: (.*)", line.strip())
    if m:
        edits[m.group(1).replace("\\", "/")].append((int(m.group(2)), m.group(3)))


def fix(text_line, msg):
    if msg.startswith("malformed HTML"):
        return re.sub(r"<(?![A-Za-z/!])", "&lt;", text_line)
    h = re.match(r"(?:unexpected heading used|heading used out of sequence): <H(\d)>, compared to implicit preceding heading: <H(\d)>", msg)
    if h:
        level = int(h.group(2)) + 1
        return re.sub(r"<(/?)h" + h.group(1) + ">", lambda g: f"<{g.group(1)}h{level}>", text_line, flags=re.I)
    if msg.startswith("reference not found") and "@see" in text_line:
        return None
    raise SystemExit(f"DOCLINT_REPAIR: unsupported message {msg!r}")


for path, rows in edits.items():
    rel = path.split("src/jdk.m3.collections/share/classes/", 1)[1]
    targets = [path] + [m.rstrip("/") + "/" + rel for m in mirrors]
    for target in targets:
        lines = open(target, encoding="utf-8").read().split("\n")
        for n, msg in sorted(rows, reverse=True):
            new = fix(lines[n - 1], msg)
            if new is None:
                del lines[n - 1]
            else:
                lines[n - 1] = new
        with open(target, "w", encoding="utf-8", newline="\n") as w:
            w.write("\n".join(lines))
    print("DOCLINT_REPAIR", rel, len(rows))
