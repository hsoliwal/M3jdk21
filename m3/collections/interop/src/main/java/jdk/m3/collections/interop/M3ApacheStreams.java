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
package jdk.m3.collections.interop;

import jdk.m3.collections.ObjectStream;
import java.util.Objects;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.MultiValuedMap;
/** Opt-in collection bridge without changing Apache predicate/admission policy. */
public final class M3ApacheStreams {
    private M3ApacheStreams() {}
    public static <K,V> ObjectStream<V> snapshotValues(MultiValuedMap<K,V> map,K key) {
        return ObjectStream.snapshot(Objects.requireNonNull(map, "map").get(key));
    }
    public static <K,V> ObjectStream<V> liveValues(MultiValuedMap<K,V> map,K key) {
        return ObjectStream.live(Objects.requireNonNull(map, "map").get(key));
    }
    public static <T> ObjectStream<T> snapshotBag(Bag<T> bag) {
        return ObjectStream.snapshot(Objects.requireNonNull(bag, "bag"));
    }

    /** Universal Iterable adapter for List, Set, Queue, Bag and other provider occurrences. */
    public static <T> ObjectStream<T> snapshotAny(Iterable<? extends T> source) {
        return ObjectStream.snapshot(Objects.requireNonNull(source, "source"));
    }

    /** Universal live adapter; encounter and fail-fast semantics remain provider-owned. */
    public static <T> ObjectStream<T> liveAny(java.util.Collection<T> source) {
        return ObjectStream.live(Objects.requireNonNull(source, "source"));
    }
}
