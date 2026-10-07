// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

/** Overflow-checked size arithmetic, before mutation or allocation. */
final class M3PackedArrayCapacity {
    private M3PackedArrayCapacity() { }

    static int required(int size, int extra) {
        if (size < 0 || extra < 0 || extra > M3PackedSupport.MAX_ARRAY_SIZE - size) {
            throw new OutOfMemoryError("required array size too large");
        }
        return size + extra;
    }
}
