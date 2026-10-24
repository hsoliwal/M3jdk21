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
import java.util.Objects;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.MultiValuedMap;
/** Opt-in collection bridge without changing Apache predicate/admission policy. */
public final class M3ApacheStreams {
    private M3ApacheStreams() {}
    public static <K,V> M3ObjectStream<V> snapshotValues(MultiValuedMap<K,V> map,K key) {
        return M3ObjectStream.snapshot(Objects.requireNonNull(map, "map").get(key));
    }
    public static <K,V> M3ObjectStream<V> liveValues(MultiValuedMap<K,V> map,K key) {
        return M3ObjectStream.live(Objects.requireNonNull(map, "map").get(key));
    }
    public static <T> M3ObjectStream<T> snapshotBag(Bag<T> bag) {
        return M3ObjectStream.snapshot(Objects.requireNonNull(bag, "bag"));
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
