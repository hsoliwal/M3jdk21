/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections.interop;

import com.m3.collections.M3ObjectStream;
import java.util.List;
import java.util.Objects;
import org.springframework.util.MultiValueMap;
/** Explicit per-key value-list semantics; existing Spring map contracts stay owned by Spring. */
public final class M3SpringStreams {
    private M3SpringStreams() {}
    public static <K,V> M3ObjectStream<V> snapshotValues(MultiValueMap<K,V> map,K key) {
        List<V> values = Objects.requireNonNull(map, "map").get(key);
        return M3ObjectStream.snapshot(values == null ? List.<V>of() : values);
    }
    /** Live with respect to mutation of the original list, not replacement of the mapping. */
    public static <K,V> M3ObjectStream<V> liveExistingValues(MultiValueMap<K,V> map,K key) {
        List<V> values = Objects.requireNonNull(map, "map").get(key);
        return M3ObjectStream.live(values == null ? List.<V>of() : values);
    }

    /** Universal Iterable adapter for List, Set, Queue, Bag and other provider occurrences. */
    public static <T> M3ObjectStream<T> snapshotAny(Iterable<? extends T> source) {
        return M3ObjectStream.snapshot(Objects.requireNonNull(source, "source"));
    }

    /** Universal live adapter; encounter and fail-fast semantics remain provider-owned. */
    public static <T> M3ObjectStream<T> liveAny(java.util.Collection<T> source) {
        return M3ObjectStream.live(Objects.requireNonNull(source, "source"));
    }
}
