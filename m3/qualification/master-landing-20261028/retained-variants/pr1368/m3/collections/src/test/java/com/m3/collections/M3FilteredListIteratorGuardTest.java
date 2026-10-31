// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Executes the same standalone contract used by bounded receiving checks. */
final class M3FilteredListIteratorGuardTest {
    @Test
    void refusesRemovalAndPreservesReadEffects() {
        M3FilteredListIteratorGuardContract.main(new String[0]);
    }
}
