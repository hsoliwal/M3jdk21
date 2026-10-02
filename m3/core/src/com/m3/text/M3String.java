/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable stock-JVM M3 String view over canonical local atoms and retained ranges.
 *
 * <p>{@link #fromString(String)} weakly canonicalizes equal live UTF-16 values without retaining
 * the caller String. Concatenation and slicing retain existing immutable payloads and allocate only
 * descriptor metadata. Equality and {@link #hashCode()} are exact over UTF-16 code units and do not
 * depend on segmentation. {@link #asString()} and {@link #toString()} are explicit materialization
 * boundaries for APIs that require an ordinary {@code java.lang.String}.</p>
 *
 * <p>This Route A type deliberately does not claim {@code String.intern()} identity, shared mapped
 * lexicon backing, or transparent JVM substitution. Those are separate integration gates.</p>
 */
public final class M3String implements CharSequence, Comparable<M3String> {
    private static final LocalM3Interner LOCAL = new LocalM3Interner();

    private final M3StringPiece piece;
    private final int hash;

    private M3String(M3StringPiece piece, int hash) {
        this.piece = Objects.requireNonNull(piece, "piece");
        this.hash = hash;
    }

    static M3String fromCanonicalPiece(LocalM3StringPiece piece, int hash) {
        return new M3String(piece, hash);
    }

    /** Canonical VM-local admission of exact UTF-16 String content. */
    public static M3String fromString(String value) {
        return LOCAL.intern(Objects.requireNonNull(value, "value"));
    }

    public static M3String valueOf(String value) {
        return fromString(value);
    }

    /**
     * Composes immutable payloads without allocating a joined character/byte payload.
     * Java String hash is composed from precomputed child hashes and powers of 31.
     */
    public static M3String join(M3String... values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) return fromString("");
        if (values.length == 1) return Objects.requireNonNull(values[0], "values[0]");
        M3StringPiece[] pieces = new M3StringPiece[values.length];
        int length = 0;
        int combinedHash = 0;
        for (int index = 0; index < values.length; index++) {
            M3String value = Objects.requireNonNull(values[index], "values[" + index + "]");
            pieces[index] = value.piece;
            length = Math.addExact(length, value.length());
            combinedHash = combinedHash * power31(value.length()) + value.hash;
        }
        M3StringPiece joined = M3StringPiece.join(pieces);
        if (joined.length() != length) throw new IllegalStateException("join length mismatch");
        return new M3String(joined, combinedHash);
    }

    public M3String concat(M3String other) {
        Objects.requireNonNull(other, "other");
        if (other.isEmpty()) return this;
        if (isEmpty()) return other;
        return join(this, other);
    }

    @Override
    public int length() {
        return piece.length();
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    @Override
    public char charAt(int index) {
        return piece.charAt(index);
    }

    @Override
    public M3String subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length());
        if (start == 0 && end == length()) return this;
        if (start == end) return fromString("");
        M3StringPiece range = piece.subSequence(start, end);
        return new M3String(range, javaHash(range));
    }

    public M3String substring(int beginIndex) {
        return subSequence(beginIndex, length());
    }

    public M3String substring(int beginIndex, int endIndex) {
        return subSequence(beginIndex, endIndex);
    }

    public int codePointAt(int index) {
        Objects.checkIndex(index, length());
        return Character.codePointAt(this, index);
    }

    public int codePointBefore(int index) {
        if (index < 1 || index > length()) throw new IndexOutOfBoundsException(index);
        return Character.codePointBefore(this, index);
    }

    public int codePointCount(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        return Character.codePointCount(this, beginIndex, endIndex);
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        if (index < 0 || index > length()) throw new IndexOutOfBoundsException(index);
        return Character.offsetByCodePoints(this, index, codePointOffset);
    }

    public void getChars(int srcBegin, int srcEnd, char[] destination, int destinationBegin) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromToIndex(srcBegin, srcEnd, length());
        Objects.checkFromIndexSize(destinationBegin, srcEnd - srcBegin, destination.length);
        for (int source = srcBegin, target = destinationBegin;
                source < srcEnd;
                source++, target++) {
            destination[target] = charAt(source);
        }
    }

    /** Returns independent writable storage. */
    public char[] toCharArray() {
        char[] result = new char[length()];
        getChars(0, length(), result, 0);
        return result;
    }

    /** Exact UTF-16 content comparison without materializing either side. */
    public boolean contentEquals(CharSequence other) {
        if (other == null || length() != other.length()) return false;
        for (int index = 0; index < length(); index++) {
            if (charAt(index) != other.charAt(index)) return false;
        }
        return true;
    }

    public boolean startsWith(M3String prefix) {
        return startsWith(prefix, 0);
    }

    public boolean startsWith(M3String prefix, int offset) {
        Objects.requireNonNull(prefix, "prefix");
        if (offset < 0 || offset > length() - prefix.length()) return false;
        for (int index = 0; index < prefix.length(); index++) {
            if (charAt(offset + index) != prefix.charAt(index)) return false;
        }
        return true;
    }

    public boolean endsWith(M3String suffix) {
        Objects.requireNonNull(suffix, "suffix");
        return startsWith(suffix, length() - suffix.length());
    }

    public boolean contains(M3String value) {
        return indexOf(value) >= 0;
    }

    public int indexOf(M3String value) {
        return indexOf(value, 0);
    }

    /** KMP exact UTF-16 search; state crosses retained storage seams without flattening. */
    public int indexOf(M3String needle, int fromIndex) {
        Objects.requireNonNull(needle, "needle");
        int start = Math.max(fromIndex, 0);
        if (needle.isEmpty()) return Math.min(start, length());
        if (start >= length() || needle.length() > length() - start) return -1;

        int[] failure = new int[needle.length()];
        for (int index = 1, matched = 0; index < needle.length(); index++) {
            char current = needle.charAt(index);
            while (matched > 0 && current != needle.charAt(matched)) {
                matched = failure[matched - 1];
            }
            if (current == needle.charAt(matched)) matched++;
            failure[index] = matched;
        }
        for (int index = start, matched = 0; index < length(); index++) {
            char current = charAt(index);
            while (matched > 0 && current != needle.charAt(matched)) {
                matched = failure[matched - 1];
            }
            if (current == needle.charAt(matched)) matched++;
            if (matched == needle.length()) return index - needle.length() + 1;
        }
        return -1;
    }

    public int lastIndexOf(M3String needle) {
        Objects.requireNonNull(needle, "needle");
        if (needle.isEmpty()) return length();
        if (needle.length() > length()) return -1;
        for (int start = length() - needle.length(); start >= 0; start--) {
            if (startsWith(needle, start)) return start;
        }
        return -1;
    }

    /** JDK regex operates directly on this CharSequence; captures may materialize String results. */
    public boolean matches(Pattern pattern) {
        return Objects.requireNonNull(pattern, "pattern").matcher(this).matches();
    }

    public String[] split(Pattern pattern, int limit) {
        return Objects.requireNonNull(pattern, "pattern").split(this, limit);
    }

    /** Explicit contiguous ordinary-String materialization boundary. */
    public String asString() {
        return piece.flatten();
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof M3String value
                && hash == value.hash
                && contentEquals(value);
    }

    @Override
    public int compareTo(M3String other) {
        Objects.requireNonNull(other, "other");
        int limit = Math.min(length(), other.length());
        for (int index = 0; index < limit; index++) {
            char left = charAt(index);
            char right = other.charAt(index);
            if (left != right) return left - right;
        }
        return length() - other.length();
    }

    /** JDK-defined case-insensitive semantics; currently an explicit materialization boundary. */
    public boolean equalsIgnoreCase(M3String other) {
        return other != null && asString().equalsIgnoreCase(other.asString());
    }

    /** JDK-defined case-insensitive semantics; currently an explicit materialization boundary. */
    public int compareToIgnoreCase(M3String other) {
        return asString().compareToIgnoreCase(Objects.requireNonNull(other, "other").asString());
    }

    private static int javaHash(CharSequence value) {
        int result = 0;
        for (int index = 0; index < value.length(); index++) {
            result = 31 * result + value.charAt(index);
        }
        return result;
    }

    private static int power31(int exponent) {
        int result = 1;
        int base = 31;
        for (int value = exponent; value != 0; value >>>= 1) {
            if ((value & 1) != 0) result *= base;
            base *= base;
        }
        return result;
    }
}
