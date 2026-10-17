/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// SPDX-License-Identifier: Apache-2.0
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project:
// adapt Apache 4.6.0 counted-iteration state transitions to primitive M3 counts.
package com.m3.collections.interop;

import com.m3.collections.M3ObjectCounts;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.function.LongSupplier;

/** Shared occurrence iterator; only the current key and primitive progress are retained. */
final class M3ApacheCountIterator<E> implements Iterator<E> {
    private final M3ObjectCounts<E> owner;
    private final Iterator<E> elements;
    private final LongSupplier attempts;
    private long expectedRevision;
    private final long expectedAttempts;
    private E current;
    private int remaining;
    private boolean canRemove;

    M3ApacheCountIterator(M3ObjectCounts<E> owner, LongSupplier attempts) {
        this.owner = owner;
        this.elements = owner.elementSet().iterator();
        this.attempts = attempts;
        this.expectedRevision = owner.countRevision();
        this.expectedAttempts = attempts.getAsLong();
    }

    @Override
    public boolean hasNext() {
        return remaining > 0 || elements.hasNext();
    }

    @Override
    public E next() {
        check();
        if (remaining == 0) {
            current = elements.next();
            remaining = owner.count(current);
        }
        remaining--;
        canRemove = true;
        return current;
    }

    @Override
    public void remove() {
        check();
        if (!canRemove) throw new IllegalStateException();
        if (owner.count(current) > 1) owner.remove(current, 1);
        else elements.remove();
        expectedRevision = owner.countRevision();
        canRemove = false;
    }

    private void check() {
        if (expectedRevision != owner.countRevision() || expectedAttempts != attempts.getAsLong())
            throw new ConcurrentModificationException();
    }
}
