// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Pure deterministic integer token-bucket decisions over event-time arrays.
 * No sleeping, mutable global limiter, network admission or realtime guarantee.
 */
public final class M3RateGovernor {
    private M3RateGovernor() { }

    public record Decisions(long[] accepted, long[] balanceAfter, int acceptedCount) {
        public Decisions {
            accepted = accepted.clone();
            balanceAfter = balanceAfter.clone();
        }
        @Override public long[] accepted() { return accepted.clone(); }
        @Override public long[] balanceAfter() { return balanceAfter.clone(); }
    }

    /** Initially full at tick zero. Rejecting a request leaves the current balance intact. */
    public static Decisions tokenBucket(long[] ticks, long[] costs, long capacity,
                                        long refillPerTick) {
        Objects.requireNonNull(ticks, "ticks");
        Objects.requireNonNull(costs, "costs");
        if (ticks.length != costs.length || ticks.length > 1_000_000
                || capacity < 1 || refillPerTick < 0)
            throw new IllegalArgumentException("token-bucket geometry");
        long[] mask = new long[(ticks.length + 63) / 64];
        long[] balanceAfter = new long[ticks.length];
        long balance = capacity, last = 0;
        int accepted = 0;
        for (int i = 0; i < ticks.length; i++) {
            long now = ticks[i], cost = costs[i];
            if (now < last || cost < 0)
                throw new IllegalArgumentException("unsorted tick or negative cost");
            long elapsed = now - last, missing = capacity - balance;
            if (elapsed > 0 && refillPerTick > 0 && missing > 0) {
                long needed = missing / refillPerTick + (missing % refillPerTick == 0 ? 0 : 1);
                if (elapsed >= needed) balance = capacity;
                else balance += elapsed * refillPerTick; // product is strictly smaller than missing
            }
            if (cost <= balance) {
                balance -= cost;
                mask[i >>> 6] |= 1L << (i & 63);
                accepted++;
            }
            balanceAfter[i] = balance;
            last = now;
        }
        return new Decisions(mask, balanceAfter, accepted);
    }
}
