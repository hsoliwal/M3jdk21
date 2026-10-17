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
// preserve Apache 4.6.0 MultiSet saturation, views and array-tail semantics
// while replacing per-key MutableInteger storage with the shared M3 count owner.
package com.m3.collections.interop;

import com.m3.collections.M3ObjectCounts;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import org.apache.commons.collections4.multiset.AbstractMultiSet;

/**
 * Apache MultiSet over M3 primitive count and state columns.
 * Positive additions saturate each count at {@link Integer#MAX_VALUE}; size is
 * capped at the same value while the owner keeps the exact long total. Apache
 * entry and unique-element view contracts are inherited from AbstractMultiSet.
 *
 * <p>Each returned entry is a caller-owned projection. No entry or boxed counter
 * is retained per distinct element. An explicitly supplied owner can be shared
 * with other provider facades. Equality remains Apache MultiSet equality, and
 * flags are owner metadata. Instances are mutable and are not thread-safe.
 *
 * @param <E> element type
 */
public final class M3ApacheMultiSet<E> extends AbstractMultiSet<E> {
    private final M3ObjectCounts<E> owner;
    private long mutationAttempts;

    public M3ApacheMultiSet() { this(new M3ObjectCounts<>()); }

    public M3ApacheMultiSet(M3ObjectCounts<E> owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override public int getCount(Object element) { return owner.count(element); }
    @Override public int size() { return (int) Math.min(owner.totalCount(), Integer.MAX_VALUE); }
    @Override public boolean isEmpty() { return owner.distinctSize() == 0; }
    @Override protected int uniqueElements() { return owner.distinctSize(); }

    @Override
    public int add(E element, int occurrences) {
        if (occurrences < 0) throw new IllegalArgumentException("Occurrences must not be negative.");
        int previous = owner.count(element);
        if (occurrences > 0) {
            mutationAttempts++;
            owner.add(element, Math.min(occurrences, Integer.MAX_VALUE - previous));
        }
        return previous;
    }

    @Override
    public int remove(Object element, int occurrences) {
        int previous = owner.remove(element, occurrences);
        if (previous > 0 && occurrences > 0) mutationAttempts++;
        return previous;
    }

    @Override
    public int setCount(E element, int count) {
        int previous = owner.setCount(element, count);
        if (previous != count) mutationAttempts++;
        return previous;
    }

    @Override
    public void clear() {
        mutationAttempts++;
        owner.clear();
    }

    @Override
    public Iterator<E> iterator() {
        return new M3ApacheCountIterator<>(owner, () -> mutationAttempts);
    }

    @Override
    protected Iterator<E> createUniqueSetIterator() {
        Iterator<E> elements = owner.elementSet().iterator();
        return new Iterator<>() {
            @Override public boolean hasNext() { return elements.hasNext(); }
            @Override public E next() { return elements.next(); }
            @Override public void remove() { elements.remove(); mutationAttempts++; }
        };
    }

    @Override
    protected Iterator<Entry<E>> createEntrySetIterator() {
        Iterator<E> elements = owner.elementSet().iterator();
        return new Iterator<>() {
            private CountEntry last;
            @Override public boolean hasNext() { return elements.hasNext(); }
            @Override public Entry<E> next() { return last = new CountEntry(elements.next()); }
            @Override public void remove() {
                if (last == null) throw new IllegalStateException();
                int count = last.getCount();
                elements.remove();
                last.detachedCount = count;
                last = null;
                mutationAttempts++;
            }
        };
    }

    private final class CountEntry extends AbstractEntry<E> {
        private final E element;
        private int detachedCount = -1;
        CountEntry(E element) { this.element = element; }
        @Override public E getElement() { return element; }
        @Override public int getCount() {
            return detachedCount >= 0 ? detachedCount : owner.count(element);
        }
    }

    @Override
    public int hashCode() {
        int[] hash = {0};
        owner.forEachCount((element, count) -> hash[0] += Objects.hashCode(element) ^ count);
        return hash[0];
    }

    /** Apache fills every excess cell with null, including cells after the terminator. */
    @Override
    public <T> T[] toArray(T[] array) {
        T[] result = super.toArray(array);
        Arrays.fill(result, size(), result.length, null);
        return result;
    }
}
