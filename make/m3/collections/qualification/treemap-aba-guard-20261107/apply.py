#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe TREEMAP_ABA_GUARD v1: revision-guarded comparator lookups of M3TreeMap (review of #2641).

#2641 made M3TreeMap.eraseAt refuse a slot a re-entrant comparator already released (B4b). The owner-lane review
found the residual ABA: a comparator inside remove(2) runs clear(); put(50); put(60), key 2's slot is live again
holding 60, the height check passes and remove(2) erases 60 and returns "v60". The same window opens wherever a slot
is located through user comparator calls and then read, written, erased or returned (SITES.tsv: every call site of
the slot locators in M3SlotMap, M3OrderedSlotMap, M3TreeMap, M3TreeSet and the filtered views over them), and the
bounded endpoint/step lookups also feed Tree.erase directly (pollFirstEntry/pollLastEntry/pollKey/bounded clear:
'tree size' damage on base, M3TreeMapReentrantLookupSelfTest).

One rule, applied where the slots come from instead of at each of their ~40 uses: after a comparator call that
changed the structure, M3TreeMap neither reads another slot nor returns one; it throws
ConcurrentModificationException. Concretely:
  - key walks (Tree.find behind findSlot/entrySlot, Tree.near behind every neighbor and bounded endpoint lookup, the
    put and defer descents) check the revision after every comparator call, so a walk never hands the comparator a
    released slot's key (NullPointerException shapes) and never returns a reused slot (the ABA);
  - lookups that probe the found slot against the view bounds after the walk (firstSlot, lastSlot, nextSlot,
    previousIndexedSlot, near) return it only through located(expected, slot), expected read before the lookup;
  - put reads the revision before its admission comparator calls too: it is the write half of compute, merge,
    computeIfAbsent and putIfAbsent, which read the old value first (compute(write) lost update on bounded views);
  - creating a cursor never fails (P2-098 'capture epoch before firstSlot'): M3SlotMap.slots and the #2641 mutable
    slot cursor read the revision first and start through cursorStart, which turns a structural change during the
    first-slot lookup into -1; the cursor reports it at its first call.
A comparator call made before any slot is read (findSlot's and near's bound/type checks, defer's admission) needs no
check: the walk that follows sees the changed structure consistently (the action-then-operation outcome). Unbounded
endpoint/step lookups call no comparator. eraseAt's height check stays as the backstop. The hash owners (M3HashMap,
M3LinkedHashMap, M3WeakHashMap) keep their lookups (their findSlot calls key equals/hashCode: a separate scope); the
shared M3SlotMap edit is neutral for them, since none of their firstSlot changes the revision (M3WeakHashMap's drain
erases nonstructurally).

Engine of qualification/filtered-views-owner-20261106/apply.py (census2-c2 lineage): anchored "sub" edits, every
anchor exactly once, LF payloads re-joined with the preimage's line ending; the rule names the git blob of its
preimage at the base commit; a target must equal the preimage (applied) or the postimage (no-op), anything else is a
drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and
EDITS.tsv is a fixed point. --check writes nothing and exits 1 when a run would change a file.

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
BASE_COMMIT = "05789961c767"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "behaviour", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
ANCHOR = ("//FLLDCIM: MRL - recipe TREEMAP_ABA_GUARD v1 (qualification/treemap-aba-guard-20261107, review of #2641):"
          " revision-guarded comparator lookups")

