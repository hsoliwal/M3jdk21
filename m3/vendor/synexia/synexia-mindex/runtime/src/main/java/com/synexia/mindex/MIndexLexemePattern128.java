// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Immutable masked query over one {@link MIndexLexemeCodec} coordinate space.
 *
 * <p>Each constrained lexeme position occupies one 0xff mask byte in the same two-long geometry
 * as {@link MIndexLexemeKey128}. Unconstrained positions have a zero mask byte. Matching is
 * therefore two masked XOR operations plus an optional exact-length comparison.</p>
 */
public final class MIndexLexemePattern128 {
    public static final int ANY_LENGTH = -1;

    private final long codecFingerprint64;
    private final long highValue;
    private final long lowValue;
    private final long highMask;
    private final long lowMask;
    private final int exactLength;
    private final int constraintCount;

    private MIndexLexemePattern128(
            long codecFingerprint64,
            long highValue,
            long lowValue,
            long highMask,
            long lowMask,
            int exactLength) {
        if (exactLength < ANY_LENGTH || exactLength > MIndexLexemeKey128.MAX_SYMBOLS) {
            throw new IllegalArgumentException("invalid exact length " + exactLength);
        }
        this.codecFingerprint64 = codecFingerprint64;
        this.highValue = highValue;
        this.lowValue = lowValue;
        this.highMask = highMask;
        this.lowMask = lowMask;
        this.exactLength = exactLength;
        this.constraintCount =
                (Long.bitCount(highMask) + Long.bitCount(lowMask)) >>> 3;
        validateMask(highMask);
        validateMask(lowMask);
        if ((highValue & ~highMask) != 0L || (lowValue & ~lowMask) != 0L) {
            throw new IllegalArgumentException("pattern value outside mask");
        }
        if (exactLength >= 0) {
            for (int position = exactLength;
                    position < MIndexLexemeKey128.MAX_SYMBOLS;
                    position++) {
                if (constrains(position)) {
                    throw new IllegalArgumentException(
                            "constraint outside exact length at position " + position);
                }
            }
        }
    }

    public static Builder builder(MIndexLexemeCodec codec) {
        return new Builder(Objects.requireNonNull(codec, "codec"));
    }

    /**
     * Compile an exact-length template where {@code wildcard} marks unconstrained positions.
     */
    public static MIndexLexemePattern128 template(
            CharSequence template, char wildcard, MIndexLexemeCodec codec) {
        Objects.requireNonNull(template, "template");
        if (template.length() > MIndexLexemeKey128.MAX_SYMBOLS) {
            throw new IllegalArgumentException("template exceeds packed lexeme length");
        }
        Builder builder = builder(codec).exactLength(template.length());
        for (int position = 0; position < template.length(); position++) {
            char unit = template.charAt(position);
            if (unit != wildcard) builder.at(position, unit);
        }
        return builder.build();
    }

    /** Compile a prefix constraint with any packed length at least as large as the prefix. */
    public static MIndexLexemePattern128 prefix(
            CharSequence prefix, MIndexLexemeCodec codec) {
        Objects.requireNonNull(prefix, "prefix");
        if (prefix.length() > MIndexLexemeKey128.MAX_SYMBOLS) {
            throw new IllegalArgumentException("prefix exceeds packed lexeme length");
        }
        Builder builder = builder(codec);
        for (int position = 0; position < prefix.length(); position++) {
            builder.at(position, prefix.charAt(position));
        }
        return builder.build();
    }

    /**
     * Compile a suffix constraint for one known total packed length.
     *
     * <p>Suffix positions are absolute packed coordinates, so total length is explicit rather than
     * inferred at match time.</p>
     */
    public static MIndexLexemePattern128 suffix(
            CharSequence suffix, int totalLength, MIndexLexemeCodec codec) {
        Objects.requireNonNull(suffix, "suffix");
        if (totalLength < 0 || totalLength > MIndexLexemeKey128.MAX_SYMBOLS) {
            throw new IllegalArgumentException("invalid total length " + totalLength);
        }
        if (suffix.length() > totalLength) {
            throw new IllegalArgumentException("suffix exceeds total length");
        }
        Builder builder = builder(codec).exactLength(totalLength);
        int start = totalLength - suffix.length();
        for (int index = 0; index < suffix.length(); index++) {
            builder.at(start + index, suffix.charAt(index));
        }
        return builder.build();
    }

    /** Compile one exact spelling as a fully constrained pattern. */
    public static MIndexLexemePattern128 exact(
            CharSequence value, MIndexLexemeCodec codec) {
        Objects.requireNonNull(value, "value");
        Builder builder = builder(codec).exactLength(value.length());
        for (int position = 0; position < value.length(); position++) {
            builder.at(position, value.charAt(position));
        }
        return builder.build();
    }

