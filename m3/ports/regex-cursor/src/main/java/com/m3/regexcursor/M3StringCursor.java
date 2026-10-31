// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Objects;

/**
 * Chunk cursor over an M3 string: each chunk is one visible run of one terminal atom, in logical
 * order, forward and backward.
 *
 * <p>The descent is the one of {@code java.lang.M3String.CodeUnitCursor.seek} (M3jdk21 master
 * 2f2beb2e): from the root {@code (owner, start, length)} go left when the position lies in the
 * left child, else right, clipping the visible run at every level, until the owner is an atom.
 * State is one atom reference plus four ints; no stack, no text copy. {@link #advance()} and
 * {@link #backtrack()} are seeks to the neighbouring position, O(tree height) each.</p>
 */
public final class M3StringCursor implements M3ChunkCursor, M3CursorFacts {
    private final M3RopeString source;
    private final int total;
    private M3RopeAtom atom;
    private int atomOffset;
    private int chunkBegin;
    private int chunkEnd;

    /** Cursor positioned on the first chunk. */
    public M3StringCursor(M3RopeString source) {
        this.source = Objects.requireNonNull(source, "source");
        this.total = source.length();
        seek(0);
    }

    @Override
    public int chunkLength() {
        return chunkEnd - chunkBegin;
    }

    @Override
    public int unit(int index) {
        return atom.unit(atomOffset + index);
    }

    @Override
    public int offset() {
        return chunkBegin;
    }

    @Override
    public int totalLength() {
        return total;
    }

    @Override
    public boolean advance() {
        if (chunkEnd >= total) return false;
        seek(chunkEnd);
        return true;
    }

    @Override
    public boolean backtrack() {
        if (chunkBegin == 0) return false;
        seek(chunkBegin - 1);
        return true;
    }

    @Override
    public void seek(int position) {
        Objects.checkIndex(position, total + 1);
        if (total == 0) {
            chunkBegin = 0;
            chunkEnd = 0;
            return;
        }
        descend(position == total ? total - 1 : position);
    }

    private void descend(int index) {
        M3RopeOwner current = source.owner();
        int position = source.start() + index;
        int before = index;
        int after = total - index;
        while (current instanceof M3RopeTuple tuple) {
            int leftLength = tuple.left.length();
            M3RopeString child = position < leftLength ? tuple.left : tuple.right;
            int childIndex = position < leftLength ? position : position - leftLength;
            before = Math.min(before, childIndex);
            after = Math.min(after, child.length() - childIndex);
            position = child.start() + childIndex;
            current = child.owner();
        }
        atom = (M3RopeAtom) current;
        atomOffset = position - before;
        chunkBegin = index - before;
        chunkEnd = index + after;
    }

    @Override
    public long unitPresenceMask() {
        return source.presence();
    }
}
