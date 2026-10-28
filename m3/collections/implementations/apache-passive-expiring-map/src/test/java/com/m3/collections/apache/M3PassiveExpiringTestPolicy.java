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
package com.m3.collections.apache;

import java.io.Serial;
import org.apache.commons.collections4.map.PassiveExpiringMap.ExpirationPolicy;

/**
 * Stateless per-entry policy for the differential runs: one fake-clock read per call and a deterministic mix of
 * never-expiring, already-expired, immediate and short expiration times. {@link M3PassiveExpiringOracle} defines a
 * second copy of this class inside the oracle's loader, so the Commons oracle and the M3 map run the same function.
 */
public final class M3PassiveExpiringTestPolicy implements ExpirationPolicy<Integer, String> {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Public for reflective construction inside the oracle loader. */
    public M3PassiveExpiringTestPolicy() {
    }

    @Override
    public long expirationTime(Integer key, String value) {
        long now = M3PassiveExpiringTestClock.currentTimeMillis();
        int k = key == null ? 17 : key;
        int h = 31 * k + (value == null ? 0 : value.hashCode());
        return switch (Math.floorMod(h, 13)) {
            case 0 -> -1L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> 0L;
            case 3 -> now;
            case 4 -> Long.MAX_VALUE;
            case 5 -> now - 3;
            default -> now + Math.floorMod(h, 40);
        };
    }
}
