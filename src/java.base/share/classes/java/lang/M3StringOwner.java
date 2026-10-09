/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

/** Canonical owner node for M3String. Text payload never lives in M3String itself. */
abstract sealed class M3StringOwner permits M3StringAtom, M3StringTuple {
    static final byte ATOM = 1;
    static final byte TUPLE = 2;

    final byte kind;
    final int length;
    final byte coder;
    final int javaHash;
    final long structuralHash64;
    private volatile M3StringFacts facts;

    /*
     * Small bounded range-fact cache. Keys are exact packed M3String coordinates and therefore
     * never alias equal numeric ranges from another owner. Four slots keep retention bounded
     * while covering the hot substring/view cases without a global cache.
     */
    private volatile RangeFact range0;
    private volatile RangeFact range1;
    private volatile RangeFact range2;
    private volatile RangeFact range3;

    M3StringOwner(byte kind, int length, byte coder, int javaHash, long structuralHash64) {
        this.kind = kind;
        this.length = length;
        this.coder = coder;
        this.javaHash = javaHash;
        this.structuralHash64 = structuralHash64;
    }

    abstract char charAt(int index);

    void getChars(int start, int end, char[] destination, int destinationStart) {
        Objects.checkFromToIndex(start, end, length);
        Objects.checkFromIndexSize(destinationStart, end - start, destination.length);
        for (int source = start, target = destinationStart; source < end; source++, target++) {
            destination[target] = charAt(source);
        }
    }

    /**
     * First index in {@code [start, start + count)} whose UTF-16 unit differs from the flat
     * compact value {@code flat} (coder {@code flatCoder}) at {@code flatOffset}, {@code -1}
     * when the ranges agree. The default folds bulk-read windows against the value array; an
     * atom compares its native bytes in place (A11).
     */
    int mismatchUnits(int start, byte[] flat, int flatOffset, byte flatCoder, int count) {
        Objects.checkFromIndexSize(start, count, length);
        Objects.checkFromIndexSize(flatOffset, count, flat.length >> flatCoder);
        char[] window = new char[Math.min(count, 256)];
        for (int base = 0; base < count; base += window.length) {
            int chunk = Math.min(window.length, count - base);
            getChars(start + base, start + base + chunk, window, 0);
            for (int index = 0; index < chunk; index++) {
                char unit = flatCoder == String.LATIN1
                        ? StringLatin1.getChar(flat, flatOffset + base + index)
                        : StringUTF16.getChar(flat, flatOffset + base + index);
                if (window[index] != unit) return base + index;
            }
        }
        return -1;
    }

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
        for (int source = start, target = destinationStart; source < end; source++, target++) {
            char unit = charAt(source);
            if (destinationCoder == String.LATIN1) {
                destination[target] = (byte) unit;
            } else {
                StringUTF16.putChar(destination, target, unit);
            }
        }
    }

    final M3StringFacts factsIfPrepared() {
        return facts;
    }

    final M3StringFacts rangeFactsIfPrepared(long coordinate) {
        RangeFact first = range0;
        if (first != null && first.coordinate == coordinate) return first.facts;
        RangeFact second = range1;
        if (second != null && second.coordinate == coordinate) return second.facts;
        RangeFact third = range2;
        if (third != null && third.coordinate == coordinate) return third.facts;
        RangeFact fourth = range3;
        return fourth != null && fourth.coordinate == coordinate ? fourth.facts : null;
    }

    final M3StringFacts facts() {
        M3StringFacts current = facts;
        if (current != null) return current;
        synchronized (this) {
            current = facts;
            if (current == null) {
                current = computeFacts();
                if (current.utf16Length != length || current.javaHash != javaHash) {
                    throw new InternalError("M3String canonical fact mismatch");
                }
                facts = current;
            }
            return current;
        }
    }

    final M3StringFacts rangeFacts(long coordinate, M3String value) {
        RangeFact first = range0;
        if (first != null && first.coordinate == coordinate) return first.facts;
        RangeFact second = range1;
        if (second != null && second.coordinate == coordinate) return second.facts;
        RangeFact third = range2;
        if (third != null && third.coordinate == coordinate) return third.facts;
        RangeFact fourth = range3;
        if (fourth != null && fourth.coordinate == coordinate) return fourth.facts;

        M3String checked = Objects.requireNonNull(value, "value");
        M3StringFacts computed = computeRangeFacts(checked.start(), checked.length());
        synchronized (this) {
            first = range0;
            if (first != null && first.coordinate == coordinate) return first.facts;
            second = range1;
            if (second != null && second.coordinate == coordinate) return second.facts;
            third = range2;
            if (third != null && third.coordinate == coordinate) return third.facts;
            fourth = range3;
            if (fourth != null && fourth.coordinate == coordinate) return fourth.facts;
            range3 = range2;
            range2 = range1;
            range1 = range0;
            range0 = new RangeFact(coordinate, computed);
        }
        return computed;
    }

    abstract M3StringFacts computeFacts();

    abstract M3StringFacts computeRangeFacts(int start, int length);

    private static final class RangeFact {
        final long coordinate;
        final M3StringFacts facts;

        RangeFact(long coordinate, M3StringFacts facts) {
            this.coordinate = coordinate;
            this.facts = Objects.requireNonNull(facts, "facts");
        }
    }
}
