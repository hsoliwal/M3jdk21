/* *****************************************************************************
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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Dynamic long multiset with rank/select in O(log distinct) using an array-backed AVL tree.
 *
 * <p>Each logical node is an integer index into parallel primitive lanes:
 * key, multiplicity, subtree multiplicity, left, right and height. Deleted
 * node indexes are recycled through the left[] lane; no Java node object is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongOrderStatisticMultiset.java}
 * blob {@code 6c7dee1415bfc0ec39e337f35a3ca0068898397d} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongOrderStatisticMultiset {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public LongOrderStatisticMultiset() {
    }

    private long[] keys = new long[17];
    private long[] multiplicities = new long[17];
    private long[] subtreeCounts = new long[17];
    private int[] left = new int[17];
    private int[] right = new int[17];
    private byte[] heights = new byte[17];

    private int root;
    private int nextNode = 1;
    private int freeHead;
    private int distinct;
    private long totalCount;

    public int distinctSize() { return distinct; }
    public long size() { return totalCount; }
    public boolean isEmpty() { return totalCount == 0L; }
    public long payloadBytes() {
        return (long) keys.length * Long.BYTES * 3L
                + (long) left.length * Integer.BYTES * 2L
                + heights.length;
    }

    public long count(long key) {
        int node = root;
        while (node != 0) {
            int comparison = Long.compare(key, keys[node]);
            if (comparison == 0) return multiplicities[node];
            node = comparison < 0 ? left[node] : right[node];
        }
        return 0L;
    }

    public void add(long key) {
        add(key, 1L);
    }

    public void add(long key, long occurrences) {
        if (occurrences <= 0L) throw new IllegalArgumentException("occurrences <= 0");
        long nextTotal = Math.addExact(totalCount, occurrences);
        root = insert(root, key, occurrences);
        totalCount = nextTotal;
    }

    public boolean remove(long key) {
        return remove(key, 1L) != 0L;
    }

    /** Removes up to occurrences and returns the number actually removed. */
    public long remove(long key, long occurrences) {
        if (occurrences <= 0L) throw new IllegalArgumentException("occurrences <= 0");
        long existing = count(key);
        if (existing == 0L) return 0L;
        long removed = Math.min(existing, occurrences);
        root = removeNode(root, key, removed);
        totalCount -= removed;
        return removed;
    }

    /** Number of occurrences whose key is strictly less than key. */
    public long rank(long key) {
        long result = 0L;
        int node = root;
        while (node != 0) {
            if (key <= keys[node]) {
                node = left[node];
            } else {
                result = Math.addExact(
                        result,
                        Math.addExact(subtree(left[node]), multiplicities[node]));
                node = right[node];
            }
        }
        return result;
    }

    /** Zero-based occurrence ordinal -> key. */
    public long select(long ordinal) {
        if (ordinal < 0L || ordinal >= totalCount) {
            throw new IndexOutOfBoundsException("ordinal=" + ordinal);
        }
        int node = root;
        long remaining = ordinal;
        while (node != 0) {
            long leftCount = subtree(left[node]);
            if (remaining < leftCount) {
                node = left[node];
            } else if (remaining < leftCount + multiplicities[node]) {
                return keys[node];
            } else {
                remaining -= leftCount + multiplicities[node];
                node = right[node];
            }
        }
        throw new IllegalStateException("subtree counts corrupted");
    }

    public long firstKey() {
        if (root == 0) throw new NoSuchElementException();
        int node = root;
        while (left[node] != 0) node = left[node];
        return keys[node];
    }

    public long lastKey() {
        if (root == 0) throw new NoSuchElementException();
        int node = root;
        while (right[node] != 0) node = right[node];
        return keys[node];
    }

    /**
     * Logical O(1) clear. All six payload lanes are primitives and no internal
     * array view escapes this class; the next allocation overwrites every lane
     * of each reused node. Retain allocated capacity to avoid repeated growth.
     * No secure memory erasure is implied by this collection operation.
     */
    public void clear() {
        root = 0;
        nextNode = 1;
        freeHead = 0;
        distinct = 0;
        totalCount = 0L;
    }

    private int insert(int node, long key, long occurrences) {
        if (node == 0) return allocate(key, occurrences);
        int comparison = Long.compare(key, keys[node]);
        if (comparison < 0) {
            // The recursive insert may grow and replace left[]. Evaluate the
            // array owner AFTER recursion, or the new child is written only
            // into the stale array and disappears from the live AVL tree.
            int child = insert(left[node], key, occurrences);
            left[node] = child;
        } else if (comparison > 0) {
            int child = insert(right[node], key, occurrences);
            right[node] = child;
        } else {
            multiplicities[node] = Math.addExact(multiplicities[node], occurrences);
            refresh(node);
            return node;
        }
        return rebalance(node);
    }

    private int removeNode(int node, long key, long occurrences) {
        int comparison = Long.compare(key, keys[node]);
        if (comparison < 0) {
            left[node] = removeNode(left[node], key, occurrences);
            return rebalance(node);
        }
        if (comparison > 0) {
            right[node] = removeNode(right[node], key, occurrences);
            return rebalance(node);
        }

        if (occurrences < multiplicities[node]) {
            multiplicities[node] -= occurrences;
            refresh(node);
            return node;
        }

        if (left[node] == 0 || right[node] == 0) {
            int replacement = left[node] != 0 ? left[node] : right[node];
            release(node);
            return replacement;
        }

        int successor = right[node];
        while (left[successor] != 0) successor = left[successor];
        long successorKey = keys[successor];
        keys[node] = successorKey;
        multiplicities[node] = multiplicities[successor];
        right[node] = removeEntire(right[node], successorKey);
        return rebalance(node);
    }

    private int removeEntire(int node, long key) {
        int comparison = Long.compare(key, keys[node]);
        if (comparison < 0) {
            left[node] = removeEntire(left[node], key);
            return rebalance(node);
        }
        if (comparison > 0) {
            right[node] = removeEntire(right[node], key);
            return rebalance(node);
        }
        if (left[node] == 0 || right[node] == 0) {
            int replacement = left[node] != 0 ? left[node] : right[node];
            release(node);
            return replacement;
        }
        int successor = right[node];
        while (left[successor] != 0) successor = left[successor];
        long successorKey = keys[successor];
        keys[node] = successorKey;
        multiplicities[node] = multiplicities[successor];
        right[node] = removeEntire(right[node], successorKey);
        return rebalance(node);
    }

    private int rebalance(int node) {
        refresh(node);
        int balance = height(left[node]) - height(right[node]);
        if (balance > 1) {
            if (height(left[left[node]]) < height(right[left[node]])) {
                left[node] = rotateLeft(left[node]);
            }
            return rotateRight(node);
        }
        if (balance < -1) {
            if (height(right[right[node]]) < height(left[right[node]])) {
                right[node] = rotateRight(right[node]);
            }
            return rotateLeft(node);
        }
        return node;
    }

    private int rotateLeft(int node) {
        int pivot = right[node];
        int transfer = left[pivot];
        left[pivot] = node;
        right[node] = transfer;
        refresh(node);
        refresh(pivot);
        return pivot;
    }

    private int rotateRight(int node) {
        int pivot = left[node];
        int transfer = right[pivot];
        right[pivot] = node;
        left[node] = transfer;
        refresh(node);
        refresh(pivot);
        return pivot;
    }

    private void refresh(int node) {
        subtreeCounts[node] = Math.addExact(
                multiplicities[node],
                Math.addExact(subtree(left[node]), subtree(right[node])));
        heights[node] = (byte) (1 + Math.max(height(left[node]), height(right[node])));
    }

    private int allocate(long key, long occurrences) {
        int node;
        if (freeHead != 0) {
            node = freeHead;
            freeHead = left[node];
        } else {
            if (nextNode == keys.length) grow();
            node = nextNode++;
        }
        keys[node] = key;
        multiplicities[node] = occurrences;
        subtreeCounts[node] = occurrences;
        left[node] = 0;
        right[node] = 0;
        heights[node] = 1;
        distinct++;
        return node;
    }

    private void release(int node) {
        keys[node] = 0L;
        multiplicities[node] = 0L;
        subtreeCounts[node] = 0L;
        right[node] = 0;
        heights[node] = 0;
        left[node] = freeHead;
        freeHead = node;
        distinct--;
    }

    private void grow() {
        int next = Math.addExact(keys.length, (keys.length >>> 1) + 1);
        // Stage every parallel primitive lane before publishing any of them.
        // An allocation failure must not expose mismatched live array lengths.
        // The transient peak memory is the old plus the new lanes; this is an
        // intentional failure-atomicity trade-off, not an allocation speed claim.
        long[] nextKeys = Arrays.copyOf(keys, next);
        long[] nextMultiplicities = Arrays.copyOf(multiplicities, next);
        long[] nextSubtreeCounts = Arrays.copyOf(subtreeCounts, next);
        int[] nextLeft = Arrays.copyOf(left, next);
        int[] nextRight = Arrays.copyOf(right, next);
        byte[] nextHeights = Arrays.copyOf(heights, next);
        keys = nextKeys;
        multiplicities = nextMultiplicities;
        subtreeCounts = nextSubtreeCounts;
        left = nextLeft;
        right = nextRight;
        heights = nextHeights;
    }

    private int height(int node) {
        return node == 0 ? 0 : heights[node] & 0xff;
    }

    private long subtree(int node) {
        return node == 0 ? 0L : subtreeCounts[node];
    }
}
