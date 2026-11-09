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
package jdk.internal.m3.collections.mat;

/**
 * Seven SI base-dimension exponents packed into one long (one signed byte per axis).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatSiDimension.java}
 * blob {@code bf31ae771c899a78012f0c6695aeaf9a4e9f2211} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatSiDimension {
    public static final int TIME_AXIS = 0;
    public static final int LENGTH_AXIS = 1;
    public static final int MASS_AXIS = 2;
    public static final int CURRENT_AXIS = 3;
    public static final int TEMPERATURE_AXIS = 4;
    public static final int AMOUNT_AXIS = 5;
    public static final int LUMINOUS_INTENSITY_AXIS = 6;
    public static final int AXES = 7;

    public static final long DIMENSIONLESS = pack(0, 0, 0, 0, 0, 0, 0);
    public static final long TIME = pack(1, 0, 0, 0, 0, 0, 0);
    public static final long LENGTH = pack(0, 1, 0, 0, 0, 0, 0);
    public static final long MASS = pack(0, 0, 1, 0, 0, 0, 0);
    public static final long CURRENT = pack(0, 0, 0, 1, 0, 0, 0);
    public static final long TEMPERATURE = pack(0, 0, 0, 0, 1, 0, 0);
    public static final long AMOUNT = pack(0, 0, 0, 0, 0, 1, 0);
    public static final long LUMINOUS_INTENSITY = pack(0, 0, 0, 0, 0, 0, 1);

    private MatSiDimension() { }

    public static long pack(
            int time,
            int length,
            int mass,
            int current,
            int temperature,
            int amount,
            int luminousIntensity) {
        int[] values = {time, length, mass, current, temperature, amount, luminousIntensity};
        long packed = 0L;
        for (int axis = 0; axis < values.length; axis++) {
            int exponent = requireExponent(values[axis]);
            packed |= ((long) exponent & 0xffL) << (axis * 8);
        }
        return packed;
    }

    public static int exponent(long dimension, int axis) {
        if (axis < 0 || axis >= AXES) {
            throw new IndexOutOfBoundsException("axis=" + axis);
        }
        return (byte) (dimension >>> (axis * 8));
    }

    public static long multiply(long left, long right) {
        return combine(left, right, 1);
    }

    public static long divide(long left, long right) {
        return combine(left, right, -1);
    }

    public static long pow(long dimension, int power) {
        int[] values = new int[AXES];
        for (int axis = 0; axis < AXES; axis++) {
            values[axis] = requireExponent(Math.multiplyExact(exponent(dimension, axis), power));
        }
        return pack(values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
    }

    private static long combine(long left, long right, int direction) {
        int[] values = new int[AXES];
        for (int axis = 0; axis < AXES; axis++) {
            int delta = Math.multiplyExact(exponent(right, axis), direction);
            values[axis] = requireExponent(Math.addExact(exponent(left, axis), delta));
        }
        return pack(values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
    }

    private static int requireExponent(int exponent) {
        if (exponent < -127 || exponent > 127) {
            throw new IllegalArgumentException("SI dimension exponent outside signed-byte range");
        }
        return exponent;
    }
}
