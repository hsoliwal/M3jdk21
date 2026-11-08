/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: MRL - mergeloss2 leaf 42: origin test restored from origin 793988c0c6a (PR #1368), dropped by retain merge 082b54fa4ab7; recipe qualification/mergeloss2-filtered-views-20261107
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class FilteredDescendingDepthTest {
    @Test void deepPrivateDescendingFiltersRetainEncounterAndPredicateOrder() {
        FilteredDescendingDepthContract.main(new String[0]);
    }

    @Test void boundedDescendingCallbacksNullsRangesAndFailuresStayCompatible() {
        FilteredDescendingDepthContract.main(new String[] {"bounded"});
    }
}