TREEMAP_EDITS = [
    ("// Modified 2026 by Hitesh Soliwal and contributors: refuse erasing a slot a re-entrant comparator already"
     " released (census2 P2-098 B4).\n",
     "// Modified 2026 by Hitesh Soliwal and contributors: refuse erasing a slot a re-entrant comparator already"
     " released (census2 P2-098 B4).\n"
     "// Modified 2026 by Hitesh Soliwal and contributors: fail fast when a comparator changes the structure during a"
     " slot lookup (TREEMAP_ABA_GUARD).\n"),
    (" * A key removal whose comparator clears the map or erases that mapping during the lookup\n"
     " * throws ConcurrentModificationException instead of erasing a released slot.\n",
     " * A key lookup or insertion during which the comparator changes the map's structure throws\n"
     " * ConcurrentModificationException: the located slot may have been released or reused for\n"
     " * another key, so it is never read, written, erased or returned. Creating an iterator never\n"
     " * fails for it: the iterator reads the revision before its first slot and throws at its first\n"
     " * call. Steps over bounded views, whose bound probes call the comparator, throw at once.\n"),
    ("    @Override int firstSlot() { return descending ? highest() : lowest(); }\n"
     "    @Override int lastSlot() { return descending ? lowest() : highest(); }\n"
     "    @Override int nextSlot(int slot) {\n"
     "        int s = tree.step(slot, descending); return slotWithin(s) ? s : -1;\n"
     "    }\n",
     "    " + ANCHOR + "\n"
     "    /**\n"
     "     * A bounded endpoint or step and a neighbor search probe the found slot's key against the\n"
     "     * view bounds (comparator calls after the walk): they return the slot only if the revision\n"
     "     * read before the lookup still holds. Key walks check after every comparator call.\n"
     "     */\n"
     "    private int located(int expected, int slot) { check(expected); return slot; }\n"
     "    @Override int firstSlot() { return located(revision(), descending ? highest() : lowest()); }\n"
     "    @Override int lastSlot() { return located(revision(), descending ? lowest() : highest()); }\n"
     "    /**\n"
     "     * First slot of a new cursor: creating a cursor never fails for a re-entrant comparator.\n"
     "     * A structural change during the lookup yields -1 and the cursor, which read the revision\n"
     "     * before, reports it at its first call; the comparator's own exceptions propagate.\n"
     "     */\n"
     "    int cursorStart(boolean last) {\n"
     "        int expected = revision();\n"
     "        try {\n"
     "            return last ? lastSlot() : firstSlot();\n"
     "        } catch (java.util.ConcurrentModificationException changed) {\n"
     "            if (revision() == expected) { throw changed; }\n"
     "            return -1;\n"
     "        }\n"
     "    }\n"
     "    @Override int firstCursorSlot() { return cursorStart(false); }\n"
     "    @Override int nextSlot(int slot) {\n"
     "        int expected = revision(), s = tree.step(slot, descending);\n"
     "        return located(expected, slotWithin(s) ? s : -1);\n"
     "    }\n"),
    ("        int s = tree.step(slot, !descending); return slotWithin(s) ? s : -1;\n",
     "        int expected = revision(), s = tree.step(slot, !descending);\n"
     "        return located(expected, slotWithin(s) ? s : -1);\n"),
    ("        int s = tree.near(key, r);\n",
     "        int expected = revision(), s = tree.near(key, r);\n"),
    ("            if (c < 0 || c == 0 && !lowInclusive) { s = lowest(); }\n"
     "        }\n"
     "        return slotWithin(s) ? s : -1;\n",
     "            if (c < 0 || c == 0 && !lowInclusive) { s = lowest(); }\n"
     "        }\n"
     "        return located(expected, slotWithin(s) ? s : -1);\n"),
    ("        if (r < 0 && hasHigh) {\n"
     "            int c = tree.compare(keyAt(s), high);\n",
     "        if (r < 0 && hasHigh) {\n"
     "            int c = tree.compare(keyAt(s), high); check(expected);\n"),
    ("        } else if (r > 0 && hasLow) {\n"
     "            int c = tree.compare(keyAt(s), low);\n",
     "        } else if (r > 0 && hasLow) {\n"
     "            int c = tree.compare(keyAt(s), low); check(expected);\n"),
    ("        tree.writable();\n"
     "        // Ordinary nonempty roots validate against visited keys during descent.\n",
     "        tree.writable();\n"
     "        int expected = revision(); // before the admission and descent comparator calls\n"
     "        // Ordinary nonempty roots validate against visited keys during descent.\n"),
    ("        int expected = revision(), p = -1, s = tree.root, cmp = 0;\n"
     "        while (s >= 0) {\n"
     "            cmp = tree.compare(key, keyAt(s));\n"
     "            if (cmp == 0) { check(expected); return writeAt(s, value); }\n",
     "        int p = -1, s = tree.root, cmp = 0;\n"
     "        while (s >= 0) {\n"
     "            cmp = tree.compare(key, keyAt(s)); check(expected);\n"
     "            if (cmp == 0) { return writeAt(s, value); }\n"),
    ("        int expected = revision(), p = -1, s = tree.root, cmp = 0;\n"
     "        while (s >= 0) {\n"
     "            cmp = tree.compare(key, keyAt(s));\n"
     "            if (cmp == 0) { check(expected); return false; }\n",
     "        int expected = revision(), p = -1, s = tree.root, cmp = 0;\n"
     "        while (s >= 0) {\n"
     "            cmp = tree.compare(key, keyAt(s)); check(expected);\n"
     "            if (cmp == 0) { return false; }\n"),
    ("        int find(K key) {\n"
     "            int s = root;\n"
     "            while (s >= 0) { int c = compare(key, M3SlotArrays.get(keys, s)); if (c == 0) { return s; }"
     " s = c < 0 ? left[s] : right[s]; }\n"
     "            return -1;\n"
     "        }\n",
     "        //FLLDCIM: MRL - recipe TREEMAP_ABA_GUARD v1: a key walk never reads another slot after its comparator"
     " changed the structure\n"
     "        /** Ends a key walk whose comparator call changed the structure before another slot is read. */\n"
     "        void walked(int expected) {\n"
     "            if (modifications != expected) { throw new java.util.ConcurrentModificationException(); }\n"
     "        }\n"
     "        int find(K key) {\n"
     "            int s = root, expected = modifications;\n"
     "            while (s >= 0) {\n"
     "                int c = compare(key, M3SlotArrays.get(keys, s)); walked(expected);\n"
     "                if (c == 0) { return s; } s = c < 0 ? left[s] : right[s];\n"
     "            }\n"
     "            return -1;\n"
     "        }\n"),
    ("            int s = root, candidate = -1;\n"
     "            while (s >= 0) {\n"
     "                int c = compare(key, M3SlotArrays.get(keys, s));\n",
     "            int s = root, candidate = -1, expected = modifications;\n"
     "            while (s >= 0) {\n"
     "                int c = compare(key, M3SlotArrays.get(keys, s)); walked(expected);\n"),
]

