// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/**
 * Caller-owned synchronous model/storage callback for a deferred tree value.
 * The key is the canonical stored key. Null is a successfully loaded value.
 * The supplied progress is never null; long-running providers should check
 * cancellation. The provider must not mutate the shared map or its views.
 * Different-key reads are allowed; cyclic reads fail with IllegalStateException.
 */
@FunctionalInterface
public interface M3LazyValueProvider<K, V> extends M3LazyEventSource<M3Progress> {
    /** No mapping in the queried view; this is not a stored flag. */
    short ABSENT = -1;
    /** Admitted key whose value has not been requested. */
    short UNLOADED = 0;
    /** Provider is currently resolving this slot. */
    short LOADING = 1;
    /** Successfully resolved or explicitly supplied, including null. */
    short LOADED = 2;
    /** Last attempt failed; a subsequent read may retry. No Throwable is retained. */
    short FAILED = 4;

    /** Load one value; exceptions propagate unchanged and do not publish a value. */
    V load(K key, M3Progress progress);
}
