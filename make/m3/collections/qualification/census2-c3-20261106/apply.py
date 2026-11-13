#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS2_C3 v1: the APPLICABLE hunks of census pass-2 leaf P2-075 (m3/collections, group C3).

Derived from make/m3/collections/qualification/census-small-batch-20261106/apply.py v1 (#2565): anchored "sub" edits, every
anchor exactly once, LF payloads re-joined with the preimage's line ending. Every rule names the git blob sha of its
preimage at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already
applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a
second run reports changed=0 and EDITS.tsv (file, before/after sha256, leaf, crate, donor blobs, rule) is a fixed
point. --leaf applies one leaf's rules only; --check writes nothing and exits 1 when a run would change a file. The
edit payloads are the crate's own pre/post hunks (hash-pinned-java/m3jdk21-lane-rotation-compact-20261029 at
com.synexia develop d5ad5090d87e) with the smallest unique context, plus //FLLDCIM: MRL anchor lines. The other C3
row needs no edit (LEAVES.tsv): P2-020 SUPERSEDED (crate received by 6f79fc00f92, replaced by the group-mask atom of
0ac0c66e4ac; its new oracle and JUnit adapter are relocated by relocate.py).

usage: python -I apply.py [--root DIR] [--check] [--leaf P2-075]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "0b7482c32633966699c4c04586db44eea85b5545"
DONOR_COMMIT = "d5ad5090d87ef8477a56317e60ac0d29c65029f7"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "crate", "donor_blobs", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
LANE = "m3jdk21-lane-rotation-compact-20261029"


class Refused(Exception):
    pass


# leaf, path, preimage blob, crate, donor blobs (post, pre), rule note, edits [(old, new)]
RULES = (
    ("P2-075", SRC + "M3LaneAlgorithms.java", "08a2e274ed7faf940ed08f281ae9b62d92e7577f", LANE,
     "post=70239054a9d324dc31a1856f10a1a0af98079f50 pre=ca8c32bf77825453e071d9dd771d97fd8a4b268d",
     "APPLICABLE: the crate preimage (before-M3LaneAlgorithms) equals the owner's first 393 lines (master only appended"
     " later methods); the five crate hunks apply with their own context: nextCyclicIndex shared by both cycle-leader"
     " rotations (no int overflow of index + shift), stable compaction skips self-writes", [
         ('\n    /**\n     * Same result as {@link #rotate(long[], int, int, int)} by the juggling (cycle-leader) method:\n',
          '\n    //FLLDCIM: MRL - census P2-075, crate m3jdk21-lane-rotation-compact-20261029 (donor blob 70239054a9d3): one wrap-safe cycle step for both rotations\n    /**\n     * Shared wrap-safe index step for the int and long cycle-leader rotations.\n     * Requires 0 <= index < length and 0 < shift < length.\n     */\n    static int nextCyclicIndex(int index, int shift, int length) {\n        int boundary = length - shift;\n        return index >= boundary ? index - boundary : index + shift;\n    }\n\n    /**\n     * Same result as {@link #rotate(long[], int, int, int)} by the juggling (cycle-leader) method:\n'),
         ('            int index = start;\n            do {\n                index += shift;\n                if (index >= n) {\n                    index -= n;\n                }\n                long displaced = lane[from + index];\n',
          '            int index = start;\n            do {\n                index = nextCyclicIndex(index, shift, n);\n                long displaced = lane[from + index];\n'),
         ('            int index = start;\n            do {\n                index += shift;\n                if (index >= n) {\n                    index -= n;\n                }\n                int displaced = lane[from + index];\n',
          '            int index = start;\n            do {\n                index = nextCyclicIndex(index, shift, n);\n                int displaced = lane[from + index];\n'),
         ('    public static int compact(long[] lane, long[] keep, int from, int to) {\n        checkRange(lane.length, from, to);\n        checkRange((long) keep.length << 6, from, to);\n        int write = from;\n        for (int read = from; read < to; read++) {\n            if (bit(keep, read)) {\n                lane[write++] = lane[read];\n            }\n',
          '    public static int compact(long[] lane, long[] keep, int from, int to) {\n        checkRange(lane.length, from, to);\n        checkRange((long) keep.length << 6, from, to);\n        int write = from;\n        for (int read = from; read < to; read++) {\n            if (bit(keep, read)) {\n                //FLLDCIM: MRL - census P2-075, crate m3jdk21-lane-rotation-compact-20261029 (donor blob 70239054a9d3): stable compaction skips self-writes\n                if (write != read) lane[write] = lane[read];\n                write++;\n            }\n'),
         ('    public static int compact(int[] lane, long[] keep, int from, int to) {\n        checkRange(lane.length, from, to);\n        checkRange((long) keep.length << 6, from, to);\n        int write = from;\n        for (int read = from; read < to; read++) {\n            if (bit(keep, read)) {\n                lane[write++] = lane[read];\n            }\n',
          '    public static int compact(int[] lane, long[] keep, int from, int to) {\n        checkRange(lane.length, from, to);\n        checkRange((long) keep.length << 6, from, to);\n        int write = from;\n        for (int read = from; read < to; read++) {\n            if (bit(keep, read)) {\n                //FLLDCIM: MRL - census P2-075, crate m3jdk21-lane-rotation-compact-20261029 (donor blob 70239054a9d3): stable compaction skips self-writes\n                if (write != read) lane[write] = lane[read];\n                write++;\n            }\n'),
     ]),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> str:
    return "\r\n" if b"\r\n" in data else "\n"


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


def postimage(pre: bytes, edits, path: str) -> bytes:
    eol = eol_of(pre)
    text = pre.decode("utf-8")
    for old, new in edits:
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, rel, blob, crate, donors, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED" and leaf not in (None, rleaf):
            state = "PENDING"
        elif state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += on_disk == post or state == "CHANGED"
        rows.append((rel, sha256(pre), sha256(post), rleaf, crate, donors, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("CENSUS2_C3 recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=sorted({rule[0] for rule in RULES}), help="apply one leaf's rules only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
