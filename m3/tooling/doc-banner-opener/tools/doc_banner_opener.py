#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Recipe DOC_BANNER_OPENER v1.1: stop the Synexia license banner from opening a Javadoc comment.

The banner line is "/" followed by only asterisks, so javac/doclint read the whole license block as a doc
comment placed before `package` and report dc.unexpected.comment ("documentation comment not expected here"),
which -Werror turns into a build failure. The recipe rewrites exactly one thing: where the banner line matches
^/\\*{3,}$ and the next line starts with " * Copyright (c) 20" and names Synexia, the opener "/**" becomes
"/* " (same length). The banner line is line 1, or (v1.1) the first line after leading "//" comment lines such
as an SPDX line. Every other byte, line ending and file mode is unchanged. Applying it twice is a no-op.

usage: doc_banner_opener.py [--check] ROOT [ROOT ...]
  default: rewrite matching files and print "REWROTE <path>" per file and a summary line
  --check: change nothing; exit 1 if any file still matches (fixed-point gate)
"""
import os
import re
import sys

LINE1 = re.compile(rb"^/\*{3,}$")
LINE2 = re.compile(rb"^ \* Copyright \(c\) 20[0-9]{2}\b.*Synexia")


def banner_offset(data):
    """Byte offset of the banner line (line 1, or the first line after leading // lines), or -1."""
    start = 0
    while data.startswith(b"//", start):
        end = data.find(b"\n", start)
        if end < 0:
            return -1
        start = end + 1
    first_end = data.find(b"\n", start)
    if first_end < 0:
        return -1
    second_end = data.find(b"\n", first_end + 1)
    line1 = data[start:first_end].rstrip(b"\r")
    line2 = data[first_end + 1:second_end if second_end >= 0 else len(data)].rstrip(b"\r")
    return start if LINE1.match(line1) and LINE2.match(line2) else -1


def matches(data):
    return banner_offset(data) >= 0


def rewrite(data):
    # "/**" -> "/* ": replace the third byte of the banner line only.
    at = banner_offset(data)
    return data[:at + 2] + b" " + data[at + 3:]


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
            f.write(new)
        print("REWROTE " + path)
    print(("DOC_BANNER_OPENER_CHECK matches=" if check else "DOC_BANNER_OPENER_APPLY rewritten=") + str(hits))
    return 1 if check and hits else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
