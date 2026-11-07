#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1, leaf 31 (m3/fs): DirectoryPage identity from its table columns, not materialized rows.

Merge-loss audit leaf 31 (com.synexia ledger mergeloss/LEAVES.tsv): origin commit 8a3540d43c2 ("refactor(m3jdk21):
refine parser state, filesystem pages and provider budgets", 2026-10-20) made M3FS.DirectoryPage.equals compare the
M3DirectoryTable columns and hashCode use M3DirectoryTable.entriesHashCode(), which computes the API entry-list hash
from the canonical UTF-16 and stat columns without building DirEntry/Stat objects; it added the JUnit oracle
M3DirectoryPageIdentityTest. The retain merge a7d07c6b1f00 (2026-10-28, "Superset: retain PR #1538") kept the older
side of M3FS.java and dropped entriesHashCode() and the test. Positive evidence: #2547 (836bdbb9d0c) lists the
M3DirectoryTable hunk as an open insert/insert conflict for owners
(m3/qualification/retained-twins-core-lanes-20261106/conflicts/pr1538/m3/fs/src/main/java/jdk/m3/fs/
M3DirectoryTable.java.diff3); both sides are additive, so this recipe takes the union. The test is the retained
variant blob b7fda3515752 (the origin test after JDK naming), restored with one anchor line.

Values are unchanged: the columns are canonical (offsets start at 0, the kind lane is the 1/2/3 bijection of
M3FS.Kind), so column equality is entry-list equality and entriesHashCode() equals entries().hashCode(); the test
checks both against independent List/record oracles. Derived from
m3/collections/qualification/census-small-batch-20261106/apply.py v1, plus create rules (a new file whose text is
a pinned in-repository blob; absent -> written, equal -> no-op, anything else refused).

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
LEAF = "31"
ORIGIN_COMMIT = "8a3540d43c25ce92dafe2dd6b3f3e61ee7783dda"
DROPPING_MERGE = "a7d07c6b1f00103fc1e7e78e0922c5717767cdde"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "rule")
FS = "m3/fs/src/main/java/jdk/m3/fs/"
FS_TEST = "m3/fs/src/test/java/jdk/m3/fs/"
ANCHOR = "//FLLDCIM: MRL - mergeloss leaf 31 (origin 8a3540d43c2, dropped by retain merge a7d07c6b1f00)\n"

# path, preimage blob (or ABSENT:<donor blob>), rule note, edits [(old, new)], later postimage blobs accepted
RULES = (
    (FS + "M3DirectoryTable.java", "8a39d3505affd63c038518efbb39e1a618634c5f",
     "RESTORE 8a3540d43c2 hunk: entriesHashCode() before equals (union of the #2547 pr1538 insert/insert conflict)", [
         ("  @Override public boolean equals(Object other) {\n"
          "    return this == other || other instanceof M3DirectoryTable table\n",
          "  " + ANCHOR +
          "  /** Preserves the API entry-list hash using the canonical UTF-16 and stat columns. */\n"
          "  int entriesHashCode() {\n"
          "    int hash = 1;\n"
          "    for (int row = 0; row < size(); row++) {\n"
          "      int nameHash = 0;\n"
          "      for (int i = offsets[row]; i < offsets[row + 1]; i++) {\n"
          "        nameHash = 31 * nameHash + names[i];\n"
          "      }\n"
          "      int lane = row * 4;\n"
          "      // Keep Stat's record component order, including the enum instance hash.\n"
          "      int statHash = Long.hashCode(scalar[lane]);\n"
          "      statHash = 31 * statHash + Long.hashCode(scalar[lane + 1]);\n"
          "      statHash = 31 * statHash + Long.hashCode(scalar[lane + 2]);\n"
          "      statHash = 31 * statHash + Long.hashCode(scalar[lane + 3]);\n"
          "      statHash = 31 * statHash + M3FS.Kind.fromNative(state[lane]).hashCode();\n"
          "      statHash = 31 * statHash + state[lane + 1];\n"
          "      statHash = 31 * statHash + state[lane + 2];\n"
          "      statHash = 31 * statHash + state[lane + 3];\n"
          "      int entryHash = 31 * nameHash + statHash;\n"
          "      hash = 31 * hash + entryHash;\n"
          "    }\n"
          "    return hash;\n"
          "  }\n"
          "\n"
          "  @Override public boolean equals(Object other) {\n"
          "    return this == other || other instanceof M3DirectoryTable table\n"),
     ], ()),
    (FS + "M3FS.java", "e08ec663505d06b2e4f6ec0e270dfd3c77067c4e",
     "RESTORE 8a3540d43c2 hunk: DirectoryPage equals/hashCode over the table columns", [
         ("    @Override public boolean equals(Object other) {\n"
          "      return other instanceof DirectoryPage page && hasMore == page.hasMore\n"
          "          && entries().equals(page.entries());\n"
          "    }\n"
          "    @Override public int hashCode() { return 31 * entries().hashCode() + Boolean.hashCode(hasMore); }\n",
          "    " + ANCHOR +
          "    @Override public boolean equals(Object other) {\n"
          "      return other instanceof DirectoryPage page && hasMore == page.hasMore\n"
          "          && table.equals(page.table);\n"
          "    }\n"
          "    @Override public int hashCode() { return 31 * table.entriesHashCode() + Boolean.hashCode(hasMore); }\n"),
     ], ()),
    (FS_TEST + "M3DirectoryPageIdentityTest.java", "ABSENT:b7fda35157522c74ea7840cd16bf4b6796cffa57",
     "RESTORE 8a3540d43c2 test from retained-variants/pr1538 (blob b7fda3515752, origin test after JDK naming)", [
         ("/** Independent public List/record oracles; also compiles against the sealed before sources. */\n",
          ANCHOR +
          "/** Independent public List/record oracles; also compiles against the sealed before sources. */\n"),
     ], ()),
)


ABSENT = "ABSENT:"


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
        if blob.startswith(ABSENT):  # create rule: the file is new; its text is a pinned in-repository donor blob
            post = postimage(git_blob(root, blob[len(ABSENT):]), edits, rel)
            on_disk = target.read_bytes() if target.exists() else None
            if on_disk is not None and on_disk != post and blob_sha(on_disk) not in later:
                raise Refused("drifted new file %s: on disk %s" % (rel, blob_sha(on_disk)))
            state = "UNCHANGED" if on_disk == post else ("APPLIED_LATER" if on_disk is not None else "CHANGED")
            if state == "CHANGED":
                changed += 1
                writes.append((target, post))
            applied += 1
            rows.append((rel, "-", sha256(post), LEAF, ORIGIN_COMMIT, DROPPING_MERGE, note))
            print("EDIT %s %s %s" % (LEAF, state, rel))
            continue
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
