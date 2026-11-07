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
package jdk.m3.collections;

import java.util.Objects;

/**
 * Bulk primitive projection of comparison evidence over an already-precomputed signal lane.
 * The caller owns every input and output array. No per-pair object is allocated or retained.
 * Distance is scalar separation, not a semantic metric or proof of object equality.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatSignalComparisonBatch.java}
 * blob {@code ae3838458a1ae8e24b6ce9131b879273a3c5315e} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatSignalComparisonBatch {
    private M3MatSignalComparisonBatch() {}

    /**
     * Writes signed comparison (-1/0/+1), absolute separation, and availability into caller lanes.
     * Unavailable separation is NaN, including finite operands whose subtraction overflows.
     * Input/output aliasing is rejected before any output mutation.
     */
    public static void compare(
            double[] signals, int[] leftRows, int[] rightRows,
            byte[] order, double[] separation, boolean[] available) {
        Objects.requireNonNull(signals, "signals");
        Objects.requireNonNull(leftRows, "leftRows");
        Objects.requireNonNull(rightRows, "rightRows");
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(separation, "separation");
        Objects.requireNonNull(available, "available");
        int length = leftRows.length;
        if (rightRows.length != length || order.length != length
                || separation.length != length || available.length != length) {
            throw new IllegalArgumentException("pair/output lengths must match");
        }
        if (separation == signals || leftRows == rightRows) {
            throw new IllegalArgumentException("input/output alias or identical pair lanes");
        }
        for (int pair = 0; pair < length; pair++) {
            Objects.checkIndex(leftRows[pair], signals.length);
            Objects.checkIndex(rightRows[pair], signals.length);
        }
        for (int pair = 0; pair < length; pair++) {
            double left = signals[leftRows[pair]];
            double right = signals[rightRows[pair]];
            order[pair] = (byte) Integer.signum(Double.compare(left, right));
            double delta = left - right;
            boolean finite = Double.isFinite(left) && Double.isFinite(right)
                    && Double.isFinite(delta);
            available[pair] = finite;
            separation[pair] = finite ? Math.abs(delta) : Double.NaN;
        }
    }
}
