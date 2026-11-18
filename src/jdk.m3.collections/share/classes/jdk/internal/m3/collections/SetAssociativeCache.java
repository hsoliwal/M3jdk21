/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Parent-owned set-associative cache over primitive int keys.
 *
 * <p>Within each set, the youngest entry is stored at offset zero. A hit does
 * not change age; inserting or replacing a key moves it to the youngest offset.
 * On a full set the oldest entry is evicted. The cache stores no per-entry
 * nodes and does not retain {@code Map.Entry} projections.
 *
 * <p>Physical array runs are allocated only when a set receives its first
 * entry. Keys, values and occupancy bits are held in the parent, and an integer
 * directory maps logical sets to their physical offsets. This class is not
 * thread safe, as is typical of a small CPU-cache-oriented set-associative
 * collection. Null values are not supported.
 *
 * <p>The cache is an M3 collection with a small original API, not an alias of
 * the larger Agrona {@code Map} API. The public scaffold descriptors and all
 * pre-existing methods are preserved.
 */
public final class SetAssociativeCache<V> {
    /** The single PRESENT bit of a packed physical-slot state mask. */
    static final long STATE_OCCUPIED = 1L;

    private static final int DEFAULT_SETS = 16;
    private static final int DEFAULT_WAYS = 4;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private final int numSets;
    private final int setSize;
    private final int setMask;
    private final int logicalCapacity;
    private final int[] setBaseLane;
    private final int[] setCountLane;
    private final Consumer<? super V> evictionConsumer;

    private int[] keyLane = new int[0];
    private Object[] valueLane = new Object[0];
    private long[] stateMaskWords = new long[0];
    private int allocatedRuns;
    private int size;
    private long cacheHits;
    private long cacheMisses;
    private long cachePuts;

    /** Backward-compatible default with 16 sets and four entries per set. */
    public SetAssociativeCache() {
        this(DEFAULT_SETS, DEFAULT_WAYS, null);
    }

    /** Creates a cache with a positive, power-of-two number of sets and ways. */
    public SetAssociativeCache(int numSets, int setSize) {
        this(numSets, setSize, null);
    }

    /** Adds an optional callback for automatic evictions (not explicit removal). */
    public SetAssociativeCache(int numSets, int setSize,
                               Consumer<? super V> evictionConsumer) {
        if (numSets <= 0 || Integer.bitCount(numSets) != 1
                || setSize <= 0 || Integer.bitCount(setSize) != 1
                || (long) numSets * setSize > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("sets and ways must be positive powers of two with finite capacity");
        }
        this.numSets = numSets;
        this.setSize = setSize;
        this.setMask = numSets - 1;
        this.logicalCapacity = numSets * setSize;
        this.evictionConsumer = evictionConsumer;
        this.setBaseLane = new int[numSets];
        this.setCountLane = new int[numSets];
        Arrays.fill(setBaseLane, -1);
    }

    /** Historical descriptor retained for the original API inventory. */
    public static String scaffoldDescriptor() {
        return "M3SetAssociativeCache|SET_ASSOCIATIVE_CACHE|OBJECT|OCCUPIED|int[] keyLane;Object[] valueLane";
    }

    /** Returns the cached value and increments the hit/miss counter exactly once. */
    public V get(int key) {
        int set = setOf(key);
        int at = locate(set, key);
        if (at < 0) {
            cacheMisses++;
            return null;
        }
        cacheHits++;
        return valueAt(at);
    }

    /**
     * Inserts the newest entry in its set; returns the previous value only
     * when replacing the same key. The callback runs after any eviction and
     * insertion leave the cache consistent, including when it throws.
     */
    public V put(int key, V value) {
        Objects.requireNonNull(value, "value");
        int set = setOf(key);
        int base = ensureSet(set);
        int count = setCountLane[set];
        int oldPosition = locate(set, key);
        V old = oldPosition < 0 ? null : valueAt(oldPosition);
        V evicted = null;

        if (oldPosition >= 0) {
            int relative = oldPosition - base;
            if (relative > 0) {
                System.arraycopy(keyLane, base, keyLane, base + 1, relative);
                System.arraycopy(valueLane, base, valueLane, base + 1, relative);
            }
        } else if (count == setSize) {
            evicted = valueAt(base + count - 1);
            if (count > 1) {
                System.arraycopy(keyLane, base, keyLane, base + 1, count - 1);
                System.arraycopy(valueLane, base, valueLane, base + 1, count - 1);
            }
        } else {
            if (count > 0) {
                System.arraycopy(keyLane, base, keyLane, base + 1, count);
                System.arraycopy(valueLane, base, valueLane, base + 1, count);
            }
            setCountLane[set] = count + 1;
            setOccupied(base + count, true);
            size++;
        }

        keyLane[base] = key;
        valueLane[base] = value;
        cachePuts++;
        if (evicted != null && evictionConsumer != null) {
            evictionConsumer.accept(evicted);
        }
        return old;
    }

