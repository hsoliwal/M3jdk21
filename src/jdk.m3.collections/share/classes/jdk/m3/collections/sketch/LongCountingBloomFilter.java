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
package jdk.m3.collections.sketch;

import java.util.Arrays;

/**
 * Deletable approximate long membership with two saturating 4-bit counters per byte.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongCountingBloomFilter.java}
 * blob {@code c351e26f7795c4c8ea632d9463ed631b08837ef2} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongCountingBloomFilter {
    private final byte[] packedCounters;
    private final int counterMask;
    private final int hashCount;

    public LongCountingBloomFilter(int counterCount, int hashCount) {
        if (counterCount <= 0) throw new IllegalArgumentException("counterCount <= 0");
        if (hashCount <= 0 || hashCount > 32) {
            throw new IllegalArgumentException("hashCount must be 1..32");
        }
        int normalized = nextPowerOfTwo(Math.max(2, counterCount));
        packedCounters = new byte[(normalized + 1) >>> 1];
        counterMask = normalized - 1;
        this.hashCount = hashCount;
    }

    public int counterCount() { return counterMask + 1; }
    public int hashCount() { return hashCount; }
    public long payloadBytes() { return packedCounters.length; }

    public void add(long value) {
        long h1 = LongBloomFilter.mix64(value);
        long h2 = LongBloomFilter.mix64(h1 ^ 0xd6e8feb86659fd93L) | 1L;
        for (int i = 0; i < hashCount; i++) {
            int index = (int) (h1 + i * h2) & counterMask;
            int current = counter(index);
            if (current < 15) setCounter(index, current + 1);
        }
    }

    public boolean mightContain(long value) {
        long h1 = LongBloomFilter.mix64(value);
        long h2 = LongBloomFilter.mix64(h1 ^ 0xd6e8feb86659fd93L) | 1L;
        for (int i = 0; i < hashCount; i++) {
            int index = (int) (h1 + i * h2) & counterMask;
            if (counter(index) == 0) return false;
        }
        return true;
    }

    /**
     * Removes one observation. Callers must only remove values previously added;
     * deleting an absent value can introduce false negatives for colliding keys.
     */
    public void remove(long value) {
        long h1 = LongBloomFilter.mix64(value);
        long h2 = LongBloomFilter.mix64(h1 ^ 0xd6e8feb86659fd93L) | 1L;
        for (int i = 0; i < hashCount; i++) {
            int index = (int) (h1 + i * h2) & counterMask;
            int current = counter(index);
            if (current > 0) setCounter(index, current - 1);
        }
    }

    public void clear() { Arrays.fill(packedCounters, (byte) 0); }

    int counterAt(int index) {
        if (index < 0 || index > counterMask) throw new IndexOutOfBoundsException(index);
        return counter(index);
    }

    private int counter(int index) {
        int packed = Byte.toUnsignedInt(packedCounters[index >>> 1]);
        return (index & 1) == 0 ? packed & 0x0f : packed >>> 4;
    }

    private void setCounter(int index, int value) {
        int slot = index >>> 1;
        int packed = Byte.toUnsignedInt(packedCounters[slot]);
        packed = (index & 1) == 0
                ? (packed & 0xf0) | value
                : (packed & 0x0f) | (value << 4);
        packedCounters[slot] = (byte) packed;
    }

    private static int nextPowerOfTwo(int value) {
        if (value > (1 << 30)) throw new IllegalArgumentException("counterCount too large");
        return value <= 1 ? 1 : Integer.highestOneBit(value - 1) << 1;
    }
}
