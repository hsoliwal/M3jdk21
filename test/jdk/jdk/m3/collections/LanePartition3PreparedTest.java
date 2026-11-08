// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LanePartition3PreparedContract;

/** Java/JNI primitive three-way partition contract, preserving the Java owner. */
//FLLDCIM: RELOCATE - donor blob 6c25241f09af at d5ad5090d87e; recipe m3/collections/qualification/census2-t1-20261106/relocate.py v5
final class LanePartition3PreparedTest {
    @Test void javaAndNativePreparedPartition3() {
        LanePartition3PreparedContract.contracts();
    }
}
