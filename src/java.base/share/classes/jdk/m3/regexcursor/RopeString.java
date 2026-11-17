// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Objects;

/**
 * Proving-ground mirror of {@code java.lang.M3String}: exactly two fields, a canonical owner and a
 * packed coordinate {@code start << 32 | length} into it.
 *
 * <p>Deliberately offers no {@code toString()} over the text and no {@code charAt}: the only way
 * to read a rope string is {@link M3StringCursor}, so tests prove the regex path never flattens.
 * Composition ({@link #concat}) and slicing ({@link #slice}) only build coordinates.</p>
 */
public final class RopeString {
    private static final RopeString EMPTY = new RopeString(RopeAtom.of(""), 0L);

    private final RopeOwner owner;
    private final long value;

    private RopeString(RopeOwner owner, long value) {
        this.owner = owner;
        this.value = value;
    }

    /** Whole-owner string over a new atom holding {@code text}. */
    public static RopeString atom(CharSequence text) {
        RopeAtom atom = RopeAtom.of(text);
        return atom.length == 0 ? EMPTY : new RopeString(atom, span(0, atom.length));
    }

    /** Composition of {@code this} then {@code tail}; empty operands are elided. */
    public RopeString concat(RopeString tail) {
        Objects.requireNonNull(tail, "tail");
        if (tail.length() == 0) return this;
        if (length() == 0) return tail;
        RopeTuple tuple = new RopeTuple(this, tail);
        return new RopeString(tuple, span(0, tuple.length));
    }

    /** Coordinate view of {@code [start, end)}; shares the owner. */
    public RopeString slice(int start, int end) {
        Objects.checkFromToIndex(start, end, length());
        if (start == end) return EMPTY;
        if (start == 0 && end == length()) return this;
        return new RopeString(owner, span(start() + start, end - start));
    }

    /** Length in UTF-16 units. */
    public int length() {
        return (int) value;
    }

    /** Start of this view inside its owner. */
    int start() {
        return (int) (value >>> 32);
    }

    /** Canonical owner. */
    RopeOwner owner() {
        return owner;
    }

    /** Tuple height of the owner (0 for an atom). */
    public int height() {
        return owner instanceof RopeTuple tuple ? tuple.height : 0;
    }

    /** Presence facts: exact for whole-owner views, {@link M3CursorFacts#UNKNOWN} for slices. */
    long presence() {
        return start() == 0 && length() == owner.length ? owner.presence : CursorFacts.UNKNOWN;
    }

    private static long span(int start, int length) {
        return ((long) start << 32) | (length & 0xFFFF_FFFFL);
    }
}
