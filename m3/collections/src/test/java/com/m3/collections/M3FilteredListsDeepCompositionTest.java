// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import org.junit.jupiter.api.Test;
/** JUnit adapter for exact deep composition and JDK 21 List order contracts. */
final class M3FilteredListsDeepCompositionTest {
    @Test void deepChainShortCircuitAndSortedListInterop() {
        M3FilteredListsDeepCompositionProof.main(new String[0]);
    }
}
