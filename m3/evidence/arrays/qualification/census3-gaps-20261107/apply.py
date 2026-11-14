#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS3_GAPS v1 (m3/arrays): the RESTORE members of census pass 3 leaves P3-007 (M3ArgumentRoleBits, M3StringTypeBits); P3-002, P3-003, P3-004 are decision-only (HOLD_DEP).

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending). Every rule names the git blob sha of its
preimage at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already
applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing (every
preimage is verified before the first write). Idempotent: a second run reports changed=0 and EDITS.tsv (file,
before/after sha256, leaf, donor blob, rule) is a fixed point. --leaf applies one leaf's rules only; --check writes
nothing and exits 1 when a run would change a file. Donor: com.synexia develop 78cf2ad4c7e5 (blobs named per rule);
per-member decisions in DECISION.tsv (only RESTORE rows have edits here).

usage: python -I apply.py [--root DIR] [--check] [--leaf LEAF]
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
BASE_COMMIT = "e0d642e53b64f41571b18b63d3c1fa1dc1473e30"
DONOR_COMMIT = "78cf2ad4c7e56f7b0ade5aa206c491bc866178a6"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "donor_blob", "rule")
ROOT_UP = 3


class Refused(Exception):
    pass


SRC = "m3/arrays/src/main/java/jdk/m3/arrays/"
ROLE_DONOR = "7dc48bec582fd7768142739cb15a8177dd581288"
TYPE_DONOR = "52545a951b49a049aa75dab2c6f66c5af2194477"
RECEIVED = ("census3 P3-007 (qualification/census3-gaps-20261107) received the atoms of requireKnown, has, hasAny,"
            " names and their private helpers from the same donor blob at develop 78cf2ad4c7e5")

