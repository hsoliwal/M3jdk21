// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3FilteredListsOwnerRemovalContract;

/** JUnit receiver for actual indexed/cursor owner-slot differential checks. */
final class M3FilteredListsOwnerRemovalTest {
    @Test void ownerMutationContracts() {
        assertTrue(M3FilteredListsOwnerRemovalContract.runAll() > 300_000L);
    }
}
