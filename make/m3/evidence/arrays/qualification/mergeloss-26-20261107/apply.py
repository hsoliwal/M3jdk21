#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1, leaf 26 (m3/arrays): second-pass null-segment refusal in nativeConcatenateUtf16.

Merge-loss audit leaf 26 (com.synexia ledger mergeloss/LEAVES.tsv): origin commit d3c0986e911 ("Receive source-fact
reuse, deep filter traversal, and JNI refusal", 2026-10-20) made the second GetObjectArrayElement pass of
Java_jdk_m3_arrays_M3ArrayNative_nativeConcatenateUtf16 release the unpublished result and throw the incumbent
IllegalArgumentException("null UTF-16 segment"), like the first pass. The retain merge a7d07c6b1f00 (2026-10-28,
"Superset: retain PR #1538") kept the older side, so master returns a bare NULL with no pending exception and leaks
the result local reference. Positive evidence: the origin's own fault fixture, held on master as
m3/qualification/master-landing-20261028/retained-variants/pr1538/m3/arrays/src/main/native/tests/
m3_array_concat_faults.c (blob 283f30104882), fails on master ("both passes preserve a pending throwable or refuse
bare null") and passes on this postimage.

Derived from make/m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending). Every rule names the git blob of its preimage;
a target must equal the preimage (applied) or the postimage (already applied, no-op); anything else is drift and the
recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and EDITS.tsv is a fixed point.
--check writes nothing and exits 1 when a run would change a file. Leaf 27 (mergeloss-27-20261107) runs after this
recipe and pins this postimage; its postimage blob is accepted here as APPLIED_LATER.

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
LEAF = "26"
ORIGIN_COMMIT = "d3c0986e91101af77d2fe406db8b250fdfdc1383"
DROPPING_MERGE = "a7d07c6b1f00103fc1e7e78e0922c5717767cdde"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "rule")
NATIVE = "m3/arrays/src/main/native/"

# path, preimage blob, rule note, edits [(old, new)], later postimage blobs of this series accepted as applied
RULES = (
    (NATIVE + "m3_arrays_jni.c", "773f08334841c23b9ee008aeda2281b426e2532b",
     "RESTORE d3c0986e911 hunk 2: second-pass null segment releases the result and throws the first pass's refusal", [
         ("    if (segment == NULL) return NULL;\n    jsize length = (*env)->GetArrayLength(env, segment);\n",
          "    //FLLDCIM: MRL - mergeloss leaf 26 (origin d3c0986e911, dropped by retain merge a7d07c6b1f00): release\n"
          "    // the unpublished result and refuse a bare null exactly like the first pass; a pending throwable is kept.\n"
          "    if (segment == NULL) {\n"
          "      (*env)->DeleteLocalRef(env, result);\n"
          "      throw_illegal(env, \"null UTF-16 segment\");\n"
          "      return NULL;\n"
          "    }\n"
          "    jsize length = (*env)->GetArrayLength(env, segment);\n"),
     ], ("985dd0837eea94de201e3a4b8f9fe7747fe9b306",)),
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
