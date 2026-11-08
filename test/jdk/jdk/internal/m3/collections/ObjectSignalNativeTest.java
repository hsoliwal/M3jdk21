/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Kernel parity of the object-signal plane pass (leaf object-signal-native-20261107): the dispatching
 * ObjectSignalOps.applyFeatureMasks against the Java kernel applyFeatureMasksJava over random and edge planes and
 * masks, below and above the 256-word JNI threshold. Without libm3signalorder on java.library.path both sides run the
 * Java kernel (the default build); with it (full-java-jni-20261018/verify.py native mode, native-gate.sh) the
 * dispatching side runs the native kernel for 256 words and more. Under m3.collections.all.native.required the library
 * must be loaded.
 */
//FLLDCIM: TOP - native vs Java kernel parity of ObjectSignalOps; recipe m3/collections/qualification/object-signal-native-20261107
final class ObjectSignalNativeTest {
    private static final int[] WORDS = {0, 1, 2, 255, 256, 257, 300, 1024};
    private static final long[] EDGE = {0L, -1L, 1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x5555_5555_5555_5555L,
        0xAAAA_AAAA_AAAA_AAAAL};

    @Test void nativeKernelLoadedWhenRequired() {
        Assumptions.assumeTrue(Boolean.getBoolean("m3.collections.all.native.required"),
            "native mode only (m3.collections.all.native.required)");
        assertTrue(ObjectSignalOps.available(), "libm3signalorder must provide applyFeatureMasksNative");
    }

    @Test void dispatchedKernelEqualsJavaKernel() {
        Random random = new Random(20261107L);
        int cases = 0;
        for (int words : WORDS) {
            for (int round = 0; round < 24; round++) {
                long[] planes = new long[64 * words];
                for (int i = 0; i < planes.length; i++) {
                    planes[i] = round % 3 == 0 ? EDGE[random.nextInt(EDGE.length)] : random.nextLong();
                }
                long[] initial = new long[words];
                for (int i = 0; i < words; i++) initial[i] = random.nextBoolean() ? -1L : random.nextLong();
                long required = mask(random);
                long forbidden = mask(random) & ~required;
                long[] dispatched = initial.clone();
                long[] java = initial.clone();
                ObjectSignalOps.applyFeatureMasks(planes, words, required, forbidden, dispatched);
                ObjectSignalOps.applyFeatureMasksJava(planes, words, required, forbidden, java);
                assertArrayEquals(java, dispatched, "words=" + words + " round=" + round);
                cases++;
            }
        }
        assertTrue(cases == WORDS.length * 24);
    }

    private static long mask(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> EDGE[random.nextInt(EDGE.length)];
            case 1 -> 1L << random.nextInt(64);
            case 2 -> random.nextLong() & random.nextLong();
            default -> random.nextLong();
        };
    }
}
