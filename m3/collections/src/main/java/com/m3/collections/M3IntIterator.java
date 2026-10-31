/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.IntConsumer;

/** Primitive iterator contract: no boxed Integer is created per element. */
public interface M3IntIterator {
    boolean hasNext();

    int nextInt();

    default void forEachRemaining(IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        while (hasNext()) {
            consumer.accept(nextInt());
        }
    }
}
