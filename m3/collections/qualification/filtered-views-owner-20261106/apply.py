#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FILTERED_VIEWS_OWNER v1: owner edits of census pass-2 leaf P2-098 (m3/collections filtered views).

The held P2-098 tests (qualification/census2-t3-20261106, HOLD_OWNER_EDIT) expect four owner behaviours master lacks.
Per-behaviour decisions are DECISION.tsv; this recipe ports the adopted ones from the donor lineage (com.synexia crate
m3jdk21-20261019 postimages = retained variants pr1463 of m3/qualification/master-landing-20261028) into master's own
owner shapes, never by copying the donor files (master's lineage differs: generic backing iterators, navigation
cursors):
  B1+B2 read-only (M3FilteredViews): over an M3TreeMap/M3TreeSet owner every filtered cursor reads the owner revision
        before the backing cursor opens and checks it before and after each hasNext/next (prepared, exhausted,
        predicate- and comparator-mutated cursors fail fast); every predicate/comparator callback is unchanged and any
        other source keeps its own iterator policy;
  B1+B2+B3 mutable (M3MutableFilteredViews): over an M3TreeMap/M3TreeSet owner the set, entry and (new) values cursors
        walk physical slots (the owner hooks indexedOwner/previousIndexedSlot of census L076/L077), read the revision
        before the first slot and check it after every predicate call and slot step; the predicate runs
        once per slot, values() reads the slot value without a second predicate call, and an own remove keeps the
        prepared successor slot. Other owners keep the navigation cursors and AbstractMap.values().
  B4 (M3TreeMap): the P2-098 'ordering' row (B4a) is no tree damage: oracle/.../FilteredViewsTreeMapRepro.java R1/R3
        show verifyInvariants running the test's still-armed comparator; it passes once the mutable cursor steps the
        bounded slot in next() (B2). The seeded differential found a real neighbour (B4b, base and head alike, R4): a
        key removal whose comparator clears the map or erases that mapping during the lookup erased a released slot
        (tree links / tree size); eraseAt now refuses a slot that is free or beyond the used prefix (CME).
  The mutable cursors' own remove erases the returned slot itself (the donor's set-cursor shape, eraseAt), so no
  comparator lookup runs inside it (the differential showed a key-based own remove re-entered by a clearing
  comparator: the lookup misses, the clear passes for the erase and the cursor walks released slots).

