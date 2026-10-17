// SPDX-License-Identifier: Apache-2.0
package com.m3.gc;

import java.util.Objects;

/**
 * One optional gate per parent-owned M3 residency arena, never per child.
 * Call applyAtSafePoint at an ordinary operation boundary, e.g. before
 * M3ObjectResidency.use(). The GC notification thread only increments an epoch.
 */
public final class M3GcPressureGate {
    private final Runnable pressureAction;
    private long observedEpoch;
    private boolean applying;

    public M3GcPressureGate(Runnable pressureAction) {
        this.pressureAction = Objects.requireNonNull(pressureAction, "pressureAction");
        observedEpoch = M3GcNotifications.epoch();
    }

    /** Coalesce GC events; leave them pending if the action throws. */
    public synchronized boolean applyAtSafePoint() {
        if (applying) return false;
        long current = M3GcNotifications.epoch();
        if (current == observedEpoch) return false;
        applying = true;
        try {
            pressureAction.run();
            observedEpoch = current;
            return true;
        } finally {
            applying = false;
        }
    }

}
