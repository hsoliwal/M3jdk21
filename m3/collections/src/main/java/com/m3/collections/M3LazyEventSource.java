// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/** Optional event configuration exposed by a lazy provider or owner. */
public interface M3LazyEventSource<M> {
    default M3LazyEventPolicy lazyEventPolicy() {
        return M3LazyEventPolicy.NONE;
    }

    default M3LazyEventSink<M> lazyEventSink() {
        return M3LazyEventSink.noop();
    }
}
