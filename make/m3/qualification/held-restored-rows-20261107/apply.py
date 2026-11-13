#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe HELD_RESTORED_ROWS v1: RESTORED status rows for the two loss-guard held tests that #2649 moved back live.

Derived from m3/collections/qualification/mergeloss-s1-20261106/apply.py (anchored "sub" edits, every anchor exactly
once, hash-pinned preimage, refusal on drift (exit 2) before any write, idempotent, --check fixed point).

#2649 (merge 664dd18c7eef, merge-loss case 05, mergeloss-s1-20261106 leaf L2) moved
m3/qualification/loss-guard-restore-20261106/held/.../M3IndexPathMasksLong{SelfTest,Test}.java back to their HELD.tsv
paths byte for byte, after its L1 restored M3IndexPathMasks.selectLong. HELD.tsv kept only the original HELD rows. This
recipe appends one status row per test and keeps the original rows. The rows use the file's own five columns (path,
source_blob, reason, evidence, owner_action: the loss-guard writer's schema, not hold.py's path/labels/gate/held_sha256/
reason of #2547's ledger), with the status RESTORED first in `reason`, the held sha256 and the citation in `evidence`.
A path-keyed reader (hold.py convention, last row wins) resolves both paths to RESTORED; a path-set reader (restore3.py
convention) still lists them, so it keeps skipping them.

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
BASE_COMMIT = "0d639c11986"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "decision", "cited_commits", "rule")
HELD_TSV = "m3/qualification/loss-guard-restore-20261106/HELD.tsv"
HELD_BLOB = "b5c1f1083ec530f2581c7279bfb81fc37d6afae5"
TEST = "m3/collections/src/test/java/jdk/m3/collections/"
CITED = "#2649 664dd18c7eef;mergeloss-s1-20261106 L2;held by #2517"

LAST_ROW = (TEST + "M3ReadOnlyViewBoundaryTest.java\t7918a03b94c8696d498be02388f458363298fd0b\t"
            "depends on held M3ReadOnlyViewBoundarySelfTest (its own JUnit methods passed)\t"
            "references M3ReadOnlyViewBoundarySelfTest; cannot compile without it\tpromote together with its SelfTest\n")

# path, held blob (= source_blob), held sha256, gate evidence of #2649
RESTORED = (
    (TEST + "M3IndexPathMasksLongSelfTest.java", "aa090bc23aa0dc3298e879b3377eceaea40dee92",
     "11347a14d915a08b9f32710ad226aceb081fb8d811338021883230bd0371a922",
     "M3IndexPathMasksLongSelfTest 3088 Java / 6162 JNI checks, default and -Xint, -Xcheck:jni (#2649 GATES.tsv)"),
    (TEST + "M3IndexPathMasksLongTest.java", "85308c0dc6601f3dbda6f048ae0131f790d8fba1",
     "7472e550658d6be53aeedd40f65c8809361a97404ba18825b64c9beddfef71b7",
     "surefire M3IndexPathMasksLongTest.longRowIdsPreserveSignedValuesAndMasks PASS (#2649 GATES.tsv)"),
)


def status_row(path: str, blob: str, held_sha256: str, gate: str) -> str:
    return "\t".join((
        path, blob,
        "RESTORED by #2649 (merge 664dd18c7eef, merge-loss case 05, mergeloss-s1-20261106 L2) after its L1 restored"
        " M3IndexPathMasks.selectLong (origin ebbfcbdf93aa, dropped by e913ab890566)",
        "held sha256 " + held_sha256 + "; moved byte for byte from held/ to this path (live blob = source_blob); " + gate,
        "none: status row; the original HELD row above is kept")) + "\n"


EDITS = [(LAST_ROW, LAST_ROW + "".join(status_row(*row) for row in RESTORED))]
NOTE = "EDIT: append 2 RESTORED status rows (file schema: path, source_blob, reason, evidence, owner_action)"


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
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


def postimage(pre: bytes) -> bytes:
    text = pre.decode("utf-8")
    for old, new in EDITS:
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, HELD_TSV, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool) -> int:
    target = root / HELD_TSV
    if not target.exists():
        raise Refused("missing preimage %s (pinned blob %s)" % (HELD_TSV, HELD_BLOB))
    on_disk = target.read_bytes()
    pre = on_disk if blob_sha(on_disk) == HELD_BLOB else git_blob(root, HELD_BLOB)
    post = postimage(pre)
    if blob_sha(on_disk) != HELD_BLOB and on_disk != post:
        raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (HELD_TSV, blob_sha(on_disk), HELD_BLOB))
    changed = int(on_disk != post)
    print("EDIT R %s %s" % ("CHANGED" if changed else "UNCHANGED", HELD_TSV))
    if changed and not check:
        target.write_bytes(post)
    row = (HELD_TSV, sha256(pre), sha256(post), "R", "RESTORED", CITED, NOTE)
    table = "\t".join(EDITS_COLUMNS) + "\n" + "\t".join(row) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("HELD_RESTORED_ROWS recipe=%s %s base=%s rules=1 changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
