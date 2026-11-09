#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS2_C2 v1: the MAPPED hunk of census pass-2 leaf P2-043 (m3/collections, group C2 filtered views).

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (#2565): anchored "sub" edits, every
anchor exactly once, LF payloads re-joined with the preimage's line ending. Every rule names the git blob sha of its
preimage at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already
applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a
second run reports changed=0 and EDITS.tsv (file, before/after sha256, leaf, crate, donor blobs, rule) is a fixed
point. --leaf applies one leaf's rules only (one commit per leaf); --check writes nothing and exits 1 when a run would
change a file. Donor: com.synexia develop d5ad5090d87e (census pass-2 pin; blobs named per rule; the census rows are
LEAVES.tsv). The other C2 rows need no edit (LEAVES.tsv): P2-042, P2-067, P2-068 SUPERSEDED by later owner commits,
P2-047, P2-048 NO_PREIMAGE_PARTIAL (the crate hunks anchor in slot cursors master never carried).

usage: python -I apply.py [--root DIR] [--check] [--leaf P2-043]
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
BASE_COMMIT = "0b7482c32633966699c4c04586db44eea85b5545"
DONOR_COMMIT = "d5ad5090d87ef8477a56317e60ac0d29c65029f7"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "crate", "donor_blobs", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
PIN = "m3jdk21-engine-pin-admission-20261021"


class Refused(Exception):
    pass


# leaf, path, preimage blob, crate, donor blobs (post, pre), rule note, edits [(old, new)]
RULES = (
    ("P2-043", SRC + "M3FilteredLists.java", "78c77749664cd56ab2759cbb04ba5d792d624ecd", PIN,
     "post=25e243eabdbf32f2de215ebc96072849600a0568 pre=- (dataset m3jdk21-filtered-list-mutation-20261018;"
     " M3JDK receiver pre m3/collections/qualification/filtered-list-mutation-20261018/pre/M3FilteredLists.java.txt)",
     "MAPPED: the cursor's reversible fail-fast probe before the three backing writes (remove, set, add) of the filtered"
     " ListIterator; the owner's later state mask (340bafb58b7: failed -> state |= FAILED) and its supported-write"
     " helpers (df6a836b26e/fd633b1d98c: removePhysical/setPhysical instead of backing.remove/set) are kept", [
         ("            @Override public int previousIndex() { live(); return logical - 1; }\n",
          "            @Override public int previousIndex() { live(); return logical - 1; }\n"
          "\n"
          "            //FLLDCIM: MRL - census P2-043, crate " + "m3jdk21-engine-pin-admission-20261021"
          " (dataset m3jdk21-filtered-list-mutation-20261018, donor blob 25e243eabdbf): fail-fast probe before backing"
          " writes\n"
          "            /** Validate the original cursor without changing its physical position.\n"
          "             * Keep writes on the backing List: a writable list may expose an\n"
          "             * unmodifiable/snapshot iterator. next() also checks an exhausted\n"
          "             * fail-fast cursor; hasNext() alone need not detect invalidation.\n"
          "             */\n"
          "            private void checkForComodification() {\n"
          "                try {\n"
          "                    try {\n"
          "                        iterator.next();\n"
          "                    } catch (NoSuchElementException exhausted) {\n"
          "                        return;\n"
          "                    }\n"
          "                    iterator.previous();\n"
          "                } catch (RuntimeException | Error failure) {\n"
          "                    state |= FAILED;\n"
          "                    throw failure;\n"
          "                }\n"
          "            }\n"),
         ("                if (lastPhysical < 0) throw new IllegalStateException(\"remove without delivered element\");\n",
          "                if (lastPhysical < 0) throw new IllegalStateException(\"remove without delivered element\");\n"
          "                checkForComodification();\n"),
         ("                requireAccepted(value);\n                setPhysical(lastPhysical, value);\n",
          "                requireAccepted(value);\n                checkForComodification();\n"
          "                setPhysical(lastPhysical, value);\n"),
         ("                requireAccepted(value);\n                int position = iterator.nextIndex();\n",
          "                requireAccepted(value);\n                checkForComodification();\n"
          "                int position = iterator.nextIndex();\n"),
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


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, rel, blob, crate, donors, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED" and leaf not in (None, rleaf):
            state = "PENDING"
        elif state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += on_disk == post or state == "CHANGED"
        rows.append((rel, sha256(pre), sha256(post), rleaf, crate, donors, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("CENSUS2_C2 recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=sorted({rule[0] for rule in RULES}), help="apply one leaf's rules only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