SLOTMAP_EDITS = [
    ("// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: restore fresh caller-owned entry"
     " projections without retained facade storage.\n",
     "// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: restore fresh caller-owned entry"
     " projections without retained facade storage.\n"
     "// Modified 2026 by Hitesh Soliwal and contributors: slot cursors read the revision before their first slot"
     " (TREEMAP_ABA_GUARD).\n"),
    ("    void accessed(int slot) { }\n",
     "    void accessed(int slot) { }\n"
     "    /**\n"
     "     * First slot of a new slot cursor. An owner whose first-slot lookup can see a re-entrant\n"
     "     * structural change returns -1 then; the cursor read the revision before and reports it.\n"
     "     */\n"
     "    int firstCursorSlot() { return firstSlot(); }\n"),
    ("            private int cursor = firstSlot();\n"
     "            private int expected = revision();\n",
     "            private int expected = revision(); // before the first slot: its lookup may call the comparator\n"
     "            private int cursor = firstCursorSlot();\n"),
]

MUTABLE_EDITS = [
    ("            pending = fromLast ? slots.lastSlot() : slots.firstSlot();\n",
     "            pending = slots.cursorStart(fromLast); // TREEMAP_ABA_GUARD: creation never fails\n"),
]

# path, preimage blob (base commit), behaviour, rule note, edits
RULES = (
    (SRC + "M3TreeMap.java", "be60ed31b2e768f418917aea9e54a122e0312949", "ABA",
     "GUARD every key walk (Tree.find, Tree.near, put and defer descents, near's bound probes) with a revision check"
     " after each comparator call, the bounded endpoint/step/neighbor lookups with located(), and put's admission;"
     " covers every use in SITES.tsv; cursorStart for cursor creation",
     TREEMAP_EDITS),
    (SRC + "M3SlotMap.java", "952986ad93d018c51c990a6de9b09bd5bc6d6a0d", "ABA",
     "slots(): read the revision before the first slot, through the firstCursorSlot hook (firstSlot for every other"
     " owner: none changes the revision in firstSlot, M3WeakHashMap's drain is a nonstructural erase)",
     SLOTMAP_EDITS),
    (SRC + "M3MutableFilteredViews.java", "4b3b7dc44f1262919ea2c804118c29128bf6fd1e", "ABA",
     "the #2641 slot cursor starts through cursorStart, so its creation never fails (P2-098 'capture epoch before"
     " firstSlot': the change is reported at the first call)",
     MUTABLE_EDITS),
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
    rows, changed, writes = [], 0, []
    for rel, blob, behaviour, note, edits in RULES:
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
        rows.append((rel, sha256(pre), sha256(post), behaviour, note))
        print("EDIT %s %s %s" % (behaviour, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("TREEMAP_ABA_GUARD recipe=%s %s base=%s rules=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, len(rows), changed, "CHANGED" if table_state else "UNCHANGED"))
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
