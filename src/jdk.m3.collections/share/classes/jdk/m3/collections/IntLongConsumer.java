/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

/** Primitive {@code (int key, long value)} visitor; the int-to-long mirror of {@link IntIntConsumer}. */
@FunctionalInterface
public interface IntLongConsumer {
    void accept(int key, long value);
}
