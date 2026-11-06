// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package jdk.m3.collections;

/** Overflow-checked size arithmetic, before mutation or allocation. */
//FLLDCIM: RELOCATE - donor blob 0606653c5bad at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
final class M3PackedArrayCapacity {
    private M3PackedArrayCapacity() { }

    static int required(int size, int extra) {
        if (size < 0 || extra < 0 || extra > M3PackedSupport.MAX_ARRAY_SIZE - size) {
            throw new OutOfMemoryError("required array size too large");
        }
        return size + extra;
    }
}
