/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package jdk.m3.collections.interop;

import com.google.common.collect.Multiset;
import java.util.Collection;
import java.util.Objects;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import org.eclipse.collections.api.bag.primitive.LongBag;

/** Explicit library boundaries for M3-owned long counts; no expanded-occurrence storage. */
//FLLDCIM: MRL - fold-close audit leaf L0050: ADD from PR #1084 head 6b16a20dcedf blob f22e5603a10d, ancestry-only landing b42ab7605b86 (tree == first parent); recipe make/m3/collections/qualification/foldaudit-collections-20261107/apply.py v1
//FLLDCIM: MRL - the PR-head owner spelled size()/distinctSize()/add(long); master's jdk.m3.collections.LongBag (e6722c63e9b, blob d00efc879547) spells totalCount()/distinctCount()/add(long, long): calls renamed, contract unchanged (HANDOFF.tsv)
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
    public static jdk.m3.collections.LongBag fromGuava(Multiset<Long> source) {
        Objects.requireNonNull(source, "source");
        jdk.m3.collections.LongBag result = new jdk.m3.collections.LongBag(source.entrySet().size());
        for (Multiset.Entry<Long> entry : source.entrySet()) {
            result.setCount(Objects.requireNonNull(entry.getElement(), "key"), entry.getCount());
        }
        return result;
    }
    /** Primitive Eclipse traversal imports counts directly; it does not claim Eclipse interface substitutability. */
    public static jdk.m3.collections.LongBag fromEclipse(LongBag source) {
        Objects.requireNonNull(source, "source");
        jdk.m3.collections.LongBag result = new jdk.m3.collections.LongBag(source.sizeDistinct());
        source.forEachWithOccurrences((key, count) -> result.setCount(key, count));
        return result;
    }
    /** JDK has no Multiset interface; this import counts occurrences and rejects null keys. */
    public static jdk.m3.collections.LongBag fromJdk(Collection<Long> source) {
        Objects.requireNonNull(source, "source");
        jdk.m3.collections.LongBag result = new jdk.m3.collections.LongBag();
        for (Long key : source) result.add(Objects.requireNonNull(key, "key"), 1L);
        return result;
    }
}
