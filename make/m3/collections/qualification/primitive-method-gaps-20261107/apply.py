#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe PRIMITIVE_METHOD_GAPS v1: positional access and small method gaps on M3 sorted/hash primitives.

Private-repo scout rank 2 (S:/m3-scratch/eb-conv/scout-private/PROPOSED_LEAVES.tsv): ABSORB the method gap of the
m3scale primitive donors into the existing jdk.m3.collections owners, semantics only (no donor class is copied).
Leaves: G1 M3LongTreeSet get(int)/indexOf(long)/addAllSorted(long[],int,int) on a lazily rebuilt page-prefix lane;
G2 M3LongLongTreeMap keyAt/valueAt/indexOfKey/putAllSorted(long[],long[],int,int); G3 trimToSize on M3IntIntHashMap,
M3IntLongHashMap and M3LongLongHashMap; G4 M3IntArrayDeque.get(int). G1 also updates the pinned field layout of
M3LongTreeSetTest.retainsOnlyPrimitiveLanes (one int[] lane and one int more). DECISION.tsv records ABSORB/ALREADY/NEC
per member, NEC.tsv the same-shape/different-contract rows, PROVENANCE.tsv the donor blob of every method.

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, every anchor
exactly once). Every rule names the git blob of its preimage at the base commit; the on-disk file, normalized to LF,
must equal the preimage (the rule applies) or the postimage (already applied, no-op); anything else is drift and the
recipe refuses (exit 2) before writing anything. Payloads are LF and re-joined with the file's line ending, so a
CRLF checkout yields the same blobs. Idempotent: a second run reports changed=0 and EDITS.tsv (file, before/after
sha256 and blob of the LF content, leaf, donor blobs, rule) is a fixed point. --leaf applies one leaf only (one commit
per leaf); --check writes nothing and exits 1 when a run would change a file.

usage: python -I apply.py [--root DIR] [--check] [--leaf G1|G2|G3|G4]
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
DONOR_COMMIT = "3385d79a2968c770aedf1d3398b2a62f05ebc092"  # m3scale donor develop head; repository and paths: PROVENANCE.tsv
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "before_blob", "after_blob", "leaf", "donor_blobs", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
LF, CRLF = b"\n", b"\r\n"


class Refused(Exception):
    pass


TREE_SET_DOC = (
    " * {@link M3LongSortedViews}.</p>\n */\npublic final class M3LongTreeSet",
    " * {@link M3LongSortedViews}.</p>\n"
    " *\n"
    " * <p>Positional access ({@link #get(int)}, {@link #indexOf(long)}) binary-searches a page-prefix\n"
    " * lane (the rank of each logical page's first value) that is rebuilt lazily, in O(pages), by the\n"
    " * first positional call after a mutation (the revision is {@code modCount}); mutations pay nothing\n"
    " * for it and repeated positional calls are O(log n) and allocation-free.\n"
    " * {@link #addAllSorted(long[], int, int)} admits a sorted slice and fills fresh full pages with\n"
    " * the values above the current last one. The positional contracts are those of the m3scale donors\n"
    " * {@code LongGapSortedSet} and {@code LongSortedSet}.</p>\n"
    " */\npublic final class M3LongTreeSet")

TREE_SET_FIELDS = (
    "    private int modCount;\n",
    "    private int modCount;\n"
    "    //FLLDCIM: MRL - primitive-method-gaps G1: lazy page-prefix lane for positional access (revision = modCount)\n"
    "    /** First rank of each logical page; valid only while {@code prefixRevision == modCount}. */\n"
    "    private int[] pagePrefix;\n"
    "    private int prefixRevision;\n")

