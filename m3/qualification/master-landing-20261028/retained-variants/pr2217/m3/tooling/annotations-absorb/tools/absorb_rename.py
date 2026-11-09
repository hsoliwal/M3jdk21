"""Mechanical absorption of a donor annotation package into a jdk.m3 module (annotations convergence, CANONICAL rows).

Every .java file of the donor package is copied with three edits and nothing else:
  1. the package name, in code and Javadoc, is renamed (word-boundary match of the dotted name);
  2. the Apache-2.0 header gains the modification notice section 4(b) requires;
  3. the file moves to the module source tree.
Usage: python -I absorb_rename.py <donor src/main/java> <from.pkg> <to.pkg> <module classes dir> <notice>
(" | " in the notice starts a new comment line).
Prints one TSV row per file: donor path, donor sha256, output path, output sha256.
"""
import hashlib
import re
import sys
from pathlib import Path

src, old, new, out, notice = sys.argv[1:6]
donor = Path(src) / old.replace(".", "/")
target = Path(out) / new.replace(".", "/")
target.mkdir(parents=True, exist_ok=True)
pattern = re.compile(r"(?<![\w.])" + re.escape(old) + r"(?![\w])")
for f in sorted(donor.glob("*.java")):
    text = f.read_text(encoding="utf-8")
    renamed, n = pattern.subn(new, text)
    assert n > 0, f
    head = renamed.index(" */\n")
    assert renamed.startswith("/*\n") and "Apache License, Version 2.0" in renamed[:head], f
    lines = "".join(" * " + part + "\n" for part in notice.split(" | "))   # " | " separates notice lines
    renamed = renamed[:head] + " *\n" + lines + renamed[head:]
    dest = target / f.name
    dest.write_text(renamed, encoding="utf-8", newline="\n")
    print("\t".join([f.as_posix(), hashlib.sha256(f.read_bytes()).hexdigest(), dest.as_posix(),
                     hashlib.sha256(dest.read_bytes()).hexdigest()]))
