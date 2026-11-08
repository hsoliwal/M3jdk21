// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Arrays;
import java.util.Random;

/** Dependency-free differential test, also invoked by the JUnit adapter. */
public final class IntCodebookSelfTestMain {
    private IntCodebookSelfTestMain() {}

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void throwsAs(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); } catch (Throwable got) {
            if (expected.isInstance(got)) return;
            throw new AssertionError("expected " + expected + ", got " + got, got);
        }
        throw new AssertionError("missing " + expected);
    }

    private static void verify(int[] values) {
        int[] reference = values.clone();
        IntCodebookArray encoded = IntCodebookArray.of(values);
        require(encoded.length() == values.length, "length");
        require(Arrays.equals(encoded.toIntArray(), reference), "java round-trip");
        require(encoded.payloadBytes() <= (long) values.length * Integer.BYTES, "never larger payload");
        for (int i = 0; i < values.length; i++)
            require(encoded.get(i) == reference[i], "random access " + i);
        throwsAs(IndexOutOfBoundsException.class, () -> encoded.get(-1));
        throwsAs(IndexOutOfBoundsException.class, () -> encoded.get(values.length));
        if (encoded.dictionaryEncoded()) {
            require(encoded.dictionarySize() > 0, "nonempty codebook");
            require(encoded.bitsPerIndex() >= 0 && encoded.bitsPerIndex() <= 15, "index width");
            byte[] detached = encoded.packedIndicesCopy();
            int[] dictionary = encoded.dictionaryCopy();
            Arrays.fill(detached, (byte) -1);
            Arrays.fill(dictionary, 99);
            require(Arrays.equals(encoded.toIntArray(), reference), "defensive copies");
            if (System.getProperty("m3.arrays.native.path") != null && values.length <= 4096 && encoded.packedIndicesCopy().length <= 4096 && encoded.dictionarySize() <= 4096)
                require(Arrays.equals(encoded.toIntArrayNativeRequired(), reference), "JNI round-trip");
        } else {
            require(encoded.dictionarySize() == 0, "raw shape");
            throwsAs(IllegalStateException.class, encoded::toIntArrayNativeRequired);
        }
        if (values.length > 0) {
            values[0] ^= -1;
            require(encoded.get(0) == reference[0], "snapshot");
        }
    }

    public static void main(String[] args) {
        verify(new int[0]);
        verify(new int[] {Integer.MIN_VALUE});
        verify(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE});
        verify(new int[] {-17, -17, -17, -17});
        verify(new int[] {3, 1, 3, 1, 3, 1, 3, 1, 3});
        Random random = new Random(0xC0DEB00CL);
        int cases = 5;
        for (int n = 2; n <= 260; n++) {
            for (int distribution = 0; distribution < 7; distribution++) {
                int[] values = new int[n];
                for (int i = 0; i < n; i++) {
                    values[i] = switch (distribution) {
                        case 0 -> 0;
                        case 1 -> i & 1;
                        case 2 -> Integer.MIN_VALUE + (i & 3);
                        case 3 -> Integer.MAX_VALUE - (i % 13);
                        case 4 -> random.nextInt(256) - 128;
                        case 5 -> random.nextInt();
                        default -> i * 8191;
                    };
                }
                verify(values);
                cases++;
            }
        }
        // Wide dictionary cardinality and stress on primitive-table resizing.
        int[] wide = new int[4096];
        for (int i = 0; i < wide.length; i++) wide[i] = i & 1023;
        verify(wide); cases++;
        throwsAs(NullPointerException.class, () -> IntCodebookArray.of(null));
        throwsAs(IllegalArgumentException.class,
                () -> IntCodebookNative.decodeRequired(new byte[] {0}, new int[] {1}, 2, 0));
        throwsAs(IllegalArgumentException.class,
                () -> IntCodebookNative.decodeRequired(new byte[0], new int[0], 0, 0));
        if (System.getProperty("m3.arrays.native.path") != null) {
            throwsAs(IllegalArgumentException.class,
                    () -> IntCodebookNative.decodeRequired(new byte[] {3}, new int[] {10, 20, 30}, 1, 2));
            throwsAs(IllegalArgumentException.class,
                    () -> IntCodebookNative.decodeRequired(new byte[] {(byte) 0x81}, new int[] {10, 20}, 1, 1));
        }
        System.out.println("M3_INT_CODEBOOK_DIFFERENTIAL_PASS cases=" + cases);
    }
}
