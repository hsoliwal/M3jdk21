// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/** Maximum observation granularity requested by one lazy event policy. */
public enum M3LazyEventGranularity {
    INHERIT,
    NONE,
    OPERATION,
    RANGE,
    ELEMENT,
    STATE_TRANSITION;

    public boolean includes(M3LazyEventGranularity required) {
        if (this == INHERIT || this == NONE || required == INHERIT || required == NONE) {
            return false;
        }
        return rank(this) >= rank(required);
    }

    private static int rank(M3LazyEventGranularity value) {
        return switch (value) {
            case OPERATION -> 1;
            case RANGE -> 2;
            case ELEMENT -> 3;
            case STATE_TRANSITION -> 4;
            case INHERIT, NONE -> 0;
        };
    }
}