TREE_SET_PUBLIC = (
    "    @Override\n    public int size() {\n",
    "    //FLLDCIM: MRL - primitive-method-gaps G1 get(int) (donor LongGapSortedSet.get, blob 87dc58978af6)\n"
    "    /**\n"
    "     * Returns the value of zero-based rank {@code index} in ascending order: a binary search of the\n"
    "     * page-prefix lane, then one page read. O(log n) and allocation-free while no mutation\n"
    "     * intervenes; the first positional call after a mutation rebuilds the lane in O(pages).\n"
    "     *\n"
    "     * @throws IndexOutOfBoundsException if {@code index} is outside {@code [0, size())}\n"
    "     */\n"
    "    public long get(int index) {\n"
    "        Objects.checkIndex(index, size);\n"
    "        int[] prefix = pagePrefix();\n"
    "        int page = Arrays.binarySearch(prefix, 0, pageCount, index);\n"
    "        if (page < 0) {\n"
    "            // Absent: the rank lies in the page before the insertion point (page 0 starts at 0).\n"
    "            page = -page - 2;\n"
    "        }\n"
    "        return values[pageOffset(page) + index - prefix[page]];\n"
    "    }\n"
    "\n"
    "    //FLLDCIM: MRL - primitive-method-gaps G1 indexOf(long) (donor LongSortedSet.indexOf, blob d2e0467cb0c4)\n"
    "    /**\n"
    "     * Returns the rank of {@code value} when present, else {@code -(insertion rank) - 1}: the\n"
    "     * {@link Arrays#binarySearch(long[], long)} convention of the donor (unlike\n"
    "     * {@link M3LongSortedSnapshot#indexOf(long)}, which returns -1). O(log n) and allocation-free\n"
    "     * while no mutation intervenes.\n"
    "     */\n"
    "    public int indexOf(long value) {\n"
    "        if (pageCount == 0) {\n"
    "            return -1;\n"
    "        }\n"
    "        int page = locatePage(value);\n"
    "        int offset = pageOffset(page);\n"
    "        int found = Arrays.binarySearch(values, offset, offset + pageCounts[page], value);\n"
    "        // Physical result to rank: shift by the page's first rank minus its physical offset.\n"
    "        int shift = pagePrefix()[page] - offset;\n"
    "        return found >= 0 ? found + shift : found - shift;\n"
    "    }\n"
    "\n"
    "    //FLLDCIM: MRL - primitive-method-gaps G1 addAllSorted (donor LongGapSortedSet.addAllSorted, blob 87dc58978af6)\n"
    "    /**\n"
    "     * Admits the nondecreasing slice {@code sorted[from, to)} and returns the number of new values;\n"
    "     * duplicates in the slice and values already present collapse. The range and the order are\n"
    "     * validated before any change. Values above the current last value fill the tail page and then\n"
    "     * fresh full pages in one pass; the others insert one at a time (O(log n) plus one page shift\n"
    "     * each). A capacity failure part-way keeps the values admitted before it.\n"
    "     *\n"
    "     * @throws NullPointerException if {@code sorted} is null\n"
    "     * @throws IndexOutOfBoundsException if {@code [from, to)} is not a range of {@code sorted}\n"
    "     * @throws IllegalArgumentException if the slice is not sorted ascending\n"
    "     */\n"
    "    public int addAllSorted(long[] sorted, int from, int to) {\n"
    "        Objects.requireNonNull(sorted, \"sorted\");\n"
    "        Objects.checkFromToIndex(from, to, sorted.length);\n"
    "        for (int i = from + 1; i < to; i++) {\n"
    "            if (sorted[i] < sorted[i - 1]) {\n"
    "                throw new IllegalArgumentException(\"input must be sorted ascending\");\n"
    "            }\n"
    "        }\n"
    "        int before = size;\n"
    "        int tail = size == 0 ? from : M3LaneSearch.upperBound(sorted, from, to, last());\n"
    "        for (int i = from; i < tail; i++) {\n"
    "            add(sorted[i]);\n"
    "        }\n"
    "        appendAscending(sorted, tail, to);\n"
    "        return size - before;\n"
    "    }\n"
    "\n"
    "    @Override\n    public int size() {\n")

TREE_SET_PAYLOAD = (
    "    /** Retained primitive lane bytes: the value lane plus the four page lanes. */\n"
    "    public long payloadBytes() {\n"
    "        return (long) values.length * Long.BYTES + (long) pageMaxima.length * Long.BYTES\n"
    "                + ((long) pageCounts.length + pageSlot.length + freeSlots.length) * Integer.BYTES;\n",
    "    /** Retained primitive lane bytes: the value lane, the four page lanes and the prefix lane once built. */\n"
    "    public long payloadBytes() {\n"
    "        //FLLDCIM: MRL - primitive-method-gaps G1: the lazily built prefix lane is retained payload\n"
    "        long prefixBytes = pagePrefix == null ? 0L : (long) pagePrefix.length * Integer.BYTES;\n"
    "        return (long) values.length * Long.BYTES + (long) pageMaxima.length * Long.BYTES\n"
    "                + ((long) pageCounts.length + pageSlot.length + freeSlots.length) * Integer.BYTES\n"
    "                + prefixBytes;\n")

