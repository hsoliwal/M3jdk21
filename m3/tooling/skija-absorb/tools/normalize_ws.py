# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""normalize_ws.py <dir> [--check] : NORMALIZE_WS v1, the last stage of the Skija leaf-1 absorption (in place,
deterministic). In every .java file under <dir>: LF line ends, no trailing spaces, tabs or CR on any line, and no blank
lines at the end of the file. A final newline is kept when the donor file has one and never added (the rules of
`git apply --whitespace=fix`; jcheck checks trailing whitespace, tabs and CR). The donor checkout has CRLF line ends and
trailing blanks in Javadoc. A tab left inside a line stops the run (jcheck forbids tabs in .java files). Prints the
number of files changed; --check writes nothing and exits 1 when a file would change."""
import os
import sys


def normalize(text):
    lines = text.replace("\r\n", "\n").split("\n")
    out = "\n".join(line.rstrip(" \t\r") for line in lines)
    return out.rstrip("\n") + "\n" if out.endswith("\n") else out


def main():
    root, check = sys.argv[1], "--check" in sys.argv[2:]
    changed, tabs = [], []
    for dirpath, dirs, files in os.walk(root):
        dirs.sort()
        for f in sorted(files):
            if not f.endswith(".java"):
                continue
            p = os.path.join(dirpath, f)
            with open(p, encoding="utf-8", newline="") as r:
                text = r.read()
            new = normalize(text)
            if "\t" in new:
                tabs.append(p)
            if new != text:
                changed.append(p)
                if not check:
                    with open(p, "w", encoding="utf-8", newline="") as w:
                        w.write(new)
    print(f"files_changed={len(changed)} tabs={len(tabs)}")
    if tabs:
        print("TAB", *tabs[:5], sep="\n")
        sys.exit(1)
    if check and changed:
        sys.exit(1)


if __name__ == "__main__":
    main()
