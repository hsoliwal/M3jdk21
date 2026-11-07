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
import java.util.Objects;

/**
 * Immutable radix-two NTT precompute modulo 998244353; O(N log N) prepared transforms.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntNttPlan.java}
 * blob {@code 5a7b68bd35d6ab9402080f3c56d65f587dfdce40} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3IntNttPlan {
    public static final int MODULUS = 998244353;
    public static final int MAX_SIZE = 1 << 23;
    private final int[] reverse;
    private final int[] roots;
    private final int[] inverseRoots;
    private final int inverseSize;

    public M3IntNttPlan(int size) {
        if (size < 1 || size > MAX_SIZE || (size & (size - 1)) != 0) {
            throw new IllegalArgumentException("size must be a power of two in [1,2^23]");
        }
        reverse = new int[size]; roots = new int[size]; inverseRoots = new int[size];
        int bits = Integer.numberOfTrailingZeros(size);
        for (int i = 0; i < size; i++) { reverse[i] = Integer.reverse(i) >>> (32 - bits); }
        for (int half = 1; half < size; half <<= 1) {
            int step = power(3, (MODULUS - 1) / (half * 2));
            int back = power(step, MODULUS - 2);
            roots[half] = 1; inverseRoots[half] = 1;
            for (int i = 1; i < half; i++) {
                roots[half + i] = multiply(roots[half + i - 1], step);
                inverseRoots[half + i] = multiply(inverseRoots[half + i - 1], back);
            }
        }
        inverseSize = power(size, MODULUS - 2);
    }

    public int size() { return reverse.length; }
    public Workspace workspace() { return new Workspace(); }

    /** Validates exact length and all canonical residues before mutating the caller's array. */
    public void transform(int[] values, boolean inverse) {
        Objects.requireNonNull(values, "values");
        if (values.length != size()) { throw new IllegalArgumentException("transform length differs from plan"); }
        for (int value : values) {
            if (value < 0 || value >= MODULUS) { throw new IllegalArgumentException("noncanonical residue"); }
        }
        execute(values, inverse);
    }

    /** Independent mutable work lanes; one workspace must not be used concurrently. */
    public final class Workspace {
        private final int[] left = new int[size()];
        private final int[] right = new int[size()];
        private Workspace() {}
        public int capacity() { return size(); }

        /**
         * Linear convolution modulo MODULUS. Signed inputs are normalized. Returns result length;
         * output suffix is unchanged. Output may alias either input. Invalid capacity writes nothing.
         */
        public int convolve(int[] a, int[] b, int[] output) {
            Objects.requireNonNull(a, "a"); Objects.requireNonNull(b, "b"); Objects.requireNonNull(output, "output");
            if (a.length == 0 || b.length == 0) { return 0; }
            long required = (long) a.length + b.length - 1;
            if (required > size() || required > output.length) { throw new IllegalArgumentException("convolution capacity exhausted"); }
            Arrays.fill(left, 0); Arrays.fill(right, 0);
            for (int i = 0; i < a.length; i++) { left[i] = Math.floorMod(a[i], MODULUS); }
            for (int i = 0; i < b.length; i++) { right[i] = Math.floorMod(b[i], MODULUS); }
            execute(left, false); execute(right, false);
            for (int i = 0; i < size(); i++) { left[i] = multiply(left[i], right[i]); }
            execute(left, true); System.arraycopy(left, 0, output, 0, (int) required);
            return (int) required;
        }
    }

    private void execute(int[] values, boolean inverse) {
        for (int i = 0; i < size(); i++) {
            int j = reverse[i];
            if (i < j) { int swap = values[i]; values[i] = values[j]; values[j] = swap; }
        }
        int[] powers = inverse ? inverseRoots : roots;
        for (int half = 1; half < size(); half <<= 1) {
            for (int start = 0; start < size(); start += half * 2) {
                for (int i = 0; i < half; i++) {
                    int u = values[start + i], v = multiply(values[start + half + i], powers[half + i]);
                    int sum = u + v, difference = u - v;
                    values[start + i] = sum >= MODULUS ? sum - MODULUS : sum;
                    values[start + half + i] = difference < 0 ? difference + MODULUS : difference;
                }
            }
        }
        if (inverse) { for (int i = 0; i < size(); i++) { values[i] = multiply(values[i], inverseSize); } }
    }
    private static int multiply(int a, int b) { return (int) ((long) a * b % MODULUS); }
    private static int power(int value, int exponent) {
        int result = 1;
        while (exponent != 0) {
            if ((exponent & 1) != 0) { result = multiply(result, value); }
            value = multiply(value, value); exponent >>>= 1;
        }
        return result;
    }
}
