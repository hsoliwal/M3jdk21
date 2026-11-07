#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FILE_DURABLE_EXTENT_STORE_OWNER_EDITS v1: owner edits of the durable extent store leaf in m3/file.

Receivers: m3/file/verify.py (the closed contract inventory of src/test/java gains the three JUnit classes of
recipe durable-extent-store-20261107 in maven_junit_sources: M3ContentExtentTest, M3DurableExtentStoreTest,
M3DurableExtentStoreOracleTest; they run in the surefire test phase, not as host or image mains) and
m3/file/README.md (one section: the store, its durability limit and the recipe). Derived from
m3/fs/recipe/fs-field-package-shadow-20261106/apply.py v1 (FS_FIELD_PACKAGE_SHADOW): only this docstring, the
recipe name and the preimage commit in the next paragraph differ.

EDITS.tsv pins, per file, the sha256 of the preimage (master 04fae52d11dd) and of the postimage, and holds one row
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

RECIPE = "FILE_DURABLE_EXTENT_STORE_OWNER_EDITS"
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
