#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1, leaf 30 (m3/fs): one shared bounded scratch lane for the serial host inventory pass.

Merge-loss audit leaf 30 (com.synexia ledger mergeloss/LEAVES.tsv): origin commit 34467281b6b ("receive(m3jdk21):
original provider capture and bounded shared scratch M3HostFileInventory.java", 2026-10-22, PR #1823) allocates one
64 KiB byte lane per inventory pass and hands it to every M3File.observe(maxBytes, scratch, progress, observer)
read. The retain merge edf132aa05a1 (2026-10-28, "Superset: retain PR #1823") kept the older side, so master
allocates an 8 KiB buffer per file. Positive evidence: #2547 (836bdbb9d0c, RESTORE_LEDGER 3WAY_CLEAN pr1823) restored
the provider half of the same PR - jdk.m3.file.M3NioFile.observe(long, byte[], M3StreamProgress, ByteObserver),
documented "may be reused serially across every input in one Maven/IDE project inventory" - and master has no
caller of that overload; this leaf is its consumer.

Observable difference: the borrowed ByteLayer callback receives up to 64 KiB per call instead of 8 KiB (the same
bytes, in order, through the same checked channel); hashes, fingerprints, progress totals and inventory rows are
unchanged. Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored edits, every
anchor exactly once, pinned preimage blob, refuses drift, idempotent; --check writes nothing).

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
LEAF = "30"
ORIGIN_COMMIT = "34467281b6be43be1bc2b62351e2d9a467915fc3"
DROPPING_MERGE = "edf132aa05a1d3011f43158b318d132339c09809"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "rule")
FS = "m3/fs/src/main/java/jdk/m3/fs/"

# path, preimage blob, rule note, edits [(old, new)], later postimage blobs of this series accepted as applied
RULES = (
    (FS + "M3HostFileInventory.java", "bed71427e9db79ac885dfa3f6b492b00640bcdf0",
     "RESTORE 34467281b6b: one 64 KiB scratch per serial pass, passed to M3NioFile.observe(long, byte[], ...)", [
         ("        Rows rows = new Rows(limits);\n        long total = 0;\n",
          "        Rows rows = new Rows(limits);\n"
          "        //FLLDCIM: MRL - mergeloss leaf 30 (origin 34467281b6b, dropped by retain merge edf132aa05a1)\n"
          "        // This pass is explicitly serial. Reuse a single bounded caller-owned\n"
          "        // byte lane for every M3NioFile provider channel, without per-file scratch\n"
          "        // allocation, duplicated file reads or extra retained native buffers.\n"
          "        byte[] scratch = new byte[64 * 1024];\n"
          "        long total = 0;\n"),
         ("                        child, (bytes, from, length) -> layer.onBytes(entry, bytes, from, length));\n",
          "                        scratch, child,\n"
          "                        (bytes, from, length) -> layer.onBytes(entry, bytes, from, length));\n"),
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
