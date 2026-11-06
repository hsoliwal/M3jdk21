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

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Checked batch packing for signed and unsigned fields of one through 64 bits.
 * Values occupy consecutive LSB-first spans in caller-owned word arrays.
 * Native execution is optional; callers must keep inputs stable during a call.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/M3PackedNative.java}
 * blob {@code 006fab23ef1b6c9fdfaba5d6eb8ff6b931f22738} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PackedNative {
    private M3PackedNative() { }

    /** Returns whether the optional JNI library can be loaded in this JVM. */
    public static boolean nativeAvailable() {
        return NativeLibrary.AVAILABLE;
    }

    /**
     * Packs an immutable-for-the-call input into a new, canonical word array.
     * Width 64 accepts all long bit patterns, including unsigned negative carriers.
     * All range checks precede output allocation; no input element is modified.
     *
     * @throws NullPointerException when values is null
     * @throws IllegalArgumentException for invalid width, dimensions or values
     */
    public static long[] pack(long[] values, int bitsPerValue, boolean signed,
                              boolean preferNative) {
        Objects.requireNonNull(values, "values");
        int wordCount = M3PrimitiveBits.packedWordCount(values.length, bitsPerValue);
        Arrays.stream(values).forEach(value -> M3PrimitiveBits.checkValue(value, bitsPerValue, signed));
        return preferNative && nativeAvailable()
                ? packNative(values, bitsPerValue, signed)
                : packJava(values, bitsPerValue, wordCount);
    }

    /**
     * Unpacks exactly count values from canonical words into a new array.
     * Signed fields are sign-extended; unsigned width 64 retains raw long bits.
     *
     * @throws NullPointerException when words is null
     * @throws IllegalArgumentException for invalid width/count, word count or tail
     */
    public static long[] unpack(long[] words, int count, int bitsPerValue,
                                boolean signed, boolean preferNative) {
        Objects.requireNonNull(words, "words");
        validatePacked(words, count, bitsPerValue);
        return preferNative && nativeAvailable()
                ? unpackNative(words, count, bitsPerValue, signed)
                : unpackJava(words, count, bitsPerValue, signed);
    }

    private static long[] packJava(long[] values, int width, int wordCount) {
        if (width == Long.SIZE) return values.clone();
        long[] words = new long[wordCount];
        long mask = M3PrimitiveBits.mask(width);
        IntStream.range(0, values.length).forEach(index -> M3PrimitiveBits.writePacked(
                words, (long) index * width, width, values[index] & mask));
        return words;
    }

    private static long[] unpackJava(long[] words, int count, int width, boolean signed) {
        if (width == Long.SIZE) return words.clone();
        return IntStream.range(0, count).mapToLong(index -> {
            long raw = M3PrimitiveBits.readPacked(words, (long) index * width, width);
            return signed ? M3PrimitiveBits.signExtend(raw, width) : raw;
        }).toArray();
    }

    private static void validatePacked(long[] words, int count, int width) {
        int expected = M3PrimitiveBits.packedWordCount(count, width);
        if (words.length != expected) {
            throw new IllegalArgumentException("noncanonical packed word count");
        }
        int tailBits = (int) (((long) count * width) & 63);
        if (tailBits != 0 && (words[words.length - 1] & ~M3PrimitiveBits.mask(tailBits)) != 0) {
            throw new IllegalArgumentException("nonzero packed tail bits");
        }
    }

    private static native long[] packNative(long[] values, int bitsPerValue, boolean signed);

    private static native long[] unpackNative(long[] words, int count, int bitsPerValue,
                                             boolean signed);

    private static final class NativeLibrary {
        private static final boolean AVAILABLE = load();

        private NativeLibrary() { }

        private static boolean load() {
            try {
                System.loadLibrary("synexia_packed_integer");
                return true;
            } catch (UnsatisfiedLinkError unavailable) {
                return false;
            }
        }
    }
}
