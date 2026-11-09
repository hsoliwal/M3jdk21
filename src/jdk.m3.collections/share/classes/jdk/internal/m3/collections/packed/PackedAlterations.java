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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;

/**
 * Sparse backing-index changes, augmented with index-delta and work totals.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/packedlists/PackedAlterations.java}
 * blob {@code 8ee44773aa364ab77c9372b80421ae9224f86d60} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedAlterations extends IntAvlForest {
    private static final long serialVersionUID = 1L;
    private int[][] keys = new int[0][];
    private int[][] deltas = new int[0][];
    private long[][] sums = new long[0][];
    private long[][] hits = new long[0][];
    private int root;

    @Override void growExtraDirectory(int n) {
        keys = Arrays.copyOf(keys, n);
        deltas = Arrays.copyOf(deltas, n);
        sums = Arrays.copyOf(sums, n);
        hits = Arrays.copyOf(hits, n);
    }
    @Override void allocateExtraLeaf(int s) {
        keys[s] = new int[LEAF_SIZE];
        deltas[s] = new int[LEAF_SIZE];
        sums[s] = new long[LEAF_SIZE];
        hits[s] = new long[LEAF_SIZE];
    }
    @Override void clearExtraSlot(int s) {
        keys[s >>> SHIFT][s & MASK] = 0;
        deltas[s >>> SHIFT][s & MASK] = 0;
        sums[s >>> SHIFT][s & MASK] = 0;
        hits[s >>> SHIFT][s & MASK] = 0;
    }
    @Override void clearExtraLeaf(int s) {
        Arrays.fill(keys[s], 0);
        Arrays.fill(deltas[s], 0);
        Arrays.fill(sums[s], 0);
        Arrays.fill(hits[s], 0);
    }
    @Override void refreshExtra(int s) {
        sums[s >>> SHIFT][s & MASK] = delta(s) + sum(left(s)) + sum(right(s));
        hits[s >>> SHIFT][s & MASK] = (delta(s) > 0 ? 1L : -(long) delta(s))
                + hit(left(s)) + hit(right(s));
    }
    int root() { return root; }
    int key(int s) { return keys[s >>> SHIFT][s & MASK]; }
    int delta(int s) { return deltas[s >>> SHIFT][s & MASK]; }
    long sum(int s) { return s == 0 ? 0 : sums[s >>> SHIFT][s & MASK]; }
    long hit(int s) { return s == 0 ? 0 : hits[s >>> SHIFT][s & MASK]; }
    int size() { return count(root); }
    int performanceHit() { return Math.toIntExact(hit(root)); }
    int first() { return first(root); }

    int ceiling(int key) {
        int s = root, found = 0;
        while (s != 0) {
            if (key(s) >= key) { found = s; s = left(s); }
            else s = right(s);
        }
        return found;
    }

    int covering(int index) {
        if (index < 0) return 0;
        int s = root, found = 0;
        while (s != 0) {
            if (key(s) <= index) { found = s; s = right(s); }
            else s = left(s);
        }
        if (found == 0) return 0;
        return index == key(found) || (delta(found) > 0 && (long) index < (long) key(found) + delta(found))
                ? found : 0;
    }

    void put(int key, int delta) {
        if (key < 0 || delta == 0 || delta == Integer.MIN_VALUE) throw new IllegalArgumentException();
        int s = root, rank = 0;
        while (s != 0) {
            if (key == key(s)) {
                deltas[s >>> SHIFT][s & MASK] = delta;
                refreshToRoot(s);
                return;
            }
            if (key < key(s)) s = left(s);
            else { rank += count(left(s)) + 1; s = right(s); }
        }
        s = allocate();
        keys[s >>> SHIFT][s & MASK] = key;
        deltas[s >>> SHIFT][s & MASK] = delta;
        root = insertSlot(root, rank, s);
    }

    void removeEntry(int index) {
        int s = covering(index);
        if (s == 0 || key(s) != index) throw new IllegalStateException("Missing alteration");
        root = removeSlot(root, s);
    }

    /** Turns one removed cell back into a value, splitting a run only when needed. */
    void fillHole(int index) {
        int s = covering(index);
        if (s == 0 || delta(s) < 1) throw new IllegalStateException("Missing hole");
        int start = key(s), length = delta(s);
        if (length == 1) { root = removeSlot(root, s); return; }
        if (index == start) {
            keys[s >>> SHIFT][s & MASK] = start + 1;
            deltas[s >>> SHIFT][s & MASK] = length - 1;
            refreshToRoot(s);
        } else if (index == start + length - 1) {
            put(start, length - 1);
        } else {
            put(start, index - start);
            put(index + 1, length - (index - start) - 1);
        }
    }

    /** Joins adjacent removed runs; callers guarantee index was a normal cell. */
    void removeCell(int index) {
        int a = covering(index - 1), b = covering(index + 1);
        if (a != 0 && delta(a) < 0) a = 0;
        if (b != 0 && delta(b) < 0) b = 0;
        if (a != 0) {
            int start = key(a), length = delta(a) + 1;
            if (b != 0) {
                length = Math.addExact(length, delta(b));
                root = removeSlot(root, b);
            }
            put(start, length);
        } else if (b != 0) {
            keys[b >>> SHIFT][b & MASK] = index;
            deltas[b >>> SHIFT][b & MASK] = Math.addExact(delta(b), 1);
            refreshToRoot(b);
        } else put(index, 1);
    }

    /** High word is backing index; low word is sub-index + 1 (zero means normal). */
    long location(int virtualIndex) {
        long main = virtualIndex;
        int s = root;
        while (s != 0) {
            long leftDiff = sum(left(s));
            long adjusted = main + leftDiff;
            if (adjusted < key(s)) s = left(s);
            else {
                if (delta(s) < 0) {
                    long first = key(s) - leftDiff;
                    if (main <= first - delta(s)) {
                        return ((long) key(s) << 32) | ((main - first + 1) & 0xffffffffL);
                    }
                }
                main = adjusted + delta(s);
                s = right(s);
            }
        }
        return (long) Math.toIntExact(main) << 32;
    }

    void clear() { clearArena(); root = 0; }
}
