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

/**
 * Primitive merge-head selector. Only the winner is advanced after build; no record objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/RunHeadSelector.java}
 * blob {@code e3db1ba374fa9444eda5775e285293d0b33749ca} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3RunHeadSelector {
    private final long[] keys;
    private final long[] ordinals;
    private final boolean[] live;
    private final int[] positions;
    private final boolean unsigned;
    private final boolean tournament;
    private final boolean loserTree;
    private int loserWinner = -1;
    private final int base;
    private int size;
    M3RunHeadSelector(int runs, boolean unsigned, M3ExternalSortOptions.Selector mode) {
        keys = new long[runs]; ordinals = new long[runs]; live = new boolean[runs];
        this.unsigned = unsigned; tournament = mode == M3ExternalSortOptions.Selector.TOURNAMENT;
        loserTree = mode == M3ExternalSortOptions.Selector.LOSER_TREE;
        int power = 1; while (power < runs) power <<= 1;
        base = power; positions = new int[tournament || loserTree ? 2 * base : runs];
    }
    void initial(int id, long key, long ordinal, boolean active) {
        keys[id] = key; ordinals[id] = ordinal; live[id] = active;
    }
    void build() {
        if (loserTree) {
            int[] winners = new int[2 * base]; Arrays.fill(winners, -1); Arrays.fill(positions, -1);
            for (int i = 0; i < live.length; i++) if (live[i]) winners[base+i] = i;
            for (int at = base-1; at > 0; at--) {
                int a = winners[2*at], b = winners[2*at+1], win = choose(a,b);
                winners[at] = win; positions[at] = win == a ? b : a;
            }
            loserWinner = winners[1];
        } else if (tournament) {
            Arrays.fill(positions, -1);
            for (int i = 0; i < live.length; i++) if (live[i]) positions[base + i] = i;
            for (int i = base - 1; i > 0; i--) positions[i] = choose(positions[2*i], positions[2*i+1]);
        } else {
            size = 0;
            for (int i = 0; i < live.length; i++) if (live[i]) positions[size++] = i;
            for (int i = (size >>> 1) - 1; i >= 0; i--) sift(i);
        }
    }
    int winner() { return loserTree ? loserWinner : tournament ? positions[1] : size == 0 ? -1 : positions[0]; }
    void advance(int id, long key, long ordinal, boolean active) {
        if (winner() != id) throw new IllegalStateException("only winning run may advance");
        initial(id, key, ordinal, active);
        if (loserTree) {
            int candidate = active ? id : -1;
            for (int at = (base+id) >>> 1; at > 0; at >>>= 1) {
                int opponent = positions[at], win = choose(candidate,opponent);
                positions[at] = win == candidate ? opponent : candidate; candidate = win;
            }
            loserWinner = candidate;
        } else if (tournament) {
            int at = base + id; positions[at] = active ? id : -1;
            while ((at >>>= 1) != 0) positions[at] = choose(positions[2*at], positions[2*at+1]);
        } else {
            if (!active) positions[0] = positions[--size];
            if (size != 0) sift(0);
        }
    }
    private int choose(int a, int b) {
        if (a < 0) return b; if (b < 0) return a;
        int c = unsigned ? Long.compareUnsigned(keys[a], keys[b]) : Long.compare(keys[a], keys[b]);
        if (c == 0) c = Long.compare(ordinals[a], ordinals[b]);
        return c <= 0 ? a : b;
    }
    private void sift(int at) {
        int id = positions[at];
        while (at < (size >>> 1)) {
            int child = 2 * at + 1;
            if (child + 1 < size && choose(positions[child], positions[child+1]) == positions[child+1]) child++;
            if (choose(id, positions[child]) == id) break;
            positions[at] = positions[child]; at = child;
        }
        positions[at] = id;
    }
}
