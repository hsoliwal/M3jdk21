/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

/** Primitive-long LIFO contract (Synexia {@code LongStack} lineage). */
public interface LongStack extends LongCollection {
    void push(long value);

    long peek();

    long pop();
}
