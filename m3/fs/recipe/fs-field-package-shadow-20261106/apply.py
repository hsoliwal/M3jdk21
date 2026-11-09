#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FS_FIELD_PACKAGE_SHADOW v1: rename the M3FS.DefaultRegexFallback field `jdk` to `jdkPattern`.

Receiver: m3/fs/src/main/java/jdk/m3/fs/M3FS.java. The JDK naming rename fe35ea98841 moved the RE2 port to the
package jdk.m3.fs.internal.re2j while M3FS.DefaultRegexFallback kept a field named `jdk` (its lazily compiled
java.util.regex.Pattern). In `jdk.m3.fs.internal.re2j.Pattern.compile(..)` the leftmost name `jdk` is an ambiguous
name, and a field in scope wins over a package (JLS 6.4.2 obscuring, 6.5.2 reclassification): javac 21 reports
`M3FS.java:[661,33] cannot find symbol: variable m3, location: variable jdk of type java.util.regex.Pattern`. Type
contexts (the field type, the local `jdk.m3.fs.internal.re2j.Matcher`) are type names and stay legal; only the
expression context breaks. The field takes the name of its role, the JDK-semantics pattern (jdkPattern), next to
its sibling `re2`; the declaration and its two uses in the class change, nothing else.

EDITS.tsv pins, per file, the sha256 of the preimage (master 0909649e8f7) and of the postimage, and holds one row
per line edit: REPLACE (the preimage line must equal `old` exactly) or INSERT_BEFORE (the line must equal `old`; `new`
goes before it). Line numbers and anchors refer to the preimage; every anchor is checked before any edit. A file
equal to its postimage is already applied (UNCHANGED); a file equal to neither pin is drift and the recipe refuses
(exit 2) without writing. A second run reports changed=0.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

RECIPE = "FS_FIELD_PACKAGE_SHADOW"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "line", "op", "old", "new", "rule")
OPS = ("REPLACE", "INSERT_BEFORE")


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_rows() -> dict[str, list[dict]]:
    """file -> its edit rows; every row of a file carries the same before/after pins."""
    lines = (RECIPE_DIR / EDITS_NAME).read_text(encoding="utf-8").splitlines()
    body = [line for line in lines if line and not line.startswith("#")]
    if not body or tuple(body[0].split("\t")) != EDITS_COLUMNS:
        raise Refused("EDITS.tsv header must be " + "\t".join(EDITS_COLUMNS))
    files: dict[str, list[dict]] = {}
    for line in body[1:]:
        fields = line.split("\t")
        if len(fields) != len(EDITS_COLUMNS):
            raise Refused("EDITS.tsv row has %d fields, need %d: %s" % (len(fields), len(EDITS_COLUMNS), line[:60]))
        row = dict(zip(EDITS_COLUMNS, fields))
        if row["op"] not in OPS or not row["line"].isdigit():
            raise Refused("bad op or line: " + line[:60])
        first = files.setdefault(row["file"], [row])[0]
        if row is not first:
            if (first["before_sha256"], first["after_sha256"]) != (row["before_sha256"], row["after_sha256"]):
                raise Refused("rows of %s disagree on their pins" % row["file"])
            files[row["file"]].append(row)
    return files


def postimage(pre: bytes, rows: list[dict], label: str) -> bytes:
    if b"\r\n" in pre:
        raise Refused("CRLF preimage " + label)
    lines = pre.decode("utf-8").split("\n")
    inserts: dict[int, list[str]] = {}
    replaces: dict[int, str] = {}
    for row in rows:
        index = int(row["line"]) - 1
        if index >= len(lines) or lines[index] != row["old"]:
            raise Refused("anchor mismatch %s:%s (%s)" % (label, row["line"], row["op"]))
        if row["op"] == "INSERT_BEFORE":
            inserts.setdefault(index, []).append(row["new"])
        elif replaces.setdefault(index, row["new"]) != row["new"]:
            raise Refused("two replacements of %s:%s" % (label, row["line"]))
    out = [text for index, line in enumerate(lines) for text in inserts.get(index, []) + [replaces.get(index, line)]]
    return "\n".join(out).encode("utf-8")


def run(root: Path, check: bool) -> int:
    changed = 0
    files = load_rows()
    for path, rows in files.items():
        target = root / path
        on_disk = target.read_bytes()
        digest = sha256(on_disk)
        pins = rows[0]
        if digest == pins["after_sha256"]:
            state = "UNCHANGED"
        elif digest == pins["before_sha256"]:
            post = postimage(on_disk, rows, path)
            if sha256(post) != pins["after_sha256"]:
                raise Refused("postimage drift %s: produced %s, pinned %s" % (path, sha256(post), pins["after_sha256"]))
            state = "CHANGED"
            if not check:
                target.write_bytes(post)
        else:
            raise Refused("drifted preimage %s: on disk %s, pinned %s" % (path, digest, pins["before_sha256"]))
        changed += state == "CHANGED"
        print("%s %s edits=%d %s" % (state, path, len(rows), pins["after_sha256"][:16]))
    print("%s recipe=apply.py %s files=%d changed=%d" % (RECIPE, VERSION, len(files), changed))
    return 1 if check and changed else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
