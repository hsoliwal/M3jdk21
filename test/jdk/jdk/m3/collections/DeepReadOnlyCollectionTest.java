/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class DeepReadOnlyCollectionTest {
    @Test void deepKnownReadOnlyIteratorsAndMembershipStayOnHeap() {
        DeepReadOnlyCollectionSelfTest.deepTraversal(65536);
    }

    @Test void originalRepeatedMembershipDemandAndFailuresArePreserved() {
        DeepReadOnlyCollectionSelfTest.membershipDemand();
    }
}
