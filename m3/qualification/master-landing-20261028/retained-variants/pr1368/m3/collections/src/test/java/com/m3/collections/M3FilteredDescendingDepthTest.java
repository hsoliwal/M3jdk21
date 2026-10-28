/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3FilteredDescendingDepthTest {
    @Test void deepPrivateDescendingFiltersRetainEncounterAndPredicateOrder() {
        M3FilteredDescendingDepthContract.main(new String[0]);
    }

    @Test void boundedDescendingCallbacksNullsRangesAndFailuresStayCompatible() {
        M3FilteredDescendingDepthContract.main(new String[] {"bounded"});
    }
}
