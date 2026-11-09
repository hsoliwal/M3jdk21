// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.FilteredListsDeepCompositionProof;
/** JUnit adapter for exact deep composition and JDK 21 List order contracts. */
final class FilteredListsDeepCompositionTest {
    @Test void deepChainShortCircuitAndSortedListInterop() {
        FilteredListsDeepCompositionProof.main(new String[0]);
    }
}
