// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Single-segment indexed text retaining lexical/semantic/relation/source metadata.
 *
 * <p>This is the compact companion to {@link MIndexCompositeString}: one canonical content ID plus
 * one metadata value. The canonical text remains owned by {@link MIndexString}; annotation never
 * mutates or duplicates the interned spelling.</p>
 */
public final class MIndexAnnotatedString implements MIndexText {
    private final MIndexString value;
    private final MIndexStringMetadata metadata;

    private MIndexAnnotatedString(MIndexString value, MIndexStringMetadata metadata) {
        this.value = Objects.requireNonNull(value, "value");
        this.metadata = metadata == null ? MIndexStringMetadata.NONE : metadata;
    }

    public static MIndexAnnotatedString of(
            MIndexString value, MIndexStringMetadata metadata) {
        return new MIndexAnnotatedString(value, metadata);
    }

    public static MIndexAnnotatedString of(
            CharSequence value, MIndexStringMetadata metadata) {
        return of(MIndexString.from(Objects.requireNonNull(value, "value")), metadata);
    }

    public MIndexStringMetadata metadata() { return metadata; }
    public int flags() { return metadata.flags(); }
    public long lexicalBits() { return metadata.lexicalBits(); }
    public long semanticBits() { return metadata.semanticBits(); }
    public long relationBits() { return metadata.relationBits(); }
    public int sourceStart() { return metadata.sourceStart(); }
    public int sourceEnd() { return metadata.sourceEnd(); }

    @Override public MIndexString materialize() { return value; }
    @Override public int segmentCount() { return 1; }

    @Override
    public MIndexString segment(int index) {
        Objects.checkIndex(index, 1);
        return value;
    }

    @Override
    public MIndexStringMetadata segmentMetadata(int index) {
        Objects.checkIndex(index, 1);
        return metadata;
    }

    @Override public int length() { return value.length(); }
    @Override public char charAt(int index) { return value.charAt(index); }

    @Override
    public CharSequence subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length());
        return value.substring(start, end);
    }

    public boolean contentEquals(CharSequence other) {
        Objects.requireNonNull(other, "other");
        if (other.length() != value.length()) return false;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != other.charAt(index)) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexAnnotatedString annotated
                && value.equals(annotated.value)
                && metadata.equals(annotated.metadata);
    }

    @Override public int hashCode() {
        return 31 * value.hashCode() + metadata.hashCode();
    }

    @Override public String toString() { return value.toString(); }
}
