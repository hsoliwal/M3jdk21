// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import org.junit.jupiter.api.Test;
/** JUnit receiver of the independently executable original Java21 owner test. */
final class M3FilteredListsOwnerSetTest {
    @Test void backingListIteratorAndCopyOnWriteContracts() {
        M3FilteredListsOwnerSetContract.main(new String[0]);
    }
}