    public long codecFingerprint64() {
        return codecFingerprint64;
    }

    public int exactLength() {
        return exactLength;
    }

    public boolean hasExactLength() {
        return exactLength >= 0;
    }

    public int constraintCount() {
        return constraintCount;
    }

    public boolean constrains(int position) {
        Objects.checkIndex(position, MIndexLexemeKey128.MAX_SYMBOLS);
        return (maskByte(position) & 0xffL) != 0L;
    }

    /** Returns the required codec symbol, or zero for an unconstrained position. */
    public int symbolConstraintAt(int position) {
        Objects.checkIndex(position, MIndexLexemeKey128.MAX_SYMBOLS);
        if (!constrains(position)) return 0;
        return (int) valueByte(position);
    }

    public boolean matches(MIndexLexemeKey128 key) {
        Objects.requireNonNull(key, "key");
        return matches(key.high(), key.low());
    }

    boolean matches(long high, long low) {
        if (exactLength >= 0 && packedLength(high, low) != exactLength) return false;
        return ((high ^ highValue) & highMask) == 0L
                && ((low ^ lowValue) & lowMask) == 0L;
    }

    private long maskByte(int position) {
        int shift = shift(position);
        long lane = position < 8 ? highMask : lowMask;
        return (lane >>> shift) & 0xffL;
    }

    private long valueByte(int position) {
        int shift = shift(position);
        long lane = position < 8 ? highValue : lowValue;
        return (lane >>> shift) & 0xffL;
    }

    private static int shift(int position) {
        int lanePosition = position < 8 ? position : position - 8;
        return 56 - lanePosition * 8;
    }

    private static int packedLength(long high, long low) {
        if (high == 0L) return 0;
        if (low == 0L) return 8 - Long.numberOfTrailingZeros(high) / 8;
        return 16 - Long.numberOfTrailingZeros(low) / 8;
    }

    private static void validateMask(long mask) {
        for (int byteIndex = 0; byteIndex < Long.BYTES; byteIndex++) {
            int shift = byteIndex * 8;
            int value = (int) ((mask >>> shift) & 0xffL);
            if (value != 0 && value != 0xff) {
                throw new IllegalArgumentException("partial-byte lexeme mask");
            }
        }
    }

    public static final class Builder {
        private final MIndexLexemeCodec codec;
        private long highValue;
        private long lowValue;
        private long highMask;
        private long lowMask;
        private int exactLength = ANY_LENGTH;

        private Builder(MIndexLexemeCodec codec) {
            this.codec = codec;
        }

        public Builder anyLength() {
            exactLength = ANY_LENGTH;
            return this;
        }

        public Builder exactLength(int length) {
            if (length < 0 || length > MIndexLexemeKey128.MAX_SYMBOLS) {
                throw new IllegalArgumentException("invalid exact length " + length);
            }
            for (int position = length;
                    position < MIndexLexemeKey128.MAX_SYMBOLS;
                    position++) {
                if (isConstrained(position)) {
                    throw new IllegalArgumentException(
                            "existing constraint outside exact length at position " + position);
                }
            }
            exactLength = length;
            return this;
        }

        public Builder at(int position, char unit) {
            Objects.checkIndex(position, MIndexLexemeKey128.MAX_SYMBOLS);
            if (exactLength >= 0 && position >= exactLength) {
                throw new IllegalArgumentException(
                        "constraint outside exact length at position " + position);
            }
            int symbol = codec.symbol(unit);
            if (symbol == 0) {
                throw new IllegalArgumentException(
                        "UTF-16 unit is outside codec: U+"
                                + String.format("%04X", (int) unit));
            }
            long mask = 0xffL << shift(position);
            long value = (long) symbol << shift(position);
            if (position < 8) {
                highMask |= mask;
                highValue = (highValue & ~mask) | value;
            } else {
                lowMask |= mask;
                lowValue = (lowValue & ~mask) | value;
            }
            return this;
        }

        public Builder clear(int position) {
            Objects.checkIndex(position, MIndexLexemeKey128.MAX_SYMBOLS);
            long mask = 0xffL << shift(position);
            if (position < 8) {
                highMask &= ~mask;
                highValue &= ~mask;
            } else {
                lowMask &= ~mask;
                lowValue &= ~mask;
            }
            return this;
        }

        public MIndexLexemePattern128 build() {
            return new MIndexLexemePattern128(
                    codec.fingerprint64(),
                    highValue,
                    lowValue,
                    highMask,
                    lowMask,
                    exactLength);
        }

        private boolean isConstrained(int position) {
            long mask = 0xffL << shift(position);
            return position < 8
                    ? (highMask & mask) != 0L
                    : (lowMask & mask) != 0L;
        }
    }
}
