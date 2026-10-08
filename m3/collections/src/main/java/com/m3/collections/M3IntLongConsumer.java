/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/** Primitive {@code (int key, long value)} visitor; the int-to-long mirror of {@link M3IntIntConsumer}. */
@FunctionalInterface
public interface M3IntLongConsumer {
    void accept(int key, long value);
}
