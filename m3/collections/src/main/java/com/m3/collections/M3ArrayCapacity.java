/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

/** Overflow-checked size arithmetic, before mutation or allocation. */
final class M3ArrayCapacity {
    private M3ArrayCapacity() { }

    static int required(int size, int extra) {
        if (size < 0 || extra < 0 || extra > M3CollectionSupport.MAX_ARRAY_SIZE - size) {
            throw new OutOfMemoryError("required array size too large");
        }
        return size + extra;
    }
}
