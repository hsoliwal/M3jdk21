#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1: merge-loss audit leaves 67 and 69 (m3/core SharedLexiconImage).

Derived from make/m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending), extended with a per-file leaf chain: the leaves
of one file apply in origin date order, so the accepted states of a file are the base and each chain prefix. The base
is pinned by git blob id; a file equal to a chain image is that state, anything else is drift and the recipe refuses
(exit 2) before any write. Idempotent: a second run reports changed=0 and EDITS.tsv (file, before/after sha256 of each
chain step, leaf, origin, dropping merge, donor blobs, rule) is a fixed point. --leaf applies the chain up to that
leaf; --check writes nothing and exits 1 when a run would change a file.

Leaves (com.synexia merge-loss audit LEAVES.tsv, 2026-11-06):
  67  origin 56847a4ec9d0 (#2471, "Use UTF-16 word comparison in mapped lexicon lookup"), dropped by the 20261106l
      landing fold 87cd0f5408dc (API_FOLD v1 kept the landing side c5b1ecb588c1 of SharedLexiconImage.java).
  69  origin dfc6800c0696 (#2473, "Reuse mapped UTF-16 words for image record ordering"), dropped by the fold
      1488b5577619 the same way.
Both PRs were closed by that landing as folded; the landing kept their files under
m3/qualification/master-landing-20261028/retained-variants/pr2471 and pr2473 (blobs a9aaf162a9cd, 662ff7f47c9d).
Master's methods are the view-free scalar loops of 49acb578201 and 8f88f9d65e2 (2026-11-01); the restore replaces each
method with the PR's postimage, which starts from the same view-free geometry and compares four UTF-16 units per
mapped long. The Guava attribution lines come with leaf 67 (adapted UnsignedBytes first-differing-lane pattern).

usage: python -I apply.py [--root DIR] [--check] [--leaf 67|69]
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
BASE_COMMIT = "72d67df316e7d6fcfc73b054680d61deddf1749a"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin", "dropping_merge", "donor_blobs", "rule")
IMAGE = "m3/core/src/jdk/m3/text/SharedLexiconImage.java"


class Refused(Exception):
    pass


def lines(*text):
    return "\n".join(text) + "\n"


HEADER_OLD = lines(
    "/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>",
    " * SPDX-License-Identifier: Apache-2.0",
    " */",
    "package jdk.m3.text;")
HEADER_NEW = lines(
    "/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>",
    " * Word-comparison portions copyright (C) 2009 The Guava Authors.",
    " * Adapted from Guava UnsignedBytes; Apache-2.0, see m3/LICENSE.",
    " * SPDX-License-Identifier: Apache-2.0",
    " */",
    "package jdk.m3.text;")

COMPARE_TO_OLD = lines(
    "    private int compareRecordTo(int index, String value) {",
    "        Objects.checkIndex(index, offsets.length);",
    "        int begin = payloadOffset + offsets[index] * 2;",
    "        int units = Math.min(lengths[index], value.length());",
    "        for (int at = 0; at < units; at++) {",
    "            int offset = begin + at * 2;",
    "            int recordUnit = (mapping.get(offset) & 0xff)",
    "                    | ((mapping.get(offset + 1) & 0xff) << 8);",
    "            char valueUnit = value.charAt(at);",
    "            if (recordUnit != valueUnit) return Integer.compare(recordUnit, valueUnit);",
    "        }",
    "        return Integer.compare(lengths[index], value.length());",
    "    }")
COMPARE_TO_NEW = lines(
    "    //FLLDCIM: MRL - merge-loss leaf 67 (origin 56847a4ec9d0 #2471, fold 87cd0f5408dc): UTF-16 long lanes",
    "    private int compareRecordTo(int index, String value) {",
    "        Objects.checkIndex(index, offsets.length);",
    "        int begin = payloadOffset + offsets[index] * 2;",
    "        int recordUnits = lengths[index];",
    "        int units = Math.min(recordUnits, value.length());",
    "        int stride = Long.BYTES / Character.BYTES;",
    "        int strideLimit = units & -stride;",
    "        int at = 0;",
    "        // Guava's first-differing-byte pattern, adapted to unsigned UTF-16 lanes.",
    "        for (; at < strideLimit; at += stride) {",
    "            int offset = begin + at * Character.BYTES;",
    "            long word = Long.reverseBytes(mapping.getLong(offset));",
    "            long query = value.charAt(at)",
    "                    | ((long) value.charAt(at + 1) << Character.SIZE)",
    "                    | ((long) value.charAt(at + 2) << (2 * Character.SIZE))",
    "                    | ((long) value.charAt(at + 3) << (3 * Character.SIZE));",
    "            long difference = word ^ query;",
    "            if (difference != 0) {",
    "                int shift = Long.numberOfTrailingZeros(difference) & -Character.SIZE;",
    "                return Integer.compare((int) (word >>> shift) & Character.MAX_VALUE,",
    "                        (int) (query >>> shift) & Character.MAX_VALUE);",
    "            }",
    "        }",
    "        for (; at < units; at++) {",
    "            int offset = begin + at * Character.BYTES;",
    "            int recordUnit = Short.toUnsignedInt(Short.reverseBytes(mapping.getShort(offset)));",
    "            char valueUnit = value.charAt(at);",
    "            if (recordUnit != valueUnit) return Integer.compare(recordUnit, valueUnit);",
    "        }",
    "        return Integer.compare(recordUnits, value.length());",
    "    }")

COMPARE_RECORDS_OLD = lines(
    "    private int compareRecords(int leftIndex, int rightIndex) {",
    "        Objects.checkIndex(leftIndex, offsets.length);",
    "        Objects.checkIndex(rightIndex, offsets.length);",
    "        int leftBegin = payloadOffset + offsets[leftIndex] * 2;",
    "        int rightBegin = payloadOffset + offsets[rightIndex] * 2;",
    "        int units = Math.min(lengths[leftIndex], lengths[rightIndex]);",
    "        for (int at = 0; at < units; at++) {",
    "            int leftUnit = (mapping.get(leftBegin + at * 2) & 0xff)",
    "                    | ((mapping.get(leftBegin + at * 2 + 1) & 0xff) << 8);",
    "            int rightUnit = (mapping.get(rightBegin + at * 2) & 0xff)",
    "                    | ((mapping.get(rightBegin + at * 2 + 1) & 0xff) << 8);",
    "            if (leftUnit != rightUnit) return Integer.compare(leftUnit, rightUnit);",
    "        }",
    "        return Integer.compare(lengths[leftIndex], lengths[rightIndex]);",
    "    }")
COMPARE_RECORDS_NEW = lines(
    "    //FLLDCIM: MRL - merge-loss leaf 69 (origin dfc6800c0696 #2473, fold 1488b5577619): UTF-16 long lanes",
    "    private int compareRecords(int leftIndex, int rightIndex) {",
    "        Objects.checkIndex(leftIndex, offsets.length);",
    "        Objects.checkIndex(rightIndex, offsets.length);",
    "        int leftBegin = payloadOffset + offsets[leftIndex] * Character.BYTES;",
    "        int rightBegin = payloadOffset + offsets[rightIndex] * Character.BYTES;",
    "        int leftUnits = lengths[leftIndex], rightUnits = lengths[rightIndex];",
    "        int units = Math.min(leftUnits, rightUnits);",
    "        int stride = Long.BYTES / Character.BYTES;",
    "        int strideLimit = units & -stride;",
    "        int at = 0;",
    "        for (; at < strideLimit; at += stride) {",
    "            int leftOffset = leftBegin + at * Character.BYTES;",
    "            int rightOffset = rightBegin + at * Character.BYTES;",
    "            long leftWord = Long.reverseBytes(mapping.getLong(leftOffset));",
    "            long rightWord = Long.reverseBytes(mapping.getLong(rightOffset));",
    "            long difference = leftWord ^ rightWord;",
    "            if (difference != 0) {",
    "                int shift = Long.numberOfTrailingZeros(difference) & -Character.SIZE;",
    "                return Integer.compare((int) (leftWord >>> shift) & Character.MAX_VALUE,",
    "                        (int) (rightWord >>> shift) & Character.MAX_VALUE);",
    "            }",
    "        }",
    "        for (; at < units; at++) {",
    "            int leftOffset = leftBegin + at * Character.BYTES;",
    "            int rightOffset = rightBegin + at * Character.BYTES;",
    "            int leftUnit = Short.toUnsignedInt(Short.reverseBytes(mapping.getShort(leftOffset)));",
    "            int rightUnit = Short.toUnsignedInt(Short.reverseBytes(mapping.getShort(rightOffset)));",
    "            if (leftUnit != rightUnit) return Integer.compare(leftUnit, rightUnit);",
    "        }",
    "        return Integer.compare(leftUnits, rightUnits);",
    "    }")

# path, base blob (master), chain of (leaf, origin, dropping merge, donor blobs, rule note, edits [(old, new)])
CHAINS = (
    (IMAGE, "41d898c4aa957743c325f83793825ea9597df83e", (
        ("67", "56847a4ec9d0dc66794f53898a592a686eb510b6", "87cd0f5408dc4e47c47455105b28619b88abc859",
         "post=8d3de015e03 pre=7d0dd69b3f9 retained=a9aaf162a9cd",
         "RESTORE compareRecordTo: the #2471 postimage method (same view-free begin/length geometry as master's"
         " 49acb578201 loop) plus its Guava UnsignedBytes attribution", [
             (HEADER_OLD, HEADER_NEW),
             (COMPARE_TO_OLD, COMPARE_TO_NEW),
         ]),
        ("69", "dfc6800c0696fc320d6bbfd9e721def867fa936c", "1488b5577619b57fb65151b9d94c6dcb9925f3d9",
         "post=941bd4b7203 pre=cfc65d20a16 retained=662ff7f47c9d",
         "RESTORE compareRecords: the #2473 postimage method (master's 8f88f9d65e2 scalar admission loop ->"
         " packed UTF-16 longs with a scalar tail)", [
             (COMPARE_RECORDS_OLD, COMPARE_RECORDS_NEW),
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
