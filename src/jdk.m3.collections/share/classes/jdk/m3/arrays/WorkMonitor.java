// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.concurrent.CancellationException;

/**
 * The work-reporting and cancellation contract of a long-running array operation, in the array substrate layer.
 * {@code jdk.m3.collections.ProgressMonitor} extends it, so every progress monitor can be passed to the monitored
 * packed-array operations (PACKED_UNION U9).
 */
public interface WorkMonitor {
    /** Reports {@code work} more completed units. */
    void worked(long work);

    /** Returns whether the operation has been canceled. */
    boolean isCanceled();

    /** Throws {@link CancellationException} when the operation has been canceled. */
    default void checkCanceled() {
        if (isCanceled()) {
            throw new CancellationException("canceled");
        }
    }
}