TREE_SET_TRIM_STAGE = (
    "        int[] nextFree = new int[pages];\n",
    "        int[] nextFree = new int[pages];\n"
    "        //FLLDCIM: MRL - primitive-method-gaps G1: the prefix lane shrinks with the page lanes; logical counts, so\n"
    "        // the prefix sums and their revision, are unchanged by a repack\n"
    "        int[] nextPrefix = pagePrefix == null ? null : Arrays.copyOf(pagePrefix, pages);\n")

TREE_SET_TRIM_PUBLISH = (
    "        freeSlots = nextFree;\n",
    "        freeSlots = nextFree;\n"
    "        pagePrefix = nextPrefix;\n")

TREE_SET_PRIVATE = (
    "    /** Value at {@code candidate} when it lies in the page, else the last value of the page before. */\n",
    "    //FLLDCIM: MRL - primitive-method-gaps G1: lazy prefix rebuild, cold path, O(pages) at most once per revision\n"
    "    /** The page-prefix lane at the current revision; reallocated only after the page lanes grew. */\n"
    "    private int[] pagePrefix() {\n"
    "        int[] prefix = pagePrefix;\n"
    "        if (prefix != null && prefixRevision == modCount) {\n"
    "            return prefix;\n"
    "        }\n"
    "        if (prefix == null || prefix.length < pageCount) {\n"
    "            prefix = new int[pageCounts.length];\n"
    "        }\n"
    "        int rank = 0;\n"
    "        for (int page = 0; page < pageCount; page++) {\n"
    "            prefix[page] = rank;\n"
    "            rank += pageCounts[page];\n"
    "        }\n"
    "        pagePrefix = prefix;\n"
    "        prefixRevision = modCount;\n"
    "        return prefix;\n"
    "    }\n"
    "\n"
    "    /** Appends a nondecreasing slice whose values all exceed the current last value. */\n"
    "    private void appendAscending(long[] sorted, int from, int to) {\n"
    "        if (from == to) {\n"
    "            return;\n"
    "        }\n"
    "        // Count the change first: a capacity failure part-way must still invalidate the prefix lane.\n"
    "        modCount++;\n"
    "        for (int i = from; i < to; i++) {\n"
    "            if (i == from || sorted[i] != sorted[i - 1]) {\n"
    "                appendValue(sorted[i]);\n"
    "            }\n"
    "        }\n"
    "    }\n"
    "\n"
    "    /** Appends one value above the current last value to the tail page, opening a page when full. */\n"
    "    private void appendValue(long value) {\n"
    "        int page = pageCount - 1;\n"
    "        if (page < 0 || pageCounts[page] == pageCapacity) {\n"
    "            page = openTailPage();\n"
    "        }\n"
    "        int count = pageCounts[page];\n"
    "        values[pageOffset(page) + count] = value;\n"
    "        pageCounts[page] = count + 1;\n"
    "        pageMaxima[page] = value;\n"
    "        size++;\n"
    "    }\n"
    "\n"
    "    /** Opens an empty logical page after the last one, allocating as {@link #splitPage} does. */\n"
    "    private int openTailPage() {\n"
    "        ensurePages(pageCount + 1);\n"
    "        int slot = allocateSlot();\n"
    "        pageSlot[pageCount] = slot;\n"
    "        pageCounts[pageCount] = 0;\n"
    "        return pageCount++;\n"
    "    }\n"
    "\n"
    "    /** Value at {@code candidate} when it lies in the page, else the last value of the page before. */\n")

TREE_MAP_DOC = (
    " * backend, not a {@code java.util.TreeMap} replacement.</p>\n */\n",
    " * backend, not a {@code java.util.TreeMap} replacement.</p>\n"
    " *\n"
    " * <p>Positional access ({@link #keyAt(int)}, {@link #valueAt(int)}, {@link #indexOfKey(long)})\n"
    " * reads the sorted lanes directly: O(1) by rank and O(log n) by key, allocation-free, with the\n"
    " * contracts of the m3scale donor {@code LongLongSortedMap}.</p>\n"
    " */\n")

