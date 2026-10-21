// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable composite indexed string.
 *
 * <p>Text identity is represented by canonical MIndexString content IDs while segment metadata
 * remains in parallel primitive lanes. Concatenation therefore preserves lexical/semantic/relation
 * and source metadata without creating an intermediate Java String.</p>
 */
public final class MIndexCompositeString implements MIndexText {
    /** A view of one canonical backing segment, retained for range oriented callers. */
    public record Segment(MIndexString parent, int start, int end) {
        public Segment {
            Objects.requireNonNull(parent, "parent");
            Objects.checkFromToIndex(start, end, parent.length());
            if (splitsSurrogate(parent, start) || splitsSurrogate(parent, end)) {
                throw new IllegalArgumentException("segment cannot split a surrogate pair");
            }
        }

        public int length() { return end - start; }
        public SubMIndexString view() { return SubMIndexString.of(parent, start, end); }

        private static boolean splitsSurrogate(MIndexString value, int offset) {
            return offset > 0 && offset < value.length()
                    && Character.isHighSurrogate(value.charAt(offset - 1))
                    && Character.isLowSurrogate(value.charAt(offset));
        }
    }

    private final int[] contentIds;
    private final int[] segmentStarts;
    private final int[] segmentEnds;
    private final int[] utf16Ends;
    private final int[] flags;
    private final long[] lexicalBits;
    private final long[] semanticBits;
    private final long[] relationBits;
    private final int[] sourceStarts;
    private final int[] sourceEnds;
    private final int length;
    private final int javaHash;
    private final int aggregateFlags;
    private final long aggregateSemanticBits;
    private final long aggregateRelationBits;
    private volatile MIndexString canonical;
    private volatile MIndexStringMetadata analyzedMetadata;

