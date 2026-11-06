# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""mutate_once.py <file> <old> <new> : MUTANT APPLY v1 (CPU). Replaces the single occurrence of <old> by <new>
in <file> (exit 2 when <old> is absent or ambiguous). <old>/<new> accept the escapes \\n and \\uXXXX written as
@n and @uXXXX, so shell layers cannot collapse backslashes. Restore with git checkout -- <file>."""
import re
import sys

ESCAPE = re.compile(r"@u([0-9a-fA-F]{4})|@n")


def unescape(text):
    return ESCAPE.sub(lambda m: "\\u" + m.group(1) if m.group(1) else "\\n", text)


def main():
    path, old, new = sys.argv[1], unescape(sys.argv[2]), unescape(sys.argv[3])
    with open(path, encoding="utf-8") as f:
        source = f.read()
    if source.count(old) != 1:
        print("MUTANT_NOT_APPLIED occurrences=%d" % source.count(old))
        sys.exit(2)
    with open(path, "w", encoding="utf-8", newline="\n") as w:
        w.write(source.replace(old, new))
    print("MUTANT_APPLIED")


main()
