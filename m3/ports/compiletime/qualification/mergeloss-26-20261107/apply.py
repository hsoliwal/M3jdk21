#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1, leaf 26 (m3/ports/compiletime half): frame exact UTF-16 through bounded bulk reads.

Merge-loss audit leaf 26 (com.synexia ledger mergeloss/LEAVES.tsv): origin commit d3c0986e911 ("Receive source-fact
reuse, deep filter traversal, and JNI refusal", 2026-10-20, PR #1538) frames every M3FileSourceFacts digest input as
its UTF-16 length followed by big-endian code units written in blocks of at most 4,096 units (String.getChars into
one reused char[], one MessageDigest.update per block) instead of two single-byte update calls per unit. The
retain merge a7d07c6b1f00 (2026-10-28, "Superset: retain PR #1538") kept the older side. #2547 (836bdbb9d0c)
restored the other compiletime hunks of the same origin (M3File.java, M3JavacASTIndex.java: RESTORE_LEDGER
3WAY_CLEAN pr1538) but took pr1573's retained copy for this file, which lacks the framing hunk. The origin receipt
is held as m3/qualification/master-landing-20261028/retained-variants/pr1538/m3/ports/compiletime/qualification/
source-facts-reuse-20261020.

Digest bytes are identical: same length prefix, same high/low byte order per raw UTF-16 unit (unpaired surrogates
included), no charset encoder. Derived from make/m3/collections/qualification/census-small-batch-20261106/apply.py v1
(anchored edits, every anchor exactly once, pinned preimage blob, refuses drift, idempotent; --check writes nothing).

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
ROOT_DEPTH = 4
LEAF = "26"
ORIGIN_COMMIT = "d3c0986e91101af77d2fe406db8b250fdfdc1383"
DROPPING_MERGE = "a7d07c6b1f00103fc1e7e78e0922c5717767cdde"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "rule")
AST = "m3/ports/compiletime/src/main/java/jdk/m3/compiletime/ast/"
ANCHOR = "//FLLDCIM: MRL - mergeloss leaf 26 (origin d3c0986e911, dropped by retain merge a7d07c6b1f00)\n"

# path, preimage blob, rule note, edits [(old, new)], later postimage blobs of this series accepted as applied
RULES = (
    (AST + "M3FileSourceFacts.java", "3ae2679bfaecc6bb1aa57a410804da30bfa0520c",
     "RESTORE d3c0986e911 M3FileSourceFacts hunk: bounded 4,096-unit bulk UTF-16 framing, identical digest bytes", [
         ("    private static final byte[] SOURCE_DOMAIN =\n",
          "    " + ANCHOR +
          "    private static final int UTF16_BLOCK_CHARS = 4_096;\n"
          "    private static final byte[] SOURCE_DOMAIN =\n"),
         ("    private static void frame(MessageDigest digest, CharSequence value) {\n"
          "        updateInt(digest, value.length());\n"
          "        for (int i = 0; i < value.length(); i++) {\n"
          "            char c = value.charAt(i);\n"
          "            digest.update((byte) (c >>> 8));\n"
          "            digest.update((byte) c);\n"
          "        }\n"
          "    }\n",
          "    " + ANCHOR +
          "    private static void frame(MessageDigest digest, CharSequence value) {\n"
          "        int length = value.length();\n"
          "        updateInt(digest, length);\n"
          "        if (length == 0) return;\n"
          "        int capacity = Math.min(length, UTF16_BLOCK_CHARS);\n"
          "        byte[] bytes = new byte[capacity * 2];\n"
          "        String text = value instanceof String string ? string : null;\n"
          "        char[] units = text == null ? null : new char[capacity];\n"
          "        for (int base = 0; base < length; ) {\n"
          "            int count = Math.min(capacity, length - base);\n"
          "            // String delegates bulk reads to its existing M3 owner. Preserve every raw\n"
          "            // UTF-16 unit; charset encoders can replace unpaired surrogates.\n"
          "            if (text != null) text.getChars(base, base + count, units, 0);\n"
          "            for (int i = 0; i < count; i++) {\n"
          "                char unit = text == null ? value.charAt(base + i) : units[i];\n"
          "                bytes[i * 2] = (byte) (unit >>> 8);\n"
          "                bytes[i * 2 + 1] = (byte) unit;\n"
          "            }\n"
          "            digest.update(bytes, 0, count * 2);\n"
          "            base += count;\n"
          "        }\n"
          "    }\n"),
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
