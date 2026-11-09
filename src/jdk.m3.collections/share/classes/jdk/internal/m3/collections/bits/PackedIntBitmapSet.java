/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.bits;

import java.util.Arrays;
import jdk.internal.m3.collections.IntUniverseSet;

/**
 * Lazy full-domain bitmap, exact membership; clear scans words, iteration scans set bits.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedIntBitmapSet.java}
 * blob {@code fe1889ec148b5a30881aa58d1da82b79db9ce324} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedIntBitmapSet implements IntUniverseSet {
    private static final long[] EMPTY = new long[0];
    private final int universeSize;
    private long[] words = EMPTY;
    private int size;
    public PackedIntBitmapSet(int universeSize) {
        if (universeSize < 0 || universeSize > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("universeSize");
        this.universeSize = universeSize;
    }
    public int universeSize() { return universeSize; }
    public int size() { return size; }
    public long arrayPayloadBytes() { return 8L * words.length; }
    public boolean contains(int value) {
        check(value); return words.length != 0 && (words[PackedBitAtoms.wordIndex(value)] & PackedBitAtoms.bitMask(value)) != 0;
    }
    public boolean add(int value) {
        check(value);
        if (words.length == 0) words = new long[PackedBitAtoms.wordsForBits(universeSize)];
        long mask = PackedBitAtoms.bitMask(value); int at = PackedBitAtoms.wordIndex(value);
        if ((words[at] & mask) != 0) return false;
        words[at] |= mask; size++; return true;
    }
    public boolean remove(int value) {
        if (!contains(value)) return false;
        words[PackedBitAtoms.wordIndex(value)] &= ~PackedBitAtoms.bitMask(value); size--; return true;
    }
    public void clear() { Arrays.fill(words, 0); size = 0; }
    public int[] toArray() {
        int[] result = new int[size]; int count = 0;
        for (int word = 0; word < words.length; word++) {
            long remaining = words[word];
            while (remaining != 0) {
                result[count++] = (word << 6) + Long.numberOfTrailingZeros(remaining);
                remaining &= remaining - 1;
            }
        }
        return result;
    }
    private void check(int value) {
        if (value < 0 || value >= universeSize) throw new IndexOutOfBoundsException(value);
    }
}
