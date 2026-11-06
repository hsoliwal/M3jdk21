#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS_SMALL_BATCH v1: the APPLICABLE/MAPPED hunks of census leaves L066 and L077 (m3/collections).

Derived from m3/qualification/progress-monitor-unify-20261106/unify.py v1 (EDIT rules only: anchored "sub" edits,
every anchor exactly once, LF payloads re-joined with the preimage's line ending). Every rule names the git blob sha
of its preimage at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already
applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a
second run reports changed=0 and EDITS.tsv (file, before/after sha256, leaf, crate, donor blobs, rule) is a fixed
point. --leaf applies one leaf's rules only (one commit per leaf); --check writes nothing and exits 1 when a run would
change a file. Donor: com.synexia develop ca1aafc4d94f (blobs named per rule; the census rows are LEAVES.tsv).
L076 (M3TreeMap hooks, MAPPED on e65f0f664fa3) left the recipe: #2547 (836bdbb9d0c) landed the same hunk on master
before this batch was pushed, so its preimage drifted to the postimage minus this recipe's anchors (APPLIED).

usage: python -I apply.py [--root DIR] [--check] [--leaf L066|L077]
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
BASE_COMMIT = "feae0743e6c25f826d7f289c0b0acca19ff65e62"
DONOR_COMMIT = "ca1aafc4d94f9dd1ee288226dcca070cc60991c9"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "crate", "donor_blobs", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
DOOP = "m3-doop-reuse-20261030"
HOOKS = "m3jdk21-indexed-tree-hooks-20261021"


class Refused(Exception):
    pass


# leaf, path, preimage blob, crate, donor blobs (post, pre), rule note, edits [(old, new)]
RULES = (
    ("L066", SRC + "M3ObjectResidency.java", "efd6ceb3f1791e5c67a9e64acfcbe4d5b7439c39", DOOP,
     "post=8966285b99e21840bed497e9e3d6434cb327ab68 pre=45caaf707b47636b565311a960bff90c60450895",
     "MAPPED m3-doop-publication-budget-20261030: admit returns its target; ensureResident re-checks it after the"
     " factory/read/decode callbacks and deletes the spill copy only after the re-check (owner lacks the crate's"
     " callback-error/reentrant-eviction predecessors, so the hunk maps by method name, one try level shallower)", [
         ("    private void ensureResident(int slot, long estimatedBytes) {\n",
          "    //FLLDCIM: MRL - census L066, crate " + DOOP + " (m3-doop-publication-budget-20261030, donor blob"
          " 8966285b99e2): re-check the admitted budget after the callbacks; delete the spill copy after it\n"
          "    private void ensureResident(int slot, long estimatedBytes) {\n"),
         ("        admit(estimatedBytes, slot);\n        try {\n            T value;\n",
          "        long target = admit(estimatedBytes, slot);\n        try {\n            T value;\n"),
         ("                Objects.requireNonNull(value, \"decoded projection\");\n"
          "                spill.delete(slot);\n            } else {\n",
          "                Objects.requireNonNull(value, \"decoded projection\");\n            } else {\n"),
         ("                value = Objects.requireNonNull(factory.create(slot), \"created projection\");\n"
          "            }\n            resident[slot] = value;\n",
          "                value = Objects.requireNonNull(factory.create(slot), \"created projection\");\n"
          "            }\n"
          "            // Factory/read/decode callbacks may consume the originally admitted capacity.\n"
          "            if ((flags & OBSERVE_ONLY) == 0 && wouldExceed(estimatedBytes, target)) {\n"
          "                throw new BudgetExceededException(estimatedBytes, residentBytes, target);\n"
          "            }\n"
          "            if (before == State.SWAPPED) spill.delete(slot);\n"
          "            resident[slot] = value;\n"),
         ("    private void admit(long incomingBytes, int requestedSlot) {\n",
          "    //FLLDCIM: MRL - census L066, crate " + DOOP + " (m3-doop-publication-budget-20261030): admission returns"
          " the target it admitted against\n"
          "    private long admit(long incomingBytes, int requestedSlot) {\n"),
         ("            throw new BudgetExceededException(incomingBytes, residentBytes, target);\n        }\n    }\n\n"
          "    private void enforceBudget(",
          "            throw new BudgetExceededException(incomingBytes, residentBytes, target);\n        }\n"
          "        return target;\n    }\n\n    private void enforceBudget("),
     ]),
    ("L077", SRC + "M3TreeSet.java", "a2ac71e17f043a4eacc014b1c62845846e235fe5", HOOKS,
     "post=d144421cf952bf24cbcbd5c6dd7de63b7d6be4a4 pre=e414f679a341392770fb482377a51fe2f5e6518f",
     "APPLICABLE: the crate hunk's context is unchanged on the owner; indexedOwner() after the insertable field", [
         ("    private final boolean insertable;\n",
          "    private final boolean insertable;\n"
          "    //FLLDCIM: MRL - census L077, crate " + HOOKS + " (donor blob d144421cf952): owner hook for sibling views\n"
          "    /**\n"
          "     * Original array-backed AVL owner, for sibling M3 filtered/sorted view\n"
          "     * algorithms only. This does not expose or duplicate any entry storage.\n"
          "     * View bounds and descending order are already encoded in this map.\n"
          "     */\n"
          "    M3TreeMap<E, ?> indexedOwner() { return map; }\n"),
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
    print("CENSUS_SMALL_BATCH recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
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
