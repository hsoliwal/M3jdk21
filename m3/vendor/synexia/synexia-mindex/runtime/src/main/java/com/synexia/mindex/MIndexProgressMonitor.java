// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.concurrent.CancellationException;

/**
 * Dependency-free progress/cancellation contract for potentially long-running
 * MIndex operations. It can be adapted to Synexia COP progress monitors without
 * coupling this standalone compiler experiment to the full reactor.
 */
public interface MIndexProgressMonitor {
    MIndexProgressMonitor NONE = new MIndexProgressMonitor() {};

    default void begin(String task, long totalWork) {}
    default void worked(long work) {}
    default void done() {}
    default boolean isCancelled() { return false; }

    default void checkCancelled() {
        if (isCancelled()) {
            throw new CancellationException("MIndex operation cancelled");
        }
    }

    static MIndexProgressMonitor none() {
        return NONE;
    }
}
