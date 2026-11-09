#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Recipe JBCOLL_MASK_COMPACTOR_PERF v1: complete mask statistic + measured dispatch of jdk.internal.mindex.M3CollectionMaskCompactor.

Derived from m3/qualification/jbcoll-arraylist-inline-20261106/apply.py (itself from progress-monitor-unify-20261106/
unify.py). Root cause: the superset merge b361ddbf0552 (PR #1363, "conflicts resolved to the candidate the compiler
accepts") joined the long-returning maskStatistics of PR #1363's head 584cf8a6d0b5 (complete scan, runs << 32 |
removed) with the early exit of the int-returning removalRuns of ff0b94fc2a3b/4476604f7d95 (`if (count > threshold)
return count;`). The int widens to long, so it compiles; the bare count lands in the low half: runs = 0, removed =
count, and every mask of 512+ entries with a removal dispatches compactRuns. compactWords and the post-statistics
compactScalar are unreachable. One rule, EDIT of
src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java, six anchored substitutions:

  STAT_EDITS      maskStatistics(long[], int): the early exit and its threshold go; the scan completes, so the
                  statistic is the one 584cf8a6d0b5 introduced (runs << 32 | removed). Javadoc states the packed
                  contract; one //FLLDCIM: HPCPOP anchor marks the line where the early exit was.
  DISPATCH_EDITS  compactRuns threshold length >>> 7 -> length >>> 4. With the complete statistic alone, sparse masks
                  (Bernoulli 1/32, runs ~ n/33) left compactRuns, which master ran by accident, for compactWords:
                  15-32% slower on both CPU sets (TIMING.tsv rule STAT).
  SURVIVOR_EDITS  NEC idea pr1908 adopted, its 1024 bound dropped: the post-statistics fallback (fragmented masks of
                  512+) runs pr1908's survivor-word method (verbatim from the retained blob, renamed compactSurvivors)
                  instead of compactScalar; with compactScalar, E-core cluster12 masks stayed 7-12% slower than master
                  (rule THRESHOLD). The compact() header comment names the paths. compactScalar keeps masks below 512.
The result must hash to POST_SHA256; SURVIVOR_METHOD must equal the slice of the pr1908 blob (checked every run).

Hash-pinned: the target must equal the preimage (rule applied) or the postimage (already applied, no-op); anything
else is drift and the recipe refuses (exit 2) without writing. Every anchor must occur exactly once. EDITS.tsv (file,
kind, before blob, before sha256, after sha256, rule note) is a fixed point; a second run prints changed=0
plan=UNCHANGED.

--whatif OUT writes the owner postimage to OUT (outside the repository) without touching the tree.
postimage_text(pre) is imported by variants.py, which derives the timing/differential variants from it.

usage: python -I apply.py [--root DIR] [--check] [--whatif OUT]
exit: 0 ok; 1 --check found a change; 2 refused
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
BASE_COMMIT = "dbce93c3abf02256f2ecf656720c339b909713b6"
PLAN_NAME = "EDITS.tsv"
PLAN_COLUMNS = ("file", "kind", "before_blob", "before_sha256", "after_sha256", "rule")
OWNER = "src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java"
PRE_BLOB = "9f8b4c965b527ed097243bb5bc4600953f85f51d"
POST_SHA256 = "ad30c982c60b573a374d97b49483269676c483ecd71023ee92e4cc5aaf213b57"
NOTE = ("maskStatistics: drop the early exit `if (count > threshold) return count;` (b361ddbf0552 merge accident) and "
        "its threshold, complete runs << 32 | removed as 584cf8a6d0b5 introduced it, javadoc + //FLLDCIM: HPCPOP anchor; "
        "compactRuns threshold length >>> 7 -> >>> 4; fragmented 512+ fallback compactScalar -> compactSurvivors "
        "(pr1908 blob 3f88d207e1f6 method, renamed, 1024 bound dropped); compact() header comment")

