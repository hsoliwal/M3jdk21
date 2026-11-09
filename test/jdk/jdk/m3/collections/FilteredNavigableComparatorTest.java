/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class FilteredNavigableComparatorTest {
    @Test void comparatorExceptionsRemainObservable() {
        FilteredNavigableComparatorContract.exceptionPreservation();
    }
    @Test void boundedQueriesAndNullableValuesRemainSupported() {
        FilteredNavigableComparatorContract.boundedAndNullableParity();
    }
}
