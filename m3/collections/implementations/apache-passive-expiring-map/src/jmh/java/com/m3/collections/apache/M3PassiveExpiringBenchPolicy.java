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
 * Per-entry benchmark policy on the fake clock: {@code now + ttl - (key & 63)}, so expiration times leave insertion
 * order and the M3 index runs as a heap. Stateless apart from the time-to-live; the oracle loader defines its own copy.
 */
public final class M3PassiveExpiringBenchPolicy implements ExpirationPolicy<Integer, String> {
    @Serial
    private static final long serialVersionUID = 1L;
    private final long ttl;

    /** Public for reflective construction inside the oracle loader. */
    public M3PassiveExpiringBenchPolicy(long ttl) {
        this.ttl = ttl;
    }

    @Override
    public long expirationTime(Integer key, String value) {
        return M3PassiveExpiringTestClock.currentTimeMillis() + ttl - (key & 63);
    }
}
