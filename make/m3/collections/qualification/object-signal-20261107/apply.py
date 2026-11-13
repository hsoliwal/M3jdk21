#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe OBJECT_SIGNAL_OWNER_EDITS v1: the owner-file edits of leaf object-signal-20261107 (m3/collections).

One rule: the family ledger make/m3/collections/JDK_FAMILY_MAP.tsv gains the row of the relocated object first-pass signal
family (M3ObjectSignalTable, M3ObjectSignalQuery, M3ObjectProjectors, M3ObjectSignalMatcher), inserted before the
entryless signed-range lane row (the ledger's last row on the base commit) so the m3-immutable-pool rows of the same
donor stay together. Everything else of the leaf is relocated by relocate.py; no Java owner, pom.xml or module-info.java
is edited (the package jdk.m3.collections is already exported by module jdk.m3.collections).

Derived from make/m3/collections/qualification/census2-c4-20261106/apply.py v1 (anchored "sub" edits, every anchor exactly
once, LF payloads re-joined with the preimage's line ending). The rule names the git blob sha of its preimage at the
base commit; a target must equal the preimage (the rule is applied) or the postimage (already applied, no-op); anything
else is a drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a second run reports
changed=0 and EDITS.tsv (file, before/after sha256, leaf, rule) is a fixed point. The payload names the donor module
and blobs, never the donor repository (make/m3/tooling/jdk-naming renames synexia-* tokens in .py files; the repository
lives in RELOCATION_MAP.tsv).

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
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "ee47f1325def394f24b8716669ed1b84750675b1"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "rule")
ROW = "\t".join((
    "Object.equals / String.equals / String.contains prefilter (bitwise first pass before the exact predicate)",
    "M3ObjectSignalTable / M3ObjectSignalQuery / M3ObjectProjectors / M3ObjectSignalMatcher",
    "RECEIVER_CANDIDATE_V1",
    "64 transposed feature planes (one long[] bitmap per signal bit) + byte kind / int domain, length, flags /"
    " long exact-hash, raw-identity lanes",
    "adapted from the M3Scale donor module m3-object-signal (SignalTable cf5b38a4654a, SignalQuery aa475998e712;"
    " repository and blobs in RELOCATION_MAP.tsv) by recipe make/m3/collections/qualification/object-signal-20261107;"
    " lossy first pass (false positives allowed, no false negative for literal String / byte[] containment), exact"
    " truth stays the caller's predicate (M3ObjectSignalMatcher counts the checks); java.lang.String and"
    " Object.equals remain authority; Java kernel only, the donor JNI kernel is a HELD row"))
# leaf, path, preimage blob at BASE_COMMIT, rule note, edits [(old, new)]
RULES = (
    ("object-signal-20261107", "make/m3/collections/JDK_FAMILY_MAP.tsv", "913a20f4938eab02d0af8a1f0b0fdb7a127d5bf2",
     "FAMILY_ROW: one ledger row for the relocated family, before the entryless signed-range lane row", [
         ("\nSupplementary primitive key/value signed-range lane selection (entryless",
          "\n" + ROW + "\nSupplementary primitive key/value signed-range lane selection (entryless"),
     ]),
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
    for leaf, rel, blob, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += 1
        rows.append((rel, sha256(pre), sha256(post), leaf, note))
        print("EDIT %s %s %s" % (leaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("OBJECT_SIGNAL_OWNER_EDITS recipe=%s %s base=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), applied, changed, "CHANGED" if table_state else "UNCHANGED"))
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
