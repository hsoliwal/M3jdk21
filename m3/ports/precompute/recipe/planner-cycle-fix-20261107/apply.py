#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe PLANNER_CYCLE_FIX v1: hash-pinned edits of M3IndexPrecomputePlanner (EDITS.tsv + edits/<id>.old.txt|new.txt).

Every EDITS.tsv row names a target file (repository root relative), the sha256 of the whole file before and after all
of its rows, and the sha256 of its old and new payloads. A target whose bytes hash to the after value is a fixed point
(not written); one that hashes to the before value gets its rows applied in table order, each old payload replaced by
its new payload and required to occur exactly once, and the result must hash to the after value before it is written;
any other content is a drifted preimage and the recipe refuses (exit 2) without writing. Payload files are checked
against their pinned sha256 first. --check writes nothing and exits 1 when a run would change a file. Prints one TSV
row per target: path, state, current sha256, after sha256.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
EDITS = HERE / "EDITS.tsv"
COLUMNS = ("id", "file", "before_sha256", "after_sha256", "old_sha256", "new_sha256", "note")


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def rows() -> list[dict]:
    lines = [line for line in EDITS.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]
    if tuple(lines[0].split("\t")) != COLUMNS:
        raise Refused("EDITS.tsv header is not " + "\t".join(COLUMNS))
    return [dict(zip(COLUMNS, line.split("\t"))) for line in lines[1:]]


def payload(row: dict, kind: str) -> bytes:
    data = (HERE / "edits" / ("%s.%s.txt" % (row["id"], kind))).read_bytes()
    if sha256(data) != row[kind + "_sha256"]:
        raise Refused("payload %s.%s.txt does not hash to EDITS.tsv" % (row["id"], kind))
    return data


def apply_file(root: Path, path: str, edits: list[dict], check: bool) -> tuple[str, str, str]:
    before, after = edits[0]["before_sha256"], edits[0]["after_sha256"]
    if any(row["before_sha256"] != before or row["after_sha256"] != after for row in edits):
        raise Refused("rows of %s disagree on before/after" % path)
    target = root / path
    data = target.read_bytes()
    current = sha256(data)
    if current == after:
        return "FIXED_POINT", current, after
    if current != before:
        raise Refused("%s hashes to %s, neither the pinned before nor after" % (path, current))
    for row in edits:
        old, new = payload(row, "old"), payload(row, "new")
        if data.count(old) != 1:
            raise Refused("edit %s: old payload occurs %d times in %s" % (row["id"], data.count(old), path))
        data = data.replace(old, new)
    if sha256(data) != after:
        raise Refused("%s after the edits hashes to %s, not the pinned after" % (path, sha256(data)))
    if not check:
        target.write_bytes(data)
    return "APPLIED" if not check else "CHECK_PENDING", current, after


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", type=Path, default=HERE.parents[4], help="repository root (default: five levels up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        by_file: dict = {}
        for row in rows():
            by_file.setdefault(row["file"], []).append(row)
        pending = 0
        for path, edits in by_file.items():
            state, current, after = apply_file(args.root.resolve(), path, edits, args.check)
            pending += state == "CHECK_PENDING"
            print("%s\t%s\t%s\t%s\tedits=%d" % (path, state, current, after, len(edits)))
        print("APPLY recipe=planner-cycle-fix-20261107/apply.py v1 files=%d edits=%d pending=%d"
              % (len(by_file), sum(len(e) for e in by_file.values()), pending))
        return 1 if args.check and pending else 0
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
