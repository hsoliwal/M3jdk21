// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.function.LongPredicate;
import jdk.internal.m3.collections.packed.PackedLongSortedSet;

/** Mutable navigable primitive-long set contract. */
public interface LongSortedSet extends LongCollection {
    /** Returns an empty sorted-array set (API_UNION U7). */
    static LongSortedSet of() { return new PackedLongSortedSet(); }

    /** Returns an empty sorted-array set sized for {@code expectedSize} values (API_UNION U7). */
    static LongSortedSet of(int expectedSize) { return new PackedLongSortedSet(expectedSize); }

    boolean add(long value);
    boolean remove(long value);
    long first();
    long last();
    long floor(long value, long defaultValue);
    long ceiling(long value, long defaultValue);
    long lower(long value, long defaultValue);
    long higher(long value, long defaultValue);
    int filterInPlace(LongPredicate predicate);
}
