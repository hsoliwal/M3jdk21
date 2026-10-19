/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the same bounded Java21 cursor contract under the collection Maven suite. */
final class M3FilteredCursorReadinessTest {
    @Test void preparedPhysicalSlotsPreserveSelectionAndRevision() {
        M3FilteredCursorReadinessSelfTest.main(new String[0]);
    }
}