STAT_EDITS = (
    ("    /** Count deletion runs, including ones crossing the 64-bit boundary. */\n"
     "    private static long maskStatistics(long[] deleted, int length) {\n"
     "        int words = ((length - 1) >>> 6) + 1;\n"
     "        int count = 0;\n"
     "        int threshold = Math.max(4, length >>> 7);\n"
     "        int removed = 0;\n",
     "    /**\n"
     "     * Returns {@code runs << 32 | removed} over the whole mask; deletion runs\n"
     "     * include ones crossing the 64-bit boundary.  compact() needs both fields\n"
     "     * to choose compactRuns, compactWords or compactSurvivors.\n"
     "     */\n"
     "    private static long maskStatistics(long[] deleted, int length) {\n"
     "        int words = ((length - 1) >>> 6) + 1;\n"
     "        int count = 0;\n"
     "        int removed = 0;\n"),
    ("            count += Long.bitCount(bits & ~((bits << 1) | preceding));\n"
     "            // This count is used only for the threshold decision in compact().\n"
     "            if (count > threshold) return count;\n"
     "            removed += Long.bitCount(bits);\n",
     "            count += Long.bitCount(bits & ~((bits << 1) | preceding));\n"
     "            //FLLDCIM: HPCPOP - no early exit: a bare count read as runs=0 and sent every 512+ mask to compactRuns\n"
     "            removed += Long.bitCount(bits);\n"),
)
DISPATCH_EDITS = (
    ("        if (removed == 0) return to;\n"
     "        if (runs <= Math.max(4, length >>> 7)) {\n",
     "        if (removed == 0) return to;\n"
     "        // Up to one deletion run per 16 entries stays on the run copier (the\n"
     "        // path sparse masks took); more runs use word-local copies.\n"
     "        if (runs <= Math.max(4, length >>> 4)) {\n"),
)
# NEC idea pr1908 (retained blob PR1908_BLOB), adopted with its `length >= 1024` bound dropped: its survivor-word
# method, sliced verbatim and renamed compactWords -> compactSurvivors (the owner already has a compactWords), replaces
# the post-statistics compactScalar fallback; --check re-derives SURVIVOR_METHOD from the blob.
PR1908_BLOB = "3f88d207e1f6d68da96477a0d184cf8d1caaa6d7"
SURVIVOR_START = "    /** Word-local survivor indexes; no element directory or rejected-bit loop. */\n"
SURVIVOR_METHOD = (
    "    /** Word-local survivor indexes; no element directory or rejected-bit loop. */\n"
    "    private static int compactSurvivors(Object[] values, int from, int length, long[] deleted) {\n"
    "        int write = from;\n"
    "        for (int base = 0; base < length;) {\n"
    "            int chunk = Math.min(64, length - base);\n"
    "            long valid = -1L >>> (64 - chunk);\n"
    "            long survivors = ~deleted[base >>> 6] & valid;\n"
    "            int start = from + base;\n"
    "            if (survivors == valid) {\n"
    "                if (write != start) System.arraycopy(values, start, values, write, chunk);\n"
    "                write += chunk;\n"
    "            } else {\n"
    "                while (survivors != 0L) {\n"
    "                    int bit = Long.numberOfTrailingZeros(survivors);\n"
    "                    values[write++] = values[start + bit];\n"
    "                    survivors &= survivors - 1;\n"
    "                }\n"
    "            }\n"
    "            base += chunk;\n"
    "        }\n"
    "        return write;\n"
    "    }\n")
SURVIVOR_EDITS = (
    ("        // Preserve the old hot scalar and long-run paths.  Word-based copying\n"
     "        // is admitted only for sparse (or nearly all) deletion states, after the\n"
     "        // callback phase has already prepared a complete immutable mask.\n",
     "        // Preserve the old hot scalar (below 512) and long-run paths.  Word-based\n"
     "        // copying serves the other deletion states (rejection words when sparse\n"
     "        // or nearly all, survivor words when fragmented), after the callback\n"
     "        // phase has already prepared a complete immutable mask.\n"),
    ("        }\n"
     "        return compactScalar(values, from, length, deleted);\n",
     "        }\n"
     "        // Fragmented masks: word-local survivor copies (retained pr1908).\n"
     "        return compactSurvivors(values, from, length, deleted);\n"),
    ("    /**\n"
     "     * Returns {@code runs << 32 | removed} over the whole mask;",
     SURVIVOR_METHOD + "\n"
     "    /**\n"
     "     * Returns {@code runs << 32 | removed} over the whole mask;"),
)
EDITS = STAT_EDITS + DISPATCH_EDITS + SURVIVOR_EDITS


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("blob %s is not in this repository (%s)" % (blob, failure)) from failure


