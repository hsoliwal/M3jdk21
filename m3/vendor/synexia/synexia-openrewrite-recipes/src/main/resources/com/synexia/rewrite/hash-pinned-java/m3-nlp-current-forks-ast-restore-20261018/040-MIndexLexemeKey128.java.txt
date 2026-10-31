// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Collision-free 128-bit coordinate for at most sixteen symbols from one
 * {@link MIndexLexemeCodec}.
 *
 * <p>Each symbol occupies one byte. Symbol zero is reserved for end/padding, therefore the first
 * eight positions occupy {@code high} from most-significant byte to least-significant byte and
 * positions eight through fifteen occupy {@code low} in the same order. The codec is external
 * immutable metadata and is not repeated in each key.</p>
 */
public record MIndexLexemeKey128(long high, long low)
        implements Comparable<MIndexLexemeKey128> {
    public static final int MAX_SYMBOLS = 16;

    public MIndexLexemeKey128 {
        boolean padding = false;
        for (int index = 0; index < MAX_SYMBOLS; index++) {
            int symbol = rawSymbol(high, low, index);
            if (symbol == 0) {
                padding = true;
            } else if (padding) {
                throw new IllegalArgumentException("non-canonical lexeme key: symbol after padding");
            }
        }
    }

    public static MIndexLexemeKey128 encode(
            CharSequence value, MIndexLexemeCodec codec) {
        return Objects.requireNonNull(codec, "codec")
                .encode(Objects.requireNonNull(value, "value"));
    }

    /** Number of non-zero packed symbols. */
    public int length() {
        if (high == 0L) return 0;
        if (low == 0L) {
            return 8 - Long.numberOfTrailingZeros(high) / 8;
        }
        return 16 - Long.numberOfTrailingZeros(low) / 8;
    }

    public int symbolAt(int index) {
        Objects.checkIndex(index, length());
        return rawSymbol(high, low, index);
    }

    public char charAt(int index, MIndexLexemeCodec codec) {
        return Objects.requireNonNull(codec, "codec").unit(symbolAt(index));
    }

    /**
     * Dense first-two-symbol coordinate used as the fixed 65,536-cell dispatch dimension.
     */
    public int prefixBucket() {
        return (int) ((high >>> 48) & 0xffffL);
    }

    /** Number of equal leading packed symbols. */
    public int commonPrefixSymbols(MIndexLexemeKey128 other) {
        Objects.requireNonNull(other, "other");
        long highDifference = high ^ other.high;
        int common;
        if (highDifference != 0L) {
            common = Long.numberOfLeadingZeros(highDifference) >>> 3;
        } else {
            long lowDifference = low ^ other.low;
            common = lowDifference == 0L
                    ? MAX_SYMBOLS
                    : 8 + (Long.numberOfLeadingZeros(lowDifference) >>> 3);
        }
        return Math.min(common, Math.min(length(), other.length()));
    }

    public boolean startsWith(MIndexLexemeKey128 prefix) {
        Objects.requireNonNull(prefix, "prefix");
        int prefixLength = prefix.length();
        return prefixLength <= length() && commonPrefixSymbols(prefix) == prefixLength;
    }

    @Override
    public int compareTo(MIndexLexemeKey128 other) {
        Objects.requireNonNull(other, "other");
        int highOrder = Long.compareUnsigned(high, other.high);
        return highOrder != 0 ? highOrder : Long.compareUnsigned(low, other.low);
    }

    public String toHexCoordinate() {
        return String.format("%016x:%016x", high, low);
    }

    private static int rawSymbol(long high, long low, int index) {
        if (index < 8) return (int) ((high >>> (56 - index * 8)) & 0xffL);
        return (int) ((low >>> (56 - (index - 8) * 8)) & 0xffL);
    }
}
