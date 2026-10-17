/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Execute the reused first-party deep-list contract corpus in the owning Maven module. */
final class M3DeepFilteredListsTest {
    @Test void orderedDeepFiltersPreserveListContracts() throws ReflectiveOperationException {
        M3DeepFilteredListsProof.run(65536, 192);
    }
}
