/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit adapter for the independent readonly set interior-null oracle. */
final class M3FilteredSetNullTest {
    @Test void readonlySetNullContract() {
        M3FilteredSetNullContract.runAll();
    }
}
