// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/** Exact composable Java String metadata algebra for frozen lexical coordinates. */
public final class TextMetadata {
    private static final int ROW_WIDTH = 5;

    /** Identity summary. No text payload or canonical spelling is retained. */
    public static final Summary EMPTY = new Summary(0, 0, 0, '\0', '\0');

    private TextMetadata() {}

    public static int pow31(int utf16Length) {
        if (utf16Length < 0) throw new IllegalArgumentException("utf16Length");
        int result = 1;
        int base = 31;
        int exponent = utf16Length;
        while (exponent != 0) {
            if ((exponent & 1) != 0) result *= base;
            base *= base;
            exponent >>>= 1;
        }
        return result;
    }

    public static int concatHash(int leftHash, int rightHash, int rightUtf16Length) {
        return leftHash * pow31(rightUtf16Length) + rightHash;
    }

    /**
     * Scalar facts about one logical UTF-16 sequence, not a content-identity proof.
     *
     * <p>Endpoints permit surrogate pairing across joins, including joins separated by empty
     * pieces. Hash arithmetic wraps like String.hashCode; lengths and counts never wrap.
     * The constructor checks shape, not that a payload exists with the supplied facts.
     */
    public record Summary(
            int utf16Length, int javaHash, int codePointLength, char first, char last) {
        public Summary {
            if (utf16Length < 0 || codePointLength < 0 || codePointLength > utf16Length
                    || codePointLength < utf16Length / 2 + utf16Length % 2) {
                throw new IllegalArgumentException("invalid UTF-16 summary counts");
            }
            if (utf16Length == 0
                    && (javaHash != 0 || first != 0 || last != 0)) {
                throw new IllegalArgumentException("nonidentity empty summary");
            }
            if (utf16Length == 1 && (first != last || javaHash != first)) {
                throw new IllegalArgumentException("invalid single-unit summary");
            }
        }
    }

    /**
     * Read exact logical units without toString, subSequence, encoding, or whole-spelling admission.
     * The caller must keep source stable for the call. Arbitrary CharSequence access cost remains
     * the source's cost; this method makes one charAt call per selected UTF-16 unit.
     */
    public static Summary summarize(CharSequence source, int start, int end) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromToIndex(start, end, source.length());
        if (start == end) return EMPTY;
        int hash = 0;
        int points = 0;
        char first = 0;
        char previous = 0;
        for (int index = start; index < end; index++) {
            char unit = source.charAt(index);
            if (index == start) first = unit;
            hash = 31 * hash + unit;
            points++;
            if (index != start && Character.isHighSurrogate(previous)
                    && Character.isLowSurrogate(unit)) {
                points--;
            }
            previous = unit;
        }
        return new Summary(end - start, hash, points, first, previous);
    }

    /** Combine facts in text order. The cost is O(log right.utf16Length()), with no payload reads. */
    public static Summary concat(Summary left, Summary right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.utf16Length() == 0) return right;
        if (right.utf16Length() == 0) return left;
        int length = Math.addExact(left.utf16Length(), right.utf16Length());
        int points = Math.addExact(left.codePointLength(), right.codePointLength());
        if (Character.isHighSurrogate(left.last()) && Character.isLowSurrogate(right.first())) {
            points--;
        }
        return new Summary(length,
                concatHash(left.javaHash(), right.javaHash(), right.utf16Length()),
                points, left.first(), right.last());
    }

    /** Deterministic Java batch oracle; snapshots references, never spelling payloads. */
    public static Summary concatAll(Summary... pieces) {
        Summary[] snapshot = Objects.requireNonNull(pieces, "pieces").clone();
        // Validate the complete row set before aggregation or native admission.
        for (Summary piece : snapshot) Objects.requireNonNull(piece, "piece");
        Summary result = EMPTY;
        for (Summary piece : snapshot) result = concat(result, piece);
        return result;
    }

    /**
     * Explicit JNI batch path over five-int metadata rows, not text payloads.
     *
     * <p>The caller first initializes the existing MIndexNativeOps library owner. This method
     * never loads another library, changes automatic dispatch, or silently falls back. An older
     * or absent library reports UnsatisfiedLinkError. Use concatAll for the Java path.
     * Native code retains no reference or pointer after the synchronous call.
     */
    public static Summary concatNativeRequired(Summary... pieces) {
        Summary[] snapshot = Objects.requireNonNull(pieces, "pieces").clone();
        for (Summary piece : snapshot) Objects.requireNonNull(piece, "piece");
        int[] rows = new int[Math.multiplyExact(snapshot.length, ROW_WIDTH)];
        int offset = 0;
        for (Summary piece : snapshot) {
            rows[offset++] = piece.utf16Length();
            rows[offset++] = piece.javaHash();
            rows[offset++] = piece.codePointLength();
            rows[offset++] = piece.first();
            rows[offset++] = piece.last();
        }
        int[] result = nativeConcat(rows);
        if (result == null || result.length != ROW_WIDTH
                || result[3] < 0 || result[3] > Character.MAX_VALUE
                || result[4] < 0 || result[4] > Character.MAX_VALUE) {
            throw new IllegalStateException("invalid native summary result");
        }
        return new Summary(result[0], result[1], result[2], (char) result[3], (char) result[4]);
    }

    /** Package-private ABI seam also exercised by malformed-row regression tests. */
    static native int[] nativeConcat(int[] rows);
}
