// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/** Primitive-long LIFO contract. */
public interface M3LongStack extends M3LongCollection {
    void push(long value);
    long peek();
    long pop();
}