def substitute(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, label, old[:70]))
    return text.replace(old, new)


def survivor_method(root: Path) -> str:
    """pr1908's survivor-word method sliced from its retained blob, renamed; must equal SURVIVOR_METHOD."""
    donor = git_blob(root, PR1908_BLOB).decode("utf-8").replace("\r\n", "\n")
    if donor.count(SURVIVOR_START) != 1:
        raise Refused("pr1908 blob %s: survivor method start occurs %d times" % (PR1908_BLOB, donor.count(SURVIVOR_START)))
    start = donor.index(SURVIVOR_START)
    end = donor.index("\n    }\n", start) + len("\n    }\n")
    sliced = donor[start:end].replace("compactWords(", "compactSurvivors(")
    if sliced != SURVIVOR_METHOD:
        raise Refused("SURVIVOR_METHOD differs from the pr1908 blob %s slice (renamed)" % PR1908_BLOB)
    return sliced


def postimage_text(pre: str, edits: tuple = EDITS) -> str:
    eol = "\r\n" if "\r\n" in pre else "\n"
    for old, new in edits:
        pre = substitute(pre, old.replace("\n", eol), new.replace("\n", eol), OWNER)
    return pre


def owner_images(root: Path) -> tuple[bytes, bytes, bytes | None]:
    target = root / OWNER
    on_disk = target.read_bytes() if target.exists() else None
    pre = on_disk if on_disk is not None and blob_sha(on_disk) == PRE_BLOB else git_blob(root, PRE_BLOB)
    if blob_sha(pre) != PRE_BLOB:
        raise Refused("pinned preimage %s does not hash to itself" % PRE_BLOB)
    post = postimage_text(pre.decode("utf-8")).encode("utf-8")
    if POST_SHA256 != "PENDING" and sha256(post) != POST_SHA256:
        raise Refused("postimage sha256 %s != pinned %s" % (sha256(post), POST_SHA256))
    return pre, post, on_disk


def run(root: Path, check: bool, whatif: str | None) -> int:
    survivor_method(root)
    pre, post, on_disk = owner_images(root)
    if whatif:
        Path(whatif).write_bytes(post)
        print("WHATIF %s sha256=%s" % (whatif, sha256(post)))
        return 0
    if on_disk is None:
        raise Refused("missing preimage %s (pinned blob %s)" % (OWNER, PRE_BLOB))
    if on_disk != pre and on_disk != post:
        raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (OWNER, blob_sha(on_disk), PRE_BLOB))
    state = "UNCHANGED" if on_disk == post else "CHANGED"
    if state == "CHANGED" and not check:
        (root / OWNER).write_bytes(post)
    print("EDIT %s %s" % (state, OWNER))
    row = (OWNER, "EDIT", PRE_BLOB, sha256(pre), sha256(post), NOTE)
    plan = "\t".join(PLAN_COLUMNS) + "\n" + "\t".join(row) + "\n"
    plan_path = RECIPE_DIR / PLAN_NAME
    plan_state = not plan_path.exists() or plan_path.read_bytes() != plan.encode("utf-8")
    if plan_state and not check:
        plan_path.write_bytes(plan.encode("utf-8"))
    changed = int(state == "CHANGED")
    print("JBCOLL_PERF recipe=%s %s base=%s rules=1 edits=%d changed=%d post=%s plan=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(EDITS), changed, sha256(post)[:16],
        "CHANGED" if plan_state else "UNCHANGED"))
    return 1 if check and (changed or plan_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--whatif", help="write the owner postimage to this path only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.whatif)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
