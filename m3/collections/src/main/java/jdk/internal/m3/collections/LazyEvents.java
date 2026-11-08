// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;
import jdk.m3.collections.LazyEventGranularity;
import jdk.m3.collections.LazyEventPolicy;
import jdk.m3.collections.M3LazyEventSink;
import jdk.m3.collections.M3LazyOperation;

/** Allocation-free dispatch helper shared by dependency-light lazy owners. */
public final class LazyEvents {
    private LazyEvents() {
    }

    public static <M> void emit(
            LazyEventPolicy policy,
            M3LazyEventSink<? super M> sink,
            long eventBit,
            LazyEventGranularity required,
            long slot,
            short oldState,
            short newState,
            M3LazyOperation operation,
            M monitor,
            Throwable failure) {
        LazyEventPolicy actualPolicy = Objects.requireNonNull(policy, "policy");
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
