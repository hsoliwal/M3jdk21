// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.FilteredListsOwnerSetContract;
/** JUnit receiver of the independently executable original Java21 owner test. */
final class FilteredListsOwnerSetTest {
    @Test void backingListIteratorAndCopyOnWriteContracts() {
        FilteredListsOwnerSetContract.main(new String[0]);
    }
}
