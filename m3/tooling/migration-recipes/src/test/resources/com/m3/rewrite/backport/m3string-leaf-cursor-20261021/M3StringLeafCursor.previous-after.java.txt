/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;
import java.util.Objects;

/**
 * Thread-confined owner/range cursor over the persistent M3String tuple DAG.
 *
 * <p>The cursor retains only canonical owners plus packed primitive coordinates. It never copies
 * text, exposes a native address, or manufactures child M3String wrappers. Callers choose whether
 * adjacent visible ranges of the same scalar owner are coalesced: canonical tuple comparison uses
 * coalescing, while token-sequence regex preserves each visible leaf occurrence.</p>
 */
final class M3StringLeafCursor {
    private M3StringOwner[] pendingOwners = new M3StringOwner[8];
    private long[] pendingCoordinates = new long[8];
    private int pendingSize;

    private final boolean coalesceAdjacent;
    private M3StringOwner owner;
    private int start;
    private int length;

    private boolean buffered;
    private M3StringOwner bufferedOwner;
    private int bufferedStart;
    private int bufferedLength;

    private M3StringOwner rawOwner;
    private int rawStart;
    private int rawLength;

    M3StringLeafCursor(M3String source, boolean coalesceAdjacent) {
        this(source, null, coalesceAdjacent);
    }

    M3StringLeafCursor(M3String first, M3String second, boolean coalesceAdjacent) {
        this.coalesceAdjacent = coalesceAdjacent;
        if (second != null) push(second.owner(), second.start(), second.length());
        if (first != null) push(first.owner(), first.start(), first.length());
    }

    boolean next() {
        if (!coalesceAdjacent) {
            if (!pullRaw()) return false;
            owner = rawOwner;
            start = rawStart;
            length = rawLength;
            return true;
        }

        if (buffered) {
            owner = bufferedOwner;
            start = bufferedStart;
            length = bufferedLength;
            buffered = false;
        } else {
            if (!pullRaw()) return false;
            owner = rawOwner;
            start = rawStart;
            length = rawLength;
        }

        while (pullRaw()) {
            if (rawOwner == owner && start + length == rawStart) {
                length = Math.addExact(length, rawLength);
            } else {
                buffered = true;
                bufferedOwner = rawOwner;
                bufferedStart = rawStart;
                bufferedLength = rawLength;
                break;
            }
        }
        return true;
    }

    M3StringOwner owner() {
        return owner;
    }

    int start() {
        return start;
    }

    int length() {
        return length;
    }

    private void push(M3StringOwner owner, int offset, int count) {
        if (count == 0) return;
        Objects.checkFromIndexSize(offset, count, owner.length);
        if (pendingSize == pendingOwners.length) {
            int capacity = Math.multiplyExact(pendingOwners.length, 2);
            pendingOwners = Arrays.copyOf(pendingOwners, capacity);
            pendingCoordinates = Arrays.copyOf(pendingCoordinates, capacity);
        }
        pendingOwners[pendingSize] = owner;
        pendingCoordinates[pendingSize] =
                ((long) offset << Integer.SIZE) | Integer.toUnsignedLong(count);
        pendingSize++;
    }

    private boolean pullRaw() {
        while (pendingSize != 0) {
            int top = --pendingSize;
            M3StringOwner current = pendingOwners[top];
            pendingOwners[top] = null;
            long coordinate = pendingCoordinates[top];
            int rangeStart = (int) (coordinate >>> Integer.SIZE);
            int rangeLength = (int) coordinate;
            if (!(current instanceof M3StringTuple tuple)) {
                rawOwner = current;
                rawStart = rangeStart;
                rawLength = rangeLength;
                return true;
            }

            int rangeEnd = Math.addExact(rangeStart, rangeLength);
            int leftLength = tuple.left.length();
            if (rangeEnd > leftLength) {
                int rightStart = Math.max(0, rangeStart - leftLength);
                int rightEnd = rangeEnd - leftLength;
                push(
                        tuple.right.owner(),
                        Math.addExact(tuple.right.start(), rightStart),
                        rightEnd - rightStart);
            }
            if (rangeStart < leftLength) {
                int leftStart = rangeStart;
                int leftEnd = Math.min(rangeEnd, leftLength);
                push(
                        tuple.left.owner(),
                        Math.addExact(tuple.left.start(), leftStart),
                        leftEnd - leftStart);
            }
        }
        return false;
    }
}
