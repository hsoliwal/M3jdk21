#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS2_RESTORE v1: restore merge-loss audit phase-2 leaf 59 (m3/collections, Spring view).

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, LF payloads
re-joined with the preimage's line ending). Extension: every edit names how many times its anchor occurs (default 1).
Every rule pins the git blob sha of its preimage on master; a target must equal the preimage (the rule is applied) or
the postimage (already applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without
writing. Idempotent: a second run reports changed=0 and EDITS.tsv is a fixed point. --leaf applies one leaf's rules
only; --check writes nothing and exits 1 when a run would change a file.

Each rule cites the origin commit of the lost lines and the merge that dropped them, and the 3-way merge it was
checked against: base = origin commit's parent blob, theirs = origin commit's blob (both put through
m3/tooling/jdk-naming rewrite_text), ours = the pinned master blob. A conflicting hunk would be resolved by ADD/SUB
rows that take ours and apply only the origin commit's own substitution (named in the rule note). Leaf 45
(M3ObjectResidency pressure sampling) left this recipe: draft #2653 (322af721aa7, residency fold leaf PS) restores
the same lines, so its decision is RESTORED_SINCE #2653 (record-only PR).

usage: python -I apply.py [--root DIR] [--check] [--leaf 59]
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
BASE_COMMIT = "baafdafdc817217d9fb40e91ece2042269f0a871"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin_commit", "dropping_merge", "threeway",
                 "rule")
SPRING = "m3/collections/implementations/spring/src/main/java/jdk/m3/collections/spring/"


class Refused(Exception):
    pass


# leaf, path, preimage blob, origin commit, dropping merge, 3-way record, rule note, edits [(old, new, count)]
RULES = (
    ("59", SPRING + "M3SingleValueMapView.java", "ab4ffb7245687d4f837d969bd719c7a5a142fc39",
     "554b4d715e0b2679f3219d2e8140e917f49b7825", "fc917d80ea3cb2a888d327240766311a03e14d42",
     "base=e397cb4ab042 theirs=bf28adb8154e ours=ab4ffb724568 git-merge-file=CLEAN (result blob 1c5e41760e14 ="
     " this rule's postimage)",
     "RESTORE: the published (Ljdk/m3/collections/spring/M3MultiValueMap;)V constructor delegates to the widened Map"
     " constructor (m3/collections/qualification/spring-scalar-constructor-abi/README.md: verified preimage blob"
     " ab4ffb724568, no original public API may be removed)", [
         ("        this.targetMap = targetMap;\n    }\n\n    @Override\n    public int size() {\n",
          "        this.targetMap = targetMap;\n    }\n\n"
          "    //FLLDCIM: MRL - merge-loss leaf 59 (origin 554b4d715e0b, dropped by fold merge fc917d80ea3c): published"
          " constructor descriptor\n"
          "    /**\n"
          "     * Retains the original M3MultiValueMap constructor descriptor for precompiled clients.\n"
          "     * The generalized Map constructor remains the single validation/ownership path.\n"
          "     */\n"
          "    public M3SingleValueMapView(final M3MultiValueMap<K, V> targetMap) {\n"
          "        this((Map<K, List<V>>) targetMap);\n"
          "    }\n\n    @Override\n    public int size() {\n", 1),
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
