// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Immutable UTF-16 range view over one canonical {@link MIndexString}.
 *
 * <p>The view carries only its parent and two offsets. It does not admit another pooled spelling
 * until {@link #intern()} or a caller explicitly materializes {@link #toString()}. This is the
 * value used by the sliding-window scanner so intermediate windows stay index/offset based.</p>
 */
public final class SubMIndexString implements CharSequence, Comparable<SubMIndexString> {
    private final MIndexString parent;
    private final int start;
    private final int end;

    private SubMIndexString(MIndexString parent, int start, int end) {
        this.parent = parent;
        this.start = start;
        this.end = end;
    }

    /**
     * Historical code-point-safe view factory. Retained for compatibility.
     *
     * <p>Use {@link #ofUtf16(MIndexString, int, int)} when the required contract is
     * java.lang.String UTF-16 substring coordinate compatibility.</p>
     */
    public static SubMIndexString of(MIndexString parent, int start, int end) {
        return ofCodePointSafe(parent, start, end);
    }

    /** Exact UTF-16 code-unit range; surrogate pairs may be split exactly like String.substring. */
    public static SubMIndexString ofUtf16(MIndexString parent, int start, int end) {
        Objects.requireNonNull(parent, "parent");
        Objects.checkFromToIndex(start, end, parent.length());
        return new SubMIndexString(parent, start, end);
    }

    /** Explicit code-point-safe range that rejects boundaries inside a surrogate pair. */
    public static SubMIndexString ofCodePointSafe(MIndexString parent, int start, int end) {
        Objects.requireNonNull(parent, "parent");
        Objects.checkFromToIndex(start, end, parent.length());
        if (splitsSurrogate(parent, start) || splitsSurrogate(parent, end)) {
            throw new IllegalArgumentException("sub-view cannot split a surrogate pair");
        }
        return new SubMIndexString(parent, start, end);
    }

    public MIndexString parent() {
        return parent;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    @Override
    public int length() {
        return end - start;
    }

    public int codePointCount() {
        return parent.toString().codePointCount(start, end);
    }

    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, length());
        return parent.charAt(start + index);
    }

    @Override
    public SubMIndexString subSequence(int subStart, int subEnd) {
        Objects.checkFromToIndex(subStart, subEnd, length());
        return of(parent, start + subStart, start + subEnd);
    }

    public boolean startsWith(CharSequence prefix) {
        Objects.requireNonNull(prefix, "prefix");
        if (prefix.length() > length()) return false;
        return regionMatches(0, prefix, 0, prefix.length());
    }

    public boolean endsWith(CharSequence suffix) {
        Objects.requireNonNull(suffix, "suffix");
        if (suffix.length() > length()) return false;
        return regionMatches(length() - suffix.length(), suffix, 0, suffix.length());
    }

    public boolean regionMatches(int offset, CharSequence other, int otherOffset, int count) {
        Objects.requireNonNull(other, "other");
        Objects.checkFromIndexSize(offset, count, length());
        Objects.checkFromIndexSize(otherOffset, count, other.length());
        String source = parent.toString();
        for (int index = 0; index < count; index++) {
            if (source.charAt(start + offset + index) != other.charAt(otherOffset + index)) {
                return false;
            }
        }
        return true;
    }

    public MIndexString intern() {
        return parent.substring(start, end);
    }

    public long contentHash64() {
        return MIndexDictionary.hash64(toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public int compareTo(SubMIndexString other) {
        Objects.requireNonNull(other, "other");
        return toString().compareTo(other.toString());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SubMIndexString value) || length() != value.length()) return false;
        String left = parent.toString();
        String right = value.parent.toString();
        return left.regionMatches(start, right, value.start, length());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        String value = parent.toString();
        for (int index = start; index < end; index++) hash = 31 * hash + value.charAt(index);
        return hash;
    }

    @Override
    public String toString() {
        return parent.toString().substring(start, end);
    }

    private static boolean splitsSurrogate(MIndexString parent, int offset) {
        return offset > 0 && offset < parent.length()
                && Character.isHighSurrogate(parent.charAt(offset - 1))
                && Character.isLowSurrogate(parent.charAt(offset));
    }
}

