// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.util.List;

/**
 * Read cursor over a composite text given as ordered atoms. Units are read in place through the atoms;
 * the composite is never joined into one array (window access only).
 */
public final class M3AtomCursor {
    private final List<? extends CharSequence> atoms;
    private final int[] starts;
    private final int length;

    /** @param atoms ordered atom views of one composite String */
    public M3AtomCursor(List<? extends CharSequence> atoms) {
        this.atoms = List.copyOf(atoms);
        this.starts = new int[atoms.size() + 1];
        for (int index = 0; index < atoms.size(); index++) {
            starts[index + 1] = Math.addExact(starts[index], atoms.get(index).length());
        }
        this.length = starts[atoms.size()];
    }

    /** @return total UTF-16 length of the composite */
    public int length() {
        return length;
    }

    /** @return the atoms, in order */
    public List<? extends CharSequence> atoms() {
        return atoms;
    }

    /** @return unit at a composite index, located by binary search over atom starts */
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(index);
        }
        int atom = atomIndex(index);
        return atoms.get(atom).charAt(index - starts[atom]);
    }

    /** @return atom index when {@code [start, end)} is exactly one whole atom, else -1 */
    public int wholeAtom(int start, int end) {
        if (start >= end) {
            return -1;
        }
        int atom = atomIndex(start);
        return starts[atom] == start && starts[atom + 1] == end ? atom : -1;
    }

    private int atomIndex(int index) {
        int low = 0;
        int high = atoms.size() - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (starts[middle] <= index) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        while (low + 1 < starts.length - 1 && starts[low + 1] <= index) {
            low++;
        }
        return low;
    }
}
