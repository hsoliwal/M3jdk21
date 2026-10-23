/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3FilteredNavigableComparatorTest {
    @Test void comparatorExceptionsRemainObservable() {
        M3FilteredNavigableComparatorContract.exceptionPreservation();
    }
    @Test void boundedQueriesAndNullableValuesRemainSupported() {
        M3FilteredNavigableComparatorContract.boundedAndNullableParity();
    }
}
