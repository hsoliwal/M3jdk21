#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe RESIDENCY_DONOR_ATOMS v1: the reentrant-eviction atom of crate m3-doop-reuse-20261030 on M3ObjectResidency.

Derived from make/m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending, every preimage verified before the first write).
The rule names the git blob sha of its preimage (M3JDK master 3beb791e741; unchanged since 107d96051f5); a target
must equal the preimage (the rule is applied) or the postimage (already applied, no-op); anything else is a drifted
preimage and the recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and EDITS.tsv
is a fixed point.
--check writes nothing and exits 1 when a run would change a file.

Leaf P1 (victim progress): the master owner's admit()/enforceBudget() loop selected a victim that evict() refuses
(a slot SWAPPING inside its own Codec.encode/SpillStore.write callback still holds its resident value), so a nested
admission from an eviction callback spun forever under the arena monitor (HANG.txt). The donor step
m3-doop-reentrant-eviction-20261030 (donor blob 0f7bf56e5aa8, preimage e8347cd7780d) is ported as one atom: both
victim selectors require RESIDENT (progress: every evict() of a selected victim detaches it or throws), and admit()
claims the requested slot (CREATING, or RESTORING for a SWAPPED slot) before eviction callbacks run and restores the
prior state in a finally, also on Error. The selector-only alternative is rejected: on the owner it turns the
livelock into a double publication of the requested slot (DECISION.tsv, VARIANTS.tsv). Donor: com.synexia develop
ca1aafc4d94f (the census G7 pin).

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
BASE_COMMIT = "3beb791e741"
DONOR_COMMIT = "ca1aafc4d94f9dd1ee288226dcca070cc60991c9"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "crate", "donor_blobs", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
DOOP = "m3-doop-reuse-20261030"
STEP = "m3-doop-reentrant-eviction-20261030"
ANCHOR = "    //FLLDCIM: MRL - residency victim progress P1, crate " + DOOP + " (" + STEP + ", donor blob 0f7bf56e5aa8): "


class Refused(Exception):
    pass


# leaf, path, preimage blob, crate, donor blobs (post, pre), rule note, edits [(old, new)]
RULES = (
    ("P1", SRC + "M3ObjectResidency.java", "d4dc9e683993694a4313b46a894fd81beb682eda", DOOP,
     "post=0f7bf56e5aa867806dc0a772e1b7839609a7c970 pre=e8347cd7780dbeb5a95759ef734a6dbf942d9edd",
     "PORTED " + STEP + ": both victim selectors require RESIDENT; admit() claims the requested slot before the"
     " eviction callbacks and restores it in a finally (the owner's admit() already returns its target, census L066)", [
         ("            evict(victim);\n        }\n        if (wouldExceed(incomingBytes, target) && (flags & OBSERVE_ONLY)"
          " == 0) {\n            throw new BudgetExceededException(incomingBytes, residentBytes, target);\n        }\n"
          "        return target;\n    }\n",
          "                evict(victim);\n            }\n            if (wouldExceed(incomingBytes, target) && (flags &"
          " OBSERVE_ONLY) == 0) {\n                throw new BudgetExceededException(incomingBytes, residentBytes,"
          " target);\n            }\n            return target;\n        } finally {\n"
          "            // Publication remains with the caller; admission restores even on Error.\n"
          "            setState(requestedSlot, before);\n        }\n    }\n"),
         ("    private long admit(long incomingBytes, int requestedSlot) {\n"
          "        long target = targetBudget(HeapSnapshot.current());\n"
          "        while (wouldExceed(incomingBytes, target)) {\n"
          "            if ((flags & OBSERVE_ONLY) != 0) break;\n"
          "            int victim = leastRecentlyUsed(requestedSlot);\n"
          "            if (victim < 0) {\n"
          "                throw new BudgetExceededException(incomingBytes, residentBytes, target);\n"
          "            }\n",
          ANCHOR + "claim the requested slot before the eviction callbacks\n"
          "    private long admit(long incomingBytes, int requestedSlot) {\n"
          "        // Eviction invokes caller codecs/stores. Claim this request before their callbacks.\n"
          "        State before = stateAt(requestedSlot);\n"
          "        setState(requestedSlot, before == State.SWAPPED ? State.RESTORING : State.CREATING);\n"
          "        try {\n"
          "            long target = targetBudget(HeapSnapshot.current());\n"
          "            while (wouldExceed(incomingBytes, target)) {\n"
          "                if ((flags & OBSERVE_ONLY) != 0) break;\n"
          "                int victim = leastRecentlyUsed(requestedSlot);\n"
          "                if (victim < 0) {\n"
          "                    throw new BudgetExceededException(incomingBytes, residentBytes, target);\n"
          "                }\n"),
         ("    private int leastRecentlyUsed(int excluded) {\n",
          ANCHOR + "a victim must be RESIDENT (a SWAPPING victim made admit() spin)\n"
          "    private int leastRecentlyUsed(int excluded) {\n"),
         ("                    && (Byte.toUnsignedInt(states[slot]) & SLOT_PINNED) == 0) return slot;\n",
          "                    && (Byte.toUnsignedInt(states[slot]) & SLOT_PINNED) == 0\n"
          "                    && stateAt(slot) == State.RESIDENT) return slot;\n"),
         ("    private int scanLeastRecentlyUsed(int excluded) {\n",
          ANCHOR + "the rollover scan skips non-RESIDENT victims too\n"
          "    private int scanLeastRecentlyUsed(int excluded) {\n"),
         ("                    || (Byte.toUnsignedInt(states[i]) & SLOT_PINNED) != 0) continue;\n",
          "                    || (Byte.toUnsignedInt(states[i]) & SLOT_PINNED) != 0\n"
          "                    || stateAt(i) != State.RESIDENT) continue;\n"),
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


def run(root: Path, check: bool) -> int:
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
        if state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += 1
        rows.append((rel, sha256(pre), sha256(post), rleaf, crate, donors, note))
        print("EDIT %s %s %s post_blob=%s" % (rleaf, state, rel, blob_sha(post)[:12]))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("RESIDENCY_DONOR_ATOMS recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
