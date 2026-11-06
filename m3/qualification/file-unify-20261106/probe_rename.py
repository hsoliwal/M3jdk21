"""Scratch probe for FILE_UNIFY v1 gates (never committed): measure the unified module with the held pairs resolved one
hypothetical way. The m3/file-side pair (NIO boundary) is renamed M3File -> M3NioFile and M3FileDelta -> M3NioFileDelta
in the files that came from m3/file (whole-word tokens), and the m3/ports/file-side pair is restored under its own
name. This is NOT the owner decision (NEC.tsv); it only shows what compiles and what stays byte-identical once a
decision exists.

usage: python -I probe_rename.py <probe worktree root> <m3/file file list at the base commit (one path per line)>
"""
import re
import shutil
import sys
from pathlib import Path

root = Path(sys.argv[1])
file_side = [line.strip() for line in Path(sys.argv[2]).read_text().splitlines() if line.strip()]
held = root / "m3/qualification/file-unify-20261106/held"
pkg = root / "m3/file/src/main/java/jdk/m3/file"
RENAMES = ((re.compile(r"\bM3FileDelta\b"), "M3NioFileDelta"), (re.compile(r"\bM3File\b"), "M3NioFile"))


def rename(text):
    for pattern, new in RENAMES:
        text = pattern.sub(new, text)
    return text


changed = 0
for rel in file_side:
    path = root / rel
    if not rel.endswith(".java") or not path.is_file():
        continue
    data = path.read_text(encoding="utf-8")
    out = rename(data)
    if out != data:
        path.write_text(out, encoding="utf-8", newline="")
        changed += 1
        print("RENAMED-TOKENS " + rel)
for name, new in (("M3File", "M3NioFile"), ("M3FileDelta", "M3NioFileDelta")):
    source = (held / "file/src/main/java/jdk/m3/file" / (name + ".java")).read_text(encoding="utf-8")
    (pkg / (new + ".java")).write_text(rename(source), encoding="utf-8", newline="")
    shutil.copyfile(held / "ports-file/src/main/java/jdk/m3/file" / (name + ".java"), pkg / (name + ".java"))
    print("PLACED %s.java (m3/file side, renamed) and %s.java (m3/ports/file side, verbatim)" % (new, name))
print("PROBE files_with_renamed_tokens=%d" % changed)
