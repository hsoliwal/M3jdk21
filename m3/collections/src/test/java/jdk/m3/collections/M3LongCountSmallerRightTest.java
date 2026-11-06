// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/** Original M3JDK 21 LC315 Java/JNI primitive index-merging contracts. */
//FLLDCIM: RELOCATE - donor blob ce53d077c93d at d5ad5090d87e; recipe m3/collections/qualification/census2-c4-20261106/relocate.py v1
final class M3LongCountSmallerRightTest {
    @Test
    void countSmallerRightIsStableAndDoesNotCopyOwnerPayload() {
        M3LongCountSmallerRightContract.main(new String[0]);
        assertArrayEquals(new int[] {2, 1, 1, 0},
                M3LongRankIndex.countSmallerAfterSelf(new long[] {5, 2, 6, 1}));
    }
}
