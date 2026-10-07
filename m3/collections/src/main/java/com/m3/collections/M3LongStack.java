// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/** Primitive-long LIFO contract. */
public interface M3LongStack extends M3LongCollection {
    void push(long value);
    long peek();
    long pop();
}
