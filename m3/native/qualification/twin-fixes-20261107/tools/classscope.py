#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Class-file scope gate of recipe qualification/twin-fixes-20261107 (stdlib + the JDK's javap).

Compares two class trees (master build vs head build of the same module): every class file is IDENTICAL (bytes),
NEW, REMOVED or CHANGED. A CHANGED class is split further with javap: LINES_ONLY when javap -c -p -constants is
identical after constant-pool indexes (#n) are dropped (only the LineNumberTable moved, e.g. a nested class declared
below an edited member), else MEMBERS: the javap -p member set (ADDED/REMOVED) and, per member, whether its normalised
bytecode CHANGED. Prints one line per non-identical class and one summary line.
usage: classscope.py MASTER_CLASSES HEAD_CLASSES [--java-home DIR] [--tmp DIR]"""
from __future__ import annotations

import argparse
import re
import subprocess
from pathlib import Path

CP_INDEX = re.compile(r"#\d+(,\d+)?")
MEMBER = re.compile(r"^  (\S.*\S);$")


def classes(root: Path) -> dict:
    return {p.relative_to(root).as_posix(): p.read_bytes() for p in root.rglob("*.class")}


def javap(java_home: str, tmp: str, root: Path, rel: str, *flags: str) -> str:
    name = rel[:-len(".class")].replace("/", ".")
    return subprocess.run([str(Path(java_home) / "bin" / "javap"), "-J-Djava.io.tmpdir=" + tmp, *flags, "-cp",
                           str(root), name], check=True, capture_output=True, text=True).stdout


def members(text: str) -> dict:
    """member declaration -> normalised body lines (javap -c -p output)."""
    out, current = {}, None
    for line in text.splitlines()[1:]:
        found = MEMBER.match(line)
        if found and not line.startswith("   "):
            current = found.group(1)
            out[current] = []
        elif current is not None:
            out[current].append(CP_INDEX.sub("#", line))
    return out


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("master")
    parser.add_argument("head")
    parser.add_argument("--java-home", default="C:/Program Files/Java/jdk-21")
    parser.add_argument("--tmp", default=".")
    args = parser.parse_args()
    a, b = Path(args.master), Path(args.head)
    old, new = classes(a), classes(b)
    counts = {"IDENTICAL": 0, "NEW": 0, "REMOVED": 0, "LINES_ONLY": 0, "MEMBERS": 0}
    for rel in sorted(set(old) | set(new)):
        if rel in old and rel in new and old[rel] == new[rel]:
            counts["IDENTICAL"] += 1
            continue
        if rel not in old or rel not in new:
            kind = "NEW" if rel not in old else "REMOVED"
            counts[kind] += 1
            print("%s %s" % (kind, rel))
            continue
        before = members(javap(args.java_home, args.tmp, a, rel, "-c", "-p", "-constants"))
        after = members(javap(args.java_home, args.tmp, b, rel, "-c", "-p", "-constants"))
        if before == after:
            counts["LINES_ONLY"] += 1
            print("LINES_ONLY %s (javap -c -p identical modulo constant-pool indexes)" % rel)
            continue
        counts["MEMBERS"] += 1
        added = [m for m in after if m not in before]
        removed = [m for m in before if m not in after]
        changed = [m for m in after if m in before and before[m] != after[m]]
        print("MEMBERS %s added=%s removed=%s bytecode_changed=%s unchanged=%d" % (
            rel, added, removed, changed, sum(1 for m in after if m in before and before[m] == after[m])))
    print("CLASSSCOPE " + " ".join("%s=%d" % (k.lower(), v) for k, v in counts.items()))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
