// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Executes the standalone differential and observer contract in the owning Maven suite. */
final class M3ObservableListTest {
    @Test
    void seededDifferentialAndNotificationOrder() {
        M3ObservableListContract.main(new String[0]);
    }
}
