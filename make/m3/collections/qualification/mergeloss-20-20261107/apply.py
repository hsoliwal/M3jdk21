#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1, leaf 20 (m3/collections): bulk-copy unopposed merge tails in M3IndexSort.stable.

Merge-loss audit leaf 20 (com.synexia ledger mergeloss/LEAVES.tsv): origin commit 0e68366337b ("perf(m3): refine
lexical reuse, primitive indices and JNI result writes", 2026-10-21, PR #1788) copies the remaining run of a merge
pass with one System.arraycopy instead of an element loop, "without changing comparisons or value-access
callbacks". The retain merge fb9b504bc047 (2026-10-28, "Superset: retain PR #1788") kept the older side. The later
non-merge 3e40ef9ec7e (2026-10-29, prepared index sort) copied PR #1882 blobs built on that older side and changed
only the Javadoc and the new stablePrepared route; it did not touch the tail copy.

Only the tail copy changes: comparator and valueAt calls, their order and the index-array state when a callback
throws are identical (the tails start after the last comparison of the pass). The JNI half of this leaf
(m3_index_path_masks.c chunked result writes) is not applied here: that file is owned by the merge-loss case 05
restore (selectLong / long_rows rewrite of the same select loop) and is recorded HOLD_CASE05 in DECISION.tsv.

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored edits, every anchor
exactly once, pinned preimage blob, refuses drift, idempotent; --check writes nothing).

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE_NAME = "MERGELOSS_RESTORE"
RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
ROOT_DEPTH = 3
LEAF = "20"
ORIGIN_COMMIT = "0e68366337be549b91ecbde593194e3ea97afd7f"
DROPPING_MERGE = "fb9b504bc047ea98a3a8d93a344c9f0651e1ccb1"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"

# path, preimage blob, rule note, edits [(old, new)], later postimage blobs of this series accepted as applied
RULES = (
    (SRC + "M3IndexSort.java", "9fdb435f3212a165262cb18ae11fcb1e9e56ae3e",
     "RESTORE 0e68366337b M3IndexSort hunk: one System.arraycopy for the unopposed tail of each merge", [
         ("                while (left < mid) destination[out++] = source[left++];\n"
          "                while (right < high) destination[out++] = source[right++];\n",
          "                //FLLDCIM: MRL - mergeloss leaf 20 (origin 0e68366337b, dropped by retain merge fb9b504bc047):\n"
          "                // bulk-copy the unopposed tail; no comparison or value-access callback follows.\n"
          "                if (left < mid) {\n"
          "                    System.arraycopy(source, left, destination, out, mid - left);\n"
          "                } else if (right < high) {\n"
          "                    System.arraycopy(source, right, destination, out, high - right);\n"
          "                }\n"),
     ], ()),
)


class Refused(Exception):
    pass


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


def run(root: Path, check: bool) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rel, blob, note, edits, later in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) in later:  # a later leaf of this series rewrote this postimage (pinned blob)
            state = "APPLIED_LATER"
        elif blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        else:
            state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += 1
        rows.append((rel, sha256(pre), sha256(post), LEAF, ORIGIN_COMMIT, DROPPING_MERGE, note))
        print("EDIT %s %s %s" % (LEAF, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("%s recipe=%s %s leaf=%s origin=%s merge=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE_NAME, RECIPE, VERSION, LEAF, ORIGIN_COMMIT[:12], DROPPING_MERGE[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[ROOT_DEPTH]), help="repository root")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
