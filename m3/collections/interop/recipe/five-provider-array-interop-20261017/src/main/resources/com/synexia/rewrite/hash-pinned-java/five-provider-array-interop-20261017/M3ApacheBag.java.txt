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
// preserve legacy Apache 4.6.0 Bag cardinality contracts over primitive M3 counts;
// temporary frequency/excess work uses M3 owners, not donor boxed counter maps.
package com.m3.collections.interop;

import com.m3.collections.M3ObjectCounts;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.Bag;

/**
 * Explicit compatibility face for the deprecated Apache Bag contract.
 * Duplicate add returns false, remove(element) erases all copies, and bulk
 * membership/removal/retention respect multiplicity. The unique set is an
 * unmodifiable live view; iterator.remove erases one occurrence.
 *
 * <p>Counts and flags belong to one M3 primitive owner, which may be supplied
 * explicitly for shared storage. Per-element and size saturation follow Apache
 * 4.6.0. Instances are mutable and are not thread-safe.
 *
 * @param <E> element type
 */
@SuppressWarnings("deprecation")
public final class M3ApacheBag<E> extends AbstractCollection<E> implements Bag<E> {
    private final M3ObjectCounts<E> owner;
    private final Set<E> unique;
    private long mutationAttempts;

    public M3ApacheBag() { this(new M3ObjectCounts<>()); }

    public M3ApacheBag(M3ObjectCounts<E> owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.unique = Collections.unmodifiableSet(owner.elementSet());
    }

    @Override public int getCount(Object element) { return owner.count(element); }
    @Override public int size() { return (int) Math.min(owner.totalCount(), Integer.MAX_VALUE); }
    @Override public boolean isEmpty() { return owner.distinctSize() == 0; }
    @Override public boolean contains(Object element) { return owner.count(element) > 0; }
    @Override public Set<E> uniqueSet() { return unique; }
    @Override public boolean add(E element) { return add(element, 1); }

    @Override
    public boolean add(E element, int copies) {
        mutationAttempts++;
        if (copies <= 0) return false;
        int previous = owner.count(element);
        owner.add(element, Math.min(copies, Integer.MAX_VALUE - previous));
        return previous == 0;
    }

    @Override
    public boolean remove(Object element) {
        int previous = owner.remove(element, Integer.MAX_VALUE);
        if (previous > 0) mutationAttempts++;
        return previous > 0;
    }

    @Override
    public boolean remove(Object element, int copies) {
        if (copies <= 0) return false;
        int previous = owner.remove(element, copies);
        if (previous > 0) mutationAttempts++;
        return previous > 0;
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
    public boolean containsAll(Collection<?> values) {
        Bag<?> other = asBag(values);
        for (Object element : other.uniqueSet()) {
            if (owner.count(element) < other.getCount(element)) return false;
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> values) {
        boolean changed = false;
        if (values != null) {
            for (Object element : values) changed = remove(element, 1) || changed;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> values) {
        Bag<?> other = asBag(values);
        boolean changed = false;
        Iterator<E> elements = owner.elementSet().iterator();
        while (elements.hasNext()) {
            E element = elements.next();
            int previous = owner.count(element), retained = other.getCount(element);
            if (retained == 0) {
                elements.remove();
                mutationAttempts++;
                changed = true;
            } else if (retained < previous) {
                owner.remove(element, previous - retained);
                mutationAttempts++;
                changed = true;
            }
        }
        return changed;
    }

    private static Bag<?> asBag(Collection<?> values) {
        if (values instanceof Bag<?> bag) return bag;
        M3ApacheBag<Object> frequencies = new M3ApacheBag<>();
        frequencies.addAll(Objects.requireNonNull(values, "values"));
        return frequencies;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Bag<?> bag) || bag.size() != size()) return false;
        for (E element : owner.elementSet()) {
            if (owner.count(element) != bag.getCount(element)) return false;
        }
        return true;
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

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[");
        Iterator<E> elements = owner.elementSet().iterator();
        while (elements.hasNext()) {
            E element = elements.next();
            result.append(owner.count(element)).append(':').append(element);
            if (elements.hasNext()) result.append(',');
        }
        return result.append(']').toString();
    }
}
