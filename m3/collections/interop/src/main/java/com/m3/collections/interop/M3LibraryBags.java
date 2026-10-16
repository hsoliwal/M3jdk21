/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import com.google.common.collect.Multiset;
import com.m3.collections.M3LongBag;
import java.util.Collection;
import java.util.Objects;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import org.eclipse.collections.api.bag.primitive.LongBag;

/** Explicit library boundaries for M3-owned long counts; no expanded-occurrence storage. */
public final class M3LibraryBags {
    private M3LibraryBags() { }
    public static Multiset<Long> guavaLongMultiset() { return new M3GuavaLongMultiset(); }

    /** Parallel partials are independently owned; no CONCURRENT collector promise. */
    public static <T> Collector<T, ?, Multiset<Long>> toGuavaLongMultiset(ToLongFunction<? super T> value) {
        Objects.requireNonNull(value, "value");
        return Collector.of(M3GuavaLongMultiset::new,
                (bag, item) -> bag.add(value.applyAsLong(item)),
                (left, right) -> { left.addAll(right); return left; },
                bag -> bag);
    }
    public static M3LongBag fromGuava(Multiset<Long> source) {
        Objects.requireNonNull(source, "source");
        M3LongBag result = new M3LongBag(source.entrySet().size());
        for (Multiset.Entry<Long> entry : source.entrySet()) {
            result.setCount(Objects.requireNonNull(entry.getElement(), "key"), entry.getCount());
        }
        return result;
    }
    /** Primitive Eclipse traversal imports counts directly; it does not claim Eclipse interface substitutability. */
    public static M3LongBag fromEclipse(LongBag source) {
        Objects.requireNonNull(source, "source");
        M3LongBag result = new M3LongBag(source.sizeDistinct());
        source.forEachWithOccurrences((key, count) -> result.setCount(key, count));
        return result;
    }
    /** JDK has no Multiset interface; this import counts occurrences and rejects null keys. */
    public static M3LongBag fromJdk(Collection<Long> source) {
        Objects.requireNonNull(source, "source");
        M3LongBag result = new M3LongBag();
        for (Long key : source) result.add(Objects.requireNonNull(key, "key"));
        return result;
    }
}
