// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

class M3FilteredCowBulkTest {
    @Test void storedOccurrencesAndBackedWrites() { M3FilteredCowBulkContract.selectedOccurrences(); }
    @Test void emptyHiddenAndNullValidation() { M3FilteredCowBulkContract.noMatchAndValidation(); }
    @Test void copyOnWritePublicationIsAtomicOnFailure() { M3FilteredCowBulkContract.failureAtomicity(); }
    @Test void ordinaryListsRetainPartialFailureBehavior() { M3FilteredCowBulkContract.regularListPartialFailure(); }
    @Test void nestedAndAliasedViewsUseTheOriginalOwner() { M3FilteredCowBulkContract.nestedAndAliasedViews(); }
}