    /** Removes a key without an eviction callback or hit/miss update. */
    public V remove(int key) {
        int set = setOf(key);
        int at = locate(set, key);
        if (at < 0) return null;
        V removed = valueAt(at);
        int base = setBaseLane[set];
        int count = setCountLane[set];
        int end = base + count - 1;
        if (at < end) {
            System.arraycopy(keyLane, at + 1, keyLane, at, end - at);
            System.arraycopy(valueLane, at + 1, valueLane, at, end - at);
        }
        keyLane[end] = 0;
        valueLane[end] = null;
        setOccupied(end, false);
        setCountLane[set] = count - 1;
        size--;
        return removed;
    }

    /** A non-statistical membership query; does not change FIFO order. */
    public boolean containsKey(int key) {
        return locate(setOf(key), key) >= 0;
    }

    /** Logical capacity; physical storage is allocated on first use per set. */
    public int capacity() { return logicalCapacity; }
    public long cacheHits() { return cacheHits; }
    public long cacheMisses() { return cacheMisses; }
    /** The number of successful put calls, including replacements. */
    public long cachePuts() { return cachePuts; }
    public int size() { return size; }

    /** Reset statistics independently from cache state. */
    public void resetCounters() {
        cacheHits = 0;
        cacheMisses = 0;
        cachePuts = 0;
    }

    /** Clear entries and physically release the optional backing lanes. */
    public void clear() {
        Arrays.fill(setBaseLane, -1);
        Arrays.fill(setCountLane, 0);
        keyLane = new int[0];
        valueLane = new Object[0];
        stateMaskWords = new long[0];
        allocatedRuns = 0;
        size = 0;
    }

    private static int spread(int key) {
        return key ^ (key >>> 16);
    }

    private int setOf(int key) { return spread(key) & setMask; }

    private int locate(int set, int key) {
        int base = setBaseLane[set];
        if (base < 0) return -1;
        int end = base + setCountLane[set];
        for (int index = base; index < end; index++) {
            if (occupied(index) && keyLane[index] == key) return index;
        }
        return -1;
    }

    private int ensureSet(int set) {
        int base = setBaseLane[set];
        if (base >= 0) return base;
        base = allocatedRuns * setSize;
        int required = base + setSize;
        if (required > keyLane.length) {
            long grown = (long) keyLane.length + (keyLane.length >>> 1) + setSize;
            int length = (int) Math.min(logicalCapacity, Math.max(required, grown));
            // Allocate all aligned lanes before publishing any replacement. A
            // later allocation failure must leave the current cache reusable.
            int[] nextKeys = Arrays.copyOf(keyLane, length);
            Object[] nextValues = Arrays.copyOf(valueLane, length);
            long[] nextState = Arrays.copyOf(stateMaskWords,
                    length == 0 ? 0 : ((length - 1) >>> 6) + 1);
            keyLane = nextKeys;
            valueLane = nextValues;
            stateMaskWords = nextState;
        }
        setBaseLane[set] = base;
        allocatedRuns++;
        return base;
    }

    private boolean occupied(int slot) {
        return (stateMaskWords[slot >>> 6] & (1L << (slot & 63))) != 0;
    }

    private void setOccupied(int slot, boolean live) {
        long bit = 1L << (slot & 63);
        int word = slot >>> 6;
        if (live) stateMaskWords[word] |= bit;
        else stateMaskWords[word] &= ~bit;
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int slot) { return (V) valueLane[slot]; }
}
