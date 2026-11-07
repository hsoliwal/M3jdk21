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
package jdk.internal.m3.eclipse;

import jdk.m3.collections.ObjectStream;
import java.util.Objects;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.list.MutableList;
/** Opt-in RichIterable/MutableList gateway into M3JDK 21 operations. */
public final class M3EclipseStreams {
    private M3EclipseStreams() {}
    public static <T> ObjectStream<T> snapshot(RichIterable<T> input) {
        return ObjectStream.snapshot((Iterable<T>) Objects.requireNonNull(input, "input"));
    }
    public static <T> ObjectStream<T> snapshotList(MutableList<T> input) {
        return ObjectStream.snapshot(Objects.requireNonNull(input, "input"));
    }
    public static <T> ObjectStream<T> liveList(MutableList<T> input) {
        return ObjectStream.live(Objects.requireNonNull(input, "input"));
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
