/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

/** Primitive two-long consumer used by packed maps without Map.Entry materialization. */
@FunctionalInterface
public interface M3LongLongConsumer {
    void accept(long first, long second);
}
