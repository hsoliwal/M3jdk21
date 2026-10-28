/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Real JUnit entry points into the standalone boundary, identity and live-owner oracle. */
final class M3FilteredNavigationBoundaryTest {
    @Test void boundedNavigationAcrossOrdersAndOwners() {
        M3FilteredNavigationBoundarySelfTest.boundedCorpus(false);
    }

    @Test void storedReferencesAndPredicateFailureIdentity() {
        M3FilteredNavigationBoundarySelfTest.identityAndFailure();
    }

    @Test void liveOwnersAndQueryValidation() {
        M3FilteredNavigationBoundarySelfTest.liveAndQueryContracts();
    }
}
