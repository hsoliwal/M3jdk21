// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Admits the original M3JDK 21 index-only owner under JUnit. */
final class M3IndexSortAdaptiveTest {
    @Test void adaptivePrimitiveIndexSorting() {
        M3IndexSortAdaptiveContract.contracts();
    }
}