Engine of m3/collections/qualification/census2-c2-20261106/apply.py v1 (#2565 lineage): anchored "sub" edits, every
anchor exactly once, LF payloads re-joined with the preimage's line ending; every rule names the git blob of its
preimage at the base commit; a target must equal the preimage (applied) or the postimage (no-op), anything else is a
drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and
EDITS.tsv (file, before/after sha256, behaviours, donor blobs, rule) is a fixed point. --check writes nothing and exits
1 when a run would change a file.

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
BASE_COMMIT = "baafdafdc81e"
DONOR_COMMIT = "d5ad5090d87ef8477a56317e60ac0d29c65029f7"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "behaviours", "crate", "donor_blobs", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
CRATE = "m3jdk21-20261019"
ANCHOR = "//FLLDCIM: MRL - census2 P2-098, recipe FILTERED_VIEWS_OWNER v1 (crate " + CRATE


class Refused(Exception):
    pass


READONLY_HELPERS = (
    "\n"
    "    " + ANCHOR + ", donor blob a6d82d9a076f = retained variant pr1463): owner epoch before the cursor opens\n"
    "    /** The array-backed AVL owner of an M3 sorted map or set (bounds and direction included), else null. */\n"
    "    private static M3TreeMap<?, ?> sortedOwner(Object source) {\n"
    "        if (source instanceof M3TreeMap<?, ?> map) return map;\n"
    "        return source instanceof M3TreeSet<?> set ? set.indexedOwner() : null;\n"
    "    }\n"
    "\n"
    "    private static int revisionOf(M3TreeMap<?, ?> owner) { return owner == null ? 0 : owner.revision(); }\n"
    "\n"
    "    /**\n"
    "     * Check a sorted M3 owner's revision, read before {@code cursor} was opened, at every call\n"
    "     * and after every step; any other source keeps its own iterator policy. Callbacks are\n"
    "     * unchanged, and a predicate exception still precedes the check of the step that raised it.\n"
    "     */\n"
    "    private static <E> Iterator<E> ownerChecked(M3TreeMap<?, ?> owner, int expected,\n"
    "            Iterator<E> cursor) {\n"
    "        if (owner == null) return cursor;\n"
    "        return new Iterator<>() {\n"
    "            @Override public boolean hasNext() {\n"
    "                owner.check(expected);\n"
    "                boolean ready = cursor.hasNext();\n"
    "                owner.check(expected);\n"
    "                return ready;\n"
    "            }\n"
    "            @Override public E next() {\n"
    "                owner.check(expected);\n"
    "                E result = cursor.next();\n"
    "                owner.check(expected);\n"
    "                return result;\n"
    "            }\n"
    "            @Override public void remove() { throw readonly(); }\n"
    "        };\n"
    "    }\n")

READONLY_EDITS = [
    ("// Reconciled 2026: retain the qualified guarded reverse atom after additive merge.\n",
     "// Reconciled 2026: retain the qualified guarded reverse atom after additive merge.\n"
     "// Modified 2026 by Hitesh Soliwal and contributors: fail-fast owner epoch on filtered cursors over M3 sorted"
     " owners (census2 P2-098).\n"),
    (" * existing iterator follows its underlying iterator's concurrent-modification policy.\n",
     " * existing iterator follows its underlying iterator's concurrent-modification policy.\n"
     " * Over an M3TreeMap or M3TreeSet owner that policy is fail-fast at every cursor call: the\n"
     " * owner revision is read before the backing cursor opens and checked before and after each\n"
     " * step, so prepared and exhausted cursors and mutating predicates or comparators also throw\n"
     " * ConcurrentModificationException.\n"),
    ("    private static UnsupportedOperationException readonly() {\n"
     "        return new UnsupportedOperationException(\"read-only filtered view\");\n"
     "    }\n",
     "    private static UnsupportedOperationException readonly() {\n"
     "        return new UnsupportedOperationException(\"read-only filtered view\");\n"
     "    }\n" + READONLY_HELPERS),
    ("        @Override public Iterator<E> iterator() {\n"
     "            if (iterationView(source) == null) return filtered(source.iterator(), predicate);\n"
     "            IterationPlan<E> plan = iterationPlan();\n"
     "            return filtered(plan.backing.iterator(), value -> {\n"
     "                for (Predicate<? super E> filter : plan.filters) {\n"
     "                    if (!filter.test(value)) return false;\n"
     "                }\n"
     "                return true;\n"
     "            });\n"
     "        }\n",
     "        @Override public Iterator<E> iterator() {\n"
     "            if (iterationView(source) == null) {\n"
     "                M3TreeMap<?, ?> owner = sortedOwner(source);\n"
     "                int expected = revisionOf(owner);\n"
     "                return ownerChecked(owner, expected, filtered(source.iterator(), predicate));\n"
     "            }\n"
     "            IterationPlan<E> plan = iterationPlan();\n"
     "            M3TreeMap<?, ?> owner = sortedOwner(plan.backing);\n"
     "            int expected = revisionOf(owner);\n"
     "            return ownerChecked(owner, expected, filtered(plan.backing.iterator(), value -> {\n"
     "                for (Predicate<? super E> filter : plan.filters) {\n"
     "                    if (!filter.test(value)) return false;\n"
     "                }\n"
     "                return true;\n"
     "            }));\n"
     "        }\n"),
    ("        @Override public Iterator<E> descendingIterator() {\n"
     "            return filtered(nav.descendingSet().iterator(), predicate);\n"
     "        }\n",
     "        @Override public Iterator<E> descendingIterator() {\n"
     "            M3TreeMap<?, ?> owner = sortedOwner(nav);\n"
     "            int expected = revisionOf(owner);\n"
     "            return ownerChecked(owner, expected, filtered(nav.descendingSet().iterator(), predicate));\n"
     "        }\n"),
    ("                @Override public Iterator<Entry<K, V>> iterator() {\n"
     "                    Iterator<Entry<K, V>> cursor = sorted.entrySet().iterator();\n"
     "                    Iterator<Entry<K, V>> filtered = M3FilteredViews.filtered(cursor,\n"
     "                            item -> predicate.test(item.getKey(), item.getValue()));\n"
     "                    return new Iterator<>() {\n"
     "                        @Override public boolean hasNext() { return filtered.hasNext(); }\n"
     "                        @Override public Entry<K, V> next() {\n"
     "                            return new SimpleImmutableEntry<>(filtered.next());\n"
     "                        }\n"
     "                        @Override public void remove() { throw readonly(); }\n"
     "                    };\n"
     "                }\n",
     "                @Override public Iterator<Entry<K, V>> iterator() {\n"
     "                    M3TreeMap<?, ?> owner = sortedOwner(sorted);\n"
     "                    int expected = revisionOf(owner);\n"
     "                    Iterator<Entry<K, V>> cursor = sorted.entrySet().iterator();\n"
     "                    Iterator<Entry<K, V>> filtered = M3FilteredViews.filtered(cursor,\n"
     "                            item -> predicate.test(item.getKey(), item.getValue()));\n"
     "                    // The entry copy reads the value through the owner (a comparator lookup):\n"
     "                    // the owner check after next() also covers that callback.\n"
     "                    return ownerChecked(owner, expected, new Iterator<Entry<K, V>>() {\n"
     "                        @Override public boolean hasNext() { return filtered.hasNext(); }\n"
     "                        @Override public Entry<K, V> next() {\n"
     "                            return new SimpleImmutableEntry<>(filtered.next());\n"
     "                        }\n"
     "                        @Override public void remove() { throw readonly(); }\n"
     "                    });\n"
     "                }\n"),
]

SLOT_CURSOR = (
    "    " + ANCHOR + ", donor blob 883195b9b194 = retained variant pr1463): fail-fast physical-slot cursor\n"
    "    /**\n"
    "     * Physical-slot cursor shared by the set and map views over an M3 owner (an M3TreeMap or\n"
    "     * the owner of an M3TreeSet; bounds and direction included). The owner revision is read\n"
    "     * before the first slot and checked at every call and after every predicate call and slot\n"
    "     * step: an owner change, a mutating predicate or comparator, and a prepared or exhausted\n"
    "     * cursor fail fast, and remove checks it before its next-required state. The predicate runs\n"
    "     * once per slot. remove erases the returned slot itself (no comparator lookup, so no\n"
    "     * re-entrant window) and keeps the prepared successor slot.\n"
    "     */\n"
    "    private abstract static class SlotCursor<T> implements Iterator<T> {\n"
    "        private final M3TreeMap<?, ?> slots;\n"
    "        private int expected;\n"
    "        private int pending;\n"
    "        private int last = -1;\n"
    "        private boolean prepared;\n"
    "\n"
    "        SlotCursor(M3TreeMap<?, ?> slots, boolean fromLast) {\n"
    "            this.slots = slots;\n"
    "            expected = slots.revision();\n"
    "            pending = fromLast ? slots.lastSlot() : slots.firstSlot();\n"
    "        }\n"
    "\n"
    "        /** Predicate visibility of a live slot. */\n"
    "        abstract boolean visible(int slot);\n"
    "        /** The following slot in this cursor's direction, or -1. */\n"
    "        abstract int step(int slot);\n"
    "        /** The caller-visible element of a live slot. */\n"
    "        abstract T take(int slot);\n"
    "\n"
    "        final void check() { slots.check(expected); }\n"
    "\n"
    "        @Override public final boolean hasNext() {\n"
    "            check();\n"
    "            if (prepared) return true;\n"
    "            while (pending >= 0) {\n"
    "                boolean visible = visible(pending);\n"
    "                check();\n"
    "                if (visible) {\n"
    "                    prepared = true;\n"
    "                    return true;\n"
    "                }\n"
    "                pending = step(pending);\n"
    "                check();\n"
    "            }\n"
    "            return false;\n"
    "        }\n"
    "\n"
    "        @Override public final T next() {\n"
    "            if (!hasNext()) throw new NoSuchElementException();\n"
    "            int slot = pending;\n"
    "            pending = step(slot);\n"
    "            check();\n"
    "            prepared = false;\n"
    "            T result = take(slot);\n"
    "            last = slot;\n"
    "            return result;\n"
    "        }\n"
    "\n"
    "        @Override public final void remove() {\n"
    "            check();\n"
    "            if (last < 0) throw new IllegalStateException(\"next required\");\n"
    "            slots.eraseAt(last);\n"
    "            expected = slots.revision();\n"
    "            last = -1;\n"
    "        }\n"
    "    }\n"
    "\n")

MAP_SLOTS = (
    "        /** The array-backed AVL owner of an M3 map (bounds and direction included), else null. */\n"
    "        private M3TreeMap<K, V> slotOwner() {\n"
    "            return source instanceof M3TreeMap<K, V> m3 ? m3 : null;\n"
    "        }\n"
    "\n"
    "        /** Entries and values of an M3 owner share one slot walk; only the projection differs. */\n"
    "        private abstract class MapSlotCursor<T> extends SlotCursor<T> {\n"
    "            final M3TreeMap<K, V> owner;\n"
    "\n"
    "            MapSlotCursor(M3TreeMap<K, V> owner) {\n"
    "                super(owner, false);\n"
    "                this.owner = owner;\n"
    "            }\n"
    "\n"
    "            @Override final boolean visible(int slot) {\n"
    "                return predicate.test(owner.keyAt(slot), owner.valueAt(slot));\n"
    "            }\n"
    "            @Override final int step(int slot) { return owner.nextSlot(slot); }\n"
    "        }\n"
    "\n"
    "        " + ANCHOR + ", donor blob 883195b9b194 = retained variant pr1463): once-per-entry values view\n"
    "        /**\n"
    "         * Over an M3 owner values() walks the same slot cursor: the predicate runs once per\n"
    "         * entry and each value is the slot's current payload. Other owners keep\n"
    "         * AbstractMap.values() (an entry walk whose LiveEntry.getValue re-tests visibility).\n"
    "         */\n"
    "        @Override public Collection<V> values() {\n"
    "            M3TreeMap<K, V> owner = slotOwner();\n"
    "            if (owner == null) return super.values();\n"
    "            return new AbstractCollection<>() {\n"
    "                @Override public int size() { return MapView.this.size(); }\n"
    "                @Override public boolean isEmpty() { return MapView.this.isEmpty(); }\n"
    "                @Override public boolean contains(Object value) { return MapView.this.containsValue(value); }\n"
    "                @Override public void clear() { MapView.this.clear(); }\n"
    "                @Override public Iterator<V> iterator() {\n"
    "                    return new MapSlotCursor<V>(owner) {\n"
    "                        @Override V take(int slot) { return owner.valueAt(slot); }\n"
    "                    };\n"
    "                }\n"
    "            };\n"
    "        }\n"
    "\n")

MUTABLE_EDITS = [
    ("// Modified 2026 by Hitesh Soliwal and contributors: propagate mutable navigation comparator failures.\n",
     "// Modified 2026 by Hitesh Soliwal and contributors: propagate mutable navigation comparator failures.\n"
     "// Modified 2026 by Hitesh Soliwal and contributors: fail-fast physical-slot cursors and a once-per-entry values"
     " view over M3 owners (census2 P2-098).\n"),
    ("import java.util.AbstractMap;\n",
     "import java.util.AbstractCollection;\nimport java.util.AbstractMap;\n"),
    (" * iterators are cursor-based and do not promise the JDK's fail-fast detection.\n",
     " * iterators are cursor-based and do not promise the JDK's fail-fast detection.\n"
     " * Over an M3TreeMap or M3TreeSet owner the cursors walk its physical slots and are\n"
     " * fail-fast: the owner revision is read before the first slot and checked at every call and\n"
     " * after every predicate call and slot step; the predicate runs once per slot,\n"
     " * values() reads the slot's current value without a second predicate call, and an own\n"
     " * remove keeps the prepared successor.\n"),
    ("    private static final class SetView<E> extends AbstractSet<E> implements NavigableSet<E> {\n",
     SLOT_CURSOR +
     "    private static final class SetView<E> extends AbstractSet<E> implements NavigableSet<E> {\n"),
    ("        private Iterator<E> cursor(boolean descending) {\n"
     "            return new Iterator<>() {\n",
     "        private Iterator<E> cursor(boolean descending) {\n"
     "            M3TreeMap<E, ?> owner = source instanceof M3TreeSet<E> m3 ? m3.indexedOwner() : null;\n"
     "            if (owner != null) return new SlotCursor<E>(owner, descending) {\n"
     "                @Override boolean visible(int slot) { return predicate.test(owner.keyAt(slot)); }\n"
     "                @Override int step(int slot) {\n"
     "                    return descending ? owner.previousIndexedSlot(slot) : owner.nextSlot(slot);\n"
     "                }\n"
     "                @Override E take(int slot) { return owner.keyAt(slot); }\n"
     "            };\n"
     "            return new Iterator<>() {\n"),
    ("        private Iterator<Entry<K, V>> entryCursor() {\n"
     "            return new Iterator<>() {\n",
     "        private Iterator<Entry<K, V>> entryCursor() {\n"
     "            M3TreeMap<K, V> owner = slotOwner();\n"
     "            if (owner != null) return new MapSlotCursor<Entry<K, V>>(owner) {\n"
     "                @Override Entry<K, V> take(int slot) { return new LiveEntry(owner.keyAt(slot)); }\n"
     "            };\n"
     "            return new Iterator<>() {\n"),
    ("        private final class LiveEntry implements Entry<K, V> {\n",
     MAP_SLOTS + "        private final class LiveEntry implements Entry<K, V> {\n"),
]

TREEMAP_EDITS = [
    ("// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type"
     " adaptation.\n",
     "// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type"
     " adaptation.\n"
     "// Modified 2026 by Hitesh Soliwal and contributors: refuse erasing a slot a re-entrant comparator already"
     " released (census2 P2-098 B4).\n"),
    (" * This collection is not thread-safe. Providers must not mutate its shared mappings.\n",
     " * This collection is not thread-safe. Providers must not mutate its shared mappings.\n"
     " * A key removal whose comparator clears the map or erases that mapping during the lookup\n"
     " * throws ConcurrentModificationException instead of erasing a released slot.\n"),
    ("    @Override V eraseAt(int slot) { tree.writable(); V old = valueAt(slot); forgetEntryFacade(keyAt(slot));"
     " tree.erase(slot); return old; }\n",
     "    " + ANCHOR + ", found by its seeded differential): no erase of a released slot\n"
     "    @Override V eraseAt(int slot) {\n"
     "        // A slot located before a re-entrant comparator cleared the map or erased the mapping is\n"
     "        // free (height 0) or beyond the used prefix; erasing it would corrupt links and free list.\n"
     "        if (slot >= tree.used || tree.height[slot] == 0) { throw new java.util.ConcurrentModificationException(); }\n"
     "        tree.writable(); V old = valueAt(slot); forgetEntryFacade(keyAt(slot)); tree.erase(slot); return old;\n"
     "    }\n"),
]

# path, preimage blob, behaviours, donor blobs, rule note, edits
RULES = (
    (SRC + "M3FilteredViews.java", "92f50be37cd05b0d4c908c4c5092f274a625ffc8", "B1+B2",
     "post=a6d82d9a076f58d6fa18511c39b96255fd0a1adf pre=c0954c06227d96ab617b2845f4c04942438657e4"
     " (= pr1463 7e9b046c741e)",
     "ADOPT B1 (owner-revision check of prepared/exhausted read-only cursors) and B2 (predicate/comparator re-entrant"
     " mutation, epoch before the backing cursor opens) as one decorator over the existing generic cursors (iterator,"
     " descendingIterator, entrySet().iterator()) when the source is an M3TreeMap/M3TreeSet; no callback added",
     READONLY_EDITS),
    (SRC + "M3MutableFilteredViews.java", "e1ce7319289b441b51853d90df3dd24dc63d12ef", "B1+B2+B3",
     "post=883195b9b194b1d0f371474d775b75098305300d pre=92bc7b2fe188f952bf0ffeec4f38e778f94c312f"
     " (= pr1463 591905084783)",
     "ADOPT B1, B2 and B3: the donor's physical-slot set/entry/values cursors (one shared SlotCursor) for"
     " M3TreeMap/M3TreeSet owners; the navigation cursors and AbstractMap.values() stay for other owners",
     MUTABLE_EDITS),
    (SRC + "M3TreeMap.java", "00ad0ecae99e67d11830ee71f01efa94ed242562", "B4",
     "- (no donor: found by this leaf's seeded differential, base and head alike)",
     "FIX B4b: a key removal whose comparator cleared the owner or erased the located mapping during the lookup"
     " erased a dead slot (verifyInvariants: tree links / tree size; FilteredViewsTreeMapRepro R4); eraseAt now"
     " refuses a slot that is no longer owned. B4a (the P2-098 'ordering' row) needs no edit: verifyInvariants ran the"
     " test's still-armed comparator (R1/R3)",
     TREEMAP_EDITS),
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
    for rel, blob, behaviours, donors, note, edits in RULES:
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
        rows.append((rel, sha256(pre), sha256(post), behaviours, CRATE, donors, note))
        print("EDIT %s %s %s" % (behaviours, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("FILTERED_VIEWS_OWNER recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