TREE_MAP_PUBLIC = (
    "    @Override\n    public boolean containsKey(long key) {\n",
    "    //FLLDCIM: MRL - primitive-method-gaps G2 putAllSorted range (donor LongLongGapSortedMap.putAllSorted, blob 58df86d7628f)\n"
    "    /**\n"
    "     * Merges the slice {@code [from, to)} of parallel nondecreasing key and value lanes with the\n"
    "     * contract of {@link #putAllSorted(long[], long[])}: the last incoming value wins, present keys\n"
    "     * are replaced and the result is the number of new keys (the donor returns nothing). Lane\n"
    "     * lengths must match and the range must lie inside them; both are checked before any change.\n"
    "     * A proper slice is staged as two copies, so the validated backward merge and its capacity\n"
    "     * rule are reused unchanged.\n"
    "     *\n"
    "     * @throws IndexOutOfBoundsException if {@code [from, to)} is not a range of the lanes\n"
    "     */\n"
    "    public int putAllSorted(long[] sourceKeys, long[] sourceValues, int from, int to) {\n"
    "        Objects.requireNonNull(sourceKeys, \"sourceKeys\");\n"
    "        Objects.requireNonNull(sourceValues, \"sourceValues\");\n"
    "        if (sourceKeys.length != sourceValues.length) {\n"
    "            throw new IllegalArgumentException(\"key/value length mismatch\");\n"
    "        }\n"
    "        Objects.checkFromToIndex(from, to, sourceKeys.length);\n"
    "        if (from == 0 && to == sourceKeys.length) {\n"
    "            return putAllSorted(sourceKeys, sourceValues);\n"
    "        }\n"
    "        return putAllSorted(Arrays.copyOfRange(sourceKeys, from, to),\n"
    "                Arrays.copyOfRange(sourceValues, from, to));\n"
    "    }\n"
    "\n"
    "    //FLLDCIM: MRL - primitive-method-gaps G2 keyAt/valueAt/indexOfKey (donor LongLongSortedMap, blob 5b0e75813d6c)\n"
    "    /** Key of zero-based rank {@code index}; throws IndexOutOfBoundsException outside {@code [0, size())}. */\n"
    "    public long keyAt(int index) {\n"
    "        return keys[Objects.checkIndex(index, size)];\n"
    "    }\n"
    "\n"
    "    /** Value at zero-based rank {@code index}; throws IndexOutOfBoundsException outside {@code [0, size())}. */\n"
    "    public long valueAt(int index) {\n"
    "        return values[Objects.checkIndex(index, size)];\n"
    "    }\n"
    "\n"
    "    /**\n"
    "     * Rank of {@code key} when present, else {@code -(insertion rank) - 1}: the\n"
    "     * {@link Arrays#binarySearch(long[], int, int, long)} convention of the donor.\n"
    "     */\n"
    "    public int indexOfKey(long key) {\n"
    "        return Arrays.binarySearch(keys, 0, size, key);\n"
    "    }\n"
    "\n"
    "    @Override\n    public boolean containsKey(long key) {\n")


def int_map_edits(value_type: str, donor: str, blob: str):
    """G3 trimToSize for the zero-key-off-lane int-keyed maps (M3IntIntHashMap, M3IntLongHashMap)."""
    trim = (
        "    /** Visits mappings: the zero key first (if present), then slot order. */\n",
        "    //FLLDCIM: MRL - primitive-method-gaps G3 trimToSize (donor " + donor + ".trimToSize, blob " + blob + ")\n"
        "    /**\n"
        "     * Rebuilds the probe lane at the smallest capacity whose load threshold holds the live keys\n"
        "     * (the off-lane zero key needs no slot); a no-op when that is the current capacity. A rebuild\n"
        "     * re-inserts keys in slot order, so iteration follows the trimmed table, and it invalidates\n"
        "     * live iterators. Allocation precedes publication: a failure leaves the map unchanged.\n"
        "     */\n"
        "    public void trimToSize() {\n"
        "        int target = capacityFor(size - (hasZero ? 1 : 0));\n"
        "        if (target < keys.length) {\n"
        "            rebuildProbeLane(target);\n"
        "            modCount++;\n"
        "        }\n"
        "    }\n"
        "\n"
        "    /** Visits mappings: the zero key first (if present), then slot order. */\n")
    helper = (
        "    private static int capacityFor(int expectedSize) {\n",
        "    //FLLDCIM: MRL - primitive-method-gaps G3: rehash(int) without its growth guard, for a smaller capacity\n"
        "    /** Re-inserts the live probe keys in slot order into fresh lanes of {@code capacity} slots. */\n"
        "    private void rebuildProbeLane(int capacity) {\n"
        "        int[] nextKeys = new int[capacity];\n"
        "        " + value_type + "[] nextValues = new " + value_type + "[capacity];\n"
        "        int nextMask = capacity - 1;\n"
        "        for (int i = 0; i < keys.length; i++) {\n"
        "            if (keys[i] != 0) {\n"
        "                int pos = emptySlot(nextKeys, keys[i], nextMask);\n"
        "                nextKeys[pos] = keys[i];\n"
        "                nextValues[pos] = values[i];\n"
        "            }\n"
        "        }\n"
        "        keys = nextKeys;\n"
        "        values = nextValues;\n"
        "        mask = nextMask;\n"
        "        threshold = thresholdFor(capacity);\n"
        "    }\n"
        "\n"
        "    /** First empty slot of the probe run of {@code key} in a lane under construction. */\n"
        "    private static int emptySlot(int[] lane, int key, int laneMask) {\n"
        "        int pos = mix(key) & laneMask;\n"
        "        while (lane[pos] != 0) {\n"
        "            pos = (pos + 1) & laneMask;\n"
        "        }\n"
        "        return pos;\n"
        "    }\n"
        "\n"
        "    private static int capacityFor(int expectedSize) {\n")
    return [trim, helper]


