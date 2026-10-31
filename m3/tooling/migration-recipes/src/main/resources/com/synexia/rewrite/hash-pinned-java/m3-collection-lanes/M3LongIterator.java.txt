/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/** Primitive iterator contract: no boxed Long is created per element. */
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
