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
            // Binary64 rounding must not allocate more than the integral parent share.
            push(Math.min(parentUnits, (long) Math.floor((double) worked * parentUnits / total)));
        }
    }

    private void push(long target) {
        if (target > reported) {
            parent.worked(target - reported);
            reported = target;
        }
    }
}
