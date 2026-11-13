#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS2_RESTORE v1: restore merge-loss audit phase-2 leaf 102 (m3/lexicon/receiver).

Same engine as make/m3/collections/qualification/mergeloss2-20261107/apply.py (derived from
make/m3/collections/qualification/census-small-batch-20261106/apply.py v1): anchored "sub" edits with a declared anchor
count, LF payloads re-joined with the preimage's line ending. The rule pins the git blob sha of its preimage on master;
the target must equal the preimage (the rule is applied) or the postimage (already applied, no-op); anything else is a
drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and
EDITS.tsv is a fixed point. --check writes nothing and exits 1 when a run would change a file.

The rule cites the origin commit of the lost line, the merge that dropped it, and the 3-way merge it was checked
against: base = origin commit's parent blob, theirs = origin commit's blob (both through make/m3/tooling/jdk-naming
rewrite_text), ours = the pinned master blob.

usage: python -I apply.py [--root DIR] [--check] [--leaf 102]
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
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "threeway",
                 "rule")
LEX = "m3/lexicon/receiver/src/main/java/jdk/m3/lexicon/"


class Refused(Exception):
    pass


# leaf, path, preimage blob, origin commit, dropping merge, 3-way record, rule note, edits [(old, new, count)]
RULES = (
    ("102", LEX + "M3WindowBoundarySummary.java", "6f4c00a2bb3ad76e2788f74d3a7e5aac04478d7b",
     "bf42c35a3e38b484ca7f39d56a9bd043068f67ba", "fc917d80ea3cb2a888d327240766311a03e14d42",
     "base=fdce0b7b067f theirs=483d57a722c1 ours=6f4c00a2bb3a git-merge-file=CLEAN (result blob 6f77c5d28721 ="
     " retained-variants/pr2390 copy = this rule's postimage without the //FLLDCIM line)",
     "RESTORE: CAMEL/PASCAL boundary() routes to the process-frozen M3CaseBoundaryPrecompute tables (on master since"
     " #2517 e629e2501ec without a caller; M3_CASE_LIANG_PRECOMPUTE.md: the 8 KiB truth table replaces the scalar"
     " decisions, out-of-range synthetic categories fall back to the exact scalar)", [
         ("    static boolean boundary(M3SeparatorKind kind, int before, int at, int after) {\n"
          "        if (kind == M3SeparatorKind.CAMEL) {\n"
          "            return at == Character.UPPERCASE_LETTER\n"
          "                    && (before == Character.LOWERCASE_LETTER || before == Character.DECIMAL_DIGIT_NUMBER);\n"
          "        }\n"
          "        boolean change = at != before\n"
          "                && !(at == Character.LOWERCASE_LETTER && before == Character.UPPERCASE_LETTER);\n"
          "        boolean acronymTail = after == Character.LOWERCASE_LETTER && at == Character.UPPERCASE_LETTER\n"
          "                && before == Character.UPPERCASE_LETTER;\n"
          "        return change || acronymTail;\n"
          "    }\n",
          "    //FLLDCIM: MRL - merge-loss leaf 102 (origin bf42c35a3e38, dropped by fold merge fc917d80ea3c): the scalar"
          " lives on as M3CaseBoundaryPrecompute.scalar, the generator and invalid-type fallback of the tables\n"
          "    static boolean boundary(M3SeparatorKind kind, int before, int at, int after) {\n"
          "        return M3CaseBoundaryPrecompute.boundary(kind, before, at, after);\n"
          "    }\n", 1),
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
    for old, new, expected in edits:
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != expected:
            raise Refused("anchor occurs %d times (need %d) in %s: %r" % (count, expected, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, rel, blob, origin, merge, threeway, note, edits in RULES:
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
        rows.append((rel, sha256(pre), sha256(post), rleaf, origin, merge, threeway, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS2_RESTORE recipe=%s %s base=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), applied, changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: five up)")
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
