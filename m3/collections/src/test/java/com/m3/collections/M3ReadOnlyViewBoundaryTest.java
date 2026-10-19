/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3ReadOnlyViewBoundaryTest {
    @Test void mutationsRefuseBeforeCallbacksOrEmptyChecks() {
        M3ReadOnlyViewBoundarySelfTest.readOnlyMutations();
    }

    @Test void nestedNullableViewsPreservePredicateDemand() {
        M3ReadOnlyViewBoundarySelfTest.nestedNavigation();
    }

    @Test void nestedNullableMapsKeepBoundsAndDetachedLiveProjections() {
        M3ReadOnlyViewBoundarySelfTest.nestedMapNavigation();
    }
}
