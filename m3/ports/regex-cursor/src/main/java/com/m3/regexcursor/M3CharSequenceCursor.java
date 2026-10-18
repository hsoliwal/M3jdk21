// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Chunk cursor over any {@code CharSequence} with an explicit chunk plan.
 *
 * <p>A flat String is one chunk; a cut plan splits it into arbitrary chunks (1-unit chunks
 * included), the M3 analogue of the donor's {@code test_rope.rs} random chunking. Units are read
 * with {@code charAt}; nothing is copied.</p>
 */
public final class M3CharSequenceCursor implements M3ChunkCursor {
    private final CharSequence text;
    private final int[] ends;
    private int chunk;
    private int begin;
    private int end;

    private M3CharSequenceCursor(CharSequence text, int[] ends) {
        this.text = text;
        this.ends = ends;
        seek(0);
    }

    /** One chunk over the whole text. */
    public static M3CharSequenceCursor whole(CharSequence text) {
        return chunked(text, new int[0]);
    }

    /**
     * Chunks ending at each strictly increasing cut in {@code (0, length)} and at the end.
     *
     * @throws IllegalArgumentException when a cut is out of range or not increasing
     */
    public static M3CharSequenceCursor chunked(CharSequence text, int[] cuts) {
        Objects.requireNonNull(text, "text");
        int length = text.length();
        int[] ends = Arrays.copyOf(cuts, cuts.length + (length == 0 ? 0 : 1));
        int previous = 0;
        for (int cut : cuts) {
            if (cut <= previous || cut >= length) throw new IllegalArgumentException("cut " + cut);
            previous = cut;
        }
        if (length != 0) ends[cuts.length] = length;
        return new M3CharSequenceCursor(text, ends);
    }

    @Override
    public int chunkLength() {
        return end - begin;
    }

    @Override
    public int unit(int index) {
        return text.charAt(begin + index);
    }

    @Override
    public int offset() {
        return begin;
    }

    @Override
    public int totalLength() {
        return text.length();
    }

    @Override
    public boolean advance() {
        if (chunk + 1 >= ends.length) return false;
        select(chunk + 1);
        return true;
    }

    @Override
    public boolean backtrack() {
        if (chunk == 0) return false;
        select(chunk - 1);
        return true;
    }

    @Override
    public void seek(int position) {
        Objects.checkIndex(position, text.length() + 1);
        if (ends.length == 0) return;
        int found = Arrays.binarySearch(ends, position);
        int index = found >= 0 ? found + 1 : -found - 1;
        select(Math.min(index, ends.length - 1));
    }

    private void select(int index) {
        chunk = index;
        begin = index == 0 ? 0 : ends[index - 1];
        end = ends[index];
    }
}
