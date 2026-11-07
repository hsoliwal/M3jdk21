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

import java.util.Objects;

/**
 * Counted 32-bit trie over flat primitive slots, with a fixed node budget including
 * its root. XOR order is unsigned; selection returns the original stored key.
 * Operations are O(32), with slot reuse on removal and no per-operation allocation.
 * Multiplicity overflow and insufficient capacity reject before mutation. Thread-confined.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntBinaryTrie.java}
 * blob {@code 787d847b44f016646437cc5b7d218a33b87bd655} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3IntBinaryTrie {
    private final int[] zero;
    private final int[] one;
    private final long[] counts;
    private final int[] path = new int[33];
    private int used = 1;
    private int free = -1;
    private int freeCount;

    public M3IntBinaryTrie(int nodeBudget) {
        if (nodeBudget < 1) { throw new IllegalArgumentException("root requires one node"); }
        zero = new int[nodeBudget]; one = new int[nodeBudget]; counts = new long[nodeBudget];
        zero[0] = -1; one[0] = -1;
    }

    public long size() { return counts[0]; }
    public int nodeCount() { return used - freeCount; }
    public int nodeBudget() { return counts.length; }

    public long count(int key) {
        int node = 0;
        for (int bit = 31; bit >= 0 && node >= 0; bit--) { node = child(node, (key >>> bit) & 1); }
        return node < 0 ? 0 : counts[node];
    }

    public void add(int key, long copies) {
        if (copies <= 0) { throw new IllegalArgumentException("copies must be positive"); }
        Math.addExact(counts[0], copies);
        int node = 0, missing = 0;
        for (int bit = 31; bit >= 0; bit--) {
            int next = child(node, (key >>> bit) & 1);
            if (next < 0) { missing = bit + 1; break; }
            node = next;
        }
        if (missing > counts.length - used + freeCount) { throw new IllegalStateException("node budget exhausted"); }
        node = 0;
        counts[0] += copies;
        for (int bit = 31; bit >= 0; bit--) {
            int branch = (key >>> bit) & 1;
            int next = child(node, branch);
            if (next < 0) {
                if (free >= 0) { next = free; free = zero[next]; freeCount--; }
                else { next = used++; }
                zero[next] = -1; one[next] = -1; counts[next] = 0;
                setChild(node, branch, next);
            }
            node = next; counts[node] += copies;
        }
    }

    public long remove(int key, long copies) {
        if (copies <= 0) { throw new IllegalArgumentException("copies must be positive"); }
        int node = 0;
        path[0] = 0;
        for (int bit = 31; bit >= 0; bit--) {
            node = child(node, (key >>> bit) & 1);
            if (node < 0) { return 0; }
            path[32 - bit] = node;
        }
        long removed = Math.min(copies, counts[node]);
        for (int depth = 0; depth <= 32; depth++) { counts[path[depth]] -= removed; }
        for (int depth = 32; depth > 0; depth--) {
            node = path[depth];
            if (counts[node] != 0) { break; }
            int branch = (key >>> (32 - depth)) & 1;
            setChild(path[depth - 1], branch, -1);
            zero[node] = free; free = node; freeCount++;
        }
        return removed;
    }

    /** Number of stored occurrences whose unsigned (key XOR mask) is below upperExclusive. */
    public long countXorLessThan(int mask, long upperExclusive) {
        if (upperExclusive < 0 || upperExclusive > (1L << 32)) {
            throw new IllegalArgumentException("unsigned XOR bound outside [0,2^32]");
        }
        if (upperExclusive == (1L << 32)) { return size(); }
        int node = 0;
        long result = 0;
        for (int bit = 31; bit >= 0 && node >= 0; bit--) {
            int branch = (mask >>> bit) & 1;
            if (((upperExclusive >>> bit) & 1) != 0) {
                int smaller = child(node, branch);
                if (smaller >= 0) { result += counts[smaller]; }
                node = child(node, branch ^ 1);
            } else { node = child(node, branch); }
        }
        return result;
    }

    /** Zero-based weighted rank in unsigned XOR order, counting duplicates. */
    public int selectXor(int mask, long ordinal) {
        Objects.checkIndex(ordinal, size());
        int node = 0, key = 0;
        for (int bit = 31; bit >= 0; bit--) {
            int preferred = (mask >>> bit) & 1;
            int next = child(node, preferred);
            long before = next < 0 ? 0 : counts[next];
            if (ordinal >= before) { ordinal -= before; preferred ^= 1; next = child(node, preferred); }
            key |= preferred << bit;
            node = next;
        }
        return key;
    }

    public int minimumXorKey(int mask) { return selectXor(mask, 0); }
    public int maximumXorKey(int mask) { return selectXor(mask, size() - 1); }
    public void clear() { used = 1; free = -1; freeCount = 0; counts[0] = 0; zero[0] = -1; one[0] = -1; }
    private int child(int node, int branch) { return branch == 0 ? zero[node] : one[node]; }
    private void setChild(int node, int branch, int child) {
        if (branch == 0) { zero[node] = child; } else { one[node] = child; }
    }
}
