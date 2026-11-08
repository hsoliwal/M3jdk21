// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Fused masked gather: standalone Java/native differential oracle. */
//FLLDCIM: RELOCATE - donor blob 88ce86d76bfd at d5ad5090d87e; recipe m3/collections/qualification/census2-t1-20261106/relocate.py v5
final class MaskedGatherTest {
    @Test
    void nativeAndJavaMaskedIndexParity() {
        MaskedGatherContract.main(new String[0]);
    }
}
