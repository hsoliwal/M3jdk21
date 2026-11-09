#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Recipe DOC_BANNER_OPENER v1: stop the Synexia license banner from opening a Javadoc comment.

The banner line 1 is "/" followed by only asterisks, so javac/doclint read the whole license block as a doc
comment placed before `package` and report dc.unexpected.comment ("documentation comment not expected here"),
which -Werror turns into a build failure. The recipe rewrites exactly one thing: in a file whose line 1 matches
^/\\*{3,}$ and whose line 2 starts with " * Copyright (c) 20" and names Synexia, the opener "/**" of line 1 becomes
"/* " (same length). Every other byte, line ending and file mode is unchanged. Applying it twice is a no-op.

usage: doc_banner_opener.py [--check] ROOT [ROOT ...]
  default: rewrite matching files and print "REWROTE <path>" per file and a summary line
  --check: change nothing; exit 1 if any file still matches (fixed-point gate)
"""
import os
import re
import sys

LINE1 = re.compile(rb"^/\*{3,}$")
LINE2 = re.compile(rb"^ \* Copyright \(c\) 20[0-9]{2}\b.*Synexia")


def split_first_lines(data):
    first_end = data.find(b"\n")
    if first_end < 0:
        return None
    second_end = data.find(b"\n", first_end + 1)
    line1 = data[:first_end].rstrip(b"\r")
    line2 = data[first_end + 1:second_end if second_end >= 0 else len(data)].rstrip(b"\r")
    return line1, line2


def matches(data):
    lines = split_first_lines(data)
    return lines is not None and LINE1.match(lines[0]) is not None and LINE2.match(lines[1]) is not None


def rewrite(data):
    # "/**" -> "/* ": replace the third byte only.
    return data[:2] + b" " + data[3:]


def walk(roots):
    for root in roots:
        if os.path.isfile(root):
            # An explicitly named file (a generator template) is checked regardless of its suffix.
            yield root
            continue
        for base, dirs, files in os.walk(root):
            dirs.sort()
            for name in sorted(files):
                if name.endswith((".java", ".c", ".h", ".cpp", ".hpp", ".cc")):
                    yield os.path.join(base, name)


def main(argv):
    check = "--check" in argv
    roots = [a for a in argv[1:] if a != "--check"]
    if not roots:
        print(__doc__)
        return 2
    hits = 0
    for path in walk(roots):
        with open(path, "rb") as f:
            data = f.read()
        if not matches(data):
            continue
        hits += 1
        if check:
            print("MATCH " + path)
            continue
        new = rewrite(data)
        assert len(new) == len(data) and not matches(new)
        with open(path, "r+b") as f:
            f.write(new[:3])
        print("REWROTE " + path)
    print(("DOC_BANNER_OPENER_CHECK matches=" if check else "DOC_BANNER_OPENER_APPLY rewritten=") + str(hits))
    return 1 if check and hits else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