LONG_MAP_TRIM = (
    "    public void forEach(M3LongLongConsumer consumer) {\n",
    "    //FLLDCIM: MRL - primitive-method-gaps G3 trimToSize (donor LongLongHashMap.trimToSize, blob 41a55a6f8646)\n"
    "    /**\n"
    "     * Rebuilds the lanes at the smallest capacity whose load limit holds the live keys, also when\n"
    "     * tombstones remain at the current capacity (as the donor does); a no-op otherwise. A rebuild\n"
    "     * re-inserts keys in slot order, so iteration follows the rebuilt table, and it invalidates\n"
    "     * live iterators. Allocation precedes publication: a failure leaves the map unchanged.\n"
    "     */\n"
    "    public void trimToSize() {\n"
    "        int target = capacityFor(size);\n"
    "        if (target < keys.length || occupied != size) {\n"
    "            rehash(target);\n"
    "            modCount++;\n"
    "        }\n"
    "    }\n"
    "\n"
    "    public void forEach(M3LongLongConsumer consumer) {\n")

DEQUE_GET = (
    "    /**\n     * Inspects the first value without removal.\n",
    "    //FLLDCIM: MRL - primitive-method-gaps G4 get(int) (donor IntDeque.get, blob 28abd2c9da20; sibling M3LongArrayDeque.get)\n"
    "    /**\n"
    "     * Inspects the value at zero-based position {@code index} from the head, without removal.\n"
    "     * @param index position from the head\n"
    "     * @return value at the position\n"
    "     * @throws IndexOutOfBoundsException if index is outside [0, size)\n"
    "     */\n"
    "    public int get(int index) {\n"
    "        return elements[(head + java.util.Objects.checkIndex(index, size)) & (elements.length - 1)];\n"
    "    }\n"
    "\n"
    "    /**\n     * Inspects the first value without removal.\n")

TREE_SET_TEST_LANES = (
    "        assertEquals(List.of(\"int\", \"int\", \"int\", \"int\", \"int\", \"int\", \"int[]\", \"int[]\", \"int[]\",\n"
    "                \"long[]\", \"long[]\"), fields);\n",
    "        //FLLDCIM: MRL - primitive-method-gaps G1: + int[] pagePrefix and int prefixRevision (positional lane)\n"
    "        assertEquals(List.of(\"int\", \"int\", \"int\", \"int\", \"int\", \"int\", \"int\", \"int[]\", \"int[]\", \"int[]\",\n"
    "                \"int[]\", \"long[]\", \"long[]\"), fields);\n")

M3SCALE = "m3scale@3385d79a:primitive/"  # donor package directory; full paths in PROVENANCE.tsv

