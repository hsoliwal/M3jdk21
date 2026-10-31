/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, see https://www.gnu.org/licenses/.
 */

/*
 * @test
 * @bug 8312164
 * @summary Preserve five Arrays.hashCode contracts after Java21 source adaptation
 * @run main/othervm HashCode8312164
 * @run main/othervm -Xint HashCode8312164
 * @run main/othervm -XX:+CompactStrings HashCode8312164
 * @run main/othervm -XX:-CompactStrings HashCode8312164
 */

import java.util.Arrays;
import java.util.Objects;
import java.util.SplittableRandom;

public class HashCode8312164 {
    private static long checks;

    private static void check(int observed, int expected, String label) {
        if (observed != expected) {
            throw new AssertionError(label + " expected=" + expected + " observed=" + observed);
        }
        checks++;
    }

    private static int longReference(long[] values) {
        if (values == null) return 0;
        int result = 1;
        for (long value : values) result = 31 * result + Long.hashCode(value);
        return result;
    }

    private static int booleanReference(boolean[] values) {
        if (values == null) return 0;
        int result = 1;
        for (boolean value : values) result = 31 * result + Boolean.hashCode(value);
        return result;
    }

    private static int floatReference(float[] values) {
        if (values == null) return 0;
        int result = 1;
        for (float value : values) result = 31 * result + Float.hashCode(value);
        return result;
    }

    private static int doubleReference(double[] values) {
        if (values == null) return 0;
        int result = 1;
        for (double value : values) result = 31 * result + Double.hashCode(value);
        return result;
    }

    private static int objectReference(Object[] values) {
        if (values == null) return 0;
        int result = 1;
        for (Object value : values) result = 31 * result + Objects.hashCode(value);
        return result;
    }

    private static final class Probe {
        private final int id;
        private final StringBuilder trace;

        private Probe(int id, StringBuilder trace) {
            this.id = id;
            this.trace = trace;
        }

        @Override
        public int hashCode() {
            trace.append(id).append(',');
            return id * 31;
        }
    }

    public static void main(String[] args) {
        check(Arrays.hashCode((long[]) null), 0, "null long");
        check(Arrays.hashCode((boolean[]) null), 0, "null boolean");
        check(Arrays.hashCode((float[]) null), 0, "null float");
        check(Arrays.hashCode((double[]) null), 0, "null double");
        check(Arrays.hashCode((Object[]) null), 0, "null object");

        check(Arrays.hashCode(new long[0]), 1, "empty long");
        check(Arrays.hashCode(new boolean[0]), 1, "empty boolean");
        check(Arrays.hashCode(new float[0]), 1, "empty float");
        check(Arrays.hashCode(new double[0]), 1, "empty double");
        check(Arrays.hashCode(new Object[0]), 1, "empty object");

        check(Arrays.hashCode(new float[]{Float.NaN, 0.0f, -0.0f, Float.POSITIVE_INFINITY}),
                floatReference(new float[]{Float.intBitsToFloat(0x7f800001), 0.0f, -0.0f, Float.POSITIVE_INFINITY}),
                "float NaN, signed zero and infinity");
        check(Arrays.hashCode(new double[]{Double.NaN, 0.0d, -0.0d, Double.NEGATIVE_INFINITY}),
                doubleReference(new double[]{Double.longBitsToDouble(0x7ff0000000000001L), 0.0d, -0.0d,
                        Double.NEGATIVE_INFINITY}), "double NaN, signed zero and infinity");
        check(Arrays.hashCode(new long[]{Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L}),
                longReference(new long[]{Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L}), "long extremes");

        SplittableRandom random = new SplittableRandom(0x8312164L);
        for (int iteration = 0; iteration < 6000; iteration++) {
            int length = iteration % 131;
            long[] longs = new long[length];
            boolean[] booleans = new boolean[length];
            float[] floats = new float[length];
            double[] doubles = new double[length];
            Object[] objects = new Object[length];
            for (int j = 0; j < length; j++) {
                longs[j] = random.nextLong();
                booleans[j] = random.nextBoolean();
                floats[j] = Float.intBitsToFloat(random.nextInt());
                doubles[j] = Double.longBitsToDouble(random.nextLong());
                objects[j] = random.nextBoolean() ? null : Integer.valueOf(random.nextInt());
            }
            check(Arrays.hashCode(longs), longReference(longs), "long randomized");
            check(Arrays.hashCode(booleans), booleanReference(booleans), "boolean randomized");
            check(Arrays.hashCode(floats), floatReference(floats), "float randomized");
            check(Arrays.hashCode(doubles), doubleReference(doubles), "double randomized");
            check(Arrays.hashCode(objects), objectReference(objects), "object randomized");
        }

        StringBuilder referenceTrace = new StringBuilder();
        StringBuilder observedTrace = new StringBuilder();
        Object[] reference = {new Probe(1, referenceTrace), null,
                new Probe(2, referenceTrace), new Probe(1, referenceTrace)};
        Object[] observed = {new Probe(1, observedTrace), null,
                new Probe(2, observedTrace), new Probe(1, observedTrace)};
        check(Arrays.hashCode(observed), objectReference(reference), "object callback result");
        if (!observedTrace.toString().equals(referenceTrace.toString())) {
            throw new AssertionError("callback order mismatch: " + observedTrace + " vs " + referenceTrace);
        }
        if (!"1,2,1,".contentEquals(observedTrace)) {
            throw new AssertionError("incorrect null/occurrence callback trace: " + observedTrace);
        }
        checks++;
        System.out.println("M3_ARRAYS_HASH_8312164_PASS checks=" + checks);
    }
}
