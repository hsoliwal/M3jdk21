#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1, leaf 27 (m3/arrays): valid zero-length UTF-16 windows return before any payload read.

Merge-loss audit leaf 27 (com.synexia ledger mergeloss/LEAVES.tsv): origin commit 259703f9fea ("m3(arrays):
materialize second serial Java/JNI zero-length fast path and regression", 2026-11-01, PR #2390 stage 2) returns 0
for count == 0 only AFTER the Java null/range checks and AFTER the JNI direct-window validation, and adds the JUnit
regression distinctArraysEmptyWindowsStillCheckBothBoundsAndOptionalNative. The landing merge fc917d80ea3c
(2026-11-06, "fold #2390 head 7c23ff3612ef through API_FOLD v1") resolved every m3/arrays file to the first parent,
so master lacks all three hunks. Positive evidence: master still carries the stage-2 receipt
m3/arrays/qualification/utf16-identity-20261101/SECOND_PASS_PINS.tsv naming these three postimages.

Results are unchanged: for count == 0 master already returns 0 (Arrays.mismatch of empty ranges, or a JNI loop with
no chunk); invalid offsets still throw IndexOutOfBoundsException (Java) or IllegalArgumentException (direct JNI)
because both fast returns follow the existing validation. The fast return only skips the JNI transition and the
IsSameObject call.

Derived from make/m3/collections/qualification/census-small-batch-20261106/apply.py v1 (see leaf 26 for the rules).
The m3_arrays_jni.c rule pins the leaf-26 postimage: run mergeloss-26-20261107/apply.py first.

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
LEAF = "27"
ORIGIN_COMMIT = "259703f9feafcd912b8706e512d9f20644f8b1a3"
DROPPING_MERGE = "fc917d80ea3cb2a888d327240766311a03e14d42"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "rule")
ARRAYS = "m3/arrays/src/"

# path, preimage blob, rule note, edits [(old, new)], later postimage blobs of this series accepted as applied
RULES = (
    (ARRAYS + "main/java/jdk/m3/arrays/M3ArrayNative.java", "34a0e4afcd60ec9dbafddac3fe260f41e2279275",
     "RESTORE 259703f9fea Java hunk: count == 0 returns 0 after both range checks, before identity/JNI", [
         ("        Objects.checkFromIndexSize(rightStart, count, right.length);\n",
          "        Objects.checkFromIndexSize(rightStart, count, right.length);\n"
          "        //FLLDCIM: MRL - mergeloss leaf 27 (origin 259703f9fea, dropped by landing merge fc917d80ea3c)\n"
          "        // Both array windows are valid, so an empty comparison has no payload reads.\n"
          "        if (count == 0) return 0;\n"),
     ], ()),
    (ARRAYS + "main/native/m3_arrays_jni.c", "dee79ed8173e3723a0abedc189576fd92fabb1c8",
     "RESTORE 259703f9fea JNI hunk: count == 0 returns 0 after the direct-window validation, before IsSameObject", [
         ("  /* Validate ranges first; JNI reference identity is not pointer-handle equality. */\n",
          "  //FLLDCIM: MRL - mergeloss leaf 27 (origin 259703f9fea, dropped by landing merge fc917d80ea3c)\n"
          "  /* Validation still precedes the zero-count fast return for direct native callers. */\n"
          "  if (count == 0) return 0;\n"
          "\n"
          "  /* Validate ranges first; JNI reference identity is not pointer-handle equality. */\n"),
     ], ()),
    (ARRAYS + "test/java/jdk/m3/arrays/M3ArrayNativeTest.java", "23404f78af3746ca1eff26de80b99e888e203796",
     "RESTORE 259703f9fea JUnit regression: distinct-array empty windows, both bounds, optional direct JNI", [
         ("    private static boolean loadOptionalNative() {\n",
          "    //FLLDCIM: MRL - mergeloss leaf 27 (origin 259703f9fea regression, dropped by landing merge fc917d80ea3c)\n"
          "    @Test\n"
          "    void distinctArraysEmptyWindowsStillCheckBothBoundsAndOptionalNative() throws ReflectiveOperationException {\n"
          "        char[] left = {'a', 'b', 'c'};\n"
          "        char[] right = {'x', 'y'};\n"
          "        for (int leftStart = 0; leftStart <= left.length; leftStart++) {\n"
          "            for (int rightStart = 0; rightStart <= right.length; rightStart++) {\n"
          "                assertEquals(0, M3ArrayNative.compareUtf16(left, leftStart, right, rightStart, 0));\n"
          "            }\n"
          "        }\n"
          "        assertThrows(IndexOutOfBoundsException.class,\n"
          "                () -> M3ArrayNative.compareUtf16(left, left.length + 1, right, 0, 0));\n"
          "        assertThrows(IndexOutOfBoundsException.class,\n"
          "                () -> M3ArrayNative.compareUtf16(left, 0, right, right.length + 1, 0));\n"
          "        if (!loadOptionalNative()) return;\n"
          "        Method compare = M3ArrayNative.class.getDeclaredMethod(\n"
          "                \"nativeCompareUtf16\", char[].class, int.class, char[].class, int.class, int.class);\n"
          "        compare.setAccessible(true);\n"
          "        for (int leftStart = 0; leftStart <= left.length; leftStart++) {\n"
          "            for (int rightStart = 0; rightStart <= right.length; rightStart++) {\n"
          "                assertEquals(0, compare.invoke(null, left, leftStart, right, rightStart, 0));\n"
          "            }\n"
          "        }\n"
          "    }\n"
          "\n"
          "    private static boolean loadOptionalNative() {\n"),
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
