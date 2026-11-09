// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class IntNttPlanTest {
    @Test void convolutionMatchesQuadraticModularArithmetic() {
        Random random = new Random(0x4e5454434f4e56L);
        IntNttPlan plan = new IntNttPlan(128);
        var workspace = plan.workspace();
        for (int trial = 0; trial < 1500; trial++) {
            int[] a = new int[random.nextInt(64)], b = new int[random.nextInt(64)];
            for (int i = 0; i < a.length; i++) { a[i] = random.nextInt(); }
            for (int i = 0; i < b.length; i++) { b[i] = random.nextInt(); }
            int[] savedA = a.clone(), savedB = b.clone();
            int[] expected = naive(a, b), output = new int[130]; Arrays.fill(output, -12345);
            assertEquals(expected.length, workspace.convolve(a, b, output));
            assertArrayEquals(expected, Arrays.copyOf(output, expected.length), "trial=" + trial);
            for (int i = expected.length; i < output.length; i++) { assertEquals(-12345, output[i]); }
            assertArrayEquals(savedA, a); assertArrayEquals(savedB, b);
        }
    }

    @Test void transformsRoundTripEveryTestedPowerOfTwo() {
        Random random = new Random(0x4e5454524f554e44L);
        for (int size = 1; size <= 1 << 16; size <<= 1) {
            IntNttPlan plan = new IntNttPlan(size);
            int[] values = new int[size];
            for (int i = 0; i < size; i++) { values[i] = random.nextInt(IntNttPlan.MODULUS); }
            int[] original = values.clone();
            plan.transform(values, false); plan.transform(values, true); assertArrayEquals(original, values);
        }
        IntNttPlan four = new IntNttPlan(4);
        int[] impulse = {1, 0, 0, 0}; four.transform(impulse, false); assertArrayEquals(new int[]{1, 1, 1, 1}, impulse);
    }

    @Test void aliasesExtremesEmptyInputsAndIndependentWorkspaces() {
        IntNttPlan plan = new IntNttPlan(8);
        var first = plan.workspace(); var second = plan.workspace();
        assertEquals(8, plan.size()); assertEquals(8, first.capacity());
        int[] a = {Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE}; int[] b = {Integer.MIN_VALUE};
        int[] expected = naive(a, b); assertEquals(4, first.convolve(a, b, a)); assertArrayEquals(expected, a);
        int[] c = {Integer.MAX_VALUE}; int[] d = {4, -2, Integer.MIN_VALUE};
        expected = naive(c, d); assertEquals(3, second.convolve(c, d, d)); assertArrayEquals(expected, d);
        int[] same = {-3}; assertEquals(1, first.convolve(same, same, same)); assertEquals(9, same[0]);
        int[] untouched = {11, 12}; assertEquals(0, first.convolve(new int[0], new int[100], untouched));
        assertArrayEquals(new int[]{11, 12}, untouched);
        assertEquals(0, second.convolve(new int[100], new int[0], new int[0]));
        int[] one = {Integer.MIN_VALUE};
        assertEquals(1, new IntNttPlan(1).workspace().convolve(one, new int[]{1}, one));
        assertEquals(Math.floorMod(Integer.MIN_VALUE, IntNttPlan.MODULUS), one[0]);
    }

    @Test void validationCompletesBeforeCallerDataIsChanged() {
        for (int size : new int[]{-1, 0, 3, 6, IntNttPlan.MAX_SIZE + 1, 1 << 24}) {
            assertThrows(IllegalArgumentException.class, () -> new IntNttPlan(size));
        }
        IntNttPlan plan = new IntNttPlan(4);
        int[] invalid = {1, 2, 3, IntNttPlan.MODULUS}, original = invalid.clone();
        assertThrows(IllegalArgumentException.class, () -> plan.transform(invalid, false)); assertArrayEquals(original, invalid);
        invalid[3] = -1; original = invalid.clone();
        assertThrows(IllegalArgumentException.class, () -> plan.transform(invalid, true)); assertArrayEquals(original, invalid);
        assertThrows(IllegalArgumentException.class, () -> plan.transform(new int[2], false));
        assertThrows(NullPointerException.class, () -> plan.transform(null, true));
        var workspace = plan.workspace(); int[] output = {13, 14};
        assertThrows(IllegalArgumentException.class, () -> workspace.convolve(new int[3], new int[3], output));
        assertArrayEquals(new int[]{13, 14}, output);
        assertThrows(IllegalArgumentException.class, () -> workspace.convolve(new int[3], new int[]{1}, output));
        assertArrayEquals(new int[]{13, 14}, output);
        assertThrows(NullPointerException.class, () -> workspace.convolve(new int[0], new int[0], null));
        assertEquals(2, workspace.convolve(new int[]{1, 2}, new int[]{3}, output)); assertArrayEquals(new int[]{3, 6}, output);
    }

    private static int[] naive(int[] a, int[] b) {
        if (a.length == 0 || b.length == 0) { return new int[0]; }
        int[] result = new int[a.length + b.length - 1];
        long modulus = IntNttPlan.MODULUS;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < b.length; j++) {
                long product = Math.floorMod((long) a[i], modulus) * Math.floorMod((long) b[j], modulus);
                result[i + j] = (int) ((result[i + j] + product) % modulus);
            }
        }
        return result;
    }
}
