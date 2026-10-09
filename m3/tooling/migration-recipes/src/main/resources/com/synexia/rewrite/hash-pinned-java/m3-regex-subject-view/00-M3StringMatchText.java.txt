/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package java.lang;

/**
 * The view a regex matcher reads while matching an M3-backed String (A20). The units are read
 * from the storage into blocks of {@link #BLOCK} units, each allocated and filled on first touch,
 * so the engine's per-unit reads are array loads instead of owner dispatch, an early failure
 * pays one block, and the String itself stays the source of every subsequence the matcher hands
 * out. Transient per matcher; a String above {@link #MAX_UNITS} units has no view.
 */
final class M3StringMatchText implements CharSequence {

    /** Above this many units the matcher reads the String directly (the block table alone would be 64 KiB). */
    static final int MAX_UNITS = 1 << 24;
    private static final int BLOCK_SHIFT = 10;
    private static final int BLOCK = 1 << BLOCK_SHIFT;
    private static final int BLOCK_MASK = BLOCK - 1;

    private final M3String storage;
    private final int length;
    private final char[][] blocks;

    private M3StringMatchText(M3String storage) {
        this.storage = storage;
        this.length = storage.length();
        this.blocks = new char[(length + BLOCK_MASK) >>> BLOCK_SHIFT][];
    }

    /** The view of an M3-backed String, or {@code null} for a flat String or one above the cap. */
    static CharSequence of(String subject) {
        M3String storage = subject.m3();
        return storage == null || storage.length() > MAX_UNITS ? null : new M3StringMatchText(storage);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        char[] block = blocks[index >>> BLOCK_SHIFT];
        if (block == null) block = fill(index >>> BLOCK_SHIFT);
        return block[index & BLOCK_MASK];
    }

    private char[] fill(int index) {
        int from = index << BLOCK_SHIFT;
        int to = Math.min(length, from + BLOCK);
        char[] block = new char[to - from];
        storage.getChars(from, to, block, 0);
        blocks[index] = block;
        return block;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().substring(start, end);
    }

    @Override
    public String toString() {
        char[] units = new char[length];
        storage.getChars(0, length, units, 0);
        return new String(units);
    }
}
