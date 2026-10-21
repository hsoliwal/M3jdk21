// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

/**
 * Chunk cursor over discontiguous symbol storage (an M3String tuple DAG, a rope, token lanes).
 *
 * <p>Pattern port of the helix-editor/regex-cursor {@code Cursor} trait (MIT OR Apache-2.0,
 * {@code src/cursor.rs} at fd4c51c7): the haystack is read one contiguous chunk at a time and is
 * never flattened. A chunk is exposed as {@link #chunkLength()} plus {@link #unit(int)} instead of
 * a borrowed array, so implementations read their storage in place.</p>
 *
 * <p>Symbols are non-negative ints: UTF-16 code units for text cursors, interned token ids for
 * token cursors. Positions are absolute logical offsets in {@code [0, totalLength()]}.</p>
 */
public interface M3ChunkCursor {
    /** Length of the current chunk; {@code 0} only when the source is empty. */
    int chunkLength();

    /** Symbol at {@code index} in the current chunk, {@code 0 <= index < chunkLength()}. */
    int unit(int index);

    /** Absolute logical offset of the first symbol of the current chunk. */
    int offset();

    /** Total number of symbols; constant for the life of the cursor. */
    int totalLength();

    /** Moves to the next chunk; returns {@code false} and keeps the chunk at the end. */
    boolean advance();

    /** Moves to the previous chunk; returns {@code false} and keeps the chunk at the start. */
    boolean backtrack();

    /**
     * Moves to the chunk containing {@code position}; {@code position == totalLength()} lands on
     * the last chunk. Replaces the donor's linear {@code Input::move_to}.
     *
     * @throws IndexOutOfBoundsException when {@code position} is outside {@code [0, total]}
     */
    void seek(int position);

    /** Symbol at an absolute position, seeking only when it lies outside the current chunk. */
    default int unitAt(int position) {
        int begin = offset();
        if (position < begin || position >= begin + chunkLength()) {
            seek(position);
            begin = offset();
        }
        return unit(position - begin);
    }
}
