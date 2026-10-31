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
// Modified 2026: reuse the private signal permutation for its terminal physical-coordinate remap.
/*
 * Adapted from Synexia MatSignalDimensions: https://github.com/hsoliwal/com.synexia/pull/11656
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;

/**
 * Stable lexicographic ordering over independent, dimension-major double signal lanes.
 * The first dimension is most significant; equal tuples retain source encounter order.
 * Filtering uses aligned byte state masks before sorting. There are no retained entry objects,
 * native object pointers, pairwise comparison caches, or runtime Synexia dependencies.
 *
 * <p>This is an ordering projection, not a proof that domain objects are equal or that
 * a numerical gap measures semantic/graph distance. Exact domain ordering requires the
 * producer to establish monotonicity of the supplied dimensions.</p>
 */
public final class M3SignalDimensions {
    private M3SignalDimensions() { }

    public static int[] order(double[][] columns, boolean[] descending) {
        return order(columns, descending, M3SignalOrder.Backend.JAVA);
    }

    public static int[] order(double[][] columns, boolean[] descending,
            M3SignalOrder.Backend backend) {
        return filtered(columns, descending, null, 0, 0, backend);
    }

    /** Masks are aligned to source row IDs, not to sorted positions. */
    public static int[] filtered(double[][] columns, boolean[] descending,
            byte[] state, int required, int excluded, M3SignalOrder.Backend backend) {
        return filtered(columns, descending, state, required, excluded, backend,
                M3IndexMaskCompactor.Backend.JAVA);
    }

    /**
     * Explicit mask-selection backend. Existing overloads remain Java-mask by default,
     * so requesting native signal sorting does not acquire a new mask JNI dependency.
     */
    public static int[] filtered(double[][] columns, boolean[] descending,
            byte[] state, int required, int excluded, M3SignalOrder.Backend backend,
            M3IndexMaskCompactor.Backend maskBackend) {
        int length = validate(columns, descending);
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(maskBackend, "maskBackend");
        validateState(state, length, required, excluded);
        if (backend == M3SignalOrder.Backend.NATIVE_REQUIRED && !M3SignalOrderNative.available()) {
            throw new UnsatisfiedLinkError("m3signalorder JNI is unavailable");
        }
        int[] rows = state == null
                ? identity(length)
                : M3IndexMaskCompactor.selectPositions(
                        state, required, excluded, maskBackend);
        return orderSelected(columns, descending, backend, rows);
    }

    /**
     * Stable multi-dimensional order over full-width physical-slot state masks.
     * Selection uses all 32 bits (including bit 31) before reading signal rows;
     * only source row ordinals and primitive radix scratch are allocated.
     * The caller's state and signal lanes are never modified or retained.
     */
    public static int[] filtered32(double[][] columns, boolean[] descending,
            int[] states, int requiredMask, int excludedMask,
            M3SignalOrder.Backend backend) {
        int length = validate(columns, descending);
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(states, "states");
        if (states.length != length) {
            throw new IllegalArgumentException("state array length mismatch");
        }
        if (backend == M3SignalOrder.Backend.NATIVE_REQUIRED
                && !M3SignalOrderNative.available()) {
            throw new UnsatisfiedLinkError("m3signalorder JNI is unavailable");
        }
        int selected = 0;
        for (int row = 0; row < length; row++) {
            int flags = states[row];
            if ((flags & requiredMask) == requiredMask
                    && (flags & excludedMask) == 0) selected++;
        }
        int[] rows = new int[selected];
        int count = 0;
        for (int row = 0; row < length; row++) {
            int flags = states[row];
            if ((flags & requiredMask) == requiredMask
                    && (flags & excludedMask) == 0) rows[count++] = row;
        }
        return orderSelected(columns, descending, backend, rows);
    }

    /**
     * Extract each source-row projection only once per selected row and dimension.
     * The caller's provider remains the value owner; only primitive lanes enter radix passes.
     */
    public static <T> int[] project(int rowCount, IntFunction<? extends T> valueAt,
            List<? extends ToDoubleFunction<? super T>> projections,
            boolean[] descending, byte[] state, int required, int excluded,
            M3SignalOrder.Backend backend) {
        return project(rowCount, valueAt, projections, descending, state, required, excluded,
                backend, M3IndexMaskCompactor.Backend.JAVA);
    }

    /**
     * Explicit mask-selection backend for source-row filtering before projections execute.
     * Filtering remains stable and projection callbacks run exactly once per selected row/dimension.
     */
    public static <T> int[] project(int rowCount, IntFunction<? extends T> valueAt,
            List<? extends ToDoubleFunction<? super T>> projections,
            boolean[] descending, byte[] state, int required, int excluded,
            M3SignalOrder.Backend backend, M3IndexMaskCompactor.Backend maskBackend) {
        if (rowCount < 0) throw new IllegalArgumentException("negative rowCount");
        Objects.requireNonNull(valueAt, "valueAt");
        Objects.requireNonNull(projections, "projections");
        Objects.requireNonNull(descending, "descending");
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(maskBackend, "maskBackend");
        if (projections.isEmpty() || projections.size() != descending.length) {
            throw new IllegalArgumentException("projection/direction dimension mismatch");
        }
        validateState(state, rowCount, required, excluded);
        if (backend == M3SignalOrder.Backend.NATIVE_REQUIRED && !M3SignalOrderNative.available()) {
            throw new UnsatisfiedLinkError("m3signalorder JNI is unavailable");
        }
        int[] physical = state == null
                ? identity(rowCount)
                : M3IndexMaskCompactor.selectPositions(
                        state, required, excluded, maskBackend);
        int selected = physical.length;
        double[][] prepared = new double[projections.size()][selected];
        for (int dimension = 0; dimension < projections.size(); dimension++) {
            ToDoubleFunction<? super T> projection = Objects.requireNonNull(
                    projections.get(dimension), "projection[" + dimension + "]");
            for (int position = 0; position < selected; position++) {
                T value = Objects.requireNonNull(valueAt.apply(physical[position]),
                        "value at row " + physical[position]);
                prepared[dimension][position] = projection.applyAsDouble(value);
            }
        }
        int[] relative = orderSelected(prepared, descending, backend, identity(selected));
        // Both coordinate lanes are private; reuse the final permutation for physical rows.
        for (int index = 0; index < selected; index++) {
            relative[index] = physical[relative[index]];
        }
        return relative;
    }

