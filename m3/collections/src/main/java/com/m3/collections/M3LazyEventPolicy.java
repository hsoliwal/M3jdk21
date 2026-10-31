// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/** Immutable bitmask and granularity policy for lazy operation observation. */
public record M3LazyEventPolicy(
        long includeMask,
        long excludeMask,
        short watchedStateMask,
        M3LazyEventGranularity granularity) {

    public static final M3LazyEventPolicy NONE =
            new M3LazyEventPolicy(0L, 0L, (short) 0, M3LazyEventGranularity.NONE);

    public M3LazyEventPolicy {
        Objects.requireNonNull(granularity, "granularity");
        if ((includeMask & ~M3LazyEventBits.ALL) != 0L
                || (excludeMask & ~M3LazyEventBits.ALL) != 0L) {
            throw new IllegalArgumentException("event mask contains unknown bits");
        }
    }

    public static M3LazyEventPolicy of(long mask, M3LazyEventGranularity granularity) {
        return new M3LazyEventPolicy(mask, 0L, (short) 0, granularity);
    }

    public long effectiveMask() {
        return includeMask & ~excludeMask;
    }

    public M3LazyEventPolicy overlay(M3LazyEventPolicy override) {
        if (override == null) {
            return this;
        }
        M3LazyEventGranularity mergedGranularity =
                override.granularity == M3LazyEventGranularity.INHERIT
                        ? granularity : override.granularity;
        short mergedStateMask =
                override.watchedStateMask == 0 ? watchedStateMask : override.watchedStateMask;
        return new M3LazyEventPolicy(
                includeMask | override.includeMask,
                excludeMask | override.excludeMask,
                mergedStateMask,
                mergedGranularity);
    }

    public boolean enabled(
            long eventBit,
            M3LazyEventGranularity required,
            short oldState,
            short newState) {
        if ((effectiveMask() & eventBit) == 0L || !granularity.includes(required)) {
            return false;
        }
        if (eventBit == M3LazyEventBits.STATE_CHANGED && watchedStateMask != 0) {
            int changed = Short.toUnsignedInt((short) (oldState ^ newState));
            int watched = Short.toUnsignedInt(watchedStateMask);
            return (changed & watched) != 0;
        }
        return true;
    }
}
