/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/*
 * @test
 * @summary Known-count reference selection preserves bounds and rejects inconsistent masks
 * @modules java.base/java.util
 * @compile --patch-module java.base=${test.src} CollectionMaskValuesCountTest.java
 * @run main/othervm --patch-module java.base=${test.classes} java.util.CollectionMaskValuesCountTest
 * @run main/othervm -Xcheck:jni -Djdk.m3.collectionMasks.native=true --patch-module java.base=${test.classes} java.util.CollectionMaskValuesCountTest --jdk-native
 */
package java.util;

/** New internal helper's explicit count/range/mutation refusal cases. */
public final class CollectionMaskValuesCountTest {
    private CollectionMaskValuesCountTest() { }
    private static int checks;
    public static void main(String[] args) {
        if (args.length != 0 && !"--jdk-native".equals(args[0])) System.load(args[0]);
        for (int length : new int[]{0, 1, 63, 64, 65, 127, 128, 129, 255, 256, 257, 1025}) {
            Object[] values = new Object[length];
            for (int i = 0; i < length; i++) values[i] = i % 7 == 0 ? null : new Object();
            Object[] before = values.clone();
            long[] words = new long[(length + 63) >>> 6];
            Arrays.fill(words, -1L); // Padding bits must not select nonexistent values.
            Object[] result = M3CollectionMasks.selectedValues(words, values, length);
            check(result != values && Arrays.equals(before, result), "full detached values");
            check(Arrays.equals(before, values), "input unchanged");
            if (length != 0) {
                expect(ConcurrentModificationException.class,
                        () -> M3CollectionMasks.selectedValues(words, values, length - 1));
                Arrays.fill(words, 0L);
                expect(ConcurrentModificationException.class,
                        () -> M3CollectionMasks.selectedValues(words, values, 1));
            }
        }
        expect(NullPointerException.class, () -> M3CollectionMasks.selectedValues(null, new Object[0], 0));
        expect(NullPointerException.class, () -> M3CollectionMasks.selectedValues(new long[0], null, 0));
        expect(IndexOutOfBoundsException.class,
                () -> M3CollectionMasks.selectedValues(new long[0], new Object[1], 0));
        expect(IllegalArgumentException.class,
                () -> M3CollectionMasks.selectedValues(new long[1], new Object[1], -1));
        expect(IllegalArgumentException.class,
                () -> M3CollectionMasks.selectedValues(new long[1], new Object[1], 2));
        System.out.println("PASS CollectionMaskValuesCountTest checks=" + checks);
    }
    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }
    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong exception " + kind, failure);
        }
        throw new AssertionError("missing " + kind);
    }
}
