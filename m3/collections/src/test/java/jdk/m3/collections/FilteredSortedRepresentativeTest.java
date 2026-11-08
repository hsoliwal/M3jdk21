/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
//FLLDCIM: MRL - mergeloss2 leaf 27: origin test restored from origin 01952e935de (PR #1619), dropped by retain merge 1633da209622; recipe qualification/mergeloss2-filtered-views-20261107
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class FilteredSortedRepresentativeTest {
    @Test void retainedSortedKeysSurviveOverloadsRangesAndPrivateNesting() {
        FilteredSortedRepresentativeContract.run();
    }
}
