// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Objects;
import jdk.internal.m3.collections.M3LazyEventBits;

/** Immutable bitmask and granularity policy for lazy operation observation. */
public record LazyEventPolicy(
        long includeMask,
        long excludeMask,
        short watchedStateMask,
        LazyEventGranularity granularity) {

    public static final LazyEventPolicy NONE =
            new LazyEventPolicy(0L, 0L, (short) 0, LazyEventGranularity.NONE);

    public LazyEventPolicy {
        Objects.requireNonNull(granularity, "granularity");
        if ((includeMask & ~M3LazyEventBits.ALL) != 0L
                || (excludeMask & ~M3LazyEventBits.ALL) != 0L) {
            throw new IllegalArgumentException("event mask contains unknown bits");
        }
    }

    public static LazyEventPolicy of(long mask, LazyEventGranularity granularity) {
        return new LazyEventPolicy(mask, 0L, (short) 0, granularity);
    }

    public long effectiveMask() {
        return includeMask & ~excludeMask;
    }

    public LazyEventPolicy overlay(LazyEventPolicy override) {
        if (override == null) {
            return this;
        }
        LazyEventGranularity mergedGranularity =
                override.granularity == LazyEventGranularity.INHERIT
                        ? granularity : override.granularity;
        short mergedStateMask =
                override.watchedStateMask == 0 ? watchedStateMask : override.watchedStateMask;
        return new LazyEventPolicy(
                includeMask | override.includeMask,
                excludeMask | override.excludeMask,
                mergedStateMask,
                mergedGranularity);
    }

    public boolean enabled(
            long eventBit,
            LazyEventGranularity required,
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
