/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import jdk.m3.collections.M3RuntimeHeap;

/**
 * Primitive-only mutable list with a separate, maintained ordering index.
 *
 * <p>Issued IDs are permanent, zero-based logical append positions. The payload and sort-key
 * lanes are never permuted. Removing an ID makes it a tombstone: IDs are not recycled, so
 * stale IDs cannot silently refer to another element. All retained per-element state lives in
 * primitive arrays; there is no Node, Entry, wrapper, callback, or object allocation per item.
 *
 * <p>For small collections, insertion points are found with binary search and a bounded
 * primitive int[] shift. After {@value #FLAT_LIMIT} live elements, a primitive array-backed AVL
 * rank/select index is built once. Its child links are array indexes, not Java references.
 * Inserts, deletes, key updates, rank and select then take O(log n) comparisons and rotations.
 * Ordinary payload reads remain O(1). Duplicate keys are ordered by stable issued ID.
 *
 * <p>This owner is not thread safe. It does not claim transparent Java array or object swapping:
 * immutable/canonical payload paging belongs to the existing M3 native swap owners.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/M3SortedIndexedLongList.java}
 * blob {@code a76657b9893904fae31874d4dfaa8946cd9d8e80} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3SortedIndexedLongList {
    public static final int FLAT_LIMIT = 256;
    private static final int INITIAL_CAPACITY = 16;
    private static final int PEAK_BYTES_PER_SLOT = 2 * Long.BYTES + 4 * Integer.BYTES + 2;

    private long[] values;
    private long[] sortKeys;
    private byte[] live;
    // Flat indexes contain *only* stable logical IDs; switched off at tree promotion.
    private int[] flat;
    // Tree node n represents immutable stable logical ID n-1. Slot 0 is null.
    private int[] left;
    private int[] right;
    private int[] subtree;
    private byte[] heights;
    private int root;
    private int liveSize;
    private int issued;
    private long version;

    public M3SortedIndexedLongList() {
        this(INITIAL_CAPACITY);
    }

    public M3SortedIndexedLongList(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        values = new long[capacity];
        sortKeys = new long[capacity];
        live = new byte[capacity];
        flat = new int[capacity];
    }

    public int size() { return liveSize; }
    public boolean isEmpty() { return liveSize == 0; }
    /** One-past-largest issued ID; it never decreases, even after clear(). */
    public int issuedIds() { return issued; }
    public int capacity() { return values.length; }
    public boolean treeIndexed() { return left != null; }
    public long version() { return version; }

    /** Payload lanes and index lanes, excluding JVM headers, unused old arrays and swap storage. */
    public long reservedPrimitiveBytes() {
        long bytes = (long) values.length * (2 * Long.BYTES + 1);
        if (flat != null) bytes += (long) flat.length * Integer.BYTES;
        if (left != null) bytes += (long) left.length * (3 * Integer.BYTES + 1);
        return bytes;
    }

    /** Append with the signed-long payload itself as the sort key; returns a stable ID. */
    public int add(long value) { return add(value, value); }

    /** Append an immutable primitive key and a mutable primitive payload without shifting data. */
    public int add(long value, long sortKey) {
        if (issued == Integer.MAX_VALUE - 1) {
            throw new IllegalStateException("stable logical ID space exhausted");
        }
        int id = issued;
        ensureCapacity(id + 1);
        if (left == null && liveSize == FLAT_LIMIT) {
            // Build all replacement index lanes before publishing this new logical value.
            int insertion = flatInsertion(sortKey, id);
            int[] sorted = new int[liveSize + 1];
            System.arraycopy(flat, 0, sorted, 0, insertion);
            sorted[insertion] = id;
            System.arraycopy(flat, insertion, sorted, insertion + 1, liveSize - insertion);
            int length = values.length + 1;
            int[] nextLeft = new int[length];
            int[] nextRight = new int[length];
            int[] nextSubtree = new int[length];
            byte[] nextHeights = new byte[length];
            int nextRoot = buildBalanced(sorted, 0, sorted.length,
                    nextLeft, nextRight, nextSubtree, nextHeights);
            values[id] = value;
            sortKeys[id] = sortKey;
            live[id] = 1;
            left = nextLeft;
            right = nextRight;
            subtree = nextSubtree;
            heights = nextHeights;
            root = nextRoot;
            flat = null;
        } else if (left == null) {
            int insertion = flatInsertion(sortKey, id);
            System.arraycopy(flat, insertion, flat, insertion + 1, liveSize - insertion);
            flat[insertion] = id;
            values[id] = value;
            sortKeys[id] = sortKey;
            live[id] = 1;
        } else {
            values[id] = value;
            sortKeys[id] = sortKey;
            live[id] = 1;
            initializeLeaf(id + 1);
            root = insert(root, id + 1);
        }
        issued++;
        liveSize++;
        version++;
        return id;
    }

    public boolean containsId(int id) {
        return id >= 0 && id < issued && live[id] != 0;
    }

    /** Returns the original payload by stable logical ID, regardless of sorted position. */
    public long valueAtId(int id) { return values[requireId(id)]; }
    public long keyAtId(int id) { return sortKeys[requireId(id)]; }

    /** Payload-only mutation, leaving its independent sort key and index unchanged. */
    public void setValue(int id, long value) {
        values[requireId(id)] = value;
        version++;
    }

    /** Reindexes this stable ID without reallocating payload or changing any other ID. */
    public void setSortKey(int id, long key) {
        int checked = requireId(id);
        long previous = sortKeys[checked];
        if (previous == key) return;
        if (left == null) {
            int oldAt = flatSearch(previous, checked);
            if (oldAt < 0) throw new IllegalStateException("broken flat index");
            System.arraycopy(flat, oldAt + 1, flat, oldAt, liveSize - oldAt - 1);
            // Search only liveSize-1 remaining IDs, not the removed trailing duplicate.
            int newAt = flatInsertion(key, checked, liveSize - 1);
            System.arraycopy(flat, newAt, flat, newAt + 1, liveSize - 1 - newAt);
            flat[newAt] = checked;
        } else {
            root = delete(root, checked + 1);
            initializeLeaf(checked + 1);
            sortKeys[checked] = key;
            root = insert(root, checked + 1);
        }
        sortKeys[checked] = key;
        version++;
    }

    /** Delete the logical value, retaining the ID tombstone rather than aliasing a new value. */
    public boolean removeId(int id) {
        if (!containsId(id)) return false;
        if (left == null) {
            int ordinal = flatSearch(sortKeys[id], id);
            if (ordinal < 0) throw new IllegalStateException("broken flat index");
            System.arraycopy(flat, ordinal + 1, flat, ordinal, liveSize - ordinal - 1);
            flat[liveSize - 1] = 0;
        } else {
            root = delete(root, id + 1);
            left[id + 1] = 0;
            right[id + 1] = 0;
            subtree[id + 1] = 0;
            heights[id + 1] = 0;
        }
        live[id] = 0;
        values[id] = 0L;
        sortKeys[id] = 0L;
        liveSize--;
        version++;
        return true;
    }

    /** Remove all logical values without ever reissuing a prior stable ID. */
    public void clear() {
        if (liveSize == 0) return;
        Arrays.fill(values, 0, issued, 0L);
        Arrays.fill(sortKeys, 0, issued, 0L);
        Arrays.fill(live, 0, issued, (byte) 0);
        if (flat != null) Arrays.fill(flat, 0, liveSize, 0);
        if (left != null) {
            Arrays.fill(left, 0);
            Arrays.fill(right, 0);
            Arrays.fill(subtree, 0);
            Arrays.fill(heights, (byte) 0);
        }
        root = 0;
        liveSize = 0;
        version++;
    }

    /** Signed-long lower bound: count of keys strictly smaller than key. */
    public int lowerBound(long key) {
        if (left == null) {
            int lo = 0, hi = liveSize;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (Long.compare(sortKeys[flat[mid]], key) < 0) lo = mid + 1;
                else hi = mid;
            }
            return lo;
        }
        int ordinal = 0, node = root;
        while (node != 0) {
            if (Long.compare(sortKeys[node - 1], key) < 0) {
                ordinal += 1 + count(left[node]);
                node = right[node];
            } else node = left[node];
        }
        return ordinal;
    }

    /** Signed-long upper bound: count of keys smaller than or equal to key. */
    public int upperBound(long key) {
        if (left == null) {
            int lo = 0, hi = liveSize;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (Long.compare(sortKeys[flat[mid]], key) <= 0) lo = mid + 1;
                else hi = mid;
            }
            return lo;
        }
        int ordinal = 0, node = root;
        while (node != 0) {
            if (Long.compare(sortKeys[node - 1], key) <= 0) {
                ordinal += 1 + count(left[node]);
                node = right[node];
            } else node = left[node];
        }
        return ordinal;
    }

    public int countOfKey(long key) { return upperBound(key) - lowerBound(key); }

    /** Signed-long key ordering; duplicate keys retain ascending stable-ID order. */
    public int sortedIdAt(int ordinal) {
        Objects.checkIndex(ordinal, liveSize);
        if (left == null) return flat[ordinal];
        int node = root, rank = ordinal;
        while (node != 0) {
            int less = count(left[node]);
            if (rank < less) node = left[node];
            else if (rank == less) return node - 1;
            else { rank -= less + 1; node = right[node]; }
        }
        throw new IllegalStateException("broken tree rank");
    }

    public long sortedValueAt(int ordinal) { return values[sortedIdAt(ordinal)]; }

    /** O(log n) lookup of one live stable ID's current ordered rank. */
    public int rankOfId(int id) {
        int checked = requireId(id);
        if (left == null) {
            int rank = flatSearch(sortKeys[checked], checked);
            if (rank < 0) throw new IllegalStateException("broken flat index");
            return rank;
        }
        int rank = 0, node = root, needle = checked + 1;
        while (node != 0) {
            int cmp = compare(needle, node);
            if (cmp < 0) node = left[node];
            else if (cmp == 0) return rank + count(left[node]);
            else { rank += count(left[node]) + 1; node = right[node]; }
        }
        throw new IllegalStateException("broken tree index");
    }

    public int firstId() {
        if (liveSize == 0) throw new NoSuchElementException();
        return sortedIdAt(0);
    }

    /** Fill one viewport or Top-K range, without building a whole sorted permutation. */
    public int fillSortedIds(int startOrdinal, int[] destination, int offset, int maximum) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromIndexSize(offset, maximum, destination.length);
        if (startOrdinal < 0 || startOrdinal > liveSize)
            throw new IndexOutOfBoundsException(startOrdinal);
        int n = Math.min(maximum, liveSize - startOrdinal);
        if (n == 0) return 0;
        if (left == null) {
            System.arraycopy(flat, startOrdinal, destination, offset, n);
            return n;
        }
        // AVL height < 64 for an int-sized collection: one tiny call-local primitive stack.
        int[] stack = new int[64];
        int top = 0, node = root, ordinal = startOrdinal;
        while (node != 0) {
            int lesser = count(left[node]);
            if (ordinal < lesser) {
                stack[top++] = node;
                node = left[node];
            } else if (ordinal == lesser) {
                stack[top++] = node;
                break;
            } else {
                ordinal -= lesser + 1;
                node = right[node];
            }
        }
        for (int i = 0; i < n; i++) {
            if (top == 0) throw new IllegalStateException("broken AVL successor traversal");
            node = stack[--top];
            destination[offset + i] = node - 1;
            node = right[node];
            while (node != 0) {
                stack[top++] = node;
                node = left[node];
            }
        }
        return n;
    }

    private int requireId(int id) {
        if (!containsId(id)) throw new IndexOutOfBoundsException("absent stable ID: " + id);
        return id;
    }

    private int flatInsertion(long key, int id) { return flatInsertion(key, id, liveSize); }
    private int flatInsertion(long key, int id, int limit) {
        int low = 0, high = limit;
        while (low < high) {
            int mid = (low + high) >>> 1;
            int candidate = flat[mid];
            int cmp = Long.compare(sortKeys[candidate], key);
            if (cmp == 0) cmp = Integer.compare(candidate, id);
            if (cmp < 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    private int flatSearch(long key, int id) {
        int slot = flatInsertion(key, id);
        return slot < liveSize && flat[slot] == id ? slot : -1;
    }

    /** Stage every growth lane before publishing any replacement. */
    private void ensureCapacity(int required) {
        if (required <= values.length) return;
        long preferred = (long) values.length + (values.length >>> 1) + 1L;
        if (preferred < required) preferred = required;
        int capacity = Math.toIntExact(M3RuntimeHeap.chooseAmortizedCapacity(
                required, Math.min((long) Integer.MAX_VALUE - 1L, preferred),
                PEAK_BYTES_PER_SLOT));
        long[] nextValues = Arrays.copyOf(values, capacity);
        long[] nextKeys = Arrays.copyOf(sortKeys, capacity);
        byte[] nextLive = Arrays.copyOf(live, capacity);
        int[] nextFlat = flat == null ? null : Arrays.copyOf(flat, capacity);
        int[] nextLeft = left == null ? null : Arrays.copyOf(left, capacity + 1);
        int[] nextRight = right == null ? null : Arrays.copyOf(right, capacity + 1);
        int[] nextSubtree = subtree == null ? null : Arrays.copyOf(subtree, capacity + 1);
        byte[] nextHeights = heights == null ? null : Arrays.copyOf(heights, capacity + 1);
        values = nextValues;
        sortKeys = nextKeys;
        live = nextLive;
        flat = nextFlat;
        left = nextLeft;
        right = nextRight;
        subtree = nextSubtree;
        heights = nextHeights;
    }

    private static int buildBalanced(int[] ordered, int low, int high,
            int[] l, int[] r, int[] weights, byte[] heights) {
        if (low == high) return 0;
        int mid = (low + high) >>> 1;
        int node = ordered[mid] + 1;
        l[node] = buildBalanced(ordered, low, mid, l, r, weights, heights);
        r[node] = buildBalanced(ordered, mid + 1, high, l, r, weights, heights);
        weights[node] = high - low;
        heights[node] = (byte) (1 + Math.max(heights[l[node]] & 0xff,
                heights[r[node]] & 0xff));
        return node;
    }

    private int count(int node) { return node == 0 ? 0 : subtree[node]; }
    private int height(int node) { return node == 0 ? 0 : heights[node] & 0xff; }
    private int compare(int a, int b) {
        int cmp = Long.compare(sortKeys[a - 1], sortKeys[b - 1]);
        return cmp != 0 ? cmp : Integer.compare(a, b);
    }
    private void initializeLeaf(int node) {
        left[node] = 0;
        right[node] = 0;
        subtree[node] = 1;
        heights[node] = 1;
    }
    private void refresh(int node) {
        subtree[node] = 1 + count(left[node]) + count(right[node]);
        heights[node] = (byte) (1 + Math.max(height(left[node]), height(right[node])));
    }
    private int rotateLeft(int node) {
        int pivot = right[node];
        right[node] = left[pivot];
        left[pivot] = node;
        refresh(node);
        refresh(pivot);
        return pivot;
    }
    private int rotateRight(int node) {
        int pivot = left[node];
        left[node] = right[pivot];
        right[pivot] = node;
        refresh(node);
        refresh(pivot);
        return pivot;
    }
    private int balance(int node) {
        refresh(node);
        int diff = height(left[node]) - height(right[node]);
        if (diff > 1) {
            if (height(left[left[node]]) < height(right[left[node]]))
                left[node] = rotateLeft(left[node]);
            return rotateRight(node);
        }
        if (diff < -1) {
            if (height(right[right[node]]) < height(left[right[node]]))
                right[node] = rotateRight(right[node]);
            return rotateLeft(node);
        }
        return node;
    }
    private int insert(int node, int id) {
        if (node == 0) return id;
        if (compare(id, node) < 0) left[node] = insert(left[node], id);
        else right[node] = insert(right[node], id);
        return balance(node);
    }
    private int detachMinimum(int node) {
        if (left[node] == 0) return right[node];
        left[node] = detachMinimum(left[node]);
        return balance(node);
    }
    /** Transplant the successor's stable *node ID*; never rewrite another element's ID. */
    private int delete(int node, int id) {
        if (node == 0) throw new IllegalStateException("missing AVL occurrence");
        int cmp = compare(id, node);
        if (cmp < 0) left[node] = delete(left[node], id);
        else if (cmp > 0) right[node] = delete(right[node], id);
        else {
            int oldLeft = left[node], oldRight = right[node];
            if (oldLeft == 0) return oldRight;
            if (oldRight == 0) return oldLeft;
            int successor = oldRight;
            while (left[successor] != 0) successor = left[successor];
            int remainingRight = detachMinimum(oldRight);
            left[successor] = oldLeft;
            right[successor] = remainingRight;
            return balance(successor);
        }
        return balance(node);
    }
}