# leaf, path, preimage blob, donor blobs, rule note, edits [(old, new)]
RULES = (
    ("G1", SRC + "M3LongTreeSet.java", "1befcc68c3ed8f7d0988d032b5f1f2860b441d16",
     M3SCALE + "LongGapSortedSet.java=87dc58978af64e0caa57ddfb969fcb19a9001287 "
     + M3SCALE + "LongSortedSet.java=d2e0467cb0c4def6f04fbcc38b0d1a4e7eab488a",
     "ABSORB get(int)+indexOf(long)+addAllSorted(long[],int,int); lazy page-prefix lane keyed by modCount (measured:"
     " BENCH.tsv); prefix lane counted by payloadBytes and shrunk by trimToSize",
     [TREE_SET_DOC, TREE_SET_FIELDS, TREE_SET_PUBLIC, TREE_SET_PAYLOAD, TREE_SET_TRIM_STAGE, TREE_SET_TRIM_PUBLISH,
      TREE_SET_PRIVATE]),
    ("G1", "m3/collections/src/test/java/jdk/m3/collections/M3LongTreeSetTest.java",
     "53706e844252420bb13348a2b7f3d6f7bad75f91", "-",
     "pinned field layout of retainsOnlyPrimitiveLanes gains the int[] prefix lane and its int revision (still"
     " primitive lanes only)", [TREE_SET_TEST_LANES]),
    ("G2", SRC + "M3LongLongTreeMap.java", "030239bd93486c6d32453e4ab00a6fa92902768e",
     M3SCALE + "LongLongSortedMap.java=5b0e75813d6c8ba33a92b7fbe906747267a7f2ed "
     + M3SCALE + "LongLongGapSortedMap.java=58df86d7628f762c39a4bdc30bd345300298325b",
     "ABSORB keyAt/valueAt/indexOfKey + putAllSorted(long[],long[],int,int) delegating to the existing validated merge",
     [TREE_MAP_DOC, TREE_MAP_PUBLIC]),
    ("G3", SRC + "M3IntIntHashMap.java", "19571d3ed4fa8a329db3e91c63c23d3c97f45663",
     M3SCALE + "IntIntHashMap.java=593e6d32cafaf1d2d975a6ec6eff6ccc284897ea",
     "ABSORB trimToSize: shrink-only rebuild at capacityFor(probe keys); remove(int,int) (collections-arrays rank 6)"
     " untouched", int_map_edits("int", "IntIntHashMap", "593e6d32cafa")),
    ("G3", SRC + "M3IntLongHashMap.java", "f1bf75b45583903c5cb7a18bfcb8cf39a16da9a5",
     M3SCALE + "IntLongHashMap.java=3d7d6f89bf72f91677eb4db49e43566198918bf7",
     "ABSORB trimToSize: shrink-only rebuild at capacityFor(probe keys)",
     int_map_edits("long", "IntLongHashMap", "3d7d6f89bf72")),
    ("G3", SRC + "M3LongLongHashMap.java", "730d0baebca6f8b333757cf18057e7c1e70ce77c",
     M3SCALE + "LongLongHashMap.java=41a55a6f864604adb1d32e881415f9e490242432",
     "ABSORB trimToSize: rehash(capacityFor(size)) when smaller or when tombstones remain (donor rule)",
     [LONG_MAP_TRIM]),
    ("G4", SRC + "M3IntArrayDeque.java", "d8a8cdb0f028f27552420643514aeaad731455bc",
     M3SCALE + "IntDeque.java=28abd2c9da20d8d5db10afdba94354dcca0771c2",
     "ABSORB get(int) (Objects.checkIndex, as M3LongArrayDeque.get); hub owner: peer session 22 (#2538) keeps it as a"
     " union with Synexia on its next refresh", [DEQUE_GET]),
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


def postimage(pre: bytes, edits, path: str) -> bytes:
    text = pre.decode("utf-8")
    for old, new in edits:
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def plan(root: Path, rule):
    rleaf, rel, blob, donors, note, edits = rule
    target = root / rel
    if not target.exists():
        raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
    disk = target.read_bytes()
    eol = CRLF if CRLF in disk else LF
    canon = disk.replace(CRLF, LF)
    pre = canon if blob_sha(canon) == blob else git_blob(root, blob)
    post = postimage(pre, edits, rel)
    if blob_sha(canon) != blob and canon != post:
        raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(canon), blob))
    row = (rel, sha256(pre), sha256(post), blob_sha(pre), blob_sha(post), rleaf, donors, note)
    return target, canon == post, post.replace(LF, eol), row


def run(root: Path, check: bool, leaf: str | None) -> int:
    planned = [(rule[0], plan(root, rule)) for rule in RULES]  # every preimage verified before the first write
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, (target, done, post, row) in planned:
        state = "UNCHANGED" if done else ("PENDING" if leaf not in (None, rleaf) else "CHANGED")
        if state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += state != "PENDING"
        rows.append(row)
        print("EDIT %s %s %s" % (rleaf, state, row[0]))
    if not check:
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("PRIMITIVE_METHOD_GAPS recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
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