    private MIndexCompositeString(Builder builder) {
        contentIds = Arrays.copyOf(builder.contentIds, builder.size);
        segmentStarts = Arrays.copyOf(builder.segmentStarts, builder.size);
        segmentEnds = Arrays.copyOf(builder.segmentEnds, builder.size);
        utf16Ends = Arrays.copyOf(builder.utf16Ends, builder.size);
        flags = Arrays.copyOf(builder.flags, builder.size);
        lexicalBits = Arrays.copyOf(builder.lexicalBits, builder.size);
        semanticBits = Arrays.copyOf(builder.semanticBits, builder.size);
        relationBits = Arrays.copyOf(builder.relationBits, builder.size);
        sourceStarts = Arrays.copyOf(builder.sourceStarts, builder.size);
        sourceEnds = Arrays.copyOf(builder.sourceEnds, builder.size);
        length = builder.length;
        javaHash = builder.javaHash;
        aggregateFlags = builder.aggregateFlags;
        aggregateSemanticBits = builder.aggregateSemanticBits;
        aggregateRelationBits = builder.aggregateRelationBits;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MIndexCompositeString of(MIndexString... values) {
        Objects.requireNonNull(values, "values");
        Builder builder = builder();
        for (MIndexString value : values) builder.add(value);
        return builder.build();
    }

    public static MIndexCompositeString of(CharSequence... values) {
        Objects.requireNonNull(values, "values");
        Builder builder = builder();
        for (CharSequence value : values) builder.append(value);
        return builder.build();
    }

    @Override public int length() { return length; }

    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, length);
        // Empty segments duplicate cumulative ends. Select the first end strictly past index,
        // so a zero-length segment can never be chosen as the character owner.
        int low = 0;
        int high = utf16Ends.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (utf16Ends[middle] <= index) low = middle + 1;
            else high = middle;
        }
        int segment = low;
        int start = segment == 0 ? 0 : utf16Ends[segment - 1];
        // A character read must not cross the explicit segment() materialization boundary.
        return parentSegment(segment).charAt(segmentStarts[segment] + index - start);
    }

    @Override
    public MIndexString subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length);
        validateBoundary(start);
        validateBoundary(end);
        if (start == 0 && end == length) return materialize();
        return MIndexString.literal(materialize().toString().substring(start, end));
    }

    /**
     * Views any UTF-16 range without joining or interning its backing segments.
     *
     * <p>Unlike {@link #subSequence(int, int)}, this accepts offsets inside surrogate pairs so a
     * regex capture can retain the exact UTF-16 range returned by a JDK matcher.</p>
     */
    public SubMIndexCompositeString view(int start, int end) {
        return new SubMIndexCompositeString(this, start, end);
    }

    @Override public int segmentCount() { return contentIds.length; }

    @Override
    public MIndexString segment(int index) {
        int checked = Objects.checkIndex(index, contentIds.length);
        MIndexString parent = parentSegment(checked);
        int start = segmentStarts[checked];
        int end = segmentEnds[checked];
        return start == 0 && end == parent.length()
                ? parent
                : parent.substring(start, end);
    }

    /** Range view of one segment; {@link #segment(int)} is the canonical compatibility boundary. */
    public Segment sourceSegment(int index) {
        int checked = Objects.checkIndex(index, contentIds.length);
        return new Segment(
                parentSegment(checked),
                segmentStarts[checked],
                segmentEnds[checked]);
    }

    public SubMIndexString segmentView(int index) { return sourceSegment(index).view(); }

    /** Exact UTF-16 view; unlike sourceSegment this may split a surrogate pair. */
    public SubMIndexString segmentViewUtf16(int index) {
        int checked = Objects.checkIndex(index, contentIds.length);
        return SubMIndexString.ofUtf16(
                parentSegment(checked),
                segmentStarts[checked],
                segmentEnds[checked]);
    }

    MIndexString parentSegment(int index) {
        return MIndexString.fromContentIndex(
                contentIds[Objects.checkIndex(index, contentIds.length)]);
    }

    int segmentStart(int index) {
        return segmentStarts[Objects.checkIndex(index, contentIds.length)];
    }

    int segmentEnd(int index) {
        return segmentEnds[Objects.checkIndex(index, contentIds.length)];
    }

    public int flags(int segment) {
        return flags[Objects.checkIndex(segment, segmentCount())];
    }

    public long lexicalBits(int segment) {
        return lexicalBits[Objects.checkIndex(segment, segmentCount())];
    }

    public long semanticBits(int segment) {
        return semanticBits[Objects.checkIndex(segment, segmentCount())];
    }

    public long relationBits(int segment) {
        return relationBits[Objects.checkIndex(segment, segmentCount())];
    }

    public int sourceStart(int segment) {
        return sourceStarts[Objects.checkIndex(segment, segmentCount())];
    }

    public int sourceEnd(int segment) {
        return sourceEnds[Objects.checkIndex(segment, segmentCount())];
    }

    public MIndexStringMetadata metadata(int segment) {
        int checked = Objects.checkIndex(segment, segmentCount());
        return new MIndexStringMetadata(
                flags[checked],
                lexicalBits[checked],
                semanticBits[checked],
                relationBits[checked],
                sourceStarts[checked],
                sourceEnds[checked]);
    }

    @Override
    public MIndexStringMetadata segmentMetadata(int segment) {
        return metadata(segment);
    }

    public int aggregateFlags() { return aggregateFlags; }
    public long aggregateSemanticBits() { return aggregateSemanticBits; }
    public long aggregateRelationBits() { return aggregateRelationBits; }

    public MIndexCompositeString sliceSegments(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, segmentCount());
        Builder builder = builder();
        for (int index = fromInclusive; index < toExclusive; index++) {
            builder.add(segment(index), metadata(index));
        }
        return builder.build();
    }

    public MIndexCompositeString concat(MIndexCompositeString other) {
        Objects.requireNonNull(other, "other");
        Builder builder = builder();
        for (int index = 0; index < segmentCount(); index++) {
            builder.add(segment(index), metadata(index));
        }
        for (int index = 0; index < other.segmentCount(); index++) {
            builder.add(other.segment(index), other.metadata(index));
        }
        return builder.build();
    }

    @Override
    public MIndexString materialize() {
        MIndexString result = canonical;
        if (result == null) {
            if (allWholeSegments()) {
                int id = MIndexStringPool.GLOBAL.concat(contentIds);
                result = MIndexStringPool.GLOBAL.canonical(id);
            } else {
                result = new MIndexString(toCharArray());
            }
            canonical = result;
        }
        return result;
    }

    /** Explicit caller-owned mutable UTF-16 materialization boundary. */
    public char[] toCharArray() {
        char[] result = new char[length];
        copyChars(0, result, 0, length);
        return result;
    }

    /** Copy a logical UTF-16 range without joining backing segments. */
    public void copyChars(int sourceStart, char[] target, int targetStart, int count) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(sourceStart, count, length);
        Objects.checkFromIndexSize(targetStart, count, target.length);
        if (count == 0) return;

        int absolute = sourceStart;
        int segment = segmentAt(absolute);
        int copied = 0;
        while (copied < count) {
            int logicalStart = segment == 0 ? 0 : utf16Ends[segment - 1];
            int available = utf16Ends[segment] - absolute;
            int chunk = Math.min(count - copied, available);
            MIndexString parent = parentSegment(segment);
            int parentOffset = segmentStarts[segment] + absolute - logicalStart;
            parent.copyChars(parentOffset, target, targetStart + copied, chunk);
            copied += chunk;
            absolute += chunk;
            if (absolute == utf16Ends[segment]) segment++;
        }
    }

    /** Whole-string materialization remains explicit for callers that need one canonical ID. */
    public MIndexString intern() { return materialize(); }

    public MIndexStringMetadata metadata() {
        MIndexStringMetadata result = analyzedMetadata;
        if (result == null) {
            result = MIndexStringMetadata.analyze(intern());
            analyzedMetadata = result;
        }
        return result;
    }

    public int codePointCount() { return metadata().codePointLength(); }
    public long contentHash64() { return metadata().contentHash64(); }
    public long rollingHash64() { return metadata().rollingHash64(); }

    public boolean startsWith(CharSequence prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return prefix.length() <= length && regionMatches(0, prefix, 0, prefix.length());
    }

    public boolean endsWith(CharSequence suffix) {
        Objects.requireNonNull(suffix, "suffix");
        return suffix.length() <= length
                && regionMatches(length - suffix.length(), suffix, 0, suffix.length());
    }

    public boolean regionMatches(int offset, CharSequence other, int otherOffset, int count) {
        Objects.requireNonNull(other, "other");
        Objects.checkFromIndexSize(offset, count, length);
        Objects.checkFromIndexSize(otherOffset, count, other.length());
        for (int index = 0; index < count; index++) {
            if (charAt(offset + index) != other.charAt(otherOffset + index)) return false;
        }
        return true;
    }

    public int compareTo(MIndexCompositeString other) {
        Objects.requireNonNull(other, "other");
        int count = Math.min(length, other.length);
        for (int index = 0; index < count; index++) {
            int comparison = Character.compare(charAt(index), other.charAt(index));
            if (comparison != 0) return comparison;
        }
        return Integer.compare(length, other.length);
    }

    private boolean allWholeSegments() {
        for (int index = 0; index < contentIds.length; index++) {
            MIndexString parent = parentSegment(index);
            if (segmentStarts[index] != 0 || segmentEnds[index] != parent.length()) return false;
        }
        return true;
    }

    private int segmentAt(int logicalIndex) {
        int low = 0;
        int high = utf16Ends.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (utf16Ends[middle] <= logicalIndex) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private void validateBoundary(int boundary) {
        if (boundary <= 0 || boundary >= length) return;
        if (Character.isHighSurrogate(charAt(boundary - 1))
                && Character.isLowSurrogate(charAt(boundary))) {
            throw new IllegalArgumentException("subsequence cannot split a surrogate pair");
        }
    }

    @Override public int hashCode() { return javaHash; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MIndexCompositeString composite)
                || composite.length != length
                || composite.javaHash != javaHash) {
            return false;
        }
        for (int index = 0; index < length; index++) {
            if (charAt(index) != composite.charAt(index)) return false;
        }
        return true;
    }

    /** Content comparison across any CharSequence without changing equals/hashCode symmetry. */
    public boolean contentEquals(CharSequence other) {
        Objects.requireNonNull(other, "other");
        if (other.length() != length) return false;
        for (int index = 0; index < length; index++) {
            if (charAt(index) != other.charAt(index)) return false;
        }
        return true;
    }

    @Override public String toString() { return materialize().toString(); }

    public static final class Builder {
        private int[] contentIds = new int[8];
        private int[] segmentStarts = new int[8];
        private int[] segmentEnds = new int[8];
        private int[] utf16Ends = new int[8];
        private int[] flags = new int[8];
        private long[] lexicalBits = new long[8];
        private long[] semanticBits = new long[8];
        private long[] relationBits = new long[8];
        private int[] sourceStarts = new int[8];
        private int[] sourceEnds = new int[8];
        private int size;
        private int length;
        private int javaHash;
        private int aggregateFlags;
        private long aggregateSemanticBits;
        private long aggregateRelationBits;
        private boolean built;

        private Builder() {
            Arrays.fill(sourceStarts, -1);
            Arrays.fill(sourceEnds, -1);
        }

        public Builder add(MIndexString value) {
            return add(value, MIndexStringMetadata.NONE);
        }

        public Builder add(MIndexString value, MIndexStringMetadata metadata) {
            MIndexString checked = Objects.requireNonNull(value, "value");
            return addRange(checked, 0, checked.length(), metadata);
        }

        public Builder append(CharSequence value) {
            requireOpen();
            Objects.requireNonNull(value, "value");
            if (value instanceof MIndexString indexed) return append(indexed);
            if (value instanceof MIndexAnnotatedString annotated) {
                return add(annotated.materialize(), annotated.metadata());
            }
            if (value instanceof SubMIndexString view) return append(view);
            if (value instanceof MIndexCompositeString composite) {
                for (int index = 0; index < composite.segmentCount(); index++) {
                    add(composite.segment(index), composite.metadata(index));
                }
                return this;
            }
            return append(MIndexString.literal(value.toString()));
        }

        public Builder append(MIndexString value) {
            requireOpen();
            Objects.requireNonNull(value, "value");
            return value.isEmpty() ? this : add(value);
        }

        public Builder append(SubMIndexString value) {
            requireOpen();
            SubMIndexString checked = Objects.requireNonNull(value, "value");
            return checked.length() == 0
                    ? this
                    : addRange(
                            checked.parent(),
                            checked.start(),
                            checked.end(),
                            MIndexStringMetadata.NONE);
        }

        public Builder add(
                MIndexString value,
                LexicalRef lexical,
                SemanticRef semantic,
                long relations,
                int sourceStart,
                int sourceEnd,
                int flags) {
            return add(
                    value,
                    MIndexStringMetadata.lexical(
                            lexical, semantic, relations, sourceStart, sourceEnd, flags));
        }

        public int segmentCount() { return size; }

        public MIndexCompositeString build() {
            requireOpen();
            built = true;
            return new MIndexCompositeString(this);
        }

        private Builder addRange(
                MIndexString value,
                int start,
                int end,
                MIndexStringMetadata metadata) {
            requireOpen();
            Objects.checkFromToIndex(start, end, value.length());
            MIndexStringMetadata actual =
                    metadata == null ? MIndexStringMetadata.NONE : metadata;
            grow(size + 1);
            int rangeLength = end - start;
            contentIds[size] = value.contentIndex();
            segmentStarts[size] = start;
            segmentEnds[size] = end;
            length = Math.addExact(length, rangeLength);
            utf16Ends[size] = length;
            flags[size] = actual.flags();
            lexicalBits[size] = actual.lexicalBits();
            semanticBits[size] = actual.semanticBits();
            relationBits[size] = actual.relationBits();
            sourceStarts[size] = actual.sourceStart();
            sourceEnds[size] = actual.sourceEnd();
            javaHash =
                    TextMetadata.concatHash(
                            javaHash, rangeJavaHash(value, start, end), rangeLength);
            aggregateFlags |= actual.flags();
            aggregateSemanticBits |= actual.semanticBits();
            aggregateRelationBits |= actual.relationBits();
            size++;
            return this;
        }

        private static int rangeJavaHash(MIndexString value, int start, int end) {
            int hash = 0;
            for (int index = start; index < end; index++) {
                hash = 31 * hash + value.charAt(index);
            }
            return hash;
        }

        private void requireOpen() {
            if (built) throw new IllegalStateException("composite builder already built");
        }

        private void grow(int required) {
            if (required <= contentIds.length) return;
            int old = contentIds.length;
            int capacity = Math.max(required, Math.multiplyExact(old, 2));
            contentIds = Arrays.copyOf(contentIds, capacity);
            segmentStarts = Arrays.copyOf(segmentStarts, capacity);
            segmentEnds = Arrays.copyOf(segmentEnds, capacity);
            utf16Ends = Arrays.copyOf(utf16Ends, capacity);
            flags = Arrays.copyOf(flags, capacity);
            lexicalBits = Arrays.copyOf(lexicalBits, capacity);
            semanticBits = Arrays.copyOf(semanticBits, capacity);
            relationBits = Arrays.copyOf(relationBits, capacity);
            sourceStarts = Arrays.copyOf(sourceStarts, capacity);
            sourceEnds = Arrays.copyOf(sourceEnds, capacity);
            Arrays.fill(sourceStarts, old, capacity, -1);
            Arrays.fill(sourceEnds, old, capacity, -1);
        }
    }
}
