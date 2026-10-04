/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package jdk.internal.mindex.function;

import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/** Allocation-conscious range, iterate, scan and fold constructs for primitive functional plans. */
public final class MIndexFunctionalSupport {
    private MIndexFunctionalSupport() {}

    public static int range(int startInclusive, int endExclusive, int step, int[] output) {
        Objects.requireNonNull(output);
        if (step == 0) throw new IllegalArgumentException("zero step");
        int count = 0;
        if (step > 0) {
            for (int value = startInclusive; value < endExclusive; value = Math.addExact(value, step)) {
                if (count == output.length) throw new IllegalArgumentException("output capacity exhausted");
                output[count++] = value;
                if (value > Integer.MAX_VALUE - step) break;
            }
        } else {
            for (int value = startInclusive; value > endExclusive; value = Math.addExact(value, step)) {
                if (count == output.length) throw new IllegalArgumentException("output capacity exhausted");
                output[count++] = value;
                if (value < Integer.MIN_VALUE - step) break;
            }
        }
        return count;
    }

    public static int iterate(int seed, IntUnaryOperator next, int count, int[] output) {
        Objects.requireNonNull(next);
        Objects.requireNonNull(output);
        if (count < 0 || count > output.length) throw new IllegalArgumentException("invalid count");
        int value = seed;
        for (int index = 0; index < count; index++) {
            output[index] = value;
            value = next.applyAsInt(value);
        }
        return count;
    }

    public static int iterate(long seed, LongUnaryOperator next, int count, long[] output) {
        Objects.requireNonNull(next);
        Objects.requireNonNull(output);
        if (count < 0 || count > output.length) throw new IllegalArgumentException("invalid count");
        long value = seed;
        for (int index = 0; index < count; index++) {
            output[index] = value;
            value = next.applyAsLong(value);
        }
        return count;
    }

    public static int iterate(double seed, DoubleUnaryOperator next, int count, double[] output) {
        Objects.requireNonNull(next);
        Objects.requireNonNull(output);
        if (count < 0 || count > output.length) throw new IllegalArgumentException("invalid count");
        double value = seed;
        for (int index = 0; index < count; index++) {
            output[index] = value;
            value = next.applyAsDouble(value);
        }
        return count;
    }

    public static void scan(int[] input, int identity, IntBinaryOperator operator, int[] output) {
        Objects.requireNonNull(input); Objects.requireNonNull(operator); Objects.requireNonNull(output);
        if (output.length < input.length) throw new IllegalArgumentException("output shorter than input");
        int value = identity;
        for (int index = 0; index < input.length; index++) {
            value = operator.applyAsInt(value, input[index]);
            output[index] = value;
        }
    }

    public static long fold(long[] input, long identity, LongBinaryOperator operator) {
        Objects.requireNonNull(input); Objects.requireNonNull(operator);
        long value = identity;
        for (long element : input) value = operator.applyAsLong(value, element);
        return value;
    }

    public static double fold(double[] input, double identity, DoubleBinaryOperator operator) {
        Objects.requireNonNull(input); Objects.requireNonNull(operator);
        double value = identity;
        for (double element : input) value = operator.applyAsDouble(value, element);
        return value;
    }
}
