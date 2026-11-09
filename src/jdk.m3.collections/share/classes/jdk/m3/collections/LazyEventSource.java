// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

/** Optional event configuration exposed by a lazy provider or owner. */
public interface LazyEventSource<M> {
    default LazyEventPolicy lazyEventPolicy() {
        return LazyEventPolicy.NONE;
    }

    default LazyEventSink<M> lazyEventSink() {
        return LazyEventSink.noop();
    }
}
