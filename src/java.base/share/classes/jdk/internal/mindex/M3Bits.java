// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

/** Shared zero-object bit-address arithmetic for packed primitive bit lanes. */
final class M3Bits {
    private M3Bits() { }

    static int wordIndex(int bitIndex) {
        return bitIndex >>> 6;
    }

    static long bitMask(int bitIndex) {
        return 1L << (bitIndex & 63);
    }

    static int wordsForBits(int bitCount) {
        return (int) ((((long) bitCount) + 63L) >>> 6);
    }

    static int bitCapacity(int wordCount) {
        return wordCount << 6;
    }
}