    /**
     * Preserve all scalar separations and the first decisive dimension.
     * NaN indicates that finite numeric separation is unavailable; signs follow Double.compare.
     */
    public static Comparison compare(double[][] columns, boolean[] descending,
            int leftRow, int rightRow) {
        int count = validate(columns, descending);
        Objects.checkIndex(leftRow, count);
        Objects.checkIndex(rightRow, count);
        int sign = 0;
        int first = -1;
        double[] distances = new double[columns.length];
        for (int dimension = 0; dimension < columns.length; dimension++) {
            double left = columns[dimension][leftRow];
            double right = columns[dimension][rightRow];
            int direction = Integer.signum(Double.compare(left, right));
            if (first < 0 && direction != 0) {
                first = dimension;
                sign = descending[dimension] ? -direction : direction;
            }
            double gap = Math.abs(left - right);
            distances[dimension] = Double.isFinite(left) && Double.isFinite(right)
                    && Double.isFinite(gap) ? gap : Double.NaN;
        }
        return new Comparison(sign, first, distances);
    }

    public record Comparison(int sign, int decisiveDimension, double[] numericSeparations) {
        public Comparison {
            Objects.requireNonNull(numericSeparations, "numericSeparations");
            if (sign < -1 || sign > 1 || decisiveDimension < -1
                    || decisiveDimension >= numericSeparations.length
                    || (decisiveDimension == -1) != (sign == 0)) {
                throw new IllegalArgumentException("invalid comparison sign/dimension");
            }
            for (double distance : numericSeparations) {
                if (!Double.isNaN(distance)
                        && (!Double.isFinite(distance) || distance < 0.0)) {
                    throw new IllegalArgumentException("invalid numeric separation");
                }
            }
            numericSeparations = numericSeparations.clone();
        }

        @Override public double[] numericSeparations() {
            return numericSeparations.clone();
        }

        public boolean finiteAt(int dimension) {
            return Double.isFinite(numericSeparations[
                    Objects.checkIndex(dimension, numericSeparations.length)]);
        }
    }

    private static int[] orderSelected(double[][] columns, boolean[] descending,
            M3SignalOrder.Backend backend, int[] rows) {
        if (rows.length < 2) return rows;
        double[] workingSignals = new double[rows.length];
        int[] next = new int[rows.length];
        byte[] nativeAll = backend == M3SignalOrder.Backend.NATIVE_REQUIRED
                ? new byte[rows.length] : null;
        for (int dimension = columns.length - 1; dimension >= 0; dimension--) {
            for (int index = 0; index < rows.length; index++) {
                workingSignals[index] = columns[dimension][rows[index]];
            }
            M3SignalOrder pass = backend == M3SignalOrder.Backend.JAVA
                    ? (descending[dimension] ? M3SignalOrder.descending(workingSignals)
                            : M3SignalOrder.ascending(workingSignals))
                    : M3SignalOrder.filtered(workingSignals, nativeAll, 0, 0,
                            !descending[dimension], backend);
            for (int index = 0; index < rows.length; index++) {
                next[index] = rows[pass.rowAt(index)];
            }
            int[] swap = rows;
            rows = next;
            next = swap;
        }
        return rows;
    }

    private static int validate(double[][] columns, boolean[] descending) {
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(descending, "descending");
        if (columns.length == 0 || columns.length != descending.length) {
            throw new IllegalArgumentException("signal/direction dimension mismatch");
        }
        int count = Objects.requireNonNull(columns[0], "columns[0]").length;
        for (int d = 1; d < columns.length; d++) {
            double[] lane = Objects.requireNonNull(columns[d], "columns[" + d + "]");
            if (lane.length != count) throw new IllegalArgumentException("unequal dimension rows");
        }
        return count;
    }

    private static void validateState(byte[] state, int count, int required, int excluded) {
        if ((required | excluded) < 0 || ((required | excluded) & ~0xff) != 0) {
            throw new IllegalArgumentException("state mask outside 0..255");
        }
        if (state == null && (required != 0 || excluded != 0)) {
            throw new IllegalArgumentException("mask flags require state array");
        }
        if (state != null && state.length != count) {
            throw new IllegalArgumentException("state array length mismatch");
        }
    }

    private static int[] identity(int count) {
        int[] indices = new int[count];
        for (int index = 0; index < count; index++) indices[index] = index;
        return indices;
    }
}
