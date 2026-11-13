#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Member-level delta of two `javap -c -p -constants` listings of one class (master vs head).

Constant-pool indexes (#12, #34:#56) shift whenever a constant is added, so they are stripped and the symbolic
`// Method ...` comments javap prints after each reference are compared instead (runs of blanks collapse, since
javap pads after the index). Each member (field or method header plus its Code block) becomes one normalized record; the result lists ADDED, REMOVED and CHANGED members and
the count of identical ones. Exit 0 always; one TSV row per member that differs.

usage: python -I member_delta.py BASE_JAVAP HEAD_JAVAP CLASS_LABEL
"""
import re
import sys

CP_REF = re.compile(r"#\d+(:#\d+)?")
SPACE = re.compile(r"\s+")
MEMBER = re.compile(r"^  (?!\s)(\S.*)$")


def members(path):
    out, current, body = {}, None, []
    for raw in open(path, encoding="utf-8", errors="replace"):
        line = raw.rstrip("\n")
        header = MEMBER.match(line)
        if header and not line.startswith("    "):
            if current is not None:
                out[current] = "\n".join(body)
            current, body = header.group(1).strip(), []
        elif current is not None and line.strip() not in ("", "}"):
            body.append(SPACE.sub(" ", CP_REF.sub("#", line.strip())))
    if current is not None:
        out[current] = "\n".join(body)
    return out


def main() -> int:
    base, head, label = members(sys.argv[1]), members(sys.argv[2]), sys.argv[3]
    same = 0
    for name in sorted(set(base) | set(head)):
        if name not in head:
            print("%s\tREMOVED\t%s" % (label, name))
        elif name not in base:
            print("%s\tADDED\t%s" % (label, name))
        elif base[name] != head[name]:
            print("%s\tCHANGED\t%s" % (label, name))
        else:
            same += 1
    print("%s\tIDENTICAL_MEMBERS\t%d" % (label, same))
    return 0


if __name__ == "__main__":
    sys.exit(main())
