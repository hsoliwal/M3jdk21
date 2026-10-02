/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.LongCollection
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/** Minimal primitive-long collection contract without Collection<Long> boxing. */
public interface M3LongCollection {
    int size();

    boolean contains(long value);

    void clear();

    M3LongIterator iterator();

    long[] toArray();

    default boolean isEmpty() {
        return size() == 0;
    }

    default void forEachLong(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        M3LongIterator iterator = iterator();
        while (iterator.hasNext()) {
            consumer.accept(iterator.nextLong());
        }
    }
}
