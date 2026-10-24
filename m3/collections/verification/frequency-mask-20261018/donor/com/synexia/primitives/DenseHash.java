/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.synexia.primitives;

final class DenseHash {
    static final float DEFAULT_LOAD_FACTOR = 0.65f;
    static final int MIN_CAPACITY = 8;

    private DenseHash() {}

    static int capacityFor(int expectedSize, float loadFactor) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize < 0");
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor must be in (0,1)");
        }
        long needed = Math.max(MIN_CAPACITY,
                (long) Math.ceil(expectedSize / (double) loadFactor));
        if (needed > (1L << 30)) throw new IllegalArgumentException("collection too large");
        int n = 1;
        while (n < needed) n <<= 1;
        return n;
    }

    static int threshold(int capacity, float loadFactor) {
        return Math.min(capacity - 1, Math.max(1, (int) (capacity * loadFactor)));
    }

    static int mix(int value) {
        int x = value;
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    static int mix(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        z ^= z >>> 31;
        return (int) (z ^ (z >>> 32));
    }
}
