// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

/**
 * Primitive lazy event sink.
 *
 * <p>The monitor type is caller-selected so dependency-light common owners can use their existing
 * progress type while higher layers can use IProgressMonitor without creating dependency cycles.</p>
 */
@FunctionalInterface
public interface LazyEventSink<M> {
    long NO_SLOT = -1L;

    void onEvent(
            long eventBit,
            long slot,
            short oldState,
            short newState,
            LazyOperation operation,
            M monitor,
            Throwable failure);

    @SuppressWarnings("unchecked")
    static <M> LazyEventSink<M> noop() {
        return (LazyEventSink<M>) NoOpHolder.INSTANCE;
    }

    final class NoOpHolder {
        private static final LazyEventSink<Object> INSTANCE =
                (eventBit, slot, oldState, newState, operation, monitor, failure) -> { };

        private NoOpHolder() {
        }
    }
}
