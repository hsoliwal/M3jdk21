// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/**
 * Ownership-aware compatibility scope for legacy Progress consumers.
 *
 * <p>An explicit monitor is borrowed. A missing monitor creates a weighted child of the shared
 * SystemProgress root and exposes it through the legacy Progress surface.</p>
 */
public final class M3ProgressScope implements AutoCloseable {
    private final M3Progress monitor;
    private final boolean owned;
    private boolean closed;

    private M3ProgressScope(M3Progress monitor, boolean owned) {
        this.monitor = Objects.requireNonNull(monitor, "monitor");
        this.owned = owned;
    }

    public static M3ProgressScope resolve(M3Progress monitor, long systemWork) {
        if (systemWork < 0L) throw new IllegalArgumentException("systemWork must be non-negative");
        return monitor == null
                ? new M3ProgressScope(M3ProgressAdapters.asLegacy(M3SystemProgress.split(systemWork)), true)
                : new M3ProgressScope(monitor, false);
    }

    public M3Progress monitor() { return monitor; }
    public boolean owned() { return owned; }

    @Override public void close() {
        if (!closed && owned) {
            closed = true;
            monitor.done();
        }
    }
}
