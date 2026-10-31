// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Java/JNI primitive three-way partition contract, preserving the Java owner. */
final class M3LanePartition3PreparedTest {
    @Test void javaAndNativePreparedPartition3() {
        M3LanePartition3PreparedContract.contracts();
    }
}
