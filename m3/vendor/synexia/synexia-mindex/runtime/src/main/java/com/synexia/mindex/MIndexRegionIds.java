// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.concurrent.atomic.AtomicLong;

/** Process-local region identity allocator for in-memory MIndex stores. */
final class MIndexRegionIds {
    private static final AtomicLong NEXT = new AtomicLong(1L);

    private MIndexRegionIds() {}

    static long next() {
        long id = NEXT.getAndIncrement();
        if (id <= 0L) {
            throw new IllegalStateException("MIndex region id space exhausted");
        }
        return id;
    }
}
