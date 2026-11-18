/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

/**
 * Canonical persistent composition node.
 *
 * <p>Tuple owners keep child coordinates, not copied text or segment arrays. Repeated composition
 * therefore forms a shared immutable DAG.</p>
 */
final class M3StringTuple extends M3StringOwner {
    final M3String left;
    final M3String right;
    final int height;
    final long canonicalId;

    M3StringTuple(M3String left, M3String right, long canonicalId, long structuralHash64) {
        super(
                TUPLE,
                Math.addExact(left.length(), right.length()),
                (byte) (left.coder() | right.coder()),
                left.hashCodeValue() * M3String.pow31(right.length()) + right.hashCodeValue(),
                structuralHash64);
        this.left = Objects.requireNonNull(left, "left");
        this.right = Objects.requireNonNull(right, "right");
        this.height = 1 + Math.max(childHeight(left), childHeight(right));
        this.canonicalId = canonicalId;
    }

    /** Smallest height at which the stored segment list replaces the descent from the root. */
    private static final int SEGMENT_MIN_HEIGHT = 3;
    /** Most segments one stored list may hold; a wider tuple keeps the descent. */
    private static final int SEGMENT_MAX_COUNT = 1024;
    /** Marks a tuple that has no stored list, so the walk over its leaves is not repeated. */
    private static final Segments NO_SEGMENTS =
            new Segments(new int[0], new M3StringOwner[0], new int[0]);

    /** The segment list of this tuple once computed; {@link #NO_SEGMENTS} when it has none. */
    private Segments segments;

    @Override
    char charAt(int index) {
        Objects.checkIndex(index, length);
        Segments list = height >= SEGMENT_MIN_HEIGHT ? segments() : NO_SEGMENTS;
        if (list != NO_SEGMENTS) return list.charAt(index);
        int leftLength = left.length();
        return index < leftLength ? left.charAt(index) : right.charAt(index - leftLength);
    }

    /** Computed once and stored. The fields of a list are final, so a racing reader is safe. */
    private Segments segments() {
        Segments known = segments;
        if (known == null) {
            known = Segments.of(this);
            segments = known;
        }
        return known;
    }

    /**
     * A rope as a segment list: the scalar leaf ranges of a tuple in text order. Segment
     * {@code i} holds the units from {@code ends[i - 1]} (0 for the first) up to
     * {@code ends[i]}; they are in {@code owners[i]} from {@code starts[i]}. A unit is found by
     * a binary search over the cumulative ends, not by a descent from the root.
     */
    static final class Segments {
        private static final int FIRST_CAPACITY = 16;
        final int[] ends;
        final M3StringOwner[] owners;
        final int[] starts;

        Segments(int[] ends, M3StringOwner[] owners, int[] starts) {
            this.ends = ends;
            this.owners = owners;
            this.starts = starts;
        }

        /**
         * The list of {@code tuple}, or {@link #NO_SEGMENTS} when it has more than
         * {@link #SEGMENT_MAX_COUNT} segments, when a leaf is not a scalar owner, or when the
         * leaves do not add up to the length of the tuple.
         */
        static Segments of(M3StringTuple tuple) {
            int[] ends = new int[FIRST_CAPACITY];
            M3StringOwner[] owners = new M3StringOwner[FIRST_CAPACITY];
            int[] starts = new int[FIRST_CAPACITY];
            int count = 0;
            int end = 0;
            M3StringLeafCursor leaves = new M3StringLeafCursor(tuple, 0, tuple.length, true);
            while (leaves.next()) {
                if (leaves.length() == 0) continue;
                if (count == SEGMENT_MAX_COUNT || leaves.owner() instanceof M3StringTuple) {
                    return NO_SEGMENTS;
                }
                if (count == ends.length) {
                    ends = java.util.Arrays.copyOf(ends, count << 1);
                    owners = java.util.Arrays.copyOf(owners, count << 1);
                    starts = java.util.Arrays.copyOf(starts, count << 1);
                }
                end += leaves.length();
                ends[count] = end;
                owners[count] = leaves.owner();
                starts[count] = leaves.start();
                count++;
            }
            if (end != tuple.length || count == 0) return NO_SEGMENTS;
            return new Segments(java.util.Arrays.copyOf(ends, count),
                    java.util.Arrays.copyOf(owners, count), java.util.Arrays.copyOf(starts, count));
        }

        char charAt(int index) {
            int at = segmentOf(ends, index);
            int begin = at == 0 ? 0 : ends[at - 1];
            return owners[at].charAt(starts[at] + index - begin);
        }

        /** The segment that holds unit {@code index}: the first whose end is greater. */
        static int segmentOf(int[] ends, int index) {
            int low = 0;
            int high = ends.length - 1;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (ends[middle] > index) {
                    high = middle;
                } else {
                    low = middle + 1;
                }
            }
            return low;
        }
    }

    @Override
    void getChars(int start, int end, char[] destination, int destinationStart) {
        Objects.checkFromToIndex(start, end, length);
        Objects.checkFromIndexSize(destinationStart, end - start, destination.length);
        // The pool cursor already resolves canonical nested owner paths and exact offsets.
        M3StringPool.copyCharsFromPath(this, start, end, destination, destinationStart);
    }

    @Override
    void getBytes(int start, int end, byte[] destination, int destinationStart,
            byte destinationCoder) {
        Objects.checkFromToIndex(start, end, length);
        int count = end - start;
        checkByteDestination(destination, destinationStart, count, destinationCoder);
        M3StringPool.copyBytesFromPath(
                this, start, end, destination, destinationStart, destinationCoder);
    }

    private static int childHeight(M3String value) {
        M3StringOwner owner = value.owner();
        return value.start() == 0
                        && value.length() == owner.length
                        && owner instanceof M3StringTuple tuple
                ? tuple.height
                : 0;
    }

    @Override
    M3StringFacts computeFacts() {
        return M3StringFacts.compose(left.facts(), right.facts());
    }

    @Override
    M3StringFacts computeRangeFacts(int start, int length) {
        Objects.checkFromIndexSize(start, length, this.length);
        if (start == 0 && length == this.length) return facts();
        if (length == 0) return M3StringFacts.scan(M3String.empty());

        int end = start + length;
        int leftLength = left.length();
        if (end <= leftLength) {
            return left.slice(start, end).facts();
        }
        if (start >= leftLength) {
            return right.slice(start - leftLength, end - leftLength).facts();
        }
        return M3StringFacts.compose(
                left.slice(start, leftLength).facts(),
                right.slice(0, end - leftLength).facts());
    }
}
