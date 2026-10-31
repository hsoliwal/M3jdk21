// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/** Allocation-free dispatch helper shared by dependency-light lazy owners. */
public final class M3LazyEvents {
    private M3LazyEvents() {
    }

    public static <M> void emit(
            M3LazyEventPolicy policy,
            M3LazyEventSink<? super M> sink,
            long eventBit,
            M3LazyEventGranularity required,
            long slot,
            short oldState,
            short newState,
            M3LazyOperation operation,
            M monitor,
            Throwable failure) {
        M3LazyEventPolicy actualPolicy = Objects.requireNonNull(policy, "policy");
        if (!actualPolicy.enabled(eventBit, required, oldState, newState)) {
            return;
        }
        Objects.requireNonNull(sink, "sink").onEvent(
                eventBit,
                slot,
                oldState,
                newState,
                Objects.requireNonNull(operation, "operation"),
                monitor,
                failure);
    }
}
