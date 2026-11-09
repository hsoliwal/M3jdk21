// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;
import org.junit.jupiter.api.Test;

/** Maven/JUnit adapter for actual M3 array/index/state-mask contract proof. */
final class ZeroMaskArrayFastPathTest {
    @Test void noOpWindowAndPrimitiveMaskCompactionStayContractIdentical() {
        ZeroMaskArrayFastPathProof.main(new String[0]);
    }
}
