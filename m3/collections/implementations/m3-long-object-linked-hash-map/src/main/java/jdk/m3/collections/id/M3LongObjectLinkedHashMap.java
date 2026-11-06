/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.id;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An insertion-ordered map from M3 ids to values: the M3 key lane of recipe ARRAY_MAP_FILL (family ID_MAP). An M3
 * key is an id (the canonical id of an interned M3String, the node id of an M3AST, any M3 key id), so the map keeps
 * {@code long} ids in a lane: the id is its own hash ({@code Atoms.foldId}), equality is {@code ==} on the lane
 * ({@code Atoms.findId}), and no key is boxed, hashed through {@code hashCode} or compared through {@code equals}.
 *
 * <p>Storage (pattern SLOTS): an open-addressed {@code int[] index} (0 empty, -1 tombstone, slot + 1), parallel
 * {@code long[] ids} and {@code Object[] values} lanes by slot and a {@code long[]} live mask. Slots are handed out
 * in increasing order and are not reused before a stable compaction, so slot order is insertion order, and
 * {@link #idAt}, {@link #valueAt} and {@link #indexOf} are select and rank over the mask. A put of a present id keeps
 * its position, as {@link java.util.LinkedHashMap} does. Not thread-safe.
 *
 * <p>Generated from templates/ID_MAP.java.tmpl and templates/ATOMS.java.inc by M3ArrayMapFill; change the templates,
 * never this file. Oracle: {@code java.util.LinkedHashMap<Long, V>} (M3IdMapDifferentialTest).
 *
 * @param <V> the value type
 */
public final class M3LongObjectLinkedHashMap<V> {

    private static final int MIN_SLOTS = 4;
    /** Bits per word of the live mask, as a shift and a mask (a word holds 1 << WORD_SHIFT slots). */
    private static final int WORD_SHIFT = 6;
    private static final int WORD_MASK = (1 << WORD_SHIFT) - 1;
    /** 2^32 divided by the golden ratio: Fibonacci hashing (Knuth); FOLD_SHIFT folds the high half down. */
    private static final int GOLDEN_RATIO_32 = 0x9E3779B9;
    private static final int FOLD_SHIFT = 16;
    /** The toString capacity hint per entry, as in the ARRAY_MAP members. */
    private static final int TO_STRING_CHARS_PER_ENTRY = 32;
    private static final String THIS_MAP = "(this Map)";
    private static final String MAP_OPEN = "{";
    private static final String MAP_CLOSE = "}";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String ENTRY_SEPARATOR = ", ";

    /** An action on one (id, value) entry. */
    @FunctionalInterface
    public interface IdValueConsumer<V> {
        /** Performs this action on the entry ID = VALUE. */
        void accept(long id, V value);
    }

    private int[] index;
    private long[] ids;
    private Object[] values;
    private long[] liveMask;
    private int used;
    private int size;
    private int modCount;

    /** An empty map with room for MIN_SLOTS ids before it grows. */
    public M3LongObjectLinkedHashMap() {
        this(MIN_SLOTS);
    }

    /** An empty map with room for EXPECTED ids before it grows. */
    public M3LongObjectLinkedHashMap(int expected) {
        if (expected < 0) {
            throw new IllegalArgumentException("negative expected size: " + expected);
        }
        allocate(Math.max(expected, MIN_SLOTS));
    }

    private void allocate(int slots) {
        index = new int[Atoms.tableSize(slots)];
        ids = new long[slots];
        values = new Object[slots];
        liveMask = new long[(slots + WORD_MASK) >>> WORD_SHIFT];
    }

    /** The number of ids. */
    public int size() {
        return size;
    }

    /** True when the map holds no id. */
    public boolean isEmpty() {
        return size == 0;
    }

    private int find(long id) {
        return Atoms.findId(index, ids, id, Atoms.foldId(id));
    }

    /** True when ID is present. */
    public boolean containsKey(long id) {
        return find(id) >= 0;
    }

    /** The value of ID, or null when ID is absent. */
    public V get(long id) {
        return getOrDefault(id, null);
    }

    /** The value of ID, or FALLBACK when ID is absent. */
    @SuppressWarnings("unchecked")
    public V getOrDefault(long id, V fallback) {
        int slot = find(id);
        return slot < 0 ? fallback : (V) values[slot];
    }

    /** Maps ID to VALUE; a present id keeps its position. Returns the previous value, or null. */
    @SuppressWarnings("unchecked")
    public V put(long id, V value) {
        int slot = find(id);
        if (slot >= 0) {
            V old = (V) values[slot];
            values[slot] = value;
            return old;
        }
        if (used == ids.length) {
            rebuild(2 * size > used ? 2 * ids.length : ids.length);
        }
        slot = used++;
        ids[slot] = id;
        values[slot] = value;
        liveMask[slot >>> WORD_SHIFT] |= 1L << slot;
        Atoms.insert(index, slot, Atoms.foldId(id));
        size++;
        modCount++;
        return null;
    }

    /** Removes ID. Returns its value, or null when ID was absent. */
    @SuppressWarnings("unchecked")
    public V remove(long id) {
        int slot = find(id);
        if (slot < 0) {
            return null;
        }
        index[Atoms.positionOf(index, Atoms.foldId(id), slot)] = -1;
        liveMask[slot >>> WORD_SHIFT] &= ~(1L << slot);
        V old = (V) values[slot];
        values[slot] = null;
        size--;
        modCount++;
        return old;
    }

    /** Removes every id; the lanes keep their capacity. */
    public void clear() {
        Arrays.fill(index, 0);
        Arrays.fill(values, 0, used, null);
        Arrays.fill(liveMask, 0L);
        used = 0;
        size = 0;
        modCount++;
    }

    /**
     * Moves the live slots, in order, to the front of lanes of SLOTS slots (stable compaction) and rebuilds the index.
     * Removed slots are reclaimed here; the index load stays at or below one half, tombstones included, because every
     * index entry names a slot below USED.
     */
    private void rebuild(int slots) {
        long[] oldIds = ids;
        Object[] oldValues = values;
        long[] oldLive = liveMask;
        int oldUsed = used;
        allocate(slots);
        used = Atoms.compactIds(oldIds, oldValues, oldLive, oldUsed, ids, values, liveMask);
        for (int slot = 0; slot < used; slot++) {
            Atoms.insert(index, slot, Atoms.foldId(ids[slot]));
        }
    }

    /** The id at POSITION of the insertion order. */
    public long idAt(int position) {
        Objects.checkIndex(position, size);
        return ids[Atoms.select(liveMask, position)];
    }

    /** The value at POSITION of the insertion order. */
    @SuppressWarnings("unchecked")
    public V valueAt(int position) {
        Objects.checkIndex(position, size);
        return (V) values[Atoms.select(liveMask, position)];
    }

    /** The position of ID in the insertion order, or -1 when ID is absent. */
    public int indexOf(long id) {
        int slot = find(id);
        return slot < 0 ? -1 : Atoms.rank(liveMask, slot);
    }

    /** The first id of the insertion order. */
    public long firstId() {
        if (size == 0) {
            throw new NoSuchElementException("the map is empty");
        }
        return ids[Atoms.nextLive(liveMask, 0, used)];
    }

    /** The last id of the insertion order. */
    public long lastId() {
        if (size == 0) {
            throw new NoSuchElementException("the map is empty");
        }
        return ids[Atoms.prevLive(liveMask, used - 1)];
    }

    /** The ids in insertion order (a new array). */
    public long[] ids() {
        long[] out = new long[size];
        int n = 0;
        for (int slot = Atoms.nextLive(liveMask, 0, used); slot >= 0; slot = Atoms.nextLive(liveMask, slot + 1, used)) {
            out[n++] = ids[slot];
        }
        return out;
    }

    /**
     * Performs ACTION on each entry in insertion order. A change of the map's structure during the traversal throws
     * {@link ConcurrentModificationException} after the action that made it.
     */
    @SuppressWarnings("unchecked")
    public void forEach(IdValueConsumer<? super V> action) {
        Objects.requireNonNull(action);
        int expected = modCount;
        for (int slot = Atoms.nextLive(liveMask, 0, used); slot >= 0; slot = Atoms.nextLive(liveMask, slot + 1, used)) {
            action.accept(ids[slot], (V) values[slot]);
            if (modCount != expected) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /** Map equality: the same ids, each mapped to an equal value; the order does not count. */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof M3LongObjectLinkedHashMap<?> map) || map.size != size) {
            return false;
        }
        for (int slot = Atoms.nextLive(liveMask, 0, used); slot >= 0; slot = Atoms.nextLive(liveMask, slot + 1, used)) {
            int at = map.find(ids[slot]);
            if (at < 0 || !Objects.equals(values[slot], map.values[at])) {
                return false;
            }
        }
        return true;
    }

    /** The hash code of {@code java.util.Map<Long, V>} with the same entries: the sum of id hash xor value hash. */
    @Override
    public int hashCode() {
        int h = 0;
        for (int slot = Atoms.nextLive(liveMask, 0, used); slot >= 0; slot = Atoms.nextLive(liveMask, slot + 1, used)) {
            h += Atoms.foldId(ids[slot]) ^ Objects.hashCode(values[slot]);
        }
        return h;
    }

    /** The text of {@code java.util.AbstractMap.toString} for the same entries in insertion order. */
    @Override
    public String toString() {
        int hint = TO_STRING_CHARS_PER_ENTRY * Math.min(size, Integer.MAX_VALUE / TO_STRING_CHARS_PER_ENTRY);
        StringBuilder buf = new StringBuilder(hint).append(MAP_OPEN);
        String separator = "";
        for (int slot = Atoms.nextLive(liveMask, 0, used); slot >= 0; slot = Atoms.nextLive(liveMask, slot + 1, used)) {
            Object value = values[slot];
            buf.append(separator).append(ids[slot]).append(KEY_VALUE_SEPARATOR).append(value == this ? THIS_MAP : value);
            separator = ENTRY_SEPARATOR;
        }
        return buf.append(MAP_CLOSE).toString();
    }

    /**
     * The pure kernels of the ARRAY_MAP and ID_MAP templates (recipe ARRAY_MAP_FILL): arrays in, values out, no
     * instance state; both templates include this class, so every member carries the same atoms.
     * Every atom states its contract in its Javadoc (Requires, Ensures, Effects, Cost), word for word its row of
     * ATOMS.tsv. M3ArrayMapAtomContractTest fails when an atom lacks the contract, the row, its own JUnit test
     * (M3ArrayMapAtomsTest) or its own performance row (M3ArrayMapAtomsPerformanceTest).
     */
    static final class Atoms {
        private Atoms() {
        }

        /**
         * Index table length for SLOTS entries.
         * Requires: SLOTS is between 0 and 2^29 inclusive.
         * Ensures: the smallest power of two that is at least max(2 * SLOTS, MIN_SLOTS), so the load stays at or below
         * one half.
         * Effects: none.
         * Cost: O(1).
         */
        static int tableSize(int slots) {
            return Integer.highestOneBit(Math.max(2 * slots, MIN_SLOTS) - 1) << 1;
        }

        /**
         * The hash code of a key as java.util.Map defines it.
         * Requires: nothing; KEY may be null.
         * Ensures: 0 for a null KEY, KEY.hashCode() otherwise.
         * Effects: calls KEY.hashCode() once when KEY is not null.
         * Cost: O(1) plus the hashCode call.
         */
        static int hash(Object key) {
            return key == null ? 0 : key.hashCode();
        }

        /**
         * Spreads a hash code over the index bits: Fibonacci hashing with the high half folded down.
         * Requires: nothing.
         * Ensures: a bijection on int, so distinct codes stay distinct, and sequential codes spread over the low bits.
         * Effects: none.
         * Cost: O(1).
         */
        static int mix(int h) {
            int m = h * GOLDEN_RATIO_32;
            return m ^ m >>> FOLD_SHIFT;
        }

        /**
         * Key equality as java.util.Map defines it, with an identity short cut.
         * Requires: nothing; either argument may be null.
         * Ensures: true when KEY and STORED are the same reference, false when exactly one is null, KEY.equals(STORED)
         * otherwise.
         * Effects: calls KEY.equals(STORED) at most once, never STORED.equals(KEY), and not at all for the same
         * reference.
         * Cost: O(1) plus the equals call.
         */
        static boolean sameKey(Object key, Object stored) {
            return key == stored || key != null && key.equals(stored);
        }

        /**
         * Linear probe for KEY.
         * Requires: INDEX.length is a power of two and INDEX holds an empty (0) entry; each positive entry e names slot
         * e - 1, filled in HASHES and KEYS; H is hash(KEY).
         * Ensures: the slot of a key with hash H that sameKey accepts, or -1; tombstones (-1) are passed and the first
         * empty entry ends the probe.
         * Effects: none; reads only.
         * Cost: expected O(1) at a load of at most one half.
         */
        static int find(int[] index, int[] hashes, Object[] keys, Object key, int h) {
            int mask = index.length - 1;
            for (int i = mix(h) & mask; ; i = i + 1 & mask) {
                int e = index[i];
                if (e == 0) {
                    return -1;
                }
                if (e > 0 && hashes[e - 1] == h && sameKey(key, keys[e - 1])) {
                    return e - 1;
                }
            }
        }

        /**
         * The index position of an indexed slot.
         * Requires: SLOT + 1 is stored on the probe of H, that is, the slot is indexed under H.
         * Ensures: the position i on the probe of H with INDEX[i] == SLOT + 1.
         * Effects: none; reads only, and does not return when the requirement is broken.
         * Cost: expected O(1) at a load of at most one half.
         */
        static int positionOf(int[] index, int h, int slot) {
            int mask = index.length - 1;
            for (int i = mix(h) & mask; ; i = i + 1 & mask) {
                if (index[i] == slot + 1) {
                    return i;
                }
            }
        }

        /**
         * Indexes SLOT under H.
         * Requires: the probe of H holds an empty (0) or tombstone (-1) entry, and SLOT is not indexed yet.
         * Ensures: SLOT + 1 is stored at the first empty or tombstone entry of the probe of H; returns true when that
         * entry was a tombstone.
         * Effects: writes exactly one INDEX entry.
         * Cost: expected O(1) at a load of at most one half.
         */
        static boolean insert(int[] index, int slot, int h) {
            int mask = index.length - 1;
            for (int i = mix(h) & mask; ; i = i + 1 & mask) {
                int e = index[i];
                if (e <= 0) {
                    index[i] = slot + 1;
                    return e < 0;
                }
            }
        }

        /**
         * Forward scan of the live mask.
         * Requires: FROM is not negative; MASK holds at least (USED + 63) / 64 words and no set bit at or above USED.
         * Ensures: the first set bit at or after FROM and below USED, or -1.
         * Effects: none; reads only.
         * Cost: O(1) plus one step per word skipped.
         */
        static int nextLive(long[] mask, int from, int used) {
            if (from >= used) {
                return -1;
            }
            int w = from >>> WORD_SHIFT;
            long word = mask[w] & -1L << from;
            for (int words = used + WORD_MASK >>> WORD_SHIFT; ; ) {
                if (word != 0) {
                    return (w << WORD_SHIFT) + Long.numberOfTrailingZeros(word);
                }
                if (++w >= words) {
                    return -1;
                }
                word = mask[w];
            }
        }

        /**
         * Backward scan of the live mask.
         * Requires: FROM is below 64 * MASK.length.
         * Ensures: the last set bit at or before FROM, or -1, which is also the answer for a negative FROM.
         * Effects: none; reads only.
         * Cost: O(1) plus one step per word skipped.
         */
        static int prevLive(long[] mask, int from) {
            if (from < 0) {
                return -1;
            }
            int w = from >>> WORD_SHIFT;
            long word = mask[w] & -1L >>> WORD_MASK - (from & WORD_MASK);
            while (true) {
                if (word != 0) {
                    return (w << WORD_SHIFT) + WORD_MASK - Long.numberOfLeadingZeros(word);
                }
                if (--w < 0) {
                    return -1;
                }
                word = mask[w];
            }
        }

        /**
         * The live slot of a given rank.
         * Requires: RANK is not negative and is below the number of set bits of MASK.
         * Ensures: the set bit with exactly RANK set bits below it.
         * Effects: none; reads only.
         * Cost: one popcount per word before the answer, plus at most 63 bit clears in its word.
         */
        static int select(long[] mask, int rank) {
            for (int w = 0; ; w++) {
                long word = mask[w];
                int count = Long.bitCount(word);
                if (rank < count) {
                    for (; rank > 0; rank--) {
                        word &= word - 1;
                    }
                    return (w << WORD_SHIFT) + Long.numberOfTrailingZeros(word);
                }
                rank -= count;
            }
        }

        /**
         * The rank of a slot.
         * Requires: SLOT is not negative and is below 64 * MASK.length.
         * Ensures: the number of set bits of MASK below SLOT.
         * Effects: none; reads only.
         * Cost: one popcount per word up to SLOT.
         */
        static int rank(long[] mask, int slot) {
            int rank = 0;
            int w = slot >>> WORD_SHIFT;
            for (int i = 0; i < w; i++) {
                rank += Long.bitCount(mask[i]);
            }
            return rank + Long.bitCount(mask[w] & (1L << slot) - 1);
        }

        /**
         * Stable two-pointer compaction of the slot lanes.
         * Requires: TO_LIVE is all zero and, like the destination lanes, has room for every live slot; LIVE has no set
         * bit at or above USED.
         * Ensures: the live slots, in ascending order, are copied to destination slots 0 to n - 1 with their bits set
         * in TO_LIVE; returns n.
         * Effects: writes the destination lanes only; the source lanes and LIVE are unchanged.
         * Cost: one step per word of LIVE below USED plus one per live slot.
         */
        static int compact(Object[] keys, Object[] values, int[] hashes, long[] live, int used, Object[] toKeys,
                Object[] toValues, int[] toHashes, long[] toLive) {
            int write = 0;
            for (int w = 0, words = used + WORD_MASK >>> WORD_SHIFT; w < words; w++) {
                for (long word = live[w]; word != 0; word &= word - 1) {
                    int read = (w << WORD_SHIFT) + Long.numberOfTrailingZeros(word);
                    toKeys[write] = keys[read];
                    toValues[write] = values[read];
                    toHashes[write] = hashes[read];
                    toLive[write >>> WORD_SHIFT] |= 1L << write;
                    write++;
                }
            }
            return write;
        }

        /**
         * The hash code of an M3 id (an M3String canonical id, an M3AST node id, any M3 key id).
         * Requires: nothing.
         * Ensures: Long.hashCode(ID), so equal ids fold equally.
         * Effects: none.
         * Cost: O(1).
         */
        static int foldId(long id) {
            return Long.hashCode(id);
        }

        /**
         * Linear probe for an M3 id: one id lane read per occupied entry, no hash lane and no equals call.
         * Requires: INDEX.length is a power of two and INDEX holds an empty (0) entry; each positive entry e names slot
         * e - 1, filled in IDS; H is foldId(ID).
         * Ensures: the slot holding ID, or -1; tombstones (-1) are passed and the first empty entry ends the probe.
         * Effects: none; reads only.
         * Cost: expected O(1) at a load of at most one half.
         */
        static int findId(int[] index, long[] ids, long id, int h) {
            int mask = index.length - 1;
            for (int i = mix(h) & mask; ; i = i + 1 & mask) {
                int e = index[i];
                if (e == 0) {
                    return -1;
                }
                if (e > 0 && ids[e - 1] == id) {
                    return e - 1;
                }
            }
        }

        /**
         * Stable two-pointer compaction of the id lanes.
         * Requires: TO_LIVE is all zero and, like TO_IDS and TO_VALUES, has room for every live slot; LIVE has no set
         * bit at or above USED.
         * Ensures: the live slots, in ascending order, are copied to destination slots 0 to n - 1 with their bits set
         * in TO_LIVE; returns n.
         * Effects: writes the destination lanes only; the source lanes and LIVE are unchanged.
         * Cost: one step per word of LIVE below USED plus one per live slot.
         */
        static int compactIds(long[] ids, Object[] values, long[] live, int used, long[] toIds, Object[] toValues,
                long[] toLive) {
            int write = 0;
            for (int w = 0, words = used + WORD_MASK >>> WORD_SHIFT; w < words; w++) {
                for (long word = live[w]; word != 0; word &= word - 1) {
                    int read = (w << WORD_SHIFT) + Long.numberOfTrailingZeros(word);
                    toIds[write] = ids[read];
                    toValues[write] = values[read];
                    toLive[write >>> WORD_SHIFT] |= 1L << write;
                    write++;
                }
            }
            return write;
        }
    }
}
