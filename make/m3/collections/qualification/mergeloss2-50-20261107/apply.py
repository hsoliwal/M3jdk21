#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1, merge-loss phase-2 leaf 50 (m3/collections): shared JNI exception atom candidate.

Merge-loss phase-2 leaf 50 (CLEAN_0E.tsv n=50; ATOM_VERDICTS rows 128, 129, 130, 183): origin commit e47eeda7c05
("m3: consolidate internal JNI exception atoms and safe exact-address mapping", 2026-10-20, PR #1553) replaced the
local FindClass/ThrowNew/DeleteLocalRef helpers of four m3/collections C owners with m3_jni_throw_by_name from
src/java.base/share/native/libjava/m3_jni_util.h, reached by a source-relative include. The retain merge
04fe6419c8b9 (2026-10-28, "Superset: retain PR #1553") kept the older side of those four files. Leaf 79 (#2677,
a180724a87ea) has since restored the header itself.

DECISION.tsv records KEEP_LOCAL: m3/collections' native build closures do not carry src/java.base (eight
workflows check out m3/collections sparsely, and every -I list names only the JDK include dirs), and two
workflows pin the git blob of m3_index_mask_window.c. This recipe therefore is the hash-pinned CANDIDATE only: it
reproduces the origin edit byte-for-byte plus one //FLLDCIM: MRL anchor per file, so the probes in GATES.tsv can
build it in a full-tree layout and in each sparse closure. While DECISION.tsv says KEEP_LOCAL it refuses to write
into its own repository; --root may name a probe copy.

Derived from make/m3/collections/qualification/mergeloss-20-20261107/apply.py v1 (anchored edits, every anchor exactly
once, pinned preimage blob, refuses drift, idempotent; --check writes nothing).

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

RECIPE_NAME = "MERGELOSS_RESTORE"
RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
ROOT_DEPTH = 3
LEAF = "50"
ORIGIN_COMMIT = "e47eeda7c05b25a5638773df26a0e6087a62ddd2"
DROPPING_MERGE = "04fe6419c8b92a1766a85414245e5dc35db90b7c"
EDITS_NAME = "EDITS.tsv"
DECISION_NAME = "DECISION.tsv"
HOLD_DECISIONS = ("KEEP_LOCAL", "HOLD", "NEC")
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "rule")
SRC = "m3/collections/src/main/native/collections/"
INCLUDE = '#include "../../../../../../src/java.base/share/native/libjava/m3_jni_util.h"\n'
# FLLDCIM: MRL - mergeloss2 leaf 50: the anchor every postimage carries above the origin include line.
ANCHOR = ("//FLLDCIM: MRL - mergeloss2 leaf 50 (origin e47eeda7c05, dropped by retain merge 04fe6419c8b9):"
          " shared JNI exception atom.\n")
IAE = '"java/lang/IllegalArgumentException"'


def local_throw(var: str, cast: str, kind: str) -> str:
    """The incumbent local helper body the origin removed: FindClass, conditional ThrowNew, DeleteLocalRef."""
    return ("    jclass %s = (*env)->FindClass(env, %s);\n"
            "    if (%s != NULL) {\n"
            "        %s(*env)->ThrowNew(env, %s, message);\n"
            "        (*env)->DeleteLocalRef(env, %s);\n"
            "    }\n") % (var, kind, var, cast, var, var)


