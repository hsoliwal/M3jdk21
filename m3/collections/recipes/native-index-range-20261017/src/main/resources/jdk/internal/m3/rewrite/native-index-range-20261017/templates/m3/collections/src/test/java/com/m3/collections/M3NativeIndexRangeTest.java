/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;
import org.junit.jupiter.api.Test;
final class M3NativeIndexRangeTest {
    @Test void snapshotContracts() throws Exception {
        M3NativeIndexRangeSelfTest.main(new String[]{
                M3IndexMaskCompactor.nativeAvailable() ? "legacy" : "none", "sharing"});
    }
}
