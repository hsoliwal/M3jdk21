// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3LongAddressKernelTest {
    @Test
    void directAddressAndBatchParity() {
        M3LongAddressKernelSelfTest.run();
    }

    @Test
    void failClosedAllocationPreflight() {
        M3LongAddressKernelPreflightContract.main(new String[0]);
    }
}
