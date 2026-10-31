// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.util.BitSet;
import java.util.Collection;

/** Immutable CPU reference prefilter. Positives require an exact lookup. */
public final class BloomHint {
    private final BitSet bits;
    private final int bitCount;
    private final int probes;

    public BloomHint(Collection<String> keys) {
        if (keys.size() > InventorySnapshot.MAX_CELLS) {
            throw new IllegalArgumentException("BLOOM_CAPACITY");
        }
        int count = Math.max(1, keys.size());
        bitCount = Math.max(64, (int) StrictMath.ceil(-count * StrictMath.log(0.01)
                / (StrictMath.log(2) * StrictMath.log(2))));
        probes = Math.max(1, (int) StrictMath.round((double) bitCount / count * StrictMath.log(2)));
        bits = new BitSet(bitCount);
        for (String key : keys) {
            long[] hash = hash(key);
            for (int i = 0; i < probes; i++) bits.set(position(hash, i));
        }
    }

    public boolean mightContain(String key) {
        long[] hash = hash(key);
        for (int i = 0; i < probes; i++) {
            if (!bits.get(position(hash, i))) return false;
        }
        return true;
    }

    private int position(long[] hash, int probe) {
        return (int) Long.remainderUnsigned(hash[0] + probe * hash[1], bitCount);
    }

    private static long[] hash(String key) {
        String digest = EntropyValues.digest(EntropyValues.text(key, 4096, true));
        return new long[] {Long.parseUnsignedLong(digest.substring(0, 16), 16),
                Long.parseUnsignedLong(digest.substring(16, 32), 16) | 1L};
    }
}
