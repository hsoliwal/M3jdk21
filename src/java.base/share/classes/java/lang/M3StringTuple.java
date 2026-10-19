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

    @Override
    char charAt(int index) {
        Objects.checkIndex(index, length);
        int leftLength = left.length();
        return index < leftLength ? left.charAt(index) : right.charAt(index - leftLength);
    }

    @Override
    void getChars(int start, int end, char[] destination, int destinationStart) {
        Objects.checkFromToIndex(start, end, length);
        Objects.checkFromIndexSize(destinationStart, end - start, destination.length);
        int leftLength = left.length();
        if (end <= leftLength) {
            left.getChars(start, end, destination, destinationStart);
            return;
        }
        if (start >= leftLength) {
            right.getChars(start - leftLength, end - leftLength, destination, destinationStart);
            return;
        }
        int leftCount = leftLength - start;
        left.getChars(start, leftLength, destination, destinationStart);
        right.getChars(0, end - leftLength, destination, destinationStart + leftCount);
    }

    @Override
    void getBytes(
            int start,
            int end,
            byte[] destination,
            int destinationStart,
            byte destinationCoder) {
        Objects.checkFromToIndex(start, end, length);
        int count = end - start;
        Objects.checkFromIndexSize(
                destinationStart << destinationCoder,
                count << destinationCoder,
                destination.length);
        int leftLength = left.length();
        if (end <= leftLength) {
            left.owner().getBytes(
                    left.start() + start,
                    left.start() + end,
                    destination,
                    destinationStart,
                    destinationCoder);
            return;
        }
        if (start >= leftLength) {
            right.owner().getBytes(
                    right.start() + start - leftLength,
                    right.start() + end - leftLength,
                    destination,
                    destinationStart,
                    destinationCoder);
            return;
        }
        int leftCount = leftLength - start;
        left.owner().getBytes(
                left.start() + start,
                left.end(),
                destination,
                destinationStart,
                destinationCoder);
        right.owner().getBytes(
                right.start(),
                right.start() + end - leftLength,
                destination,
                destinationStart + leftCount,
                destinationCoder);
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
