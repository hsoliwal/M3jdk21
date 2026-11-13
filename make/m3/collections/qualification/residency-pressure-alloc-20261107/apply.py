#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe RESIDENCY_PRESSURE_ALLOC v1: the PRESSURE_AWARE budget of M3ObjectResidency reads the heap into primitives.

After #2653 (recipe RESIDENCY_DONOR_ATOMS v3 leaf PS) targetBudget() built HeapSnapshot.current() inside the
PRESSURE_AWARE test and read one field of it. Whether the 48-byte record disappears depends on C2 scalar replacement:
#2653's JIT runs saw 48 B/call on the resident use() hit path in 2 of 8 runs, -Xint always 48, and an -Xbatch run that
went from 0 (fixed budget) to 48 (PRESSURE_AWARE) in one JVM. This rule replaces the record by a private static
sampledHeadroomBytes(): the same Runtime reads in the same order (totalMemory, freeMemory, maxMemory), the same used
clamp and the same headroom clamp, returned as a long. No cache, no epoch: every boundary samples the running heap
exactly as before, so budgets and victims are unchanged by construction. HeapSnapshot (public API) is untouched.

Mechanics (as qualification/residency-donor-semantics-20261106/apply.py v3): anchored "sub" edits, every anchor must
occur exactly once, LF payloads re-joined with the preimage's line ending. The rule pins the git blob of its
preimage (#2653's postimage) and the sha256 of its postimage; the target must equal the preimage or the postimage,
anything else refuses (exit 2) without writing. EDITS.tsv is regenerated and must be a fixed point. --check writes
nothing and exits 1 when a run would change a file.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
NAME = "RESIDENCY_PRESSURE_ALLOC"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "6f495787649"  # M3JDK master the rule was written on; the preimage is #2653's postimage
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_blob", "before_sha256", "after_sha256", "leaf", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
LEAF = "residency-pressure-alloc-20261107"
PRE_BLOB = "b8793d63a3cd9ee468b6188461261335fefff30e"
POST_SHA256 = "7dfb31ac019661ee2626826265d0c83ee3a6595e58829f26cce5143d8968bdb7"


class Refused(Exception):
    pass


# leaf, path, preimage blob, pinned postimage sha256, rule note, edits [(old, new)]
RULES = (
    ("PA", SRC + "M3ObjectResidency.java", PRE_BLOB, POST_SHA256,
     "targetBudget() reads the PRESSURE_AWARE heap headroom into primitives (sampledHeadroomBytes: the Runtime reads"
     " of HeapSnapshot.current() in the same order, no record); no snapshot, cache or epoch, budgets unchanged", [
         ('    private long targetBudget() {\n'
          '        long target = maxResidentBytes;\n'
          '        if ((flags & PRESSURE_AWARE) != 0) {\n'
          '            HeapSnapshot heap = HeapSnapshot.current();\n'
          '            long available = Math.max(0L, heap.estimatedHeadroomBytes() - reserveBytes);\n'
          '            target = Math.min(target, available);\n'
          '        }\n'
          '        return target;\n'
          '    }\n',
          '    //FLLDCIM: HPCPOP - ' + LEAF + ': the PRESSURE_AWARE budget reads the heap into primitives; no'
          ' HeapSnapshot per boundary (resident use() hit path 0 B/call under C2, -Xbatch and -Xint)\n'
          '    private long targetBudget() {\n'
          '        long target = maxResidentBytes;\n'
          '        if ((flags & PRESSURE_AWARE) != 0) {\n'
          '            long available = Math.max(0L, sampledHeadroomBytes() - reserveBytes);\n'
          '            target = Math.min(target, available);\n'
          '        }\n'
          '        return target;\n'
          '    }\n'
          '\n'
          '    //FLLDCIM: MRL - ' + LEAF + ': HeapSnapshot.current().estimatedHeadroomBytes() without the record'
          ' (same Runtime reads, same order, same clamps)\n'
          '    /** The estimated headroom of {@link HeapSnapshot#current()}, sampled into primitives at each call. */\n'
          '    private static long sampledHeadroomBytes() {\n'
          '        Runtime runtime = Runtime.getRuntime();\n'
          '        long committed = runtime.totalMemory();\n'
          '        long used = Math.max(0L, committed - runtime.freeMemory());\n'
          '        return Math.max(0L, runtime.maxMemory() - used);\n'
          '    }\n'),
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


def plan(root: Path, rule) -> tuple:
    """(path, preimage, postimage, state) of one rule; every pin is verified, nothing is written."""
    leaf, rel, blob, post_sha, note, edits = rule
    path = root / rel
    if not path.exists():
        raise Refused("missing target %s (pinned preimage blob %s)" % (rel, blob))
    on_disk = path.read_bytes()
    pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
    if blob_sha(pre) != blob:
        raise Refused("preimage blob of %s is %s, pinned %s" % (rel, blob_sha(pre), blob))
    post = postimage(pre, edits, rel)
    if sha256(post) != post_sha:
        raise Refused("postimage pin of %s leaf %s: computed %s, pinned %s" % (rel, leaf, sha256(post), post_sha))
    if on_disk == post:
        return path, pre, post, "UNCHANGED"
    if on_disk == pre:
        return path, pre, post, "CHANGED"
    raise Refused("drift: %s is blob %s, neither the pinned preimage nor the postimage" % (rel, blob_sha(on_disk)))


def run(root: Path, check: bool) -> int:
    plans = [plan(root, rule) for rule in RULES]  # all pins verified before the first write
    changed, rows = 0, []
    for rule, (path, pre, post, state) in zip(RULES, plans):
        if state == "CHANGED":
            changed += 1
            if not check:
                path.write_bytes(post)
        rows.append((rule[1], rule[2], sha256(pre), sha256(post), rule[0], rule[4]))
        print("EDIT %s %s %s post_blob=%s post_sha256=%s" % (rule[0], state, rule[1], blob_sha(post)[:12],
                                                             sha256(post)[:12]))
    table = ("# SPDX-License-Identifier: Apache-2.0\n# Edit rows of recipe qualification/%s/%s %s (%s), written"
             " by the recipe (fixed point). before_blob/before_sha256 pin the preimage, after_sha256 the postimage.\n"
             % (LEAF, RECIPE, VERSION, NAME))
    table += "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("%s recipe=%s %s base=%s rules=%d changed=%d edits=%s" % (
        NAME, RECIPE, VERSION, BASE_COMMIT, len(rows), changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
