// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Arrays;
import java.util.Objects;

/** Immutable typed-atom view over one canonical MIndexString value. */
public final class MIndexLexicalView implements CharSequence, Comparable<MIndexLexicalView> {
    private final MIndexString source;
    private final MIndexLexiconSpace space;
    private final long[] atomBits;

    MIndexLexicalView(MIndexString source, MIndexLexiconSpace space, long[] atomBits) {
        this.source = Objects.requireNonNull(source, "source");
        this.space = Objects.requireNonNull(space, "space");
        this.atomBits = Objects.requireNonNull(atomBits, "atomBits").clone();
    }

    public MIndexString source() { return source; }
    public MIndexLexiconSpace space() { return space; }
    public int atomCount() { return atomBits.length; }

    public MIndexAtomRef atom(int index) {
        return new MIndexAtomRef(atomBits[Objects.checkIndex(index, atomBits.length)]);
    }

    public long atomBits(int index) {
        return atomBits[Objects.checkIndex(index, atomBits.length)];
    }

    public long[] atomBits() {
        return atomBits.clone();
    }

    public String materializeAtoms() {
        return space.decode(atomBits);
    }

    public boolean exactRoundTrip() {
        return source.toString().equals(materializeAtoms());
    }

    @Override public int length() { return source.length(); }
    @Override public char charAt(int index) { return source.charAt(index); }
    @Override public CharSequence subSequence(int start, int end) { return source.subSequence(start, end); }
    @Override public String toString() { return source.toString(); }

    @Override
    public int compareTo(MIndexLexicalView other) {
        return source.compareTo(Objects.requireNonNull(other, "other").source);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof MIndexLexicalView value
                && space == value.space
                && source.equals(value.source)
                && Arrays.equals(atomBits, value.atomBits));
    }

    @Override
    public int hashCode() {
        int result = 31 * source.hashCode() + System.identityHashCode(space);
        return 31 * result + Arrays.hashCode(atomBits);
    }
}
