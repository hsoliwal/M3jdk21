// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class LongAddressKernelTest {
    @Test
    void directAddressAndBatchParity() {
        LongAddressKernelSelfTest.run();
    }

    @Test
    void failClosedAllocationPreflight() {
        LongAddressKernelPreflightContract.main(new String[0]);
    }
}
