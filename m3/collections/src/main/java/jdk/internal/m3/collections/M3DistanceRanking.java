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
import java.util.function.IntToLongFunction;

/**
 * Query-relative ranking by explicitly supplied, frozen, nonnegative integral distances.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/DistanceRanking.java}
 * blob {@code 8a0fe72ce955980d9495aa21ec82c4008f22dc47} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3DistanceRanking {
    private M3DistanceRanking() { }

    /** Extract once per source ordinal, then sort; source order breaks equal-distance ties. */
    public static int[] rank(int size, IntToLongFunction distance) {
        return M3LongKeyOrder.ascending(capture(size, distance), false);
    }

    /**
     * Exact nearest-K. k=0 skips extraction; otherwise every source distance is evaluated once.
     * The source is not pruned by an unproved heuristic. Storage is 8*n+4*min(k,n) payload bytes.
     */
    public static int[] nearest(int size, int k, IntToLongFunction distance) {
        if (size < 0 || k < 0) throw new IllegalArgumentException("negative size or k");
        Objects.requireNonNull(distance, "distance");
        return k == 0 ? new int[0] : M3LongKeyOrder.smallest(capture(size, distance), k, false);
    }

    private static long[] capture(int size, IntToLongFunction distance) {
        if (size < 0) throw new IllegalArgumentException("size");
        Objects.requireNonNull(distance, "distance");
        long[] scores = new long[size];
        for (int i = 0; i < size; i++) {
            long score = distance.applyAsLong(i);
            if (score < 0) throw new IllegalArgumentException("negative distance at " + i);
            scores[i] = score;
        }
        return scores;
    }
}
