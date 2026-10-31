// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * Child of {@link M3FileProgress#split(long)}: maps its own total onto {@code parentUnits} of the parent.
 *
 * <p>Before {@link #begin} (or with total {@code 0}) units pass through one to one, capped at the share;
 * after it they are scaled. The parent never receives more than the share; {@link #done()} tops it up.
 * Cancellation is the parent's.</p>
 */
final class M3FileSubProgress implements M3FileProgress {
    private final M3FileProgress parent;
    private final long parentUnits;
    private long total;
    private long worked;
    private long reported;

    M3FileSubProgress(M3FileProgress parent, long parentUnits) {
        this.parent = parent;
        this.parentUnits = parentUnits;
    }

    @Override
    public void begin(String task, long totalUnits) {
        if (totalUnits < 0) throw new IllegalArgumentException("negative total: " + totalUnits);
        total = totalUnits;
        report();
    }

    @Override
    public void worked(long units) {
        if (units <= 0) return;
        worked = worked > Long.MAX_VALUE - units ? Long.MAX_VALUE : worked + units;
        report();
    }

    @Override
    public boolean isCanceled() {
        return parent.isCanceled();
    }

    @Override
    public void done() {
        push(parentUnits);
    }

    private void report() {
        if (total == 0) {
            push(Math.min(worked, parentUnits));
        } else if (worked >= total) {
            push(parentUnits);
        } else {
            push(scaled(worked, parentUnits, total));
        }
    }

    /**
     * Exact floor(work * share / total), no floating point, BigInteger or heap
     * allocation. The common path uses an unsigned divide. Oversized products
     * use two machine words and a bounded 64-step unsigned division.
     */
    private static long scaled(long work, long share, long total) {
        if (work == 0 || share == 0) return 0;
        long low = work * share;
        long high = Math.multiplyHigh(work, share);
        if (high == 0) return Long.divideUnsigned(low, total);
        long remainder = high;
        long quotient = 0;
        for (int bit = 63; bit >= 0; bit--) {
            remainder = (remainder << 1) | ((low >>> bit) & 1L);
            if (Long.compareUnsigned(remainder, total) >= 0) {
                remainder -= total;
                quotient |= 1L << bit;
            }
        }
        return quotient;
    }

    private void push(long target) {
        if (target > reported) {
            parent.worked(target - reported);
            reported = target;
        }
    }
}
