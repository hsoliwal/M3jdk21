/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Primitive long-to-long PATRICIA (crit-bit) trie on dense lanes, ordered by the keys' bit
 * strings, i.e. {@link Long#compareUnsigned}, with prefix queries and unsigned navigation and no
 * stored node objects (catalogue row {@code patricia-trie}, leaf GAP-LC-catalogue-gaps).
 *
 * <p>Mechanics follow the PATRICIA idea that Apache Commons Collections {@code PatriciaTrie}
 * (contract reference only, nothing copied, no runtime dependency) implements with linked
 * {@code TrieEntry} nodes: an internal node stores only the index of the most significant bit at
 * which its two subtrees differ, so lookup tests at most 64 bits and insertion adds exactly one
 * internal node. Here the trie is two dense arenas addressed by offset arithmetic: leaf lanes
 * {@code keys}/{@code values}/{@code leafParent} at slots {@code [0, size)} and internal-node lanes
 * {@code critBit} (a {@code byte}, bit 63..0), {@code child0}/{@code child1}/{@code nodeParent} at
 * slots {@code [0, size - 1)}. A child reference {@code r >= 0} names internal node {@code r};
 * {@code r < 0} names leaf {@code ~r}. Slot state is the dense-prefix invariant rather than a
 * stored mask: a slot is live exactly when its index is below the arena's count, because removal
 * swap-moves the last leaf and the last internal node into the vacated slots and re-points their
 * single parent reference. Get/put/remove/ceiling/floor are O(64); in-order successor walks use the
 * parent lanes, so iteration and prefix scans need no stack. Per-element allocation: 0 (lanes grow
 * by doubling). Every {@code long} is a legal key and value. Proving-ground backend; not a
 * {@code java.util.SortedMap} or Commons {@code Trie}.</p>
 */
public final class M3LongPatriciaTrie {
    private static final int MIN_CAPACITY = 8;
    private static final int MAX_CAPACITY = 1 << 30;
    private static final int NONE = -1;
    private static final int NONE_REF = Integer.MIN_VALUE;

    private long[] keys;
    private long[] values;
    private int[] leafParent;
    private byte[] critBit;
    private int[] child0;
    private int[] child1;
    private int[] nodeParent;
    private int root;
    private int size;
    private int modCount;

    public M3LongPatriciaTrie() {
        this(0);
    }

    public M3LongPatriciaTrie(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        allocate(Math.max(MIN_CAPACITY, Math.min(MAX_CAPACITY, expectedSize)));
    }

    /** Inserts or replaces; returns {@code true} when a new key was added. */
    public boolean put(long key, long value) {
        if (size == 0) {
            keys[0] = key;
            values[0] = value;
            leafParent[0] = NONE;
            root = ~0;
            size = 1;
            modCount++;
            return true;
        }
        int leaf = descend(key);
        long difference = keys[leaf] ^ key;
        if (difference == 0) {
            values[leaf] = value;
            return false;
        }
        if (size == keys.length) {
            if (size == MAX_CAPACITY) {
                throw new IllegalStateException("maximum trie capacity reached");
            }
            grow();
        }
        int bit = 63 - Long.numberOfLeadingZeros(difference);
        int parent = NONE;
        int ref = root;
        while (ref >= 0 && critBit[ref] > bit) {
            parent = ref;
            ref = isSet(key, critBit[ref]) ? child1[ref] : child0[ref];
        }
        int newLeaf = size;
        int newNode = size - 1;
        keys[newLeaf] = key;
        values[newLeaf] = value;
        leafParent[newLeaf] = newNode;
        critBit[newNode] = (byte) bit;
        if (isSet(key, bit)) {
            child0[newNode] = ref;
            child1[newNode] = ~newLeaf;
        } else {
            child0[newNode] = ~newLeaf;
            child1[newNode] = ref;
        }
        setParent(ref, newNode);
        nodeParent[newNode] = parent;
        replaceChild(parent, ref, newNode);
        size++;
        modCount++;
        return true;
    }

    public long getOrDefault(long key, long defaultValue) {
        int leaf = find(key);
        return leaf == NONE ? defaultValue : values[leaf];
    }

    public boolean containsKey(long key) {
        return find(key) != NONE;
    }

    public boolean remove(long key) {
        int leaf = find(key);
        if (leaf == NONE) {
            return false;
        }
        if (size == 1) {
            keys[0] = 0L;
            values[0] = 0L;
            size = 0;
            modCount++;
            return true;
        }
        int parent = leafParent[leaf];
        int sibling = child0[parent] == ~leaf ? child1[parent] : child0[parent];
        int grandparent = nodeParent[parent];
        replaceChild(grandparent, parent, sibling);
        setParent(sibling, grandparent);
        moveLeaf(size - 1, leaf);
        moveNode(size - 2, parent);
        size--;
        keys[size] = 0L;
        values[size] = 0L;
        modCount++;
        return true;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Lane capacity of both arenas. */
    public int capacity() {
        return keys.length;
    }

    /** Retained primitive payload: three leaf lanes and four internal-node lanes. */
    public long payloadBytes() {
        return (long) keys.length * (2L * Long.BYTES + 4L * Integer.BYTES + Byte.BYTES);
    }

    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(keys, 0, size, 0L);
        Arrays.fill(values, 0, size, 0L);
        size = 0;
        modCount++;
    }

    /** Smallest key in unsigned order. */
    public long firstKey() {
        requireNonEmpty();
        return keys[leftmost(root)];
    }

    /** Largest key in unsigned order. */
    public long lastKey() {
        requireNonEmpty();
        return keys[rightmost(root)];
    }

    /** Least key {@code >= key} (unsigned), or {@code defaultValue} when none. */
    public long ceilingKeyOrDefault(long key, long defaultValue) {
        int leaf = ceilingLeaf(key);
        return leaf == NONE ? defaultValue : keys[leaf];
    }

    /** Greatest key {@code <= key} (unsigned), or {@code defaultValue} when none. */
    public long floorKeyOrDefault(long key, long defaultValue) {
        int leaf = floorLeaf(key);
        return leaf == NONE ? defaultValue : keys[leaf];
    }

    /** Least key {@code > key} (unsigned), or {@code defaultValue} when none. */
    public long higherKeyOrDefault(long key, long defaultValue) {
        int leaf = key == -1L ? NONE : ceilingLeaf(key + 1);
        return leaf == NONE ? defaultValue : keys[leaf];
    }

    /** Greatest key {@code < key} (unsigned), or {@code defaultValue} when none. */
    public long lowerKeyOrDefault(long key, long defaultValue) {
        int leaf = key == 0L ? NONE : floorLeaf(key - 1);
        return leaf == NONE ? defaultValue : keys[leaf];
    }

    /** Number of keys whose top {@code bits} bits equal those of {@code prefix} ({@code 0..64}). */
    public int prefixCount(long prefix, int bits) {
        int subtree = prefixSubtree(prefix, bits);
        if (subtree == NONE_REF) {
            return 0;
        }
        int count = 0;
        int end = rightmost(subtree);
        for (int leaf = leftmost(subtree); ; leaf = successor(leaf)) {
            count++;
            if (leaf == end) {
                return count;
            }
        }
    }

    /** Keys whose top {@code bits} bits equal those of {@code prefix}, in unsigned order. */
    public long[] prefixKeys(long prefix, int bits) {
        long[] result = new long[prefixCount(prefix, bits)];
        if (result.length > 0) {
            int leaf = leftmost(prefixSubtree(prefix, bits));
            for (int i = 0; i < result.length; i++, leaf = successor(leaf)) {
                result[i] = keys[leaf];
            }
        }
        return result;
    }

    /** All keys in unsigned order. */
    public long[] keysToArray() {
        long[] result = new long[size];
        if (size > 0) {
            int leaf = leftmost(root);
            for (int i = 0; i < size; i++, leaf = successor(leaf)) {
                result[i] = keys[leaf];
            }
        }
        return result;
    }

    /** Unsigned-order key iterator; fail-fast against structural modification. */
    public M3LongIterator keyIterator() {
        int expectedModCount = modCount;
        return new M3LongIterator() {
            private int following = size == 0 ? NONE : leftmost(root);

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return following != NONE;
            }

            @Override
            public long nextLong() {
                checkForComodification(expectedModCount);
                if (following == NONE) {
                    throw new NoSuchElementException();
                }
                long key = keys[following];
                following = successor(following);
                return key;
            }
        };
    }

    /** Reference of the subtree holding exactly the keys with the given prefix, or {@link #NONE_REF}. */
    private int prefixSubtree(long prefix, int bits) {
        if (bits < 0 || bits > 64) {
            throw new IllegalArgumentException("bits must be in 0..64");
        }
        if (size == 0) {
            return NONE_REF;
        }
        int lowestPrefixBit = 64 - bits;
        int ref = root;
        while (ref >= 0 && critBit[ref] >= lowestPrefixBit) {
            ref = isSet(prefix, critBit[ref]) ? child1[ref] : child0[ref];
        }
        if (bits == 0) {
            return ref;
        }
        long sample = keys[leftmost(ref)];
        return ((sample ^ prefix) >>> lowestPrefixBit) == 0 ? ref : NONE_REF;
    }

    private int ceilingLeaf(long key) {
        if (size == 0) {
            return NONE;
        }
        int leaf = descend(key);
        long difference = keys[leaf] ^ key;
        if (difference == 0) {
            return leaf;
        }
        int bit = 63 - Long.numberOfLeadingZeros(difference);
        int ref = divergence(key, bit);
        return isSet(key, bit) ? successor(rightmost(ref)) : leftmost(ref);
    }

    private int floorLeaf(long key) {
        if (size == 0) {
            return NONE;
        }
        int leaf = descend(key);
        long difference = keys[leaf] ^ key;
        if (difference == 0) {
            return leaf;
        }
        int bit = 63 - Long.numberOfLeadingZeros(difference);
        int ref = divergence(key, bit);
        return isSet(key, bit) ? rightmost(ref) : predecessor(leftmost(ref));
    }

    /** Subtree whose keys all share {@code key}'s bits above {@code bit} and differ from it at {@code bit}. */
    private int divergence(long key, int bit) {
        int ref = root;
        while (ref >= 0 && critBit[ref] > bit) {
            ref = isSet(key, critBit[ref]) ? child1[ref] : child0[ref];
        }
        return ref;
    }

    /** Leaf reached by following {@code key}'s bits at every crit bit; requires a non-empty trie. */
    private int descend(long key) {
        int ref = root;
        while (ref >= 0) {
            ref = isSet(key, critBit[ref]) ? child1[ref] : child0[ref];
        }
        return ~ref;
    }

    private int find(long key) {
        if (size == 0) {
            return NONE;
        }
        int leaf = descend(key);
        return keys[leaf] == key ? leaf : NONE;
    }

    private int leftmost(int ref) {
        int current = ref;
        while (current >= 0) {
            current = child0[current];
        }
        return ~current;
    }

    private int rightmost(int ref) {
        int current = ref;
        while (current >= 0) {
            current = child1[current];
        }
        return ~current;
    }

    /** Next leaf in unsigned order, or {@link #NONE}. */
    private int successor(int leaf) {
        int ref = ~leaf;
        int parent = leafParent[leaf];
        while (parent != NONE && child1[parent] == ref) {
            ref = parent;
            parent = nodeParent[parent];
        }
        return parent == NONE ? NONE : leftmost(child1[parent]);
    }

    /** Previous leaf in unsigned order, or {@link #NONE}. */
    private int predecessor(int leaf) {
        int ref = ~leaf;
        int parent = leafParent[leaf];
        while (parent != NONE && child0[parent] == ref) {
            ref = parent;
            parent = nodeParent[parent];
        }
        return parent == NONE ? NONE : rightmost(child0[parent]);
    }

    /** Moves leaf slot {@code from} into the vacated slot {@code to} and re-points its parent. */
    private void moveLeaf(int from, int to) {
        if (from == to) {
            return;
        }
        keys[to] = keys[from];
        values[to] = values[from];
        int parent = leafParent[from];
        leafParent[to] = parent;
        replaceChild(parent, ~from, ~to);
    }

    /** Moves internal slot {@code from} into the vacated slot {@code to}; re-points parent and children. */
    private void moveNode(int from, int to) {
        if (from == to) {
            return;
        }
        critBit[to] = critBit[from];
        child0[to] = child0[from];
        child1[to] = child1[from];
        int parent = nodeParent[from];
        nodeParent[to] = parent;
        replaceChild(parent, from, to);
        setParent(child0[to], to);
        setParent(child1[to], to);
    }

    private void setParent(int ref, int parent) {
        if (ref >= 0) {
            nodeParent[ref] = parent;
        } else {
            leafParent[~ref] = parent;
        }
    }

    private void replaceChild(int parent, int oldRef, int newRef) {
        if (parent == NONE) {
            root = newRef;
        } else if (child0[parent] == oldRef) {
            child0[parent] = newRef;
        } else {
            child1[parent] = newRef;
        }
    }

    private void grow() {
        int capacity = keys.length >= (MAX_CAPACITY >>> 1) ? MAX_CAPACITY : keys.length << 1;
        keys = Arrays.copyOf(keys, capacity);
        values = Arrays.copyOf(values, capacity);
        leafParent = Arrays.copyOf(leafParent, capacity);
        critBit = Arrays.copyOf(critBit, capacity);
        child0 = Arrays.copyOf(child0, capacity);
        child1 = Arrays.copyOf(child1, capacity);
        nodeParent = Arrays.copyOf(nodeParent, capacity);
    }

    private void allocate(int capacity) {
        keys = new long[capacity];
        values = new long[capacity];
        leafParent = new int[capacity];
        critBit = new byte[capacity];
        child0 = new int[capacity];
        child1 = new int[capacity];
        nodeParent = new int[capacity];
    }

    private void requireNonEmpty() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    private static boolean isSet(long key, int bit) {
        return ((key >>> bit) & 1L) != 0;
    }
}
