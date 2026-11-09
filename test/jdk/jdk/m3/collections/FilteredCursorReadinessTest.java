/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the same bounded Java21 cursor contract under the collection Maven suite. */
//FLLDCIM: RELOCATE - donor blob 6086bc8870f2 at d5ad5090d87e; recipe m3/collections/qualification/filtered-views-owner-20261106/relocate.py v6
final class FilteredCursorReadinessTest {
    @Test void preparedPhysicalSlotsPreserveSelectionAndRevision() {
        FilteredCursorReadinessSelfTest.main(new String[0]);
    }
}
