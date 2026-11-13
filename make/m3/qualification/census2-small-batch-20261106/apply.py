#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS2_SMALL_BATCH v1: the APPLICABLE hunks of census2 leaves P2-001, P2-025 and P2-064.

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (#2565): every rule names the git
blob sha of its preimage at the base commit; a target must equal the preimage (the rule is applied) or the postimage
(already applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing. Every
preimage is verified before the first write. The anchored edits live in hunks/<leaf>.edits (@@@ FILE/HUNK/OLD/NEW/END,
every OLD block exactly once in the preimage; tab-indented sources keep their tabs). Idempotent: a second run reports
changed=0, and EDITS.tsv (file, before/after sha256, leaf, crate, donor sha chain, rule) is a fixed point. --leaf
applies one leaf's rules only (one commit per leaf); --check writes nothing and exits 1 when a run would change a file.

Each postimage is the donor postimage after jdk_naming (m3/tooling/jdk-naming rewrite_text, the rename fe35ea98841)
plus one //FLLDCIM: MRL anchor line; for P2-025 the base also carries faa9d9fff46's empty-identity check, which the
crate preimage predates, so the crate's one-line hunk is replayed on the base. Donor: com.synexia develop
d5ad5090d87e (census2 pin). P2-001 and P2-064 were also resolved by the chain resolver of #2602,
m3/qualification/cpu-owner-extensions-20261105/crate_chain2.py (+ named_apply.py; HASH_PINNED_CHAIN v2, rename parent
06495fea4157): APPLY. Census rows, verdicts and the no-edit leaves are LEAVES.tsv; gates GATES.tsv.

usage: python -I apply.py [--root DIR] [--check] [--leaf P2-001|P2-025|P2-064]
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
BASE_COMMIT = "8ec6289aab6fe3cf20c9040a9e146e320b3f20a8"
DONOR_COMMIT = "d5ad5090d87ef8477a56317e60ac0d29c65029f7"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "crate", "donor_chain", "rule")
MARK = "@@@ "


class Refused(Exception):
    pass


# leaf, path, preimage blob, crate, donor chain (sha256 prefixes; blobs at DONOR_COMMIT), rule note
RULES = (
    ("P2-001", "m3/arrays/src/main/java/jdk/m3/arrays/M3Arrays.java", "4145c7d2ec231f27cf05eea351521550800a8e0d",
     "m3-cpu-owner-extensions-20261105",
     "rename-parent 06495fea4157 sha256 3221d674 (manifest before) -> owner-view-20261105 final 67c2c232 (blob"
     " 474dda35c66e); jdk_naming(start) == base a7f4c973; jdk_naming(final) 5d99e07e; resolver"
     " m3/qualification/cpu-owner-extensions-20261105/crate_chain2.py + named_apply.py (#2602 8ec6289aab6f): APPLY",
     "APPLICABLE (chain resolver APPLY): the base is jdk_naming of the crate preimage; the postimage adds"
     " stringView(String) and its StringView slice (53 lines, no deletion)"),
    ("P2-025", "m3/core/src/jdk/m3/text/SharedRelatedLexemeCatalog.java", "822035981f5462c610d95ba2b86d9d7a056e8c9a",
     "m3-current-java-engine-qualification-20261101",
     "m3-related-empty-source-range-20261031: pre 38102366 (blob 8ca7c18c9ce8) -> post df379886 (blob"
     " dbdc6ad622a9); base = jdk_naming(pre) + faa9d9fff46 empty-identity check (2 lines)",
     "APPLICABLE (chain resolver NO_START: its rename parent predates the receiving commit 29feb5f14b5): the crate's"
     " one-line hunk; findBySource returns List.of() for an empty source range"),
    ("P2-064", "m3/collections/src/main/java/jdk/m3/collections/M3ListDiff.java", "98313bedc822b2796003898519b3d64b7d92c337",
     "m3-listdiff-exact-runs-20261030",
     "preimages/M3ListDiff.java fc3b925f (blob 72c549046d50) -> M3ListDiff.java.txt 648c59dc (blob 129add567dbf);"
     " jdk_naming(pre) == base 22aac16e; jdk_naming(post) 262308f8",
     "APPLICABLE (census pre=absent was a lookup miss: the crate carries preimages/; chain resolver APPLY):"
     " runs(boolean[],boolean) counts runs first and fills exact-size lanes"),
)


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


def load_hunks(leaf: str, path: str) -> list[tuple[str, str]]:
    text = (RECIPE_DIR / "hunks" / (leaf + ".edits")).read_text(encoding="utf-8")
    hunks, current, mode, old, new = [], None, None, [], []
    for line in text.split("\n")[:-1]:
        if line.startswith(MARK):
            word, _, rest = line[len(MARK):].partition(" ")
            if word == "FILE":
                current = rest.strip()
            elif word == "HUNK":
                mode, old, new = None, [], []
            elif word in ("OLD", "NEW"):
                mode = word
            elif word == "END":
                if current != path or mode != "NEW":
                    raise Refused("malformed hunk in %s for %s" % (leaf, path))
                hunks.append(("\n".join(old) + "\n", "\n".join(new) + "\n"))
                mode = None
            else:
                raise Refused("unknown marker in %s: %s" % (leaf, line))
        elif mode == "OLD":
            old.append(line)
        elif mode == "NEW":
            new.append(line)
    if not hunks:
        raise Refused("no hunks for %s %s" % (leaf, path))
    return hunks


def postimage(pre: bytes, hunks, path: str) -> bytes:
    if b"\r\n" in pre:
        raise Refused("CRLF preimage %s (receivers are LF in git)" % path)
    text = pre.decode("utf-8")
    for old, new in hunks:
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, rel, blob, crate, chain, note in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, load_hunks(rleaf, rel), rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED" and leaf not in (None, rleaf):
            state = "PENDING"
        elif state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += on_disk == post or state == "CHANGED"
        rows.append((rel, sha256(pre), sha256(post), rleaf, crate, chain, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("CENSUS2_SMALL_BATCH recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[2]), help="repository root (default: three up)")
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
