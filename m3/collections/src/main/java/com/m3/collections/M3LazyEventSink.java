// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/**
 * Primitive lazy event sink.
 *
 * <p>The monitor type is caller-selected so dependency-light common owners can use their existing
 * progress type while higher layers can use IProgressMonitor without creating dependency cycles.</p>
 */
@FunctionalInterface
public interface M3LazyEventSink<M> {
    long NO_SLOT = -1L;

    void onEvent(
            long eventBit,
            long slot,
            short oldState,
            short newState,
            M3LazyOperation operation,
            M monitor,
            Throwable failure);

    @SuppressWarnings("unchecked")
    static <M> M3LazyEventSink<M> noop() {
        return (M3LazyEventSink<M>) NoOpHolder.INSTANCE;
    }

    final class NoOpHolder {
        private static final M3LazyEventSink<Object> INSTANCE =
                (eventBit, slot, oldState, newState, operation, monitor, failure) -> { };

        private NoOpHolder() {
        }
    }
}
