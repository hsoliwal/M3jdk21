/*******************************************************************************
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