# leaf, path, preimage blob, donor blob, rule note, edits [(old, new)]
RULES = (
    ("P3-007", SRC + "M3ArgumentRoleBits.java", "70202d27bc9808acb6b65d36b087ffc6fbcd0902",
     "develop MIndexArgumentRoleBits blob " + ROLE_DONOR,
     "RESTORE private add(List, int, int, String): its only caller names() and the other scaffold atoms (requireKnown,"
     " has, hasAny) are received from the same donor blob the scaffold was projected from; constructor still throws", [
         ("import java.util.List;\n", "import java.util.ArrayList;\nimport java.util.List;\n"),
         (" * Every member body throws {@link M3NotYetReceived}.\n",
          " * Every member body throws {@link M3NotYetReceived}, except: " + RECEIVED + ".\n"),
         ("    public static void requireKnown(int bits) {\n"
          "        throw new M3NotYetReceived(\"M3ArgumentRoleBits#requireKnown/1\");\n    }\n",
          "    //FLLDCIM: MRL - census3 P3-007, donor MIndexArgumentRoleBits blob 7dc48bec582f: atoms received\n"
          "    public static void requireKnown(int bits) {\n"
          "        if ((bits & ~KNOWN_MASK) != 0) {\n"
          "            throw new IllegalArgumentException(\"unknown argument role bits: \" + bits);\n"
          "        }\n    }\n"),
         ("    public static boolean has(int bits, int required) {\n"
          "        throw new M3NotYetReceived(\"M3ArgumentRoleBits#has/2\");\n    }\n",
          "    public static boolean has(int bits, int required) {\n"
          "        requireKnown(required);\n"
          "        return (bits & required) == required;\n    }\n"),
         ("    public static boolean hasAny(int bits, int mask) {\n"
          "        throw new M3NotYetReceived(\"M3ArgumentRoleBits#hasAny/2\");\n    }\n",
          "    public static boolean hasAny(int bits, int mask) {\n"
          "        requireKnown(mask);\n"
          "        return (bits & mask) != 0;\n    }\n"),
         ("    public static List<String> names(int bits) {\n"
          "        throw new M3NotYetReceived(\"M3ArgumentRoleBits#names/1\");\n    }\n}\n",
          "    public static List<String> names(int bits) {\n"
          "        requireKnown(bits);\n"
          "        ArrayList<String> names = new ArrayList<>();\n"
          "        add(names, bits, CLAIM, \"CLAIM\");\n"
          "        add(names, bits, FACT, \"FACT\");\n"
          "        add(names, bits, EVIDENCE, \"EVIDENCE\");\n"
          "        add(names, bits, REASON, \"REASON\");\n"
          "        add(names, bits, RULE, \"RULE\");\n"
          "        add(names, bits, CONCLUSION, \"CONCLUSION\");\n"
          "        add(names, bits, CONTEXT, \"CONTEXT\");\n"
          "        add(names, bits, ASSUMPTION, \"ASSUMPTION\");\n"
          "        add(names, bits, OBSERVATION, \"OBSERVATION\");\n"
          "        add(names, bits, COUNTEREXAMPLE, \"COUNTEREXAMPLE\");\n"
          "        add(names, bits, OTHER, \"OTHER\");\n"
          "        return List.copyOf(names);\n    }\n"
          "\n"
          "    //FLLDCIM: MRL - census3 P3-007, donor MIndexArgumentRoleBits blob 7dc48bec582f: restored private helper\n"
          "    private static void add(List<String> names, int bits, int mask, String name) {\n"
          "        if ((bits & mask) != 0) names.add(name);\n"
          "    }\n}\n"),
     ]),
    ("P3-007", SRC + "M3StringTypeBits.java", "6c4371e77f3d8bf6e1c3e23e5ccca9ad82c7a77a",
     "develop MIndexStringTypeBits blob " + TYPE_DONOR,
     "RESTORE NUMERIC (INTEGER|DECIMAL) and the private requireKnown/add: their callers has/hasAny/names are received"
     " from the same donor blob; classify and its four private predicates stay HOLD_DEP (MIndexString,"
     " IndexCharacterFlags, MIndexStringMetadata); constructor still throws", [
         ("import java.util.List;\n", "import java.util.ArrayList;\nimport java.util.List;\n"),
         (" * Every member body throws {@link M3NotYetReceived}.\n",
          " * Every member body throws {@link M3NotYetReceived}, except: " + RECEIVED + ".\n"),
         (" *   <li>{@code constant NUMERIC (int)}</li>\n",
          " *   <li>{@code member classify(MIndexString, int, byte)} and its private predicates isWord, isIdentifier,"
          " isPunctuation, isSymbol (HOLD_DEP: no M3 owner for MIndexString, IndexCharacterFlags,"
          " MIndexStringMetadata; census3 P3-007)</li>\n"),
         ("    public static final int KNOWN_MASK = (1 << 13) - 1;\n",
          "    //FLLDCIM: MRL - census3 P3-007, donor MIndexStringTypeBits blob 52545a951b49: constant received\n"
          "    public static final int NUMERIC = INTEGER | DECIMAL;\n"
          "\n"
          "    public static final int KNOWN_MASK = (1 << 13) - 1;\n"),
         ("    public static boolean has(int bits, int required) {\n"
          "        throw new M3NotYetReceived(\"M3StringTypeBits#has/2\");\n    }\n",
          "    //FLLDCIM: MRL - census3 P3-007, donor MIndexStringTypeBits blob 52545a951b49: atoms received\n"
          "    public static boolean has(int bits, int required) {\n"
          "        requireKnown(required);\n"
          "        return (bits & required) == required;\n    }\n"),
         ("    public static boolean hasAny(int bits, int mask) {\n"
          "        throw new M3NotYetReceived(\"M3StringTypeBits#hasAny/2\");\n    }\n",
          "    public static boolean hasAny(int bits, int mask) {\n"
          "        requireKnown(mask);\n"
          "        return (bits & mask) != 0;\n    }\n"),
         ("    public static List<String> names(int bits) {\n"
          "        throw new M3NotYetReceived(\"M3StringTypeBits#names/1\");\n    }\n}\n",
          "    public static List<String> names(int bits) {\n"
          "        requireKnown(bits);\n"
          "        ArrayList<String> names = new ArrayList<>();\n"
          "        add(names, bits, EMPTY, \"EMPTY\");\n"
          "        add(names, bits, BLANK, \"BLANK\");\n"
          "        add(names, bits, WORD, \"WORD\");\n"
          "        add(names, bits, IDENTIFIER, \"IDENTIFIER\");\n"
          "        add(names, bits, INTEGER, \"INTEGER\");\n"
          "        add(names, bits, DECIMAL, \"DECIMAL\");\n"
          "        add(names, bits, BOOLEAN, \"BOOLEAN\");\n"
          "        add(names, bits, PUNCTUATION, \"PUNCTUATION\");\n"
          "        add(names, bits, SYMBOL, \"SYMBOL\");\n"
          "        add(names, bits, ASCII, \"ASCII\");\n"
          "        add(names, bits, COMPOSITE, \"COMPOSITE\");\n"
          "        add(names, bits, MASKED, \"MASKED\");\n"
          "        add(names, bits, OTHER, \"OTHER\");\n"
          "        return List.copyOf(names);\n    }\n"
          "\n"
          "    //FLLDCIM: MRL - census3 P3-007, donor MIndexStringTypeBits blob 52545a951b49: restored private helpers\n"
          "    private static void requireKnown(int bits) {\n"
          "        if ((bits & ~KNOWN_MASK) != 0) {\n"
          "            throw new IllegalArgumentException(\"unknown M3String type bits: \" + bits);\n"
          "        }\n    }\n"
          "\n"
          "    private static void add(List<String> out, int bits, int mask, String name) {\n"
          "        if ((bits & mask) != 0) out.add(name);\n"
          "    }\n}\n"),
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
    for rleaf, rel, blob, donor, note, edits in RULES:
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
        rows.append((rel, sha256(pre), sha256(post), rleaf, donor, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("CENSUS3_GAPS recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[ROOT_UP]), help="repository root")
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
