/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

/** Primitive int-key, int-value callback: no entry object or boxing per visit. */
@FunctionalInterface
public interface IntIntConsumer {
    void accept(int key, int value);
}