# path, preimage blob, postimage sha256, rule note, edits [(old, new)]
# FLLDCIM: MRL - mergeloss2 leaf 50: four rules, each the exact e47eeda7c05 hunk pair (include + helper body).
RULES = (
    (SRC + "m3_index_mask_window.c", "baa23467163a0e9344fe3c1d78dcc87cb707fd56",
     "465f92c13e8e2f2e3436c478021ae088deacb1652c856fc953aa079d48f25a33",
     "CANDIDATE e47eeda7c05 m3_window_invalid: shared m3_jni_throw_by_name, same class and message", [
         ("#include <stdint.h>\n\n#define M3_MASK_WINDOW_CHUNK 128\n",
          "#include <stdint.h>\n" + ANCHOR + INCLUDE + "\n#define M3_MASK_WINDOW_CHUNK 128\n"),
         (local_throw("type", "", IAE), "    m3_jni_throw_by_name(env, %s, message);\n" % IAE),
     ]),
    (SRC + "m3_long_index_path_masks_jni.c", "21e8eb690c5618c8521fc13def91d32e563381d8",
     "1ddf46e3d7f172ebefb05803f210f3eb4f33deef2caffff1a5231a9f75b55c73",
     "CANDIDATE e47eeda7c05 m3_throw: shared m3_jni_throw_by_name, caller-supplied class and message", [
         ("#include <stddef.h>\n\n#define M3_LONG_PATH_MAX_WINDOW 4096\n",
          "#include <stddef.h>\n" + ANCHOR + INCLUDE + "\n#define M3_LONG_PATH_MAX_WINDOW 4096\n"),
         (local_throw("cls", "(void)", "type"), "    m3_jni_throw_by_name(env, type, message);\n"),
     ]),
    (SRC + "m3_state_mask_histogram.c", "57c7eb3cc71b909540779f5d961c239a3fa5c081",
     "38b0665aea5d8c011af942b6bfbd9b969e74a7dfa30cf3fd1f35996ec7cab919",
     "CANDIDATE e47eeda7c05 m3_invalid: shared m3_jni_throw_by_name, same class and message", [
         ("#include <stdint.h>\n\n#define M3_STATE_COUNT 256\n",
          "#include <stdint.h>\n" + ANCHOR + INCLUDE + "\n#define M3_STATE_COUNT 256\n"),
         (local_throw("type", "", IAE), "    m3_jni_throw_by_name(env, %s, message);\n" % IAE),
     ]),
    (SRC + "segmented_bit_lane.c", "0d1a6f61897a6a5f3a600df0b426100156364e57",
     "bef7015090bcb35853d63c4e7bb77b2f29595a322136b525e81c98ffee77cd7e",
     "CANDIDATE e47eeda7c05 invalid: shared m3_jni_throw_by_name, same class and message", [
         ("#include <stdint.h>\n\nstatic void invalid(",
          "#include <stdint.h>\n" + ANCHOR + INCLUDE + "\nstatic void invalid("),
         (local_throw("type", "", IAE), "    m3_jni_throw_by_name(env, %s, message);\n" % IAE),
     ]),
)


# preimage sha256 of each pinned blob (the before column of EDITS.tsv)
PRE_SHA256 = {
    SRC + "m3_index_mask_window.c": "9ed1244ef90a72845d580b8919c670760434a81cf6dfc48f3aecbc8483726c02",
    SRC + "m3_long_index_path_masks_jni.c": "95a8ee0c924b570ef19a8403ac9f5ee2d970e9d27d1df562cb87d40b5e5b2ba4",
    SRC + "m3_state_mask_histogram.c": "b870257196569335389a01c805d4e75e2412e5e2be9c2e2187153d6f45baf4cd",
    SRC + "segmented_bit_lane.c": "3567f3eed5ab042b5d88574da8f77cce967c3f35bcaf1b99d2c373af4d4ef674",
}


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> str:
    return "\r\n" if b"\r\n" in data else "\n"


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


def held_decision() -> str:
    """The leaf decision from DECISION.tsv; a hold decision forbids writes into this recipe's own repository."""
    path = RECIPE_DIR / DECISION_NAME
    if not path.exists():
        return ""
    rows = [line.split("\t") for line in path.read_text(encoding="utf-8").splitlines()]
    column = rows[0].index("decision")
    held = sorted({row[column] for row in rows[1:] if len(row) > column and row[column] in HOLD_DECISIONS})
    return ",".join(held)


def run(root: Path, check: bool) -> int:
    held = held_decision()
    if held and not check and root == RECIPE_DIR.parents[ROOT_DEPTH]:
        raise Refused("leaf %s decision %s in %s: candidate is applied to probe copies only (--root)" % (
            LEAF, held, DECISION_NAME))
    rows, changed, applied, writes = [], 0, 0, []
    for rel, blob, after, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        if sha256(on_disk) == after:  # idempotent: the pinned postimage is already on disk
            post, state = on_disk, "UNCHANGED"
        elif blob_sha(on_disk) == blob:
            state = "CHANGED"
            post = postimage(on_disk, edits, rel)
            if sha256(post) != after:
                raise Refused("postimage %s is %s, pinned %s" % (rel, sha256(post), after))
        else:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s or postimage %s" % (
                rel, blob_sha(on_disk), blob, after[:12]))
        if state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += 1
        rows.append((rel, PRE_SHA256[rel], after, LEAF, ORIGIN_COMMIT, DROPPING_MERGE, note))
        print("EDIT %s %s %s" % (LEAF, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("%s recipe=%s %s leaf=%s origin=%s merge=%s rules=%d applied=%d changed=%d edits=%s hold=%s" % (
        RECIPE_NAME, RECIPE, VERSION, LEAF, ORIGIN_COMMIT[:12], DROPPING_MERGE[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED", held or "-"))
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
