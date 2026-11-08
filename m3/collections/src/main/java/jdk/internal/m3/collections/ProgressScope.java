// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;
import jdk.m3.collections.M3Progress;
import jdk.m3.collections.M3ProgressAdapters;

/**
 * Ownership-aware compatibility scope for legacy Progress consumers.
 *
 * <p>An explicit monitor is borrowed. A missing monitor creates a weighted child of the shared
 * SystemProgress root and exposes it through the legacy Progress surface.</p>
 */
public final class ProgressScope implements AutoCloseable {
    private final M3Progress monitor;
    private final boolean owned;
    private boolean closed;

    private ProgressScope(M3Progress monitor, boolean owned) {
        this.monitor = Objects.requireNonNull(monitor, "monitor");
        this.owned = owned;
    }

    public static ProgressScope resolve(M3Progress monitor, long systemWork) {
        if (systemWork < 0L) throw new IllegalArgumentException("systemWork must be non-negative");
        return monitor == null
                ? new ProgressScope(M3ProgressAdapters.asLegacy(SystemProgress.split(systemWork)), true)
                : new ProgressScope(monitor, false);
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
