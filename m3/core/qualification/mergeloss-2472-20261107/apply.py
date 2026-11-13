#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1: merge-loss follow-up for #2472 (m3/core SharedLexiconImage.startsWith).

Engine: m3/core/qualification/mergeloss-67-69-20261107/apply.py v1 (itself derived from
make/m3/collections/qualification/census-small-batch-20261106/apply.py v1): anchored "sub" edits, every anchor exactly once,
LF payloads re-joined with the preimage's line ending, a per-file leaf chain whose accepted states are the base (pinned
by git blob id) and each chain prefix. Anything else is drift and the recipe refuses (exit 2) before any write.
Idempotent: a second run reports files_changed=0 and EDITS.tsv is a fixed point. --check writes nothing and exits 1
when a run would change a file.

Leaf 2472 (not one of the audit's numbered leaves; LOSS_CANDIDATES rows 462/466): origin 5a6209f8b2ca "Reuse mapped
UTF-16 words for lexicon prefix checks" (2026-11-05, PR #2472, stacked between #2471 and #2473), dropped by the
20261106l landing fold 3fb1026f54f3 (API_FOLD v1 kept the landing side). The PR was closed 2026-11-06T05:46:41Z as
folded; the landing kept its file under m3/qualification/master-landing-20261028/retained-variants/pr2472 (blob
d50e2bbab56b, the PR head renamed). Master's startsWith is the view-free scalar loop of 49acb578201 (2026-11-01); the
PR method starts from the same index/length checks and compares four UTF-16 units per mapped long.

Base: master's SharedLexiconImage.java after #2640 (leaves 67 and 69, merged as 317819ed0fb4; blob 62c0f4962df1),
which already carries the Guava UnsignedBytes attribution for this pattern; the pre-#2640 blob 41d898c4aa95 refuses.

usage: python -I apply.py [--root DIR] [--check] [--leaf 2472]
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
BASE_COMMIT = "092bd739190e64276552279e673cd0ca847c33f3"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin", "dropping_merge", "donor_blobs", "rule")
IMAGE = "m3/core/src/jdk/m3/text/SharedLexiconImage.java"


class Refused(Exception):
    pass


def lines(*text):
    return "\n".join(text) + "\n"


STARTS_WITH_OLD = lines(
    "    /** Exact UTF-16 prefix test directly against mapped bytes. */",
    "    boolean startsWith(int index, String prefix) {",
    "        Objects.requireNonNull(prefix);",
    "        Objects.checkIndex(index, offsets.length);",
    "        int begin = payloadOffset + offsets[index] * 2;",
    "        if (prefix.length() > lengths[index]) return false;",
    "        for (int at = 0; at < prefix.length(); at++) {",
    "            int offset = begin + at * 2;",
    "            int unit = (mapping.get(offset) & 0xff) | ((mapping.get(offset + 1) & 0xff) << 8);",
    "            if (unit != prefix.charAt(at)) return false;",
    "        }",
    "        return true;",
    "    }")
STARTS_WITH_NEW = lines(
    "    //FLLDCIM: MRL - merge-loss #2472 (origin 5a6209f8b2ca, fold 3fb1026f54f3): UTF-16 long lanes",
    "    /** Exact UTF-16 prefix test directly against mapped bytes. */",
    "    boolean startsWith(int index, String prefix) {",
    "        Objects.requireNonNull(prefix);",
    "        Objects.checkIndex(index, offsets.length);",
    "        int units = prefix.length();",
    "        if (units > lengths[index]) return false;",
    "        int begin = payloadOffset + offsets[index] * Character.BYTES;",
    "        int stride = Long.BYTES / Character.BYTES;",
    "        int strideLimit = units & -stride;",
    "        int at = 0;",
    "        for (; at < strideLimit; at += stride) {",
    "            int offset = begin + at * Character.BYTES;",
    "            long word = Long.reverseBytes(mapping.getLong(offset));",
    "            long query = prefix.charAt(at)",
    "                    | ((long) prefix.charAt(at + 1) << Character.SIZE)",
    "                    | ((long) prefix.charAt(at + 2) << (2 * Character.SIZE))",
    "                    | ((long) prefix.charAt(at + 3) << (3 * Character.SIZE));",
    "            if (word != query) return false;",
    "        }",
    "        for (; at < units; at++) {",
    "            int offset = begin + at * Character.BYTES;",
    "            int unit = Short.toUnsignedInt(Short.reverseBytes(mapping.getShort(offset)));",
    "            if (unit != prefix.charAt(at)) return false;",
    "        }",
    "        return true;",
    "    }")

# path, base blob (#2640), chain of (leaf, origin, dropping merge, donor blobs, rule note, edits [(old, new)])
CHAINS = (
    (IMAGE, "62c0f4962df1ed8f7a4a33fef2052c447e3b1f20", (
        ("2472", "5a6209f8b2ca59bee6fabc31fb5dd53334ae4a6b", "3fb1026f54f35ce27298f99de1688d104fdca00c",
         "post=cfc65d20a16 pre=8d3de015e03 retained=d50e2bbab56b",
         "RESTORE startsWith: the #2472 postimage method (same index/length checks as master's 49acb578201 loop)"
         " compares four UTF-16 units per mapped long with a scalar tail", [
             (STARTS_WITH_OLD, STARTS_WITH_NEW),
         ]),
    )),
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
        raise Refused("base blob %s is not in this repository (%s)" % (blob, failure)) from failure


def step(pre: bytes, edits, path: str) -> bytes:
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
    for rel, blob, chain in CHAINS:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        images = [on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)]
        for link in chain:
            images.append(step(images[-1], link[5], rel))
        if on_disk not in images:
            raise Refused("drifted preimage %s: on disk %s is no image of its chain (base blob %s)"
                          % (rel, blob_sha(on_disk), blob))
        at = images.index(on_disk)
        goal = len(chain) if leaf is None else next(
            (i + 1 for i, link in enumerate(chain) if link[0] == leaf), at)
        goal = max(goal, at)  # never undo a later leaf
        for i, (rleaf, origin, merge, donors, note, _) in enumerate(chain):
            state = "APPLIED" if i < at else ("CHANGED" if i < goal else "PENDING")
            applied += state != "PENDING"
            rows.append((rel, sha256(images[i]), sha256(images[i + 1]), rleaf, origin[:12], merge[:12], donors, note))
            print("EDIT %s %s %s" % (rleaf, state, rel))
        if goal > at:
            changed += 1
            writes.append((target, images[goal]))
    if not check:  # every chain verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS_RESTORE recipe=%s %s base=%s leaves=%d applied=%d files_changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), applied, changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=sorted({link[0] for chain in CHAINS for link in chain[2]}),
                        help="apply the chain up to this leaf only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
