/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.LongIterator
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/** Primitive iterator contract that does not box one Long per element. */
public interface M3LongIterator {
    boolean hasNext();

    long nextLong();

    default void forEachRemaining(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        while (hasNext()) {
            consumer.accept(nextLong());
        }
    }
}
