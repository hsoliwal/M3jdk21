/*******************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Arrays;

/**
 * Approximate membership, explicit bit/hash counts, no deletes; single-threaded.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongBloomFilter.java}
 * blob {@code 87983771a328f26c05e0cb661ec3457057f1fbef} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongBloomFilter implements M3LongBloomMembership {
    private final long[] words;
    private final int bitSize;
    private final int hashFunctions;
    public M3PackedLongBloomFilter(int bitSize, int hashFunctions) {
        if (bitSize < 1 || hashFunctions < 1 || hashFunctions > 32) throw new IllegalArgumentException("Bloom dimensions");
        this.bitSize = bitSize; this.hashFunctions = hashFunctions;
        words = new long[(int) ((bitSize + 63L) >>> 6)];
    }
    public int bitSize() { return bitSize; }
    public int hashFunctions() { return hashFunctions; }
    public long arrayPayloadBytes() { return 8L * words.length; }
    /** Returns whether any bit changed, not whether the value was previously absent. */
    public boolean add(long value) {
        long hash = mix(value), step = mix(value ^ 0x9e3779b97f4a7c15L) | 1L;
        boolean changed = false;
        for (int i = 0; i < hashFunctions; i++, hash += step) {
            int bit = (int) Long.remainderUnsigned(hash, bitSize); long mask = 1L << (bit & 63);
            changed |= (words[bit >>> 6] & mask) == 0; words[bit >>> 6] |= mask;
        }
        return changed;
    }
    public boolean mightContain(long value) {
        long hash = mix(value), step = mix(value ^ 0x9e3779b97f4a7c15L) | 1L;
        for (int i = 0; i < hashFunctions; i++, hash += step) {
            int bit = (int) Long.remainderUnsigned(hash, bitSize);
            if ((words[bit >>> 6] & (1L << (bit & 63))) == 0) return false;
        }
        return true;
    }
    public void clear() { Arrays.fill(words, 0); }
    private static long mix(long value) {
        value ^= value >>> 33; value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33; value *= 0xc4ceb9fe1a85ec53L;
        return value ^ (value >>> 33);
    }
}
